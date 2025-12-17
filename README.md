# 🏛️ Citizen Card Management System

Hệ thống quản lý thẻ cư dân thông minh sử dụng JavaCard applet và ứng dụng desktop JavaFX.

## 📋 Mục lục

- [Tổng quan](#-tổng-quan)
- [Tính năng](#-tính-năng)
- [Kiến trúc hệ thống](#-kiến-trúc-hệ-thống)
- [Cấu trúc dự án](#-cấu-trúc-dự-án)
- [Yêu cầu hệ thống](#-yêu-cầu-hệ-thống)
- [Hướng dẫn cài đặt](#-hướng-dẫn-cài-đặt)
- [Hướng dẫn sử dụng](#-hướng-dẫn-sử-dụng)
- [APDU Commands](#-apdu-commands)
- [Bảo mật](#-bảo-mật)
- [Cơ sở dữ liệu](#-cơ-sở-dữ-liệu)
- [Xử lý sự cố](#-xử-lý-sự-cố)

---

## 🎯 Tổng quan

Citizen Card Management System là hệ thống quản lý thẻ cư dân thông minh, cho phép:
- Lưu trữ thông tin cá nhân được mã hóa trên thẻ JavaCard
- Quản lý số dư và giao dịch thanh toán
- Xác thực bằng mã PIN với bảo mật cao
- Lưu trữ ảnh đại diện trên thẻ
- Ký số RSA cho các giao dịch quan trọng

### Công nghệ sử dụng

| Thành phần | Công nghệ |
|------------|-----------|
| Smart Card | JavaCard 2.2.1 |
| Desktop App | JavaFX 20.0.1 |
| Database | H2 (Embedded) |
| Build Tool | Maven 3.x |
| Card I/O | javax.smartcardio |
| Mã hóa | AES-128, RSA-1024, MD5 |

---

## ✨ Tính năng

### 🔐 Bảo mật thẻ
- **Xác thực PIN**: Mã PIN 4 số với MD5 hash, tối đa 5 lần thử
- **Mã hóa AES-128**: Tất cả dữ liệu nhạy cảm được mã hóa
- **Chữ ký RSA-1024**: Ký số cho các giao dịch quan trọng
- **Tự động khóa**: Thẻ bị khóa sau 5 lần nhập sai PIN

### 👨‍💼 Chức năng Admin
- Đăng ký thẻ mới cho cư dân
- Upload ảnh đại diện lên thẻ
- Quản lý trạng thái thẻ (kích hoạt/khóa)
- Xem danh sách thẻ đã đăng ký
- Gửi hóa đơn cho cư dân
- Duyệt yêu cầu nạp tiền
- Reset PIN và mở khóa thẻ
- Xem cơ sở dữ liệu H2

### 👤 Chức năng Cư dân
- Xem thông tin cá nhân trên thẻ
- Kiểm tra số dư
- Xem và thanh toán hóa đơn
- Yêu cầu nạp tiền (chuyển khoản)
- Đổi mã PIN
- Cập nhật thông tin cá nhân
- Quản lý ảnh đại diện

### 💳 Quản lý số dư
- Nạp tiền qua chuyển khoản ngân hàng
- Thanh toán hóa đơn tự động
- Lịch sử giao dịch chi tiết
- Số dư được mã hóa trên thẻ

---

## 🏗️ Kiến trúc hệ thống

```
┌─────────────────────────────────────────────────────────────┐
│                    Desktop Application                       │
│  ┌─────────────────────────────────────────────────────┐    │
│  │              JavaFX UI Layer                         │    │
│  │  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐    │    │
│  │  │   Login     │ │   Admin     │ │  Citizen    │    │    │
│  │  │   View      │ │  Dashboard  │ │  Dashboard  │    │    │
│  │  └─────────────┘ └─────────────┘ └─────────────┘    │    │
│  └─────────────────────────────────────────────────────┘    │
│                           │                                  │
│  ┌─────────────────────────────────────────────────────┐    │
│  │              Service Layer                           │    │
│  │  ┌─────────────────────┐  ┌─────────────────────┐   │    │
│  │  │    CardService      │  │      CardDAO        │   │    │
│  │  │  (APDU Commands)    │  │   (H2 Database)     │   │    │
│  │  └─────────────────────┘  └─────────────────────┘   │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
                           │
                           │ javax.smartcardio
                           │ ISO 7816-4 APDU
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                   JavaCard Applet                            │
│  ┌─────────────────────────────────────────────────────┐    │
│  │                 citizen_applet                       │    │
│  │  ┌───────────┐ ┌───────────┐ ┌───────────────────┐  │    │
│  │  │    PIN    │ │  Balance  │ │  Personal Info    │  │    │
│  │  │  (MD5)    │ │  (AES)    │ │     (AES)         │  │    │
│  │  └───────────┘ └───────────┘ └───────────────────┘  │    │
│  │  ┌───────────┐ ┌───────────┐ ┌───────────────────┐  │    │
│  │  │   RSA     │ │  Avatar   │ │     Card ID       │  │    │
│  │  │  Keys     │ │  (8KB)    │ │                   │  │    │
│  │  └───────────┘ └───────────┘ └───────────────────┘  │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

---

## 📁 Cấu trúc dự án

```
citizen_card/
│
├── applet/                              # JavaCard Applet
│   ├── src/citizen_applet/
│   │   └── citizen_applet.java          # Main applet (~500 lines)
│   ├── bin/                             # Compiled .class và .cap files
│   └── applet.jcproj                    # JCIDE project file
│
├── desktop/                             # JavaFX Desktop Application
│   ├── src/main/java/citizencard/
│   │   ├── MainApp.java                 # Entry point
│   │   ├── controller/                  # UI Controllers
│   │   │   ├── LoginViewController.java
│   │   │   ├── AdminDashboardController.java
│   │   │   ├── CitizenDashboardController.java
│   │   │   ├── DemoWorkflowController.java
│   │   │   └── PhotoManagementController.java
│   │   ├── service/
│   │   │   └── CardService.java         # APDU communication (~1000 lines)
│   │   ├── dao/
│   │   │   └── CardDAO.java             # Database operations
│   │   ├── model/
│   │   │   └── CitizenInfo.java         # Data model
│   │   └── util/
│   │       ├── DatabaseViewer.java
│   │       ├── DialogUtils.java
│   │       ├── PhotoUtils.java
│   │       ├── PinInputDialog.java
│   │       ├── CitizenInfoParser.java
│   │       ├── DataValidator.java
│   │       └── RSAUtils.java
│   ├── src/main/resources/
│   │   ├── css/styles.css               # UI Styles (~450 lines)
│   │   └── qr_bank.png                  # QR code for bank transfer
│   ├── data/                            # H2 Database (auto-generated)
│   └── pom.xml                          # Maven configuration
│
├── view-database.bat                    # Database viewer (Windows)
├── view-database.sh                     # Database viewer (Linux/Mac)
└── README.md                            # This file
```

---

## 💻 Yêu cầu hệ thống

### Phần mềm bắt buộc
- **Java JDK 17+** (khuyến nghị JDK 21)
- **Maven 3.6+**
- **JCIDE** (để phát triển và load applet)

### Phần cứng
- **Smart Card Reader** (USB)
- **JavaCard** tương thích JavaCard 2.2.1+

### Kiểm tra cài đặt
```bash
# Kiểm tra Java
java -version

# Kiểm tra Maven
mvn -version
```

---

## 🚀 Hướng dẫn cài đặt

### 1. Clone repository
```bash
git clone <repository-url>
cd citizen_card
```

### 2. Build và Load Applet

#### Sử dụng JCIDE (khuyến nghị)
1. Mở JCIDE
2. File → Open Project → chọn `applet/applet.jcproj`
3. Nhấn **F7** để build
4. Output: `applet/bin/citizen_applet.cap`
5. Kết nối card reader và insert thẻ
6. Load applet lên thẻ qua JCIDE

#### Thông tin Applet
```
Package AID: 11 22 33 44 55
Applet AID:  11 22 33 44 55 00
```

### 3. Chạy Desktop Application

```bash
cd desktop

# Build project
mvn clean compile

# Chạy ứng dụng
mvn javafx:run
```

### 4. Xem Database (tùy chọn)

**⚠️ Quan trọng: Đóng ứng dụng trước khi xem database!**

```bash
# Windows
view-database.bat

# Linux/Mac
./view-database.sh
```

---

## 📖 Hướng dẫn sử dụng

### Đăng nhập

1. **Kết nối thẻ**: Cắm thẻ vào card reader
2. **Khởi động JCIDE**: Đảm bảo JCIDE terminal đang chạy
3. **Chạy ứng dụng**: `mvn javafx:run`
4. **Chọn chế độ**:
   - **Quản trị viên**: Truy cập trực tiếp dashboard admin
   - **Cư dân**: Cần nhập PIN để xác thực

### Chế độ Admin

#### Tạo thẻ mới
1. Vào **Tổng quan** → **Tạo thẻ mới**
2. Nhập thông tin cư dân:
   - Họ tên
   - CCCD
   - Số phòng
   - Ngày sinh
   - Số điện thoại
3. Thiết lập PIN (4 số)
4. Upload ảnh đại diện (tùy chọn)
5. Nhấn **Tạo thẻ**

#### Gửi hóa đơn
1. Vào **Quản lý cư dân**
2. Tìm cư dân theo ID thẻ
3. Nhấn **Gửi hóa đơn**
4. Nhập số tiền và mô tả
5. Xác nhận gửi

#### Duyệt nạp tiền
1. Vào **Yêu cầu nạp tiền**
2. Xem danh sách yêu cầu chờ duyệt
3. Kiểm tra tài khoản ngân hàng
4. Nhấn **Duyệt** hoặc **Từ chối**

### Chế độ Cư dân

#### Xem thông tin thẻ
1. Đăng nhập bằng PIN
2. Vào **Thông tin thẻ**
3. Xem thông tin cá nhân và số dư

#### Thanh toán hóa đơn
1. Vào **Hóa đơn**
2. Xem danh sách hóa đơn chờ thanh toán
3. Nhấn **Thanh toán** và nhập PIN xác nhận

#### Yêu cầu nạp tiền
1. Vào **Nạp tiền**
2. Nhập số tiền cần nạp
3. Chuyển khoản theo thông tin hiển thị
4. Nhấn **Xác nhận đã chuyển**
5. Chờ admin duyệt

#### Đổi PIN
1. Vào **Bảo mật**
2. Nhấn **Đổi PIN**
3. Nhập PIN cũ và PIN mới
4. Xác nhận

---

## 📡 APDU Commands

### Cấu trúc APDU
```
CLA | INS | P1 | P2 | Lc | Data | Le
```

### INS Codes

| INS | Hex | Mô tả |
|-----|-----|-------|
| VERIFY | 0x00 | Xác thực PIN |
| CREATE | 0x01 | Khởi tạo thẻ/tạo dữ liệu |
| GET | 0x02 | Đọc dữ liệu |
| UPDATE | 0x03 | Cập nhật dữ liệu |
| RESET_TRY_PIN | 0x10 | Reset số lần thử PIN |
| CLEAR_CARD | 0x11 | Xóa toàn bộ dữ liệu thẻ |

### P1 Parameters

| P1 | Hex | Mô tả |
|----|-----|-------|
| PIN | 0x04 | Thao tác với PIN |
| CITIZEN_INFO | 0x05 | Thông tin cư dân |
| SIGNATURE | 0x06 | Chữ ký RSA |

### P2 Parameters

| P2 | Hex | Mô tả |
|----|-----|-------|
| INFORMATION | 0x07 | Thông tin cá nhân |
| TRY_REMAINING | 0x08 | Số lần thử PIN còn lại |
| AVATAR | 0x09 | Ảnh đại diện |
| CARD_ID | 0x0A | ID thẻ |
| PUBLIC_KEY | 0x0B | Public key RSA |
| BALANCE | 0x0C | Số dư |

### Ví dụ APDU Commands

```
# Khởi tạo thẻ với PIN "1234"
00 01 04 00 04 31 32 33 34

# Xác thực PIN
00 00 04 00 04 31 32 33 34

# Đọc số dư
00 02 00 0C 00

# Đọc Card ID
00 02 00 0A 00

# Nạp tiền 100,000 VND
00 03 05 0C 05 01 00 01 86 A0

# Thanh toán 50,000 VND
00 03 05 0C 05 02 00 00 C3 50
```

---

## 🔒 Bảo mật

### Applet Security

| Tính năng | Mô tả |
|-----------|-------|
| PIN Hash | MD5 hash, không lưu plaintext |
| AES-128 | Mã hóa số dư, thông tin cá nhân |
| RSA-1024 | Chữ ký số cho giao dịch |
| PIN Tries | Tối đa 5 lần, sau đó khóa thẻ |
| Key Storage | Private key không bao giờ rời thẻ |

### Data Protection

| Dữ liệu | Vị trí | Bảo vệ |
|---------|--------|--------|
| PIN | Smart Card | MD5 Hash |
| Private Key | Smart Card | Không export được |
| Balance | Smart Card | AES-128 |
| Personal Info | Smart Card | AES-128 |
| Avatar | Smart Card | AES-128 |
| Card Registry | H2 Database | Local only |
| Transaction Logs | H2 Database | Local only |

### Lưu ý bảo mật
- ❌ PIN và private key **KHÔNG** được lưu trong database
- ✅ Database chỉ chứa thông tin đăng ký và logs
- ✅ Tất cả dữ liệu nhạy cảm được mã hóa trên thẻ
- ✅ Giao tiếp qua APDU theo chuẩn ISO 7816-4

---

## 🗄️ Cơ sở dữ liệu

### Schema

#### Bảng CARDS
```sql
CREATE TABLE CARDS (
    id INT PRIMARY KEY AUTO_INCREMENT,
    card_id VARCHAR(50) UNIQUE NOT NULL,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    registered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_accessed TIMESTAMP
);
```

#### Bảng TRANSACTIONS
```sql
CREATE TABLE TRANSACTIONS (
    id INT PRIMARY KEY AUTO_INCREMENT,
    card_id VARCHAR(50),
    type VARCHAR(50),
    success BOOLEAN,
    details VARCHAR(255),
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### Bảng INVOICES
```sql
CREATE TABLE INVOICES (
    id INT PRIMARY KEY AUTO_INCREMENT,
    card_id VARCHAR(50),
    amount BIGINT,
    description VARCHAR(255),
    status VARCHAR(20) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    paid_at TIMESTAMP
);
```

#### Bảng TOPUP_REQUESTS
```sql
CREATE TABLE TOPUP_REQUESTS (
    id INT PRIMARY KEY AUTO_INCREMENT,
    card_id VARCHAR(50),
    amount BIGINT,
    status VARCHAR(20) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    approved_at TIMESTAMP
);
```

### Xem Database

```bash
# Chạy database viewer
view-database.bat  # Windows
./view-database.sh # Linux/Mac

# Hoặc truy cập H2 Console
# URL: http://localhost:8082
# JDBC URL: jdbc:h2:./desktop/data/citizen_card
# User: sa
# Password: (để trống)
```

---

## 🔧 Xử lý sự cố

### Lỗi build Applet
```
❌ Lỗi: Cannot find symbol
✅ Giải pháp: Kiểm tra JavaCard API path trong JCIDE
```

### Ứng dụng không khởi động
```
❌ Lỗi: JavaFX runtime components are missing
✅ Giải pháp: 
   - Chạy: mvn clean compile
   - Kiểm tra Java 17+ đã cài đặt
   - Sử dụng: mvn javafx:run
```

### Không kết nối được thẻ
```
❌ Lỗi: No card terminals found
✅ Giải pháp:
   - Kiểm tra card reader đã kết nối
   - Kiểm tra driver card reader
   - Đảm bảo JCIDE terminal đang chạy
```

### Thẻ bị khóa
```
❌ Lỗi: Card blocked after 5 failed PIN attempts
✅ Giải pháp:
   - Đăng nhập Admin
   - Vào Tổng quan → Mở khóa thẻ
   - Hoặc Reset thẻ (mất dữ liệu)
```

### Lỗi Database
```
❌ Lỗi: Database may be already in use
✅ Giải pháp:
   - Đóng ứng dụng desktop
   - Đóng tất cả H2 Console
   - Thử lại
```

### Reset hoàn toàn
```bash
# Xóa database
rm -rf desktop/data/

# Rebuild
cd desktop
mvn clean compile
mvn javafx:run
```

---

## 📝 Ghi chú phát triển

### Dependencies (pom.xml)
```xml
<dependencies>
    <!-- JavaFX -->
    <dependency>
        <groupId>org.openjfx</groupId>
        <artifactId>javafx-controls</artifactId>
        <version>20.0.1</version>
    </dependency>
    <dependency>
        <groupId>org.openjfx</groupId>
        <artifactId>javafx-fxml</artifactId>
        <version>20.0.1</version>
    </dependency>
    
    <!-- H2 Database -->
    <dependency>
        <groupId>com.h2database</groupId>
        <artifactId>h2</artifactId>
        <version>2.2.224</version>
    </dependency>
    
    <!-- Logging -->
    <dependency>
        <groupId>org.slf4j</groupId>
        <artifactId>slf4j-api</artifactId>
        <version>2.0.7</version>
    </dependency>
</dependencies>
```

### Giới hạn kỹ thuật
- **Photo size**: Tối đa 8KB (do giới hạn bộ nhớ JavaCard)
- **Personal info**: Tối đa 256 bytes
- **APDU data**: Tối đa 255 bytes/command (chunked transfer cho photo)
- **RSA key**: 1024-bit (giới hạn JavaCard 2.2.1)

---

## 📄 License

MIT License - Xem file LICENSE để biết thêm chi tiết.

---

## 👥 Đóng góp

Mọi đóng góp đều được chào đón! Vui lòng tạo Pull Request hoặc Issue.

---

## 🙏 Acknowledgments

- Oracle JavaCard Technology
- OpenJFX Team
- H2 Database Engine

---

**Citizen Card Management System v1.0** | Dự án học tập về JavaCard và Smart Card Development
