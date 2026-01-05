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
