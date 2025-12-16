package citizencard;

import java.sql.*;
import java.io.File;

/**
 * All-in-one Database Service
 * Simple database for card registration
 */
public class CardDAO {
    
    private static final String DB_DIR = "data";
    private static final String DB_FILE = "citizen_card";
    private static final String DB_URL = "jdbc:h2:file:./" + DB_DIR + File.separator + DB_FILE + ";AUTO_SERVER=FALSE;DB_CLOSE_DELAY=-1";
    private static CardDAO instance;

    private CardDAO() {
        ensureDataDirectory();
        initializeDatabase();
    }

    public static synchronized CardDAO getInstance() {
        if (instance == null) {
            instance = new CardDAO();
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

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    private void initializeDatabase() {
        try (Connection conn = getConnection()) {
            boolean tablesExist = checkTablesExist(conn);
            if (!tablesExist) {
                System.out.println("[INFO] Creating database schema...");
                createSchema(conn);
                System.out.println("✅ Database initialized successfully");
            } else {
                System.out.println("[INFO] Database already exists");
            }
        } catch (SQLException e) {
            System.err.println("❌ Error initializing database: " + e.getMessage());
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    private boolean checkTablesExist(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(
                    "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME='REGISTERED_CARDS'");
            return rs.next();
        }
    }

    private void createSchema(Connection conn) throws SQLException {
        String[] createStatements = {
            "CREATE TABLE IF NOT EXISTS registered_cards (" +
            "id INTEGER PRIMARY KEY AUTO_INCREMENT," +
            "card_id TEXT UNIQUE NOT NULL," +
            "public_key TEXT NOT NULL," +
            "card_status TEXT DEFAULT 'ACTIVE'," +
            "registered_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
            "last_accessed DATETIME DEFAULT CURRENT_TIMESTAMP," +
            "CONSTRAINT chk_card_status CHECK (card_status IN ('ACTIVE', 'BLOCKED', 'EXPIRED'))" +
            ")",
            
            "CREATE TABLE IF NOT EXISTS transaction_logs (" +
            "id INTEGER PRIMARY KEY AUTO_INCREMENT," +
            "card_id TEXT NOT NULL," +
            "operation_type TEXT NOT NULL," +
            "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP," +
            "success BOOLEAN DEFAULT TRUE," +
            "error_message TEXT," +
            "FOREIGN KEY (card_id) REFERENCES registered_cards(card_id) ON DELETE CASCADE," +
            "CONSTRAINT chk_operation_type CHECK (operation_type IN ('LOGIN', 'TOPUP', 'PAYMENT', 'UPDATE_INFO', 'CHANGE_PIN'))" +
            ")",
            
            "CREATE INDEX IF NOT EXISTS idx_cards_card_id ON registered_cards(card_id)",
            "CREATE INDEX IF NOT EXISTS idx_cards_status ON registered_cards(card_status)",
            "CREATE INDEX IF NOT EXISTS idx_logs_card_id ON transaction_logs(card_id)",
            "CREATE INDEX IF NOT EXISTS idx_logs_timestamp ON transaction_logs(timestamp)"
        };

        for (String sql : createStatements) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
            }
        }
    }

    /**
     * Register a new card in database
     */
    public boolean registerCard(String cardId, String publicKey) {
        String sql = "INSERT INTO registered_cards (card_id, public_key, card_status, registered_at, last_accessed) " +
                    "VALUES (?, ?, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, cardId);
            stmt.setString(2, publicKey);
            
            return stmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Error registering card: " + e.getMessage());
            return false;
        }
    }

    /**
     * Check if card exists and is active
     */
    public boolean isCardRegistered(String cardId) {
        String sql = "SELECT id FROM registered_cards WHERE card_id = ? AND card_status = 'ACTIVE'";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, cardId);
            ResultSet rs = stmt.executeQuery();
            
            return rs.next();
            
        } catch (SQLException e) {
            System.err.println("Error checking card registration: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get public key for a card
     */
    public String getPublicKey(String cardId) {
        String sql = "SELECT public_key FROM registered_cards WHERE card_id = ? AND card_status = 'ACTIVE'";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, cardId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getString("public_key");
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting public key: " + e.getMessage());
        }
        
        return null;
    }

    /**
     * Update last accessed time
     */
    public void updateLastAccessed(String cardId) {
        String sql = "UPDATE registered_cards SET last_accessed = CURRENT_TIMESTAMP WHERE card_id = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, cardId);
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            System.err.println("Error updating last accessed: " + e.getMessage());
        }
    }

    /**
     * Block a card
     */
    public boolean blockCard(String cardId) {
        String sql = "UPDATE registered_cards SET card_status = 'BLOCKED' WHERE card_id = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, cardId);
            return stmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Error blocking card: " + e.getMessage());
            return false;
        }
    }

    /**
     * Log transaction for audit
     */
    public void logTransaction(String cardId, String operationType, boolean success, String errorMessage) {
        String sql = "INSERT INTO transaction_logs (card_id, operation_type, timestamp, success, error_message) " +
                    "VALUES (?, ?, CURRENT_TIMESTAMP, ?, ?)";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, cardId);
            stmt.setString(2, operationType);
            stmt.setBoolean(3, success);
            stmt.setString(4, errorMessage);
            
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            System.err.println("Error logging transaction: " + e.getMessage());
        }
    }
}