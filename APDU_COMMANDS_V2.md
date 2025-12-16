# APDU COMMAND REFERENCE v2.0

## 📋 Tổng quan
Applet v2.0 sử dụng cấu trúc APDU mới với Extended APDU support cho photo transfer và cải thiện security với MD5 PIN hashing.

## 🔧 Command Structure
```
CLA INS P1  P2  Lc  Data...     Le
00  XX  XX  XX  XX  [payload]   XX
```

## 📊 Command Table

| INS | Command | P1 | P2 | Data | Response | Description |
|-----|---------|----|----|------|----------|-------------|
| **00** | **VERIFY PIN** | 04 | 00 | [PIN: 4 bytes] | [ok][remaining] | Xác thực PIN |
| **01** | **CREATE/INIT CARD** | 04 | 00 | [PIN: 4 bytes] | [cardId: 32B] | Khởi tạo thẻ |
| **01** | **CREATE AVATAR** | 05 | 09 | [avatar bytes] | [length: 2B] | Upload ảnh |
| **02** | **GET CARD ID** | 00 | 0A | - | [cardId: 32B] | Lấy ID thẻ |
| **02** | **GET TRY REMAINING** | 00 | 08 | - | [tries: 1B] | Số lần thử PIN còn lại |
| **02** | **GET PUBLIC KEY** | 00 | 0B | - | [key: ~135B] | Export RSA public key |
| **02** | **GET AVATAR** | 05 | 09 | - | [avatar bytes] | Download ảnh |
| **02** | **GET BALANCE** | 00 | 0C | - | [balance: 4B] | Lấy số dư (cần PIN) |
| **02** | **GET INFO** | 05 | 07 | - | [info bytes] | Lấy thông tin cá nhân |
| **03** | **UPDATE PIN** | 04 | 00 | [old][new] | [ok: 1B] | Đổi PIN |
| **03** | **UPDATE AVATAR** | 05 | 09 | [avatar bytes] | [length: 2B] | Cập nhật ảnh |
| **03** | **UPDATE INFO** | 05 | 07 | [info bytes] | [ok: 1B] | Cập nhật thông tin |
| **03** | **UPDATE BALANCE** | 05 | 0C | [type][amount] | [newBalance:4B] | Nạp tiền/thanh toán |
| **03** | **SIGN DATA** | 06 | 00 | [data to sign] | [signature] | Ký số RSA |
| **03** | **ACTIVATE CARD** | 0B | 00 | [PIN: 4 bytes] | [ok: 1B] | Kích hoạt thẻ |
| **03** | **DEACTIVATE CARD** | 0C | 00 | - | [ok: 1B] | Vô hiệu hóa thẻ |
| **03** | **FORGET PIN** | 0A | 00 | [newPIN: 4B] | [ok: 1B] | Reset PIN (admin) |
| **10** | **RESET PIN TRIES** | 00 | 00 | - | [tries: 1B] | Reset số lần thử |

## 🔐 Security Features

### PIN Management
- **Hash Algorithm**: MD5 (16 bytes)
- **Max Tries**: 5 attempts
- **Auto-deactivation**: Card deactivated when tries = 0
- **AES Key Derivation**: `AES_KEY = MD5(PIN + SALT)`

### Encryption
- **AES-128-ECB**: For sensitive data (balance, personal info)
- **RSA-1024**: For digital signatures and key exchange
- **Extended APDU**: Support for large data (photos up to 15KB)

## 📝 Detailed Commands

### 1. Initialize Card (INS=01, P1=04, P2=00)
```
Command:  00 01 04 00 04 [PIN: 4 bytes]
Response: [Card ID: 32 bytes]
```
- Tạo Card ID unique
- Hash PIN với MD5
- Generate AES key từ PIN
- Kích hoạt thẻ
- Khởi tạo balance = 0

### 2. Verify PIN (INS=00, P1=04, P2=00)
```
Command:  00 00 04 00 04 [PIN: 4 bytes]
Response: [Success: 1B][Remaining tries: 1B]
```
- Success: 0x01 = OK, 0x00 = Failed
- Remaining tries: 0-5
- Generate AES key nếu PIN đúng

### 3. Upload Avatar (INS=01, P1=05, P2=09)
```
Command:  00 01 05 09 [Lc] [Avatar data...]
Response: [Avatar length: 2 bytes]
```
- Extended APDU support
- Max size: 15KB (15360 bytes)
- Cần PIN verification trước

### 4. Get Public Key (INS=02, P1=00, P2=0B)
```
Command:  00 02 00 0B 00
Response: [Exp_len:2B][Exponent][Mod_len:2B][Modulus]
```
- Format: Length-prefixed exponent + modulus
- Total ~135 bytes cho RSA-1024
- Không cần PIN verification

### 5. Update Balance (INS=03, P1=05, P2=0C)
```
Command:  00 03 05 0C 05 [Type:1B][Amount:4B]
Response: [New Balance: 4 bytes]
```
- Type: 0x01 = Top-up, 0x02 = Payment
- Amount: 32-bit integer (big-endian)
- Cần PIN verification
- Check insufficient funds cho payment

### 6. Sign Data (INS=03, P1=06, P2=00)
```
Command:  00 03 06 00 [Lc] [Data to sign...]
Response: [RSA Signature: ~128 bytes]
```
- RSA-SHA-PKCS1 signature
- Cần PIN verification
- Return signature length varies

## 🏗️ Implementation Notes

### Extended APDU Support
```java
public class citizen_applet extends Applet implements ExtendedLength {
    // Supports large data transfers for photos
}
```

### Error Codes
- `6985`: Security status not satisfied (PIN required)
- `6A86`: Wrong P1P2 parameters
- `6700`: Wrong length
- `6A84`: Not enough memory (photo too large)
- `6982`: Security status not satisfied (card blocked)

### Data Formats
- **Card ID**: 32-byte ASCII string "CITIZEN-" + 24 hex chars
- **PIN**: 4-byte ASCII digits
- **Balance**: 4-byte big-endian integer (VND)
- **Avatar**: Raw image bytes (JPEG/PNG)
- **Personal Info**: Encrypted JSON/text data

## 🔄 Typical Workflow

### Card Initialization
1. `INS=01, P1=04`: Initialize with PIN
2. `INS=02, P1=00, P2=0A`: Get Card ID
3. `INS=02, P1=00, P2=0B`: Get Public Key

### Daily Usage
1. `INS=00, P1=04`: Verify PIN
2. `INS=02, P1=00, P2=0C`: Check Balance
3. `INS=03, P1=05, P2=0C`: Make Payment/Top-up

### Photo Management
1. `INS=00, P1=04`: Verify PIN
2. `INS=01, P1=05, P2=09`: Upload Avatar (Extended APDU)
3. `INS=02, P1=05, P2=09`: Download Avatar

### Security Operations
1. `INS=03, P1=06`: Sign Data
2. `INS=03, P1=04`: Change PIN
3. `INS=03, P1=0C`: Deactivate Card

## 🚀 Migration from v1.0

### Key Changes
- **New INS structure**: More organized command set
- **Extended APDU**: Large photo support
- **MD5 PIN hashing**: Instead of SHA-256
- **Card activation**: Explicit activate/deactivate
- **Simplified photo**: Single avatar instead of chunked transfer

### Compatibility
- **Not backward compatible** with v1.0
- **CardService.java** needs complete rewrite
- **Database schema** may need updates for new Card ID format
- **Photo handling** simplified (no more chunking)

## 📋 Testing Commands

### Basic Test Sequence
```bash
# 1. Initialize card
00 01 04 00 04 31 32 33 34  # PIN = "1234"

# 2. Verify PIN
00 00 04 00 04 31 32 33 34

# 3. Get card info
00 02 00 0A 00              # Get Card ID
00 02 00 0B 00              # Get Public Key
00 02 00 0C 00              # Get Balance

# 4. Update balance (top-up 100000)
00 03 05 0C 05 01 00 01 86 A0

# 5. Make payment (50000)
00 03 05 0C 05 02 00 00 C3 50
```

---

**Note**: Đây là specification hoàn chỉnh cho Applet v2.0. CardService.java cần được cập nhật để tương thích với command structure mới này.