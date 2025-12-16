# 🗄️ Hướng dẫn xem Database H2

## 📋 Tổng quan
Hệ thống Citizen Card sử dụng H2 Database để lưu trữ thông tin thẻ và giao dịch. Có nhiều cách để xem và quản lý database.

## 🌐 Cách 1: H2 Web Console (Khuyến nghị)

### Từ ứng dụng:
1. Mở ứng dụng Citizen Card
2. Đăng nhập với quyền Admin
3. Chọn "🗄️ Cơ sở dữ liệu" từ menu
4. Nhấn "🌐 Mở Web Console"

### Từ script:
```bash
# Windows
view-database.bat

# Linux/Mac
./view-database.sh
```

### Truy cập trực tiếp:
1. Chạy ứng dụng để khởi tạo database
2. Mở browser: http://localhost:8082
3. Thông tin kết nối:
   - **JDBC URL**: `jdbc:h2:file:./desktop/data/citizen_card`
   - **User Name**: (để trống)
   - **Password**: (để trống)

## 📊 Cách 2: Console Output

### Từ ứng dụng:
1. Chọn "📊 In ra Console" trong Database Viewer
2. Kiểm tra console/terminal để xem kết quả

### Từ command line:
```bash
cd desktop
java -cp "target/classes:target/dependency/*" citizencard.util.DatabaseViewer print
```

## 📈 Cách 3: Database Statistics

### Xem thống kê:
```bash
cd desktop
java -cp "target/classes:target/dependency/*" citizencard.util.DatabaseViewer stats
```

## 🗂️ Cấu trúc Database

### Bảng REGISTERED_CARDS:
```sql
CREATE TABLE registered_cards (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    card_id TEXT UNIQUE NOT NULL,
    public_key TEXT NOT NULL,
    card_status TEXT DEFAULT 'ACTIVE',
    registered_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    last_accessed DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

### Bảng TRANSACTION_LOGS:
```sql
CREATE TABLE transaction_logs (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    card_id TEXT NOT NULL,
    operation_type TEXT NOT NULL,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    success BOOLEAN DEFAULT TRUE,
    error_message TEXT
);
```

## 🔍 Các SQL Query hữu ích

### Xem tất cả thẻ đã đăng ký:
```sql
SELECT * FROM registered_cards ORDER BY registered_at DESC;
```

### Xem giao dịch gần nhất:
```sql
SELECT * FROM transaction_logs ORDER BY timestamp DESC LIMIT 10;
```

### Thống kê thẻ theo trạng thái:
```sql
SELECT card_status, COUNT(*) as count 
FROM registered_cards 
GROUP BY card_status;
```

### Tỷ lệ thành công giao dịch:
```sql
SELECT 
    operation_type,
    COUNT(*) as total,
    SUM(CASE WHEN success = TRUE THEN 1 ELSE 0 END) as successful,
    ROUND(SUM(CASE WHEN success = TRUE THEN 1 ELSE 0 END) * 100.0 / COUNT(*), 2) as success_rate
FROM transaction_logs 
GROUP BY operation_type;
```

## 🛠️ Troubleshooting

### Lỗi "Database not found":
- Chạy ứng dụng ít nhất 1 lần để tạo database
- Kiểm tra thư mục `desktop/data/` có file `citizen_card.mv.db`

### Lỗi "Port 8082 already in use":
- Đóng H2 Console khác đang chạy
- Hoặc thay đổi port trong DatabaseViewer.java

### Lỗi "Connection refused":
- Đảm bảo database path đúng
- Kiểm tra quyền truy cập file

## 📁 File Locations

- **Database files**: `desktop/data/citizen_card.*`
- **DatabaseViewer**: `desktop/src/main/java/citizencard/util/DatabaseViewer.java`
- **Scripts**: `view-database.bat` (Windows), `view-database.sh` (Linux/Mac)

## 🔒 Bảo mật

- Database chỉ lưu `card_id` và `public_key`
- Không lưu thông tin cá nhân nhạy cảm
- Dữ liệu thực tế được lưu trong smart card
- H2 Console chỉ nên dùng trong môi trường development