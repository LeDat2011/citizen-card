# Desktop Application - Maven Project

## ✅ Đây là Maven Project

Project này đã được cấu hình như một **Maven project** với file `pom.xml`.

## 📋 Cấu trúc Maven Project

```
desktop/
├── pom.xml                    # ✅ Maven configuration file
├── src/
│   └── main/
│       └── java/              # Java source code
│           └── com/citizencard/
│               ├── desktop/   # Desktop UI code
│               └── backend/   # Backend service code (integrated)
└── target/                    # Build output (tự động tạo)
```

## 🔧 Cách IntelliJ Nhận Diện Maven Project

### Tự động:
1. IntelliJ tự động phát hiện file `pom.xml`
2. Hiển thị popup: **"Maven projects need to be imported"**
3. Click **Import Maven Projects** → IntelliJ tự động setup

### Thủ công (nếu cần):
1. **File → Project Structure** (`Ctrl+Alt+Shift+S`)
2. Tab **Modules**
3. Click **+** → **Import Module**
4. Chọn file `pom.xml` trong thư mục `desktop`
5. Click **OK**

## ✅ Kiểm Tra Maven Project

### Trong IntelliJ:
- Xem tab **Maven** ở dưới màn hình (hoặc `Alt+3`)
- Sẽ thấy module: `citizen-card-desktop`
- Có thể mở rộng để xem dependencies

### Trong Project View:
- Thư mục `desktop` sẽ có icon Maven (chữ "M" hoặc icon đặc biệt)
- Có thể thấy **External Libraries** chứa các dependencies

## 🚀 Chạy Maven Project

### Cách 1: Chạy trong IntelliJ (Khuyến nghị)
1. **Run → Edit Configurations...**
2. Click **+** → **Application**
3. Cấu hình:
   - **Name**: `Desktop App`
   - **Main class**: `com.citizencard.desktop.MainApp`
   - **Module**: `citizen-card-desktop`
4. Click **Run** (▶️)

### Cách 2: Chạy qua Maven (Terminal)
```bash
cd desktop
mvn clean javafx:run
```

## 📦 Dependencies

Maven tự động quản lý các dependencies:
- **JavaFX** (UI framework)
- **SQLite JDBC** (Database)
- **Jackson** (JSON processing)
- **SLF4J** (Logging)

Xem file `pom.xml` để biết chi tiết.

## ⚠️ Lưu ý

- ✅ **KHÔNG cần** cài Maven riêng - IntelliJ có Maven tích hợp sẵn
- ✅ **KHÔNG cần** chạy Maven commands - IntelliJ tự động build khi Run
- ✅ Chỉ cần click **Run** trong IntelliJ là đủ








