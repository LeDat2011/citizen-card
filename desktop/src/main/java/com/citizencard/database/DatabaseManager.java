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
            String schema = readSchema();

            String[] statements = schema.split(";(?=(?:[^']*'[^']*')*[^']*$)");

            for (String statement : statements) {
                statement = statement.trim();
                if (!statement.isEmpty() && !statement.startsWith("--")) {
                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute(statement);
                        System.out.println(
                                "✅ Executed: " + statement.substring(0, Math.min(60, statement.length())) + "...");
                    } catch (SQLException e) {
                        String errorMsg = e.getMessage();
                        if (errorMsg != null && (errorMsg.contains("already exists") ||
                                errorMsg.contains("UNIQUE constraint") ||
                                errorMsg.contains("duplicate column") ||
                                errorMsg.contains("no such table"))) {
                            if (errorMsg.contains("no such table")) {
                                System.err.println("⚠️  Warning: " + errorMsg);
                                System.err.println("   Statement: "
                                        + statement.substring(0, Math.min(60, statement.length())) + "...");
                            } else {
                                System.out.println("⚠️  Skipped (already exists): "
                                        + statement.substring(0, Math.min(60, statement.length())) + "...");
                            }
                        } else {
                            System.err.println("❌ Error executing statement: "
                                    + statement.substring(0, Math.min(60, statement.length())) + "...");
                            System.err.println("   Error: " + errorMsg);
                        }
                    }
                }
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

