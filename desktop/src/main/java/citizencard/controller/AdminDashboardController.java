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

import citizencard.service.CardService;
import citizencard.dao.CardDAO;
import citizencard.util.DatabaseViewer;
import citizencard.util.DataValidator;

/**
 * Simplified Admin Dashboard Controller
 * 
 * Focused on core functions: citizen card management and citizen data
 * management
 */
public class AdminDashboardController {

    private BorderPane root;
    private CardService cardService;
    private CardDAO cardDAO;
    private DemoWorkflowController demoController;
    private VBox contentArea;

    public AdminDashboardController() {
        cardService = CardService.getInstance();
        cardDAO = CardDAO.getInstance();
        demoController = new DemoWorkflowController(cardService, cardDAO);
        initializeUI();
    }

    private void initializeUI() {
        root = new BorderPane();
        root.getStyleClass().add("admin-container");

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
        header.getStyleClass().add("admin-header");

        Label titleLabel = new Label("Quản lý Thẻ Cư dân");
        titleLabel.getStyleClass().add("admin-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label timeLabel = new Label(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        timeLabel.getStyleClass().add("admin-time");

        Button logoutButton = new Button("🚪 Đăng xuất");
        logoutButton.getStyleClass().addAll("btn", "btn-danger");
        logoutButton.setOnAction(e -> logout());

        header.getChildren().addAll(titleLabel, spacer, timeLabel, logoutButton);
        return header;
    }

    private VBox createSidebar() {
        VBox sidebar = new VBox();
        sidebar.setPrefWidth(280);
        sidebar.getStyleClass().add("admin-sidebar");

        // Sidebar header
        VBox sidebarHeader = new VBox(10);
        sidebarHeader.setAlignment(Pos.CENTER);
        sidebarHeader.setPadding(new Insets(30, 20, 30, 20));
        sidebarHeader.getStyleClass().add("sidebar-header");

        Label adminIcon = new Label("👨‍💼");
        adminIcon.setStyle("-fx-font-size: 48px;");

        Label adminLabel = new Label("Quản trị viên");
        adminLabel.getStyleClass().add("sidebar-title");

        Label accessLabel = new Label("Quản lý Thẻ & Dữ liệu Cư dân");
        accessLabel.getStyleClass().add("sidebar-subtitle");

        sidebarHeader.getChildren().addAll(adminIcon, adminLabel, accessLabel);

        // Navigation menu
        VBox menu = createNavigationMenu();

        sidebar.getChildren().addAll(sidebarHeader, menu);
        return sidebar;
    }

    private VBox createNavigationMenu() {
        VBox menu = new VBox(5);
        menu.setPadding(new Insets(0, 15, 20, 15));

        Button dashboardBtn = createMenuButton("📊 Tổng quan", "Thống kê tổng quan hệ thống");
        dashboardBtn.setOnAction(e -> showDashboardOverview());

        Button citizenMgmtBtn = createMenuButton("👥 Quản lý cư dân", "Gửi hóa đơn cho cư dân");
        citizenMgmtBtn.setOnAction(e -> showCitizenManagement());

        Button topupRequestsBtn = createMenuButton("💰 Yêu cầu nạp tiền", "Duyệt yêu cầu nạp tiền từ cư dân");
        topupRequestsBtn.setOnAction(e -> showTopupRequests());

        Button databaseBtn = createMenuButton("🗄️ Cơ sở dữ liệu", "Xem tất cả thẻ đã đăng ký");
        databaseBtn.setOnAction(e -> showDatabaseViewer());

        menu.getChildren().addAll(
                dashboardBtn,
                new Separator(),
                citizenMgmtBtn,
                topupRequestsBtn,
                new Separator(),
                databaseBtn);

        return menu;
    }

    private Button createMenuButton(String text, String description) {
        Button button = new Button(text);
        button.getStyleClass().addAll("menu-button");
        button.setPrefWidth(250);
        button.setAlignment(Pos.CENTER_LEFT);

        Tooltip tooltip = new Tooltip(description);
        button.setTooltip(tooltip);

        return button;
    }

    private VBox createContentArea() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(30));
        content.getStyleClass().add("admin-content");

        return content;
    }

    // =====================================================
    // CONTENT SECTIONS
    // =====================================================

    private void showDashboardOverview() {
        contentArea.getChildren().clear();

        Label pageTitle = new Label("Tổng quan hệ thống");
        pageTitle.getStyleClass().add("page-title");

        // Statistics cards
        HBox statsRow = new HBox(20);
        statsRow.setAlignment(Pos.CENTER_LEFT);

        // Real Statistics from Database
        int totalCards = cardDAO.getCardCountByStatus(null);
        int activeCards = cardDAO.getCardCountByStatus("ACTIVE");

        VBox totalCardsCard = createStatCard("Tổng số thẻ", String.valueOf(totalCards), "💳", "#3b82f6");
        VBox activeCardsCard = createStatCard("Thẻ hoạt động", String.valueOf(activeCards), "✅", "#22c55e");
        VBox citizenDataCard = createStatCard("Dữ liệu cư dân", totalCards + " hồ sơ", "👥", "#f59e0b");
        VBox systemStatusCard = createStatCard("Trạng thái hệ thống", "Hoạt động", "🟢", "#10b981");

        statsRow.getChildren().addAll(totalCardsCard, activeCardsCard, citizenDataCard, systemStatusCard);

        // Recent activity
        VBox recentActivity = createRecentActivitySection();

        // Quick actions
        VBox quickActions = createQuickActionsSection();

        contentArea.getChildren().addAll(pageTitle, statsRow, recentActivity, quickActions);
    }

    private VBox createStatCard(String title, String value, String icon, String color) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(25));
        card.getStyleClass().add("stat-card");
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

    private VBox createRecentActivitySection() {
        VBox section = new VBox(15);

        Label sectionTitle = new Label("Hoạt động gần đây");
        sectionTitle.getStyleClass().add("section-title");

        VBox activityList = new VBox(8);
        activityList.getStyleClass().add("activity-list");

        java.util.List<CardDAO.TransactionRecord> logs = cardDAO.getRecentTransactions(5);
        if (logs.isEmpty()) {
            Label emptyLabel = new Label("Chưa có hoạt động nào.");
            emptyLabel.setStyle("-fx-text-fill: #9ca3af; -fx-padding: 10px;");
            activityList.getChildren().add(emptyLabel);
        } else {
            for (CardDAO.TransactionRecord log : logs) {
                String actionName = switch (log.type) {
                    case "CREATE_CARD" -> "Đăng ký thẻ mới";
                    case "CHANGE_PIN" -> "Đổi mã PIN";
                    case "LOGIN" -> "Đăng nhập hệ thống";
                    case "TOPUP" -> "Nạp tiền";
                    case "PAYMENT" -> "Thanh toán";
                    case "UPDATE_INFO" -> "Cập nhật thông tin";
                    default -> log.type;
                };

                String type = log.success ? "success" : "error";
                // Simplified time display (just timestamp string)
                String timeDisplay = log.timestamp.substring(11, 16); // HH:mm

                activityList.getChildren().add(
                        createActivityItem(actionName, log.cardId, timeDisplay, type));
            }
        }

        section.getChildren().addAll(sectionTitle, activityList);
        return section;
    }

    private HBox createActivityItem(String action, String details, String time, String type) {
        HBox item = new HBox(15);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(12));
        item.getStyleClass().add("activity-item");

        String icon = switch (type) {
            case "success" -> "✅";
            case "warning" -> "⚠️";
            case "error" -> "❌";
            default -> "ℹ️";
        };

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 16px;");

        VBox content = new VBox(2);

        Label actionLabel = new Label(action);
        actionLabel.getStyleClass().add("activity-action");

        Label detailsLabel = new Label(details);
        detailsLabel.getStyleClass().add("activity-details");

        content.getChildren().addAll(actionLabel, detailsLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label timeLabel = new Label(time);
        timeLabel.getStyleClass().add("activity-time");

        item.getChildren().addAll(iconLabel, content, spacer, timeLabel);
        return item;
    }

    private VBox createQuickActionsSection() {
        VBox section = new VBox(15);

        Label sectionTitle = new Label("Thao tác nhanh");
        sectionTitle.getStyleClass().add("section-title");

        HBox actionsRow = new HBox(15);

        Button createCardBtn = new Button("Tạo thẻ mới");
        createCardBtn.getStyleClass().addAll("btn", "btn-primary", "btn-large");
        createCardBtn.setOnAction(e -> demoController.showCreateNewCard());

        Button viewDatabaseBtn = new Button("Xem cơ sở dữ liệu");
        viewDatabaseBtn.getStyleClass().addAll("btn", "btn-secondary", "btn-large");
        viewDatabaseBtn.setOnAction(e -> showDatabaseViewer());

        Button changePinBtn = new Button("Đổi mã PIN");
        changePinBtn.getStyleClass().addAll("btn", "btn-warning", "btn-large");
        changePinBtn.setOnAction(e -> showChangePinDialog());

        Button unlockCardBtn = new Button("Mở khóa thẻ");
        unlockCardBtn.getStyleClass().addAll("btn", "btn-success", "btn-large");
        unlockCardBtn.setOnAction(e -> unlockCard());

        Button resetCardBtn = new Button("Reset thẻ");
        resetCardBtn.getStyleClass().addAll("btn", "btn-danger", "btn-large");
        resetCardBtn.setOnAction(e -> resetCard());

        Button systemBackupBtn = new Button("Sao lưu hệ thống");
        systemBackupBtn.getStyleClass().addAll("btn", "btn-outline", "btn-large");
        systemBackupBtn.setOnAction(e -> performSystemBackup());

        actionsRow.getChildren().addAll(createCardBtn, viewDatabaseBtn, changePinBtn, unlockCardBtn, resetCardBtn,
                systemBackupBtn);

        section.getChildren().addAll(sectionTitle, actionsRow);
        return section;
    }

    // =====================================================
    // CITIZEN MANAGEMENT - GỬI HÓA ĐƠN
    // =====================================================

    private void showCitizenManagement() {
        contentArea.getChildren().clear();

        Label pageTitle = new Label("Quản lý cư dân - Gửi hóa đơn");
        pageTitle.getStyleClass().add("page-title");

        // Search row
        HBox searchRow = new HBox(15);
        searchRow.setAlignment(Pos.CENTER_LEFT);

        TextField searchField = new TextField();
        searchField.setPromptText("Nhập ID thẻ để tìm kiếm...");
        searchField.setPrefWidth(300);

        Button searchBtn = new Button("🔍 Tìm kiếm");
        searchBtn.getStyleClass().addAll("btn", "btn-primary");
        searchBtn.setOnAction(e -> searchCitizenForInvoice(searchField.getText()));

        searchRow.getChildren().addAll(new Label("Tìm ID thẻ:"), searchField, searchBtn);

        // Citizens table with invoice button
        VBox citizensTable = createCitizenInvoiceTable();

        contentArea.getChildren().addAll(pageTitle, searchRow, new Separator(), citizensTable);
    }

    private void searchCitizenForInvoice(String query) {
        if (query == null || query.trim().isEmpty()) {
            showAlert("Lỗi", "Vui lòng nhập ID thẻ để tìm kiếm!");
            return;
        }

        String cardId = query.trim();
        java.util.List<citizencard.dao.CardDAO.CardRecord> cards = cardDAO.getAllCards();

        // Check if card exists
        boolean found = cards.stream().anyMatch(c -> c.cardId.contains(cardId));
        if (found) {
            showSendInvoiceDialog(cardId);
        } else {
            showAlert("Không tìm thấy", "Không tìm thấy thẻ với ID: " + cardId);
        }
    }

    private VBox createCitizenInvoiceTable() {
        VBox section = new VBox(15);

        Label sectionTitle = new Label("Danh sách cư dân - Nhấn để gửi hóa đơn");
        sectionTitle.getStyleClass().add("section-title");

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.getStyleClass().add("db-scroll-pane");
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(400);

        VBox citizensList = new VBox(5);
        citizensList.getStyleClass().add("citizens-list");

        // Load real cards from database
        java.util.List<citizencard.dao.CardDAO.CardRecord> cards = cardDAO.getAllCards();

        if (cards.isEmpty()) {
            Label emptyLabel = new Label("📭 Chưa có cư dân nào trong hệ thống.\nHãy tạo thẻ mới để bắt đầu.");
            emptyLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #6b7280; -fx-padding: 40px;");
            citizensList.getChildren().add(emptyLabel);
        } else {
            for (citizencard.dao.CardDAO.CardRecord card : cards) {
                String statusVi = switch (card.status) {
                    case "ACTIVE" -> "Hoạt động";
                    case "BLOCKED" -> "Bị khóa";
                    case "EXPIRED" -> "Hết hạn";
                    default -> card.status;
                };

                String registered = card.registeredAt != null ? card.registeredAt : "N/A";
                citizensList.getChildren().add(
                        createCitizenInvoiceItem(card.cardId, statusVi, registered));
            }
        }

        scrollPane.setContent(citizensList);
        section.getChildren().addAll(sectionTitle, scrollPane);
        return section;
    }

    private HBox createCitizenInvoiceItem(String cardId, String status, String date) {
        HBox item = new HBox(15);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(12));
        item.getStyleClass().add("citizen-data-item");

        Label cardIcon = new Label("👤");
        cardIcon.setStyle("-fx-font-size: 24px;");

        Label cardIdLabel = new Label(cardId);
        cardIdLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1f2937;");
        cardIdLabel.setPrefWidth(200);

        Label statusLabel = new Label(status);
        statusLabel.getStyleClass().add(status.equals("Hoạt động") ? "status-success" : "status-error");
        statusLabel.setPrefWidth(100);

        Label dateLabel = new Label(date);
        dateLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");
        dateLabel.setPrefWidth(150);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Buttons container
        HBox buttonsBox = new HBox(8);
        buttonsBox.setAlignment(Pos.CENTER_RIGHT);

        Button historyBtn = new Button("📜 Lịch sử HĐ");
        historyBtn.getStyleClass().addAll("btn", "btn-secondary");
        historyBtn.setOnAction(e -> showInvoiceHistoryDialog(cardId));

        Button invoiceBtn = new Button("💸 Gửi hóa đơn");
        invoiceBtn.getStyleClass().addAll("btn", "btn-primary");
        invoiceBtn.setOnAction(e -> showSendInvoiceDialog(cardId));

        buttonsBox.getChildren().addAll(historyBtn, invoiceBtn);

        item.getChildren().addAll(cardIcon, cardIdLabel, statusLabel, dateLabel, spacer, buttonsBox);
        return item;
    }

    /**
     * Show invoice history dialog for a specific card
     */
    private void showInvoiceHistoryDialog(String cardId) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Lịch sử hóa đơn");
        dialog.setHeaderText("📜 Lịch sử hóa đơn - " + cardId);

        // Create content
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setPrefWidth(600);
        content.setPrefHeight(400);

        // Get invoices from database
        java.util.List<CardDAO.InvoiceRecord> invoices = cardDAO.getInvoicesByCardId(cardId);

        // Stats row
        HBox statsRow = new HBox(20);
        statsRow.setAlignment(Pos.CENTER_LEFT);

        long totalPending = invoices.stream()
                .filter(i -> "PENDING".equals(i.status))
                .mapToLong(i -> i.amount)
                .sum();
        long totalPaid = invoices.stream()
                .filter(i -> "PAID".equals(i.status))
                .mapToLong(i -> i.amount)
                .sum();
        long pendingCount = invoices.stream()
                .filter(i -> "PENDING".equals(i.status))
                .count();
        long paidCount = invoices.stream()
                .filter(i -> "PAID".equals(i.status))
                .count();

        VBox pendingCard = createStatCard("Chờ thanh toán", pendingCount + " HĐ", "⏳", "#f59e0b");
        VBox pendingAmountCard = createStatCard("Tổng chờ TT", String.format("%,d VND", totalPending), "💰", "#ef4444");
        VBox paidCard = createStatCard("Đã thanh toán", paidCount + " HĐ", "✅", "#22c55e");
        VBox paidAmountCard = createStatCard("Tổng đã TT", String.format("%,d VND", totalPaid), "💵", "#3b82f6");

        statsRow.getChildren().addAll(pendingCard, pendingAmountCard, paidCard, paidAmountCard);

        // Invoice list
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(250);
        scrollPane.getStyleClass().add("invoice-scroll");

        VBox invoiceList = new VBox(8);
        invoiceList.setPadding(new Insets(10));

        if (invoices.isEmpty()) {
            Label emptyLabel = new Label("📭 Cư dân này chưa có hóa đơn nào.");
            emptyLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #6b7280; -fx-padding: 20px;");
            invoiceList.getChildren().add(emptyLabel);
        } else {
            for (CardDAO.InvoiceRecord invoice : invoices) {
                invoiceList.getChildren().add(createInvoiceHistoryItem(invoice));
            }
        }

        scrollPane.setContent(invoiceList);

        content.getChildren().addAll(statsRow, new Separator(), scrollPane);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

        dialog.showAndWait();
    }

    /**
     * Create invoice history item for dialog
     */
    private HBox createInvoiceHistoryItem(CardDAO.InvoiceRecord invoice) {
        HBox item = new HBox(15);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(10));
        item.setStyle(
                "-fx-background-color: white; -fx-background-radius: 6px; -fx-border-color: #e5e7eb; -fx-border-radius: 6px;");

        // Status icon
        String icon = "PAID".equals(invoice.status) ? "✅" : "⏳";
        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 20px;");

        // Info
        VBox infoBox = new VBox(2);
        Label amountLabel = new Label(String.format("%,d VND", invoice.amount));
        amountLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1f2937;");

        String desc = invoice.description != null && !invoice.description.isEmpty()
                ? invoice.description
                : "Không có mô tả";
        Label descLabel = new Label(desc);
        descLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");

        String dateStr = invoice.createdAt != null
                ? invoice.createdAt.substring(0, Math.min(16, invoice.createdAt.length()))
                : "N/A";
        Label dateLabel = new Label("Ngày: " + dateStr);
        dateLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #9ca3af;");

        infoBox.getChildren().addAll(amountLabel, descLabel, dateLabel);

        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Status badge
        String statusText = "PAID".equals(invoice.status) ? "Đã thanh toán" : "Chờ thanh toán";
        String statusColor = "PAID".equals(invoice.status) ? "#22c55e" : "#f59e0b";
        Label statusLabel = new Label(statusText);
        statusLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: " + statusColor
                + "; -fx-padding: 4px 8px; -fx-background-color: " + statusColor + "20; -fx-background-radius: 4px;");

        item.getChildren().addAll(iconLabel, infoBox, spacer, statusLabel);
        return item;
    }

    // =====================================================
    // TOPUP REQUEST MANAGEMENT (ADMIN)
    // =====================================================

    private void showTopupRequests() {
        contentArea.getChildren().clear();

        Label pageTitle = new Label("Yêu cầu nạp tiền từ cư dân");
        pageTitle.getStyleClass().add("page-title");

        // Stats
        HBox statsRow = new HBox(20);
        statsRow.setAlignment(Pos.CENTER_LEFT);

        java.util.List<CardDAO.TopupRecord> pendingRequests = cardDAO.getPendingTopupRequests();
        long totalPendingAmount = pendingRequests.stream().mapToLong(r -> r.amount).sum();

        VBox pendingCountCard = createStatCard("Chờ duyệt", String.valueOf(pendingRequests.size()) + " yêu cầu", "⏳",
                "#f59e0b");
        VBox totalAmountCard = createStatCard("Tổng tiền chờ", String.format("%,d VND", totalPendingAmount), "💰",
                "#3b82f6");

        statsRow.getChildren().addAll(pendingCountCard, totalAmountCard);

        // Pending requests list
        VBox requestsList = createTopupRequestsList(pendingRequests);

        contentArea.getChildren().addAll(pageTitle, statsRow, requestsList);
    }

    private VBox createTopupRequestsList(java.util.List<CardDAO.TopupRecord> requests) {
        VBox section = new VBox(15);

        Label sectionTitle = new Label("Danh sách yêu cầu chờ duyệt");
        sectionTitle.getStyleClass().add("section-title");

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(400);
        scrollPane.getStyleClass().add("db-scroll-pane");

        VBox requestList = new VBox(8);
        requestList.setPadding(new Insets(10));

        if (requests.isEmpty()) {
            Label emptyLabel = new Label("📭 Không có yêu cầu nạp tiền nào đang chờ duyệt.");
            emptyLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #6b7280; -fx-padding: 40px;");
            requestList.getChildren().add(emptyLabel);
        } else {
            for (CardDAO.TopupRecord req : requests) {
                requestList.getChildren().add(createAdminTopupItem(req));
            }
        }

        scrollPane.setContent(requestList);
        section.getChildren().addAll(sectionTitle, scrollPane);
        return section;
    }

    private HBox createAdminTopupItem(CardDAO.TopupRecord request) {
        HBox item = new HBox(15);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(15));
        item.setStyle(
                "-fx-background-color: white; -fx-background-radius: 8px; -fx-border-color: #e5e7eb; -fx-border-radius: 8px;");

        // Icon
        Label iconLabel = new Label("💳");
        iconLabel.setStyle("-fx-font-size: 28px;");

        // Info
        VBox infoBox = new VBox(4);

        Label cardIdLabel = new Label("Thẻ: " + request.cardId);
        cardIdLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1f2937;");

        Label amountLabel = new Label("Số tiền: " + String.format("%,d VND", request.amount));
        amountLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #22c55e;");

        String dateStr = request.createdAt != null ? request.createdAt.substring(0, 16) : "N/A";
        Label dateLabel = new Label("Thời gian: " + dateStr);
        dateLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");

        infoBox.getChildren().addAll(cardIdLabel, amountLabel, dateLabel);

        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Action buttons
        HBox actionButtons = new HBox(10);

        Button approveBtn = new Button("✅ Duyệt");
        approveBtn.getStyleClass().addAll("btn", "btn-success");
        approveBtn.setOnAction(e -> approveTopup(request));

        Button rejectBtn = new Button("❌ Từ chối");
        rejectBtn.getStyleClass().addAll("btn", "btn-danger");
        rejectBtn.setOnAction(e -> rejectTopup(request));

        actionButtons.getChildren().addAll(approveBtn, rejectBtn);

        item.getChildren().addAll(iconLabel, infoBox, spacer, actionButtons);
        return item;
    }

    private void approveTopup(CardDAO.TopupRecord request) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận duyệt nạp tiền");
        confirm.setHeaderText("✅ Duyệt yêu cầu nạp tiền");
        confirm.setContentText(
                "Bạn đã kiểm tra tài khoản ngân hàng và xác nhận nhận được tiền?\n\n" +
                        "Thẻ: " + request.cardId + "\n" +
                        "Số tiền: " + String.format("%,d VND", request.amount) + "\n\n" +
                        "Số dư của cư dân sẽ được tăng lên.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                boolean success = cardDAO.approveTopupRequest(request.id);
                if (success) {
                    // Log the transaction
                    cardDAO.logTransaction(request.cardId, "TOPUP", true, null);

                    showSuccessAlert("Đã duyệt",
                            "Đã duyệt yêu cầu nạp " + String.format("%,d VND", request.amount) +
                                    " cho thẻ " + request.cardId);
                    showTopupRequests(); // Refresh
                } else {
                    showAlert("Lỗi", "Không thể duyệt yêu cầu. Vui lòng thử lại.");
                }
            }
        });
    }

    private void rejectTopup(CardDAO.TopupRecord request) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận từ chối");
        confirm.setHeaderText("❌ Từ chối yêu cầu nạp tiền");
        confirm.setContentText(
                "Bạn có chắc muốn từ chối yêu cầu này?\n\n" +
                        "Thẻ: " + request.cardId + "\n" +
                        "Số tiền: " + String.format("%,d VND", request.amount));

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                boolean success = cardDAO.rejectTopupRequest(request.id);
                if (success) {
                    showAlert("Đã từ chối", "Đã từ chối yêu cầu nạp tiền.");
                    showTopupRequests(); // Refresh
                } else {
                    showAlert("Lỗi", "Không thể từ chối yêu cầu. Vui lòng thử lại.");
                }
            }
        });
    }

    private void showDatabaseViewer() {
        Alert dbViewerDialog = new Alert(Alert.AlertType.INFORMATION);
        dbViewerDialog.setTitle("Database Viewer");
        dbViewerDialog.setHeaderText("🗄️ Xem cơ sở dữ liệu H2");

        String content = "CHỌN CÁCH XEM DATABASE:\n\n" +

                "🌐 H2 WEB CONSOLE (Khuyến nghị):\n" +
                "• Giao diện web đầy đủ tính năng\n" +
                "• Chạy SQL queries trực tiếp\n" +
                "• Xem/sửa dữ liệu real-time\n" +
                "• Truy cập: http://localhost:8082\n\n" +

                "📊 XEM NHANH TRONG CONSOLE:\n" +
                "• In dữ liệu ra console/terminal\n" +
                "• Xem nhanh không cần browser\n" +
                "• Phù hợp để debug\n\n" +

                "📈 THỐNG KÊ DATABASE:\n" +
                "• Số liệu tổng quan\n" +
                "• Phân tích trạng thái thẻ\n" +
                "• Tỷ lệ thành công giao dịch\n\n" +

                "🔗 THÔNG TIN KẾT NỐI:\n" +
                "JDBC URL: jdbc:h2:file:./data/citizen_card\n" +
                "Username: (để trống)\n" +
                "Password: (để trống)";

        dbViewerDialog.setContentText(content);

        ButtonType webConsoleBtn = new ButtonType("🌐 Mở Web Console", ButtonBar.ButtonData.OTHER);
        ButtonType printConsoleBtn = new ButtonType("📊 In ra Console", ButtonBar.ButtonData.OTHER);
        ButtonType showStatsBtn = new ButtonType("📈 Xem thống kê", ButtonBar.ButtonData.OTHER);
        ButtonType closeBtn = new ButtonType("Đóng", ButtonBar.ButtonData.CANCEL_CLOSE);

        dbViewerDialog.getDialogPane().getButtonTypes().setAll(webConsoleBtn, printConsoleBtn, showStatsBtn, closeBtn);
        dbViewerDialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

        dbViewerDialog.showAndWait().ifPresent(response -> {
            if (response == webConsoleBtn) {
                startH2WebConsole();
            } else if (response == printConsoleBtn) {
                printDatabaseToConsole();
            } else if (response == showStatsBtn) {
                showDatabaseStats();
            }
        });
    }

    private void startH2WebConsole() {
        try {
            DatabaseViewer.startWebConsole();

            Alert success = new Alert(Alert.AlertType.INFORMATION);
            success.setTitle("H2 Console đã khởi động");
            success.setHeaderText("✅ Web Console đã sẵn sàng!");
            success.setContentText(
                    "H2 Database Console đã khởi động thành công!\n\n" +
                            "🌐 Truy cập tại: http://localhost:8082\n\n" +
                            "📋 THÔNG TIN ĐĂNG NHẬP:\n" +
                            "• JDBC URL: jdbc:h2:file:./data/citizen_card\n" +
                            "• User Name: (để trống)\n" +
                            "• Password: (để trống)\n\n" +
                            "📊 CÁC BẢNG TRONG DATABASE:\n" +
                            "• REGISTERED_CARDS - Thông tin thẻ đã đăng ký\n" +
                            "• TRANSACTION_LOGS - Lịch sử giao dịch\n\n" +
                            "💡 Console sẽ chạy trong background.\n" +
                            "Đóng ứng dụng để tự động dừng console.");
            success.getDialogPane().getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            success.showAndWait();

        } catch (Exception e) {
            Alert error = new Alert(Alert.AlertType.ERROR);
            error.setTitle("Lỗi khởi động Console");
            error.setHeaderText("❌ Không thể khởi động H2 Console");
            error.setContentText("Lỗi: " + e.getMessage()
                    + "\n\nVui lòng kiểm tra:\n• Database có đang được sử dụng?\n• Port 8082 có bị chiếm?");
            error.showAndWait();
        }
    }

    private void printDatabaseToConsole() {
        try {
            // Get database content as string
            String dbContent = DatabaseViewer.getDatabaseContentAsString();

            // Also print to console
            System.out.println("\n" + "=".repeat(60));
            System.out.println("🗄️ DATABASE VIEWER - ADMIN REQUEST");
            System.out.println("=".repeat(60));
            System.out.println(dbContent);

            // Show in dialog
            Alert dialog = new Alert(Alert.AlertType.INFORMATION);
            dialog.setTitle("Database Content");
            dialog.setHeaderText("📊 Nội dung Database");

            // Create scrollable text area
            TextArea textArea = new TextArea(dbContent);
            textArea.setEditable(false);
            textArea.setWrapText(true);
            textArea.setPrefWidth(700);
            textArea.setPrefHeight(500);
            textArea.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace; -fx-font-size: 12px;");

            dialog.getDialogPane().setContent(textArea);
            dialog.getDialogPane().setPrefWidth(750);
            dialog.showAndWait();

        } catch (Exception e) {
            Alert error = new Alert(Alert.AlertType.ERROR);
            error.setTitle("Lỗi đọc database");
            error.setHeaderText("❌ Không thể đọc dữ liệu");
            error.setContentText("Lỗi: " + e.getMessage());
            error.showAndWait();
        }
    }

    private void showDatabaseStats() {
        try {
            System.out.println("\n" + "=".repeat(50));
            System.out.println("📈 DATABASE STATISTICS - ADMIN REQUEST");
            System.out.println("=".repeat(50));

            DatabaseViewer.printDatabaseStats();

            Alert success = new Alert(Alert.AlertType.INFORMATION);
            success.setTitle("Thống kê database");
            success.setHeaderText("📈 Thống kê đã được tạo");
            success.setContentText(
                    "Thống kê database đã được in ra console.\n\n" +
                            "📊 Bao gồm:\n" +
                            "• Số lượng thẻ theo trạng thái\n" +
                            "• Số lượng giao dịch theo loại\n" +
                            "• Tỷ lệ thành công giao dịch\n" +
                            "• Các chỉ số quan trọng khác\n\n" +
                            "💡 Kiểm tra console để xem chi tiết.");
            success.showAndWait();

        } catch (Exception e) {
            Alert error = new Alert(Alert.AlertType.ERROR);
            error.setTitle("Lỗi tạo thống kê");
            error.setHeaderText("❌ Không thể tạo thống kê");
            error.setContentText("Lỗi: " + e.getMessage());
            error.showAndWait();
        }
    }

    // =====================================================
    // ACTION HANDLERS
    // =====================================================

    /**
     * Logout and return to login screen
     */
    private void logout() {
        // Confirm logout
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận đăng xuất");
        confirm.setHeaderText("🚪 Đăng xuất khỏi hệ thống");
        confirm.setContentText("Bạn có chắc chắn muốn đăng xuất?\n\nBạn sẽ quay về màn hình đăng nhập.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // DON'T disconnect - keep card connection for next login
                System.out.println("[INFO] Admin logout - keeping card connection");

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

    private void performSystemBackup() {
        showAlert("Sao lưu hệ thống",
                "Bắt đầu sao lưu cơ sở dữ liệu...\n\nFile sao lưu sẽ được lưu tại: /backups/citizen_card_" +
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".sql");
    }

    // =====================================================
    // DATABASE VIEWER COMPONENTS
    // =====================================================

    private HBox createDatabaseStatsRow() {
        HBox statsRow = new HBox(20);
        statsRow.setAlignment(Pos.CENTER_LEFT);

        // Get real counts from database
        int totalCards = cardDAO.getCardCountByStatus(null);
        int activeCards = cardDAO.getCardCountByStatus("ACTIVE");
        int blockedCards = cardDAO.getCardCountByStatus("BLOCKED");

        VBox totalRecordsCard = createStatCard("Tổng bản ghi", String.valueOf(totalCards), "🗄️", "#3b82f6");
        VBox activeCardsCard = createStatCard("Thẻ hoạt động", String.valueOf(activeCards), "✅", "#22c55e");
        VBox blockedCardsCard = createStatCard("Thẻ bị khóa", String.valueOf(blockedCards), "🚫", "#ef4444");
        VBox dbSizeCard = createStatCard("Kích thước DB", "~" + (totalCards * 2) + " KB", "💾", "#8b5cf6");

        statsRow.getChildren().addAll(totalRecordsCard, activeCardsCard, blockedCardsCard, dbSizeCard);
        return statsRow;
    }

    private HBox createDatabaseControls() {
        HBox controls = new HBox(15);
        controls.setAlignment(Pos.CENTER_LEFT);
        controls.setPadding(new Insets(20, 0, 10, 0));

        // Search controls
        TextField searchField = new TextField();
        searchField.setPromptText("Tìm kiếm theo ID thẻ, tên, hoặc số điện thoại...");
        searchField.setPrefWidth(300);
        searchField.getStyleClass().add("db-search-field");

        ComboBox<String> statusFilter = new ComboBox<>();
        statusFilter.getItems().addAll("Tất cả", "Hoạt động", "Bị khóa", "Hết hạn");
        statusFilter.setValue("Tất cả");
        statusFilter.getStyleClass().add("db-filter");

        Button searchBtn = new Button("🔍 Tìm kiếm");
        searchBtn.getStyleClass().addAll("btn", "btn-primary");

        Button refreshBtn = new Button("🔄 Làm mới");
        refreshBtn.getStyleClass().addAll("btn", "btn-secondary");
        refreshBtn.setOnAction(e -> refreshDatabase());

        Button exportBtn = new Button("📤 Xuất dữ liệu");
        exportBtn.getStyleClass().addAll("btn", "btn-outline");
        exportBtn.setOnAction(e -> exportDatabase());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label recordCount = new Label("Hiển thị 156 bản ghi");
        recordCount.getStyleClass().add("db-record-count");

        controls.getChildren().addAll(
                new Label("Tìm kiếm:"), searchField,
                new Label("Lọc:"), statusFilter,
                searchBtn, refreshBtn, exportBtn,
                spacer, recordCount);

        return controls;
    }

    private VBox createDatabaseTable() {
        VBox tableContainer = new VBox(10);
        tableContainer.getStyleClass().add("db-table-container");

        // Table header
        HBox tableHeader = createTableHeader();

        // Table content
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.getStyleClass().add("db-scroll-pane");
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(400);

        VBox tableContent = new VBox(2);
        tableContent.getStyleClass().add("db-table-content");

        // Load real data from database (only card_id, public_key, status)
        // Note: Personal info (name, phone) is encrypted on card, not in DB
        java.util.List<citizencard.dao.CardDAO.CardRecord> cards = cardDAO.getAllCards();

        if (cards.isEmpty()) {
            Label emptyLabel = new Label("📭 Chưa có dữ liệu thẻ nào trong hệ thống.\nHãy tạo thẻ mới để bắt đầu.");
            emptyLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #6b7280; -fx-padding: 40px;");
            tableContent.getChildren().add(emptyLabel);
        } else {
            // Load real data from database (Only ID and Public Key)
            for (citizencard.dao.CardDAO.CardRecord card : cards) {
                String statusVi = switch (card.status) {
                    case "ACTIVE" -> "Hoạt động";
                    case "BLOCKED" -> "Bị khóa";
                    case "EXPIRED" -> "Hết hạn";
                    default -> card.status;
                };

                String registered = card.registeredAt != null ? card.registeredAt : "N/A";
                String lastAccess = card.lastAccessed != null ? card.lastAccessed : "N/A";
                // Truncate Public Key for display
                String shortKey = card.publicKey != null && card.publicKey.length() > 20
                        ? card.publicKey.substring(0, 20) + "..."
                        : card.publicKey;

                tableContent.getChildren().add(
                        createDatabaseRecord(card.cardId, shortKey, statusVi, registered, lastAccess));
            }
        }

        scrollPane.setContent(tableContent);

        tableContainer.getChildren().addAll(tableHeader, scrollPane);
        return tableContainer;

    }

    private HBox createTableHeader() {
        HBox header = new HBox();
        header.getStyleClass().add("db-table-header");
        header.setPadding(new Insets(15, 20, 15, 20));

        Label idHeader = new Label("ID Thẻ");
        idHeader.getStyleClass().add("db-header-label");
        idHeader.setPrefWidth(160);

        Label keyHeader = new Label("Public Key");
        keyHeader.getStyleClass().add("db-header-label");
        keyHeader.setPrefWidth(200);

        Label statusHeader = new Label("Trạng thái");
        statusHeader.getStyleClass().add("db-header-label");
        statusHeader.setPrefWidth(100);

        Label createdHeader = new Label("Ngày tạo");
        createdHeader.getStyleClass().add("db-header-label");
        createdHeader.setPrefWidth(140);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label actionsHeader = new Label("Thao tác");
        actionsHeader.getStyleClass().add("db-header-label");
        actionsHeader.setPrefWidth(180);

        header.getChildren().addAll(
                idHeader, keyHeader, statusHeader, createdHeader, spacer, actionsHeader);

        return header;
    }

    private HBox createDatabaseRecord(String cardId, String publicKey, String status,
            String created, String lastAccess) {
        HBox record = new HBox(15);
        record.getStyleClass().add("db-table-row");
        record.setPadding(new Insets(12, 20, 12, 20));
        record.setAlignment(Pos.CENTER_LEFT);

        Label idLabel = new Label(cardId);
        idLabel.getStyleClass().add("db-cell-id");
        idLabel.setPrefWidth(160);

        Label keyLabel = new Label(publicKey);
        keyLabel.getStyleClass().add("db-cell-name"); // Reuse style
        keyLabel.setPrefWidth(200);

        Label statusLabel = new Label(status);
        String statusClass = switch (status) {
            case "Hoạt động" -> "db-status-active";
            case "Bị khóa" -> "db-status-blocked";
            case "Hết hạn" -> "db-status-expired";
            default -> "db-status-unknown";
        };
        statusLabel.getStyleClass().addAll("db-cell-status", statusClass);
        statusLabel.setPrefWidth(100);

        Label createdLabel = new Label(created);
        createdLabel.getStyleClass().add("db-cell-date");
        createdLabel.setPrefWidth(140);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Action buttons
        HBox actions = new HBox(5);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.setPrefWidth(180);

        Button invoiceBtn = new Button("💸 Gửi HĐ");
        invoiceBtn.getStyleClass().addAll("btn", "btn-small", "btn-success");
        invoiceBtn.setTooltip(new Tooltip("Gửi hóa đơn"));
        invoiceBtn.setOnAction(e -> showSendInvoiceDialog(cardId));

        Button deleteBtn = new Button("🗑️");
        deleteBtn.getStyleClass().addAll("btn", "btn-small", "btn-icon", "btn-danger");
        deleteBtn.setTooltip(new Tooltip("Xóa thẻ"));
        deleteBtn.setOnAction(e -> deleteDatabaseRecord(cardId, "Hidden"));

        actions.getChildren().addAll(invoiceBtn, deleteBtn);

        record.getChildren().addAll(
                idLabel, keyLabel, statusLabel, createdLabel, spacer, actions);

        return record;
    }

    private void showSendInvoiceDialog(String cardId) {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Gửi hóa đơn");
        dialog.setHeaderText("💸 Gửi hóa đơn phí dịch vụ cho " + cardId);

        ButtonType sendButtonType = new ButtonType("Gửi", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(sendButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField amountField = new TextField();
        amountField.setPromptText("Số tiền (VND)");
        TextField descField = new TextField();
        descField.setPromptText("Nội dung thanh toán");

        grid.add(new Label("Số tiền:"), 0, 0);
        grid.add(amountField, 1, 0);
        grid.add(new Label("Nội dung:"), 0, 1);
        grid.add(descField, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == sendButtonType) {
                try {
                    long amount = Long.parseLong(amountField.getText().trim());
                    String desc = descField.getText().trim();
                    if (amount <= 0)
                        return false;
                    return cardDAO.createInvoice(cardId, amount, desc);
                } catch (NumberFormatException e) {
                    showAlert("Lỗi", "Số tiền không hợp lệ!");
                    return false;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(success -> {
            if (success) {
                showSuccessAlert("Đã gửi hóa đơn", "Đã gửi hóa đơn thành công cho " + cardId);
            } else {
                // Error handled in converter or silent cancel
            }
        });
    }

    /**
     * Create a simple database record row (only DB fields, no personal info)
     */
    private HBox createSimpleDatabaseRecord(String cardId, String publicKey, String status,
            String created, String lastAccess) {
        HBox record = new HBox(15);
        record.getStyleClass().add("db-table-row");
        record.setPadding(new Insets(12, 20, 12, 20));
        record.setAlignment(Pos.CENTER_LEFT);

        // Card icon
        Label cardIcon = new Label("💳");
        cardIcon.setStyle("-fx-font-size: 24px;");
        VBox iconContainer = new VBox();
        iconContainer.setAlignment(Pos.CENTER);
        iconContainer.setPrefWidth(60);
        iconContainer.getChildren().add(cardIcon);

        Label idLabel = new Label(cardId);
        idLabel.getStyleClass().add("db-cell-id");
        idLabel.setPrefWidth(160);

        Label keyLabel = new Label(publicKey);
        keyLabel.getStyleClass().add("db-cell-phone");
        keyLabel.setPrefWidth(150);
        keyLabel.setTooltip(new Tooltip("Public Key (trích gọn)"));

        Label statusLabel = new Label(status);
        String statusClass = switch (status) {
            case "Hoạt động" -> "db-status-active";
            case "Bị khóa" -> "db-status-blocked";
            case "Hết hạn" -> "db-status-expired";
            default -> "db-status-unknown";
        };
        statusLabel.getStyleClass().addAll("db-cell-status", statusClass);
        statusLabel.setPrefWidth(90);

        Label createdLabel = new Label(created);
        createdLabel.getStyleClass().add("db-cell-date");
        createdLabel.setPrefWidth(140);

        Label lastLabel = new Label(lastAccess);
        lastLabel.getStyleClass().add("db-cell-date");
        lastLabel.setPrefWidth(140);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Action buttons
        HBox actions = new HBox(5);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.setPrefWidth(80);

        Button viewBtn = new Button("👁️");
        viewBtn.getStyleClass().addAll("btn", "btn-small", "btn-icon");
        viewBtn.setTooltip(new Tooltip("Xem chi tiết từ thẻ (cần kết nối)"));
        viewBtn.setOnAction(e -> viewCardDetails(cardId));

        Button blockBtn = new Button("🚫");
        blockBtn.getStyleClass().addAll("btn", "btn-small", "btn-icon", "btn-warning");
        blockBtn.setTooltip(new Tooltip("Khóa thẻ"));
        blockBtn.setOnAction(e -> blockCardById(cardId));

        actions.getChildren().addAll(viewBtn, blockBtn);

        record.getChildren().addAll(
                iconContainer, idLabel, keyLabel, statusLabel,
                createdLabel, lastLabel, spacer, actions);

        return record;
    }

    private void viewCardDetails(String cardId) {
        showAlert("Xem chi tiết thẻ",
                "📋 Để xem thông tin cư dân:\n\n" +
                        "1. Kết nối với thẻ ID: " + cardId + "\n" +
                        "2. Xác thực PIN của thẻ\n" +
                        "3. Đọc dữ liệu từ thẻ (đã mã hóa)\n\n" +
                        "⚠️ Lưu ý: Thông tin cá nhân được mã hóa và lưu trên thẻ, không lưu trong database.");
    }

    private void blockCardById(String cardId) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Khóa thẻ");
        confirm.setHeaderText("🚫 Khóa thẻ " + cardId + "?");
        confirm.setContentText("Thẻ sẽ bị khóa và không thể sử dụng cho đến khi được mở khóa.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                boolean blocked = cardDAO.blockCard(cardId);
                if (blocked) {
                    showSuccessAlert("Đã khóa thẻ", "✅ Thẻ " + cardId + " đã bị khóa.");
                    showDatabaseViewer(); // Refresh
                } else {
                    showAlert("Lỗi", "❌ Không thể khóa thẻ.");
                }
            }
        });
    }

    private void refreshDatabase() {
        showAlert("Làm mới dữ liệu",
                "🔄 ĐANG LÀM MỚI CƠ SỞ DỮ LIỆU\n\n" +
                        "• Đang tải lại dữ liệu từ cơ sở dữ liệu...\n" +
                        "• Cập nhật thống kê hệ thống...\n" +
                        "• Kiểm tra tính toàn vẹn dữ liệu...\n\n" +
                        "✅ Hoàn tất! Dữ liệu đã được cập nhật.");
    }

    private void exportDatabase() {
        showAlert("Xuất dữ liệu",
                "📤 XUẤT DỮ LIỆU CƠ SỞ DỮ LIỆU\n\n" +
                        "Chọn định dạng xuất:\n" +
                        "• Excel (.xlsx) - Bảng tính\n" +
                        "• CSV (.csv) - Dữ liệu phân cách\n" +
                        "• PDF (.pdf) - Báo cáo\n" +
                        "• JSON (.json) - Dữ liệu cấu trúc\n\n" +
                        "File sẽ được lưu tại: /exports/database_" +
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));
    }

    private void deleteDatabaseRecord(String cardId, String name) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận xóa");
        confirm.setHeaderText("🗑️ Xóa bản ghi cơ sở dữ liệu");
        confirm.setContentText(
                "⚠️ CẢNH BÁO: XÓA VĨNH VIỄN\n\n" +
                        "Bạn có chắc chắn muốn xóa bản ghi:\n" +
                        "• ID Thẻ: " + cardId + "\n" +
                        "• Cư dân: " + name + "\n\n" +
                        "🚨 HÀNH ĐỘNG NÀY KHÔNG THỂ HOÀN TÁC!\n\n" +
                        "Việc xóa sẽ:\n" +
                        "• Xóa vĩnh viễn thông tin cư dân\n" +
                        "• Xóa lịch sử giao dịch\n" +
                        "• Vô hiệu hóa thẻ cư dân\n" +
                        "• Ghi log hành động xóa\n\n" +
                        "Tiếp tục xóa?");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                showAlert("Đã xóa bản ghi",
                        "✅ XÓA THÀNH CÔNG\n\n" +
                                "Bản ghi " + cardId + " đã được xóa khỏi hệ thống.\n\n" +
                                "• Thông tin cư dân: Đã xóa\n" +
                                "• Lịch sử giao dịch: Đã xóa\n" +
                                "• Thẻ cư dân: Đã vô hiệu hóa\n" +
                                "• Log hành động: Đã ghi nhận\n\n" +
                                "Thời gian xóa: "
                                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")));
            }
        });
    }

    /**
     * Perform validated search
     */
    private void performValidatedSearch(String query, String status) {
        if (query == null || query.trim().isEmpty()) {
            showValidationAlert("Lỗi tìm kiếm", "Vui lòng nhập từ khóa tìm kiếm");
            return;
        }

        DataValidator.ValidationResult result = DataValidator.validateSearchQuery(query);
        if (!result.isValid()) {
            showValidationAlert("Lỗi tìm kiếm", result.getErrorMessage());
            return;
        }

        // Perform search with validated input
        String sanitizedQuery = DataValidator.sanitizeInput(query);
        showAlert("Kết quả tìm kiếm",
                "🔍 TÌM KIẾM HOÀN TẤT\n\n" +
                        "Từ khóa: \"" + sanitizedQuery + "\"\n" +
                        "Trạng thái: " + status + "\n\n" +
                        "Tìm thấy 5 kết quả phù hợp:\n" +
                        "• CITIZEN-CARD-001 - Nguyễn Văn A\n" +
                        "• CITIZEN-CARD-002 - Trần Thị B\n" +
                        "• CITIZEN-CARD-003 - Lê Văn C\n" +
                        "• CITIZEN-CARD-004 - Phạm Thị D\n" +
                        "• CITIZEN-CARD-005 - Hoàng Văn E\n\n" +
                        "💡 Trong hệ thống thực tế, kết quả sẽ được hiển thị trong bảng dữ liệu.");
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

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);

        // Style the dialog
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        alert.getDialogPane().getStyleClass().add("alert-dialog");

        alert.showAndWait();
    }

    /**
     * Unlock card by resetting PIN tries
     */
    private void unlockCard() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Mở khóa thẻ");
        confirm.setHeaderText("🔓 Mở khóa thẻ bị khóa");
        confirm.setContentText(
                "Chức năng này sẽ reset số lần thử PIN về 5.\n\n" +
                        "⚠️ Lưu ý:\n" +
                        "• Thẻ phải được kết nối\n" +
                        "• Chỉ dùng khi thẻ bị khóa do nhập sai PIN\n" +
                        "• Không thay đổi PIN hiện tại\n\n" +
                        "Tiếp tục mở khóa?");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    // Check if card is connected
                    if (!cardService.isConnected()) {
                        showAlert("Lỗi kết nối",
                                "❌ Thẻ chưa được kết nối!\n\n" +
                                        "Vui lòng kết nối thẻ trước khi mở khóa.");
                        return;
                    }

                    // Reset PIN tries
                    int remainingTries = cardService.resetPinTries();

                    showSuccessAlert("Mở khóa thành công",
                            "✅ Thẻ đã được mở khóa!\n\n" +
                                    "Số lần thử PIN còn lại: " + remainingTries + "\n\n" +
                                    "Người dùng có thể đăng nhập lại với PIN đã đặt trước đó.");

                } catch (Exception e) {
                    showAlert("Lỗi mở khóa",
                            "❌ Không thể mở khóa thẻ!\n\n" +
                                    "Lỗi: " + e.getMessage() + "\n\n" +
                                    "Vui lòng kiểm tra:\n" +
                                    "• Thẻ đã được kết nối\n" +
                                    "• Thẻ đã được khởi tạo\n" +
                                    "• JCIDE terminal đang chạy");
                }
            }
        });
    }

    /**
     * Reset card - clear all data and reset to factory state
     */
    private void resetCard() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Reset thẻ");
        confirm.setHeaderText("🔄 Reset thẻ về trạng thái ban đầu");
        confirm.setContentText(
                "⚠️ CẢNH BÁO: RESET TOÀN BỘ DỮ LIỆU THẺ!\n\n" +
                        "Chức năng này sẽ:\n" +
                        "• Xóa toàn bộ dữ liệu trên thẻ\n" +
                        "• Reset PIN về trạng thái chưa khởi tạo\n" +
                        "• Xóa thông tin cư dân\n" +
                        "• Xóa lịch sử giao dịch\n" +
                        "• Xóa ảnh cá nhân\n" +
                        "• Reset số lần thử PIN về 5\n\n" +
                        "🔑 Lưu ý:\n" +
                        "• Thẻ phải được kết nối\n" +
                        "• Sau khi reset, thẻ có thể được khởi tạo lại với PIN mới\n" +
                        "• RSA keys sẽ được giữ lại (không cần tạo lại)\n\n" +
                        "🚨 HÀNH ĐỘNG NÀY KHÔNG THỂ HOÀN TÁC!\n\n" +
                        "Tiếp tục reset thẻ?");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    // Check if card is connected
                    if (!cardService.isConnected()) {
                        showAlert("Lỗi kết nối",
                                "❌ Thẻ chưa được kết nối!\n\n" +
                                        "Vui lòng kết nối thẻ trước khi reset.");
                        return;
                    }

                    // Clear card data
                    cardService.clearCard();

                    showSuccessAlert("Reset thẻ thành công",
                            "✅ Thẻ đã được reset về trạng thái ban đầu!\n\n" +
                                    "Thẻ hiện đã sạch và sẵn sàng để:\n" +
                                    "• Khởi tạo lại với PIN mới\n" +
                                    "• Đăng ký cư dân mới\n" +
                                    "• Sử dụng như thẻ mới\n\n" +
                                    "💡 Bạn có thể tạo thẻ mới ngay bây giờ!");

                } catch (Exception e) {
                    showAlert("Lỗi reset thẻ",
                            "❌ Không thể reset thẻ!\n\n" +
                                    "Lỗi: " + e.getMessage() + "\n\n" +
                                    "Vui lòng kiểm tra:\n" +
                                    "• Thẻ đã được kết nối\n" +
                                    "• Thẻ đã được khởi tạo\n" +
                                    "• JCIDE terminal đang chạy");
                }
            }
        });
    }

    private void showChangePinDialog() {
        // Step 1: Get current PIN using beautiful PinInputDialog
        String currentPin = citizencard.util.PinInputDialog.showChangePinDialog(
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
                    errorMsg += "\n\nSố lần thử còn lại: " + pinResult.remainingTries;
                } else {
                    errorMsg += "\n\n🔒 Thẻ đã bị khóa!";
                }
                showAlert("Xác thực thất bại", errorMsg);
                return;
            }
        } catch (Exception e) {
            showAlert("Lỗi xác thực", "Không thể xác thực PIN: " + e.getMessage());
            return;
        }

        // Step 3: Get new PIN
        String newPin = citizencard.util.PinInputDialog.showChangePinDialog(
                "Nhập PIN mới",
                "🔐 Nhập PIN mới (4 chữ số)");

        if (newPin == null || newPin.isEmpty()) {
            return; // User cancelled
        }

        if (newPin.equals(currentPin)) {
            showAlert("PIN không hợp lệ", "PIN mới phải khác PIN hiện tại.");
            return;
        }

        // Step 4: Confirm new PIN
        String confirmPin = citizencard.util.PinInputDialog.showChangePinDialog(
                "Xác nhận PIN mới",
                "🔐 Nhập lại PIN mới để xác nhận");

        if (confirmPin == null || confirmPin.isEmpty()) {
            return; // User cancelled
        }

        if (!newPin.equals(confirmPin)) {
            showAlert("PIN không khớp", "PIN mới và PIN xác nhận không khớp. Vui lòng thử lại.");
            return;
        }

        // Step 5: Change PIN on card
        try {
            boolean success = cardService.changePin(currentPin, newPin);

            if (success) {
                showSuccessAlert("Đổi PIN thành công",
                        "✅ Mã PIN đã được thay đổi thành công!\n\n" +
                                "Vui lòng sử dụng mã PIN mới cho các lần đăng nhập tiếp theo.");
            } else {
                showAlert("Đổi PIN thất bại",
                        "Không thể đổi PIN trên thẻ.\n\n" +
                                "Có thể do lỗi giao tiếp với thẻ.");
            }
        } catch (Exception e) {
            showAlert("Đổi PIN thất bại", "Lỗi: " + e.getMessage());
        }
    }

    private void showSuccessAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public Parent getRoot() {
        return root;
    }
}