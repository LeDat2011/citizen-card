package com.citizencard.desktop;

import javafx.application.Application;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import com.citizencard.desktop.ui.LoginView;
import com.citizencard.backend.service.CitizenCardService;
import com.citizencard.backend.database.DatabaseManager;

public class MainApp extends Application {
    private static CitizenCardService backendService;
    
    @Override
    public void start(Stage primaryStage) {
        try {
            // ✅ Khởi tạo Database trước
            DatabaseManager.getInstance();
            System.out.println("✅ Database initialized");
            
            // ✅ Khởi tạo Backend service trong cùng process (Local Communication)
            // Lưu ý: RealCardClient sẽ quét và kết nối với JCIDE terminal khi cần (lazy connection)
            backendService = new CitizenCardService();
            
            System.out.println("✅ Backend service initialized (Local mode - no HTTP server needed)");
            System.out.println("ℹ️  JCIDE phải chạy và terminal phải được mở để sử dụng thẻ");
            
            // ✅ Truyền service vào LoginView
            LoginView loginView = new LoginView(primaryStage, backendService);
            loginView.show();
        } catch (Exception e) {
            System.err.println("❌ Error initializing application: " + e.getMessage());
            e.printStackTrace();
            
            // Hiển thị dialog lỗi cho user
            javafx.application.Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Lỗi Khởi Tạo");
                alert.setHeaderText("Không thể khởi tạo ứng dụng");
                
                String errorMsg = e.getMessage();
                if (errorMsg != null && (errorMsg.contains("terminal") || errorMsg.contains("JCIDE"))) {
                    alert.setContentText("Không thể kết nối với JCIDE terminal.\n\n" +
                                       "Vui lòng đảm bảo:\n" +
                                       "1. JCIDE đang chạy\n" +
                                       "2. Terminal đã được mở trong JCIDE\n" +
                                       "3. Thẻ đã được đưa vào terminal\n\n" +
                                       "Lưu ý: KHÔNG cần chạy Backend HTTP Server (port 8080)");
                } else {
                    alert.setContentText("Lỗi: " + errorMsg + "\n\n" +
                                       "Vui lòng kiểm tra:\n" +
                                       "1. Maven dependencies đã được resolve\n" +
                                       "2. Database có thể tạo được\n" +
                                       "3. Xem console để biết chi tiết lỗi");
                }
                alert.showAndWait();
            });
        }
    }
    
    public static CitizenCardService getBackendService() {
        return backendService;
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}

