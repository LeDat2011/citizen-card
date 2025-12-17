package citizencard.dao;

import java.sql.*;
import java.io.File;

/**
 * All-in-one Database Service
 * Simple database for card registration
 */
public class CardDAO {

    private static final String DB_DIR = "data";
    private static final String DB_FILE = "citizen_card";
    private static final String DB_URL = "jdbc:h2:file:./" + DB_DIR + File.separator + DB_FILE
            + ";AUTO_SERVER=FALSE;DB_CLOSE_DELAY=-1";
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
                // Only store card_id, public_key, status - personal data is encrypted on card
                "CREATE TABLE IF NOT EXISTS registered_cards (" +
                        "id INTEGER PRIMARY KEY AUTO_INCREMENT," +
                        "card_id TEXT UNIQUE NOT NULL," +
                        "public_key TEXT NOT NULL," +
                        "card_status TEXT DEFAULT 'ACTIVE'," +
                        "registered_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
                        "last_accessed DATETIME DEFAULT CURRENT_TIMESTAMP," +
                        "CONSTRAINT chk_card_status CHECK (card_status IN ('ACTIVE', 'BLOCKED', 'EXPIRED'))" +
                        ")",

                "CREATE TABLE IF NOT EXISTS invoices (" +
                        "id INTEGER PRIMARY KEY AUTO_INCREMENT," +
                        "card_id TEXT NOT NULL," +
                        "amount BIGINT NOT NULL," +
                        "description TEXT," +
                        "status TEXT DEFAULT 'PENDING'," +
                        "created_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
                        "FOREIGN KEY (card_id) REFERENCES registered_cards(card_id) ON DELETE CASCADE," +
                        "CONSTRAINT chk_inv_status CHECK (status IN ('PENDING', 'PAID', 'CANCELLED'))" +
                        ")",

                "CREATE TABLE IF NOT EXISTS transaction_logs (" +
                        "id INTEGER PRIMARY KEY AUTO_INCREMENT," +
                        "card_id TEXT NOT NULL," +
                        "operation_type TEXT NOT NULL," +
                        "amount INTEGER DEFAULT 0," +
                        "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP," +
                        "success BOOLEAN DEFAULT TRUE," +
                        "error_message TEXT," +
                        "FOREIGN KEY (card_id) REFERENCES registered_cards(card_id) ON DELETE CASCADE," +
                        "CONSTRAINT chk_operation_type CHECK (operation_type IN ('LOGIN', 'TOPUP', 'PAYMENT', 'UPDATE_INFO', 'CHANGE_PIN', 'CREATE_CARD'))"
                        +
                        ")",

                "CREATE TABLE IF NOT EXISTS topup_requests (" +
                        "id INTEGER PRIMARY KEY AUTO_INCREMENT," +
                        "card_id TEXT NOT NULL," +
                        "amount BIGINT NOT NULL," +
                        "status TEXT DEFAULT 'PENDING'," +
                        "created_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
                        "approved_at DATETIME," +
                        "FOREIGN KEY (card_id) REFERENCES registered_cards(card_id) ON DELETE CASCADE," +
                        "CONSTRAINT chk_topup_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'SYNCED'))" +
                        ")",

                "CREATE INDEX IF NOT EXISTS idx_cards_card_id ON registered_cards(card_id)",
                "CREATE INDEX IF NOT EXISTS idx_cards_status ON registered_cards(card_status)",
                "CREATE INDEX IF NOT EXISTS idx_logs_card_id ON transaction_logs(card_id)",
                "CREATE INDEX IF NOT EXISTS idx_logs_timestamp ON transaction_logs(timestamp)",
                "CREATE INDEX IF NOT EXISTS idx_topup_card_id ON topup_requests(card_id)",
                "CREATE INDEX IF NOT EXISTS idx_topup_status ON topup_requests(status)"
        };

        for (String sql : createStatements) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
            }
        }
    }

    /**
     * Register a new card in database
     * Only stores card_id and public_key - personal data is encrypted on card
     */
    public boolean registerCard(String cardId, String publicKey) {
        String sql = "INSERT INTO registered_cards (card_id, public_key, card_status, registered_at, last_accessed) "
                +
                "VALUES (?, ?, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";

        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, cardId);
            stmt.setString(2, publicKey);

            boolean result = stmt.executeUpdate() > 0;
            if (result) {
                logTransaction(cardId, "CREATE_CARD", true, null);
                System.out.println("[DB] Registered card: " + cardId);
            }
            return result;

        } catch (SQLException e) {
            System.err.println("Error registering card: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get all registered cards for admin dashboard
     * Note: Personal info (name, phone) is stored encrypted on card, not in DB
     */
    public java.util.List<CardRecord> getAllCards() {
        java.util.List<CardRecord> cards = new java.util.ArrayList<>();
        String sql = "SELECT card_id, public_key, card_status, registered_at, last_accessed "
                +
                "FROM registered_cards ORDER BY registered_at DESC";

        try (Connection conn = getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                CardRecord record = new CardRecord();
                record.cardId = rs.getString("card_id");
                record.publicKey = rs.getString("public_key");
                record.status = rs.getString("card_status");
                record.registeredAt = rs.getString("registered_at");
                record.lastAccessed = rs.getString("last_accessed");
                cards.add(record);
            }

        } catch (SQLException e) {
            System.err.println("Error getting cards: " + e.getMessage());
        }

        return cards;
    }

    public boolean createInvoice(String cardId, long amount, String description) {
        String sql = "INSERT INTO invoices (card_id, amount, description, status, created_at) VALUES (?, ?, ?, 'PENDING', CURRENT_TIMESTAMP)";
        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, cardId);
            stmt.setLong(2, amount);
            stmt.setString(3, description);
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                System.out.println("[DB] Invoice created for " + cardId + ": " + amount);
                return true;
            }
            return false;
        } catch (SQLException e) {
            System.err.println("Error creating invoice: " + e.getMessage());
            return false;
        }
    }

    /**
     * Invoice record model
     */
    public static class InvoiceRecord {
        public int id;
        public String cardId;
        public long amount;
        public String description;
        public String status;
        public String createdAt;
    }

    /**
     * Get invoices for a specific card
     */
    public java.util.List<InvoiceRecord> getInvoicesByCardId(String cardId) {
        java.util.List<InvoiceRecord> invoices = new java.util.ArrayList<>();
        String sql = "SELECT id, card_id, amount, description, status, created_at FROM invoices WHERE card_id = ? ORDER BY created_at DESC";

        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, cardId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                InvoiceRecord record = new InvoiceRecord();
                record.id = rs.getInt("id");
                record.cardId = rs.getString("card_id");
                record.amount = rs.getLong("amount");
                record.description = rs.getString("description");
                record.status = rs.getString("status");
                record.createdAt = rs.getString("created_at");
                invoices.add(record);
            }
        } catch (SQLException e) {
            System.err.println("Error getting invoices: " + e.getMessage());
        }
        return invoices;
    }

    /**
     * Pay an invoice (update status to PAID)
     */
    public boolean payInvoice(int invoiceId) {
        String sql = "UPDATE invoices SET status = 'PAID' WHERE id = ? AND status = 'PENDING'";
        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, invoiceId);
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                System.out.println("[DB] Invoice " + invoiceId + " paid successfully");
                return true;
            }
            return false;
        } catch (SQLException e) {
            System.err.println("Error paying invoice: " + e.getMessage());
            return false;
        }
    }

    // =====================================================
    // TOPUP REQUEST MANAGEMENT
    // =====================================================

    /**
     * Topup request record model
     */
    public static class TopupRecord {
        public int id;
        public String cardId;
        public long amount;
        public String status;
        public String createdAt;
        public String approvedAt;
    }

    /**
     * Create a new topup request
     */
    public boolean createTopupRequest(String cardId, long amount) {
        String sql = "INSERT INTO topup_requests (card_id, amount, status, created_at) VALUES (?, ?, 'PENDING', CURRENT_TIMESTAMP)";
        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, cardId);
            stmt.setLong(2, amount);
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                System.out.println("[DB] Topup request created for " + cardId + ": " + amount + " VND");
                return true;
            }
            return false;
        } catch (SQLException e) {
            System.err.println("Error creating topup request: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get all pending topup requests (for admin)
     */
    public java.util.List<TopupRecord> getPendingTopupRequests() {
        java.util.List<TopupRecord> requests = new java.util.ArrayList<>();
        String sql = "SELECT id, card_id, amount, status, created_at, approved_at FROM topup_requests WHERE status = 'PENDING' ORDER BY created_at ASC";

        try (Connection conn = getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                TopupRecord record = new TopupRecord();
                record.id = rs.getInt("id");
                record.cardId = rs.getString("card_id");
                record.amount = rs.getLong("amount");
                record.status = rs.getString("status");
                record.createdAt = rs.getString("created_at");
                record.approvedAt = rs.getString("approved_at");
                requests.add(record);
            }
        } catch (SQLException e) {
            System.err.println("Error getting pending topup requests: " + e.getMessage());
        }
        return requests;
    }

    /**
     * Get topup requests for a specific card (for citizen)
     */
    public java.util.List<TopupRecord> getTopupRequestsByCardId(String cardId) {
        java.util.List<TopupRecord> requests = new java.util.ArrayList<>();
        String sql = "SELECT id, card_id, amount, status, created_at, approved_at FROM topup_requests WHERE card_id = ? ORDER BY created_at DESC";

        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, cardId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                TopupRecord record = new TopupRecord();
                record.id = rs.getInt("id");
                record.cardId = rs.getString("card_id");
                record.amount = rs.getLong("amount");
                record.status = rs.getString("status");
                record.createdAt = rs.getString("created_at");
                record.approvedAt = rs.getString("approved_at");
                requests.add(record);
            }
        } catch (SQLException e) {
            System.err.println("Error getting topup requests: " + e.getMessage());
        }
        return requests;
    }

    /**
     * Approve a topup request (admin only)
     */
    public boolean approveTopupRequest(int requestId) {
        String sql = "UPDATE topup_requests SET status = 'APPROVED', approved_at = CURRENT_TIMESTAMP WHERE id = ? AND status = 'PENDING'";
        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, requestId);
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                System.out.println("[DB] Topup request " + requestId + " approved");
                return true;
            }
            return false;
        } catch (SQLException e) {
            System.err.println("Error approving topup request: " + e.getMessage());
            return false;
        }
    }

    /**
     * Reject a topup request (admin only)
     */
    public boolean rejectTopupRequest(int requestId) {
        String sql = "UPDATE topup_requests SET status = 'REJECTED', approved_at = CURRENT_TIMESTAMP WHERE id = ? AND status = 'PENDING'";
        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, requestId);
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                System.out.println("[DB] Topup request " + requestId + " rejected");
                return true;
            }
            return false;
        } catch (SQLException e) {
            System.err.println("Error rejecting topup request: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get topup request by ID
     */
    public TopupRecord getTopupRequestById(int requestId) {
        String sql = "SELECT id, card_id, amount, status, created_at, approved_at FROM topup_requests WHERE id = ?";
        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, requestId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                TopupRecord record = new TopupRecord();
                record.id = rs.getInt("id");
                record.cardId = rs.getString("card_id");
                record.amount = rs.getLong("amount");
                record.status = rs.getString("status");
                record.createdAt = rs.getString("created_at");
                record.approvedAt = rs.getString("approved_at");
                return record;
            }
        } catch (SQLException e) {
            System.err.println("Error getting topup request: " + e.getMessage());
        }
        return null;
    }

    /**
     * Mark topup as synced (balance credited to card)
     */
    public boolean markTopupAsSynced(int requestId) {
        String sql = "UPDATE topup_requests SET status = 'SYNCED' WHERE id = ? AND status = 'APPROVED'";
        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, requestId);
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                System.out.println("[DB] Topup request " + requestId + " marked as synced");
                return true;
            }
            return false;
        } catch (SQLException e) {
            System.err.println("Error marking topup as synced: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get card count by status
     */
    public int getCardCountByStatus(String status) {
        String sql = status == null ? "SELECT COUNT(*) FROM registered_cards"
                : "SELECT COUNT(*) FROM registered_cards WHERE card_status = ?";

        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (status != null) {
                stmt.setString(1, status);
            }
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error counting cards: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Card record model for database results
     * Only contains data stored in DB, not personal info (which is on card)
     */
    public static class CardRecord {
        public String cardId;
        public String publicKey;
        public String status;
        public String registeredAt;
        public String lastAccessed;
    }

    public static class TransactionRecord {
        public String cardId;
        public String type;
        public String timestamp;
        public boolean success;
    }

    public java.util.List<TransactionRecord> getRecentTransactions(int limit) {
        java.util.List<TransactionRecord> logs = new java.util.ArrayList<>();
        String sql = "SELECT card_id, operation_type, timestamp, success FROM transaction_logs ORDER BY timestamp DESC LIMIT ?";

        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, limit);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                TransactionRecord log = new TransactionRecord();
                log.cardId = rs.getString("card_id");
                log.type = rs.getString("operation_type");
                log.timestamp = rs.getString("timestamp");
                log.success = rs.getBoolean("success");
                logs.add(log);
            }
        } catch (SQLException e) {
            System.err.println("Error getting transactions: " + e.getMessage());
        }
        return logs;
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
     * Get most recently created card ID
     * Used for citizen login when we only have applet card ID
     */
    public String getMostRecentCardId() {
        String sql = "SELECT card_id FROM registered_cards WHERE card_status = 'ACTIVE' ORDER BY registered_at DESC LIMIT 1";

        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getString("card_id");
            }
            return null;

        } catch (SQLException e) {
            System.err.println("Error getting most recent card: " + e.getMessage());
            return null;
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