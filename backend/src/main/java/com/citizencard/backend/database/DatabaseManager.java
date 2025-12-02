package com.citizencard.backend.database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Quản lý kết nối và khởi tạo database SQLite
 */
public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:citizen_card.db";
    private static DatabaseManager instance;
    
    private DatabaseManager() {
        initializeDatabase();
    }
    
    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }
    
    /**
     * Lấy connection
     */
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }
    
    /**
     * Khởi tạo database và tạo bảng
     */
    private void initializeDatabase() {
        try (Connection conn = getConnection()) {
            // Đọc và thực thi schema
            String schema = readSchema();
            String[] statements = schema.split(";");
            
            for (String statement : statements) {
                statement = statement.trim();
                if (!statement.isEmpty() && !statement.startsWith("--")) {
                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute(statement);
                    }
                }
            }
            
            System.out.println("Database initialized successfully");
        } catch (SQLException e) {
            System.err.println("Error initializing database: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Đọc schema từ file hoặc embedded
     */
    private String readSchema() {
        return "-- Bảng cư dân (residents)\n" +
                "CREATE TABLE IF NOT EXISTS residents (\n" +
                "    id INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                "    card_id TEXT UNIQUE NOT NULL,\n" +
                "    full_name TEXT NOT NULL,\n" +
                "    date_of_birth TEXT NOT NULL,\n" +
                "    room_number TEXT NOT NULL,\n" +
                "    phone_number TEXT,\n" +
                "    email TEXT,\n" +
                "    id_number TEXT UNIQUE,\n" +
                "    balance INTEGER DEFAULT 0,\n" +
                "    photo_path TEXT,\n" +
                "    status TEXT DEFAULT 'ACTIVE',\n" +
                "    pin_hash TEXT,\n" +
                "    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,\n" +
                "    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,\n" +
                "    CONSTRAINT chk_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'BLOCKED'))\n" +
                ");\n" +
                "\n" +
                "-- Bảng gửi xe đơn giản (parking_simple)\n" +
                "CREATE TABLE IF NOT EXISTS parking_simple (\n" +
                "    id INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                "    resident_id INTEGER NOT NULL,\n" +
                "    license_plate TEXT NOT NULL,\n" +
                "    vehicle_type TEXT NOT NULL,\n" +
                "    action_type TEXT NOT NULL,\n" +
                "    monthly_fee INTEGER DEFAULT 200000,\n" +
                "    expired_date DATE,\n" +
                "    gate_location TEXT,\n" +
                "    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,\n" +
                "    notes TEXT,\n" +
                "    FOREIGN KEY (resident_id) REFERENCES residents(id) ON DELETE CASCADE,\n" +
                "    CONSTRAINT chk_vehicle_type CHECK (vehicle_type IN ('MOTORBIKE', 'CAR', 'BICYCLE')),\n" +
                "    CONSTRAINT chk_action_type CHECK (action_type IN ('REGISTER', 'CHECK_IN', 'CHECK_OUT'))\n" +
                ");\n" +
                "\n" +
                "-- Bảng hóa đơn đơn giản (invoices_simple)\n" +
                "CREATE TABLE IF NOT EXISTS invoices_simple (\n" +
                "    id INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                "    resident_id INTEGER NOT NULL,\n" +
                "    service_name TEXT NOT NULL,\n" +
                "    service_code TEXT NOT NULL,\n" +
                "    amount INTEGER NOT NULL,\n" +
                "    payment_status TEXT DEFAULT 'PENDING',\n" +
                "    invoice_date DATE NOT NULL,\n" +
                "    payment_date DATETIME,\n" +
                "    description TEXT,\n" +
                "    FOREIGN KEY (resident_id) REFERENCES residents(id) ON DELETE CASCADE,\n" +
                "    CONSTRAINT chk_payment_status CHECK (payment_status IN ('PENDING', 'PAID'))\n" +
                ");\n" +
                "\n" +
                "-- Bảng giao dịch đơn giản (transactions_simple)\n" +
                "CREATE TABLE IF NOT EXISTS transactions_simple (\n" +
                "    id INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                "    resident_id INTEGER NOT NULL,\n" +
                "    card_id TEXT NOT NULL,\n" +
                "    transaction_type TEXT NOT NULL,\n" +
                "    amount INTEGER NOT NULL,\n" +
                "    balance_before INTEGER NOT NULL,\n" +
                "    balance_after INTEGER NOT NULL,\n" +
                "    description TEXT,\n" +
                "    reference_id INTEGER,\n" +
                "    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,\n" +
                "    FOREIGN KEY (resident_id) REFERENCES residents(id) ON DELETE CASCADE,\n" +
                "    CONSTRAINT chk_transaction_type CHECK (transaction_type IN ('TOPUP', 'PAYMENT', 'DEBIT'))\n" +
                ");\n" +
                "\n" +
                "-- INDEXES\n" +
                "CREATE INDEX IF NOT EXISTS idx_residents_card_id ON residents(card_id);\n" +
                "CREATE INDEX IF NOT EXISTS idx_residents_room ON residents(room_number);\n" +
                "CREATE INDEX IF NOT EXISTS idx_parking_resident ON parking_simple(resident_id);\n" +
                "CREATE INDEX IF NOT EXISTS idx_parking_license ON parking_simple(license_plate);\n" +
                "CREATE INDEX IF NOT EXISTS idx_invoices_resident ON invoices_simple(resident_id);\n" +
                "CREATE INDEX IF NOT EXISTS idx_invoices_status ON invoices_simple(payment_status);\n" +
                "CREATE INDEX IF NOT EXISTS idx_transactions_resident ON transactions_simple(resident_id);\n" +
                "CREATE INDEX IF NOT EXISTS idx_transactions_card ON transactions_simple(card_id);\n" +
                "\n" +
                "-- TRIGGER đồng bộ số dư\n" +
                "CREATE TRIGGER IF NOT EXISTS trg_sync_balance\n" +
                "    AFTER INSERT ON transactions_simple\n" +
                "BEGIN\n" +
                "    UPDATE residents \n" +
                "    SET balance = NEW.balance_after \n" +
                "    WHERE id = NEW.resident_id;\n" +
                "END;";
    }
}








