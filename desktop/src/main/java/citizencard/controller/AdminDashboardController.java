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
 * Focused on core functions: citizen card management and citizen data management
 */
public class AdminDashboardController {
    
    private BorderPane root;
    private CardService cardService;
    private CardDAO cardDAO;
    private DemoWorkflowController demoController;
    private VBox contentArea;
    
    public AdminDashboardController() {
        cardService = new CardService();
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
        
        Button citizenModeButton = new Button("Chuyển sang chế độ Cư dân");
        citizenModeButton.getStyleClass().addAll("btn", "btn-outline");
        citizenModeButton.setOnAction(e -> switchToCitizenMode());
        
        header.getChildren().addAll(titleLabel, spacer, timeLabel, citizenModeButton);
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
        
        Button cardMgmtBtn = createMenuButton("💳 Quản lý thẻ", "Tạo và quản lý thẻ cư dân");
        cardMgmtBtn.setOnAction(e -> showCardManagement());
        
        Button citizenDataBtn = createMenuButton("👥 Dữ liệu cư dân", "Quản lý thông tin cư dân");
        citizenDataBtn.setOnAction(e -> showCitizenDataManagement());
        
        Button databaseBtn = createMenuButton("🗄️ Cơ sở dữ liệu", "Xem tất cả thẻ đã đăng ký");
        databaseBtn.setOnAction(e -> showDatabaseViewer());
        
        menu.getChildren().addAll(
            dashboardBtn, 
            new Separator(),
            cardMgmtBtn, 
            citizenDataBtn,
            new Separator(),
            databaseBtn
        );
        
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
        
        VBox totalCardsCard = createStatCard("Tổng số thẻ", "156", "💳", "#3b82f6");
        VBox activeCardsCard = createStatCard("Thẻ hoạt động", "142", "✅", "#22c55e");
        VBox citizenDataCard = createStatCard("Dữ liệu cư dân", "156 hồ sơ", "👥", "#f59e0b");
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
        
        activityList.getChildren().addAll(
            createActivityItem("Đăng ký thẻ mới", "CITIZEN-CARD-20251216091755", "2 phút trước", "success"),
            createActivityItem("Đổi PIN", "CITIZEN-CARD-001", "15 phút trước", "info"),
            createActivityItem("Giao dịch hoàn tất", "Thanh toán 500,000 VND", "1 giờ trước", "success"),
            createActivityItem("Khóa thẻ", "CITIZEN-CARD-045", "3 giờ trước", "warning"),
            createActivityItem("Sao lưu hệ thống", "Sao lưu cơ sở dữ liệu thành công", "6 giờ trước", "info")
        );
        
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
        
        Button systemBackupBtn = new Button("Sao lưu hệ thống");
        systemBackupBtn.getStyleClass().addAll("btn", "btn-outline", "btn-large");
        systemBackupBtn.setOnAction(e -> performSystemBackup());
        
        actionsRow.getChildren().addAll(createCardBtn, viewDatabaseBtn, systemBackupBtn);
        
        section.getChildren().addAll(sectionTitle, actionsRow);
        return section;
    }
    
    private void showCitizenDataManagement() {
        contentArea.getChildren().clear();
        
        Label pageTitle = new Label("Quản lý dữ liệu cư dân");
        pageTitle.getStyleClass().add("page-title");
        
        // Action buttons
        HBox actionButtons = new HBox(15);
        
        Button viewAllBtn = new Button("👥 Xem tất cả cư dân");
        viewAllBtn.getStyleClass().addAll("btn", "btn-primary");
        viewAllBtn.setOnAction(e -> showAllCitizens());
        
        Button searchBtn = new Button("🔍 Tìm kiếm cư dân");
        searchBtn.getStyleClass().addAll("btn", "btn-secondary");
        searchBtn.setOnAction(e -> showCitizenSearch());
        
        Button statisticsBtn = new Button("📊 Thống kê");
        statisticsBtn.getStyleClass().addAll("btn", "btn-outline");
        statisticsBtn.setOnAction(e -> showCitizenStatistics());
        
        actionButtons.getChildren().addAll(viewAllBtn, searchBtn, statisticsBtn);
        
        // Search and filter
        HBox searchRow = new HBox(15);
        searchRow.setAlignment(Pos.CENTER_LEFT);
        
        TextField searchField = new TextField();
        searchField.setPromptText("Tìm kiếm theo ID thẻ, tên, số điện thoại...");
        searchField.setPrefWidth(300);
        
        ComboBox<String> statusFilter = new ComboBox<>();
        statusFilter.getItems().addAll("Tất cả trạng thái", "Hoạt động", "Bị khóa", "Hết hạn");
        statusFilter.setValue("Tất cả trạng thái");
        
        Button quickSearchBtn = new Button("Tìm kiếm");
        quickSearchBtn.getStyleClass().addAll("btn", "btn-primary");
        
        searchRow.getChildren().addAll(new Label("Tìm kiếm:"), searchField, new Label("Trạng thái:"), statusFilter, quickSearchBtn);
        
        // Citizens data table
        VBox citizensTable = createCitizensDataTable();
        
        contentArea.getChildren().addAll(pageTitle, actionButtons, new Separator(), searchRow, citizensTable);
    }
    
    private VBox createCitizensDataTable() {
        VBox section = new VBox(15);
        
        Label sectionTitle = new Label("Danh sách cư dân");
        sectionTitle.getStyleClass().add("section-title");
        
        // Sample citizen data
        VBox citizensList = new VBox(5);
        citizensList.getStyleClass().add("citizens-list");
        
        citizensList.getChildren().addAll(
            createCitizenDataItem("CITIZEN-CARD-001", "Nguyễn Văn A", "0901234567", "Hoạt động", "15-12-2025"),
            createCitizenDataItem("CITIZEN-CARD-002", "Trần Thị B", "0912345678", "Hoạt động", "16-12-2025"),
            createCitizenDataItem("CITIZEN-CARD-003", "Lê Văn C", "0923456789", "Bị khóa", "14-12-2025"),
            createCitizenDataItem("CITIZEN-CARD-004", "Phạm Thị D", "0934567890", "Hoạt động", "13-12-2025"),
            createCitizenDataItem("CITIZEN-CARD-005", "Hoàng Văn E", "0945678901", "Hoạt động", "12-12-2025")
        );
        
        section.getChildren().addAll(sectionTitle, citizensList);
        return section;
    }
    
    private HBox createCitizenDataItem(String cardId, String name, String phone, String status, String date) {
        HBox item = new HBox(15);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(12));
        item.getStyleClass().add("citizen-data-item");
        
        Label cardIdLabel = new Label(cardId);
        cardIdLabel.getStyleClass().add("citizen-card-id");
        cardIdLabel.setPrefWidth(150);
        
        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("citizen-name");
        nameLabel.setPrefWidth(120);
        
        Label phoneLabel = new Label(phone);
        phoneLabel.getStyleClass().add("citizen-phone");
        phoneLabel.setPrefWidth(100);
        
        Label statusLabel = new Label(status);
        statusLabel.getStyleClass().add(status.equals("Hoạt động") ? "status-success" : "status-error");
        statusLabel.setPrefWidth(80);
        
        Label dateLabel = new Label(date);
        dateLabel.getStyleClass().add("citizen-date");
        dateLabel.setPrefWidth(100);
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Button viewBtn = new Button("Xem");
        viewBtn.getStyleClass().addAll("btn", "btn-small", "btn-outline");
        viewBtn.setOnAction(e -> showCitizenDetails(cardId, name));
        
        Button editBtn = new Button("Sửa");
        editBtn.getStyleClass().addAll("btn", "btn-small", "btn-secondary");
        editBtn.setOnAction(e -> editCitizenData(cardId, name));
        
        HBox actionButtons = new HBox(5);
        actionButtons.getChildren().addAll(viewBtn, editBtn);
        
        item.getChildren().addAll(cardIdLabel, nameLabel, phoneLabel, statusLabel, dateLabel, spacer, actionButtons);
        return item;
    }
    
    private void showCardManagement() {
        contentArea.getChildren().clear();
        
        Label pageTitle = new Label("Quản lý thẻ cư dân");
        pageTitle.getStyleClass().add("page-title");
        
        // Action buttons
        HBox actionButtons = new HBox(15);
        
        Button createNewBtn = new Button("🆕 Tạo thẻ mới");
        createNewBtn.getStyleClass().addAll("btn", "btn-primary");
        createNewBtn.setOnAction(e -> demoController.showCreateNewCard());
        
        Button viewCardsBtn = new Button("📋 Xem tất cả thẻ");
        viewCardsBtn.getStyleClass().addAll("btn", "btn-secondary");
        viewCardsBtn.setOnAction(e -> showAllCards());
        
        Button cardStatsBtn = new Button("📊 Thống kê thẻ");
        cardStatsBtn.getStyleClass().addAll("btn", "btn-outline");
        cardStatsBtn.setOnAction(e -> showCardStatistics());
        
        actionButtons.getChildren().addAll(createNewBtn, viewCardsBtn, cardStatsBtn);
        
        // Search and filter
        HBox searchRow = new HBox(15);
        searchRow.setAlignment(Pos.CENTER_LEFT);
        
        TextField searchField = new TextField();
        searchField.setPromptText("Tìm kiếm thẻ theo ID, tên, hoặc số điện thoại...");
        searchField.setPrefWidth(300);
        
        ComboBox<String> statusFilter = new ComboBox<>();
        statusFilter.getItems().addAll("Tất cả trạng thái", "Hoạt động", "Bị khóa", "Hết hạn");
        statusFilter.setValue("Tất cả trạng thái");
        
        Button searchBtn = new Button("Tìm kiếm");
        searchBtn.getStyleClass().addAll("btn", "btn-primary");
        
        searchRow.getChildren().addAll(new Label("Tìm kiếm:"), searchField, new Label("Trạng thái:"), statusFilter, searchBtn);
        
        // Cards table
        TableView<String> cardsTable = createCardsTable();
        
        contentArea.getChildren().addAll(pageTitle, actionButtons, new Separator(), searchRow, cardsTable);
    }
    
    private TableView<String> createCardsTable() {
        TableView<String> table = new TableView<>();
        table.setPrefHeight(400);
        
        // Add sample data
        table.getItems().addAll(
            "CITIZEN-CARD-001 | Nguyen Van A | 0901234567 | ACTIVE | 2025-12-15",
            "CITIZEN-CARD-002 | Tran Thi B | 0912345678 | ACTIVE | 2025-12-16",
            "CITIZEN-CARD-003 | Le Van C | 0923456789 | BLOCKED | 2025-12-14"
        );
        
        return table;
    }
    
    private void showDatabaseViewer() {
        Alert dbViewerDialog = new Alert(Alert.AlertType.INFORMATION);
        dbViewerDialog.setTitle("Database Viewer");
        dbViewerDialog.setHeaderText("🗄️ Xem cơ sở dữ liệu H2");
        
        String content = 
            "CHỌN CÁCH XEM DATABASE:\n\n" +
            
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
                "Đóng ứng dụng để tự động dừng console."
            );
            success.getDialogPane().getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            success.showAndWait();
            
        } catch (Exception e) {
            Alert error = new Alert(Alert.AlertType.ERROR);
            error.setTitle("Lỗi khởi động Console");
            error.setHeaderText("❌ Không thể khởi động H2 Console");
            error.setContentText("Lỗi: " + e.getMessage() + "\n\nVui lòng kiểm tra:\n• Database có đang được sử dụng?\n• Port 8082 có bị chiếm?");
            error.showAndWait();
        }
    }
    
    private void printDatabaseToConsole() {
        try {
            System.out.println("\n" + "=".repeat(60));
            System.out.println("🗄️ DATABASE VIEWER - ADMIN REQUEST");
            System.out.println("=".repeat(60));
            
            DatabaseViewer.printDatabaseContent();
            
            Alert success = new Alert(Alert.AlertType.INFORMATION);
            success.setTitle("Dữ liệu đã in");
            success.setHeaderText("✅ Dữ liệu database đã được in ra console");
            success.setContentText(
                "Dữ liệu database đã được in ra console/terminal.\n\n" +
                "📋 Bao gồm:\n" +
                "• Danh sách thẻ đã đăng ký\n" +
                "• 10 giao dịch gần nhất\n" +
                "• Thông tin chi tiết từng bản ghi\n\n" +
                "💡 Kiểm tra console để xem chi tiết."
            );
            success.showAndWait();
            
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
                "💡 Kiểm tra console để xem chi tiết."
            );
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
    
    private void showAllCitizens() {
        showAlert("Danh sách cư dân", 
            "📋 DANH SÁCH CƯ DÂN\n\n" +
            "Tổng số cư dân: 156\n" +
            "• Hoạt động: 142 cư dân\n" +
            "• Bị khóa: 12 cư dân\n" +
            "• Hết hạn: 2 cư dân\n\n" +
            "Chức năng này sẽ hiển thị bảng danh sách đầy đủ với:\n" +
            "• Thông tin chi tiết từng cư dân\n" +
            "• Tùy chọn lọc và sắp xếp\n" +
            "• Chức năng xuất dữ liệu");
    }
    
    private void showCitizenSearch() {
        showAlert("Tìm kiếm cư dân", 
            "🔍 TÌM KIẾM NÂNG CAO\n\n" +
            "Tìm kiếm cư dân theo:\n" +
            "• ID thẻ\n" +
            "• Họ tên\n" +
            "• Số điện thoại\n" +
            "• Địa chỉ\n" +
            "• Ngày sinh\n" +
            "• Trạng thái thẻ");
    }
    
    private void showCitizenStatistics() {
        showAlert("Thống kê cư dân", 
            "📊 THỐNG KÊ DỮ LIỆU CƯ DÂN\n\n" +
            "• Tổng số cư dân: 156\n" +
            "• Thẻ hoạt động: 142 (91%)\n" +
            "• Thẻ bị khóa: 12 (8%)\n" +
            "• Thẻ hết hạn: 2 (1%)\n\n" +
            "• Đăng ký mới tháng này: 23\n" +
            "• Trung bình tuổi: 35 tuổi\n" +
            "• Tỷ lệ nam/nữ: 52%/48%");
    }
    
    private void showCitizenDetails(String cardId, String name) {
        showAlert("Chi tiết cư dân", 
            "👤 THÔNG TIN CƯ DÂN\n\n" +
            "ID Thẻ: " + cardId + "\n" +
            "Họ tên: " + name + "\n" +
            "Ngày sinh: 15/03/1988\n" +
            "Giới tính: Nam\n" +
            "Địa chỉ: 123 Đường ABC, Quận 1, TP.HCM\n" +
            "Số điện thoại: 0901234567\n" +
            "Email: nguyen.van.a@email.com\n\n" +
            "Trạng thái thẻ: Hoạt động\n" +
            "Ngày phát hành: 15-12-2025\n" +
            "Ngày hết hạn: 15-12-2030");
    }
    
    private void editCitizenData(String cardId, String name) {
        showAlert("Chỉnh sửa dữ liệu", 
            "✏️ CHỈNH SỬA THÔNG TIN CƯ DÂN\n\n" +
            "Cư dân: " + name + "\n" +
            "ID Thẻ: " + cardId + "\n\n" +
            "Chức năng này sẽ mở form chỉnh sửa cho phép cập nhật:\n" +
            "• Thông tin cá nhân\n" +
            "• Địa chỉ liên hệ\n" +
            "• Trạng thái thẻ\n" +
            "• Ghi chú quản lý");
    }
    
    private void showAllCards() {
        showAlert("Danh sách tất cả thẻ", 
            "📋 DANH SÁCH THẺ CƯ DÂN\n\n" +
            "Tổng số thẻ: 156\n" +
            "• Hoạt động: 142 thẻ\n" +
            "• Bị khóa: 12 thẻ\n" +
            "• Hết hạn: 2 thẻ\n\n" +
            "Chức năng này sẽ hiển thị bảng danh sách đầy đủ với:\n" +
            "• Thông tin chi tiết từng thẻ\n" +
            "• Tùy chọn lọc và sắp xếp\n" +
            "• Chức năng xuất dữ liệu");
    }
    
    private void showCardStatistics() {
        showAlert("Thống kê thẻ cư dân", 
            "📊 THỐNG KÊ THẺ CƯ DÂN\n\n" +
            "📈 Tổng quan:\n" +
            "• Tổng số thẻ phát hành: 156\n" +
            "• Thẻ đang hoạt động: 142 (91%)\n" +
            "• Thẻ bị khóa: 12 (8%)\n" +
            "• Thẻ hết hạn: 2 (1%)\n\n" +
            "📅 Theo thời gian:\n" +
            "• Phát hành tháng này: 23 thẻ\n" +
            "• Phát hành tuần này: 7 thẻ\n" +
            "• Phát hành hôm nay: 2 thẻ\n\n" +
            "🔒 Bảo mật:\n" +
            "• Thẻ cần đổi PIN: 5\n" +
            "• Thẻ sắp hết hạn: 8");
    }
    
    private void switchToCitizenMode() {
        showAlert("Chuyển chế độ", "Chuyển sang chế độ Cư dân...\n\nChức năng này sẽ quay lại màn hình đăng nhập chính để người dùng có thể chọn chế độ Cư dân.");
    }
    
    private void performSystemBackup() {
        showAlert("Sao lưu hệ thống", "Bắt đầu sao lưu cơ sở dữ liệu...\n\nFile sao lưu sẽ được lưu tại: /backups/citizen_card_" + 
                 LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".sql");
    }
    
    // =====================================================
    // DATABASE VIEWER COMPONENTS
    // =====================================================
    
    private HBox createDatabaseStatsRow() {
        HBox statsRow = new HBox(20);
        statsRow.setAlignment(Pos.CENTER_LEFT);
        
        VBox totalRecordsCard = createStatCard("Tổng bản ghi", "156", "🗄️", "#3b82f6");
        VBox activeCardsCard = createStatCard("Thẻ hoạt động", "142", "✅", "#22c55e");
        VBox blockedCardsCard = createStatCard("Thẻ bị khóa", "12", "🚫", "#ef4444");
        VBox dbSizeCard = createStatCard("Kích thước DB", "2.4 MB", "💾", "#8b5cf6");
        
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
            spacer, recordCount
        );
        
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
        
        // Sample database records with photo support
        tableContent.getChildren().addAll(
            createDatabaseRecord("CITIZEN-CARD-001", "Nguyễn Văn A", "0901234567", "Hoạt động", "500,000 VND", "15-12-2025 08:30", "16-12-2025 09:15"),
            createDatabaseRecord("CITIZEN-CARD-002", "Trần Thị B", "0912345678", "Hoạt động", "750,000 VND", "15-12-2025 09:00", "16-12-2025 08:45"),
            createDatabaseRecord("CITIZEN-CARD-003", "Lê Văn C", "0923456789", "Bị khóa", "0 VND", "14-12-2025 14:20", "15-12-2025 16:30"),
            createDatabaseRecord("CITIZEN-CARD-004", "Phạm Thị D", "0934567890", "Hoạt động", "1,200,000 VND", "13-12-2025 11:15", "16-12-2025 07:20"),
            createDatabaseRecord("CITIZEN-CARD-005", "Hoàng Văn E", "0945678901", "Hoạt động", "300,000 VND", "12-12-2025 16:45", "15-12-2025 19:10"),
            createDatabaseRecord("CITIZEN-CARD-006", "Võ Thị F", "0956789012", "Hoạt động", "850,000 VND", "11-12-2025 10:30", "16-12-2025 06:55"),
            createDatabaseRecord("CITIZEN-CARD-007", "Đặng Văn G", "0967890123", "Hết hạn", "0 VND", "10-12-2025 13:20", "12-12-2025 14:40"),
            createDatabaseRecord("CITIZEN-CARD-008", "Bùi Thị H", "0978901234", "Hoạt động", "650,000 VND", "09-12-2025 15:10", "16-12-2025 05:30")
        );
        
        scrollPane.setContent(tableContent);
        
        tableContainer.getChildren().addAll(tableHeader, scrollPane);
        return tableContainer;
    }
    
    private HBox createTableHeader() {
        HBox header = new HBox();
        header.getStyleClass().add("db-table-header");
        header.setPadding(new Insets(15, 20, 15, 20));
        
        Label photoHeader = new Label("Ảnh");
        photoHeader.getStyleClass().add("db-header-label");
        photoHeader.setPrefWidth(60);
        
        Label idHeader = new Label("ID Thẻ");
        idHeader.getStyleClass().add("db-header-label");
        idHeader.setPrefWidth(140);
        
        Label nameHeader = new Label("Họ tên");
        nameHeader.getStyleClass().add("db-header-label");
        nameHeader.setPrefWidth(120);
        
        Label phoneHeader = new Label("Số điện thoại");
        phoneHeader.getStyleClass().add("db-header-label");
        phoneHeader.setPrefWidth(110);
        
        Label statusHeader = new Label("Trạng thái");
        statusHeader.getStyleClass().add("db-header-label");
        statusHeader.setPrefWidth(90);
        
        Label balanceHeader = new Label("Số dư");
        balanceHeader.getStyleClass().add("db-header-label");
        balanceHeader.setPrefWidth(100);
        
        Label createdHeader = new Label("Ngày tạo");
        createdHeader.getStyleClass().add("db-header-label");
        createdHeader.setPrefWidth(120);
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Label actionsHeader = new Label("Thao tác");
        actionsHeader.getStyleClass().add("db-header-label");
        actionsHeader.setPrefWidth(120);
        
        header.getChildren().addAll(
            photoHeader, idHeader, nameHeader, phoneHeader, statusHeader, 
            balanceHeader, createdHeader, spacer, actionsHeader
        );
        
        return header;
    }
    
    private HBox createDatabaseRecord(String cardId, String name, String phone, String status, 
                                    String balance, String created, String lastAccess) {
        HBox record = new HBox(15);
        record.getStyleClass().add("db-table-row");
        record.setPadding(new Insets(12, 20, 12, 20));
        record.setAlignment(Pos.CENTER_LEFT);
        
        // Photo thumbnail
        javafx.scene.image.ImageView photoThumb = new javafx.scene.image.ImageView();
        photoThumb.setFitWidth(40);
        photoThumb.setFitHeight(40);
        photoThumb.setPreserveRatio(true);
        photoThumb.getStyleClass().add("db-photo-thumb");
        
        // Create sample avatar or use default
        try {
            // In real implementation, this would load from database
            // For demo, create a colored circle with initials
            javafx.scene.canvas.Canvas canvas = new javafx.scene.canvas.Canvas(40, 40);
            javafx.scene.canvas.GraphicsContext gc = canvas.getGraphicsContext2D();
            
            // Generate color based on name hash
            int hash = name.hashCode();
            String[] colors = {"#3b82f6", "#ef4444", "#10b981", "#f59e0b", "#8b5cf6", "#06b6d4"};
            String color = colors[Math.abs(hash) % colors.length];
            
            gc.setFill(javafx.scene.paint.Color.web(color));
            gc.fillOval(0, 0, 40, 40);
            
            gc.setFill(javafx.scene.paint.Color.WHITE);
            gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 14));
            gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
            
            String initials = getInitials(name);
            gc.fillText(initials, 20, 25);
            
            javafx.scene.image.WritableImage avatar = new javafx.scene.image.WritableImage(40, 40);
            canvas.snapshot(null, avatar);
            photoThumb.setImage(avatar);
            
        } catch (Exception e) {
            // Fallback to default icon
            Label defaultIcon = new Label("👤");
            defaultIcon.setStyle("-fx-font-size: 24px;");
        }
        
        VBox photoContainer = new VBox();
        photoContainer.setAlignment(Pos.CENTER);
        photoContainer.setPrefWidth(60);
        photoContainer.getChildren().add(photoThumb);
        
        Label idLabel = new Label(cardId);
        idLabel.getStyleClass().add("db-cell-id");
        idLabel.setPrefWidth(140);
        
        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("db-cell-name");
        nameLabel.setPrefWidth(120);
        
        Label phoneLabel = new Label(phone);
        phoneLabel.getStyleClass().add("db-cell-phone");
        phoneLabel.setPrefWidth(110);
        
        Label statusLabel = new Label(status);
        String statusClass = switch (status) {
            case "Hoạt động" -> "db-status-active";
            case "Bị khóa" -> "db-status-blocked";
            case "Hết hạn" -> "db-status-expired";
            default -> "db-status-unknown";
        };
        statusLabel.getStyleClass().addAll("db-cell-status", statusClass);
        statusLabel.setPrefWidth(90);
        
        Label balanceLabel = new Label(balance);
        balanceLabel.getStyleClass().add("db-cell-balance");
        balanceLabel.setPrefWidth(100);
        
        Label createdLabel = new Label(created);
        createdLabel.getStyleClass().add("db-cell-date");
        createdLabel.setPrefWidth(120);
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        // Action buttons
        HBox actions = new HBox(5);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.setPrefWidth(120);
        
        Button viewBtn = new Button("👁️");
        viewBtn.getStyleClass().addAll("btn", "btn-small", "btn-icon");
        viewBtn.setTooltip(new Tooltip("Xem chi tiết"));
        viewBtn.setOnAction(e -> viewDatabaseRecord(cardId, name));
        
        Button editBtn = new Button("✏️");
        editBtn.getStyleClass().addAll("btn", "btn-small", "btn-icon");
        editBtn.setTooltip(new Tooltip("Chỉnh sửa"));
        editBtn.setOnAction(e -> editDatabaseRecord(cardId, name));
        
        Button photoBtn = new Button("📷");
        photoBtn.getStyleClass().addAll("btn", "btn-small", "btn-icon");
        photoBtn.setTooltip(new Tooltip("Xem ảnh"));
        photoBtn.setOnAction(e -> viewPhoto(cardId, name));
        
        Button deleteBtn = new Button("🗑️");
        deleteBtn.getStyleClass().addAll("btn", "btn-small", "btn-icon", "btn-danger");
        deleteBtn.setTooltip(new Tooltip("Xóa"));
        deleteBtn.setOnAction(e -> deleteDatabaseRecord(cardId, name));
        
        actions.getChildren().addAll(viewBtn, editBtn, photoBtn, deleteBtn);
        
        record.getChildren().addAll(
            photoContainer, idLabel, nameLabel, phoneLabel, statusLabel, 
            balanceLabel, createdLabel, spacer, actions
        );
        
        return record;
    }
    
    /**
     * Get initials from full name
     */
    private String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) return "?";
        
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        } else {
            return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
        }
    }
    
    /**
     * View photo in full size
     */
    private void viewPhoto(String cardId, String name) {
        Alert photoDialog = new Alert(Alert.AlertType.INFORMATION);
        photoDialog.setTitle("Ảnh cá nhân");
        photoDialog.setHeaderText("📷 Ảnh cá nhân - " + name);
        
        // Create photo viewer content
        VBox content = new VBox(15);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(20));
        
        // In real implementation, load actual photo from database
        javafx.scene.image.ImageView fullPhoto = new javafx.scene.image.ImageView();
        fullPhoto.setFitWidth(300);
        fullPhoto.setFitHeight(400);
        fullPhoto.setPreserveRatio(true);
        fullPhoto.getStyleClass().add("photo-viewer");
        
        // Create sample photo or show placeholder
        Label photoPlaceholder = new Label("📷\n\nẢnh cá nhân\n" + name + "\n\nTrong hệ thống thực tế,\nảnh sẽ được hiển thị ở đây");
        photoPlaceholder.getStyleClass().add("photo-placeholder");
        photoPlaceholder.setStyle(
            "-fx-font-size: 16px; " +
            "-fx-text-alignment: center; " +
            "-fx-text-fill: #6b7280; " +
            "-fx-background-color: #f9fafb; " +
            "-fx-border-color: #d1d5db; " +
            "-fx-border-width: 2; " +
            "-fx-border-style: dashed; " +
            "-fx-padding: 40px; " +
            "-fx-background-radius: 12px; " +
            "-fx-border-radius: 12px;"
        );
        
        Label photoInfo = new Label(
            "ID Thẻ: " + cardId + "\n" +
            "Cư dân: " + name + "\n" +
            "Định dạng: JPEG\n" +
            "Kích thước: 300x400 pixels\n" +
            "Dung lượng: 45.2 KB\n" +
            "Ngày tải lên: 15-12-2025"
        );
        photoInfo.getStyleClass().add("photo-info-detail");
        photoInfo.setStyle(
            "-fx-font-size: 12px; " +
            "-fx-text-fill: #6b7280; " +
            "-fx-background-color: #f8fafc; " +
            "-fx-padding: 12px 16px; " +
            "-fx-background-radius: 8px;"
        );
        
        content.getChildren().addAll(photoPlaceholder, photoInfo);
        photoDialog.getDialogPane().setContent(content);
        
        // Add custom buttons
        ButtonType downloadBtn = new ButtonType("💾 Tải xuống", ButtonBar.ButtonData.OTHER);
        ButtonType updateBtn = new ButtonType("🔄 Cập nhật ảnh", ButtonBar.ButtonData.OTHER);
        ButtonType closeBtn = new ButtonType("Đóng", ButtonBar.ButtonData.CANCEL_CLOSE);
        
        photoDialog.getDialogPane().getButtonTypes().setAll(downloadBtn, updateBtn, closeBtn);
        
        photoDialog.showAndWait().ifPresent(response -> {
            if (response == downloadBtn) {
                showAlert("Tải xuống ảnh", "Chức năng tải xuống ảnh sẽ được triển khai trong phiên bản đầy đủ.");
            } else if (response == updateBtn) {
                showAlert("Cập nhật ảnh", "Chức năng cập nhật ảnh sẽ được triển khai trong phiên bản đầy đủ.");
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
    
    private void viewDatabaseRecord(String cardId, String name) {
        showAlert("Chi tiết bản ghi", 
            "👁️ CHI TIẾT BẢNG GHI CƠ SỞ DỮ LIỆU\n\n" +
            "🆔 ID Thẻ: " + cardId + "\n" +
            "👤 Họ tên: " + name + "\n" +
            "📱 Số điện thoại: 0901234567\n" +
            "📧 Email: nguyen.van.a@email.com\n" +
            "🏠 Địa chỉ: 123 Đường ABC, Quận 1, TP.HCM\n" +
            "🎂 Ngày sinh: 15/03/1988\n" +
            "👨 Giới tính: Nam\n\n" +
            "💳 THÔNG TIN THẺ:\n" +
            "• Trạng thái: Hoạt động\n" +
            "• Số dư: 500,000 VND\n" +
            "• Ngày tạo: 15-12-2025 08:30\n" +
            "• Truy cập cuối: 16-12-2025 09:15\n" +
            "• Số lần đăng nhập: 25\n" +
            "• Tổng giao dịch: 15");
    }
    
    private void editDatabaseRecord(String cardId, String name) {
        showAlert("Chỉnh sửa bản ghi", 
            "✏️ CHỈNH SỬA BẢNG GHI\n\n" +
            "Bản ghi: " + cardId + " - " + name + "\n\n" +
            "Chức năng này sẽ mở form chỉnh sửa cho phép:\n" +
            "• Cập nhật thông tin cá nhân\n" +
            "• Thay đổi trạng thái thẻ\n" +
            "• Điều chỉnh số dư\n" +
            "• Cập nhật thông tin liên hệ\n" +
            "• Thêm ghi chú quản lý\n\n" +
            "⚠️ Lưu ý: Thay đổi sẽ được ghi log và cần xác thực admin.");
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
            "Tiếp tục xóa?"
        );
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                showAlert("Đã xóa bản ghi", 
                    "✅ XÓA THÀNH CÔNG\n\n" +
                    "Bản ghi " + cardId + " đã được xóa khỏi hệ thống.\n\n" +
                    "• Thông tin cư dân: Đã xóa\n" +
                    "• Lịch sử giao dịch: Đã xóa\n" +
                    "• Thẻ cư dân: Đã vô hiệu hóa\n" +
                    "• Log hành động: Đã ghi nhận\n\n" +
                    "Thời gian xóa: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")));
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
    
    public Parent getRoot() {
        return root;
    }
}