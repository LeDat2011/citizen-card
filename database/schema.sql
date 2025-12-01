
-- Bảng cư dân (residents)
CREATE TABLE IF NOT EXISTS residents (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    card_id TEXT UNIQUE NOT NULL,           -- ID thẻ (16 bytes hex)
    full_name TEXT NOT NULL,                -- Họ tên đầy đủ
    date_of_birth TEXT NOT NULL,            -- Ngày sinh (YYYY-MM-DD)
    room_number TEXT NOT NULL,              -- Số phòng/căn hộ
    phone_number TEXT,                      -- Số điện thoại
    email TEXT,                             -- Email
    id_number TEXT,                         -- Số CMND/CCCD
    balance INTEGER DEFAULT 0,              -- Số dư trong thẻ (VND)
    pin_hash TEXT,                          -- Hash của PIN (để backup)
    photo_path TEXT                         -- Ảnh đại diện (base64 string ngắn hoặc path)
);

-- Bảng giao dịch (transactions) - Gộp cả invoices
)
CREATE TABLE IF NOT EXISTS transactions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    resident_id INTEGER NOT NULL,
    card_id TEXT NOT NULL,
    transaction_type TEXT NOT NULL,         -- TOPUP, PAYMENT, INVOICE
    amount INTEGER NOT NULL,                -- Số tiền (VND)
    balance_after INTEGER NOT NULL,         -- Số dư sau giao dịch
    payment_status TEXT,                    -- NULL, PENDING, PAID (chỉ cho INVOICE)
    service_name TEXT,                      -- Tên dịch vụ (cho INVOICE)
    description TEXT,                       -- Mô tả: "Nạp tiền", "Thanh toán hóa đơn", etc.
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (resident_id) REFERENCES residents(id) ON DELETE CASCADE
);

-- Bảng gửi xe (parking)
CREATE TABLE IF NOT EXISTS parking (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    resident_id INTEGER NOT NULL,
    license_plate TEXT NOT NULL,            -- Biển số xe
    vehicle_type TEXT NOT NULL,             -- Loại xe: MOTORBIKE, CAR, BICYCLE
    monthly_fee INTEGER DEFAULT 200000,     -- Phí tháng (VND)
    expired_date DATE,                      -- Ngày hết hạn đăng ký
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (resident_id) REFERENCES residents(id) ON DELETE CASCADE
);

-- INDEXES - Tối thiểu cho performance
CREATE INDEX IF NOT EXISTS idx_residents_card_id ON residents(card_id);
CREATE INDEX IF NOT EXISTS idx_transactions_resident ON transactions(resident_id);
CREATE INDEX IF NOT EXISTS idx_transactions_card ON transactions(card_id);
CREATE INDEX IF NOT EXISTS idx_transactions_type ON transactions(transaction_type);
CREATE INDEX IF NOT EXISTS idx_transactions_status ON transactions(payment_status);
CREATE INDEX IF NOT EXISTS idx_parking_resident ON parking(resident_id);

-- =====================================================
-- Dữ liệu mẫu: Tạo sẵn 1 user mặc định
-- Vì hệ thống chỉ load được 1 applet = 1 user
-- Admin và User đều dùng cùng 1 resident này
-- =====================================================
-- Chèn user mặc định nếu chưa có
INSERT OR IGNORE INTO residents (id, card_id, full_name, date_of_birth, room_number, 
                                 phone_number, email, id_number, balance, pin_hash, photo_path)
VALUES (1, 'CARD001', 'Nguyễn Văn A', '1990-01-01', '101', 
        '0901234567', 'nguyenvana@example.com', '001234567890', 0, '123456', NULL);
