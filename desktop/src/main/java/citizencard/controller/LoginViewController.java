package citizencard.controller;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.paint.Color;

import citizencard.service.CardService;
import citizencard.dao.CardDAO;
import citizencard.util.PinInputDialog;

/**
 * Modern Login View Controller
 * 
 * Beautiful UI for Smart Card authentication
 * All logic is handled by the Smart Card Applet
 */
public class LoginViewController {

    private BorderPane root;
    private CardService cardService;
    private CardDAO cardDAO;
    private DemoWorkflowController demoController;
    private Label statusLabel;
    private Label connectionStatusLabel;
    private Button connectButton;
    private Button loginButton;
    private PasswordField pinField;
    private VBox loginSection;
    private VBox modeSelection;
    private ProgressIndicator loadingIndicator;
    private boolean isAdminMode = false;

    public LoginViewController() {
        cardService = CardService.getInstance();
        cardDAO = CardDAO.getInstance();
        demoController = new DemoWorkflowController(cardService, cardDAO);
        initializeUI();

        // Auto-connect to card on startup
        javafx.application.Platform.runLater(() -> {
            autoConnectToCard();
        });
    }

    private void initializeUI() {
        root = new BorderPane();
        root.getStyleClass().add("main-container");

        // Create main content with scroll pane
        VBox mainContent = createMainContent();

        // Wrap in ScrollPane to allow scrolling if content is too tall
        ScrollPane scrollPane = new ScrollPane(mainContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.getStyleClass().add("scroll-pane");

        root.setCenter(scrollPane);

        // Create footer
        HBox footer = createFooter();
        root.setBottom(footer);
    }

    private VBox createMainContent() {
        VBox content = new VBox(30);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(60, 80, 60, 80));
        content.setMaxWidth(600);
        content.getStyleClass().add("main-content");

        // Header section with icon and title
        VBox headerSection = createHeaderSection();

        // Card status section
        VBox statusSection = createStatusSection();

        // Connection section (shown first)
        VBox connectionSection = createConnectionSection();

        // Login mode selection (hidden until card connected)
        modeSelection = createModeSelection();
        modeSelection.setVisible(false);
        modeSelection.setManaged(false);

        // Login section (hidden until mode selected)
        loginSection = createLoginSection();
        loginSection.setVisible(false);
        loginSection.setManaged(false);

        content.getChildren().addAll(
                headerSection,
                statusSection,
                connectionSection,
                modeSelection,
                loginSection);

        return content;
    }

    private VBox createHeaderSection() {
        VBox header = new VBox(15);
        header.setAlignment(Pos.CENTER);

        // App icon
        Label iconLabel = new Label("🏛️");
        iconLabel.setStyle("-fx-font-size: 64px;");

        // Title
        Label titleLabel = new Label("Hệ thống Thẻ Cư dân");
        titleLabel.getStyleClass().add("app-title");

        // Subtitle
        Label subtitleLabel = new Label("Xác thực Thẻ thông minh An toàn");
        subtitleLabel.getStyleClass().add("app-subtitle");

        header.getChildren().addAll(iconLabel, titleLabel, subtitleLabel);
        return header;
    }

    private VBox createModeSelection() {
        VBox section = new VBox(20);
        section.setAlignment(Pos.CENTER);
        section.getStyleClass().add("mode-section");

        Label modeLabel = new Label("Chọn chế độ đăng nhập");
        modeLabel.getStyleClass().add("mode-title");

        // Mode buttons
        HBox modeButtons = new HBox(20);
        modeButtons.setAlignment(Pos.CENTER);

        Button adminModeButton = new Button("👨‍💼 Quản trị viên");
        adminModeButton.getStyleClass().addAll("btn", "btn-primary", "btn-large", "mode-button");
        adminModeButton.setPrefWidth(200);
        adminModeButton.setOnAction(e -> setAdminMode());

        Button citizenModeButton = new Button("👤 Cư dân");
        citizenModeButton.getStyleClass().addAll("btn", "btn-secondary", "btn-large", "mode-button");
        citizenModeButton.setPrefWidth(200);
        citizenModeButton.setOnAction(e -> setCitizenMode());

        modeButtons.getChildren().addAll(adminModeButton, citizenModeButton);

        // Mode description
        Label descLabel = new Label("Quản trị viên: Quản lý hệ thống | Cư dân: Dịch vụ thẻ (cần mã PIN)");
        descLabel.getStyleClass().add("mode-description");

        section.getChildren().addAll(modeLabel, modeButtons, descLabel);
        return section;
    }

    private VBox createStatusSection() {
        VBox section = new VBox(10);
        section.setAlignment(Pos.CENTER);
        section.getStyleClass().add("status-section");

        // Status indicator
        HBox statusIndicator = new HBox(10);
        statusIndicator.setAlignment(Pos.CENTER);

        connectionStatusLabel = new Label("Chưa kết nối");
        connectionStatusLabel.getStyleClass().add("status-text");

        statusIndicator.getChildren().add(connectionStatusLabel);

        // Main status message
        statusLabel = new Label("Vui lòng kết nối thẻ để tiếp tục");
        statusLabel.getStyleClass().add("status-message");

        section.getChildren().addAll(statusIndicator, statusLabel);
        return section;
    }

    private VBox createConnectionSection() {
        VBox section = new VBox(20);
        section.setAlignment(Pos.CENTER);
        section.getStyleClass().add("connection-section");

        // Connection card
        VBox connectionCard = new VBox(20);
        connectionCard.setAlignment(Pos.CENTER);
        connectionCard.getStyleClass().add("connection-card");
        connectionCard.setPadding(new Insets(30));

        // Card icon
        Label cardIcon = new Label("💳");
        cardIcon.setStyle("-fx-font-size: 48px;");

        // Instructions
        Label instructionLabel = new Label("Cắm thẻ thông minh của bạn");
        instructionLabel.getStyleClass().add("instruction-text");

        Label detailLabel = new Label("Đảm bảo JCIDE terminal đang chạy");
        detailLabel.getStyleClass().add("detail-text");

        // Connect button
        connectButton = new Button("Kết nối với Thẻ thông minh");
        connectButton.getStyleClass().addAll("btn", "btn-primary", "btn-large");
        connectButton.setOnAction(e -> connectToCard());

        // Loading indicator (hidden by default)
        loadingIndicator = new ProgressIndicator();
        loadingIndicator.setVisible(false);
        loadingIndicator.setPrefSize(30, 30);

        connectionCard.getChildren().addAll(
                cardIcon,
                instructionLabel,
                detailLabel,
                connectButton,
                loadingIndicator);

        section.getChildren().add(connectionCard);
        return section;
    }

    private VBox createLoginSection() {
        VBox section = new VBox(25);
        section.setAlignment(Pos.CENTER);
        section.getStyleClass().add("login-section");

        // Login card
        VBox loginCard = new VBox(25);
        loginCard.setAlignment(Pos.CENTER);
        loginCard.getStyleClass().add("login-card");
        loginCard.setPadding(new Insets(35));

        // Lock icon
        Label lockIcon = new Label("🔐");
        lockIcon.setStyle("-fx-font-size: 42px;");

        // Title
        Label loginTitle = new Label("Nhập mã PIN của bạn");
        loginTitle.getStyleClass().add("login-title");

        // PIN input section
        VBox pinSection = new VBox(15);
        pinSection.setAlignment(Pos.CENTER);

        Label pinLabel = new Label("Nhấn để nhập mã PIN");
        pinLabel.getStyleClass().add("pin-label");

        // PIN input button (opens dialog)
        Button pinInputButton = new Button("📱 Nhập mã PIN");
        pinInputButton.getStyleClass().addAll("btn", "btn-primary", "btn-large");
        pinInputButton.setPrefWidth(200);
        pinInputButton.setOnAction(e -> openPinDialog());

        pinSection.getChildren().addAll(pinLabel, pinInputButton);

        // Button section
        VBox buttonSection = new VBox(15);
        buttonSection.setAlignment(Pos.CENTER);

        // Status label for PIN
        Label pinStatusLabel = new Label();
        pinStatusLabel.getStyleClass().add("pin-status-label");
        pinStatusLabel.setVisible(false);

        buttonSection.getChildren().add(pinStatusLabel);

        loginCard.getChildren().addAll(
                lockIcon,
                loginTitle,
                pinSection,
                buttonSection);

        section.getChildren().add(loginCard);
        return section;
    }

    private HBox createFooter() {
        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER);
        footer.setPadding(new Insets(20));
        footer.getStyleClass().add("footer");

        Label footerText = new Label("Hệ thống Quản lý Thẻ Cư dân v1.0 | Công nghệ Thẻ thông minh An toàn");
        footerText.getStyleClass().add("footer-text");

        footer.getChildren().add(footerText);
        return footer;
    }

    // =====================================================
    // MODE SELECTION HANDLERS
    // =====================================================

    private void showModeSelection() {
        modeSelection.setVisible(true);
        modeSelection.setManaged(true);
    }

    private void setAdminMode() {
        isAdminMode = true;
        statusLabel.setText("Chế độ Quản trị viên: Đang tải bảng điều khiển admin...");

        // Admin mode - direct access to dashboard
        showAdminDashboard();
    }

    private void setCitizenMode() {
        isAdminMode = false;

        // Citizen mode - first challenge card, then check registration
        try {
            // Challenge card to verify authenticity using RSA
            System.out.println("[INFO] Citizen mode selected, challenging card...");
            boolean isAuthentic = cardService.challengeCard();

            if (!isAuthentic) {
                System.out.println("[WARN] Card authentication FAILED");
                statusLabel.setText("⚠️ CẢNH BÁO: Không thể xác thực thẻ!");
                showAlert("Thẻ không hợp lệ",
                        "Không thể xác minh tính xác thực của thẻ này.\n\n" +
                                "Thẻ có thể chưa được khởi tạo hoặc là bản sao không hợp lệ.\n\n" +
                                "Vui lòng liên hệ quản trị viên.");
                return;
            }

            System.out.println("[INFO] Card authentication PASSED");
            String cardId = cardService.getCardId();

            // Check if card is registered
            if (cardDAO.isCardRegistered(cardId)) {
                statusLabel.setText("✅ Thẻ đã được xác thực! Vui lòng nhập mã PIN.");
                showLoginSection();
            } else {
                statusLabel.setText("⚠️ Thẻ chưa được đăng ký trong hệ thống.");
                showAlert("Thẻ chưa được đăng ký",
                        "Thẻ này chưa được đăng ký trong hệ thống.\n\n" +
                                "Vui lòng liên hệ quản trị viên hệ thống để đăng ký thẻ này.\n\n" +
                                "ID Thẻ: " + cardId);
            }
        } catch (Exception e) {
            // Card is connected but not initialized
            statusLabel.setText("Thẻ chưa được khởi tạo. Vui lòng liên hệ quản trị viên.");
            showAlert("Thẻ chưa được khởi tạo",
                    "Thẻ này chưa được khởi tạo.\n\n" +
                            "Vui lòng liên hệ quản trị viên hệ thống để thiết lập thẻ này.\n\n" +
                            "Lỗi: " + e.getMessage());
        }
    }

    private void showAdminDashboard() {
        // Create new admin dashboard window
        AdminDashboardController adminController = new AdminDashboardController();

        // Replace current scene - keep same window size
        javafx.stage.Stage stage = (javafx.stage.Stage) root.getScene().getWindow();
        double w = stage.getWidth();
        double h = stage.getHeight();
        javafx.scene.Scene adminScene = new javafx.scene.Scene(adminController.getRoot(), w, h);

        // Load CSS
        adminScene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

        stage.setScene(adminScene);
        stage.setTitle("Hệ thống Quản lý Thẻ Cư dân - Bảng điều khiển Quản trị viên");
        // Don't centerOnScreen to keep window position

        System.out.println("🎛️ Bảng điều khiển Quản trị viên đã tải");
    }

    // =====================================================
    // CARD CONNECTION HANDLERS
    // =====================================================

    /**
     * Auto-connect to card on startup (silent)
     */
    private void autoConnectToCard() {
        System.out.println("[INFO] Auto-connecting to card...");

        // Check if already connected
        if (cardService.isConnected()) {
            System.out.println("[INFO] Card already connected");
            updateConnectionStatus(true);
            connectButton.setText("✓ Đã kết nối");
            connectButton.getStyleClass().removeAll("btn-primary");
            connectButton.getStyleClass().add("btn-success");
            connectButton.setDisable(true);
            statusLabel.setText("✅ Thẻ đã kết nối! Vui lòng chọn chế độ đăng nhập.");
            showModeSelection();
            return;
        }

        // Try to connect silently
        new Thread(() -> {
            try {
                boolean connected = cardService.connectToCard();

                javafx.application.Platform.runLater(() -> {
                    if (connected) {
                        System.out.println("[INFO] Auto-connect successful");
                        updateConnectionStatus(true);
                        connectButton.setText("✓ Đã kết nối");
                        connectButton.getStyleClass().removeAll("btn-primary");
                        connectButton.getStyleClass().add("btn-success");
                        connectButton.setDisable(true);
                        statusLabel.setText("✅ Thẻ đã kết nối! Vui lòng chọn chế độ đăng nhập.");
                        showModeSelection();
                    } else {
                        System.out.println("[WARN] Auto-connect failed - user must connect manually");
                        statusLabel.setText("⚠️ Không thể tự động kết nối. Vui lòng nhấn nút kết nối.");
                    }
                });

            } catch (Exception e) {
                System.err.println("[ERROR] Auto-connect error: " + e.getMessage());
                javafx.application.Platform.runLater(() -> {
                    statusLabel.setText("⚠️ Không thể tự động kết nối. Vui lòng nhấn nút kết nối.");
                });
            }
        }).start();
    }

    private void connectToCard() {
        // Show loading state
        connectButton.setDisable(true);
        connectButton.setText("Đang kết nối...");
        loadingIndicator.setVisible(true);
        statusLabel.setText("Đang kết nối với Thẻ thông minh...");
        connectionStatusLabel.setText("Đang kết nối...");

        // Run in background thread
        new Thread(() -> {
            try {
                Thread.sleep(1000); // Small delay for better UX
                boolean connected = cardService.connectToCard();

                javafx.application.Platform.runLater(() -> {
                    loadingIndicator.setVisible(false);

                    if (connected) {
                        System.out.println("[INFO] Manual connect successful");

                        // Update UI for successful connection
                        updateConnectionStatus(true);
                        connectButton.setText("✓ Đã kết nối");
                        connectButton.getStyleClass().removeAll("btn-primary");
                        connectButton.getStyleClass().add("btn-success");
                        connectButton.setDisable(true);

                        // Show mode selection - RSA challenge will happen when user selects Citizen
                        // mode
                        statusLabel.setText("✅ Thẻ đã kết nối! Vui lòng chọn chế độ đăng nhập.");
                        showModeSelection();

                    } else {
                        // Connection failed
                        updateConnectionStatus(false);
                        statusLabel.setText("Không thể kết nối với Thẻ thông minh. Vui lòng kiểm tra thẻ và thử lại.");
                        connectButton.setText("Thử kết nối lại");
                        connectButton.setDisable(false);
                    }
                });

            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    loadingIndicator.setVisible(false);
                    updateConnectionStatus(false);
                    statusLabel.setText("Lỗi kết nối: " + e.getMessage());
                    connectButton.setText("Thử kết nối lại");
                    connectButton.setDisable(false);
                });
            }
        }).start();
    }

    private void updateConnectionStatus(boolean connected) {
        if (connected) {
            connectionStatusLabel.setText("Đã kết nối");
            connectionStatusLabel.getStyleClass().removeAll("status-text");
            connectionStatusLabel.getStyleClass().add("status-text-success");
        } else {
            connectionStatusLabel.setText("Chưa kết nối");
            connectionStatusLabel.getStyleClass().removeAll("status-text-success");
            connectionStatusLabel.getStyleClass().add("status-text");
        }
    }

    private void showLoginSection() {
        loginSection.setVisible(true);
        loginSection.setManaged(true);
    }

    /**
     * Open PIN input dialog
     */
    private void openPinDialog() {
        String pin = PinInputDialog.showPinDialog(
                "Xác thực thẻ cư dân",
                "🔐 Nhập mã PIN để truy cập thẻ của bạn");

        if (pin != null && !pin.isEmpty()) {
            loginWithPin(pin);
        }
    }

    // =====================================================
    // LOGIN HANDLERS
    // =====================================================

    /**
     * Login with PIN from dialog
     */
    private void loginWithPin(String pin) {
        // Validate PIN format (already validated in dialog, but double-check)
        if (pin.length() != 4 || !pin.matches("\\d{4}")) {
            showAlert("Định dạng PIN không hợp lệ", "PIN phải có đúng 4 chữ số.\n\nVui lòng thử lại.");
            return;
        }

        // Show loading state
        statusLabel.setText("Đang xác minh mã PIN của bạn...");
        statusLabel.getStyleClass().removeAll("status-error", "status-success");
        statusLabel.getStyleClass().add("status-loading");

        // Run in background thread
        new Thread(() -> {
            try {
                Thread.sleep(800); // Small delay for better UX
                CardService.PinVerificationResult pinResult = cardService.verifyPin(pin);

                javafx.application.Platform.runLater(() -> {
                    if (pinResult.success) {
                        statusLabel.setText("✅ Xác thực thành công! Đang tải tài khoản của bạn...");

                        // Get card info and show dashboard
                        try {
                            // Get applet card ID (internal, not displayed)
                            String appletCardId = cardService.getCardId();
                            System.out.println("[DEBUG] Applet Card ID: " + appletCardId);

                            // Get actual Card ID from database (most recent card)
                            // TODO: Implement proper mapping between applet ID and desktop ID
                            // For now, use the most recently created card
                            String cardId = cardDAO.getMostRecentCardId();
                            if (cardId == null) {
                                throw new Exception(
                                        "Không tìm thấy thẻ trong hệ thống.\nVui lòng liên hệ quản trị viên.");
                            }
                            System.out.println("[DEBUG] Desktop Card ID: " + cardId);

                            int balance = cardService.getBalance();

                            // Update database
                            cardDAO.updateLastAccessed(cardId);
                            cardDAO.logTransaction(cardId, "LOGIN", true, null);

                            // Show success and then dashboard
                            showSuccessMessage("Chào mừng!",
                                    "Xác thực thành công.\n\nĐang tải bảng điều khiển của bạn...");

                            // Delay before showing dashboard
                            new Thread(() -> {
                                try {
                                    Thread.sleep(1500);
                                    javafx.application.Platform.runLater(() -> {
                                        showDashboard(cardId, balance);
                                    });
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                }
                            }).start();

                        } catch (Exception e) {
                            showAlert("Lỗi hệ thống", "Xác thực thành công nhưng không thể tải thông tin tài khoản:\n\n"
                                    + e.getMessage());
                            resetLoginForm();
                        }

                    } else {
                        statusLabel.setText("❌ Mã PIN không đúng. Vui lòng kiểm tra và thử lại.");
                        String errorMsg = "Mã PIN bạn nhập không chính xác.\n\n";

                        if (pinResult.remainingTries > 0) {
                            errorMsg += "Số lần thử còn lại: " + pinResult.remainingTries + "\n\n";
                            errorMsg += "⚠️ Quá nhiều lần thử sai sẽ khóa thẻ của bạn.";
                        } else {
                            errorMsg += "🔒 Thẻ đã bị khóa do nhập sai PIN quá nhiều lần.\n\n";
                            errorMsg += "Vui lòng liên hệ quản trị viên để mở khóa.";
                        }

                        showAlert("Xác thực thất bại", errorMsg);
                        resetLoginForm();
                    }
                });

            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    statusLabel.setText("❌ Đã xảy ra lỗi xác thực.");
                    showAlert("Lỗi kết nối",
                            "Có lỗi khi giao tiếp với Thẻ thông minh của bạn:\n\n" +
                                    e.getMessage() + "\n\n" +
                                    "Vui lòng đảm bảo thẻ được cắm đúng cách và thử lại.");
                    resetLoginForm();
                });
            }
        }).start();
    }

    private void resetLoginForm() {
        statusLabel.setText("Sẵn sàng để xác thực");
        statusLabel.getStyleClass().removeAll("status-error", "status-success", "status-loading");
    }

    private void showDashboard(String cardId, int balance) {
        // Create new citizen dashboard window (balance will be loaded from card)
        CitizenDashboardController citizenController = new CitizenDashboardController(cardService, cardId);

        // Replace current scene - keep same window size
        javafx.stage.Stage stage = (javafx.stage.Stage) root.getScene().getWindow();
        double w = stage.getWidth();
        double h = stage.getHeight();
        javafx.scene.Scene citizenScene = new javafx.scene.Scene(citizenController.getRoot(), w, h);

        // Load CSS
        citizenScene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

        stage.setScene(citizenScene);
        stage.setTitle("Hệ thống Quản lý Thẻ Cư dân - Tài khoản của tôi");
        // Don't centerOnScreen to keep window position

        System.out.println("🏠 Bảng điều khiển Cư dân đã tải cho thẻ: " + cardId);
    }

    private void logout() {
        // Disconnect from card
        cardService.disconnect();

        // Reset UI state
        updateConnectionStatus(false);
        statusLabel.setText("Phiên làm việc đã kết thúc. Vui lòng kết nối Thẻ thông minh để tiếp tục.");
        connectButton.setText("Kết nối với Thẻ thông minh");
        connectButton.getStyleClass().removeAll("btn-success");
        connectButton.getStyleClass().add("btn-primary");
        connectButton.setDisable(false);

        // Hide login section
        loginSection.setVisible(false);
        pinField.clear();

        showSuccessMessage("Đã đăng xuất", "Bạn đã đăng xuất an toàn.\n\nCảm ơn bạn đã sử dụng Hệ thống Thẻ Cư dân!");
    }

    // =====================================================
    // UTILITY METHODS
    // =====================================================

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);

        // Style the dialog
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        alert.getDialogPane().getStyleClass().add("alert-dialog");

        alert.showAndWait();
    }

    private void showSuccessMessage(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText("✅ " + title);
        alert.setContentText(message);

        // Style the dialog
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        alert.getDialogPane().getStyleClass().add("success-dialog");

        alert.showAndWait();
    }

    public Parent getRoot() {
        return root;
    }
}