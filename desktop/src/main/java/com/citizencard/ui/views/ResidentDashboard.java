package com.citizencard.ui.views;

import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import com.citizencard.model.Resident;
import com.citizencard.model.Invoice;
import com.citizencard.model.Transaction;
import com.citizencard.util.ModelConverter;
import com.citizencard.service.CitizenCardService;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.StageStyle;
import javafx.geometry.Pos;
import java.util.List;
import java.util.Base64;
import java.util.Optional;
import com.citizencard.ui.components.PinInputComponent;
import com.citizencard.ui.components.UITheme;

public class ResidentDashboard {
    private Stage stage;
    private Resident resident;
    private CitizenCardService service;
    private BorderPane root;
    private StackPane contentArea;
    private StackPane rootLayer;
    private String currentPage = "home";
    private boolean isRefreshingBalance = false;

    public ResidentDashboard(Stage stage, Resident resident, CitizenCardService service) {
        this.stage = stage;
        this.resident = resident;
        this.service = service;
    }

    private void navigateTo(String page) {
        currentPage = page;
        renderCurrentPage();
    }

    private void renderCurrentPage() {
        if (contentArea == null) {
            return;
        }
        switch (currentPage) {
            case "home":
                showHomePage(contentArea);
                break;
            case "balance":
                showBalancePage(contentArea);
                break;
            case "topup":
                showTopupPage(contentArea);
                break;
            case "invoices":
                showInvoicesPage(contentArea);
                break;
            case "parking":
                showParkingPage(contentArea);
                break;
            case "transactions":
                showTransactionsPage(contentArea);
                break;
            case "profile":
                showProfilePage(contentArea);
                break;
            case "picture":
                showPicturePage(contentArea);
                break;
            case "changepin":
                showChangePinPage(contentArea);
                break;
            default:
                showHomePage(contentArea);
        }
    }

    private Optional<String> showPinDialogForTransaction(String title, String message) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("🔐 Xác thực PIN");
        dialog.setHeaderText(title);
        dialog.setContentText(message);
        dialog.initStyle(StageStyle.UTILITY);
        dialog.initModality(Modality.APPLICATION_MODAL);

        PinInputComponent pinInput = new PinInputComponent();

        VBox content = new VBox(12);
        content.setPadding(new Insets(20));
        content.setAlignment(Pos.CENTER);
        content.getChildren().addAll(new Label("Mã PIN:"), pinInput);

        dialog.getDialogPane().setContent(content);

        ButtonType confirmButtonType = new ButtonType("Xác nhận", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmButtonType, ButtonType.CANCEL);
        UITheme.styleDialogPane(dialog.getDialogPane());
        Button confirmButton = (Button) dialog.getDialogPane().lookupButton(confirmButtonType);
        confirmButton.addEventFilter(ActionEvent.ACTION, event -> {
            if (!pinInput.isComplete()) {
                event.consume();
            }
        });

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == confirmButtonType) {
                return pinInput.getPin();
            }
            return null;
        });

        return dialog.showAndWait();
    }

    public void show() {
        root = new BorderPane();
        contentArea = new StackPane();
        rootLayer = new StackPane();
        rootLayer.getStyleClass().add("root-container");

        // Tạo header bar với nút refresh ở góc phải
        HBox headerBar = createHeaderBar();

        // Tạo VBox chứa header và content
        VBox centerContainer = new VBox();
        centerContainer.getChildren().addAll(headerBar, contentArea);
        VBox.setVgrow(contentArea, javafx.scene.layout.Priority.ALWAYS);

        root.setCenter(centerContainer);

        VBox sidebar = createSidebar();
        root.setLeft(sidebar);

        renderCurrentPage();

        rootLayer.getChildren().add(root);
        Scene scene = new Scene(rootLayer, 1200, 820);
        // Load global CSS
        try {
            String css = getClass().getResource("/css/styles.css").toExternalForm();
            scene.getStylesheets().add(css);
        } catch (Exception e) {
            System.err.println("❌ Error loading CSS: " + e.getMessage());
        }

        stage.setTitle("Dashboard Cư dân - " + resident.getFullName());
        stage.setScene(scene);
        stage.show();
    }

    private HBox createHeaderBar() {
        HBox headerBar = new HBox();
        headerBar.setPadding(new Insets(20, 30, 20, 30));
        headerBar.getStyleClass().add("header-bar");
        headerBar.setAlignment(Pos.CENTER_LEFT);
        headerBar.setSpacing(20);

        // Title label (sẽ được cập nhật khi chuyển trang)
        Label pageTitle = new Label("🏠 Trang chủ");
        pageTitle.getStyleClass().add("label-title");
        pageTitle.setStyle("-fx-font-size: 22px; -fx-font-weight: 700;");
        pageTitle.setId("pageTitle");

        // Spacer để đẩy nút refresh sang phải
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        // User info
        HBox userInfo = new HBox(10);
        userInfo.setAlignment(Pos.CENTER_RIGHT);
        
        Label userNameLabel = new Label(resident.getFullName());
        userNameLabel.getStyleClass().add("user-name");
        userNameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #475569;");
        
        // Nút refresh ở góc phải
        Button refreshBtn = new Button("🔄 Làm mới");
        refreshBtn.setPrefHeight(36);
        UITheme.applyPrimaryButton(refreshBtn);
        refreshBtn.setOnAction(e -> refreshData());

        userInfo.getChildren().addAll(userNameLabel, refreshBtn);
        headerBar.getChildren().addAll(pageTitle, spacer, userInfo);

        return headerBar;
    }

    private VBox createSidebar() {
        VBox sidebar = new VBox();
        sidebar.getStyleClass().add("sidebar");

        // Header với gradient
        VBox header = new VBox(8);
        header.getStyleClass().add("sidebar-header");
        header.setPadding(new Insets(30, 25, 30, 25));
        
        Label title = new Label("👤 Cư dân");
        title.getStyleClass().add("sidebar-title");
        Label subtitle = new Label("Menu chính");
        subtitle.getStyleClass().add("sidebar-subtitle");
        header.getChildren().addAll(title, subtitle);

        // Menu items
        VBox menu = new VBox(8);
        menu.getStyleClass().add("sidebar-menu");
        menu.setPadding(new Insets(20, 15, 20, 15));

        Button homeBtn = createMenuButton("🏠 Trang chủ");
        Button balanceBtn = createMenuButton("💰 Số dư");
        Button topupBtn = createMenuButton("💳 Nạp tiền");
        Button invoicesBtn = createMenuButton("📄 Hóa đơn");
        Button parkingBtn = createMenuButton("🚗 Gửi xe");
        Button transactionsBtn = createMenuButton("📊 Lịch sử giao dịch");
        Button profileBtn = createMenuButton("👤 Thông tin cá nhân");
        Button pictureBtn = createMenuButton("🖼️ Ảnh đại diện");
        Button changePinBtn = createMenuButton("🔐 Đổi mã PIN");

        homeBtn.setOnAction(e -> navigateTo("home"));
        balanceBtn.setOnAction(e -> navigateTo("balance"));
        topupBtn.setOnAction(e -> navigateTo("topup"));
        invoicesBtn.setOnAction(e -> navigateTo("invoices"));
        parkingBtn.setOnAction(e -> navigateTo("parking"));
        transactionsBtn.setOnAction(e -> navigateTo("transactions"));
        profileBtn.setOnAction(e -> navigateTo("profile"));
        pictureBtn.setOnAction(e -> navigateTo("picture"));
        changePinBtn.setOnAction(e -> navigateTo("changepin"));

        menu.getChildren().addAll(homeBtn, balanceBtn, topupBtn, invoicesBtn,
                parkingBtn, transactionsBtn, profileBtn, pictureBtn, changePinBtn);

        // Spacer
        Region spacer = new Region();
        VBox.setVgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        // Logout button
        VBox footer = new VBox();
        footer.setPadding(new Insets(15));
        Button logoutBtn = new Button("🚪 Đăng xuất");
        logoutBtn.setPrefWidth(Double.MAX_VALUE);
        logoutBtn.setPrefHeight(45);
        UITheme.applyDangerButton(logoutBtn);
        logoutBtn.setOnAction(e -> {
            LoginView loginView = new LoginView(stage, service);
            loginView.show();
        });
        footer.getChildren().add(logoutBtn);

        sidebar.getChildren().addAll(header, menu, spacer, footer);

        return sidebar;
    }

    private Button createMenuButton(String text) {
        Button btn = new Button(text);
        UITheme.applySidebarButton(btn);
        return btn;
    }

    private void refreshData() {
        try {
            Resident backendResident = service.getResident(resident.getId());
            if (backendResident != null) {
                resident = ModelConverter.toDesktopResident(backendResident);
                stage.setTitle("Dashboard Cư dân - " + resident.getFullName());

                showAlert("Thành công", "Dữ liệu đã được cập nhật từ database!", Alert.AlertType.INFORMATION);
                renderCurrentPage();
            } else {
                showAlert("Lỗi", "Không thể tải dữ liệu từ database", Alert.AlertType.ERROR);
            }
        } catch (Exception e) {
            showAlert("Lỗi", "Lỗi làm mới dữ liệu: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void updatePageTitle(String title) {
        // Tìm và cập nhật label pageTitle trong root
        Label pageTitle = (Label) root.lookup("#pageTitle");
        if (pageTitle != null) {
            pageTitle.setText(title);
        }
    }

    private void showHomePage(StackPane contentArea) {
        updatePageTitle("🏠 Trang chủ");
        VBox content = new VBox(30);
        content.setPadding(new Insets(40));
        content.getStyleClass().add("content-area");
        content.setAlignment(Pos.TOP_CENTER);

        // Stats cards row
        HBox statsRow = new HBox(20);
        statsRow.setAlignment(Pos.CENTER);

        // Quick stats
        try {
            int balance = service.getBalance(resident.getCardId());
            List<Transaction> pendingInvoices = service.getPendingInvoices(resident.getId());
            int pendingCount = pendingInvoices != null ? pendingInvoices.size() : 0;

            VBox balanceCard = createStatCard("💰 Số dư", String.format("%,d VND", balance), "#22c55e");
            VBox invoicesCard = createStatCard("📄 Hóa đơn chưa thanh toán", String.valueOf(pendingCount), "#ef4444");
            
            statsRow.getChildren().addAll(balanceCard, invoicesCard);
        } catch (Exception e) {
            System.err.println("Error loading stats: " + e.getMessage());
        }

        // Main welcome card
        VBox welcomeCard = new VBox(25);
        welcomeCard.setPadding(new Insets(40));
        welcomeCard.getStyleClass().add("card");
        welcomeCard.setMaxWidth(700);
        welcomeCard.setAlignment(Pos.CENTER);

        // Hiển thị ảnh đại diện nếu có
        if (resident.getPhotoPath() != null && !resident.getPhotoPath().isEmpty()) {
            try {
                byte[] imageBytes = Base64.getDecoder().decode(resident.getPhotoPath());
                Image image = new Image(new java.io.ByteArrayInputStream(imageBytes));
                ImageView imageView = new ImageView(image);
                imageView.setFitWidth(150);
                imageView.setFitHeight(150);
                imageView.setPreserveRatio(true);
                imageView.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 12, 0, 0, 4);");
                VBox imageBox = new VBox();
                imageBox.setAlignment(Pos.CENTER);
                imageBox.setPadding(new Insets(0, 0, 20, 0));
                imageBox.getChildren().add(imageView);
                welcomeCard.getChildren().add(imageBox);
            } catch (Exception e) {
                System.err.println("Error loading photo: " + e.getMessage());
            }
        }

        Label welcomeLabel = new Label("Chào mừng, " + resident.getFullName() + "!");
        welcomeLabel.getStyleClass().add("label-title");
        welcomeLabel.setAlignment(Pos.CENTER);

        Label roomLabel = new Label("Phòng: " + (resident.getRoomNumber() != null ? resident.getRoomNumber() : "N/A"));
        roomLabel.getStyleClass().add("label-subtitle");
        roomLabel.setAlignment(Pos.CENTER);

        welcomeCard.getChildren().addAll(welcomeLabel, roomLabel);

        content.getChildren().addAll(statsRow, welcomeCard);
        contentArea.getChildren().clear();
        contentArea.getChildren().add(content);
    }

    private VBox createStatCard(String title, String value, String color) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(25));
        card.getStyleClass().add("stat-card");
        card.setPrefWidth(280);

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #64748b;");

        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: 700; -fx-text-fill: " + color + ";");

        card.getChildren().addAll(titleLabel, valueLabel);
        return card;
    }

    private void showBalancePage(StackPane contentArea) {
        updatePageTitle("💰 Số dư tài khoản");
        if (isRefreshingBalance) {
            return;
        }

        isRefreshingBalance = true;

        VBox content = new VBox(30);
        content.setPadding(new Insets(50));
        content.getStyleClass().add("content-area");
        content.setAlignment(Pos.CENTER);

        VBox balanceCard = new VBox(20);
        balanceCard.setPadding(new Insets(50));
        balanceCard.setAlignment(Pos.CENTER);
        balanceCard.getStyleClass().add("stat-card");
        balanceCard.setMaxWidth(600);

        Label title = new Label("💰 Số dư tài khoản");
        title.getStyleClass().add("label-title");

        int balance = 0;
        try {
            balance = service.getBalance(resident.getCardId());
        } catch (Exception e) {
            System.err.println("Error getting balance: " + e.getMessage());
        } finally {
            isRefreshingBalance = false;
        }

        Label balanceLabel = new Label(String.format("%,d", balance) + " VND");
        balanceLabel.setStyle("-fx-font-size: 48px; -fx-font-weight: bold; -fx-text-fill: #16a34a;");

        balanceCard.getChildren().addAll(title, balanceLabel);
        content.getChildren().addAll(balanceCard);
        contentArea.getChildren().clear();
        contentArea.getChildren().add(content);
    }

    private void showTopupPage(StackPane contentArea) {
        updatePageTitle("💳 Nạp tiền");
        VBox content = new VBox(25);
        content.setPadding(new Insets(50));
        content.getStyleClass().add("content-area");

        Label title = new Label("💳 Nạp tiền");
        title.getStyleClass().add("label-title");

        VBox formCard = new VBox(25);
        formCard.setPadding(new Insets(40));
        formCard.getStyleClass().add("card");
        formCard.setMaxWidth(500);
        formCard.setAlignment(javafx.geometry.Pos.CENTER);

        Label amountLabel = new Label("Số tiền (VND):");

        // Tạo fields với styled versions ngay từ đầu (final)
        final TextField amountField = new TextField();
        amountField.setPromptText("Nhập số tiền");
        UITheme.styleTextField(amountField);
        amountField.setPrefWidth(400);

        final Label resultLabel = new Label();
        resultLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 500; -fx-text-fill: #475569;");

        final Button topupBtn = createPrimaryButton("💳 Nạp tiền", "#22c55e");
        topupBtn.setPrefWidth(200);

        topupBtn.setOnAction(e -> {
            try {
                int amount = Integer.parseInt(amountField.getText());
                if (amount <= 0) {
                    resultLabel.setText("Số tiền phải lớn hơn 0");
                    resultLabel.getStyleClass().setAll("label", "label-danger");
                    return;
                }

                Optional<String> pinResult = showPinDialogForTransaction(
                        "Xác thực PIN để nạp tiền",
                        "Vui lòng nhập mã PIN để xác thực giao dịch nạp tiền");

                if (!pinResult.isPresent() || pinResult.get().isEmpty()) {
                    return;
                }

                String pin = pinResult.get();
                Transaction transaction;
                try {
                    transaction = service.topUp(resident.getCardId(), amount, pin);
                } catch (Exception ex) {
                    resultLabel.setText("❌ Lỗi: " + ex.getMessage());
                    resultLabel.getStyleClass().setAll("label", "label-danger");
                    return;
                }

                resultLabel.setText("✅ Nạp tiền thành công! Số dư mới: " +
                        String.format("%,d", transaction.getBalanceAfter()) + " VND");
                resultLabel.getStyleClass().setAll("label", "label-success");
                amountField.clear();

                resident.setBalance(transaction.getBalanceAfter());
            } catch (NumberFormatException ex) {
                resultLabel.setText("Vui lòng nhập số hợp lệ");
                resultLabel.getStyleClass().setAll("label", "label-danger");
            }
        });

        VBox fieldsBox = new VBox(15);
        fieldsBox.setAlignment(javafx.geometry.Pos.CENTER);
        fieldsBox.getChildren().addAll(amountLabel, amountField, topupBtn, resultLabel);

        formCard.getChildren().addAll(fieldsBox);
        content.getChildren().addAll(title, formCard);
        contentArea.getChildren().clear();
        contentArea.getChildren().add(content);
    }

    private Button createPrimaryButton(String text, String color) {
        Button btn = new Button(text);
        btn.setPrefHeight(50);
        switch (color) {
            case "#22c55e":
            case "#2ecc71":
                UITheme.applyAccentButton(btn);
                break;
            case "#ef4444":
            case "#e74c3c":
                UITheme.applyDangerButton(btn);
                break;
            default:
                UITheme.applyPrimaryButton(btn);
                break;
        }
        return btn;
    }

    private void showInvoicesPage(StackPane contentArea) {
        updatePageTitle("📄 Hóa đơn chưa thanh toán");
        VBox content = new VBox(20);
        content.setPadding(new Insets(40));

        Label title = new Label("Hóa đơn chưa thanh toán");
        title.getStyleClass().add("label-title");

        TableView<Invoice> table = new TableView<>();
        table.setPrefHeight(400);
        UITheme.styleTable(table);

        TableColumn<Invoice, String> serviceCol = new TableColumn<>("Dịch vụ");
        serviceCol.setCellValueFactory(new PropertyValueFactory<>("serviceName"));
        serviceCol.setPrefWidth(150);

        TableColumn<Invoice, Integer> amountCol = new TableColumn<>("Số tiền");
        amountCol.setCellValueFactory(new PropertyValueFactory<>("amount"));
        amountCol.setPrefWidth(150);
        amountCol.setCellFactory(column -> new TableCell<Invoice, Integer>() {
            @Override
            protected void updateItem(Integer amount, boolean empty) {
                super.updateItem(amount, empty);
                if (empty || amount == null) {
                    setText(null);
                } else {
                    setText(String.format("%,d VND", amount));
                }
            }
        });

        TableColumn<Invoice, String> dateCol = new TableColumn<>("Ngày tạo");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("invoiceDate"));
        dateCol.setPrefWidth(150);

        TableColumn<Invoice, String> statusCol = new TableColumn<>("Trạng thái");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("paymentStatus"));
        statusCol.setPrefWidth(120);

        TableColumn<Invoice, Void> actionCol = new TableColumn<>("Thao tác");
        actionCol.setPrefWidth(150);
        actionCol.setCellFactory(column -> new TableCell<Invoice, Void>() {
            private final Button payBtn = new Button("Thanh toán");

            {
                UITheme.applyPrimaryButton(payBtn);
                payBtn.setPrefWidth(130);
                payBtn.setOnAction(e -> {
                    Invoice invoice = getTableView().getItems().get(getIndex());
                    payInvoice(invoice);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Invoice invoice = getTableView().getItems().get(getIndex());
                    if ("PENDING".equals(invoice.getPaymentStatus())) {
                        payBtn.setDisable(false);
                        setGraphic(payBtn);
                    } else {
                        payBtn.setDisable(true);
                        setGraphic(payBtn);
                    }
                }
            }
        });

        table.getColumns().setAll(List.of(serviceCol, amountCol, dateCol, statusCol, actionCol));

        // ✅ Load invoices - gọi trực tiếp service (giờ là Transaction với type=INVOICE)
        loadPendingInvoices(table);

        Button refreshBtn = new Button("Làm mới");
        UITheme.applyPrimaryButton(refreshBtn);
        refreshBtn.setOnAction(e -> loadPendingInvoices(table));

        content.getChildren().addAll(title, table, refreshBtn);
        contentArea.getChildren().clear();
        contentArea.getChildren().add(content);
    }

    private void loadPendingInvoices(TableView<Invoice> table) {
        try {
            table.getItems().clear();
            if (resident == null || resident.getId() == null) {
                showAlert("Lỗi", "Không thể tải dữ liệu: Thông tin cư dân không hợp lệ", Alert.AlertType.ERROR);
                return;
            }
            List<Transaction> backendInvoices = service.getPendingInvoices(resident.getId());
            if (backendInvoices == null) {
                return;
            }
            List<Invoice> desktopInvoices = ModelConverter.transactionsToDesktopInvoices(backendInvoices);
            if (desktopInvoices != null && !desktopInvoices.isEmpty()) {
                table.getItems().addAll(desktopInvoices);
            }
        } catch (Exception e) {
            System.err.println("Error loading invoices: " + e.getMessage());
            showAlert("Lỗi", "Không thể tải danh sách hóa đơn: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void payInvoice(Invoice invoice) {
        Optional<String> pinResult = showPinDialogForTransaction(
                "Xác thực PIN để thanh toán",
                "Vui lòng nhập mã PIN để xác thực thanh toán hóa đơn");

        if (!pinResult.isPresent() || pinResult.get().isEmpty()) {
            return;
        }

        String pin = pinResult.get();
        try {
            service.payInvoice(resident.getCardId(), invoice.getId(), pin);
            showAlert("Thành công", "Thanh toán hóa đơn thành công!", Alert.AlertType.INFORMATION);
            renderCurrentPage();
        } catch (Exception e) {
            showAlert("Lỗi", "Lỗi thanh toán: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void showParkingPage(StackPane contentArea) {
        updatePageTitle("🚗 Gửi xe");
        VBox content = new VBox(25);
        content.setPadding(new Insets(40));
        content.getStyleClass().add("content-area");

        Label title = new Label("🚗 Gửi xe");
        title.getStyleClass().add("label-title");

        VBox formCard = new VBox(20);
        formCard.setPadding(new Insets(40));
        formCard.getStyleClass().add("card");
        formCard.setMaxWidth(500);
        formCard.setAlignment(Pos.CENTER);

        Label licenseLabel = new Label("Biển số xe:");
        licenseLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #475569;");
        
        TextField licensePlateField = new TextField();
        licensePlateField.setPromptText("Nhập biển số xe");
        licensePlateField.setPrefWidth(400);
        UITheme.styleTextField(licensePlateField);

        Label vehicleLabel = new Label("Loại xe:");
        vehicleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #475569;");

        ComboBox<String> vehicleTypeCombo = new ComboBox<>();
        vehicleTypeCombo.getItems().addAll("MOTORBIKE", "CAR", "BICYCLE");
        vehicleTypeCombo.setValue("MOTORBIKE");
        vehicleTypeCombo.setPrefWidth(400);
        UITheme.styleComboBox(vehicleTypeCombo);

        Label resultLabel = new Label();
        resultLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #475569;");

        Button registerBtn = new Button("🚗 Đăng ký gửi xe");
        registerBtn.setPrefWidth(200);
        UITheme.applyPrimaryButton(registerBtn);

        registerBtn.setOnAction(e -> {
            try {
                if (licensePlateField.getText().trim().isEmpty()) {
                    resultLabel.setText("❌ Vui lòng nhập biển số xe");
                    resultLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 14px;");
                    return;
                }
                
                // ✅ Gọi trực tiếp service để đăng ký gửi xe
                service.registerParking(
                        resident.getId(),
                        licensePlateField.getText(),
                        vehicleTypeCombo.getValue());

                resultLabel.setText("✅ Đăng ký gửi xe thành công!");
                resultLabel.setStyle("-fx-text-fill: #16a34a; -fx-font-size: 14px;");
                licensePlateField.clear();
                showAlert("Thành công", "Đăng ký gửi xe thành công!", Alert.AlertType.INFORMATION);
            } catch (Exception ex) {
                resultLabel.setText("❌ Lỗi: " + ex.getMessage());
                resultLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 14px;");
                showAlert("Lỗi", "Lỗi đăng ký: " + ex.getMessage(), Alert.AlertType.ERROR);
            }
        });

        VBox fieldsBox = new VBox(15);
        fieldsBox.setAlignment(Pos.CENTER);
        fieldsBox.getChildren().addAll(licenseLabel, licensePlateField, vehicleLabel, vehicleTypeCombo, registerBtn, resultLabel);

        formCard.getChildren().addAll(fieldsBox);
        content.getChildren().addAll(title, formCard);
        contentArea.getChildren().clear();
        contentArea.getChildren().add(content);
    }

    private void showTransactionsPage(StackPane contentArea) {
        updatePageTitle("📊 Lịch sử giao dịch");
        VBox content = new VBox(20);
        content.setPadding(new Insets(40));

        Label title = new Label("Lịch sử giao dịch");
        title.getStyleClass().add("label-title");

        TableView<Transaction> table = new TableView<>();
        table.setPrefHeight(500);
        UITheme.styleTable(table);

        TableColumn<Transaction, String> typeCol = new TableColumn<>("Loại");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("transactionType"));
        typeCol.setPrefWidth(100);
        typeCol.setCellFactory(column -> new TableCell<Transaction, String>() {
            @Override
            protected void updateItem(String type, boolean empty) {
                super.updateItem(type, empty);
                if (empty || type == null) {
                    setText(null);
                } else {
                    switch (type) {
                        case "TOPUP":
                            setText("Nạp tiền");
                            break;
                        case "PAYMENT":
                            setText("Thanh toán");
                            break;
                        case "DEBIT":
                            setText("Trừ tiền");
                            break;
                        default:
                            setText(type);
                    }
                }
            }
        });

        TableColumn<Transaction, Integer> amountCol = new TableColumn<>("Số tiền");
        amountCol.setCellValueFactory(new PropertyValueFactory<>("amount"));
        amountCol.setPrefWidth(150);
        amountCol.setCellFactory(column -> new TableCell<Transaction, Integer>() {
            @Override
            protected void updateItem(Integer amount, boolean empty) {
                super.updateItem(amount, empty);
                if (empty || amount == null) {
                    setText(null);
                } else {
                    setText(String.format("%,d VND", amount));
                }
            }
        });

        TableColumn<Transaction, Integer> balanceBeforeCol = new TableColumn<>("Số dư trước");
        balanceBeforeCol.setCellValueFactory(new PropertyValueFactory<>("balanceBefore"));
        balanceBeforeCol.setPrefWidth(150);
        balanceBeforeCol.setCellFactory(column -> new TableCell<Transaction, Integer>() {
            @Override
            protected void updateItem(Integer balance, boolean empty) {
                super.updateItem(balance, empty);
                if (empty || balance == null) {
                    setText(null);
                } else {
                    setText(String.format("%,d VND", balance));
                }
            }
        });

        TableColumn<Transaction, Integer> balanceAfterCol = new TableColumn<>("Số dư sau");
        balanceAfterCol.setCellValueFactory(new PropertyValueFactory<>("balanceAfter"));
        balanceAfterCol.setPrefWidth(150);
        balanceAfterCol.setCellFactory(column -> new TableCell<Transaction, Integer>() {
            @Override
            protected void updateItem(Integer balance, boolean empty) {
                super.updateItem(balance, empty);
                if (empty || balance == null) {
                    setText(null);
                } else {
                    setText(String.format("%,d VND", balance));
                }
            }
        });

        TableColumn<Transaction, String> descriptionCol = new TableColumn<>("Mô tả");
        descriptionCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        descriptionCol.setPrefWidth(250);
        descriptionCol.setCellFactory(column -> new TableCell<Transaction, String>() {
            @Override
            protected void updateItem(String description, boolean empty) {
                super.updateItem(description, empty);
                setText(empty ? null : description);
            }
        });

        TableColumn<Transaction, String> timestampCol = new TableColumn<>("Thời gian");
        timestampCol.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        timestampCol.setPrefWidth(180);

        table.getColumns()
                .setAll(List.of(typeCol, amountCol, balanceBeforeCol, balanceAfterCol, descriptionCol, timestampCol));

        loadTransactions(table);

        Button refreshBtn = new Button("Làm mới");
        UITheme.applyPrimaryButton(refreshBtn);
        refreshBtn.setOnAction(e -> loadTransactions(table));

        content.getChildren().addAll(title, table, refreshBtn);
        contentArea.getChildren().clear();
        contentArea.getChildren().add(content);
    }

    private void loadTransactions(TableView<Transaction> table) {
        try {
            table.getItems().clear();
            if (resident == null || resident.getCardId() == null || resident.getCardId().isEmpty()) {
                showAlert("Lỗi", "Không thể tải dữ liệu: Card ID không hợp lệ", Alert.AlertType.ERROR);
                return;
            }
            List<Transaction> backendTransactions = service.getTransactionHistory(resident.getCardId());
            if (backendTransactions == null) {
                return;
            }
            List<Transaction> desktopTransactions = ModelConverter.toDesktopTransactions(backendTransactions);
            if (desktopTransactions != null && !desktopTransactions.isEmpty()) {
                table.getItems().addAll(desktopTransactions);
            }
        } catch (Exception e) {
            System.err.println("Error loading transactions: " + e.getMessage());
            showAlert("Lỗi", "Không thể tải lịch sử giao dịch: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void showProfilePage(StackPane contentArea) {
        updatePageTitle("👤 Thông tin cá nhân");
        VBox content = new VBox(25);
        content.setPadding(new Insets(40));
        content.getStyleClass().add("content-area");

        Label title = new Label("👤 Thông tin cá nhân");
        title.getStyleClass().add("label-title");

        // Form card
        VBox formCard = new VBox(25);
        formCard.setPadding(new Insets(40));
        formCard.getStyleClass().add("card");
        formCard.setMaxWidth(600);

        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(20);
        grid.setPadding(new Insets(20));

        // Thông tin cá nhân
        Label infoTitle = new Label("📝 Thông tin cá nhân");
        infoTitle.getStyleClass().add("label-subtitle");
        grid.add(infoTitle, 0, 0, 2, 1);

        TextField nameField = new TextField(resident.getFullName());
        UITheme.styleTextField(nameField);
        TextField dobField = new TextField(resident.getDateOfBirth());
        UITheme.styleTextField(dobField);
        TextField roomField = new TextField(resident.getRoomNumber());
        UITheme.styleTextField(roomField);
        TextField phoneField = new TextField();
        phoneField.setPromptText("Số điện thoại");
        UITheme.styleTextField(phoneField);
        TextField emailField = new TextField();
        emailField.setPromptText("Email");
        UITheme.styleTextField(emailField);
        TextField idNumberField = new TextField();
        idNumberField.setPromptText("CMND/CCCD");
        UITheme.styleTextField(idNumberField);

        phoneField.setText(resident.getPhoneNumber() != null ? resident.getPhoneNumber() : "");
        emailField.setText(resident.getEmail() != null ? resident.getEmail() : "");
        idNumberField.setText(resident.getIdNumber() != null ? resident.getIdNumber() : "");

        grid.add(new Label("Họ tên:"), 0, 1);
        grid.add(nameField, 1, 1);
        grid.add(new Label("Ngày sinh:"), 0, 2);
        grid.add(dobField, 1, 2);
        grid.add(new Label("Số phòng:"), 0, 3);
        grid.add(roomField, 1, 3);
        grid.add(new Label("Số điện thoại:"), 0, 4);
        grid.add(phoneField, 1, 4);
        grid.add(new Label("Email:"), 0, 5);
        grid.add(emailField, 1, 5);
        grid.add(new Label("CMND/CCCD:"), 0, 6);
        grid.add(idNumberField, 1, 6);

        final Label resultLabel = new Label();
        resultLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #475569;");

        Button saveBtn = createPrimaryButton("💾 Lưu thông tin", "#0ea5e9");

        HBox buttonBox = new HBox(15);
        buttonBox.getChildren().addAll(saveBtn);

        saveBtn.setOnAction(e -> {
            Optional<String> pinResult = showPinDialogForTransaction(
                    "Xác thực PIN để cập nhật thông tin",
                    "Vui lòng nhập mã PIN để xác thực cập nhật thông tin");

            if (!pinResult.isPresent() || pinResult.get().isEmpty()) {
                return;
            }

            String pin = pinResult.get();

            resident.setFullName(nameField.getText());
            resident.setDateOfBirth(dobField.getText());
            resident.setRoomNumber(roomField.getText());
            resident.setPhoneNumber(phoneField.getText());
            resident.setEmail(emailField.getText());
            resident.setIdNumber(idNumberField.getText());

            try {
                Resident backendResident = ModelConverter.toBackendResident(resident);
                Resident updated = service.updateResidentInfo(
                        resident.getId(),
                        backendResident.getFullName(),
                        backendResident.getDateOfBirth(),
                        backendResident.getRoomNumber(),
                        backendResident.getPhoneNumber(),
                        backendResident.getEmail(),
                        backendResident.getIdNumber(),
                        pin);

                resident = ModelConverter.toDesktopResident(updated);

                resultLabel.setText("✅ Cập nhật thông tin thành công!");
                resultLabel.setStyle("-fx-text-fill: #16a34a; -fx-font-size: 14px;");
            } catch (Exception ex) {
                resultLabel.setText("❌ Lỗi: " + ex.getMessage());
                resultLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 14px;");
            }
        });

        grid.add(resultLabel, 0, 8, 2, 1);

        formCard.getChildren().addAll(grid, buttonBox);
        content.getChildren().addAll(title, formCard);
        contentArea.getChildren().clear();
        contentArea.getChildren().add(content);
    }

    private void showChangePinPage(StackPane contentArea) {
        updatePageTitle("🔐 Đổi mã PIN");
        VBox content = new VBox(25);
        content.setPadding(new Insets(50));
        content.getStyleClass().add("content-area");

        Label title = new Label("🔐 Đổi mã PIN");
        title.getStyleClass().add("label-title");

        VBox formCard = new VBox(25);
        formCard.setPadding(new Insets(40));
        formCard.getStyleClass().add("card");
        formCard.setMaxWidth(500);
        formCard.setAlignment(javafx.geometry.Pos.CENTER);

        Label oldPinLabel = new Label("Mã PIN cũ:");
        oldPinLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #475569;");

        final PinInputComponent oldPinField = new PinInputComponent();

        Label newPinLabel = new Label("Mã PIN mới:");
        newPinLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #475569;");

        final PinInputComponent newPinField = new PinInputComponent();

        Label confirmPinLabel = new Label("Xác nhận mã PIN mới:");
        confirmPinLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #475569;");

        final PinInputComponent confirmPinField = new PinInputComponent();

        final Label resultLabel = new Label();
        resultLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 500; -fx-text-fill: #475569;");

        final Button changePinBtn = createPrimaryButton("🔐 Đổi mã PIN", "#0ea5e9");
        changePinBtn.setPrefWidth(200);

        changePinBtn.setOnAction(e -> {
            String oldPin = oldPinField.getPin();
            String newPin = newPinField.getPin();
            String confirmPin = confirmPinField.getPin();

            // Validate inputs
            if (oldPin.isEmpty() || newPin.isEmpty() || confirmPin.isEmpty()) {
                resultLabel.setText("❌ Vui lòng điền đầy đủ thông tin");
                resultLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 14px;");
                return;
            }

            if (!newPin.equals(confirmPin)) {
                resultLabel.setText("❌ Mã PIN mới không khớp");
                resultLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 14px;");
                return;
            }

            if (oldPin.equals(newPin)) {
                resultLabel.setText("❌ Mã PIN mới phải khác mã PIN cũ");
                resultLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 14px;");
                return;
            }

            if (newPin.length() < 4 || newPin.length() > 8) {
                resultLabel.setText("❌ Mã PIN phải có độ dài từ 4-8 ký tự");
                resultLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 14px;");
                return;
            }

            try {
                boolean success = service.changePin(resident.getCardId(), oldPin, newPin);
                if (success) {
                    resultLabel.setText("✅ Đổi mã PIN thành công!");
                    resultLabel.setStyle("-fx-text-fill: #16a34a; -fx-font-size: 14px;");
                    oldPinField.clear();
                    newPinField.clear();
                    confirmPinField.clear();
                    showAlert("Thành công", "Mã PIN đã được thay đổi thành công!", Alert.AlertType.INFORMATION);
                } else {
                    resultLabel.setText("❌ Không thể đổi mã PIN");
                    resultLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 14px;");
                }
            } catch (Exception ex) {
                resultLabel.setText("❌ Lỗi: " + ex.getMessage());
                resultLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 14px;");
            }
        });

        VBox fieldsBox = new VBox(15);
        fieldsBox.setAlignment(javafx.geometry.Pos.CENTER);
        fieldsBox.getChildren().addAll(
                oldPinLabel, oldPinField,
                newPinLabel, newPinField,
                confirmPinLabel, confirmPinField,
                changePinBtn, resultLabel);

        formCard.getChildren().addAll(fieldsBox);
        content.getChildren().addAll(title, formCard);
        contentArea.getChildren().clear();
        contentArea.getChildren().add(content);
    }

    private void showPicturePage(StackPane contentArea) {
        VBox content = new VBox(20);
        content.setPadding(new Insets(40));

        Label title = new Label("Ảnh đại diện");
        title.getStyleClass().add("label-title");

        ImageView imageView = new ImageView();
        imageView.setFitWidth(300);
        imageView.setFitHeight(300);
        imageView.setPreserveRatio(true);

        // ✅ Load ảnh hiện tại - gọi trực tiếp service
        try {
            String pictureBase64 = service.getPicture(resident.getCardId());
            if (pictureBase64 != null && !pictureBase64.isEmpty()) {
                // Nếu là base64 string, decode nó
                byte[] pictureBytes = Base64.getDecoder().decode(pictureBase64);
                Image image = new Image(new java.io.ByteArrayInputStream(pictureBytes));
                imageView.setImage(image);
            }
        } catch (Exception e) {
            System.err.println("Error loading picture: " + e.getMessage());
        }

        Button uploadBtn = new Button("Tải ảnh lên");
        UITheme.applyPrimaryButton(uploadBtn);

        uploadBtn.setOnAction(e -> {
            javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
            chooser.getExtensionFilters().add(
                    new javafx.stage.FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
            java.io.File file = chooser.showOpenDialog(stage);
            if (file != null) {
                try {
                    byte[] fileBytes = java.nio.file.Files.readAllBytes(file.toPath());

                    // ✅ Gọi trực tiếp service để cập nhật ảnh
                    boolean success = service.updatePicture(resident.getCardId(), fileBytes);

                    if (success) {
                        Image image = new Image(file.toURI().toString());
                        imageView.setImage(image);
                        showAlert("Thành công", "Cập nhật ảnh thành công!", Alert.AlertType.INFORMATION);
                    } else {
                        showAlert("Lỗi", "Không thể cập nhật ảnh", Alert.AlertType.ERROR);
                    }
                } catch (Exception ex) {
                    showAlert("Lỗi", "Lỗi cập nhật ảnh: " + ex.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });

        content.getChildren().addAll(title, imageView, uploadBtn);
        contentArea.getChildren().clear();
        contentArea.getChildren().add(content);
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        UITheme.styleDialogPane(alert.getDialogPane());
        alert.showAndWait();
    }

}
