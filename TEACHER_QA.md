# 🎓 Câu Hỏi & Trả Lời - Bảo Vệ Đồ Án Citizen Card

> Tài liệu chuẩn bị cho phần hỏi đáp với giáo viên

---

## 📋 Mục Lục

1. [Nhóm 1: PIN và RSA](#nhóm-1-pin-và-rsa)
2. [Nhóm 2: Mã hóa dữ liệu](#nhóm-2-mã-hóa-dữ-liệu)
3. [Nhóm 3: RSA và Public Key](#nhóm-3-rsa-và-public-key)
4. [Nhóm 4: PBKDF2 và Master Key](#nhóm-4-pbkdf2-và-master-key)
5. [Nhóm 5: Đổi PIN](#nhóm-5-đổi-pin)
6. [Nhóm 6: Luồng hoạt động](#nhóm-6-luồng-hoạt-động)
7. [Nhóm 7: Kỹ thuật khác](#nhóm-7-kỹ-thuật-khác)

---

## Nhóm 1: PIN và RSA

### Câu 1: Sự khác nhau giữa mã PIN vs ký xác thực RSA như nào?

| Mã PIN (PBKDF2) | Ký xác thực RSA |
|-----------------|-----------------|
| **Mục đích**: Xác thực người dùng với thẻ | **Mục đích**: Xác thực thẻ với server |
| Dùng để bảo vệ dữ liệu trên thẻ | Dùng để chứng minh thẻ là thật (không giả mạo) |
| Sinh ra PIN Key → giải mã Master Key | Ký challenge từ server → server verify chữ ký |
| **Symmetric** (đối xứng - cùng một key) | **Asymmetric** (bất đối xứng - public/private key) |
| Thuật toán: PBKDF2-HMAC-SHA1 + AES-128 | Thuật toán: RSA-1024 với SHA1withRSA |
| Nếu sai 5 lần → thẻ bị khóa | Không giới hạn số lần verify |

---

### Câu 2: Xác thực mã PIN để làm gì?

**Mục đích của xác thực PIN:**
1. **Mở khóa thẻ**: Cho phép truy cập dữ liệu sau khi verify thành công
2. **Sinh PIN Key**: `PBKDF2(PIN, cardId)` → PIN Key (16 bytes)
3. **Giải mã Master Key**: `AES_DECRYPT(encryptedMasterKey, pinKey)` → Master Key
4. **Cho phép thao tác**: Đặt `pinVerified = true` → có thể đọc/ghi Balance, Info, Avatar

---

### Câu 3: Xác thực mã PIN khác xác thực RSA như nào?

| Tiêu chí | Xác thực PIN | Xác thực RSA |
|----------|--------------|--------------|
| **Hướng xác thực** | User → Thẻ | Thẻ → Server |
| **Dữ liệu đầu vào** | PIN 4 số | Challenge ngẫu nhiên |
| **Kết quả** | `pinVerified = true` | Signature 128 bytes |
| **Kiểm tra bởi** | Thẻ (on-card) | Server (off-card) |
| **Sử dụng cho** | Truy cập dữ liệu thẻ | Xác minh thẻ hợp lệ |
| **Giới hạn** | 5 lần sai → khóa thẻ | Không giới hạn |

---

### Câu 4: Có dùng để xác thực trong giao dịch không?

**CÓ!** PIN được xác thực trước **MỌI** giao dịch:

```java
// Trong mọi hàm xử lý giao dịch
if (!pinVerified) {
    ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED); // 6982
}
```

**Các giao dịch yêu cầu PIN:**
- Nạp tiền (Topup)
- Thanh toán (Payment)
- Đọc/Ghi thông tin cá nhân
- Đọc/Ghi avatar
- Đổi PIN

---

## Nhóm 2: Mã hóa dữ liệu

### Câu 5: Mã hoá từng trường vs chuỗi khác nhau như nào?

**Trong project này**: Mỗi trường dữ liệu được mã hóa **RIÊNG BIỆT**:

| Trường | Plain Size | Padded Size | Format |
|--------|------------|-------------|--------|
| `encryptedBalance` | 4 bytes | 16 bytes | `[balance:4][padding:12]` |
| `encryptedInfo` | Max 512B | Max 528B | `[info:N][padding]` |
| `avatar` | Max 15KB | Max 15KB+ | `[image:N][padding]` |
| `encryptedMasterKey` | 16 bytes | 16 bytes | `[masterKey:16]` |

**Tại sao mã hóa riêng biệt?**
- ✅ Đọc/ghi từng trường **độc lập** (lấy balance không cần giải mã avatar)
- ✅ Tiết kiệm bộ nhớ và thời gian
- ✅ Dễ debug và bảo trì

---

### Câu 6: Khi nhập thông tin thì mã hóa ở đâu? Host hay thẻ?

**Mã hóa 100% TRÊN THẺ (On-Card)!**

```
Desktop App                    Smart Card
    │                              │
    │  Gửi plaintext               │
    │ ─────────────────────────────▶
    │                              │ 1. Nhận plaintext
    │                              │ 2. AES_ENCRYPT(data, MasterKey)
    │                              │ 3. Lưu encryptedData vào EEPROM
    │                              │
    │  Nhận status (9000)          │
    │ ◀─────────────────────────────
```

**Lý do:**
- Master Key **KHÔNG BAO GIỜ** rời khỏi thẻ
- Desktop chỉ gửi/nhận plaintext
- Chip bảo mật xử lý toàn bộ crypto

---

### Câu 7: Có mã hóa không?

**CÓ!** Tất cả dữ liệu nhạy cảm đều được mã hóa **AES-128 ECB**:

| Dữ liệu | Trạng thái trên thẻ |
|---------|---------------------|
| Balance | ✅ Encrypted |
| Info | ✅ Encrypted |
| Avatar | ✅ Encrypted |
| Master Key | ✅ Encrypted (bởi PIN Key) |
| Card ID | ❌ Plain (dùng làm salt) |

---

### Câu 8: Mỗi lần nhét thẻ vào là 1 lần xác thực RSA, mã hóa như nào để xác thực được?

**Challenge-Response Protocol:**

```
Bước 1: Server sinh random challenge (16-32 bytes)
         └─▶ challenge = SecureRandom.nextBytes(32)

Bước 2: Gửi challenge lên thẻ qua APDU
         └─▶ CLA=0x00, INS=0x01, P1=0x06 + [challenge]

Bước 3: Thẻ ký bằng Private Key
         └─▶ rsaSignature.init(privateKey, MODE_SIGN)
         └─▶ signature = rsaSignature.sign(challenge) // 128 bytes

Bước 4: Desktop nhận signature, verify bằng Public Key
         └─▶ RSAUtils.verifySignature(signature, publicKey, challenge)

Bước 5: Nếu verify = true → Thẻ hợp lệ!
```

---

## Nhóm 3: RSA và Public Key

### Câu 9: RSA đẩy lên CSDL như nào?

**Format serialize Public Key:**
```
[expLen:2 bytes][exponent:3 bytes][modLen:2 bytes][modulus:128 bytes]
= Tổng: 135 bytes
```

**Luồng xử lý:**
1. Applet gọi `serializePublicKey()` → tạo 135 bytes
2. Desktop nhận qua APDU `CREATE_INIT`
3. Desktop lưu vào DB dưới dạng **hex string** hoặc **Base64**

```java
// Lưu vào database
String publicKeyHex = RSAUtils.bytesToHex(publicKeyBytes);
cardDAO.saveCard(cardId, publicKeyHex);
```

---

### Câu 10: Cho cô xem cách public lên server (Public Key)

**Code trên Applet - serializePublicKey():**
```java
private short serializePublicKey(byte[] buffer, short offset) {
    short pos = offset;
    
    // 1. Ghi exponent length (2 bytes) + exponent data
    short expLen = rsaPublicKey.getExponent(buffer, (short)(pos + 2));
    Util.setShort(buffer, pos, expLen);
    pos += (short)(2 + expLen);  // 2 + 3 = 5 bytes
    
    // 2. Ghi modulus length (2 bytes) + modulus data
    short modLen = rsaPublicKey.getModulus(buffer, (short)(pos + 2));
    Util.setShort(buffer, pos, modLen);
    pos += (short)(2 + modLen);  // 2 + 128 = 130 bytes
    
    return (short)(pos - offset);  // Total: 135 bytes
}
```

**Code trên Desktop - Parse và lưu:**
```java
// 1. Nhận từ thẻ
byte[] publicKeyData = cardService.getPublicKey();

// 2. Parse thành PublicKey object
PublicKey publicKey = RSAUtils.generatePublicKeyFromBytes(publicKeyData);

// 3. Lưu vào database
String keyHex = RSAUtils.bytesToHex(publicKeyData);
database.savePublicKey(cardId, keyHex);
```

---

### Câu 11: Dùng gì để ghép public key?

Dùng **RSAPublicKeySpec** trong Java:

```java
// RSAUtils.generatePublicKeyFromBytes()
public static PublicKey generatePublicKeyFromBytes(byte[] data) {
    // 1. Parse exponent
    int expLen = ((data[0] & 0xFF) << 8) | (data[1] & 0xFF);
    byte[] expBytes = new byte[expLen];
    System.arraycopy(data, 2, expBytes, 0, expLen);
    
    // 2. Parse modulus
    int modOffset = 2 + expLen;
    int modLen = ((data[modOffset] & 0xFF) << 8) | (data[modOffset + 1] & 0xFF);
    byte[] modBytes = new byte[modLen];
    System.arraycopy(data, modOffset + 2, modBytes, 0, modLen);
    
    // 3. Tạo BigInteger
    BigInteger exponent = new BigInteger(1, expBytes);
    BigInteger modulus = new BigInteger(1, modBytes);
    
    // 4. Tạo PublicKey
    RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
    return keyFactory.generatePublic(spec);
}
```

---

## Nhóm 4: PBKDF2 và Master Key

### Câu 12: Mỗi lần đăng nhập thì master key giống nhau hay khác nhau? Lưu dạng bản rõ hay gì?

| Câu hỏi | Trả lời |
|---------|---------|
| Master Key giống nhau không? | **GIỐNG NHAU** qua tất cả các lần đăng nhập |
| Sinh khi nào? | **Một lần duy nhất** khi khởi tạo thẻ |
| Lưu dạng gì? | **ĐÃ MÃ HÓA** bởi PIN Key (`encryptedMasterKey`) |
| Khi verify PIN xong? | Giải mã → lấy Master Key plaintext vào **RAM** |
| Khi ngắt kết nối? | RAM bị clear → phải verify PIN lại |

```java
// Khi khởi tạo thẻ
randomData.generateData(tempBuffer, (short)0, (short)16);  // Sinh Master Key ngẫu nhiên
aesCipher.init(pinKey, Cipher.MODE_ENCRYPT);
aesCipher.doFinal(tempBuffer, 0, 16, encryptedMasterKey, 0);  // Mã hóa bằng PIN Key
```

---

### Câu 13: Salt sinh như nào? DerivedKey dùng như nào?

**Salt = Card ID (UUID)**

```java
// Salt được truyền vào khi khởi tạo thẻ
cardId = new byte[40];  // Max 40 bytes
Util.arrayCopy(buffer, offset, cardId, 0, cardIdLen);
```

**DerivedKey (PIN Key) dùng để:**
1. **So sánh khi verify PIN:**
   ```java
   derivePinKey(inputPIN) → tempPinKey
   if (Util.arrayCompare(pinKey, tempPinKey) == 0) → PIN đúng
   ```

2. **Mã hóa/Giải mã Master Key:**
   ```java
   // Giải mã
   aesCipher.init(pinKey, MODE_DECRYPT);
   aesCipher.doFinal(encryptedMasterKey) → masterKey
   ```

---

### Câu 14: Số vòng lặp là bao nhiêu?

**Số vòng lặp = 1000** (không phải 10000 hay 100000)

```java
private static final short PBKDF2_ITERATIONS = 1000;
```

**Lý do chọn 1000:**
- ⚡ JavaCard có CPU chậm, RAM hạn chế
- ⏱️ 1000 iterations ≈ 100ms delay mỗi lần thử
- 🔒 5 lần thử × 100ms = đủ chống brute-force
- 📊 Xác suất đoán đúng: 5/10000 = 0.05%

---

## Nhóm 5: Đổi PIN

### Câu 15: Trong trường hợp đổi mã PIN người dùng thì sẽ như nào?

**Quy trình đổi PIN (5 bước):**

```
Bước 1: Xác thực PIN cũ
         PBKDF2(oldPIN, cardId) → oldPinKey
         So sánh với pinKey đã lưu ✓

Bước 2: Giải mã Master Key
         AES_DECRYPT(encryptedMasterKey, oldPinKey) → masterKey (plaintext)

Bước 3: Sinh PIN Key mới
         PBKDF2(newPIN, cardId) → newPinKey

Bước 4: Mã hóa lại Master Key
         AES_ENCRYPT(masterKey, newPinKey) → encryptedMasterKey (mới)

Bước 5: Cập nhật pinKey = newPinKey
```

> **QUAN TRỌNG**: Dữ liệu (Balance, Info, Avatar) **KHÔNG CẦN** mã hóa lại vì Master Key **KHÔNG ĐỔI**!

---

## Nhóm 6: Luồng hoạt động

### Câu 16: Nạp tiền như nào?

**Luồng nạp tiền (Topup):**

```
1. User nhập số tiền trên Desktop App
   └─▶ amount = 100000 VND

2. Gửi APDU lên thẻ
   └─▶ CLA=0x00, INS=0x01, P1=0x02, P2=0x01 + [amount:4 bytes]

3. Applet xử lý:
   a. Kiểm tra pinVerified == true
   b. Giải mã: AES_DECRYPT(encryptedBalance, masterKey) → currentBalance
   c. Tính: newBalance = currentBalance + amount
   d. Mã hóa: AES_ENCRYPT(newBalance, masterKey) → encryptedBalance
   e. Trả về newBalance

4. Desktop hiển thị số dư mới
```

---

### Câu 17: Lưu biến nào?

**Các biến quan trọng trên thẻ:**

| Biến | Mô tả | Mã hóa? | Vùng nhớ |
|------|-------|---------|----------|
| `cardId` | UUID định danh thẻ | ❌ | EEPROM |
| `pinKey` | Khóa sinh từ PIN | ❌ | EEPROM |
| `encryptedMasterKey` | Master Key đã mã hóa | ✅ AES | EEPROM |
| `encryptedBalance` | Số dư | ✅ AES | EEPROM |
| `encryptedInfo` | Thông tin cá nhân | ✅ AES | EEPROM |
| `avatar` | Ảnh đại diện | ✅ AES | EEPROM |
| `rsaPrivateKey` | Khóa riêng RSA | ✅ Key Object | EEPROM |
| `rsaPublicKey` | Khóa công khai RSA | ❌ | EEPROM |
| `pinVerified` | Trạng thái xác thực | ❌ | RAM |
| `pinTryCounter` | Số lần thử còn lại | ❌ | EEPROM |

---

## Nhóm 7: Kỹ thuật khác

### Câu 18: Có dùng APDU mở rộng không?

**KHÔNG** sử dụng Extended APDU. Dùng **Chunked Transfer**:

```java
// Avatar upload: chia thành nhiều chunk ~200 bytes
for (int offset = 0; offset < avatarData.length; offset += CHUNK_SIZE) {
    byte[] chunk = Arrays.copyOfRange(avatarData, offset, offset + CHUNK_SIZE);
    sendAPDU(CREATE_AVATAR, [totalLen:2][offset:2][chunk]);
}
```

**Lý do:**
- Nhiều card reader không hỗ trợ Extended APDU
- Standard APDU giới hạn 255 bytes
- Chunked transfer đảm bảo tương thích

---

### Câu 19: Thay đổi cái gì trong bài thực hành 3, 4?

**Các thay đổi chính so với bài thực hành gốc:**

| # | Thay đổi | Bài gốc | Project này |
|---|----------|---------|-------------|
| 1 | Sinh khóa từ PIN | MD5 hash trực tiếp | PBKDF2-HMAC-SHA1 (1000 iterations) |
| 2 | Kiến trúc khóa | 1 lớp (PIN = Key) | 2 lớp (PIN Key + Master Key) |
| 3 | Xác thực thẻ | Không có | RSA-1024 Digital Signature |
| 4 | Salt | Không có | Card ID làm salt |
| 5 | Đổi PIN | Re-encrypt toàn bộ data | Chỉ re-encrypt Master Key |

---

## 📝 Tóm Tắt Nhanh

```
┌─────────────────────────────────────────────────────────────┐
│                    CITIZEN CARD SECURITY                     │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  PIN (4 số) ──PBKDF2──▶ PIN Key ──AES──▶ Master Key         │
│                          (1000 iters)      │                 │
│                                            │                 │
│                                            ▼                 │
│                          ┌────────────────────────────┐     │
│                          │ AES-128 ECB                │     │
│                          │ ├── encryptedBalance       │     │
│                          │ ├── encryptedInfo          │     │
│                          │ └── avatar                 │     │
│                          └────────────────────────────┘     │
│                                                              │
│  RSA-1024: Private Key (thẻ) ◀──verify──▶ Public Key (DB)  │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

---

> 📅 **Ngày tạo**: 2026-01-05
> 
> 📚 **Tài liệu tham khảo**: `SECURITY_MECHANISM.md`, `ENCRYPTION_GUIDE.md`

---

# 📖 PHẦN MỞ RỘNG - CÂU HỎI TƯƠNG TỰ

---

## Nhóm A: Về Thuật Toán Mã Hóa

### Câu A1: Tại sao dùng AES-128 mà không dùng AES-256?

**Lý do:**
- JavaCard có **tài nguyên hạn chế** (RAM ~2KB, EEPROM ~32KB)
- AES-128 đã đủ mạnh (2^128 khả năng = không thể brute-force)
- AES-256 chậm hơn và tốn RAM hơn
- JavaCard API chỉ hỗ trợ tốt AES-128

---

### Câu A2: Tại sao dùng ECB mode mà không dùng CBC?

**ECB (Electronic Codebook):**
- ✅ Đơn giản, không cần IV (Initialization Vector)
- ✅ Phù hợp cho dữ liệu nhỏ (balance 4 bytes, master key 16 bytes)
- ⚠️ Nhược điểm: Cùng plaintext → cùng ciphertext

**CBC (Cipher Block Chaining):**
- Cần quản lý IV → phức tạp hơn
- Tốn thêm 16 bytes lưu IV

**Kết luận:** ECB chấp nhận được vì dữ liệu mỗi block là unique (balance thay đổi, info khác nhau).

---

### Câu A3: HMAC-SHA1 là gì? Tại sao dùng trong PBKDF2?

**HMAC-SHA1** = Hash-based Message Authentication Code với SHA-1

```
HMAC(K, m) = SHA1((K ⊕ opad) || SHA1((K ⊕ ipad) || m))
```

**Vai trò trong PBKDF2:**
- Là **PRF** (Pseudo-Random Function) được dùng trong từng vòng lặp
- Đảm bảo output không thể đoán ngược
- Kết hợp password và salt an toàn

---

### Câu A4: Tại sao RSA-1024 mà không dùng RSA-2048?

| RSA-1024 | RSA-2048 |
|----------|----------|
| Key size: 128 bytes | Key size: 256 bytes |
| Signature: 128 bytes | Signature: 256 bytes |
| Nhanh hơn trên JavaCard | Chậm gấp 4-8 lần |
| Đủ an toàn cho smart card | Quá tốn tài nguyên |

**Kết luận:** RSA-1024 là cân bằng giữa bảo mật và hiệu năng trên JavaCard.

---

## Nhóm B: Về APDU Commands

### Câu B1: Cấu trúc APDU Command như thế nào?

```
┌─────┬─────┬─────┬─────┬─────┬──────────┬─────┐
│ CLA │ INS │ P1  │ P2  │ Lc  │   Data   │ Le  │
│ 1B  │ 1B  │ 1B  │ 1B  │ 1B  │  0-255B  │ 1B  │
└─────┴─────┴─────┴─────┴─────┴──────────┴─────┘
```

| Field | Mô tả | Giá trị trong project |
|-------|-------|----------------------|
| CLA | Class byte | 0x00 |
| INS | Instruction | 0x01 |
| P1 | Parameter 1 | Function type |
| P2 | Parameter 2 | Sub-function |
| Lc | Data length | Độ dài data |
| Data | Payload | Dữ liệu gửi đi |
| Le | Expected length | Độ dài mong đợi |

---

### Câu B2: Các lệnh APDU trong project là gì?

| P1 | Chức năng | Data gửi đi |
|----|-----------|-------------|
| 0x01 | VERIFY_PIN | PIN (4 bytes) |
| 0x02 | UPDATE_BALANCE | Type + Amount |
| 0x03 | GET_BALANCE | (không có) |
| 0x04 | UPDATE_INFO | Info data |
| 0x05 | GET_INFO | (không có) |
| 0x06 | CREATE_SIGNATURE | Challenge |
| 0x07 | UPDATE_PIN | OldPIN + NewPIN |
| 0x08 | CREATE_AVATAR | Chunk data |
| 0x09 | GET_AVATAR | Offset |
| 0x0A | CREATE_INIT | PIN + CardID |

---

### Câu B3: Status Word (SW) trả về nghĩa là gì?

| SW | Hex | Ý nghĩa |
|----|-----|---------|
| 9000 | OK | Thành công |
| 6982 | Security | Chưa xác thực PIN |
| 6985 | Conditions | Điều kiện không thỏa |
| 6700 | Wrong Length | Độ dài sai |
| 6A86 | Wrong P1P2 | Tham số sai |
| 6D00 | INS Not Supported | Lệnh không hỗ trợ |

---

## Nhóm C: Về Bảo Mật

### Câu C1: Làm sao chống brute-force PIN?

**3 lớp bảo vệ:**

1. **PBKDF2 với 1000 iterations:**
   - Mỗi lần thử PIN tốn ~100ms
   - Không thể thử nhanh

2. **Giới hạn 5 lần thử:**
   ```java
   if (pinTryCounter <= 0) {
       cardActive = false;  // Khóa thẻ vĩnh viễn
   }
   ```

3. **Salt = Card ID:**
   - Mỗi thẻ có PIN Key khác nhau dù cùng PIN
   - Rainbow table attack không hiệu quả

---

### Câu C2: Nếu mất thẻ thì dữ liệu có bị lộ không?

**KHÔNG!** Vì:

1. Dữ liệu đều **đã mã hóa AES-128**
2. Không có PIN → không có PIN Key → không giải mã được Master Key
3. Sau 5 lần thử sai → thẻ bị khóa vĩnh viễn
4. Private Key RSA **không thể export** ra ngoài

---

### Câu C3: Làm sao chống thẻ giả mạo?

**Challenge-Response với RSA:**

```
1. Server sinh random challenge
2. Gửi challenge → Thẻ
3. Thẻ ký bằng Private Key → Signature
4. Server verify bằng Public Key (lưu trong DB)
5. Nếu verify thất bại → Thẻ giả!
```

**Tại sao không thể giả mạo:**
- Private Key sinh và lưu **bên trong chip**
- Không có API nào để export Private Key
- Không thể tạo signature hợp lệ mà không có Private Key

---

### Câu C4: Dữ liệu truyền giữa Desktop và Thẻ có mã hóa không?

**Hiện tại: KHÔNG** (plaintext qua APDU)

**Lý do:**
- Khoảng cách vật lý rất ngắn (thẻ cắm vào reader)
- Khó bị sniff như network
- Thẻ đã mã hóa dữ liệu bên trong

**Nếu cần bảo mật hơn:**
- Có thể thêm Secure Messaging (SM)
- Mã hóa APDU data bằng session key

---

## Nhóm D: Về Luồng Dữ Liệu

### Câu D1: Thanh toán (Payment) hoạt động như thế nào?

```
1. User chọn thanh toán, nhập số tiền
   └─▶ amount = 50000

2. Desktop kiểm tra số dư
   └─▶ getBalance() → currentBalance

3. Nếu đủ tiền:
   └─▶ sendAPDU(UPDATE_BALANCE, type=0x02, amount)

4. Applet xử lý:
   a. Decrypt encryptedBalance → balance
   b. newBalance = balance - amount
   c. Kiểm tra newBalance >= 0
   d. Encrypt newBalance → encryptedBalance
   e. Return newBalance

5. Desktop cập nhật UI
```

---

### Câu D2: Avatar lớn 15KB thì truyền như thế nào?

**Chunked Transfer Protocol:**

```java
// Desktop: Chia avatar thành chunks
int CHUNK_SIZE = 200;  // ~200 bytes mỗi chunk

for (int offset = 0; offset < avatarBytes.length; offset += CHUNK_SIZE) {
    byte[] chunk = Arrays.copyOfRange(avatarBytes, offset, 
                                       Math.min(offset + CHUNK_SIZE, avatarBytes.length));
    
    // Gửi APDU với [totalLen:2][offset:2][chunkData]
    byte[] apdu = buildAvatarAPDU(totalLen, offset, chunk);
    cardService.sendAPDU(apdu);
}
```

**Applet: Nhận và ghép chunks**
```java
// Copy chunk vào buffer
Util.arrayCopy(buffer, dataOffset, avatarBuffer, offset, chunkLen);

// Nếu là chunk cuối → mã hóa toàn bộ
if (isLastChunk) {
    aesCipher.doFinal(avatarBuffer, 0, paddedLen, avatar, 0);
}
```

---

### Câu D3: Khi đọc thông tin cá nhân thì luồng như thế nào?

```
Desktop                          Smart Card
   │                                  │
   │ 1. GET_INFO APDU                 │
   │ ────────────────────────────────▶│
   │                                  │ 2. Check pinVerified
   │                                  │ 3. AES_DECRYPT(encryptedInfo)
   │                                  │ 4. Remove padding
   │                                  │
   │ 5. Plaintext info (512 bytes)    │
   │ ◀────────────────────────────────│
   │                                  │
   │ 6. Parse JSON, hiển thị UI       │
```

---

## Nhóm E: Về Implementation

### Câu E1: RandomData sinh Master Key như thế nào?

```java
// Trên JavaCard
RandomData randomData = RandomData.getInstance(RandomData.ALG_SECURE_RANDOM);

// Sinh 16 bytes ngẫu nhiên
randomData.generateData(tempBuffer, (short)0, (short)16);
```

**ALG_SECURE_RANDOM:**
- Sử dụng hardware random number generator
- Không thể đoán trước (unpredictable)
- Phù hợp cho sinh khóa mật mã

---

### Câu E2: Transient vs Persistent memory trên JavaCard?

| Transient (RAM) | Persistent (EEPROM) |
|-----------------|---------------------|
| Mất khi ngắt điện | Giữ vĩnh viễn |
| Nhanh | Chậm hơn |
| ~2KB | ~32KB |
| `pinVerified`, `tempBuffer` | `encryptedBalance`, `pin` |

```java
// Transient array - clear khi reset
tempBuffer = JCSystem.makeTransientByteArray((short)256, JCSystem.CLEAR_ON_RESET);

// Persistent array - lưu trữ lâu dài
encryptedBalance = new byte[16];
```

---

### Câu E3: Serialize Info như thế nào?

**Format JSON:**
```json
{
  "fullName": "Nguyen Van A",
  "dateOfBirth": "1990-01-15",
  "address": "123 ABC Street",
  "idNumber": "0123456789"
}
```

**Xử lý trên Desktop:**
```java
// Serialize
String json = gson.toJson(citizenInfo);
byte[] data = json.getBytes(StandardCharsets.UTF_8);
cardService.updateInfo(data);

// Deserialize
byte[] data = cardService.getInfo();
String json = new String(data, StandardCharsets.UTF_8);
CitizenInfo info = gson.fromJson(json, CitizenInfo.class);
```

---

## Nhóm F: Câu Hỏi Tình Huống

### Câu F1: Nếu quên PIN thì làm thế nào?

**Không có cách khôi phục!** Vì:
- PIN không lưu dạng plaintext
- Không có "Forgot PIN" mechanism
- Đây là by design để bảo mật

**Giải pháp:**
- Phải phát hành thẻ mới
- Admin khởi tạo lại từ đầu

---

### Câu F2: Nếu thẻ bị khóa (5 lần sai) thì sao?

**Thẻ bị khóa vĩnh viễn:**
```java
cardActive = false;  // Không thể restore
```

**Xử lý:**
- Cần phát hành thẻ mới
- Dữ liệu cũ mất hoàn toàn (không thể recover)

---

### Câu F3: Điều gì xảy ra nếu ngắt điện giữa chừng khi đổi PIN?

**Transaction Protection:**
```java
JCSystem.beginTransaction();
try {
    // 1. Giải mã Master Key bằng old PIN Key
    // 2. Sinh new PIN Key
    // 3. Mã hóa lại Master Key
    // 4. Cập nhật pinKey
    JCSystem.commitTransaction();
} catch (Exception e) {
    JCSystem.abortTransaction();  // Rollback về trạng thái cũ
}
```

**Kết quả:** Nếu ngắt điện → rollback → dữ liệu không bị corrupt.

---

### Câu F4: Server bị hack, Public Key bị thay đổi thì sao?

**Hậu quả:**
- Thẻ thật sẽ **không verify được** (signature không khớp với fake public key)
- Kẻ tấn công **không thể tạo thẻ giả** vì không có Private Key

**Phòng chống:**
- Backup Public Key
- Checksum/Hash để detect tampering
- Audit log cho database changes

---

## 📊 Bảng Tổng Hợp Nhanh

| Thành phần | Thuật toán | Key Size | Mục đích |
|------------|------------|----------|----------|
| PIN → PIN Key | PBKDF2-HMAC-SHA1 | 128 bit | Sinh khóa từ mật khẩu |
| Mã hóa dữ liệu | AES-128 ECB | 128 bit | Mã hóa Balance, Info, Avatar |
| Mã hóa Master Key | AES-128 ECB | 128 bit | Bảo vệ Master Key |
| Xác thực thẻ | RSA-1024 | 1024 bit | Chữ ký số |
| Hash trong PBKDF2 | SHA-1 | 160 bit | PRF function |

---

> 📅 **Cập nhật**: 2026-01-05
> 
> 💡 **Tip**: Nắm vững sơ đồ luồng và các con số (1000 iterations, 5 lần thử, 16 bytes key...)
