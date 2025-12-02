-- Bảng cư dân (residents)
CREATE TABLE IF NOT EXISTS residents (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    card_id TEXT UNIQUE NOT NULL,
    full_name TEXT NOT NULL,
    date_of_birth TEXT NOT NULL,
    room_number TEXT NOT NULL,
    phone_number TEXT,
    email TEXT,
    id_number TEXT UNIQUE,
    balance INTEGER DEFAULT 0,
    photo_path TEXT,
    status TEXT DEFAULT 'ACTIVE',
    pin_hash TEXT,
    public_key TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'BLOCKED'))
);

-- Bảng gửi xe đơn giản (parking_simple)
CREATE TABLE IF NOT EXISTS parking_simple (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    resident_id INTEGER NOT NULL,
    license_plate TEXT NOT NULL,
    vehicle_type TEXT NOT NULL,
    action_type TEXT NOT NULL,
    monthly_fee INTEGER DEFAULT 200000,
    expired_date DATE,
    gate_location TEXT,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    notes TEXT,
    FOREIGN KEY (resident_id) REFERENCES residents(id) ON DELETE CASCADE,
    CONSTRAINT chk_vehicle_type CHECK (vehicle_type IN ('MOTORBIKE', 'CAR', 'BICYCLE')),
    CONSTRAINT chk_action_type CHECK (action_type IN ('REGISTER', 'CHECK_IN', 'CHECK_OUT'))
);

-- Bảng hóa đơn đơn giản (invoices_simple)
CREATE TABLE IF NOT EXISTS invoices_simple (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    resident_id INTEGER NOT NULL,
    service_name TEXT NOT NULL,
    service_code TEXT NOT NULL,
    amount INTEGER NOT NULL,
    payment_status TEXT DEFAULT 'PENDING',
    invoice_date DATE NOT NULL,
    payment_date DATETIME,
    description TEXT,
    FOREIGN KEY (resident_id) REFERENCES residents(id) ON DELETE CASCADE,
    CONSTRAINT chk_payment_status CHECK (payment_status IN ('PENDING', 'PAID'))
);

-- Bảng giao dịch đơn giản (transactions_simple)
CREATE TABLE IF NOT EXISTS transactions_simple (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    resident_id INTEGER NOT NULL,
    card_id TEXT NOT NULL,
    transaction_type TEXT NOT NULL,
    amount INTEGER NOT NULL,
    balance_before INTEGER NOT NULL,
    balance_after INTEGER NOT NULL,
    description TEXT,
    reference_id INTEGER,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (resident_id) REFERENCES residents(id) ON DELETE CASCADE,
    CONSTRAINT chk_transaction_type CHECK (transaction_type IN ('TOPUP', 'PAYMENT', 'DEBIT'))
);

-- INDEXES
CREATE INDEX IF NOT EXISTS idx_residents_card_id ON residents(card_id);
CREATE INDEX IF NOT EXISTS idx_residents_room ON residents(room_number);
CREATE INDEX IF NOT EXISTS idx_parking_resident ON parking_simple(resident_id);
CREATE INDEX IF NOT EXISTS idx_parking_license ON parking_simple(license_plate);
CREATE INDEX IF NOT EXISTS idx_invoices_resident ON invoices_simple(resident_id);
CREATE INDEX IF NOT EXISTS idx_invoices_status ON invoices_simple(payment_status);
CREATE INDEX IF NOT EXISTS idx_transactions_resident ON transactions_simple(resident_id);
CREATE INDEX IF NOT EXISTS idx_transactions_card ON transactions_simple(card_id);

-- Migration: add missing columns for legacy databases
ALTER TABLE residents ADD COLUMN status TEXT DEFAULT 'ACTIVE';
ALTER TABLE residents ADD COLUMN created_at DATETIME DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE residents ADD COLUMN updated_at DATETIME DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE transactions_simple ADD COLUMN balance_before INTEGER DEFAULT 0;
ALTER TABLE transactions_simple ADD COLUMN reference_id INTEGER;
ALTER TABLE residents ADD COLUMN public_key TEXT;

-- TRIGGER đồng bộ số dư
CREATE TRIGGER IF NOT EXISTS trg_sync_balance
    AFTER INSERT ON transactions_simple
BEGIN
    UPDATE residents 
    SET balance = NEW.balance_after 
    WHERE id = NEW.resident_id;
END;

-- Seed cư dân mặc định
INSERT OR IGNORE INTO residents (
    id, card_id, full_name, date_of_birth, room_number,
    phone_number, email, id_number, balance, pin_hash, photo_path, public_key
) VALUES (
    1, 'CARD001', 'Nguyễn Văn A', '1990-01-01', '101',
    '0901234567', 'nguyenvana@example.com', '001234567890', 0, '123456', NULL, NULL
);
