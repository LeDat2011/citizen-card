# Cách xem file Database SQLite

## 📍 Vị trí file database

File database được tạo với tên: **`citizen_card.db`**

**Vị trí hiện tại của file:**
```
D:\citizen_card\data\citizen_card.db
```

File này được tạo tự động trong thư mục `data/` ở root của project khi bạn chạy ứng dụng lần đầu.

## 🔍 Cách tìm file database

### Trên Windows (PowerShell):
```powershell
# Tìm file trong toàn bộ project
Get-ChildItem -Path . -Filter "citizen_card.db" -Recurse -ErrorAction SilentlyContinue

# Hoặc tìm trong thư mục desktop
Get-ChildItem -Path desktop -Filter "citizen_card.db" -Recurse -ErrorAction SilentlyContinue
```

### Trên Windows (Command Prompt):
```cmd
dir /s citizen_card.db
```

## 🛠️ Cách xem nội dung database

### Cách 1: Sử dụng DB Browser for SQLite (Khuyến nghị)

1. **Tải DB Browser for SQLite:**
   - Truy cập: https://sqlitebrowser.org/
   - Tải và cài đặt

2. **Mở file database:**
   - Mở DB Browser for SQLite
   - Click "Open Database"
   - Chọn file `citizen_card.db`
   - Bạn sẽ thấy tất cả tables và dữ liệu

3. **Xem dữ liệu:**
   - Click vào tab "Browse Data"
   - Chọn table muốn xem (residents, transactions, parking)
   - Dữ liệu sẽ hiển thị dưới dạng bảng

### Cách 2: Sử dụng SQLite Command Line

1. **Tải SQLite CLI:**
   - Tải từ: https://www.sqlite.org/download.html
   - Hoặc sử dụng SQLite có sẵn trong hệ thống

2. **Mở database:**
   ```bash
   sqlite3 citizen_card.db
   ```

3. **Xem dữ liệu:**
   ```sql
   -- Xem tất cả tables
   .tables
   
   -- Xem cấu trúc table
   .schema residents
   
   -- Xem dữ liệu trong table residents
   SELECT * FROM residents;
   
   -- Xem dữ liệu trong table transactions
   SELECT * FROM transactions;
   
   -- Xem dữ liệu trong table parking
   SELECT * FROM parking;
   
   -- Thoát
   .exit
   ```

### Cách 3: Sử dụng IntelliJ IDEA Database Tool

1. **Mở Database Tool Window:**
   - View → Tool Windows → Database
   - Hoặc nhấn `Alt + 1` rồi chọn Database

2. **Thêm SQLite Data Source:**
   - Click dấu `+` → Data Source → SQLite
   - Chọn file `citizen_card.db`
   - Click OK

3. **Xem dữ liệu:**
   - Mở rộng database connection
   - Click vào table muốn xem
   - Dữ liệu sẽ hiển thị trong tab mới

### Cách 4: Sử dụng VS Code Extension

1. **Cài đặt extension:**
   - Mở VS Code
   - Cài đặt extension "SQLite Viewer" hoặc "SQLite"

2. **Mở file database:**
   - Click chuột phải vào file `citizen_card.db`
   - Chọn "Open Database"

## ⚠️ Lưu ý quan trọng

**Dữ liệu đã được mã hóa!**

Tất cả dữ liệu trong database đã được mã hóa bằng AES với khóa từ PIN. Khi bạn xem trực tiếp trong database, bạn sẽ thấy:

- ❌ Dữ liệu đã mã hóa (các chuỗi Base64 dài)
- ❌ Không thể đọc được thông tin gốc

**Ví dụ:**
```
full_name: "U2FsdGVkX1+abc123..." (đã mã hóa)
phone_number: "U2FsdGVkX1+xyz789..." (đã mã hóa)
```

Để xem dữ liệu gốc, bạn cần:
1. Sử dụng ứng dụng Desktop (đã có logic giải mã)
2. Hoặc giải mã thủ công bằng PIN (không khuyến nghị)

## 🔐 Giải mã dữ liệu (nếu cần)

Nếu bạn muốn xem dữ liệu gốc từ database, bạn cần:
1. Biết PIN của user
2. Sử dụng `EncryptionService.decryptWithAES()` với PIN đó

**Không khuyến nghị** vì lý do bảo mật.

## 📝 Kiểm tra nhanh

Chạy lệnh này để tìm file database:
```powershell
Get-ChildItem -Path . -Filter "citizen_card.db" -Recurse -ErrorAction SilentlyContinue | Select-Object FullName
```

