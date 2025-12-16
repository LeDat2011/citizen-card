package citizencard.util;

import org.h2.tools.Server;
import java.sql.*;

/**
 * Database Viewer Utility
 * 
 * Provides multiple ways to view and interact with H2 database
 */
public class DatabaseViewer {
    
    private static final String DB_URL = "jdbc:h2:file:./data/citizen_card;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1";
    private static final String WEB_CONSOLE_URL = "http://localhost:8082";
    private static Server webServer;
    
    /**
     * Start H2 Web Console
     * Access at: http://localhost:8082
     */
    public static void startWebConsole() {
        try {
            if (webServer != null && webServer.isRunning(false)) {
                System.out.println("✅ H2 Console đã chạy tại: " + WEB_CONSOLE_URL);
                return;
            }
            
            // Start H2 web console
            webServer = Server.createWebServer("-web", "-webAllowOthers", "-webPort", "8082");
            webServer.start();
            
            System.out.println("🚀 H2 Console đã khởi động!");
            System.out.println("📱 Truy cập tại: " + WEB_CONSOLE_URL);
            System.out.println("🔗 JDBC URL: " + DB_URL);
            System.out.println("👤 User Name: (để trống)");
            System.out.println("🔑 Password: (để trống)");
            System.out.println();
            System.out.println("📋 CÁC BẢNG TRONG DATABASE:");
            System.out.println("• REGISTERED_CARDS - Thông tin thẻ đã đăng ký");
            System.out.println("• TRANSACTION_LOGS - Lịch sử giao dịch");
            
        } catch (SQLException e) {
            System.err.println("❌ Lỗi khởi động H2 Console: " + e.getMessage());
        }
    }
    
    /**
     * Stop H2 Web Console
     */
    public static void stopWebConsole() {
        if (webServer != null && webServer.isRunning(false)) {
            webServer.stop();
            System.out.println("⏹️ H2 Console đã dừng");
        }
    }
    
    /**
     * Print database content to console
     */
    public static void printDatabaseContent() {
        System.out.println("📊 DATABASE CONTENT - CITIZEN CARD SYSTEM");
        System.out.println("=" .repeat(60));
        
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            
            // Print registered cards
            printRegisteredCards(conn);
            System.out.println();
            
            // Print transaction logs
            printTransactionLogs(conn);
            
        } catch (SQLException e) {
            System.err.println("❌ Lỗi kết nối database: " + e.getMessage());
        }
    }
    
    private static void printRegisteredCards(Connection conn) throws SQLException {
        System.out.println("🎫 REGISTERED CARDS:");
        System.out.println("-".repeat(60));
        
        String sql = "SELECT * FROM registered_cards ORDER BY registered_at DESC";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (!rs.next()) {
                System.out.println("   (Chưa có thẻ nào được đăng ký)");
                return;
            }
            
            // Print header
            System.out.printf("%-5s %-20s %-15s %-20s %-20s%n", 
                "ID", "CARD_ID", "STATUS", "REGISTERED_AT", "LAST_ACCESSED");
            System.out.println("-".repeat(80));
            
            do {
                System.out.printf("%-5d %-20s %-15s %-20s %-20s%n",
                    rs.getInt("id"),
                    rs.getString("card_id"),
                    rs.getString("card_status"),
                    rs.getTimestamp("registered_at"),
                    rs.getTimestamp("last_accessed")
                );
            } while (rs.next());
        }
    }
    
    private static void printTransactionLogs(Connection conn) throws SQLException {
        System.out.println("📝 TRANSACTION LOGS (10 gần nhất):");
        System.out.println("-".repeat(60));
        
        String sql = "SELECT * FROM transaction_logs ORDER BY timestamp DESC LIMIT 10";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (!rs.next()) {
                System.out.println("   (Chưa có giao dịch nào)");
                return;
            }
            
            // Print header
            System.out.printf("%-5s %-20s %-15s %-8s %-20s%n", 
                "ID", "CARD_ID", "OPERATION", "SUCCESS", "TIMESTAMP");
            System.out.println("-".repeat(70));
            
            do {
                System.out.printf("%-5d %-20s %-15s %-8s %-20s%n",
                    rs.getInt("id"),
                    rs.getString("card_id"),
                    rs.getString("operation_type"),
                    rs.getBoolean("success") ? "✅" : "❌",
                    rs.getTimestamp("timestamp")
                );
            } while (rs.next());
        }
    }
    
    /**
     * Get database statistics
     */
    public static void printDatabaseStats() {
        System.out.println("📈 DATABASE STATISTICS");
        System.out.println("=" .repeat(40));
        
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            
            // Count cards by status
            String cardsSql = "SELECT card_status, COUNT(*) as count FROM registered_cards GROUP BY card_status";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(cardsSql)) {
                
                System.out.println("🎫 Thẻ theo trạng thái:");
                while (rs.next()) {
                    System.out.printf("   %s: %d thẻ%n", 
                        rs.getString("card_status"), 
                        rs.getInt("count"));
                }
            }
            
            // Count transactions by type
            String transSql = "SELECT operation_type, COUNT(*) as count FROM transaction_logs GROUP BY operation_type";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(transSql)) {
                
                System.out.println("📝 Giao dịch theo loại:");
                while (rs.next()) {
                    System.out.printf("   %s: %d lần%n", 
                        rs.getString("operation_type"), 
                        rs.getInt("count"));
                }
            }
            
            // Success rate
            String successSql = "SELECT " +
                "COUNT(*) as total, " +
                "SUM(CASE WHEN success = TRUE THEN 1 ELSE 0 END) as successful " +
                "FROM transaction_logs";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(successSql)) {
                
                if (rs.next()) {
                    int total = rs.getInt("total");
                    int successful = rs.getInt("successful");
                    double successRate = total > 0 ? (successful * 100.0 / total) : 0;
                    
                    System.out.printf("📊 Tỷ lệ thành công: %.1f%% (%d/%d)%n", 
                        successRate, successful, total);
                }
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Lỗi lấy thống kê: " + e.getMessage());
        }
    }
    
    /**
     * Main method for standalone usage
     */
    public static void main(String[] args) {
        if (args.length > 0) {
            switch (args[0].toLowerCase()) {
                case "console":
                case "web":
                    startWebConsole();
                    System.out.println("Nhấn Enter để dừng...");
                    try {
                        System.in.read();
                    } catch (Exception e) {}
                    stopWebConsole();
                    break;
                case "print":
                case "show":
                    printDatabaseContent();
                    break;
                case "stats":
                    printDatabaseStats();
                    break;
                default:
                    printUsage();
            }
        } else {
            printUsage();
        }
    }
    
    private static void printUsage() {
        System.out.println("🔧 DATABASE VIEWER USAGE:");
        System.out.println("java citizencard.util.DatabaseViewer [command]");
        System.out.println();
        System.out.println("Commands:");
        System.out.println("  console/web  - Khởi động H2 Web Console");
        System.out.println("  print/show   - In nội dung database ra console");
        System.out.println("  stats        - Hiển thị thống kê database");
    }
}