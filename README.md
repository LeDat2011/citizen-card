# Hệ thống Quản lý Thẻ Cư dân

Hệ thống quản lý thẻ cư dân sử dụng **javax.smartcardio** để kết nối với **JCIDE terminal**. Desktop app JavaFX tích hợp toàn bộ backend logic (không cần HTTP server riêng).

## 🏗️ Kiến trúc hệ thống

```
┌─────────────────────────────────────────┐
│  Desktop App (JavaFX)                   │
│  ┌───────────────────────────────────┐  │
│  │  UI Components                    │  │
│  │  └─> CitizenCardService (Local)   │  │
│  └───────────────────────────────────┘  │
│              │                           │
│              │ Direct Method Calls       │
│              │ (Same JVM, no HTTP)       │
│              ▼                           │
│  ┌───────────────────────────────────┐  │
│  │  Backend Service                   │  │
│  │  - Business Logic                  │  │
│  │  - DAO Layer                       │  │
│  │  - RealCardClient                  │  │
│  └───────────────────────────────────┘  │
│              │                           │
│              │ SQLite (File-based)       │
│              ▼                           │
│  ┌───────────────────────────────────┐  │
│  │  SQLite Database                   │  │
│  └───────────────────────────────────┘  │
└─────────────────────────────────────────┘
              │
              │ javax.smartcardio
              │ (ISO 7816 T=1)
              ▼
┌─────────────────────────────────────────┐
│  JCIDE Terminal                         │
│  (JavaCard Applet)                      │
└─────────────────────────────────────────┘
```

## 📦 Cấu trúc Project

```
citizen_card/
├── desktop/                    # Desktop application (All-in-one module)
│   ├── src/main/java/
│   │   └── com/citizencard/
│   │       ├── app/                   # Application entry point
│   │       │   └── MainApp.java      # JavaFX main
│   │       ├── ui/                    # Presentation layer
│   │       │   ├── views/             # Main views
│   │       │   │   ├── LoginView.java
│   │       │   │   ├── ResidentDashboard.java
│   │       │   │   └── AdminDashboard.java
│   │       │   └── components/        # Reusable UI components
│   │       │       ├── PinInputComponent.java
│   │       │       ├── UITheme.java
│   │       │       └── NotificationService.java
│   │       ├── service/               # Business logic layer
│   │       │   └── CitizenCardService.java
│   │       ├── dao/                   # Data access layer
│   │       │   ├── ResidentDAO.java
│   │       │   ├── TransactionDAO.java
│   │       │   ├── InvoiceDAO.java
│   │       │   └── ParkingDAO.java
│   │       ├── model/                 # Domain models (chỉ 1 bộ)
│   │       │   ├── Resident.java
│   │       │   ├── Transaction.java
│   │       │   ├── Invoice.java
│   │       │   └── Parking.java
│   │       ├── card/                  # Smartcard communication layer
│   │       │   ├── CardService.java   # Xử lý các INS commands
│   │       │   └── RealCardClient.java # javax.smartcardio client
│   │       ├── database/              # Database management
│   │       │   └── DatabaseManager.java
│   │       ├── validation/            # Validation logic
│   │       │   └── ValidationService.java
│   │       └── util/                  # Utilities
│   │           └── ModelConverter.java
│   ├── data/                   # Data files (auto-generated)
│   │   └── citizen_card.db     # SQLite database
│   └── pom.xml
├── jcardsim-applet/           # JavaCard applet cho JCIDE
│   └── src/citizen/
│       └── citizen.java      # Applet code (with RSA encryption)
└── README.md
```

**Lưu ý:**
- ✅ **Cấu trúc theo chuẩn Layered Architecture** - rõ ràng, dễ maintain
- ✅ **Chỉ 1 bộ model** - không trùng lặp
- ✅ Database file `citizen_card.db` nằm trong thư mục `desktop/data/`
- ✅ Tất cả code chạy trong cùng một module, không cần HTTP server
- 🔐 **Applet hỗ trợ RSA encryption** - Private key được lưu trong thẻ

## 🚀 Hướng dẫn chạy

### Yêu cầu
- Java 17+ (khuyến nghị JDK 21)
- IntelliJ IDEA (Community hoặc Ultimate)
- JCIDE (để chạy JavaCard applet)
- Maven 3.6+ (tích hợp sẵn trong IntelliJ - không cần cài riêng)

### Bước 1: Chạy JCIDE và Load Applet

1. Mở **JCIDE**
2. Load applet từ `jcardsim-applet/src/citizen/citizen.java`
3. **Build** project (Ctrl+B)
4. **Debug/Run** applet (F11) để mở terminal
5. **Quan trọng**: Đảm bảo terminal đang mở trong JCIDE

**Lưu ý**: Terminal phải được mở trong JCIDE để Desktop App kết nối trực tiếp (không cần JCardSimServer hay server mô phỏng nào khác).

### Bước 2: Chạy Desktop App

**Trong IntelliJ:**
1. Mở project: `File → Open → citizen_card/desktop`
2. IntelliJ tự động import Maven project
3. Chạy: `Run → Desktop App` (hoặc `Shift+F10`)
4. ✅ **Xong!** - Không cần chạy Maven commands

Desktop App sẽ:
- Tự động khởi tạo Backend service (trong cùng module)
- Tự động khởi tạo Database (nếu chưa có)
- Tự động quét và kết nối với JCIDE terminal
- Hiển thị màn hình đăng nhập

**Lưu ý:**
- ✅ **KHÔNG cần** chạy Maven commands qua terminal
- ✅ **KHÔNG cần** mở port nào (chỉ local)
- ✅ IntelliJ tự động build và chạy

### Sử dụng

**Đăng nhập Cư dân:**
1. Click **"👤 Đăng nhập Cư dân"**
2. Hệ thống tự động quét và kết nối với JCIDE terminal
3. Tự động SELECT applet và đọc Card ID
4. Nhập PIN khi được yêu cầu
5. Vào Dashboard cư dân

**Đăng nhập Admin:**
1. Click **"🔐 Đăng nhập Admin"**
2. Vào Dashboard admin trực tiếp

## 📝 Lưu ý về Kiến trúc

Hệ thống sử dụng **Local Communication** - Desktop App gọi trực tiếp Backend service methods trong cùng JVM, **KHÔNG qua HTTP REST API**. Điều này giúp:
- ✅ **KHÔNG cần HTTP server** (không cần mở port 8080 hay bất kỳ port nào)
- ✅ **KHÔNG cần backend server riêng** - tất cả code backend được tích hợp trong desktop module
- ✅ Desktop App gọi **trực tiếp database** qua DAO classes (không qua API)
- ✅ Nhanh hơn (không có network overhead)
- ✅ Đơn giản hơn (direct method calls trong cùng JVM)

**Kết nối với JCIDE terminal** qua **javax.smartcardio** (ISO 7816 T=1 protocol).

### 🔄 Luồng dữ liệu

```
Desktop UI → CitizenCardService → DAO Classes → SQLite Database
                ↓
         RealCardClient → javax.smartcardio → JCIDE Terminal
```

**Tất cả đều chạy trong cùng một JVM process, không có network communication.**

### 💾 Lưu trữ Dữ liệu

**JCIDE Terminal (JavaCard Applet):**
- Dữ liệu thẻ được lưu trong applet (persistent trong card memory)
- Dữ liệu được lưu khi applet chạy trong JCIDE
- Dữ liệu chỉ mất khi **reload applet** trong JCIDE

**SQLite Database:**
- Lưu trong file `desktop/data/citizen_card.db` (persistent)
- **Không mất** khi restart Desktop App
- Dữ liệu cư dân, giao dịch, hóa đơn được lưu vĩnh viễn
- Tự động tạo khi chạy lần đầu (dựa trên `database/schema.sql`)
- **Không lưu PIN** - PIN được xác thực trực tiếp bởi thẻ
- **Lưu Public Key** - để mã hóa dữ liệu gửi đến thẻ

### Service Methods

Desktop App gọi trực tiếp các methods trong `CitizenCardService` (trong cùng module):
- `loginByCard()` - Đăng nhập bằng thẻ
- `verifyPin(cardId, pin)` - Xác thực PIN
- `topUp(cardId, amount)` - Nạp tiền
- `payInvoice(cardId, invoiceId)` - Thanh toán hóa đơn
- `initializeCard(...)` - Khởi tạo thẻ (Admin)
- Và nhiều methods khác...

Xem `desktop/src/main/java/com/citizencard/service/CitizenCardService.java` để biết đầy đủ các methods.

### ❓ Tại sao không cần Backend Server?

Vì chạy **hoàn toàn local**, Desktop App có thể:
- Gọi trực tiếp `CitizenCardService` methods (trong cùng JVM)
- Truy cập trực tiếp SQLite database qua JDBC (file-based, không cần server)
- Kết nối trực tiếp với JCIDE terminal qua `javax.smartcardio` (local device)

**Không cần HTTP API** vì không có network communication giữa các components.

## 📋 INS Commands

`CardService` sử dụng các INS code sau để giao tiếp với JavaCard applet trong JCIDE:

| INS | Chức năng |
|-----|-----------|
| A4  | SELECT APPLET |
| 29  | CHECK CARD CREATED |
| 18  | CLEAR CARD |
| 20  | UPDATE CUSTOMER INFO |
| 13  | GET CUSTOMER INFO |
| 14  | GET BALANCE |
| 16  | UPDATE BALANCE |
| 26  | UPDATE CARD ID |
| 27  | GET CARD ID |
| 21  | UPDATE PIN |
| 24  | VERIFY PIN |
| 25  | UNBLOCK PIN |
| 28  | CHECK PIN STATUS |
| 22  | UPDATE PICTURE |
| 23  | GET PICTURE |
| 2A  | GET PUBLIC KEY (RSA) |

## 🔐 Protocol

Sử dụng **ISO 7816 T=1** protocol qua `javax.smartcardio`:
- **SELECT APPLET**: `00 A4 04 00 [AID length] [AID]`
- **APDU Commands**: `CLA INS P1 P2 [Lc] [Data] [Le]`
- **Response**: `[Data] SW1 SW2` (0x9000 = success)

Xem file `QUICK_START.md` để biết hướng dẫn nhanh về cách chạy với JCIDE.

## 📊 Database Schema

Schema database được lưu trong `desktop/src/main/resources/database/schema.sql` và được tự động load khi khởi tạo database.

## 💻 Tài liệu Hướng dẫn

**Xem file `QUICK_START.md`** để chạy nhanh (3 bước đơn giản)

**Xem file `HOW_TO_VIEW_DATABASE.md`** để biết cách xem và quản lý dữ liệu trong SQLite database

**Các file hướng dẫn khác:**
- `HUONG_DAN_KHOI_TAO_THE.md` - Hướng dẫn khởi tạo thẻ
- `HUONG_DAN_VALIDATION.md` - Hướng dẫn về validation
- `HUONG_DAN_XAC_DINH_CU_DAN.md` - Hướng dẫn xác định cư dân

## ❓ Câu Hỏi Thường Gặp

### Có cần Maven không?
- ✅ **Có** - để quản lý dependencies (JavaFX, SQLite không có sẵn trong JDK)
- ✅ **Nhưng KHÔNG cần chạy Maven commands** - IntelliJ tự động xử lý
- ✅ **Chỉ cần click Run** - IntelliJ tự động build

### Có cần mở port hoặc chạy JCardSimServer không?
- ❌ **KHÔNG cần mở port nào, KHÔNG cần JCardSimServer**
- ✅ Chỉ cần **JCIDE terminal** (local, không qua network)
- ✅ Tất cả chạy local trong cùng máy

## 🐛 Troubleshooting

### Desktop App không kết nối được JCIDE terminal

- Kiểm tra JCIDE đang chạy và applet đã được load
- Kiểm tra terminal đã được mở trong JCIDE (F11 để mở terminal)
- Kiểm tra thẻ đã được "insert" vào terminal trong JCIDE
- Kiểm tra applet đã được SELECT thành công (xem console logs)
- Đảm bảo sử dụng protocol T=1 (đã được cấu hình sẵn)

### Desktop app không chạy được

- Kiểm tra Maven dependencies đã được resolve
- Kiểm tra JCIDE đang chạy và terminal đã mở
- Xem logs trong console để biết lỗi cụ thể

### Lỗi database

- Database sẽ tự động tạo khi chạy lần đầu
- File database: `desktop/data/citizen_card.db`
- Nếu cần reset database, xóa file `desktop/data/citizen_card.db` và chạy lại app
- Xem `HOW_TO_VIEW_DATABASE.md` để biết cách xem dữ liệu trong database

## 📝 License

MIT License

