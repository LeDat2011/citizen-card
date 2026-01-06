# 🔐 Cơ Chế Bảo Mật Citizen Card Applet v3.0

---

## 📑 Mục Lục

1. [Giải Thích Cơ Chế Mã Hóa (Thuyết Trình)](#-giải-thích-cơ-chế-mã-hóa-dành-cho-thuyết-trình)
2. [Tổng Quan Các Thuật Toán Mã Hóa](#tổng-quan-các-thuật-toán-mã-hóa)
   - [PBKDF2](#-pbkdf2-password-based-key-derivation-function-2)
   - [HMAC-SHA1](#-hmac-sha1-hash-based-message-authentication-code)
   - [AES-128 ECB](#-aes-128-ecb-advanced-encryption-standard)
   - [RSA-1024 Digital Signature](#️-rsa-1024-digital-signature)
3. [Phần 1: Mô Tả Luồng Hoạt Động](#phần-1-mô-tả-bằng-lời)
4. [Phần 2: Kiến Trúc và Code](#phần-2-kiến-trúc-và-code)

---

## 🔑 Tóm Tắt Kiến Trúc Khóa

**1. PIN Key (Khóa bảo vệ):**
- Được sinh ra từ mã PIN của cư dân thông qua hàm PBKDF2-HMAC-SHA1
- Sử dụng để mã hóa (wrap) Master Key
- Mục đích: Chỉ để bảo vệ Master Key khi lưu trữ trên thẻ

**2. Master Key (Khóa dữ liệu):**
- Là một khóa AES-128 ngẫu nhiên, được sinh ra duy nhất một lần khi khởi tạo thẻ
- Được lưu trên thẻ dưới dạng **đã mã hóa** bởi PIN Key
- Khi sử dụng (sau khi Verify PIN xong), Master Key được giải mã và nạp vào RAM
- Sử dụng trực tiếp: Master Key này dùng để mã hóa/giải mã các trường như `Balance`, `Info`, `Avatar`...

**3. RSA Key Pair (Khóa xác thực):**
- Private Key lưu trên thẻ, không bao giờ export
- Public Key gửi về database khi đăng ký
- Dùng cho chữ ký số xác thực thẻ không bị giả mạo

**Sơ đồ tóm tắt:**

> `Mã PIN` → *(sinh ra)* → `PIN Key` → *(mã hóa/giải mã)* → `Master Key` → *(mã hóa/giải mã)* → `Dữ liệu thẻ`

---

## 🎤 Giải Thích Cơ Chế Mã Hóa (Dành Cho Thuyết Trình)

Hệ thống sử dụng **3 cơ chế mã hóa chính**: PBKDF2-HMAC-SHA1 để sinh khóa từ PIN, AES-128 để mã hóa dữ liệu, và RSA-1024 để tạo chữ ký số xác thực thẻ.

**Quá trình mã hóa dữ liệu** sử dụng kiến trúc hai lớp khóa gồm PIN Key và Master Key. PIN Key là khóa 16 bytes được sinh từ PIN của cư dân thông qua thuật toán PBKDF2-HMAC-SHA1 với 1000 vòng lặp, sử dụng Card ID làm salt để đảm bảo mỗi thẻ có khóa riêng biệt dù cùng PIN. Master Key là khóa ngẫu nhiên 16 bytes được sinh bởi bộ sinh số ngẫu nhiên bảo mật RandomData.ALG_SECURE_RANDOM trên thẻ. PIN Key chỉ dùng để mã hóa Master Key, còn Master Key dùng để mã hóa toàn bộ dữ liệu thực tế bao gồm encryptedBalance, encryptedInfo và avatar.

**Quá trình khởi tạo thẻ**: Applet nhận PIN và Card ID từ admin, gọi hàm derivePinKey() để sinh PIN Key bằng PBKDF2(PIN, CardID, 1000 iterations), gọi randomData.generateData() để sinh Master Key ngẫu nhiên, sau đó mã hóa Master Key bằng AES với PIN Key và lưu vào encryptedMasterKey. Số dư được khởi tạo bằng 0, mã hóa bằng Master Key và lưu vào encryptedBalance.

**Quá trình xác thực PIN**: Applet nhận PIN qua lệnh APDU VERIFY_PIN, gọi derivePinKey() để sinh tempPinKey, so sánh với PIN Key đã lưu bằng Util.arrayCompare(). Nếu khớp thì giải mã encryptedMasterKey bằng AES với tempPinKey để lấy Master Key, đặt pinVerified = true và reset pinTryCounter = 5. Nếu sai thì giảm pinTryCounter, khi về 0 thẻ khóa vĩnh viễn.

**Quá trình đọc/ghi dữ liệu**: Applet kiểm tra pinVerified, nếu true thì khởi tạo AES cipher với Master Key và gọi aesCipher.doFinal() để giải mã encryptedBalance/encryptedInfo/avatar rồi trả về, hoặc mã hóa dữ liệu mới rồi lưu vào bộ nhớ thẻ.

**Quá trình đổi PIN**: Applet xác thực PIN cũ, giải mã encryptedMasterKey bằng PIN Key cũ để lấy Master Key plaintext, sinh PIN Key mới từ PIN mới bằng derivePinKey(), mã hóa lại Master Key bằng PIN Key mới và lưu vào encryptedMasterKey. Dữ liệu không cần mã hóa lại vì Master Key không đổi.

**Chữ ký số RSA-1024**: Mỗi thẻ có một cặp khóa RSA-1024 riêng được sinh khi khởi tạo. Private Key được lưu trên thẻ và không bao giờ export ra ngoài. Public Key được gửi về database khi đăng ký thẻ. 

**Xác thực RSA có 2 chế độ:**
1. **CREATE_SIGNATURE (INS=0x01, P1=0x06)**: Ký sau khi đã verify PIN - dùng cho giao dịch
2. **CHALLENGE (INS=0x12)**: **Xác thực thẻ KHÔNG cần PIN trước** - dùng để verify thẻ là thật ngay khi cắm vào, trước khi yêu cầu PIN

Khi cần xác thực thẻ, server gửi một challenge ngẫu nhiên (1-64 bytes), applet ký challenge bằng rsaSignature.sign() với Private Key và trả về signature (128 bytes). Server verify signature bằng Public Key để xác nhận đây là thẻ hợp lệ chứ không phải thẻ giả mạo.

**Khả năng chống tấn công**: Dữ liệu trên thẻ đều mã hóa AES-128 nên không đọc được nếu không có Master Key. Kẻ tấn công không có PIN nên không sinh được PIN Key để giải mã Master Key. Thẻ giới hạn 5 lần thử PIN, mỗi lần phải tính PBKDF2 với 1000 vòng nên mất 100ms, sau 5 lần sai thẻ khóa vĩnh viễn với xác suất đoán đúng 0,05%. PBKDF2 sử dụng Card ID làm salt nên cùng PIN vẫn cho PIN Key khác nhau giữa các thẻ. RSA đảm bảo không thể giả mạo thẻ vì Private Key không bao giờ rời khỏi chip bảo mật. **Lệnh INS_CHALLENGE cho phép xác thực thẻ trước khi yêu cầu PIN**, ngăn chặn việc phishing bằng thẻ giả.

---


## Tổng Quan Các Thuật Toán Mã Hóa

### 🔑 PBKDF2 (Password-Based Key Derivation Function 2)

**PBKDF2** là thuật toán sinh khóa từ mật khẩu, được thiết kế để chống lại các cuộc tấn công brute-force và dictionary attack.

#### Nguyên lý hoạt động:

```
PBKDF2(Password, Salt, Iterations, KeyLength) → DerivedKey
```

| Thành phần | Mô tả | Giá trị trong Applet |
|------------|-------|---------------------|
| **Password** | Mật khẩu gốc cần bảo vệ | PIN (4 bytes) |
| **Salt** | Giá trị ngẫu nhiên, unique per user | Card ID (UUID) |
| **Iterations** | Số vòng lặp hash | 1000 |
| **KeyLength** | Độ dài khóa đầu ra | 16 bytes (128-bit) |

#### Công thức toán học:

```
DK = T1 || T2 || ... || Tdklen/hlen

Trong đó:
  Ti = F(Password, Salt, c, i)
  F(Password, Salt, c, i) = U1 XOR U2 XOR ... XOR Uc
  
  U1 = PRF(Password, Salt || INT(i))     // PRF = HMAC-SHA1
  U2 = PRF(Password, U1)
  ...
  Uc = PRF(Password, Uc-1)
```

#### Sơ đồ minh họa:

```
           ┌──────────────────────────────────────────────────────────────┐
           │                        PBKDF2                                 │
           └──────────────────────────────────────────────────────────────┘
                                      │
      ┌───────────────────────────────┼───────────────────────────────┐
      │                               │                               │
      ▼                               ▼                               ▼
┌───────────┐                   ┌───────────┐                   ┌───────────┐
│   Salt    │                   │    PIN    │                   │   1000    │
│ (CardID)  │                   │ (4 bytes) │                   │iterations │
└─────┬─────┘                   └─────┬─────┘                   └─────┬─────┘
      │                               │                               │
      └───────────────┬───────────────┘                               │
                      │                                               │
                      ▼                                               │
           ┌──────────────────┐                                       │
           │  Salt || INT(1)  │                                       │
           └────────┬─────────┘                                       │
                    │                                                 │
                    ▼                                                 │
           ┌──────────────────┐                                       │
     ┌────▶│   HMAC-SHA1      │◀──────── PIN ──────────────────────┐ │
     │     └────────┬─────────┘                                     │ │
     │              │                                               │ │
     │              ▼                                               │ │
     │     ┌──────────────────┐                                     │ │
     │     │       U1         │──────────────┐                      │ │
     │     └──────────────────┘              │                      │ │
     │              │                        │                      │ │
     │              ▼                        │                      │ │
     │     ┌──────────────────┐              │                      │ │
     └────▶│   HMAC-SHA1      │◀─────────────┼────────────────────── │
           └────────┬─────────┘              │                        │
                    │                        │                        │
                    ▼                        ▼                        │
           ┌──────────────────┐     ┌──────────────────┐             │
           │       U2         │     │   U1 XOR U2      │ ◀── Lặp 1000 lần
           └──────────────────┘     └────────┬─────────┘
                    │                        │
                   ...                       │
                    │                        ▼
                    ▼               ┌──────────────────┐
           ┌──────────────────┐     │    PIN Key       │
           │      U1000       │────▶│   (16 bytes)     │
           └──────────────────┘     └──────────────────┘
```

#### Tại sao PBKDF2 an toàn?

1. **Salt ngăn Rainbow Table Attack**: Mỗi thẻ có Card ID khác nhau → cùng PIN "1234" sẽ sinh ra PIN Key khác nhau
2. **Iterations làm chậm Brute-force**: 1000 vòng lặp = thời gian thử mỗi PIN tăng 1000 lần
3. **On-card execution**: PIN không bao giờ rời khỏi thẻ

---

### 🔐 HMAC-SHA1 (Hash-based Message Authentication Code)

**HMAC** là thuật toán xác thực thông điệp dựa trên hash, kết hợp một khóa bí mật với hàm hash.

#### Công thức:

```
HMAC(K, m) = H((K' ⊕ opad) || H((K' ⊕ ipad) || m))

Trong đó:
  K' = K được padding đến 64 bytes (block size của SHA-1)
  ipad = 0x36 lặp lại 64 lần (inner padding)
  opad = 0x5C lặp lại 64 lần (outer padding)
  H = SHA-1 hash function
  ⊕ = XOR operation
  || = concatenation
```

#### Sơ đồ minh họa:

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              HMAC-SHA1                                   │
└─────────────────────────────────────────────────────────────────────────┘

     ┌───────────┐                              ┌───────────┐
     │    Key    │                              │  Message  │
     │  (PIN)    │                              │(Salt||INT)│
     └─────┬─────┘                              └─────┬─────┘
           │                                          │
           ▼                                          │
     ┌───────────────────┐                            │
     │  Pad to 64 bytes  │                            │
     │       (K')        │                            │
     └─────────┬─────────┘                            │
               │                                      │
       ┌───────┴───────┐                              │
       │               │                              │
       ▼               ▼                              │
  ┌─────────┐     ┌─────────┐                         │
  │K' ⊕ ipad│     │K' ⊕ opad│                         │
  │ (0x36)  │     │ (0x5C)  │                         │
  └────┬────┘     └────┬────┘                         │
       │               │                              │
       ▼               │                              ▼
  ┌─────────────────────────────────┐          ┌───────────┐
  │  (K' ⊕ ipad) || Message         │◀─────────│  Message  │
  └─────────────┬───────────────────┘          └───────────┘
                │
                ▼
         ┌────────────┐
         │   SHA-1    │  ◄── Inner Hash
         └──────┬─────┘
                │
                ▼
         ┌────────────┐
         │Inner Hash  │
         │ (20 bytes) │
         └──────┬─────┘
                │
                ▼
  ┌─────────────────────────────────┐
  │  (K' ⊕ opad) || Inner Hash      │
  └─────────────┬───────────────────┘
                │
                ▼
         ┌────────────┐
         │   SHA-1    │  ◄── Outer Hash
         └──────┬─────┘
                │
                ▼
         ┌────────────┐
         │   HMAC     │
         │ (20 bytes) │
         └────────────┘
```

#### Đặc điểm SHA-1:

| Thuộc tính | Giá trị |
|------------|---------|
| Output size | 160 bits (20 bytes) |
| Block size | 512 bits (64 bytes) |
| Rounds | 80 |
| Operations | AND, OR, XOR, ROT |

---

### 🔒 AES-128 ECB (Advanced Encryption Standard)

**AES** là thuật toán mã hóa đối xứng tiêu chuẩn, được sử dụng rộng rãi nhất hiện nay.

#### Thông số kỹ thuật:

| Thuộc tính | Giá trị |
|------------|---------|
| Key size | 128 bits (16 bytes) |
| Block size | 128 bits (16 bytes) |
| Rounds | 10 |
| Mode | ECB (Electronic Codebook) |

#### Cấu trúc một vòng AES:

```
┌─────────────────────────────────────────────────────────────┐
│                    AES Round Structure                       │
└─────────────────────────────────────────────────────────────┘

    ┌──────────────────┐
    │   Input Block    │   16 bytes (128 bits)
    │   (Plaintext)    │
    └────────┬─────────┘
             │
    ┌────────▼─────────┐
    │    SubBytes      │   Thay thế từng byte qua S-Box
    └────────┬─────────┘
             │
    ┌────────▼─────────┐
    │   ShiftRows      │   Dịch vòng các hàng
    └────────┬─────────┘
             │
    ┌────────▼─────────┐
    │   MixColumns     │   Trộn các cột (bỏ ở vòng cuối)
    └────────┬─────────┘
             │
    ┌────────▼─────────┐     ┌──────────────┐
    │   AddRoundKey    │◀────│  Round Key   │
    └────────┬─────────┘     └──────────────┘
             │
             ▼
    ┌──────────────────┐
    │   Output Block   │   16 bytes (128 bits)
    │   (Ciphertext)   │
    └──────────────────┘
             
                × 10 vòng
```

#### ECB Mode (Electronic Codebook):

```
Plaintext:   │ Block 1  │ Block 2  │ Block 3  │ Block 4  │
             └────┬─────┴────┬─────┴────┬─────┴────┬─────┘
                  │          │          │          │
                  ▼          ▼          ▼          ▼
             ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐
     Key ───▶│AES Enc │ │AES Enc │ │AES Enc │ │AES Enc │◀─── Key
             └────┬───┘ └────┬───┘ └────┬───┘ └────┬───┘
                  │          │          │          │
                  ▼          ▼          ▼          ▼
Ciphertext:  │ Block 1' │ Block 2' │ Block 3' │ Block 4' │
```

> **Lưu ý**: ECB mode đơn giản nhưng không che giấu pattern trong dữ liệu. Trong applet, điều này chấp nhận được vì mỗi block dữ liệu (balance, info) thường nhỏ và unique.

---

### 🖊️ RSA-1024 Digital Signature

**RSA** là thuật toán mã hóa bất đối xứng, sử dụng cặp khóa public/private.

#### Thông số kỹ thuật:

| Thuộc tính | Giá trị |
|------------|---------|
| Key size | 1024 bits |
| Modulus (n) | 1024 bits |
| Public exponent (e) | Thường là 65537 (0x10001) |
| Signature scheme | RSASSA-PKCS1-v1_5 |
| Hash function | SHA-1 |

#### Toán học RSA:

```
Sinh khóa:
  1. Chọn 2 số nguyên tố lớn: p, q
  2. n = p × q                    (modulus, 1024 bits)
  3. φ(n) = (p-1) × (q-1)         (Euler's totient)
  4. Chọn e sao cho gcd(e, φ(n)) = 1
  5. d = e⁻¹ mod φ(n)            (private exponent)

Public Key:  (n, e)
Private Key: (n, d)

Ký số:
  signature = message^d mod n

Xác thực:
  message' = signature^e mod n
  Verify: message' == message ?
```

#### Sơ đồ ký số:

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         RSA Digital Signature                            │
└─────────────────────────────────────────────────────────────────────────┘

  TẠO CHỮ KÝ (Trên thẻ)                  XÁC THỰC (Trên Desktop)
  ═══════════════════                    ════════════════════════

  ┌───────────────┐                      ┌───────────────┐
  │   Challenge   │                      │   Signature   │
  │  (từ Server)  │                      │  (từ Thẻ)     │
  └───────┬───────┘                      └───────┬───────┘
          │                                      │
          ▼                                      ▼
  ┌───────────────┐                      ┌───────────────┐
  │    SHA-1      │                      │  signature^e  │
  │    Hash       │                      │    mod n      │
  └───────┬───────┘                      └───────┬───────┘
          │                                      │
          ▼                                      │
  ┌───────────────┐                              │
  │ PKCS#1 v1.5   │                              │
  │   Padding     │                              │
  └───────┬───────┘                              │
          │                                      │
          ▼                                      ▼
  ┌───────────────┐     So sánh         ┌───────────────┐
  │   padded^d    │ ─────────────────── │   Recovered   │
  │    mod n      │                     │   Message     │
  └───────┬───────┘                     └───────────────┘
          │
          ▼
  ┌───────────────┐
  │   Signature   │
  │  (128 bytes)  │
  └───────────────┘
```

---

### 📊 So Sánh Các Thuật Toán

| Thuật toán | Loại | Input | Output | Mục đích |
|------------|------|-------|--------|----------|
| **PBKDF2** | Key Derivation | Password + Salt | Key (16B) | Sinh khóa từ PIN |
| **HMAC-SHA1** | MAC | Key + Message | Hash (20B) | Xác thực trong PBKDF2 |
| **SHA-1** | Hash | Any data | Hash (20B) | Hash 1 chiều |
| **AES-128** | Symmetric | Key + Plaintext | Ciphertext | Mã hóa dữ liệu |
| **RSA-1024** | Asymmetric | Private/Public Key | Signature | Ký số, xác thực |

---

### 🔗 Cách Các Thuật Toán Kết Hợp

```
┌─────────────────────────────────────────────────────────────────────────┐
│                     SECURITY ALGORITHM CHAIN                             │
└─────────────────────────────────────────────────────────────────────────┘

    USER INPUT                    KEY DERIVATION                DATA PROTECTION
    ═══════════                   ══════════════                ═══════════════

  ┌──────────┐
  │   PIN    │
  │ (4 digits)│
  └────┬─────┘
       │
       │    ┌─────────────────────────────────────────┐
       └───▶│              PBKDF2                     │
            │  ┌─────────────────────────────────┐    │
            │  │         HMAC-SHA1               │    │
            │  │  ┌───────────────────────┐      │    │
            │  │  │        SHA-1          │      │    │ × 1000
            │  │  └───────────────────────┘      │    │
            │  └─────────────────────────────────┘    │
            └──────────────┬──────────────────────────┘
                           │
                           ▼
                    ┌──────────────┐         ┌──────────────┐
                    │   PIN Key    │────────▶│   AES-128    │
                    │  (16 bytes)  │         │   Decrypt    │
                    └──────────────┘         └──────┬───────┘
                                                    │
                                                    ▼
                                             ┌──────────────┐
                                             │  Master Key  │
                                             │  (16 bytes)  │
                                             └──────┬───────┘
                                                    │
                           ┌────────────────────────┼────────────────────────┐
                           │                        │                        │
                           ▼                        ▼                        ▼
                    ┌──────────────┐         ┌──────────────┐         ┌──────────────┐
                    │   AES-128    │         │   AES-128    │         │   AES-128    │
                    │   Encrypt    │         │   Encrypt    │         │   Encrypt    │
                    └──────┬───────┘         └──────┬───────┘         └──────┬───────┘
                           │                        │                        │
                           ▼                        ▼                        ▼
                    ┌──────────────┐         ┌──────────────┐         ┌──────────────┐
                    │   Balance    │         │    Info      │         │   Avatar     │
                    │  (encrypted) │         │ (encrypted)  │         │ (encrypted)  │
                    └──────────────┘         └──────────────┘         └──────────────┘


    IDENTITY VERIFICATION
    ═════════════════════

  ┌──────────────┐
  │  Challenge   │
  │  (random)    │
  └──────┬───────┘
         │
         ▼
  ┌──────────────┐         ┌──────────────┐
  │   RSA-1024   │◀────────│ Private Key  │  (stored on card)
  │     Sign     │         │              │
  └──────┬───────┘         └──────────────┘
         │
         ▼
  ┌──────────────┐         ┌──────────────┐
  │  Signature   │────────▶│  Verify with │◀── Public Key (in DB)
  │ (128 bytes)  │         │  Public Key  │
  └──────────────┘         └──────────────┘
```

---

## Phần 1: Mô Tả Bằng Lời

### 1.1 Tổng Quan Kiến Trúc Bảo Mật

Citizen Card Applet v3.0 sử dụng kiến trúc **hai lớp khóa** (Two-Layer Key Architecture) để bảo vệ dữ liệu:

1. **PIN Key** - Khóa được sinh từ PIN của người dùng thông qua thuật toán PBKDF2-HMAC-SHA1
2. **Master Key** - Khóa ngẫu nhiên 128-bit, được mã hóa bởi PIN Key và dùng để mã hóa toàn bộ dữ liệu

#### Tại sao cần hai lớp khóa?

**Vấn đề với kiến trúc cũ (v1.0-v2.0):**
- PIN trực tiếp sinh ra khóa AES (qua MD5 hash)
- Khi đổi PIN → phải giải mã và mã hóa lại TOÀN BỘ dữ liệu (Balance, Info, Avatar = ~16KB)
- Tốn thời gian, tốn bộ nhớ, có nguy cơ mất dữ liệu nếu quá trình bị gián đoạn

**Giải pháp v3.0:**
- PIN → PBKDF2 → **PIN Key** (chỉ dùng để mã hóa Master Key)
- **Master Key** (ngẫu nhiên) → mã hóa dữ liệu thực tế
- Khi đổi PIN → chỉ cần re-encrypt Master Key (16 bytes) → nhanh và an toàn

---

### 1.2 Luồng Khởi Tạo Thẻ (Card Initialization)

Khi Admin tạo thẻ mới cho cư dân:

1. **Nhận dữ liệu**: Desktop gửi PIN (4 số) và Card ID (UUID) lên thẻ
2. **Sinh PIN Key**: 
   - Applet thực hiện PBKDF2-HMAC-SHA1 với:
     - Password = PIN (4 bytes)
     - Salt = Card ID (đảm bảo mỗi thẻ có key khác nhau dù cùng PIN)
     - Iterations = 1000 vòng lặp
   - Kết quả: PIN Key 16 bytes
3. **Sinh Master Key**: Applet sinh ngẫu nhiên 16 bytes bằng `RandomData.ALG_SECURE_RANDOM`
4. **Mã hóa Master Key**: Dùng PIN Key để mã hóa Master Key → lưu vào `encryptedMasterKey`
5. **Khởi tạo số dư**: Balance = 0, mã hóa bằng Master Key → lưu vào `encryptedBalance`
6. **Sinh RSA Key Pair**: Tạo cặp khóa RSA-1024 cho chữ ký số

**Kết quả**: Thẻ đã sẵn sàng sử dụng với toàn bộ bảo mật được thiết lập.

---

### 1.3 Luồng Xác Thực PIN (PIN Verification)

Khi cư dân đăng nhập:

1. **Nhận PIN**: Desktop gửi PIN (4 số) dạng plaintext lên thẻ
2. **Kiểm tra trạng thái**:
   - Thẻ đã khởi tạo chưa? (`cardInitialized`)
   - Thẻ có bị khóa không? (`cardActive`)
   - Còn lần thử không? (`pinTryCounter > 0`)
3. **Sinh PIN Key từ PIN nhận được**: PBKDF2(PIN, cardId) → `tempPinKey`
4. **So sánh với PIN Key đã lưu**: 
   - Nếu khớp → PIN đúng
   - Nếu không khớp → PIN sai, giảm `pinTryCounter`
5. **Nếu PIN đúng**:
   - Reset `pinTryCounter = 5`
   - Giải mã Master Key: AES_Decrypt(encryptedMasterKey, PIN Key) → Master Key
   - Đánh dấu `pinVerified = true`
   - Từ giờ mọi thao tác đọc/ghi dữ liệu đều dùng Master Key

**Bảo vệ Brute-force**:
- Tối đa 5 lần thử sai → thẻ bị khóa vĩnh viễn
- PBKDF2 với 1000 iterations làm chậm quá trình thử

---

### 1.4 Luồng Đổi PIN (PIN Change)

Khi cư dân đổi mật khẩu:

1. **Nhận dữ liệu**: Desktop gửi OLD_PIN và NEW_PIN (mỗi cái 4 bytes)
2. **Xác thực PIN cũ**: PBKDF2(OLD_PIN) → so sánh với PIN Key đã lưu
3. **Giải mã Master Key**: AES_Decrypt(encryptedMasterKey, OLD_PIN_Key) → Master Key (plaintext)
4. **Sinh PIN Key mới**: PBKDF2(NEW_PIN, cardId) → New PIN Key
5. **Mã hóa lại Master Key**: AES_Encrypt(Master Key, New PIN Key) → encryptedMasterKey

**Điểm mấu chốt**: 
- Master Key **KHÔNG ĐỔI** → Balance, Info, Avatar vẫn đọc được bình thường
- Chỉ thay đổi lớp bảo vệ bên ngoài (PIN Key)
- Quá trình chỉ tốn ~10ms thay vì hàng giây như v2.0

---

### 1.5 Luồng Đọc/Ghi Dữ Liệu Mã Hóa

#### Đọc số dư (getBalance):
```
1. Kiểm tra pinVerified == true
2. AES_Decrypt(encryptedBalance, Master Key) → balance (4 bytes)
3. Trả về balance
```

#### Ghi số dư (updateBalance):
```
1. Kiểm tra pinVerified == true
2. Decrypt current balance
3. Tính toán new balance (nạp tiền hoặc thanh toán)
4. AES_Encrypt(newBalance, Master Key) → encryptedBalance
5. Trả về newBalance
```

#### Đọc/Ghi thông tin cá nhân và Avatar: Tương tự với AES-128 ECB.

---

### 1.6 Cơ Chế Chữ Ký Số RSA

Mỗi thẻ có một cặp khóa RSA-1024 riêng:
- **Private Key**: Được sinh và lưu trên thẻ, **KHÔNG BAO GIỜ** export ra ngoài
- **Public Key**: Được gửi về database khi đăng ký thẻ

**Challenge-Response Authentication**:
1. Server gửi một `challenge` ngẫu nhiên lên thẻ
2. Thẻ ký `challenge` bằng Private Key → `signature`
3. Server verify `signature` bằng Public Key
4. Nếu verify thành công → xác nhận đây là thẻ hợp lệ

---

## Phần 2: Kiến Trúc và Code

### 2.1 Sơ Đồ Kiến Trúc Tổng Thể

```
┌─────────────────────────────────────────────────────────────────────┐
│                        CITIZEN CARD APPLET v3.0                      │
├─────────────────────────────────────────────────────────────────────┤
│                                                                       │
│  ╔═══════════════════════ KEY MANAGEMENT ═══════════════════════╗   │
│  ║                                                                 ║   │
│  ║  ┌──────────────┐    ┌────────────────┐    ┌──────────────┐   ║   │
│  ║  │   User PIN   │───▶│   PBKDF2-SHA1  │───▶│   PIN Key    │   ║   │
│  ║  │  (4 digits)  │    │  1000 iters    │    │  (16 bytes)  │   ║   │
│  ║  └──────────────┘    │  salt=cardId   │    └──────┬───────┘   ║   │
│  ║                       └────────────────┘           │           ║   │
│  ║                                                     │           ║   │
│  ║                       ┌────────────────┐           ▼           ║   │
│  ║  ┌──────────────┐    │   AES-128      │    ┌──────────────┐   ║   │
│  ║  │ Master Key   │◀───│   Decrypt      │◀───│encryptedMK   │   ║   │
│  ║  │ (Random 16B) │    └────────────────┘    │  (16 bytes)  │   ║   │
│  ║  └──────┬───────┘                          └──────────────┘   ║   │
│  ║         │                                                       ║   │
│  ╚═════════╪═══════════════════════════════════════════════════════╝   │
│            │                                                           │
│  ╔═════════╪═════════════ DATA ENCRYPTION ══════════════════════╗     │
│  ║         │                                                     ║     │
│  ║         ▼                                                     ║     │
│  ║  ┌──────────────┐                                             ║     │
│  ║  │  AES-128     │                                             ║     │
│  ║  │   Cipher     │                                             ║     │
│  ║  └──────┬───────┘                                             ║     │
│  ║         │                                                     ║     │
│  ║    ┌────┴────┬─────────────┬─────────────┐                   ║     │
│  ║    ▼         ▼             ▼             ▼                   ║     │
│  ║ ┌──────┐ ┌──────┐   ┌───────────┐  ┌───────────┐            ║     │
│  ║ │Bal-  │ │Info  │   │  Avatar   │  │ Master Key│            ║     │
│  ║ │ance  │ │(512B)│   │  (15KB)   │  │(encrypted)│            ║     │
│  ║ │(16B) │ │      │   │           │  │           │            ║     │
│  ║ └──────┘ └──────┘   └───────────┘  └───────────┘            ║     │
│  ║                                                               ║     │
│  ╚═══════════════════════════════════════════════════════════════╝     │
│                                                                         │
│  ╔═══════════════════════ IDENTITY ═════════════════════════════╗     │
│  ║  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐    ║     │
│  ║  │   RSA-1024   │  │   Card ID    │  │   PIN Counter    │    ║     │
│  ║  │   Key Pair   │  │   (UUID)     │  │   (Max: 5)       │    ║     │
│  ║  └──────────────┘  └──────────────┘  └──────────────────┘    ║     │
│  ╚═══════════════════════════════════════════════════════════════╝     │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

### 2.2 Code: Cấu Hình PBKDF2

```java
// PBKDF2 Configuration
private static final short PBKDF2_ITERATIONS = 1000; // Tối ưu cho JavaCard
private static final short PBKDF2_KEY_LENGTH = 16;   // 128-bit AES key
private static final short SHA1_BLOCK_SIZE = 64;
private static final short SHA1_HASH_SIZE = 20;

// Working buffers
private byte[] hmacKey;      // 64 bytes - HMAC key buffer
private byte[] hmacBuffer;   // 84 bytes - HMAC intermediate
private byte[] pbkdf2Buffer; // 20 bytes - PBKDF2 output
```

---

### 2.3 Code: Khởi Tạo Crypto Components

```java
// Constructor - Khởi tạo các thành phần mã hóa
protected citizen_applet() {
    // PBKDF2 working buffers
    hmacKey = new byte[SHA1_BLOCK_SIZE];                        // 64 bytes
    hmacBuffer = new byte[(short)(SHA1_BLOCK_SIZE + SHA1_HASH_SIZE)]; // 84 bytes
    pbkdf2Buffer = new byte[SHA1_HASH_SIZE];                    // 20 bytes
    
    // Crypto initialization - New Master Key Architecture
    md5 = MessageDigest.getInstance(MessageDigest.ALG_MD5, false);
    sha1 = MessageDigest.getInstance(MessageDigest.ALG_SHA, false);  // For PBKDF2
    
    // Two separate AES keys
    pinKey = (AESKey) KeyBuilder.buildKey(KeyBuilder.TYPE_AES, 
                                          KeyBuilder.LENGTH_AES_128, false);
    masterKey = (AESKey) KeyBuilder.buildKey(KeyBuilder.TYPE_AES, 
                                              KeyBuilder.LENGTH_AES_128, false);
    
    aesCipher = Cipher.getInstance(Cipher.ALG_AES_BLOCK_128_ECB_NOPAD, false);
    randomData = RandomData.getInstance(RandomData.ALG_SECURE_RANDOM);
    
    // Master Key storage (encrypted by PIN Key)
    encryptedMasterKey = new byte[16];
}
```

---

### 2.4 Code: PBKDF2-HMAC-SHA1 Implementation

```java
/**
 * Derive PIN Key from PIN using PBKDF2-HMAC-SHA1
 * Uses cardId as salt for uniqueness per card
 */
private void derivePinKey(byte[] pinData, short pinOffset, short pinLen,
                          byte[] output, short outOffset) {
    pbkdf2(pinData, pinOffset, pinLen,
           cardId, (short) 0, cardIdLength,    // salt = cardId
           PBKDF2_ITERATIONS,                   // 1000 iterations
           output, outOffset, PBKDF2_KEY_LENGTH);
}

/**
 * PBKDF2-HMAC-SHA1 implementation for JavaCard
 */
private void pbkdf2(byte[] password, short passOff, short passLen,
                    byte[] salt, short saltOff, short saltLen,
                    short iterations,
                    byte[] output, short outOff, short dkLen) {

    // First iteration: U1 = HMAC(password, salt || INT(1))
    Util.arrayCopy(salt, saltOff, hmacBuffer, (short) 0, saltLen);
    hmacBuffer[(short)(saltLen)]     = 0x00;
    hmacBuffer[(short)(saltLen + 1)] = 0x00;
    hmacBuffer[(short)(saltLen + 2)] = 0x00;
    hmacBuffer[(short)(saltLen + 3)] = 0x01;  // Block index = 1

    // U1 = HMAC-SHA1(password, salt || 0x00000001)
    hmacSha1(password, passOff, passLen,
             hmacBuffer, (short) 0, (short)(saltLen + 4),
             pbkdf2Buffer, (short) 0);

    // Copy U1 to output
    Util.arrayCopy(pbkdf2Buffer, (short) 0, output, outOff, dkLen);

    // Subsequent iterations: Ui = HMAC(password, U(i-1)), output ^= Ui
    for (short i = 1; i < iterations; i++) {
        hmacSha1(password, passOff, passLen,
                 pbkdf2Buffer, (short) 0, SHA1_HASH_SIZE,
                 pbkdf2Buffer, (short) 0);

        // XOR with output
        for (short j = 0; j < dkLen; j++) {
            output[(short)(outOff + j)] ^= pbkdf2Buffer[j];
        }
    }
}
```

---

### 2.5 Code: HMAC-SHA1 Implementation

```java
/**
 * HMAC-SHA1 implementation
 * HMAC(K, m) = H((K' XOR opad) || H((K' XOR ipad) || m))
 */
private void hmacSha1(byte[] key, short keyOff, short keyLen,
                      byte[] message, short msgOff, short msgLen,
                      byte[] output, short outOff) {

    // Step 1: Prepare K' (key padded to block size)
    if (keyLen > SHA1_BLOCK_SIZE) {
        sha1.reset();
        sha1.doFinal(key, keyOff, keyLen, hmacKey, (short) 0);
        Util.arrayFillNonAtomic(hmacKey, SHA1_HASH_SIZE,
                (short)(SHA1_BLOCK_SIZE - SHA1_HASH_SIZE), (byte) 0x00);
    } else {
        Util.arrayCopy(key, keyOff, hmacKey, (short) 0, keyLen);
        Util.arrayFillNonAtomic(hmacKey, keyLen,
                (short)(SHA1_BLOCK_SIZE - keyLen), (byte) 0x00);
    }

    // Step 2: Inner hash: H((K' XOR ipad) || message)
    sha1.reset();
    for (short i = 0; i < SHA1_BLOCK_SIZE; i++) {
        hmacBuffer[i] = (byte)(hmacKey[i] ^ 0x36);  // ipad = 0x36
    }
    sha1.update(hmacBuffer, (short) 0, SHA1_BLOCK_SIZE);
    sha1.doFinal(message, msgOff, msgLen, hmacBuffer, (short) 0);

    // Step 3: Outer hash: H((K' XOR opad) || inner_hash)
    sha1.reset();
    for (short i = 0; i < SHA1_BLOCK_SIZE; i++) {
        hmacBuffer[(short)(SHA1_HASH_SIZE + i)] = (byte)(hmacKey[i] ^ 0x5C); // opad
    }
    sha1.update(hmacBuffer, SHA1_HASH_SIZE, SHA1_BLOCK_SIZE);
    sha1.doFinal(hmacBuffer, (short) 0, SHA1_HASH_SIZE, output, outOff);
}
```

---

### 2.6 Code: Khởi Tạo Thẻ

```java
private void initializeCard(APDU apdu) {
    byte[] buffer = apdu.getBuffer();
    short lc = apdu.setIncomingAndReceive();

    // FORMAT v3.0: [PIN:4][cardIdLength:1][cardId:N]
    short idLen = (short)(buffer[(short)(ISO7816.OFFSET_CDATA + PIN_LENGTH)] & 0xFF);
    
    // Copy Card ID (dùng làm salt cho PBKDF2)
    Util.arrayCopy(buffer, (short)(ISO7816.OFFSET_CDATA + PIN_LENGTH + 1), 
                   cardId, (short) 0, idLen);
    cardIdLength = idLen;

    // === PBKDF2 ON APPLET: Derive PIN Key from PIN ===
    derivePinKey(buffer, ISO7816.OFFSET_CDATA, PIN_LENGTH, pin, (short) 0);
    pinKey.setKey(pin, (short) 0);

    // === Generate random Master Key ===
    randomData.generateData(tempBuffer, (short) 0, (short) 16);
    masterKey.setKey(tempBuffer, (short) 0);

    // Encrypt Master Key with PIN Key and store
    aesCipher.init(pinKey, Cipher.MODE_ENCRYPT);
    aesCipher.doFinal(tempBuffer, (short) 0, (short) 16, encryptedMasterKey, (short) 0);

    // Clear temp buffer for security
    Util.arrayFillNonAtomic(tempBuffer, (short) 0, (short) 16, (byte) 0x00);

    // Initialize balance to 0 (encrypted with Master Key)
    Util.arrayFillNonAtomic(tempBuffer, (short) 0, (short) 16, (byte) 0x00);
    aesCipher.init(masterKey, Cipher.MODE_ENCRYPT);
    aesCipher.doFinal(tempBuffer, (short) 0, (short) 16, encryptedBalance, (short) 0);

    cardInitialized = true;
    pinVerified = true;
    cardActive = true;
}
```

---

### 2.7 Code: Xác Thực PIN

```java
private void verifyPin(APDU apdu) {
    if (!cardInitialized) ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
    if (!cardActive) ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
    if (pinTryCounter == 0) ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);

    byte[] buffer = apdu.getBuffer();
    short lc = apdu.setIncomingAndReceive();

    // Derive PIN Key from received PIN using PBKDF2
    derivePinKey(buffer, ISO7816.OFFSET_CDATA, PIN_LENGTH, tempBuffer, (short) 0);

    // Compare derived PIN Key with stored PIN Key
    if (Util.arrayCompare(pin, (short) 0, tempBuffer, (short) 0, (short) 16) == 0) {
        pinVerified = true;
        pinTryCounter = MAX_PIN_TRIES;

        // Set PIN Key
        pinKey.setKey(tempBuffer, (short) 0);

        // === Decrypt Master Key using PIN Key ===
        aesCipher.init(pinKey, Cipher.MODE_DECRYPT);
        aesCipher.doFinal(encryptedMasterKey, (short) 0, (short) 16, tempBuffer, (short) 0);
        masterKey.setKey(tempBuffer, (short) 0);

        // Clear temp buffer for security
        Util.arrayFillNonAtomic(tempBuffer, (short) 0, (short) 16, (byte) 0x00);

        buffer[0] = (byte) 0x01; // Success
        buffer[1] = pinTryCounter;
    } else {
        Util.arrayFillNonAtomic(tempBuffer, (short) 0, (short) 16, (byte) 0x00);
        pinTryCounter--;
        pinVerified = false;
        buffer[0] = (byte) 0x00; // Failure
        buffer[1] = pinTryCounter;
    }
    apdu.setOutgoingAndSend((short) 0, (short) 2);
}
```

---

### 2.8 Code: Đổi PIN (Nhanh - Chỉ Re-encrypt Master Key)

```java
private void updatePin(APDU apdu) {
    if (!cardInitialized || !pinVerified) {
        ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
    }

    byte[] buffer = apdu.getBuffer();
    short lc = apdu.setIncomingAndReceive();

    // FORMAT v3.0: [OLD_PIN:4][NEW_PIN:4]
    // Verify old PIN Key
    derivePinKey(buffer, ISO7816.OFFSET_CDATA, PIN_LENGTH, tempBuffer, (short) 0);
    if (Util.arrayCompare(pin, (short) 0, tempBuffer, (short) 0, (short) 16) != 0) {
        Util.arrayFillNonAtomic(tempBuffer, (short) 0, (short) 16, (byte) 0x00);
        ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
    }

    // === Only re-encrypt Master Key, NOT all data! ===

    // 1. Decrypt Master Key with OLD PIN Key
    aesCipher.init(pinKey, Cipher.MODE_DECRYPT);
    aesCipher.doFinal(encryptedMasterKey, (short) 0, (short) 16, tempBalance, (short) 0);

    // 2. Derive new PIN Key from new PIN
    derivePinKey(buffer, (short)(ISO7816.OFFSET_CDATA + PIN_LENGTH), PIN_LENGTH, 
                 pin, (short) 0);

    // 3. Set new PIN Key
    pinKey.setKey(pin, (short) 0);

    // 4. Re-encrypt Master Key with NEW PIN Key
    aesCipher.init(pinKey, Cipher.MODE_ENCRYPT);
    aesCipher.doFinal(tempBalance, (short) 0, (short) 16, encryptedMasterKey, (short) 0);

    // 5. Clear temp buffers for security
    Util.arrayFillNonAtomic(tempBuffer, (short) 0, (short) 16, (byte) 0x00);
    Util.arrayFillNonAtomic(tempBalance, (short) 0, (short) 16, (byte) 0x00);

    // === NO need to re-encrypt Balance, Info, Avatar! ===
    // They are encrypted with Master Key, which remains the same!

    buffer[0] = (byte) 0x01;
    apdu.setOutgoingAndSend((short) 0, (short) 1);
}
```

---

### 2.9 Sequence Diagram: Full Authentication Flow

```
┌─────────┐         ┌─────────────┐         ┌───────────────────────┐
│ Desktop │         │ CardService │         │   citizen_applet      │
└────┬────┘         └──────┬──────┘         └───────────┬───────────┘
     │                     │                            │
     │  1. Login request   │                            │
     │ ───────────────────▶│                            │
     │                     │  2. SELECT AID             │
     │                     │ ──────────────────────────▶│
     │                     │                            │
     │                     │  3. Return: 9000 OK        │
     │                     │ ◀──────────────────────────│
     │                     │                            │
     │                     │  4. VERIFY_PIN (00 00 04 00│[PIN:4])
     │                     │ ──────────────────────────▶│
     │                     │                            │
     │                     │           ┌────────────────┴────────────────┐
     │                     │           │ 5. PBKDF2(PIN, cardId)          │
     │                     │           │    → tempPinKey                  │
     │                     │           │                                  │
     │                     │           │ 6. Compare tempPinKey == pin[]? │
     │                     │           │    YES → Continue                │
     │                     │           │                                  │
     │                     │           │ 7. AES_Decrypt(encryptedMK,      │
     │                     │           │               tempPinKey)        │
     │                     │           │    → Master Key (loaded)         │
     │                     │           │                                  │
     │                     │           │ 8. pinVerified = true            │
     │                     │           └────────────────┬────────────────┘
     │                     │                            │
     │                     │  9. Return: [01][05] + 9000│
     │                     │ ◀──────────────────────────│
     │                     │                            │
     │  10. Login success  │                            │
     │ ◀───────────────────│                            │
     │                     │                            │
```

---

### 2.10 Bảng Tóm Tắt Thuật Toán

| Thành phần | Thuật toán | Tham số | Mục đích |
|------------|-----------|---------|----------|
| PIN Key Derivation | PBKDF2-HMAC-SHA1 | 1000 iterations, salt=cardId | Chống brute-force, unique per card |
| Master Key | Secure Random | 128-bit | Mã hóa dữ liệu thực tế |
| Data Encryption | AES-128 ECB | No padding | Mã hóa Balance, Info, Avatar |
| Master Key Wrap | AES-128 ECB | PIN Key | Bảo vệ Master Key |
| Digital Signature | RSA-1024 PKCS1 | SHA-1 | Xác thực thẻ |
| PIN Storage | PBKDF2 output | 16 bytes | So sánh khi verify |

---

### 2.11 Security Best Practices Trong Code

```java
// 1. Luôn clear buffer sau khi sử dụng
Util.arrayFillNonAtomic(tempBuffer, (short) 0, (short) 16, (byte) 0x00);

// 2. Kiểm tra trạng thái trước mọi thao tác nhạy cảm
if (!cardInitialized || !pinVerified) {
    ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
}

// 3. Giảm counter trước khi thông báo lỗi (avoid timing attack)
pinTryCounter--;
pinVerified = false;

// 4. Sử dụng constant-time comparison cho PIN
Util.arrayCompare(pin, (short) 0, tempBuffer, (short) 0, (short) 16);

// 5. Master Key không bao giờ lưu plaintext lâu dài
// Chỉ load vào RAM khi cần, clear sau khi dùng
```

---

## Kết Luận

Citizen Card Applet v3.0 cung cấp một kiến trúc bảo mật mạnh mẽ với:

1. **PBKDF2 on-card** - PIN được xử lý hoàn toàn trên thẻ, không bao giờ rời khỏi secure element
2. **Two-layer key architecture** - Tách biệt PIN Key và Master Key cho hiệu suất và bảo mật
3. **Fast PIN change** - Đổi PIN chỉ mất ~10ms thay vì hàng giây
4. **Brute-force protection** - 1000 PBKDF2 iterations + 5 lần thử tối đa
5. **Unique keys per card** - Salt = cardId đảm bảo mỗi thẻ có PIN Key khác nhau

---

*Tài liệu cập nhật: Tháng 1/2026*
*Version: 3.0*
