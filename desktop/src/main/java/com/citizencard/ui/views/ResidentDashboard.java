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
        rootLayer.setStyle("-fx-background-color: linear-gradient(to bottom, #020617, #0f172a 45%, #111827 100%); "
                + "-fx-padding: 25;");

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
        stage.setTitle("Dashboard Cư dân - " + resident.getFullName());
        stage.setScene(scene);
        stage.show();
    }

    private HBox createHeaderBar() {
        HBox headerBar = new HBox();
        headerBar.setPadding(new Insets(20, 30, 20, 30));
        headerBar.setStyle("-fx-background-color: linear-gradient(to right, #ffffff, #f8f9fa); " +
                "-fx-border-color: #e0e0e0; " +
                "-fx-border-width: 0 0 2 0; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(102,126,234,0.1), 10, 0, 0, 3);");
        headerBar.setAlignment(Pos.CENTER_LEFT);
        headerBar.setSpacing(20);

        // Title label (sẽ được cập nhật khi chuyển trang)
        Label pageTitle = new Label("🏠 Trang chủ");
        pageTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; " +
                "-fx-text-fill: linear-gradient(to right, #667eea, #764ba2); " +
                "-fx-text-fill: #2c3e50;");
        pageTitle.setId("pageTitle"); // ID để có thể update sau

        // Spacer để đẩy nút refresh sang phải
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        // Nút refresh ở góc phải
        Button refreshBtn = new Button("🔄 Làm mới");
        UITheme.applyPrimaryButton(refreshBtn);
        refreshBtn.setOnAction(e -> refreshData());

        headerBar.getChildren().addAll(pageTitle, spacer, refreshBtn);

        return headerBar;
    }

    private VBox createSidebar() {
        VBox sidebar = new VBox(8);
        sidebar.setPadding(new Insets(25));
        sidebar.setStyle("-fx-background-color: linear-gradient(to bottom right, #667eea 0%, #764ba2 50%, #f093fb 100%); " +
                "-fx-min-width: 260px; " +
                "-fx-border-color: rgba(255,255,255,0.3); " +
                "-fx-border-width: 0 2 0 0; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(102,126,234,0.4), 15, 0, 0, 5);");

        VBox header = new VBox(5);
        header.setPadding(new Insets(0, 0, 20, 0));
        Label title = new Label("👤 Cư dân");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 26px; -fx-font-weight: bold; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 8, 0, 0, 2);");
        Label subtitle = new Label("Menu chính");
        subtitle.setStyle("-fx-text-fill: rgba(255,255,255,0.95); -fx-font-size: 14px; -fx-font-weight: 500;");
        header.getChildren().addAll(title, subtitle);

        Separator separator = new Separator();
        separator.setStyle("-fx-background-color: rgba(255,255,255,0.3);");

        Button homeBtn = createMenuButton("🏠 Trang chủ");
        Button balanceBtn = createMenuButton("💰 Số dư");
        Button topupBtn = createMenuButton("💳 Nạp tiền");
        Button invoicesBtn = createMenuButton("📄 Hóa đơn");
        Button parkingBtn = createMenuButton("🚗 Gửi xe");
        Button transactionsBtn = createMenuButton("📊 Lịch sử giao dịch");
        Button profileBtn = createMenuButton("👤 Thông tin cá nhân");
        Button pictureBtn = createMenuButton("🖼️ Ảnh đại diện");
        Button changePinBtn = createMenuButton("🔐 Đổi mã PIN");
        Button refreshBtn = createMenuButton("🔄 Làm mới dữ liệu");

        Button logoutBtn = new Button("🚪 Đăng xuất");
        UITheme.applyDangerButton(logoutBtn);

        homeBtn.setOnAction(e -> navigateTo("home"));
        balanceBtn.setOnAction(e -> navigateTo("balance"));
        topupBtn.setOnAction(e -> navigateTo("topup"));
        invoicesBtn.setOnAction(e -> navigateTo("invoices"));
        parkingBtn.setOnAction(e -> navigateTo("parking"));
        transactionsBtn.setOnAction(e -> navigateTo("transactions"));
        profileBtn.setOnAction(e -> navigateTo("profile"));
        pictureBtn.setOnAction(e -> navigateTo("picture"));
        changePinBtn.setOnAction(e -> navigateTo("changepin"));
        refreshBtn.setOnAction(e -> refreshData());
        logoutBtn.setOnAction(e -> {
            LoginView loginView = new LoginView(stage, service);
            loginView.show();
        });

        VBox spacer = new VBox();
        VBox.setVgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        sidebar.getChildren().addAll(header, separator, homeBtn, balanceBtn, topupBtn, invoicesBtn,
                parkingBtn, transactionsBtn, profileBtn, pictureBtn, changePinBtn, refreshBtn, spacer, logoutBtn);

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
        VBox content = new VBox(25);
        content.setPadding(new Insets(50));
        content.setStyle("-fx-background-color: linear-gradient(to bottom, #f8f9fa, #ffffff);");

        VBox welcomeCard = new VBox(15);
        welcomeCard.setPadding(new Insets(40));
        welcomeCard.setStyle("-fx-background-color: linear-gradient(to right, #667eea 0%, #764ba2 100%); " +
                "-fx-background-radius: 20; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(102,126,234,0.4), 20, 0, 0, 5);");

        Label welcomeLabel = new Label("Chào mừng, " + resident.getFullName() + "!");
        welcomeLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: white; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 10, 0, 0, 2);");

        Label roomLabel = new Label("Phòng: " + resident.getRoomNumber());
        roomLabel.setStyle("-fx-font-size: 20px; -fx-text-fill: rgba(255,255,255,0.95); -fx-font-weight: 500;");
        
        welcomeCard.getChildren().addAll(welcomeLabel, roomLabel);

        // Hiển thị ảnh đại diện nếu có
        if (resident.getPhotoPath() != null && !resident.getPhotoPath().isEmpty()) {
            try {
                byte[] imageBytes = Base64.getDecoder().decode(resident.getPhotoPath());
                Image image = new Image(new java.io.ByteArrayInputStream(imageBytes));
                ImageView imageView = new ImageView(image);
                imageView.setFitWidth(220);
                imageView.setFitHeight(220);
                imageView.setPreserveRatio(true);
                imageView.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 15, 0, 0, 5);");
                VBox imageBox = new VBox();
                imageBox.setAlignment(Pos.CENTER);
                imageBox.getChildren().add(imageView);
                content.getChildren().add(imageBox);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        content.getChildren().addAll(welcomeCard);
        contentArea.getChildren().clear();
        contentArea.getChildren().add(content);
    }

    private void showBalancePage(StackPane contentArea) {
        updatePageTitle("💰 Số dư tài khoản");
        if (isRefreshingBalance) {
            return;
        }

        isRefreshingBalance = true;

        VBox content = new VBox(30);
        content.setPadding(new Insets(50));
        content.setStyle("-fx-background-color: linear-gradient(to bottom, #f8f9fa, #ffffff);");
        content.setAlignment(Pos.CENTER);

        VBox balanceCard = new VBox(20);
        balanceCard.setPadding(new Insets(50));
        balanceCard.setAlignment(Pos.CENTER);
        balanceCard.setStyle("-fx-background-color: linear-gradient(to right, #2ecc71, #27ae60, #16a085); " +
                "-fx-background-radius: 25; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(46,204,113,0.4), 25, 0, 0, 8);");
        balanceCard.setMaxWidth(600);

        Label title = new Label("💰 Số dư tài khoản");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: white;");

        int balance = 0;
        try {
            balance = service.getBalance(resident.getCardId());
        } catch (Exception e) {
            System.err.println("Error getting balance: " + e.getMessage());
        } finally {
            isRefreshingBalance = false;
        }

        Label balanceLabel = new Label(String.format("%,d", balance) + " VND");
        balanceLabel.setStyle("-fx-font-size: 48px; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 15, 0, 0, 3);");

        balanceCard.getChildren().addAll(title, balanceLabel);
        content.getChildren().addAll(balanceCard);
        contentArea.getChildren().clear();
        contentArea.getChildren().add(content);
    }

    private void showTopupPage(StackPane contentArea) {
        updatePageTitle("💳 Nạp tiền");
        VBox content = new VBox(25);
        content.setPadding(new Insets(50));
        content.setStyle("-fx-background-color: #f8f9fa;");

        Label title = new Label("💳 Nạp tiền");
        title.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        VBox formCard = new VBox(25);
        formCard.setPadding(new Insets(40));
        formCard.setStyle("-fx-background-color: white; -fx-background-radius: 15; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 15, 0, 0, 5);");
        formCard.setMaxWidth(500);
        formCard.setAlignment(javafx.geometry.Pos.CENTER);

        Label amountLabel = new Label("Số tiền (VND):");
        amountLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #34495e;");

        // Tạo fields với styled versions ngay từ đầu (final)
        final TextField amountField = createStyledTextField("Nhập số tiền");
        amountField.setPrefWidth(400);

        final Label resultLabel = new Label();
        resultLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 500;");

        final Button topupBtn = createPrimaryButton("💳 Nạp tiền", "#2ecc71");
        topupBtn.setPrefWidth(200);

        topupBtn.setOnAction(e -> {
            try {
                int amount = Integer.parseInt(amountField.getText());
                if (amount <= 0) {
                    resultLabel.setText("Số tiền phải lớn hơn 0");
                    resultLabel.setStyle("-fx-text-fill: red;");
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
                    resultLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 14px;");
                    return;
                }

                resultLabel.setText("✅ Nạp tiền thành công! Số dư mới: " +
                        String.format("%,d", transaction.getBalanceAfter()) + " VND");
                resultLabel.setStyle("-fx-text-fill: #2ecc71; -fx-font-size: 14px;");
                amountField.clear();

                resident.setBalance(transaction.getBalanceAfter());
            } catch (NumberFormatException ex) {
                resultLabel.setText("Vui lòng nhập số hợp lệ");
                resultLabel.setStyle("-fx-text-fill: red;");
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

    private TextField createStyledTextField(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setPrefHeight(40);
        field.setPrefWidth(300);
        field.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #dee2e6; " +
                "-fx-border-radius: 8; -fx-background-radius: 8; " +
                "-fx-border-width: 1.5; -fx-padding: 10; " +
                "-fx-font-size: 14px;");
        field.setOnMouseEntered(e -> field.setStyle(
                "-fx-background-color: white; -fx-border-color: #3498db; " +
                        "-fx-border-radius: 8; -fx-background-radius: 8; " +
                        "-fx-border-width: 2; -fx-padding: 10; " +
                        "-fx-font-size: 14px;"));
        field.setOnMouseExited(e -> field.setStyle(
                "-fx-background-color: #f8f9fa; -fx-border-color: #dee2e6; " +
                        "-fx-border-radius: 8; -fx-background-radius: 8; " +
                        "-fx-border-width: 1.5; -fx-padding: 10; " +
                        "-fx-font-size: 14px;"));
        return field;
    }

    private Button createPrimaryButton(String text, String color) {
        Button btn = new Button(text);
        btn.setPrefHeight(50);
        switch (color) {
            case "#2ecc71":
                UITheme.applyAccentButton(btn);
                break;
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
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        TableView<Invoice> table = new TableView<>();
        table.setPrefHeight(400);
        UITheme.applyTableStyle(table);

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
            List<Transaction> backendInvoices = service.getPendingInvoices(resident.getId());
            List<Invoice> desktopInvoices = ModelConverter.transactionsToDesktopInvoices(backendInvoices);
            if (desktopInvoices != null) {
                table.getItems().addAll(desktopInvoices);
            }
        } catch (Exception e) {
            System.err.println("Error loading invoices: " + e.getMessage());
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
        VBox content = new VBox(20);
        content.setPadding(new Insets(40));

        Label title = new Label("Gửi xe");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        TextField licensePlateField = new TextField();
        licensePlateField.setPromptText("Biển số xe");

        ComboBox<String> vehicleTypeCombo = new ComboBox<>();
        vehicleTypeCombo.getItems().addAll("MOTORBIKE", "CAR", "BICYCLE");
        vehicleTypeCombo.setValue("MOTORBIKE");

        Button registerBtn = new Button("Đăng ký");
        UITheme.applyPrimaryButton(registerBtn);

        registerBtn.setOnAction(e -> {
            try {
                // ✅ Gọi trực tiếp service để đăng ký gửi xe
                service.registerParking(
                        resident.getId(),
                        licensePlateField.getText(),
                        vehicleTypeCombo.getValue());

                showAlert("Thành công", "Đăng ký gửi xe thành công!", Alert.AlertType.INFORMATION);
            } catch (Exception ex) {
                showAlert("Lỗi", "Lỗi đăng ký: " + ex.getMessage(), Alert.AlertType.ERROR);
            }
        });

        content.getChildren().addAll(title, licensePlateField, vehicleTypeCombo, registerBtn);
        contentArea.getChildren().clear();
        contentArea.getChildren().add(content);
    }

    private void showTransactionsPage(StackPane contentArea) {
        updatePageTitle("📊 Lịch sử giao dịch");
        VBox content = new VBox(20);
        content.setPadding(new Insets(40));

        Label title = new Label("Lịch sử giao dịch");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        TableView<Transaction> table = new TableView<>();
        table.setPrefHeight(500);
        UITheme.applyTableStyle(table);

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

        table.getColumns().setAll(List.of(typeCol, amountCol, balanceBeforeCol, balanceAfterCol, descriptionCol, timestampCol));

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
            List<Transaction> backendTransactions = service.getTransactionHistory(resident.getCardId());
            List<Transaction> desktopTransactions = ModelConverter.toDesktopTransactions(backendTransactions);
            if (desktopTransactions != null) {
                table.getItems().addAll(desktopTransactions);
            }
        } catch (Exception e) {
            System.err.println("Error loading transactions: " + e.getMessage());
        }
    }

    private void showProfilePage(StackPane contentArea) {
        updatePageTitle("👤 Thông tin cá nhân");
        VBox content = new VBox(25);
        content.setPadding(new Insets(40));
        content.setStyle("-fx-background-color: #f8f9fa;");

        Label title = new Label("👤 Thông tin cá nhân");
        title.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        // Form card
        VBox formCard = new VBox(25);
        formCard.setPadding(new Insets(40));
        formCard.setStyle("-fx-background-color: white; -fx-background-radius: 15; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 15, 0, 0, 5);");
        formCard.setMaxWidth(600);

        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(20);
        grid.setPadding(new Insets(20));

        // Thông tin cá nhân
        Label infoTitle = new Label("📝 Thông tin cá nhân");
        infoTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #34495e;");
        grid.add(infoTitle, 0, 0, 2, 1);

        TextField nameField = createStyledTextField("Họ tên");
        nameField.setText(resident.getFullName());
        TextField dobField = createStyledTextField("Ngày sinh");
        dobField.setText(resident.getDateOfBirth());
        TextField roomField = createStyledTextField("Số phòng");
        roomField.setText(resident.getRoomNumber());
        TextField phoneField = createStyledTextField("Số điện thoại");
        TextField emailField = createStyledTextField("Email");
        TextField idNumberField = createStyledTextField("CMND/CCCD");

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
        resultLabel.setStyle("-fx-font-size: 14px;");

        Button saveBtn = createPrimaryButton("💾 Lưu thông tin", "#3498db");

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
                resultLabel.setStyle("-fx-text-fill: #2ecc71; -fx-font-size: 14px;");
            } catch (Exception ex) {
                resultLabel.setText("❌ Lỗi: " + ex.getMessage());
                resultLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 14px;");
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
        content.setStyle("-fx-background-color: #f8f9fa;");

        Label title = new Label("🔐 Đổi mã PIN");
        title.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        VBox formCard = new VBox(25);
        formCard.setPadding(new Insets(40));
        formCard.setStyle("-fx-background-color: white; -fx-background-radius: 15; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 15, 0, 0, 5);");
        formCard.setMaxWidth(500);
        formCard.setAlignment(javafx.geometry.Pos.CENTER);

        Label oldPinLabel = new Label("Mã PIN cũ:");
        oldPinLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #34495e;");

        final PinInputComponent oldPinField = new PinInputComponent();

        Label newPinLabel = new Label("Mã PIN mới:");
        newPinLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #34495e;");

        final PinInputComponent newPinField = new PinInputComponent();

        Label confirmPinLabel = new Label("Xác nhận mã PIN mới:");
        confirmPinLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #34495e;");

        final PinInputComponent confirmPinField = new PinInputComponent();

        final Label resultLabel = new Label();
        resultLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 500;");

        final Button changePinBtn = createPrimaryButton("🔐 Đổi mã PIN", "#3498db");
        changePinBtn.setPrefWidth(200);

        changePinBtn.setOnAction(e -> {
            String oldPin = oldPinField.getPin();
            String newPin = newPinField.getPin();
            String confirmPin = confirmPinField.getPin();

            // Validate inputs
            if (oldPin.isEmpty() || newPin.isEmpty() || confirmPin.isEmpty()) {
                resultLabel.setText("❌ Vui lòng điền đầy đủ thông tin");
                resultLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 14px;");
                return;
            }

            if (!newPin.equals(confirmPin)) {
                resultLabel.setText("❌ Mã PIN mới không khớp");
                resultLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 14px;");
                return;
            }

            if (oldPin.equals(newPin)) {
                resultLabel.setText("❌ Mã PIN mới phải khác mã PIN cũ");
                resultLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 14px;");
                return;
            }

            if (newPin.length() < 4 || newPin.length() > 8) {
                resultLabel.setText("❌ Mã PIN phải có độ dài từ 4-8 ký tự");
                resultLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 14px;");
                return;
            }

            try {
                boolean success = service.changePin(resident.getCardId(), oldPin, newPin);
                if (success) {
                    resultLabel.setText("✅ Đổi mã PIN thành công!");
                    resultLabel.setStyle("-fx-text-fill: #2ecc71; -fx-font-size: 14px;");
                    oldPinField.clear();
                    newPinField.clear();
                    confirmPinField.clear();
                    showAlert("Thành công", "Mã PIN đã được thay đổi thành công!", Alert.AlertType.INFORMATION);
                } else {
                    resultLabel.setText("❌ Không thể đổi mã PIN");
                    resultLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 14px;");
                }
            } catch (Exception ex) {
                resultLabel.setText("❌ Lỗi: " + ex.getMessage());
                resultLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 14px;");
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
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

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
