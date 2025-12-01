# 🔧 Hướng Dẫn Setup Maven Project

## ✅ Project Đã Là Maven Project

Thư mục `desktop/` đã được cấu hình như một **Maven project** với file `pom.xml`.

## 📋 Kiểm Tra Maven Project

### 1. Kiểm tra file `pom.xml` có tồn tại:
```
desktop/
└── pom.xml  ✅ (File này xác định đây là Maven project)
```

### 2. Kiểm tra trong IntelliJ:

**Cách 1: Xem tab Maven**
- **View → Tool Windows → Maven** (`Alt+3`)
- Sẽ thấy module: `citizen-card-desktop`
- Có thể mở rộng để xem:
  - **Dependencies** (JavaFX, SQLite, etc.)
  - **Lifecycle** (clean, compile, install, etc.)

**Cách 2: Xem Project Structure**
- **File → Project Structure** (`Ctrl+Alt+Shift+S`)
- Tab **Modules**
- Sẽ thấy module: `citizen-card-desktop`
- Tab **Libraries**
- Sẽ thấy các dependencies đã được tải

**Cách 3: Xem Project View**
- Thư mục `desktop` sẽ có icon Maven (chữ "M" hoặc icon đặc biệt)
- Có thể thấy **External Libraries** chứa các dependencies

## 🔄 Nếu IntelliJ Chưa Nhận Diện Maven Project

### Cách 1: Import Maven Project (Tự động)
1. **File → Open** → Chọn thư mục `desktop`
2. IntelliJ sẽ tự động phát hiện `pom.xml`
3. Click **Import Maven Projects** nếu có popup

### Cách 2: Add Maven Project (Thủ công)
1. **File → Project Structure** (`Ctrl+Alt+Shift+S`)
2. Tab **Modules**
3. Click **+** → **Import Module**
4. Chọn file `pom.xml` trong thư mục `desktop`
5. Click **OK**
6. Chọn **Import Maven project**
7. Click **Next** → **Finish**

### Cách 3: Reload Maven Project
1. **View → Tool Windows → Maven** (`Alt+3`)
2. Click icon **Reload All Maven Projects** (🔄)
3. Hoặc right-click vào module → **Reload project**

## ✅ Sau Khi Import Thành Công

Bạn sẽ thấy:
- ✅ Module `citizen-card-desktop` trong tab Maven
- ✅ **External Libraries** chứa các dependencies
- ✅ Có thể chạy Maven goals (clean, compile, install, etc.)

## 🚀 Chạy Project

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

Maven tự động quản lý các dependencies (xem `pom.xml`):
- **JavaFX** (UI framework)
- **SQLite JDBC** (Database)
- **Jackson** (JSON processing)
- **SLF4J** (Logging)

## ⚠️ Lưu Ý

- ✅ **KHÔNG cần** cài Maven riêng - IntelliJ có Maven tích hợp sẵn
- ✅ **KHÔNG cần** chạy Maven commands - IntelliJ tự động build khi Run
- ✅ Chỉ cần click **Run** trong IntelliJ là đủ
- ✅ Dependencies tự động tải từ Maven Central Repository






