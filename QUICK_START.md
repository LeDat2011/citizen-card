# 🚀 Quick Start - Chạy Nhanh

## ✅ Chỉ cần 3 bước:

### 1. Mở Maven Project trong IntelliJ
- **File → Open** → Chọn thư mục `citizen_card/desktop` (thư mục chứa `pom.xml`)
- IntelliJ tự động nhận diện đây là **Maven project**
- Nếu thấy popup **"Maven projects need to be imported"**, click **Import Maven Projects**
- Đợi dependencies tải xong (lần đầu tiên có thể mất vài phút)
- Kiểm tra tab **Maven** (`Alt+3`) để thấy module `citizen-card-desktop`

### 2. Chạy JCIDE
- Mở **JCIDE**
- Load applet: `jcardsim-applet/src/citizen/citizen.java`
- **Build** (Ctrl+B)
- **Debug/Run** (F11) để mở terminal
- ✅ **Quan trọng**: Terminal phải đang mở

### 3. Chạy Desktop App
- Trong IntelliJ: **Run → Desktop App** (hoặc `Shift+F10`)
- ✅ **Xong!** - Chỉ cần click Run

## ❓ Về Maven:

### Maven vẫn cần vì:
- ✅ Quản lý dependencies (JavaFX, SQLite không có sẵn trong JDK)
- ✅ Build project (compile Java code)

### Nhưng KHÔNG cần:
- ❌ Chạy `mvn clean install` qua terminal
- ❌ Cấu hình Maven thủ công
- ❌ Quan tâm đến Maven commands

**IntelliJ tự động xử lý Maven** - bạn chỉ cần click Run!

## ❓ Về Port:

- ❌ **KHÔNG cần mở port nào**
- ✅ Chỉ cần **JCIDE terminal** (local, không qua network)
- ✅ Kết nối qua **javax.smartcardio** (local communication)

## 📝 Tóm Tắt:

| Việc cần làm | Cách làm |
|-------------|----------|
| Mở project | IntelliJ → Open → `desktop` folder |
| Chạy JCIDE | JCIDE → Load applet → Run → Terminal mở |
| Chạy Desktop App | IntelliJ → Run → Xong! |
| Maven | IntelliJ tự động (không cần làm gì) |
| Port | Không cần (chỉ local) |

## ⚠️ Lưu ý:

- **Lần đầu tiên**: IntelliJ sẽ tải dependencies (mất vài phút)
- **Sau đó**: Chỉ cần click Run là xong
- **JCIDE terminal phải mở** trước khi chạy Desktop App

