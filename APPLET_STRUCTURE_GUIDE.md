# APPLET CODE STRUCTURE GUIDE

## 📋 Tổng quan
File `citizen_applet.java` đã được tổ chức lại với cấu trúc rõ ràng, dễ đọc và maintain. Tất cả code được gộp trong một file nhưng chia thành các section logic.

## 🏗️ Cấu trúc Code

### 1. 📋 Constants & Configuration
```java
// INS codes cho các lệnh APDU
// Storage limits và security parameters
// Photo transfer constants
```

### 2. 🗄️ Persistent Storage & State Variables
```java
// Card state và security status
// RSA key pair (private/public)
// AES key derived from PIN
// Encrypted data storage arrays
// Photo buffer và transfer state
```

### 3. 🚀 Applet Lifecycle & Initialization
```java
// install() method
// Constructor với crypto initialization
// RSA key pair generation
// AES cipher setup
```

### 4. 🔄 Main APDU Processing & Command Routing
```java
// process() method
// Switch statement cho tất cả INS codes
// Error handling
```

### 5. 🆔 Card Initialization & ID Generation
```java
// initializeCard() - setup ban đầu
// generateCardId() - tạo unique ID
// PIN setup và AES key derivation
```

### 6. 🔐 PIN Management & Authentication System
```java
// verifyPin() - xác thực PIN
// changePin() - đổi PIN
// PIN hashing với SHA-256
// Security policies và retry limits
```

### 7. 📄 Card Information & RSA Key Management
```java
// getCardId() - trả về card ID
// getPublicKey() - export RSA public key
// RSA key format và encoding
```

### 8. 💰 Financial Operations & Transaction Management
```java
// getBalance() - lấy số dư (AES decrypt)
// topupBalance() - nạp tiền
// makePayment() - thanh toán
// Transaction logging
```

### 9. 🔧 AES Crypto Helper Methods
```java
// decryptBalance() - giải mã số dư
// setBalance() - mã hóa và lưu số dư
// generateAESKeyFromPin() - tạo AES key từ PIN
// addTransactionRecord() - log giao dịch
```

### 10. 📸 Photo Management & Chunked Transfer System
```java
// startPhotoUpload() - bắt đầu upload
// uploadPhotoChunk() - upload từng chunk
// finishPhotoUpload() - hoàn thành upload
// getPhotoSize() - lấy kích thước ảnh
// getPhotoChunk() - download từng chunk
```

## 🔐 Crypto Architecture

### RSA-1024 Usage
- **Key Exchange**: Secure communication với desktop app
- **Authentication**: Digital signatures cho critical operations  
- **Public Key Export**: Chia sẻ với desktop để verification
- **Private Key**: Không bao giờ rời khỏi thẻ

### AES-128 Usage
- **Data Encryption**: Tất cả sensitive data (balance, personal info, transactions)
- **Key Derivation**: `AES_KEY = SHA-256(PIN + SALT)[0:16]`
- **Automatic Protection**: Data không đọc được nếu không có PIN đúng
- **Performance**: Mã hóa đối xứng nhanh cho bulk data

### Security Model
```
PIN → SHA-256 → AES Key → Encrypted Data Storage
RSA Keys → Secure Communication Channel
```

## 📝 Code Organization Benefits

### ✅ Advantages của Single File
- **Build đơn giản**: Chỉ cần compile 1 file
- **Deployment dễ**: Không cần manage nhiều .class files
- **JavaCard compatibility**: Tránh issues với multiple classes
- **Size optimization**: Nhỏ gọn hơn cho smart card memory

### ✅ Clear Structure
- **Section headers**: Emoji và comments rõ ràng
- **Logical grouping**: Functions được nhóm theo chức năng
- **Easy navigation**: Dễ tìm code cần thiết
- **Maintainable**: Dễ sửa đổi và mở rộng

### ✅ Documentation
- **Inline comments**: Giải thích crypto operations
- **Function headers**: Mô tả purpose và parameters
- **Architecture notes**: Tổng quan security model
- **APDU format**: Documented cho mỗi command

## 🔧 Development Workflow

### 1. Editing Code
```bash
# Mở file chính
vim applet/src/citizen_applet/citizen_applet.java

# Tìm section cần sửa bằng emoji hoặc comment
# Ví dụ: tìm "🔐 PIN MANAGEMENT" để sửa PIN logic
```

### 2. Building
```bash
# Build applet (single file)
cd applet
ant build

# Deploy to JCIDE
# Load citizen_applet.cap file
```

### 3. Testing
```bash
# Test với desktop app
cd desktop
mvn javafx:run

# Hoặc test individual APDU commands
```

## 📚 Key Sections for Common Tasks

| Task | Section | Line Range (approx) |
|------|---------|-------------------|
| Add new INS code | 📋 Constants | 30-50 |
| Modify PIN logic | 🔐 PIN Management | 300-450 |
| Change crypto | 🔧 AES Crypto | 600-750 |
| Add new command | 🔄 Main Processing | 200-250 |
| Modify photo handling | 📸 Photo Management | 800-900 |
| Financial operations | 💰 Financial Operations | 500-600 |

## 🎯 Best Practices

### Code Maintenance
- **Follow emoji sections**: Giữ code trong đúng section
- **Update comments**: Khi thay đổi logic, update documentation
- **Test thoroughly**: Mỗi thay đổi cần test với desktop app
- **Version control**: Commit frequently với clear messages

### Security Considerations
- **Never log sensitive data**: PIN, keys, personal info
- **Clear temporary arrays**: Sau khi dùng crypto operations
- **Validate all inputs**: Check APDU parameters
- **Handle exceptions**: Proper error codes cho mọi failure case

### Performance Tips
- **Minimize object creation**: Reuse arrays khi có thể
- **Efficient crypto**: Batch operations khi possible
- **Memory management**: Clear unused data promptly
- **APDU optimization**: Minimize round trips

## 🚀 Future Enhancements

### Possible Additions
- **Biometric authentication**: Fingerprint support
- **Advanced crypto**: ECC curves, newer algorithms  
- **Multi-application**: Support multiple applets
- **Secure messaging**: Full ISO 7816-4 compliance
- **Contactless features**: NFC optimizations

### Refactoring Options
- **Helper classes**: Nếu code trở nên quá lớn
- **Modular design**: Chia thành logical modules
- **Configuration**: External config cho parameters
- **Internationalization**: Multi-language support

---

**Kết luận**: Cấu trúc hiện tại cân bằng tốt giữa simplicity (single file) và organization (clear sections), phù hợp cho smart card development và dễ maintain.