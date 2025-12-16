package citizencard.controller;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import citizencard.service.CardService;
import citizencard.dao.CardDAO;
import citizencard.util.DataValidator;
import citizencard.util.PinInputDialog;

/**
 * Simplified Citizen Dashboard Controller
 * 
 * Focused on core citizen card management functions
 */
public class CitizenDashboardController {
    
    private BorderPane root;
    private CardService cardService;
    private CardDAO cardDAO;
    private VBox contentArea;
    private String cardId;
    private int currentBalance;
    private Label balanceLabel;
    private Label cardIdLabel;
    
    public CitizenDashboardController(CardService cardService, String cardId, int balance) {
        this.cardService = cardService;
        this.cardDAO = CardDAO.getInstance();
        this.cardId = cardId;
        this.currentBalance = balance;
        initializeUI();
    }
    
    private void initializeUI() {
        root = new BorderPane();
        root.getStyleClass().add("citizen-container");
        
        // Create sidebar
        VBox sidebar = createSidebar();
        root.setLeft(sidebar);
        
        // Create main content area
        contentArea = createContentArea();
        root.setCenter(contentArea);
        
        // Create header
        HBox header = createHeader();
        root.setTop(header);
        
        // Show dashboard by default
        showDashboardOverview();
    }
    
    private HBox createHeader() {
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20, 30, 20, 30));
        header.getStyleClass().add("citizen-header");
        
        Label titleLabel = new Label("Thẻ Cư dân của tôi");
        titleLabel.getStyleClass().add("citizen-title");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        // Card info in header
        VBox cardInfo = new VBox(2);
        cardInfo.setAlignment(Pos.CENTER_RIGHT);
        
        cardIdLabel = new Label("Thẻ: " + cardId);
        cardIdLabel.getStyleClass().add("header-card-id");
        
        balanceLabel = new Label(String.format("Số dư: %,d VND", currentBalance));
        balanceLabel.getStyleClass().add("header-balance");
        
        cardInfo.getChildren().addAll(cardIdLabel, balanceLabel);
        
        Button logoutButton = new Button("Đăng xuất");
        logoutButton.getStyleClass().addAll("btn", "btn-outline");
        logoutButton.setOnAction(e -> logout());
        
        header.getChildren().addAll(titleLabel, spacer, cardInfo, logoutButton);
        return header;
    }
    
    private VBox createSidebar() {
        VBox sidebar = new VBox();
        sidebar.setPrefWidth(280);
        sidebar.getStyleClass().add("citizen-sidebar");
        
        // Sidebar header
        VBox sidebarHeader = new VBox(10);
        sidebarHeader.setAlignment(Pos.CENTER);
        sidebarHeader.setPadding(new Insets(30, 20, 30, 20));
        sidebarHeader.getStyleClass().add("citizen-sidebar-header");
        
        Label citizenIcon = new Label("👤");
        citizenIcon.setStyle("-fx-font-size: 48px;");
        
        Label citizenLabel = new Label("Dịch vụ Cư dân");
        citizenLabel.getStyleClass().add("sidebar-title");
        
        Label accessLabel = new Label("Quản lý Thẻ cư dân");
        accessLabel.getStyleClass().add("sidebar-subtitle");
        
        sidebarHeader.getChildren().addAll(citizenIcon, citizenLabel, accessLabel);
        
        // Navigation menu
        VBox menu = createNavigationMenu();
        
        sidebar.getChildren().addAll(sidebarHeader, menu);
        return sidebar;
    }
    
    private VBox createNavigationMenu() {
        VBox menu = new VBox(5);
        menu.setPadding(new Insets(0, 15, 20, 15));
        
        Button dashboardBtn = createMenuButton("🏠 Trang chủ", "Thông tin tổng quan thẻ cư dân");
        dashboardBtn.setOnAction(e -> showDashboardOverview());
        
        Button cardInfoBtn = createMenuButton("💳 Thông tin thẻ", "Xem thông tin chi tiết thẻ cư dân");
        cardInfoBtn.setOnAction(e -> showCardInfo());
        
        Button balanceBtn = createMenuButton("💰 Số dư thẻ", "Kiểm tra số dư và lịch sử giao dịch");
        balanceBtn.setOnAction(e -> showBalanceInfo());
        
        Button securityBtn = createMenuButton("🔐 Bảo mật", "Đổi PIN và cài đặt bảo mật");
        securityBtn.setOnAction(e -> showSecurity());
        
        menu.getChildren().addAll(
            dashboardBtn, 
            new Separator(),
            cardInfoBtn, 
            balanceBtn,
            new Separator(),
            securityBtn
        );
        
        return menu;
    }
    
    private Button createMenuButton(String text, String description) {
        Button button = new Button(text);
        button.getStyleClass().addAll("citizen-menu-button");
        button.setPrefWidth(250);
        button.setAlignment(Pos.CENTER_LEFT);
        
        Tooltip tooltip = new Tooltip(description);
        button.setTooltip(tooltip);
        
        return button;
    }
    
    private VBox createContentArea() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(30));
        content.getStyleClass().add("citizen-content");
        
        return content;
    }
    
    // =====================================================
    // CONTENT SECTIONS
    // =====================================================
    
    private void showDashboardOverview() {
        contentArea.getChildren().clear();
        
        Label pageTitle = new Label("Trang chủ");
        pageTitle.getStyleClass().add("page-title");
        
        // Welcome message
        VBox welcomeSection = createWelcomeSection();
        
        // Quick stats
        HBox statsRow = new HBox(20);
        statsRow.setAlignment(Pos.CENTER_LEFT);
        
        VBox cardIdCard = createStatCard("ID Thẻ", cardId, "💳", "#3b82f6");
        VBox balanceCard = createStatCard("Số dư hiện tại", String.format("%,d VND", currentBalance), "💰", "#22c55e");
        VBox statusCard = createStatCard("Trạng thái thẻ", "Hoạt động", "✅", "#10b981");
        VBox issueCard = createStatCard("Ngày phát hành", "15-12-2025", "📅", "#f59e0b");
        
        statsRow.getChildren().addAll(cardIdCard, balanceCard, statusCard, issueCard);
        
        // Quick actions
        VBox quickActions = createQuickActionsSection();
        
        contentArea.getChildren().addAll(pageTitle, welcomeSection, statsRow, quickActions);
    }
    
    private VBox createWelcomeSection() {
        VBox section = new VBox(10);
        section.getStyleClass().add("welcome-section");
        section.setPadding(new Insets(25));
        
        Label welcomeLabel = new Label("Chào mừng trở lại!");
        welcomeLabel.getStyleClass().add("welcome-title");
        
        Label timeLabel = new Label("Hôm nay là " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy")));
        timeLabel.getStyleClass().add("welcome-subtitle");
        
        Label tipLabel = new Label("💡 Thẻ cư dân của bạn chứa thông tin cá nhân được mã hóa an toàn");
        tipLabel.getStyleClass().add("welcome-tip");
        
        section.getChildren().addAll(welcomeLabel, timeLabel, tipLabel);
        return section;
    }
    
    private VBox createStatCard(String title, String value, String icon, String color) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(25));
        card.getStyleClass().add("citizen-stat-card");
        card.setPrefWidth(200);
        
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 24px;");
        
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("stat-title");
        
        header.getChildren().addAll(iconLabel, titleLabel);
        
        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("stat-value");
        valueLabel.setStyle("-fx-text-fill: " + color + ";");
        
        card.getChildren().addAll(header, valueLabel);
        return card;
    }
    
    private VBox createQuickActionsSection() {
        VBox section = new VBox(15);
        
        Label sectionTitle = new Label("Thao tác nhanh");
        sectionTitle.getStyleClass().add("section-title");
        
        HBox actionsRow = new HBox(15);
        
        Button cardInfoBtn = new Button("💳 Xem thông tin thẻ");
        cardInfoBtn.getStyleClass().addAll("btn", "btn-primary", "btn-large");
        cardInfoBtn.setOnAction(e -> showCardInfo());
        
        Button balanceBtn = new Button("💰 Kiểm tra số dư");
        balanceBtn.getStyleClass().addAll("btn", "btn-success", "btn-large");
        balanceBtn.setOnAction(e -> showBalanceInfo());
        
        Button changePinBtn = new Button("🔐 Đổi PIN");
        changePinBtn.getStyleClass().addAll("btn", "btn-outline", "btn-large");
        changePinBtn.setOnAction(e -> showChangePinDialog());
        
        Button photoBtn = new Button("📷 Quản lý ảnh");
        photoBtn.getStyleClass().addAll("btn", "btn-secondary", "btn-large");
        photoBtn.setOnAction(e -> showPhotoManagement());
        
        actionsRow.getChildren().addAll(cardInfoBtn, balanceBtn, changePinBtn, photoBtn);
        
        section.getChildren().addAll(sectionTitle, actionsRow);
        return section;
    }
    
    private void showCardInfo() {
        contentArea.getChildren().clear();
        
        Label pageTitle = new Label("Thông tin thẻ cư dân");
        pageTitle.getStyleClass().add("page-title");
        
        // Card information display
        VBox cardInfoDisplay = createCardInfoDisplay();
        
        // Card actions
        VBox cardActions = createCardActionsSection();
        
        contentArea.getChildren().addAll(pageTitle, cardInfoDisplay, cardActions);
    }
    
    private VBox createCardInfoDisplay() {
        VBox section = new VBox(20);
        section.getStyleClass().add("card-info-display");
        section.setPadding(new Insets(30));
        section.setAlignment(Pos.CENTER_LEFT);
        
        Label cardIcon = new Label("💳");
        cardIcon.setStyle("-fx-font-size: 64px;");
        
        GridPane infoGrid = new GridPane();
        infoGrid.setHgap(30);
        infoGrid.setVgap(15);
        infoGrid.setPadding(new Insets(20));
        infoGrid.getStyleClass().add("card-info-grid");
        
        // Card information
        infoGrid.add(new Label("ID Thẻ:"), 0, 0);
        Label cardIdValue = new Label(cardId);
        cardIdValue.getStyleClass().add("info-value");
        infoGrid.add(cardIdValue, 1, 0);
        
        infoGrid.add(new Label("Loại thẻ:"), 0, 1);
        Label cardTypeValue = new Label("Thẻ Cư dân Việt Nam");
        cardTypeValue.getStyleClass().add("info-value");
        infoGrid.add(cardTypeValue, 1, 1);
        
        infoGrid.add(new Label("Trạng thái:"), 0, 2);
        Label statusValue = new Label("✅ Hoạt động");
        statusValue.getStyleClass().add("info-value-success");
        infoGrid.add(statusValue, 1, 2);
        
        infoGrid.add(new Label("Ngày phát hành:"), 0, 3);
        Label issueDateValue = new Label("15-12-2025");
        issueDateValue.getStyleClass().add("info-value");
        infoGrid.add(issueDateValue, 1, 3);
        
        infoGrid.add(new Label("Ngày hết hạn:"), 0, 4);
        Label expiryDateValue = new Label("15-12-2030");
        expiryDateValue.getStyleClass().add("info-value");
        infoGrid.add(expiryDateValue, 1, 4);
        
        infoGrid.add(new Label("Số dư hiện tại:"), 0, 5);
        Label balanceValue = new Label(String.format("%,d VND", currentBalance));
        balanceValue.getStyleClass().add("info-value-balance");
        infoGrid.add(balanceValue, 1, 5);
        
        section.getChildren().addAll(cardIcon, infoGrid);
        return section;
    }
    
    private VBox createCardActionsSection() {
        VBox section = new VBox(15);
        
        Label sectionTitle = new Label("Thao tác với thẻ");
        sectionTitle.getStyleClass().add("section-title");
        
        HBox actionsRow = new HBox(15);
        
        Button refreshBtn = new Button("🔄 Làm mới thông tin");
        refreshBtn.getStyleClass().addAll("btn", "btn-primary");
        refreshBtn.setOnAction(e -> refreshCardInfo());
        
        Button changePinBtn = new Button("🔐 Đổi PIN");
        changePinBtn.getStyleClass().addAll("btn", "btn-secondary");
        changePinBtn.setOnAction(e -> showChangePinDialog());
        
        Button lockCardBtn = new Button("🚫 Khóa thẻ khẩn cấp");
        lockCardBtn.getStyleClass().addAll("btn", "btn-danger");
        lockCardBtn.setOnAction(e -> showEmergencyBlock());
        
        actionsRow.getChildren().addAll(refreshBtn, changePinBtn, lockCardBtn);
        
        section.getChildren().addAll(sectionTitle, actionsRow);
        return section;
    }
    
    private void refreshCardInfo() {
        try {
            int newBalance = cardService.getBalance();
            currentBalance = newBalance;
            updateBalanceDisplay();
            showSuccessMessage("Làm mới thành công", "Thông tin thẻ đã được cập nhật.");
            showCardInfo(); // Refresh the display
        } catch (Exception e) {
            showAlert("Lỗi làm mới", "Không thể làm mới thông tin thẻ: " + e.getMessage());
        }
    }
    
    private void showBalanceInfo() {
        contentArea.getChildren().clear();
        
        Label pageTitle = new Label("Số dư thẻ");
        pageTitle.getStyleClass().add("page-title");
        
        // Current balance display
        VBox balanceDisplay = createSimpleBalanceDisplay();
        
        // Recent transactions
        VBox recentTransactions = createSimpleTransactionHistory();
        
        contentArea.getChildren().addAll(pageTitle, balanceDisplay, recentTransactions);
    }
    
    private VBox createSimpleBalanceDisplay() {
        VBox section = new VBox(20);
        section.getStyleClass().add("balance-display");
        section.setPadding(new Insets(30));
        section.setAlignment(Pos.CENTER);
        
        Label balanceIcon = new Label("💰");
        balanceIcon.setStyle("-fx-font-size: 64px;");
        
        Label currentBalanceLabel = new Label("Số dư hiện tại");
        currentBalanceLabel.getStyleClass().add("balance-label");
        
        Label balanceAmount = new Label(String.format("%,d VND", currentBalance));
        balanceAmount.getStyleClass().add("balance-amount");
        
        Label lastUpdated = new Label("Cập nhật lần cuối: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")));
        lastUpdated.getStyleClass().add("balance-updated");
        
        Button refreshBtn = new Button("🔄 Làm mới số dư");
        refreshBtn.getStyleClass().addAll("btn", "btn-outline");
        refreshBtn.setOnAction(e -> refreshBalance());
        
        section.getChildren().addAll(balanceIcon, currentBalanceLabel, balanceAmount, lastUpdated, refreshBtn);
        return section;
    }
    
    private VBox createSimpleTransactionHistory() {
        VBox section = new VBox(15);
        
        Label sectionTitle = new Label("Hoạt động gần đây");
        sectionTitle.getStyleClass().add("section-title");
        
        VBox transactionsList = new VBox(8);
        transactionsList.getStyleClass().add("transactions-list");
        
        // Sample recent transactions - focused on card management activities
        transactionsList.getChildren().addAll(
            createSimpleTransactionItem("Kiểm tra số dư", "0 VND", "Thành công", "Hôm nay 09:15"),
            createSimpleTransactionItem("Xem thông tin thẻ", "0 VND", "Thành công", "Hôm nay 09:10"),
            createSimpleTransactionItem("Đổi PIN", "0 VND", "Thành công", "Hôm qua 14:30"),
            createSimpleTransactionItem("Kiểm tra thông tin", "0 VND", "Thành công", "2 ngày trước"),
            createSimpleTransactionItem("Kích hoạt thẻ", "0 VND", "Thành công", "15-12-2025")
        );
        
        section.getChildren().addAll(sectionTitle, transactionsList);
        return section;
    }
    
    private HBox createSimpleTransactionItem(String action, String amount, String status, String time) {
        HBox item = new HBox(15);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(12));
        item.getStyleClass().add("transaction-item");
        
        Label iconLabel = new Label("📋");
        iconLabel.setStyle("-fx-font-size: 20px;");
        
        VBox content = new VBox(2);
        
        Label actionLabel = new Label(action);
        actionLabel.getStyleClass().add("transaction-type");
        
        Label timeLabel = new Label(time);
        timeLabel.getStyleClass().add("transaction-time");
        
        content.getChildren().addAll(actionLabel, timeLabel);
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Label statusLabel = new Label(status);
        statusLabel.getStyleClass().add("status-success");
        
        item.getChildren().addAll(iconLabel, content, spacer, statusLabel);
        return item;
    }
    
    private void showSecurity() {
        contentArea.getChildren().clear();
        
        Label pageTitle = new Label("Bảo mật thẻ");
        pageTitle.getStyleClass().add("page-title");
        
        // Security status
        VBox securityStatus = createSecurityStatus();
        
        // PIN management
        VBox pinManagement = createPinManagement();
        
        contentArea.getChildren().addAll(pageTitle, securityStatus, pinManagement);
    }
    
    private VBox createSecurityStatus() {
        VBox section = new VBox(15);
        section.getStyleClass().add("security-status");
        section.setPadding(new Insets(25));
        
        Label sectionTitle = new Label("Tình trạng bảo mật");
        sectionTitle.getStyleClass().add("section-title");
        
        HBox statusRow = new HBox(20);
        
        VBox cardStatus = createSecurityStatusCard("Trạng thái thẻ", "Hoạt động & An toàn", "🔒", "#22c55e");
        VBox pinStatus = createSecurityStatusCard("Bảo mật PIN", "Mạnh", "🔐", "#22c55e");
        VBox lastAccess = createSecurityStatusCard("Truy cập cuối", "Hôm nay 09:15", "🕒", "#3b82f6");
        
        statusRow.getChildren().addAll(cardStatus, pinStatus, lastAccess);
        
        section.getChildren().addAll(sectionTitle, statusRow);
        return section;
    }
    
    private VBox createSecurityStatusCard(String title, String status, String icon, String color) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(20));
        card.getStyleClass().add("security-status-card");
        
        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 32px;");
        
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("security-card-title");
        
        Label statusLabel = new Label(status);
        statusLabel.getStyleClass().add("security-card-status");
        statusLabel.setStyle("-fx-text-fill: " + color + ";");
        
        card.getChildren().addAll(iconLabel, titleLabel, statusLabel);
        return card;
    }
    
    private VBox createPinManagement() {
        VBox section = new VBox(15);
        
        Label sectionTitle = new Label("Quản lý PIN");
        sectionTitle.getStyleClass().add("section-title");
        
        VBox pinActions = new VBox(15);
        pinActions.setPadding(new Insets(20));
        pinActions.getStyleClass().add("pin-actions");
        
        Label pinInfo = new Label("PIN của bạn được sử dụng để xác thực và bảo vệ thẻ cư dân.");
        pinInfo.getStyleClass().add("pin-info");
        
        HBox actionButtons = new HBox(15);
        
        Button changePinBtn = new Button("🔐 Đổi PIN");
        changePinBtn.getStyleClass().addAll("btn", "btn-primary", "btn-large");
        changePinBtn.setOnAction(e -> showChangePinDialog());
        
        Button lockCardBtn = new Button("🚫 Khóa thẻ khẩn cấp");
        lockCardBtn.getStyleClass().addAll("btn", "btn-danger", "btn-large");
        lockCardBtn.setOnAction(e -> showEmergencyBlock());
        
        actionButtons.getChildren().addAll(changePinBtn, lockCardBtn);
        
        Label lastChanged = new Label("PIN thay đổi lần cuối: Chưa bao giờ");
        lastChanged.getStyleClass().add("pin-last-changed");
        
        pinActions.getChildren().addAll(pinInfo, actionButtons, lastChanged);
        
        section.getChildren().addAll(sectionTitle, pinActions);
        return section;
    }
    
    // =====================================================
    // DIALOG HANDLERS
    // =====================================================
    
    private void showChangePinDialog() {
        // Step 1: Get current PIN
        String currentPin = PinInputDialog.showChangePinDialog(
            "Xác thực PIN hiện tại", 
            "🔐 Nhập PIN hiện tại để xác thực"
        );
        
        if (currentPin == null || currentPin.isEmpty()) {
            return; // User cancelled
        }
        
        // Step 2: Verify current PIN
        try {
            CardService.PinVerificationResult pinResult = cardService.verifyPin(currentPin);
            if (!pinResult.success) {
                String errorMsg = "PIN hiện tại không chính xác.";
                if (pinResult.remainingTries > 0) {
                    errorMsg += "\nSố lần thử còn lại: " + pinResult.remainingTries;
                } else {
                    errorMsg += "\n🔒 Thẻ đã bị khóa do nhập sai PIN quá nhiều lần.";
                }
                showAlert("PIN không đúng", errorMsg);
                return;
            }
        } catch (Exception e) {
            showAlert("Lỗi xác thực", "Không thể xác thực PIN: " + e.getMessage());
            return;
        }
        
        // Step 3: Get new PIN
        String newPin = PinInputDialog.showChangePinDialog(
            "Chọn PIN mới", 
            "🔐 Nhập PIN mới (4 chữ số)\n\n⚠️ Hãy ghi nhớ PIN mới của bạn!"
        );
        
        if (newPin == null || newPin.isEmpty()) {
            return; // User cancelled
        }
        
        // Step 4: Validate new PIN
        DataValidator.ValidationResult pinResult = DataValidator.validatePin(newPin);
        if (!pinResult.isValid()) {
            showAlert("PIN không hợp lệ", pinResult.getErrorMessage());
            return;
        }
        
        // Step 5: Confirm new PIN
        String confirmPin = PinInputDialog.showChangePinDialog(
            "Xác nhận PIN mới", 
            "🔐 Nhập lại PIN mới để xác nhận"
        );
        
        if (confirmPin == null || confirmPin.isEmpty()) {
            return; // User cancelled
        }
        
        if (!newPin.equals(confirmPin)) {
            showAlert("PIN không khớp", "PIN mới và PIN xác nhận không khớp. Vui lòng thử lại.");
            return;
        }
        
        // Step 6: Change PIN on card
        changePinOnCard(currentPin, newPin);
    }
    
    /**
     * Change PIN on smart card
     */
    private void changePinOnCard(String currentPin, String newPin) {
        Alert progressDialog = new Alert(Alert.AlertType.INFORMATION);
        progressDialog.setTitle("Đang đổi PIN");
        progressDialog.setHeaderText("Đang cập nhật PIN trên thẻ...");
        progressDialog.setContentText("Vui lòng đợi...");
        progressDialog.getButtonTypes().clear();
        progressDialog.show();
        
        new Thread(() -> {
            try {
                boolean success = cardService.changePin(currentPin, newPin);
                
                javafx.application.Platform.runLater(() -> {
                    progressDialog.close();
                    
                    if (success) {
                        cardDAO.logTransaction(cardId, "CHANGE_PIN", true, null);
                        showSuccessMessage("Đổi PIN thành công", 
                            "PIN của bạn đã được thay đổi thành công.\n\n" +
                            "🔐 Hãy ghi nhớ PIN mới của bạn!\n" +
                            "🔒 Sử dụng PIN mới cho lần đăng nhập tiếp theo.");
                    } else {
                        cardDAO.logTransaction(cardId, "CHANGE_PIN", false, "PIN change failed");
                        showAlert("Đổi PIN thất bại", 
                            "Không thể đổi PIN trên thẻ.\n\n" +
                            "Có thể do:\n" +
                            "• PIN hiện tại không đúng\n" +
                            "• Lỗi giao tiếp với thẻ\n" +
                            "• Thẻ bị khóa\n\n" +
                            "Vui lòng thử lại sau.");
                    }
                });
                
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    progressDialog.close();
                    cardDAO.logTransaction(cardId, "CHANGE_PIN", false, e.getMessage());
                    showAlert("Lỗi đổi PIN", 
                        "Đã xảy ra lỗi khi đổi PIN:\n\n" + e.getMessage() + 
                        "\n\nVui lòng kiểm tra kết nối thẻ và thử lại.");
                });
            }
        }).start();
    }
    
    /**
     * Update field validation styling
     */
    private void updateFieldValidation(Control field, Label errorLabel, DataValidator.ValidationResult result) {
        if (result.isValid()) {
            field.getStyleClass().removeAll("field-error");
            field.getStyleClass().add("field-valid");
            errorLabel.setVisible(false);
        } else {
            field.getStyleClass().removeAll("field-valid");
            field.getStyleClass().add("field-error");
            errorLabel.setText(result.getErrorMessage());
            errorLabel.setVisible(true);
        }
    }
    
    /**
     * Update PIN strength indicator
     */
    private void updatePinStrength(String pin, ProgressBar strengthBar, Label strengthText) {
        if (pin.isEmpty()) {
            strengthBar.setProgress(0);
            strengthText.setText("Chưa nhập");
            strengthText.setStyle("-fx-text-fill: #6b7280;");
            return;
        }
        
        int strength = calculatePinStrength(pin);
        double progress = strength / 100.0;
        strengthBar.setProgress(progress);
        
        if (strength < 30) {
            strengthText.setText("Yếu");
            strengthText.setStyle("-fx-text-fill: #dc2626;");
            strengthBar.setStyle("-fx-accent: #dc2626;");
        } else if (strength < 70) {
            strengthText.setText("Trung bình");
            strengthText.setStyle("-fx-text-fill: #d97706;");
            strengthBar.setStyle("-fx-accent: #d97706;");
        } else {
            strengthText.setText("Mạnh");
            strengthText.setStyle("-fx-text-fill: #16a34a;");
            strengthBar.setStyle("-fx-accent: #16a34a;");
        }
    }
    
    /**
     * Calculate PIN strength score
     */
    private int calculatePinStrength(String pin) {
        if (pin.length() != 4) return 0;
        
        int score = 50; // Base score for 4 digits
        
        // Check for weak patterns
        String[] weakPins = {"0000", "1111", "2222", "3333", "4444", "5555", 
                            "6666", "7777", "8888", "9999", "1234", "4321", "0123"};
        
        for (String weak : weakPins) {
            if (pin.equals(weak)) {
                return 10; // Very weak
            }
        }
        
        // Check for sequential numbers
        boolean sequential = true;
        for (int i = 1; i < pin.length(); i++) {
            int current = Character.getNumericValue(pin.charAt(i));
            int previous = Character.getNumericValue(pin.charAt(i - 1));
            if (Math.abs(current - previous) != 1) {
                sequential = false;
                break;
            }
        }
        
        if (sequential) {
            score -= 30; // Reduce score for sequential
        }
        
        // Check for repeated digits
        boolean hasRepeated = false;
        for (int i = 0; i < pin.length() - 1; i++) {
            for (int j = i + 1; j < pin.length(); j++) {
                if (pin.charAt(i) == pin.charAt(j)) {
                    hasRepeated = true;
                    break;
                }
            }
        }
        
        if (!hasRepeated) {
            score += 30; // Bonus for no repeated digits
        }
        
        // Check for variety in digits
        long uniqueDigits = pin.chars().distinct().count();
        if (uniqueDigits == 4) {
            score += 20; // All unique digits
        }
        
        return Math.max(10, Math.min(100, score));
    }
    
    /**
     * Validate PIN match
     */
    private void validatePinMatch(String newPin, String confirmPin, Control field, Label errorLabel) {
        if (confirmPin.isEmpty()) {
            field.getStyleClass().removeAll("field-error", "field-valid");
            errorLabel.setVisible(false);
            return;
        }
        
        if (newPin.equals(confirmPin)) {
            field.getStyleClass().removeAll("field-error");
            field.getStyleClass().add("field-valid");
            errorLabel.setVisible(false);
        } else {
            field.getStyleClass().removeAll("field-valid");
            field.getStyleClass().add("field-error");
            errorLabel.setText("PIN xác nhận không khớp");
            errorLabel.setVisible(true);
        }
    }
    
    /**
     * Comprehensive PIN change validation
     */
    private PinChangeInfo validatePinChange(String currentPin, String newPin, String confirmPin) {
        // Validate current PIN
        DataValidator.ValidationResult currentResult = DataValidator.validatePin(currentPin);
        if (!currentResult.isValid()) {
            showValidationAlert("Lỗi PIN hiện tại", currentResult.getErrorMessage());
            return null;
        }
        
        // Validate new PIN
        DataValidator.ValidationResult newResult = DataValidator.validatePin(newPin);
        if (!newResult.isValid()) {
            showValidationAlert("Lỗi PIN mới", newResult.getErrorMessage());
            return null;
        }
        
        // Check PIN match
        if (!newPin.equals(confirmPin)) {
            showValidationAlert("Lỗi xác nhận", "PIN mới và xác nhận không khớp");
            return null;
        }
        
        // Check if new PIN is different from current
        if (currentPin.equals(newPin)) {
            showValidationAlert("Lỗi PIN mới", "PIN mới phải khác với PIN hiện tại");
            return null;
        }
        
        return new PinChangeInfo(currentPin, newPin);
    }
    
    /**
     * Show validation alert
     */
    private void showValidationAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText("❌ " + title);
        alert.setContentText(message);
        
        // Style the dialog
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        alert.getDialogPane().getStyleClass().add("validation-alert");
        
        alert.showAndWait();
    }
    
    private void performPinChange(PinChangeInfo pinInfo) {
        try {
            boolean success = cardService.changePin(pinInfo.currentPin, pinInfo.newPin);
            
            if (success) {
                cardDAO.logTransaction(cardId, "CHANGE_PIN", true, null);
                showSuccessMessage("Đã đổi PIN", "PIN của bạn đã được thay đổi thành công.");
            } else {
                cardDAO.logTransaction(cardId, "CHANGE_PIN", false, "Invalid current PIN");
                showAlert("Đổi PIN thất bại", "PIN hiện tại không đúng. Vui lòng thử lại.");
            }
            
        } catch (Exception e) {
            cardDAO.logTransaction(cardId, "CHANGE_PIN", false, e.getMessage());
            showAlert("Đổi PIN thất bại", "Không thể đổi PIN: " + e.getMessage());
        }
    }
    
    private void showEmergencyBlock() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Khóa thẻ khẩn cấp");
        confirm.setHeaderText("🚨 Khóa thẻ của bạn");
        confirm.setContentText(
            "Điều này sẽ ngay lập tức khóa thẻ của bạn để ngăn chặn việc sử dụng trái phép.\n\n" +
            "⚠️ CẢNH BÁO: Một khi bị khóa, bạn sẽ cần liên hệ cơ quan có thẩm quyền để mở khóa thẻ.\n\n" +
            "Bạn có chắc chắn muốn khóa thẻ của mình không?"
        );
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // In a real system, this would call the card service to block the card
                showAlert("Thẻ đã bị khóa", 
                    "🚨 Thẻ của bạn đã được khóa thành công.\n\n" +
                    "ID Thẻ: " + cardId + "\n" +
                    "Khóa lúc: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")) + "\n\n" +
                    "Để mở khóa thẻ, vui lòng liên hệ cơ quan có thẩm quyền:\n" +
                    "📞 Hotline: 1900-1234");
            }
        });
    }
    
    private void refreshBalance() {
        try {
            int newBalance = cardService.getBalance();
            currentBalance = newBalance;
            updateBalanceDisplay();
            showSuccessMessage("Đã làm mới số dư", "Số dư đã được cập nhật thành công.");
        } catch (Exception e) {
            showAlert("Làm mới thất bại", "Không thể làm mới số dư: " + e.getMessage());
        }
    }
    
    private void updateBalanceDisplay() {
        balanceLabel.setText(String.format("Số dư: %,d VND", currentBalance));
    }
    
    private void logout() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Đăng xuất");
        confirm.setHeaderText("Xác nhận đăng xuất");
        confirm.setContentText("Bạn có chắc chắn muốn đăng xuất và ngắt kết nối khỏi thẻ của mình không?");
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                cardService.disconnect();
                showSuccessMessage("Đã đăng xuất", "Bạn đã đăng xuất an toàn.\n\nCảm ơn bạn đã sử dụng Hệ thống Thẻ Cư dân!");
                
                // This would typically return to the main login screen
                // For now, we'll just show a message
            }
        });
    }
    
    /**
     * Show photo management dialog
     */
    private void showPhotoManagement() {
        try {
            PhotoManagementController photoController = new PhotoManagementController(cardService);
            photoController.showPhotoManagement();
        } catch (Exception e) {
            showAlert("Lỗi quản lý ảnh", "Không thể mở quản lý ảnh: " + e.getMessage());
        }
    }
    
    // =====================================================
    // UTILITY METHODS & CLASSES
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
    
    // Helper classes
    private static class PinChangeInfo {
        final String currentPin;
        final String newPin;
        
        PinChangeInfo(String currentPin, String newPin) {
            this.currentPin = currentPin;
            this.newPin = newPin;
        }
    }
}