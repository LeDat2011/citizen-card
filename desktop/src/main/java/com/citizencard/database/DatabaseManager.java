package com.citizencard.database;

import java.sql.*;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class DatabaseManager {
    private static final String DB_DIR = "data";
    private static final String DB_FILE = "citizen_card.db";
    private static final String DB_URL = "jdbc:sqlite:" + DB_DIR + File.separator + DB_FILE;
    private static DatabaseManager instance;

    private DatabaseManager() {
        ensureDataDirectory();
        initializeDatabase();
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    private void ensureDataDirectory() {
        File dataDir = new File(DB_DIR);
        if (!dataDir.exists()) {
            dataDir.mkdirs();
            System.out.println("✅ Created data directory: " + dataDir.getAbsolutePath());
        }
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    private void initializeDatabase() {
        try (Connection conn = getConnection()) {
            // Kiểm tra xem bảng residents đã tồn tại chưa
            boolean tablesExist = checkTablesExist(conn);

            if (!tablesExist) {
                System.out.println("[INFO] Database tables not found, creating schema...");
                createSchema(conn);
            } else {
                System.out.println("[INFO] Database tables already exist, skipping schema creation");
            }

            System.out.println("✅ Database initialized successfully at: " + DB_URL);
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

    private boolean checkTablesExist(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(
                    "SELECT name FROM sqlite_master WHERE type='table' AND name='residents'");
            return rs.next();
        }
    }

    private void createSchema(Connection conn) throws SQLException {
        String schema = readSchema();
        if (schema == null || schema.trim().isEmpty()) {
            throw new SQLException("Schema file is empty or could not be read");
        }

        System.out.println("[INFO] Schema file loaded (" + schema.length() + " characters)");
        String[] statements = schema.split(";(?=(?:[^']*'[^']*')*[^']*$)");
        System.out.println("[INFO] Found " + statements.length + " SQL statements in schema");

        int tablesCreated = 0;
        int triggersCreated = 0;
        int insertsExecuted = 0;

        // SQLite tự động commit mỗi statement, không cần transaction
        // Chỉ tạo các bảng và trigger, bỏ qua CREATE INDEX (sẽ tạo sau)
        for (int i = 0; i < statements.length; i++) {
            String statement = statements[i].trim();
            // Remove full-line comments
            statement = statement.replaceAll("(?m)^--.*$", "").trim();

            if (statement.isEmpty()) {
                continue;
            }

            // Handle split trigger (reconstruct if split by semicolon)
            if (statement.toUpperCase().startsWith("CREATE TRIGGER") && !statement.toUpperCase().endsWith("END")) {
                StringBuilder sb = new StringBuilder(statement);
                int j = i + 1;
                while (j < statements.length) {
                    String nextPart = statements[j].trim().replaceAll("(?m)^--.*$", "").trim();
                    if (nextPart.isEmpty()) {
                        j++;
                        continue;
                    }
                    sb.append(";").append(nextPart);
                    i = j; // Advance main loop
                    if (nextPart.toUpperCase().endsWith("END")) {
                        break;
                    }
                    j++;
                }
                statement = sb.toString();
            }

            // Bỏ qua CREATE INDEX trong schema, sẽ tạo riêng sau
            if (statement.toUpperCase().contains("CREATE INDEX")) {
                continue;
            }

            // Bỏ qua statement "END" đơn lẻ (từ trigger)
            if (statement.toUpperCase().trim().equals("END")) {
                continue;
            }

            try (Statement stmt = conn.createStatement()) {
                stmt.execute(statement);

                // Log các statement quan trọng
                String upper = statement.toUpperCase();
                if (upper.startsWith("CREATE TABLE")) {
                    String tableName = extractTableName(statement);
                    System.out.println("[OK] Created table: " + tableName);
                    tablesCreated++;
                } else if (upper.startsWith("CREATE TRIGGER")) { // Changed from trimmed to upper for consistency
                    System.out.println("[OK] Created trigger");
                    triggersCreated++;
                } else if (upper.startsWith("INSERT")) {
                    insertsExecuted++;
                }
            } catch (SQLException e) {
                String errorMsg = e.getMessage();
                if (errorMsg != null) {
                    // Bỏ qua các lỗi expected
                    if (errorMsg.contains("already exists") ||
                            errorMsg.contains("UNIQUE constraint") ||
                            errorMsg.contains("duplicate column")) {
                        // Expected - already exists, bỏ qua
                        System.out.println("ℹ️  Bỏ qua (đã tồn tại): " +
                                statement.substring(0, Math.min(50, statement.length())) + "...");
                    } else if (errorMsg.contains("no transaction is active") &&
                            statement.toUpperCase().trim().equals("END")) {
                        // Bỏ qua lỗi END đơn lẻ
                        continue;
                    } else {
                        // Lỗi nghiêm trọng - log và throw exception
                        System.err.println("❌ Lỗi ở statement #" + (i + 1) + ": " + errorMsg);
                        System.err
                                .println("   Statement: " + statement.substring(0, Math.min(100, statement.length())));
                        throw new SQLException("Lỗi khi tạo schema ở statement #" + (i + 1) + ": " + errorMsg +
                                "\nStatement: " + statement.substring(0, Math.min(100, statement.length())), e);
                    }
                }
            }
        }

        System.out.println("[OK] Schema created successfully:");
        System.out.println("   - Tables: " + tablesCreated);
        System.out.println("   - Triggers: " + triggersCreated);
        System.out.println("   - Inserts: " + insertsExecuted);

        // Kiểm tra lại các bảng đã được tạo chưa
        if (checkTablesExist(conn)) {
            System.out.println("[OK] Verified residents table exists");
            // Tạo các index sau khi đã có bảng
            createIndexes(conn);
        } else {
            System.err.println("❌ LỖI: Bảng residents chưa được tạo sau khi chạy schema!");
            throw new SQLException("Bảng residents không tồn tại sau khi tạo schema");
        }
    }

    private void createIndexes(Connection conn) throws SQLException {
        // Kiểm tra các bảng đã tồn tại chưa
        if (!checkTablesExist(conn)) {
            System.out.println("⚠️  Bảng chưa tồn tại, bỏ qua tạo index (sẽ tạo lại sau)");
            return;
        }

        String[] indexStatements = {
                "CREATE INDEX IF NOT EXISTS idx_residents_card_id ON residents(card_id)",
                "CREATE INDEX IF NOT EXISTS idx_residents_room ON residents(room_number)",
                "CREATE INDEX IF NOT EXISTS idx_parking_resident ON parking_simple(resident_id)",
                "CREATE INDEX IF NOT EXISTS idx_parking_license ON parking_simple(license_plate)",
                "CREATE INDEX IF NOT EXISTS idx_invoices_resident ON invoices_simple(resident_id)",
                "CREATE INDEX IF NOT EXISTS idx_invoices_status ON invoices_simple(payment_status)",
                "CREATE INDEX IF NOT EXISTS idx_transactions_resident ON transactions_simple(resident_id)",
                "CREATE INDEX IF NOT EXISTS idx_transactions_card ON transactions_simple(card_id)"
        };

        int successCount = 0;
        for (String indexSql : indexStatements) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(indexSql);
                successCount++;
            } catch (SQLException e) {
                String errorMsg = e.getMessage();
                // Bỏ qua các lỗi expected
                if (errorMsg != null && (errorMsg.contains("already exists") ||
                        errorMsg.contains("no such table"))) {
                    // Expected - bỏ qua im lặng
                } else {
                    System.err.println("⚠️  Lỗi tạo index: " + errorMsg);
                }
            }
        }

        if (successCount > 0) {
            System.out.println("[OK] Created " + successCount + " indexes");
        }
    }

    private String extractTableName(String createTableStatement) {
        // Extract table name from "CREATE TABLE IF NOT EXISTS table_name"
        String upper = createTableStatement.toUpperCase().trim();
        int start = upper.indexOf("TABLE");
        if (start == -1)
            return "unknown";

        String afterTable = createTableStatement.substring(start + 5).trim();
        if (afterTable.toUpperCase().startsWith("IF NOT EXISTS")) {
            afterTable = afterTable.substring(14).trim();
        }

        // Find first space or opening parenthesis
        int end = afterTable.length();
        for (int i = 0; i < afterTable.length(); i++) {
            if (afterTable.charAt(i) == ' ' || afterTable.charAt(i) == '(') {
                end = i;
                break;
            }
        }

        return afterTable.substring(0, end).trim();
    }

    private String readSchema() {
        try {
            InputStream inputStream = getClass().getClassLoader()
                    .getResourceAsStream("database/schema.sql");
            if (inputStream == null) {
                throw new RuntimeException("Schema file not found: database/schema.sql");
            }

            Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8.name());
            StringBuilder schema = new StringBuilder();
            while (scanner.hasNextLine()) {
                schema.append(scanner.nextLine()).append("\n");
            }
            scanner.close();
            inputStream.close();

            return schema.toString();
        } catch (Exception e) {
            System.err.println("❌ Error reading schema file: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to read schema file", e);
        }
    }
}
