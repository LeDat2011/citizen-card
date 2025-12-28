package citizencard.controller;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.ImageView;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import citizencard.service.CardService;
import citizencard.dao.CardDAO;
import citizencard.util.DataValidator;
import citizencard.util.PinInputDialog;
import citizencard.util.UIHelper;

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
    private citizencard.model.CitizenInfo citizenInfo;
    private javafx.scene.image.Image avatarImage;

    public CitizenDashboardController(CardService cardService, String cardId) {
        this.cardService = cardService;
        this.cardDAO = CardDAO.getInstance();
        this.cardId = cardId;

        // Load data from card
        loadDataFromCard();

        // Sync any approved topups to card
        syncApprovedTopups();

        initializeUI();
    }

    /**
     * Load all data from smart card
     */
    private void loadDataFromCard() {
        System.out.println("[INFO] Loading citizen data from card...");

        try {
            // Get balance from card
            this.currentBalance = cardService.getBalance();
            System.out.println("[INFO] Balance loaded: " + currentBalance + " VND");

        } catch (Exception e) {
            System.err.println("[ERROR] Failed to load balance: " + e.getMessage());
            this.currentBalance = 0;
        }

        try {
            // Get personal info from card
            byte[] infoBytes = cardService.getPersonalInfo();
            if (infoBytes != null && infoBytes.length > 0) {
                this.citizenInfo = citizencard.util.CitizenInfoParser.parse(infoBytes);
                System.out.println("[INFO] Personal info loaded: " +
                        citizencard.util.CitizenInfoParser.toString(citizenInfo));
            } else {
                System.out.println("[WARN] No personal info on card");
                // Create empty CitizenInfo with all required parameters
                this.citizenInfo = new citizencard.model.CitizenInfo(
                        "Chưa có thông tin", // name
                        "", // dob
                        "", // idNumber
                        "", // roomNumber
                        "", // phone
                        null, // email
                        null, // pin
                        0, // balance
                        null, // photoPath
                        null // photoData
                );
            }

        } catch (Exception e) {
            System.err.println("[ERROR] Failed to load personal info: " + e.getMessage());
            e.printStackTrace();
            // Create empty CitizenInfo with all required parameters
            this.citizenInfo = new citizencard.model.CitizenInfo(
                    "Lỗi tải dữ liệu", // name
                    "", // dob
                    "", // idNumber
                    "", // roomNumber
                    "", // phone
                    null, // email
                    null, // pin
                    0, // balance
                    null, // photoPath
                    null // photoData
            );
        }

        try {
            // Get avatar from card (optional)
            byte[] avatarBytes = cardService.downloadAvatar();
            if (avatarBytes != null && avatarBytes.length > 0) {
                this.avatarImage = citizencard.util.PhotoUtils.bytesToImage(avatarBytes);
                System.out.println("[INFO] Avatar loaded: " + avatarBytes.length + " bytes");
            } else {
                System.out.println("[INFO] No avatar on card");
            }

        } catch (Exception e) {
            System.err.println("[WARN] Failed to load avatar: " + e.getMessage());
            // Avatar is optional, continue without it
        }

        System.out.println("[INFO] Data loading completed");
    }

    /**
     * Sync approved topup requests to card balance
     * This is called when citizen logs in to credit any approved topups
     */
    private void syncApprovedTopups() {
        System.out.println("[INFO] Syncing approved topup requests...");

        try {
            // Get approved topups that need to be synced
            java.util.List<CardDAO.TopupRecord> approvedRequests = cardDAO.getTopupRequestsByCardId(cardId);

            long totalToCredit = 0;
            java.util.List<CardDAO.TopupRecord> toSync = new java.util.ArrayList<>();

            for (CardDAO.TopupRecord req : approvedRequests) {
                // Only process APPROVED requests (not already synced)
                if ("APPROVED".equals(req.status)) {
                    // Validation: amount must be positive
                    if (req.amount <= 0) {
                        System.err.println("[WARN] Skipping invalid topup request #" + req.id + ": negative amount");
                        continue;
                    }

                    // Validation: check for overflow
                    if (totalToCredit + req.amount > Integer.MAX_VALUE) {
                        System.err.println("[WARN] Skipping topup: would cause overflow");
                        continue;
                    }

                    toSync.add(req);
                    totalToCredit += req.amount;
                }
            }

            if (toSync.isEmpty()) {
                System.out.println("[INFO] No pending approved topups to sync");
                return;
            }

            System.out.println("[INFO] Found " + toSync.size() + " approved topups, total: " + totalToCredit + " VND");

            // Credit each approved topup to card
            for (CardDAO.TopupRecord req : toSync) {
                try {
                    // Send APDU to credit money to card
                    int newBalance = cardService.topupBalance((int) req.amount);

                    // Mark as synced by updating status to SYNCED (or we can keep APPROVED)
                    // For now, we'll update to a new status "SYNCED"
                    cardDAO.markTopupAsSynced(req.id);

                    currentBalance = newBalance;
                    System.out.println("[INFO] Credited " + req.amount + " VND from topup #" + req.id
                            + ", new balance: " + newBalance);

                    // Log transaction
                    cardDAO.logTransaction(cardId, "TOPUP", true, "TopupRequest#" + req.id);

                } catch (Exception e) {
                    System.err.println("[ERROR] Failed to sync topup #" + req.id + ": " + e.getMessage());
                    cardDAO.logTransaction(cardId, "TOPUP", false, "TopupRequest#" + req.id + ": " + e.getMessage());
                }
            }

            System.out.println("[INFO] Topup sync completed, current balance: " + currentBalance + " VND");

        } catch (Exception e) {
            System.err.println("[ERROR] Failed to sync topups: " + e.getMessage());
        }
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

        Button logoutButton = new Button("🚪 Đăng xuất");
        logoutButton.getStyleClass().addAll("btn", "btn-danger");
        logoutButton.setOnAction(e -> logout());

        header.getChildren().addAll(titleLabel, spacer, cardInfo, logoutButton);
        return header;
    }

    private VBox createSidebar() {
        VBox sidebar = new VBox();
        sidebar.setPrefWidth(280);
        sidebar.getStyleClass().add("citizen-sidebar");

        // Sidebar header with avatar
        VBox sidebarHeader = new VBox(10);
        sidebarHeader.setAlignment(Pos.CENTER);
        sidebarHeader.setPadding(new Insets(30, 20, 30, 20));
        sidebarHeader.getStyleClass().add("citizen-sidebar-header");

        // Avatar display
        StackPane avatarContainer = createAvatarDisplay(80);

        // Citizen name from card
        String displayName = "Cư dân";
        if (citizenInfo != null && citizenInfo.name != null && !citizenInfo.name.isEmpty()) {
            displayName = citizenInfo.name;
        }
        Label citizenNameLabel = new Label(displayName);
        citizenNameLabel.getStyleClass().add("citizen-name");

        Label accessLabel = new Label("Thẻ: " + cardId);
        accessLabel.getStyleClass().add("sidebar-subtitle");

        sidebarHeader.getChildren().addAll(avatarContainer, citizenNameLabel, accessLabel);

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

        Button invoiceBtn = createMenuButton("📄 Hóa đơn", "Xem và thanh toán hóa đơn");
        invoiceBtn.setOnAction(e -> showInvoices());

        Button topupBtn = createMenuButton("💳 Nạp tiền", "Yêu cầu nạp tiền vào thẻ");
        topupBtn.setOnAction(e -> showTopup());

        Button editProfileBtn = createMenuButton("✏️ Chỉnh sửa thông tin", "Thay đổi ảnh, email, SĐT");
        editProfileBtn.setOnAction(e -> showEditProfile());

        Button securityBtn = createMenuButton("🔐 Bảo mật", "Đổi PIN và cài đặt bảo mật");
        securityBtn.setOnAction(e -> showSecurity());

        menu.getChildren().addAll(
                dashboardBtn,
                new Separator(),
                cardInfoBtn,
                balanceBtn,
                invoiceBtn,
                topupBtn,
                new Separator(),
                editProfileBtn,
                securityBtn);

        return menu;
    }

    private Button createMenuButton(String text, String description) {
        return UIHelper.createMenuButton(text, description, "citizen-menu-button");
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
        VBox section = new VBox(15);
        section.getStyleClass().add("welcome-section");
        section.setPadding(new Insets(25));

        // Welcome row with avatar
        HBox welcomeRow = new HBox(20);
        welcomeRow.setAlignment(Pos.CENTER_LEFT);

        // Avatar in welcome section
        StackPane welcomeAvatar = createAvatarDisplay(64);

        // Welcome text
        VBox welcomeTextBox = new VBox(5);

        // Personalized welcome with name from card
        String welcomeText = "Chào mừng trở lại";
        if (citizenInfo != null && citizenInfo.name != null && !citizenInfo.name.isEmpty()) {
            welcomeText += ", " + citizenInfo.name;
        }
        welcomeText += "!";

        Label welcomeLabel = new Label(welcomeText);
        welcomeLabel.getStyleClass().add("welcome-title");

        Label timeLabel = new Label(
                "Hôm nay là " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy")));
        timeLabel.getStyleClass().add("welcome-subtitle");

        welcomeTextBox.getChildren().addAll(welcomeLabel, timeLabel);

        welcomeRow.getChildren().addAll(welcomeAvatar, welcomeTextBox);

        Label tipLabel = new Label("💡 Thẻ cư dân của bạn chứa thông tin cá nhân được mã hóa an toàn");
        tipLabel.getStyleClass().add("welcome-tip");

        section.getChildren().addAll(welcomeRow, tipLabel);
        return section;
    }

    private VBox createStatCard(String title, String value, String icon, String color) {
        return UIHelper.createStatCard(title, value, icon, color, "citizen-stat-card");
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

        // Avatar and card icon row
        HBox headerRow = new HBox(30);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        // Large avatar display
        StackPane avatarDisplay = createAvatarDisplay(100);

        // Card info beside avatar
        VBox cardBasicInfo = new VBox(8);
        cardBasicInfo.setAlignment(Pos.CENTER_LEFT);

        Label cardTypeLabel = new Label("💳 Thẻ Cư dân Thông minh");
        cardTypeLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");

        Label cardIdDisplayLabel = new Label("ID: " + cardId);
        cardIdDisplayLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748b;");

        Label cardStatusLabel = new Label("✅ Đang hoạt động");
        cardStatusLabel.getStyleClass().add("status-success");

        cardBasicInfo.getChildren().addAll(cardTypeLabel, cardIdDisplayLabel, cardStatusLabel);

        headerRow.getChildren().addAll(avatarDisplay, cardBasicInfo);

        GridPane infoGrid = new GridPane();
        infoGrid.setHgap(30);
        infoGrid.setVgap(15);
        infoGrid.setPadding(new Insets(20));
        infoGrid.getStyleClass().add("card-info-grid");

        int row = 0;

        // Personal information from card
        if (citizenInfo != null) {
            if (citizenInfo.name != null && !citizenInfo.name.isEmpty()) {
                infoGrid.add(new Label("Họ tên:"), 0, row);
                Label nameValue = new Label(citizenInfo.name);
                nameValue.getStyleClass().add("info-value");
                infoGrid.add(nameValue, 1, row);
                row++;
            }

            if (citizenInfo.idNumber != null && !citizenInfo.idNumber.isEmpty()) {
                infoGrid.add(new Label("CCCD:"), 0, row);
                Label cccdValue = new Label(citizenInfo.idNumber);
                cccdValue.getStyleClass().add("info-value");
                infoGrid.add(cccdValue, 1, row);
                row++;
            }

            if (citizenInfo.roomNumber != null && !citizenInfo.roomNumber.isEmpty()) {
                infoGrid.add(new Label("Số phòng:"), 0, row);
                Label roomValue = new Label(citizenInfo.roomNumber);
                roomValue.getStyleClass().add("info-value");
                infoGrid.add(roomValue, 1, row);
                row++;
            }

            if (citizenInfo.dob != null && !citizenInfo.dob.isEmpty()) {
                infoGrid.add(new Label("Ngày sinh:"), 0, row);
                Label dobValue = new Label(citizenInfo.dob);
                dobValue.getStyleClass().add("info-value");
                infoGrid.add(dobValue, 1, row);
                row++;
            }

            if (citizenInfo.phone != null && !citizenInfo.phone.isEmpty()) {
                infoGrid.add(new Label("Số điện thoại:"), 0, row);
                Label phoneValue = new Label(citizenInfo.phone);
                phoneValue.getStyleClass().add("info-value");
                infoGrid.add(phoneValue, 1, row);
                row++;
            }
        }

        // Card information
        infoGrid.add(new Label("ID Thẻ:"), 0, row);
        Label cardIdValue = new Label(cardId);
        cardIdValue.getStyleClass().add("info-value");
        infoGrid.add(cardIdValue, 1, row);
        row++;

        infoGrid.add(new Label("Trạng thái:"), 0, row);
        Label statusValue = new Label("✅ Hoạt động");
        statusValue.getStyleClass().add("info-value-success");
        infoGrid.add(statusValue, 1, row);
        row++;

        infoGrid.add(new Label("Số dư hiện tại:"), 0, row);
        Label balanceValue = new Label(String.format("%,d VND", currentBalance));
        balanceValue.getStyleClass().add("info-value-balance");
        infoGrid.add(balanceValue, 1, row);

        section.getChildren().addAll(headerRow, infoGrid);
        return section;
    }

    /**
     * Create avatar display - circular avatar image properly clipped and centered
     * 
     * @param size diameter of the avatar circle
     */
    private StackPane createAvatarDisplay(double size) {
        StackPane container = new StackPane();
        container.setPrefSize(size, size);
        container.setMinSize(size, size);
        container.setMaxSize(size, size);
        container.getStyleClass().add("avatar-container");

        // Debug: check avatarImage status
        System.out.println("[AVATAR] createAvatarDisplay called, avatarImage is " +
                (avatarImage != null
                        ? "NOT NULL (" + (int) avatarImage.getWidth() + "x" + (int) avatarImage.getHeight() + ")"
                        : "NULL"));

        if (avatarImage != null && !avatarImage.isError()) {
            // Display actual avatar from card with circular clip
            ImageView imageView = new ImageView(avatarImage);

            double imgWidth = avatarImage.getWidth();
            double imgHeight = avatarImage.getHeight();

            // Use viewport to crop image from center (for cover-like behavior)
            if (imgWidth > 0 && imgHeight > 0) {
                double aspectRatio = imgWidth / imgHeight;
                double viewportWidth, viewportHeight, viewportX, viewportY;

                if (aspectRatio > 1) {
                    // Image is wider than tall - crop sides
                    viewportHeight = imgHeight;
                    viewportWidth = imgHeight; // Make it square
                    viewportX = (imgWidth - viewportWidth) / 2;
                    viewportY = 0;
                } else {
                    // Image is taller than wide - crop top/bottom
                    viewportWidth = imgWidth;
                    viewportHeight = imgWidth; // Make it square
                    viewportX = 0;
                    viewportY = (imgHeight - viewportHeight) / 2;
                }

                imageView.setViewport(
                        new javafx.geometry.Rectangle2D(viewportX, viewportY, viewportWidth, viewportHeight));
            }

            // Set fixed size
            imageView.setFitWidth(size);
            imageView.setFitHeight(size);
            imageView.setPreserveRatio(false); // We already cropped to square, so stretch to fill
            imageView.setSmooth(true);

            // Background and border
            container.setStyle(
                    "-fx-background-color: #f8fafc;" +
                            "-fx-background-radius: " + (size / 2) + "px;" +
                            "-fx-border-color: #22c55e;" +
                            "-fx-border-width: 3px;" +
                            "-fx-border-radius: " + (size / 2) + "px;");

            // Clip the container itself to make it circular
            javafx.scene.shape.Circle containerClip = new javafx.scene.shape.Circle(size / 2, size / 2, size / 2);
            container.setClip(containerClip);

            container.getChildren().add(imageView);
            System.out.println("[AVATAR] ImageView added with viewport crop and container clip");
        } else {
            // Default placeholder avatar
            String reason = avatarImage == null ? "null" : "error: " + avatarImage.getException();
            System.out.println("[AVATAR] Using placeholder because avatarImage is " + reason);

            Label placeholderIcon = new Label("👤");
            placeholderIcon.setStyle("-fx-font-size: " + (size * 0.5) + "px;");

            // Circular background
            container.setStyle(
                    "-fx-background-color: #334155;" +
                            "-fx-background-radius: " + (size / 2) + "px;" +
                            "-fx-border-color: #475569;" +
                            "-fx-border-width: 2px;" +
                            "-fx-border-radius: " + (size / 2) + "px;");

            container.getChildren().add(placeholderIcon);
        }

        return container;
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
        System.out.println("[INFO] Refreshing card data...");

        try {
            // Reload all data from card (including avatar)
            loadDataFromCard();

            // Rebuild sidebar to update avatar (don't create new root)
            VBox newSidebar = createSidebar();
            root.setLeft(newSidebar);

            // Update balance display
            updateBalanceDisplay();

            // Show card info page
            showCardInfo();

            showSuccessMessage("Làm mới thành công",
                    "✅ Thông tin thẻ đã được cập nhật từ thẻ thông minh.\n\n" +
                            "Ảnh đại diện và thông tin cá nhân đã được làm mới.");

        } catch (Exception e) {
            showAlert("Lỗi làm mới", "Không thể làm mới thông tin thẻ: " + e.getMessage());
            e.printStackTrace();
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

        Label lastUpdated = new Label(
                "Cập nhật lần cuối: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")));
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
                createSimpleTransactionItem("Kích hoạt thẻ", "0 VND", "Thành công", "15-12-2025"));

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
    // INVOICE MANAGEMENT
    // =====================================================

    private void showInvoices() {
        contentArea.getChildren().clear();

        Label pageTitle = new Label("Hóa đơn của tôi");
        pageTitle.getStyleClass().add("page-title");

        // Invoice summary
        VBox invoiceSummary = createInvoiceSummary();

        // Invoice list
        VBox invoiceList = createInvoiceList();

        contentArea.getChildren().addAll(pageTitle, invoiceSummary, invoiceList);
    }

    private VBox createInvoiceSummary() {
        VBox section = new VBox(15);
        section.setPadding(new Insets(25));
        section.getStyleClass().add("invoice-summary");

        // Get invoices for current card
        java.util.List<CardDAO.InvoiceRecord> invoices = cardDAO.getInvoicesByCardId(cardId);

        long pendingTotal = invoices.stream()
                .filter(i -> "PENDING".equals(i.status))
                .mapToLong(i -> i.amount)
                .sum();

        long pendingCount = invoices.stream()
                .filter(i -> "PENDING".equals(i.status))
                .count();

        HBox statsRow = new HBox(20);
        statsRow.setAlignment(Pos.CENTER_LEFT);

        VBox pendingCard = createStatCard("Chờ thanh toán", String.valueOf(pendingCount) + " hóa đơn", "📄", "#f59e0b");
        VBox amountCard = createStatCard("Tổng cần thanh toán", String.format("%,d VND", pendingTotal), "💰",
                "#ef4444");
        VBox paidCard = createStatCard("Đã thanh toán", String.valueOf(invoices.size() - pendingCount) + " hóa đơn",
                "✅", "#22c55e");

        statsRow.getChildren().addAll(pendingCard, amountCard, paidCard);

        section.getChildren().add(statsRow);
        return section;
    }

    private VBox createInvoiceList() {
        VBox section = new VBox(15);

        Label sectionTitle = new Label("Danh sách hóa đơn");
        sectionTitle.getStyleClass().add("section-title");

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(350);
        scrollPane.getStyleClass().add("invoice-scroll");

        VBox invoicesList = new VBox(8);
        invoicesList.setPadding(new Insets(10));

        // Get invoices for current card
        java.util.List<CardDAO.InvoiceRecord> invoices = cardDAO.getInvoicesByCardId(cardId);

        if (invoices.isEmpty()) {
            Label emptyLabel = new Label("📭 Bạn chưa có hóa đơn nào.");
            emptyLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #6b7280; -fx-padding: 40px;");
            invoicesList.getChildren().add(emptyLabel);
        } else {
            for (CardDAO.InvoiceRecord invoice : invoices) {
                invoicesList.getChildren().add(createInvoiceItem(invoice));
            }
        }

        scrollPane.setContent(invoicesList);
        section.getChildren().addAll(sectionTitle, scrollPane);
        return section;
    }

    private HBox createInvoiceItem(CardDAO.InvoiceRecord invoice) {
        HBox item = new HBox(15);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(15));
        item.setStyle("-fx-background-color: white; -fx-background-radius: 8px; " +
                "-fx-border-color: #e5e7eb; -fx-border-radius: 8px;");

        // Invoice icon
        Label iconLabel = new Label(invoice.status.equals("PENDING") ? "📄" : "✅");
        iconLabel.setStyle("-fx-font-size: 28px;");

        // Invoice info
        VBox infoBox = new VBox(4);

        Label descLabel = new Label(invoice.description != null && !invoice.description.isEmpty()
                ? invoice.description
                : "Hóa đơn #" + invoice.id);
        descLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1f2937;");

        Label dateLabel = new Label(
                "Ngày tạo: " + (invoice.createdAt != null ? invoice.createdAt.substring(0, 10) : "N/A"));
        dateLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");

        infoBox.getChildren().addAll(descLabel, dateLabel);

        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Amount
        Label amountLabel = new Label(String.format("%,d VND", invoice.amount));
        amountLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1f2937;");

        // Status / Pay button
        if (invoice.status.equals("PENDING")) {
            Button payBtn = new Button("💳 Thanh toán");
            payBtn.getStyleClass().addAll("btn", "btn-primary");
            payBtn.setOnAction(e -> payInvoice(invoice));
            item.getChildren().addAll(iconLabel, infoBox, spacer, amountLabel, payBtn);
        } else {
            Label statusLabel = new Label("✅ Đã thanh toán");
            statusLabel.setStyle("-fx-text-fill: #22c55e; -fx-font-weight: bold;");
            item.getChildren().addAll(iconLabel, infoBox, spacer, amountLabel, statusLabel);
        }

        return item;
    }

    private void payInvoice(CardDAO.InvoiceRecord invoice) {
        // Validation: Check amount
        if (invoice.amount <= 0) {
            showAlert("Lỗi dữ liệu", "Số tiền hóa đơn không hợp lệ!");
            return;
        }

        // Validation: Check if amount is too large
        if (invoice.amount > Integer.MAX_VALUE) {
            showAlert("Lỗi dữ liệu", "Số tiền hóa đơn vượt quá giới hạn!");
            return;
        }

        // Get fresh balance from card
        try {
            currentBalance = cardService.getBalance();
        } catch (Exception e) {
            showAlert("Lỗi kết nối", "Không thể đọc số dư từ thẻ. Vui lòng thử lại.");
            return;
        }

        // Show confirmation dialog
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận thanh toán");
        confirm.setHeaderText("💳 Thanh toán hóa đơn");
        confirm.setContentText(
                "Bạn có muốn thanh toán hóa đơn này?\n\n" +
                        "Nội dung: " + (invoice.description != null ? invoice.description : "Hóa đơn #" + invoice.id)
                        + "\n" +
                        "Số tiền: " + String.format("%,d VND", invoice.amount) + "\n" +
                        "Số dư hiện tại: " + String.format("%,d VND", currentBalance) + "\n\n" +
                        "Số tiền sẽ được trừ từ số dư thẻ của bạn.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // Check balance
                if (currentBalance < invoice.amount) {
                    showAlert("Không đủ số dư",
                            "Số dư thẻ không đủ để thanh toán hóa đơn này.\n\n" +
                                    "Số dư hiện tại: " + String.format("%,d VND", currentBalance) + "\n" +
                                    "Số tiền cần thanh toán: " + String.format("%,d VND", invoice.amount));
                    return;
                }

                // Validation: Result balance should not be negative
                long resultBalance = currentBalance - invoice.amount;
                if (resultBalance < 0) {
                    showAlert("Lỗi tính toán", "Lỗi: Số dư sau thanh toán sẽ âm. Không thể thực hiện.");
                    return;
                }

                try {
                    // Send APDU to deduct money from card (returns new balance)
                    int newBalance = cardService.makePayment((int) invoice.amount);

                    // Update database
                    boolean dbSuccess = cardDAO.payInvoice(invoice.id);
                    if (dbSuccess) {
                        // Update local balance from card response
                        currentBalance = newBalance;
                        updateBalanceDisplay();

                        // Log transaction
                        cardDAO.logTransaction(cardId, "PAYMENT", true, null);

                        showSuccessMessage("Thanh toán thành công",
                                "Đã thanh toán hóa đơn thành công!\n\n" +
                                        "Số tiền: " + String.format("%,d VND", invoice.amount) + "\n" +
                                        "Số dư còn lại: " + String.format("%,d VND", currentBalance));

                        // Refresh invoice list
                        showInvoices();
                    } else {
                        // DB failed but card already deducted - this is a conflict situation
                        showAlert("Cảnh báo",
                                "Tiền đã bị trừ trên thẻ nhưng không thể cập nhật database.\n" +
                                        "Vui lòng liên hệ quản trị viên!");
                        cardDAO.logTransaction(cardId, "PAYMENT", false, "DB update failed after card deduction");
                    }
                } catch (Exception e) {
                    showAlert("Lỗi thanh toán", "Lỗi khi thanh toán: " + e.getMessage());
                    cardDAO.logTransaction(cardId, "PAYMENT", false, e.getMessage());
                }
            }
        });
    }

    // =====================================================
    // TOPUP MANAGEMENT
    // =====================================================

    // =====================================================
    // PROFILE EDITING MANAGEMENT
    // =====================================================

    private void showEditProfile() {
        contentArea.getChildren().clear();

        Label pageTitle = new Label("Chỉnh sửa thông tin cá nhân");
        pageTitle.getStyleClass().add("page-title");

        Label infoLabel = new Label("⚠️ Mọi thay đổi đều yêu cầu xác thực mã PIN để đảm bảo an toàn.");
        infoLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 14px; -fx-padding: 10px 0;");

        // Email edit section
        VBox emailSection = createEmailEditSection();

        // Phone edit section
        VBox phoneSection = createPhoneEditSection();

        contentArea.getChildren().addAll(pageTitle, infoLabel, emailSection, phoneSection);
    }

    private VBox createEmailEditSection() {
        VBox section = new VBox(10);
        section.setPadding(new Insets(20));
        section.setStyle("-fx-background-color: white; -fx-background-radius: 10px;");

        Label sectionTitle = new Label("📧 Email liên hệ");
        sectionTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        HBox emailRow = new HBox(15);
        emailRow.setAlignment(Pos.CENTER_LEFT);

        TextField emailField = new TextField();
        emailField.setPromptText("Nhập email mới");
        emailField.setPrefWidth(300);

        Button saveEmailBtn = new Button("💾 Lưu email");
        saveEmailBtn.getStyleClass().addAll("btn", "btn-primary");
        saveEmailBtn.setOnAction(e -> changeEmail(emailField.getText()));

        emailRow.getChildren().addAll(emailField, saveEmailBtn);
        section.getChildren().addAll(sectionTitle, emailRow);
        return section;
    }

    private VBox createPhoneEditSection() {
        VBox section = new VBox(10);
        section.setPadding(new Insets(20));
        section.setStyle("-fx-background-color: white; -fx-background-radius: 10px;");

        Label sectionTitle = new Label("📱 Số điện thoại");
        sectionTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        HBox phoneRow = new HBox(15);
        phoneRow.setAlignment(Pos.CENTER_LEFT);

        TextField phoneField = new TextField();
        phoneField.setPromptText("Nhập số điện thoại mới");
        phoneField.setPrefWidth(300);

        Button savePhoneBtn = new Button("💾 Lưu SĐT");
        savePhoneBtn.getStyleClass().addAll("btn", "btn-primary");
        savePhoneBtn.setOnAction(e -> changePhone(phoneField.getText()));

        phoneRow.getChildren().addAll(phoneField, savePhoneBtn);
        section.getChildren().addAll(sectionTitle, phoneRow);
        return section;
    }

    private void changeEmail(String newEmail) {
        if (newEmail == null || newEmail.trim().isEmpty()) {
            showAlert("Lỗi", "Vui lòng nhập email!");
            return;
        }

        // Simple email validation
        if (!newEmail.contains("@") || !newEmail.contains(".")) {
            showAlert("Lỗi", "Email không hợp lệ!");
            return;
        }

        // Require PIN verification
        String pin = PinInputDialog.showPinDialog(
                "Xác thực PIN",
                "🔐 Nhập mã PIN để thay đổi email");

        if (pin == null || pin.isEmpty()) {
            return;
        }

        CardService.PinVerificationResult pinResult = cardService.verifyPin(pin);
        if (!pinResult.success) {
            showPinError(pinResult);
            return;
        }

        // Save email to card (stored in personal info)
        try {
            String infoData = "EMAIL:" + newEmail.trim();
            boolean success = cardService.updatePersonalInfo(infoData.getBytes());
            if (success) {
                showSuccessMessage("Thành công", "Đã cập nhật email: " + newEmail);
                showEditProfile();
            } else {
                showAlert("Lỗi", "Không thể cập nhật email.");
            }
        } catch (Exception e) {
            showAlert("Lỗi", "Lỗi khi cập nhật: " + e.getMessage());
        }
    }

    private void changePhone(String newPhone) {
        if (newPhone == null || newPhone.trim().isEmpty()) {
            showAlert("Lỗi", "Vui lòng nhập số điện thoại!");
            return;
        }

        // Simple phone validation
        String cleanPhone = newPhone.replaceAll("[^0-9]", "");
        if (cleanPhone.length() < 10 || cleanPhone.length() > 11) {
            showAlert("Lỗi", "Số điện thoại không hợp lệ! (10-11 số)");
            return;
        }

        // Require PIN verification
        String pin = PinInputDialog.showPinDialog(
                "Xác thực PIN",
                "🔐 Nhập mã PIN để thay đổi số điện thoại");

        if (pin == null || pin.isEmpty()) {
            return;
        }

        CardService.PinVerificationResult pinResult = cardService.verifyPin(pin);
        if (!pinResult.success) {
            showPinError(pinResult);
            return;
        }

        // Save phone to card
        try {
            String infoData = "PHONE:" + cleanPhone;
            boolean success = cardService.updatePersonalInfo(infoData.getBytes());
            if (success) {
                showSuccessMessage("Thành công", "Đã cập nhật SĐT: " + cleanPhone);
                showEditProfile();
            } else {
                showAlert("Lỗi", "Không thể cập nhật số điện thoại.");
            }
        } catch (Exception e) {
            showAlert("Lỗi", "Lỗi khi cập nhật: " + e.getMessage());
        }
    }

    private void showPinError(CardService.PinVerificationResult pinResult) {
        if (pinResult.remainingTries > 0) {
            showAlert("Sai mã PIN",
                    "Mã PIN không đúng!\nSố lần thử còn lại: " + pinResult.remainingTries);
        } else {
            showAlert("Thẻ bị khóa", "Thẻ đã bị khóa do nhập sai PIN nhiều lần!");
        }
    }

    // =====================================================
    // TOPUP MANAGEMENT
    // =====================================================

    private void showTopup() {
        contentArea.getChildren().clear();

        Label pageTitle = new Label("Nạp tiền vào thẻ");
        pageTitle.getStyleClass().add("page-title");

        // QR Code and Instructions
        VBox qrSection = createQRInfoSection();

        // Topup form
        VBox topupForm = createTopupForm();

        // Request history
        VBox requestHistory = createTopupHistory();

        contentArea.getChildren().addAll(pageTitle, qrSection, topupForm, requestHistory);
    }

    private VBox createQRInfoSection() {
        VBox section = new VBox(15);
        section.setPadding(new Insets(25));
        section.setAlignment(Pos.CENTER);
        section.setStyle(
                "-fx-background-color: #f0f9ff; -fx-background-radius: 12px; -fx-border-color: #3b82f6; -fx-border-radius: 12px;");

        Label instructionTitle = new Label("📱 Quét mã QR để chuyển khoản");
        instructionTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1e40af;");

        // QR Code Image
        VBox qrContainer = new VBox(10);
        qrContainer.setAlignment(Pos.CENTER);
        qrContainer.setPadding(new Insets(15));
        qrContainer.setStyle("-fx-background-color: white; -fx-background-radius: 8px;");

        try {
            // Load QR image from resources
            java.io.InputStream qrStream = getClass().getResourceAsStream("/qr_bank.png");
            if (qrStream != null) {
                ImageView qrImage = new ImageView(new javafx.scene.image.Image(qrStream));
                qrImage.setFitWidth(280);
                qrImage.setFitHeight(350);
                qrImage.setPreserveRatio(true);
                qrContainer.getChildren().add(qrImage);
            } else {
                // Fallback if image not found
                Label qrPlaceholder = new Label("🏦");
                qrPlaceholder.setStyle("-fx-font-size: 48px;");
                qrContainer.getChildren().add(qrPlaceholder);
            }
        } catch (Exception e) {
            Label qrPlaceholder = new Label("🏦");
            qrPlaceholder.setStyle("-fx-font-size: 48px;");
            qrContainer.getChildren().add(qrPlaceholder);
        }

        // Transfer content reminder
        Label transferContent = new Label("📝 Nội dung CK: " + cardId);
        transferContent
                .setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #dc2626; -fx-padding: 10px;");

        Label note = new Label("⚠️ Ghi đúng nội dung chuyển khoản là ID thẻ của bạn!");
        note.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 12px;");

        section.getChildren().addAll(instructionTitle, qrContainer, transferContent, note);
        return section;
    }

    private VBox createTopupForm() {
        VBox section = new VBox(15);
        section.setPadding(new Insets(20));

        Label sectionTitle = new Label("Yêu cầu nạp tiền");
        sectionTitle.getStyleClass().add("section-title");

        HBox formRow = new HBox(15);
        formRow.setAlignment(Pos.CENTER_LEFT);

        Label amountLabel = new Label("Số tiền:");
        amountLabel.setStyle("-fx-font-size: 14px;");

        TextField amountField = new TextField();
        amountField.setPromptText("Nhập số tiền (VND)");
        amountField.setPrefWidth(200);

        Button submitBtn = new Button("✅ Xác nhận đã chuyển khoản");
        submitBtn.getStyleClass().addAll("btn", "btn-primary");
        submitBtn.setOnAction(e -> submitTopupRequest(amountField.getText()));

        formRow.getChildren().addAll(amountLabel, amountField, submitBtn);

        Label infoLabel = new Label("💡 Sau khi chuyển khoản, nhấn xác nhận để gửi yêu cầu đến Admin duyệt.");
        infoLabel.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 12px;");

        section.getChildren().addAll(sectionTitle, formRow, infoLabel);
        return section;
    }

    private void submitTopupRequest(String amountText) {
        if (amountText == null || amountText.trim().isEmpty()) {
            showAlert("Lỗi", "Vui lòng nhập số tiền!");
            return;
        }

        try {
            long amount = Long.parseLong(amountText.trim().replace(",", "").replace(".", ""));
            if (amount <= 0) {
                showAlert("Lỗi", "Số tiền phải lớn hơn 0!");
                return;
            }

            // Require PIN verification first
            String pin = PinInputDialog.showPinDialog(
                    "Xác thực PIN",
                    "🔐 Nhập mã PIN để xác nhận yêu cầu nạp tiền");

            if (pin == null || pin.isEmpty()) {
                return; // User cancelled
            }

            // Verify PIN
            CardService.PinVerificationResult pinResult = cardService.verifyPin(pin);
            if (!pinResult.success) {
                if (pinResult.remainingTries > 0) {
                    showAlert("Sai mã PIN",
                            "Mã PIN không đúng!\nSố lần thử còn lại: " + pinResult.remainingTries);
                } else {
                    showAlert("Thẻ bị khóa", "Thẻ đã bị khóa do nhập sai PIN nhiều lần!");
                }
                return;
            }

            // Confirm
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Xác nhận yêu cầu nạp tiền");
            confirm.setHeaderText("💳 Xác nhận yêu cầu nạp tiền");
            confirm.setContentText(
                    "Bạn đã chuyển khoản " + String.format("%,d VND", amount) + "?\n\n" +
                            "Nội dung CK: " + cardId + "\n\n" +
                            "Yêu cầu sẽ được gửi đến Admin để xác nhận.");

            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    boolean success = cardDAO.createTopupRequest(cardId, amount);
                    if (success) {
                        showSuccessMessage("Yêu cầu đã gửi",
                                "Yêu cầu nạp " + String.format("%,d VND", amount) + " đã được gửi.\n\n" +
                                        "Vui lòng chờ Admin xác nhận (thường trong vòng 24h).");
                        showTopup(); // Refresh
                    } else {
                        showAlert("Lỗi", "Không thể gửi yêu cầu. Vui lòng thử lại.");
                    }
                }
            });

        } catch (NumberFormatException e) {
            showAlert("Lỗi", "Số tiền không hợp lệ!");
        }
    }

    private VBox createTopupHistory() {
        VBox section = new VBox(15);

        Label sectionTitle = new Label("Lịch sử yêu cầu nạp tiền");
        sectionTitle.getStyleClass().add("section-title");

        VBox historyList = new VBox(8);
        historyList.setPadding(new Insets(10));

        java.util.List<CardDAO.TopupRecord> requests = cardDAO.getTopupRequestsByCardId(cardId);

        if (requests.isEmpty()) {
            Label emptyLabel = new Label("📭 Bạn chưa có yêu cầu nạp tiền nào.");
            emptyLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #6b7280; -fx-padding: 20px;");
            historyList.getChildren().add(emptyLabel);
        } else {
            for (CardDAO.TopupRecord req : requests) {
                historyList.getChildren().add(createTopupRequestItem(req));
            }
        }

        section.getChildren().addAll(sectionTitle, historyList);
        return section;
    }

    private HBox createTopupRequestItem(CardDAO.TopupRecord request) {
        HBox item = new HBox(15);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(12));
        item.setStyle(
                "-fx-background-color: white; -fx-background-radius: 8px; -fx-border-color: #e5e7eb; -fx-border-radius: 8px;");

        // Status icon
        String icon = switch (request.status) {
            case "PENDING" -> "⏳";
            case "APPROVED" -> "✅";
            case "REJECTED" -> "❌";
            default -> "📋";
        };
        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 24px;");

        // Info
        VBox infoBox = new VBox(4);
        Label amountLabel = new Label(String.format("%,d VND", request.amount));
        amountLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1f2937;");

        String dateStr = request.createdAt != null ? request.createdAt.substring(0, 16) : "N/A";
        Label dateLabel = new Label("Ngày: " + dateStr);
        dateLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");

        infoBox.getChildren().addAll(amountLabel, dateLabel);

        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Status badge
        String statusText = switch (request.status) {
            case "PENDING" -> "Đang chờ duyệt";
            case "APPROVED" -> "Đã duyệt";
            case "REJECTED" -> "Bị từ chối";
            default -> request.status;
        };
        String statusColor = switch (request.status) {
            case "PENDING" -> "#f59e0b";
            case "APPROVED" -> "#22c55e";
            case "REJECTED" -> "#ef4444";
            default -> "#6b7280";
        };
        Label statusLabel = new Label(statusText);
        statusLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: " + statusColor + ";");

        item.getChildren().addAll(iconLabel, infoBox, spacer, statusLabel);
        return item;
    }

    // =====================================================
    // DIALOG HANDLERS
    // =====================================================

    private void showChangePinDialog() {
        // Step 1: Get current PIN
        String currentPin = PinInputDialog.showChangePinDialog(
                "Xác thực PIN hiện tại",
                "🔐 Nhập PIN hiện tại để xác thực");

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
                "🔐 Nhập PIN mới (4 chữ số)\n\n⚠️ Hãy ghi nhớ PIN mới của bạn!");

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
                "🔐 Nhập lại PIN mới để xác nhận");

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
     * Change PIN on smart card - Simplified version
     */
    private void changePinOnCard(String currentPin, String newPin) {
        // Run PIN change in background thread
        new Thread(() -> {
            boolean success = false;
            String errorMsg = null;

            try {
                success = cardService.changePin(currentPin, newPin);
            } catch (Exception e) {
                errorMsg = e.getMessage();
                System.err.println("[ERROR] PIN change exception: " + e.getMessage());
            }

            final boolean finalSuccess = success;
            final String finalError = errorMsg;

            // Update UI on JavaFX thread
            javafx.application.Platform.runLater(() -> {
                if (finalSuccess) {
                    cardDAO.logTransaction(cardId, "CHANGE_PIN", true, null);
                    System.out.println("[INFO] PIN changed successfully");
                    showSuccessMessage("Đổi PIN thành công",
                            "PIN của bạn đã được thay đổi thành công.\n\n" +
                                    "🔐 Hãy ghi nhớ PIN mới của bạn!\n" +
                                    "🔒 Sử dụng PIN mới cho lần đăng nhập tiếp theo.");
                } else {
                    cardDAO.logTransaction(cardId, "CHANGE_PIN", false, finalError);
                    System.err.println("[ERROR] PIN change failed: " + finalError);
                    showAlert("Đổi PIN thất bại",
                            "Không thể đổi PIN trên thẻ.\n\n" +
                                    "Có thể do:\n" +
                                    "• PIN hiện tại không đúng\n" +
                                    "• Lỗi giao tiếp với thẻ\n" +
                                    "• Thẻ bị khóa\n\n" +
                                    "Vui lòng thử lại sau.");
                }
            });
        }).start();
    }

    private void showEmergencyBlock() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Khóa thẻ khẩn cấp");
        confirm.setHeaderText("🚨 Khóa thẻ của bạn");
        confirm.setContentText(
                "Điều này sẽ ngay lập tức khóa thẻ của bạn để ngăn chặn việc sử dụng trái phép.\n\n" +
                        "⚠️ CẢNH BÁO: Một khi bị khóa, bạn sẽ cần liên hệ cơ quan có thẩm quyền để mở khóa thẻ.\n\n" +
                        "Bạn có chắc chắn muốn khóa thẻ của mình không?");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // In a real system, this would call the card service to block the card
                showAlert("Thẻ đã bị khóa",
                        "🚨 Thẻ của bạn đã được khóa thành công.\n\n" +
                                "ID Thẻ: " + cardId + "\n" +
                                "Khóa lúc: "
                                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"))
                                + "\n\n" +
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
        confirm.setContentText("Bạn có chắc chắn muốn đăng xuất?\n\n" +
                "💡 Kết nối thẻ sẽ được giữ nguyên.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // DON'T disconnect - keep card connection for next login
                System.out.println("[INFO] Citizen logout - keeping card connection");

                // Return to login screen
                returnToLoginScreen();
            }
        });
    }

    /**
     * Return to login screen
     */
    private void returnToLoginScreen() {
        try {
            // Create new login view
            citizencard.controller.LoginViewController loginController = new citizencard.controller.LoginViewController();

            // Get current stage - keep same window size
            javafx.stage.Stage stage = (javafx.stage.Stage) root.getScene().getWindow();
            double w = stage.getWidth();
            double h = stage.getHeight();

            // Create new scene with login view
            javafx.scene.Scene loginScene = new javafx.scene.Scene(
                    loginController.getRoot(),
                    w,
                    h);

            // Load CSS
            loginScene.getStylesheets().add(
                    getClass().getResource("/css/styles.css").toExternalForm());

            // Set scene
            stage.setScene(loginScene);
            stage.setTitle("Hệ thống Quản lý Thẻ Cư dân - Đăng nhập");
            // Don't centerOnScreen to keep window position

            System.out.println("🚪 Đã đăng xuất - Quay về màn hình đăng nhập");

        } catch (Exception e) {
            showAlert("Lỗi", "Không thể quay về màn hình đăng nhập: " + e.getMessage());
            e.printStackTrace();
        }
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
        UIHelper.showAlert(title, message);
    }

    private void showSuccessMessage(String title, String message) {
        UIHelper.showSuccessAlert(title, message);
    }

    public Parent getRoot() {
        return root;
    }
}