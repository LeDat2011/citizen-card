# Hệ thống Quản lý Thẻ Cư dân

Hệ thống quản lý thẻ cư dân sử dụng **javax.smartcardio** để kết nối với **JCIDE terminal**, với backend Java và Desktop app JavaFX.

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
              │ (ISO 7816 T=0)
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
│   │       ├── desktop/                # Desktop UI
│   │       │   ├── MainApp.java        # JavaFX main
│   │       │   ├── model/              # Desktop data models
│   │       │   ├── util/               # Utilities (ModelConverter)
│   │       │   └── ui/                 # UI components
│   │       └── backend/                # Backend service (integrated)
│   │           ├── RealCardClient.java # javax.smartcardio client
│   │           ├── CardService.java   # Xử lý các INS commands
│   │           ├── dao/                # Data Access Objects
│   │           ├── database/           # Database manager
│   │           ├── model/              # Backend data models
│   │           └── service/            # Business logic
│   └── pom.xml
├── jcardsim-applet/            # JavaCard applet cho JCIDE
│   └── src/citizen/
│       └── citizen.java        # Applet code
├── database/
│   └── schema.sql              # Database schema
└── README.md
```

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

**Lưu ý**: Terminal phải được mở trong JCIDE để Desktop App có thể kết nối.

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

Hệ thống sử dụng **Local Communication** - Desktop App gọi trực tiếp Backend service methods trong cùng JVM, không qua HTTP REST API. Điều này giúp:
- ✅ Không cần HTTP server (port 8080)
- ✅ Nhanh hơn (không có network overhead)
- ✅ Đơn giản hơn (direct method calls)

Kết nối với JCIDE terminal qua **javax.smartcardio** (ISO 7816 T=0 protocol).

### 💾 Lưu trữ Dữ liệu

**JCIDE Terminal (JavaCard Applet):**
- Dữ liệu thẻ được lưu trong applet (persistent trong card memory)
- Dữ liệu được lưu khi applet chạy trong JCIDE
- Dữ liệu chỉ mất khi **reload applet** trong JCIDE

**SQLite Database:**
- Lưu trong file `citizen_card.db` (persistent)
- **Không mất** khi restart Desktop App hoặc Backend
- Dữ liệu cư dân, giao dịch, hóa đơn được lưu vĩnh viễn

### Service Methods

Desktop App gọi trực tiếp các methods trong `CitizenCardService`:
- `loginByCard()` - Đăng nhập bằng thẻ
- `verifyPin(cardId, pin)` - Xác thực PIN
- `topUp(cardId, amount)` - Nạp tiền
- `payInvoice(cardId, invoiceId)` - Thanh toán hóa đơn
- `initializeCard(...)` - Khởi tạo thẻ (Admin)
- Và nhiều methods khác...

Xem `backend/src/main/java/com/citizencard/backend/service/CitizenCardService.java` để biết đầy đủ các methods.

## 📋 INS Commands

Backend sử dụng các INS code sau để giao tiếp với JavaCard applet trong JCIDE:

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
| 22  | UPDATE PICTURE |
| 23  | GET PICTURE |

## 🔐 Protocol

Sử dụng **ISO 7816 T=0** protocol qua `javax.smartcardio`:
- **SELECT APPLET**: `00 A4 04 00 [AID length] [AID]`
- **APDU Commands**: `CLA INS P1 P2 [Lc] [Data] [Le]`
- **Response**: `[Data] SW1 SW2` (0x9000 = success)

Xem file `docs/JCIDE_CONNECTION_GUIDE.md` để biết chi tiết về kết nối với JCIDE terminal.

## 📊 Database Schema

Xem file `database/schema.sql` để biết chi tiết cấu trúc database.

## 💻 Chạy Trong IntelliJ IDEA

**Xem file `QUICK_START.md`** để chạy nhanh (3 bước đơn giản)

**Xem file `HOW_TO_RUN_INTELLIJ.md`** để biết hướng dẫn chi tiết:
- Cấu hình JDK và Maven
- Tạo Run Configurations
- Chạy Desktop App với JCIDE terminal
- Troubleshooting các lỗi thường gặp

**Xem file `docs/JCIDE_APPLET_SETUP.md`** để biết cách setup applet trong JCIDE.

## ❓ Câu Hỏi Thường Gặp

### Có cần Maven không?
- ✅ **Có** - để quản lý dependencies (JavaFX, SQLite không có sẵn trong JDK)
- ✅ **Nhưng KHÔNG cần chạy Maven commands** - IntelliJ tự động xử lý
- ✅ **Chỉ cần click Run** - IntelliJ tự động build

### Có cần mở port không?
- ❌ **KHÔNG cần mở port nào**
- ✅ Chỉ cần **JCIDE terminal** (local, không qua network)
- ✅ Tất cả chạy local trong cùng máy

## 🐛 Troubleshooting

### Backend không kết nối được JCIDE terminal

- Kiểm tra JCIDE đang chạy và applet đã được load
- Kiểm tra terminal đã được mở trong JCIDE
- Kiểm tra thẻ đã được "insert" vào terminal trong JCIDE
- Xem `docs/JCIDE_CONNECTION_GUIDE.md` để biết chi tiết

### Desktop app không chạy được

- Kiểm tra Maven dependencies đã được resolve
- Kiểm tra JCIDE đang chạy và terminal đã mở
- Xem logs trong console để biết lỗi cụ thể

### Lỗi database

- Database sẽ tự động tạo khi chạy lần đầu
- File database: `citizen_card.db` trong thư mục backend

## 📝 License

MIT License

