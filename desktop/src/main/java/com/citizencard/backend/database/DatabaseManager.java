package com.citizencard.backend.database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

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
    
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }
    
    private void initializeDatabase() {
        try (Connection conn = getConnection()) {
            String schema = readSchema();
            
            String[] statements = schema.split(";(?=(?:[^']*'[^']*')*[^']*$)");
            
            for (String statement : statements) {
                statement = statement.trim();
                if (!statement.isEmpty() && !statement.startsWith("--")) {
                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute(statement);
                        System.out.println("✅ Executed: " + statement.substring(0, Math.min(60, statement.length())) + "...");
                    } catch (SQLException e) {
                        String errorMsg = e.getMessage();
                        if (errorMsg != null && (errorMsg.contains("already exists") || 
                            errorMsg.contains("UNIQUE constraint") ||
                            errorMsg.contains("duplicate column") ||
                            errorMsg.contains("no such table"))) {
                            if (errorMsg.contains("no such table")) {
                                System.err.println("⚠️  Warning: " + errorMsg);
                                System.err.println("   Statement: " + statement.substring(0, Math.min(60, statement.length())) + "...");
                            } else {
                                System.out.println("⚠️  Skipped (already exists): " + statement.substring(0, Math.min(60, statement.length())) + "...");
                            }
                        } else {
                            System.err.println("❌ Error executing statement: " + statement.substring(0, Math.min(60, statement.length())) + "...");
                            System.err.println("   Error: " + errorMsg);
                        }
                    }
                }
            }
            
            System.out.println("✅ Database initialized successfully");
        } catch (SQLException e) {
            System.err.println("❌ Error initializing database: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize database", e);
        } catch (Exception e) {
            System.err.println("❌ Unexpected error initializing database: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize database", e);
        }
    }
    
    private String readSchema() {
        StringBuilder schema = new StringBuilder();
        
        schema.append("CREATE TABLE IF NOT EXISTS residents (");
        schema.append("    id INTEGER PRIMARY KEY AUTOINCREMENT,");
        schema.append("    card_id TEXT UNIQUE NOT NULL,");
        schema.append("    full_name TEXT NOT NULL,");
        schema.append("    date_of_birth TEXT NOT NULL,");
        schema.append("    room_number TEXT NOT NULL,");
        schema.append("    phone_number TEXT,");
        schema.append("    email TEXT,");
        schema.append("    id_number TEXT,");
        schema.append("    balance INTEGER DEFAULT 0,");
        schema.append("    pin_hash TEXT,");
        schema.append("    photo_path TEXT");
        schema.append(");");
        
        schema.append("CREATE TABLE IF NOT EXISTS transactions (");
        schema.append("    id INTEGER PRIMARY KEY AUTOINCREMENT,");
        schema.append("    resident_id INTEGER NOT NULL,");
        schema.append("    card_id TEXT NOT NULL,");
        schema.append("    transaction_type TEXT NOT NULL,");
        schema.append("    amount INTEGER NOT NULL,");
        schema.append("    balance_after INTEGER NOT NULL,");
        schema.append("    payment_status TEXT,");
        schema.append("    service_name TEXT,");
        schema.append("    description TEXT,");
        schema.append("    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,");
        schema.append("    FOREIGN KEY (resident_id) REFERENCES residents(id) ON DELETE CASCADE");
        schema.append(");");
        
        schema.append("CREATE TABLE IF NOT EXISTS parking (");
        schema.append("    id INTEGER PRIMARY KEY AUTOINCREMENT,");
        schema.append("    resident_id INTEGER NOT NULL,");
        schema.append("    license_plate TEXT NOT NULL,");
        schema.append("    vehicle_type TEXT NOT NULL,");
        schema.append("    monthly_fee INTEGER DEFAULT 200000,");
        schema.append("    expired_date DATE,");
        schema.append("    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,");
        schema.append("    FOREIGN KEY (resident_id) REFERENCES residents(id) ON DELETE CASCADE");
        schema.append(");");
        
        schema.append("CREATE INDEX IF NOT EXISTS idx_residents_card_id ON residents(card_id);");
        schema.append("CREATE INDEX IF NOT EXISTS idx_transactions_resident ON transactions(resident_id);");
        schema.append("CREATE INDEX IF NOT EXISTS idx_transactions_card ON transactions(card_id);");
        schema.append("CREATE INDEX IF NOT EXISTS idx_transactions_type ON transactions(transaction_type);");
        schema.append("CREATE INDEX IF NOT EXISTS idx_transactions_status ON transactions(payment_status);");
        schema.append("CREATE INDEX IF NOT EXISTS idx_parking_resident ON parking(resident_id);");
        
        schema.append("INSERT OR IGNORE INTO residents (id, card_id, full_name, date_of_birth, room_number, phone_number, email, id_number, balance, pin_hash, photo_path) VALUES (1, 'CARD001', 'Nguyễn Văn A', '1990-01-01', '101', '0901234567', 'nguyenvana@example.com', '001234567890', 0, '123456', NULL);");
        
        return schema.toString();
    }
}


