package com.citizencard.ui.views;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import com.citizencard.model.Resident;
import com.citizencard.model.Invoice;
import com.citizencard.model.Parking;
import com.citizencard.model.Transaction;
import com.citizencard.util.ModelConverter;
import com.citizencard.service.CitizenCardService;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.TableCell;
import java.util.List;
import com.citizencard.ui.components.PinInputComponent;
import com.citizencard.ui.components.UITheme;

public class AdminDashboard {
    private Stage stage;
    private CitizenCardService service;
    private BorderPane root;
    private StackPane contentArea;

    public AdminDashboard(Stage stage, CitizenCardService service) {
        this.stage = stage;
        this.service = service;
    }

    public void show() {
        root = new BorderPane();
        contentArea = new StackPane();
        
        // Tạo header bar với nút refresh ở góc phải
        HBox headerBar = createHeaderBar();

        // Tạo VBox chứa header và content
        VBox centerContainer = new VBox();
        centerContainer.getChildren().addAll(headerBar, contentArea);
        VBox.setVgrow(contentArea, javafx.scene.layout.Priority.ALWAYS);

        root.setCenter(centerContainer);

        VBox sidebar = createSidebar();
        root.setLeft(sidebar);

        showHomePage(contentArea);

        Scene scene = new Scene(root, 1400, 900);
        // Load global CSS
        try {
            String css = getClass().getResource("/css/styles.css").toExternalForm();
            scene.getStylesheets().add(css);
        } catch (Exception e) {
            System.err.println("❌ Error loading CSS: " + e.getMessage());
        }

        stage.setTitle("Dashboard Admin");
        stage.setScene(scene);
        stage.show();
    }

    private VBox createSidebar() {
        VBox sidebar = new VBox();
        sidebar.getStyleClass().add("sidebar");

        // Header với gradient
        VBox header = new VBox(8);
        header.getStyleClass().add("sidebar-header");
        header.setPadding(new Insets(30, 25, 30, 25));
        
        Label title = new Label("🔐 Admin Panel");
        title.getStyleClass().add("sidebar-title");
        Label subtitle = new Label("Quản lý hệ thống");
        subtitle.getStyleClass().add("sidebar-subtitle");
        header.getChildren().addAll(title, subtitle);

        // Menu items
        VBox menu = new VBox(8);
        menu.getStyleClass().add("sidebar-menu");
        menu.setPadding(new Insets(20, 15, 20, 15));

        Button homeBtn = createMenuButton("🏠 Trang chủ");
        Button initCardBtn = createMenuButton("✨ Khởi tạo thẻ");
        Button clearCardBtn = createMenuButton("🗑️ Xóa thẻ");
        Button changePinBtn = createMenuButton("🔑 Đổi PIN thẻ");
        Button unblockPinBtn = createMenuButton("🔓 Mở khóa thẻ");
        Button residentsBtn = createMenuButton("👥 Quản lý cư dân");
        Button invoicesBtn = createMenuButton("📄 Quản lý hóa đơn");
        Button parkingBtn = createMenuButton("🚗 Quản lý gửi xe");

        homeBtn.setOnAction(e -> {
            updatePageTitle("🏠 Trang chủ");
            showHomePage(contentArea);
        });
        initCardBtn.setOnAction(e -> {
            updatePageTitle("✨ Khởi tạo thẻ");
            showInitCardPage(contentArea);
        });
        clearCardBtn.setOnAction(e -> {
            updatePageTitle("🗑️ Xóa thẻ");
            showClearCardPage(contentArea);
        });
        changePinBtn.setOnAction(e -> {
            updatePageTitle("🔑 Đổi PIN thẻ");
            showChangePinPage(contentArea);
        });
        unblockPinBtn.setOnAction(e -> {
            updatePageTitle("🔓 Mở khóa thẻ");
            showUnblockPinPage(contentArea);
        });
        residentsBtn.setOnAction(e -> {
            updatePageTitle("👥 Quản lý cư dân");
            showResidentsPage(contentArea);
        });
        invoicesBtn.setOnAction(e -> {
            updatePageTitle("📄 Quản lý hóa đơn");
            showInvoicesPage(contentArea);
        });
        parkingBtn.setOnAction(e -> {
            updatePageTitle("🚗 Quản lý gửi xe");
            showParkingPage(contentArea);
        });

        menu.getChildren().addAll(homeBtn, initCardBtn, clearCardBtn, changePinBtn, unblockPinBtn,
                residentsBtn, invoicesBtn, parkingBtn);

        // Spacer
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

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

        // Nút refresh ở góc phải
        Button refreshBtn = new Button("🔄 Làm mới");
        refreshBtn.setPrefHeight(36);
        UITheme.applyPrimaryButton(refreshBtn);
        refreshBtn.setOnAction(e -> refreshData());

        headerBar.getChildren().addAll(pageTitle, spacer, refreshBtn);

        return headerBar;
    }

    private void updatePageTitle(String title) {
        Label pageTitle = (Label) root.lookup("#pageTitle");
        if (pageTitle != null) {
            pageTitle.setText(title);
        }
    }

    private void refreshData() {
        // Refresh current page
        if (contentArea != null && !contentArea.getChildren().isEmpty()) {
            // Re-render current page
            showHomePage(contentArea);
        }
    }

    private void showHomePage(StackPane contentArea) {
        updatePageTitle("🏠 Trang chủ");
        VBox content = new VBox(30);
        content.setPadding(new Insets(50));
        content.getStyleClass().add("content-area");

        // Header Card
        VBox headerCard = new VBox(15);
        headerCard.setPadding(new Insets(30));
        headerCard.getStyleClass().add("card");

        Label title = new Label("👋 Dashboard Admin");
        title.getStyleClass().add("label-title");

        Label welcomeLabel = new Label("Chào mừng đến với hệ thống quản lý thẻ cư dân");
        welcomeLabel.getStyleClass().add("label-subtitle");

        headerCard.getChildren().addAll(title, welcomeLabel);

        // Stats Cards
        HBox statsBox = new HBox(20);
        statsBox.setPadding(new Insets(20, 0, 0, 0));
        
        // Load real data
        try {
            List<Resident> residents = service.getAllResidents();
            int totalResidents = residents != null ? residents.size() : 0;
            
            int totalBalance = 0;
            if (residents != null) {
                for (Resident r : residents) {
                    try {
                        totalBalance += service.getBalance(r.getCardId());
                    } catch (Exception e) {
                        // Skip if error
                    }
                }
            }
            
            List<Transaction> pendingInvoices = service.getAllInvoices();
            int pendingCount = 0;
            if (pendingInvoices != null) {
                pendingCount = (int) pendingInvoices.stream()
                    .filter(t -> "INVOICE".equals(t.getTransactionType()) && "PENDING".equals(t.getPaymentStatus()))
                    .count();
            }
            
            List<Parking> parkings = service.getAllParking();
            int parkingCount = parkings != null ? parkings.size() : 0;

            VBox statCard1 = createStatCard("📊 Tổng cư dân", String.valueOf(totalResidents), "#0ea5e9");
            VBox statCard2 = createStatCard("💰 Tổng số dư", String.format("%,d VND", totalBalance), "#22c55e");
            VBox statCard3 = createStatCard("📄 Hóa đơn chưa thanh toán", String.valueOf(pendingCount), "#ef4444");
            VBox statCard4 = createStatCard("🚗 Xe đã đăng ký", String.valueOf(parkingCount), "#f59e0b");

            statsBox.getChildren().addAll(statCard1, statCard2, statCard3, statCard4);
        } catch (Exception e) {
            System.err.println("Error loading stats: " + e.getMessage());
            VBox statCard1 = createStatCard("📊 Tổng cư dân", "0", "#0ea5e9");
            VBox statCard2 = createStatCard("💰 Tổng số dư", "0 VND", "#22c55e");
            VBox statCard3 = createStatCard("📄 Hóa đơn chưa thanh toán", "0", "#ef4444");
            VBox statCard4 = createStatCard("🚗 Xe đã đăng ký", "0", "#f59e0b");
            statsBox.getChildren().addAll(statCard1, statCard2, statCard3, statCard4);
        }

        content.getChildren().addAll(headerCard, statsBox);
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

    private void showInitCardPage(StackPane contentArea) {
        VBox content = new VBox(25);
        content.setPadding(new Insets(50));
        // Header
        Label title = new Label("✨ Khởi tạo thẻ mới");
        title.getStyleClass().add("label-title");

        // Info label
        Label infoLabel = new Label(
                "⚠️ Lưu ý: Mỗi lần build lại applet = thẻ trắng. Ghi dữ liệu vào thẻ trắng để demo.");
        infoLabel.getStyleClass().add("label-subtitle");

        // Form Card
        VBox formCard = new VBox(20);
        formCard.setPadding(new Insets(40));
        formCard.getStyleClass().add("card");
        formCard.setMaxWidth(600);

        // Grid layout for form fields
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(15);
        grid.setPadding(new Insets(10));

        // Row 1
        Label cardIdLabel = new Label("Card ID:");
        TextField cardIdField = createStyledTextField("Card ID (16 bytes hex)");
        grid.add(cardIdLabel, 0, 0);
        grid.add(cardIdField, 1, 0);

        // Row 2
        Label nameLabel = new Label("Họ tên:");
        TextField nameField = createStyledTextField("Họ tên đầy đủ");
        grid.add(nameLabel, 0, 1);
        grid.add(nameField, 1, 1);

        // Row 3
        Label dobLabel = new Label("Ngày sinh:");
        TextField dobField = createStyledTextField("YYYY-MM-DD");
        grid.add(dobLabel, 0, 2);
        grid.add(dobField, 1, 2);

        // Row 4
        Label roomLabel = new Label("Số phòng:");
        TextField roomField = createStyledTextField("Số phòng/căn hộ");
        grid.add(roomLabel, 0, 3);
        grid.add(roomField, 1, 3);

        // Row 5
        Label phoneLabel = new Label("Số điện thoại:");
        TextField phoneField = createStyledTextField("Số điện thoại");
        grid.add(phoneLabel, 0, 4);
        grid.add(phoneField, 1, 4);

        // Row 6
        Label emailLabel = new Label("Email:");
        TextField emailField = createStyledTextField("Email");
        grid.add(emailLabel, 0, 5);
        grid.add(emailField, 1, 5);

        // Row 7
        Label idLabel = new Label("CMND/CCCD:");
        TextField idNumberField = createStyledTextField("CMND/CCCD");
        grid.add(idLabel, 0, 6);
        grid.add(idNumberField, 1, 6);

        // Row 8
        Label pinLabel = new Label("PIN:");
        PinInputComponent pinField = new PinInputComponent();
        grid.add(pinLabel, 0, 7);
        grid.add(pinField, 1, 7);

        // Button
        Button initBtn = createPrimaryButton("✨ Khởi tạo thẻ", "#0ea5e9");
        initBtn.setPrefWidth(200);

        Label resultLabel = new Label();
        resultLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 500; -fx-text-fill: #475569;");

        initBtn.setOnAction(e -> {
            // Lấy các giá trị trực tiếp từ fields
            String cardId = cardIdField.getText();
            String fullName = nameField.getText();
            String dateOfBirth = dobField.getText();
            String roomNumber = roomField.getText();
            String phoneNumber = phoneField.getText();
            String email = emailField.getText();
            String idNumber = idNumberField.getText();
            String pin = pinField.getPin();

            try {
                // ✅ Gọi trực tiếp service để khởi tạo thẻ
                service.initializeCard(
                        cardId,
                        fullName,
                        dateOfBirth,
                        roomNumber,
                        phoneNumber,
                        email,
                        idNumber,
                        pin);

                resultLabel.setText("✅ Khởi tạo thẻ thành công!");
                resultLabel.getStyleClass().setAll("label", "label-success");
                // Clear fields
                cardIdField.clear();
                nameField.clear();
                dobField.clear();
                roomField.clear();
                phoneField.clear();
                emailField.clear();
                idNumberField.clear();
                pinField.clear();
            } catch (Exception ex) {
                resultLabel.setText("❌ Lỗi: " + ex.getMessage());
                resultLabel.getStyleClass().setAll("label", "label-danger");
            }
        });

        VBox buttonBox = new VBox(10);
        buttonBox.setAlignment(javafx.geometry.Pos.CENTER);
        buttonBox.getChildren().addAll(initBtn, resultLabel);

        formCard.getChildren().addAll(grid, buttonBox);
        content.getChildren().addAll(title, infoLabel, formCard);
        contentArea.getChildren().clear();
        contentArea.getChildren().add(content);
    }

    private TextField createStyledTextField(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setPrefHeight(40);
        field.setPrefWidth(300);
        UITheme.styleTextField(field);
        return field;
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
            case "#f59e0b":
            case "#f39c12":
                // Warning button - use accent style
                UITheme.applyAccentButton(btn);
                btn.setStyle("-fx-background-color: #f59e0b; -fx-text-fill: #ffffff;");
                break;
            default:
                UITheme.applyPrimaryButton(btn);
                break;
        }
        return btn;
    }

    private void showClearCardPage(StackPane contentArea) {
        VBox content = new VBox(25);
        content.setPadding(new Insets(50));
        content.getStyleClass().add("content-area");

        Label title = new Label("🗑️ Xóa thẻ");
        title.getStyleClass().add("label-title");

        VBox formCard = new VBox(25);
        formCard.setPadding(new Insets(40));
        formCard.getStyleClass().add("card");
        formCard.setMaxWidth(500);
        formCard.setAlignment(javafx.geometry.Pos.CENTER);

        Label cardIdLabel = new Label("Card ID:");
        cardIdLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 600; -fx-text-fill: #64748b;");
        TextField cardIdField = createStyledTextField("Card ID");
        cardIdField.setPrefWidth(400);

        Button clearBtn = createPrimaryButton("🗑️ Xóa thẻ", "#ef4444");
        clearBtn.setPrefWidth(200);

        Label resultLabel = new Label();
        resultLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 500; -fx-text-fill: #475569;");

        clearBtn.setOnAction(e -> {
            try {
                // ✅ Gọi trực tiếp service để xóa thẻ
                boolean success = service.clearCard(cardIdField.getText());

                if (success) {
                    resultLabel.setText("✅ Xóa thẻ thành công!");
                    resultLabel.setStyle("-fx-text-fill: #16a34a; -fx-font-size: 14px;");
                    cardIdField.clear();
                } else {
                    resultLabel.setText("❌ Lỗi: Không thể xóa thẻ");
                    resultLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 14px;");
                }
            } catch (Exception ex) {
                resultLabel.setText("❌ Lỗi: " + ex.getMessage());
                resultLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 14px;");
            }
        });

        VBox fieldsBox = new VBox(15);
        fieldsBox.setAlignment(javafx.geometry.Pos.CENTER);
        fieldsBox.getChildren().addAll(cardIdLabel, cardIdField, clearBtn, resultLabel);

        formCard.getChildren().addAll(fieldsBox);
        content.getChildren().addAll(title, formCard);
        contentArea.getChildren().clear();
        contentArea.getChildren().add(content);
    }

    private void showChangePinPage(StackPane contentArea) {
        VBox content = new VBox(25);
        content.setPadding(new Insets(50));
        content.getStyleClass().add("content-area");

        Label title = new Label("🔑 Đổi PIN thẻ");
        title.getStyleClass().add("label-title");

        VBox formCard = new VBox(25);
        formCard.setPadding(new Insets(40));
        formCard.getStyleClass().add("card");
        formCard.setMaxWidth(500);
        formCard.setAlignment(javafx.geometry.Pos.CENTER);

        Label infoLabel = new Label("Admin có toàn quyền - Không cần nhập PIN cũ");
        infoLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748b; -fx-font-style: italic;");

        Label pinLabel = new Label("PIN mới (6 chữ số):");
        PinInputComponent pinField = new PinInputComponent();

        Button changePinBtn = createPrimaryButton("🔑 Đổi PIN", "#0ea5e9");
        changePinBtn.setPrefWidth(200);

        Label resultLabel = new Label();
        resultLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 500; -fx-text-fill: #475569;");
        resultLabel.setWrapText(true);

        changePinBtn.setOnAction(e -> {
            try {
                String newPin = pinField.getPin();
                if (newPin.length() != 6) {
                    resultLabel.setText("❌ PIN phải có 6 chữ số");
                    resultLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 14px;");
                    return;
                }

                // ✅ Gọi trực tiếp service để đổi PIN
                boolean success = service.changePinByAdmin(newPin);

                if (success) {
                    resultLabel.setText("✅ Đổi PIN thành công!");
                    resultLabel.setStyle("-fx-text-fill: #16a34a; -fx-font-size: 14px;");
                    pinField.clear();
                } else {
                    resultLabel.setText("❌ Lỗi: Không thể đổi PIN");
                    resultLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 14px;");
                }
            } catch (Exception ex) {
                resultLabel.setText("❌ Lỗi: " + ex.getMessage());
                resultLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 14px;");
            }
        });

        VBox fieldsBox = new VBox(15);
        fieldsBox.setAlignment(javafx.geometry.Pos.CENTER);
        fieldsBox.getChildren().addAll(infoLabel, pinLabel, pinField, changePinBtn, resultLabel);

        formCard.getChildren().addAll(fieldsBox);
        content.getChildren().addAll(title, formCard);
        contentArea.getChildren().clear();
        contentArea.getChildren().add(content);
    }

    private void showUnblockPinPage(StackPane contentArea) {
        VBox content = new VBox(25);
        content.setPadding(new Insets(50));
        content.getStyleClass().add("content-area");

        Label title = new Label("🔓 Mở khóa thẻ");
        title.getStyleClass().add("label-title");

        VBox formCard = new VBox(25);
        formCard.setPadding(new Insets(40));
        formCard.getStyleClass().add("card");
        formCard.setMaxWidth(500);
        formCard.setAlignment(javafx.geometry.Pos.CENTER);

        Label infoLabel = new Label("Mở khóa thẻ bị khóa do nhập sai PIN nhiều lần");
        infoLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748b; -fx-font-style: italic;");

        Button unblockBtn = createPrimaryButton("🔓 Mở khóa thẻ", "#f59e0b");
        unblockBtn.setPrefWidth(200);

        Label resultLabel = new Label();
        resultLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 500; -fx-text-fill: #475569;");

        unblockBtn.setOnAction(e -> {
            try {
                // ✅ Gọi trực tiếp service để mở khóa
                boolean success = service.unblockPin();

                if (success) {
                    resultLabel.setText("✅ Mở khóa thẻ thành công!");
                    resultLabel.setStyle("-fx-text-fill: #16a34a; -fx-font-size: 14px;");
                } else {
                    resultLabel.setText("❌ Lỗi: Không thể mở khóa thẻ");
                    resultLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 14px;");
                }
            } catch (Exception ex) {
                resultLabel.setText("❌ Lỗi: " + ex.getMessage());
                resultLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 14px;");
            }
        });

        VBox fieldsBox = new VBox(20);
        fieldsBox.setAlignment(javafx.geometry.Pos.CENTER);
        fieldsBox.getChildren().addAll(infoLabel, unblockBtn, resultLabel);

        formCard.getChildren().addAll(fieldsBox);
        content.getChildren().addAll(title, formCard);
        contentArea.getChildren().clear();
        contentArea.getChildren().add(content);
    }

    private void showResidentsPage(StackPane contentArea) {
        VBox content = new VBox(20);
        content.setPadding(new Insets(40));
        content.getStyleClass().add("content-area");

        Label title = new Label("Quản lý cư dân");
        title.getStyleClass().add("label-title");

        TableView<Resident> table = new TableView<>();
        table.setPrefHeight(500);
        UITheme.styleTable(table);

        TableColumn<Resident, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(50);

        TableColumn<Resident, String> nameCol = new TableColumn<>("Họ tên");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        nameCol.setPrefWidth(200);

        TableColumn<Resident, String> roomCol = new TableColumn<>("Số phòng");
        roomCol.setCellValueFactory(new PropertyValueFactory<>("roomNumber"));
        roomCol.setPrefWidth(100);

        TableColumn<Resident, String> cardIdCol = new TableColumn<>("Card ID");
        cardIdCol.setCellValueFactory(new PropertyValueFactory<>("cardId"));
        cardIdCol.setPrefWidth(200);

        TableColumn<Resident, Integer> balanceCol = new TableColumn<>("Số dư");
        balanceCol.setCellValueFactory(new PropertyValueFactory<>("balance"));
        balanceCol.setPrefWidth(150);
        balanceCol.setCellFactory(column -> new TableCell<Resident, Integer>() {
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

        TableColumn<Resident, String> statusCol = new TableColumn<>("Trạng thái");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setPrefWidth(100);

        TableColumn<Resident, Void> actionCol = new TableColumn<>("Thao tác");
        actionCol.setPrefWidth(150);
        actionCol.setCellFactory(column -> new TableCell<Resident, Void>() {
            private final Button editBtn = new Button("Sửa");

            {
                UITheme.applyPrimaryButton(editBtn);
                editBtn.setPrefWidth(90);
                editBtn.setOnAction(e -> {
                    Resident resident = getTableView().getItems().get(getIndex());
                    showEditResidentDialog(resident, table);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(editBtn);
                }
            }
        });

        table.getColumns().addAll(idCol, nameCol, roomCol, cardIdCol, balanceCol, statusCol, actionCol);

        // ✅ Load residents - gọi trực tiếp service
        try {
            List<Resident> backendResidents = service.getAllResidents();
            List<Resident> desktopResidents = ModelConverter.toDesktopResidents(backendResidents);
            if (desktopResidents != null) {
                table.getItems().addAll(desktopResidents);
            }
        } catch (Exception e) {
            System.err.println("Error loading residents: " + e.getMessage());
        }

        Button refreshBtn = new Button("Làm mới");
        UITheme.applyPrimaryButton(refreshBtn);
        refreshBtn.setOnAction(e -> {
            try {
                table.getItems().clear();
                List<Resident> backendResidents = service.getAllResidents();
                List<Resident> desktopResidents = ModelConverter.toDesktopResidents(backendResidents);
                if (desktopResidents != null) {
                    table.getItems().addAll(desktopResidents);
                }
            } catch (Exception ex) {
                System.err.println("Error refreshing residents: " + ex.getMessage());
            }
        });

        content.getChildren().addAll(title, table, refreshBtn);
        contentArea.getChildren().clear();
        contentArea.getChildren().add(content);
    }

    private void showEditResidentDialog(Resident resident, TableView<Resident> table) {
        Dialog<Resident> dialog = new Dialog<>();
        dialog.setTitle("Sửa thông tin cư dân");
        dialog.setHeaderText("Cập nhật thông tin cư dân");

        VBox content = new VBox(10);
        content.setPadding(new Insets(20));

        TextField nameField = new TextField(resident.getFullName());
        TextField dobField = new TextField(resident.getDateOfBirth());
        TextField roomField = new TextField(resident.getRoomNumber());
        TextField phoneField = new TextField(resident.getPhoneNumber());
        TextField emailField = new TextField(resident.getEmail());
        TextField idNumberField = new TextField(resident.getIdNumber());

        UITheme.styleTextField(nameField);
        UITheme.styleTextField(dobField);
        UITheme.styleTextField(roomField);
        UITheme.styleTextField(phoneField);
        UITheme.styleTextField(emailField);
        UITheme.styleTextField(idNumberField);

        content.getChildren().addAll(
                createStyledLabel("Họ tên:"), nameField,
                createStyledLabel("Ngày sinh:"), dobField,
                createStyledLabel("Số phòng:"), roomField,
                createStyledLabel("Số điện thoại:"), phoneField,
                createStyledLabel("Email:"), emailField,
                createStyledLabel("CMND/CCCD:"), idNumberField);

        dialog.getDialogPane().setContent(content);

        ButtonType saveButtonType = new ButtonType("Lưu", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        UITheme.styleDialogPane(dialog.getDialogPane());

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                resident.setFullName(nameField.getText());
                resident.setDateOfBirth(dobField.getText());
                resident.setRoomNumber(roomField.getText());
                resident.setPhoneNumber(phoneField.getText());
                resident.setEmail(emailField.getText());
                resident.setIdNumber(idNumberField.getText());
                return resident;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(updatedResident -> {
            try {
                Resident backendResident = ModelConverter
                        .toBackendResident(updatedResident);
                List<Resident> allResidents = service.getAllResidents();
                Resident existingResident = allResidents.stream()
                        .filter(r -> r.getId().equals(updatedResident.getId()))
                        .findFirst()
                        .orElse(null);

                String pin = existingResident != null && existingResident.getPinHash() != null
                        ? existingResident.getPinHash()
                        : "000000";

                service.updateResidentInfoByAdmin(
                        updatedResident.getId(),
                        backendResident.getFullName(),
                        backendResident.getDateOfBirth(),
                        backendResident.getRoomNumber(),
                        backendResident.getPhoneNumber(),
                        backendResident.getEmail(),
                        backendResident.getIdNumber(),
                        pin);

                showAlert("Thành công", "Cập nhật thông tin thành công!", Alert.AlertType.INFORMATION);
                // Refresh table
                table.getItems().clear();
                List<Resident> backendResidents = service.getAllResidents();
                List<Resident> desktopResidents = ModelConverter.toDesktopResidents(backendResidents);
                if (desktopResidents != null) {
                    table.getItems().addAll(desktopResidents);
                }
            } catch (Exception ex) {
                showAlert("Lỗi", "Lỗi cập nhật: " + ex.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }

    private Label createStyledLabel(String text) {
        Label label = new Label(text);
        // Default label style is fine now
        return label;
    }

    private void showInvoicesPage(StackPane contentArea) {
        VBox content = new VBox(20);
        content.setPadding(new Insets(40));
        content.getStyleClass().add("content-area");

        Label title = new Label("Quản lý hóa đơn");
        title.getStyleClass().add("label-title");

        // Form tạo hóa đơn
        VBox form = new VBox(10);
        form.setPadding(new Insets(20));
        form.getStyleClass().add("card");

        Label formTitle = new Label("Tạo hóa đơn mới");
        formTitle.getStyleClass().add("label-subtitle");

        // Table hiển thị tất cả hóa đơn (khai báo trước để dùng trong lambda)
        TableView<Invoice> invoicesTable = new TableView<>();
        invoicesTable.setPrefHeight(400);
        UITheme.styleTable(invoicesTable);

        // ComboBox để chọn resident
        Label residentLabel = new Label("Chọn cư dân:");

        ComboBox<String> residentComboBox = new ComboBox<>();
        residentComboBox.setPromptText("-- Chọn cư dân --");
        residentComboBox.setPrefWidth(300);
        UITheme.styleComboBox(residentComboBox);

        // Load danh sách residents
        try {
            List<Resident> backendResidents = service.getAllResidents();
            for (Resident resident : backendResidents) {
                // Format: "ID - Tên - Phòng"
                String displayText = resident.getId() + " - " +
                        (resident.getFullName() != null ? resident.getFullName() : "N/A") +
                        " - Phòng " +
                        (resident.getRoomNumber() != null ? resident.getRoomNumber() : "N/A");
                residentComboBox.getItems().add(displayText);
            }
            // Chọn resident đầu tiên mặc định
            if (!residentComboBox.getItems().isEmpty()) {
                residentComboBox.getSelectionModel().selectFirst();
            }
        } catch (Exception ex) {
            System.err.println("Error loading residents: " + ex.getMessage());
        }

        TextField serviceNameField = new TextField();
        serviceNameField.setPromptText("Tên dịch vụ (VD: Điện, Nước, Phí quản lý)");
        serviceNameField.setPrefWidth(300);
        UITheme.styleTextField(serviceNameField);

        TextField amountField = new TextField();
        amountField.setPromptText("Số tiền (VND)");
        amountField.setPrefWidth(300);
        UITheme.styleTextField(amountField);

        TextArea descriptionArea = new TextArea();
        descriptionArea.setPromptText("Mô tả");
        descriptionArea.setPrefWidth(300);
        descriptionArea.setPrefRowCount(3);
        UITheme.styleTextArea(descriptionArea);

        Button createBtn = new Button("Tạo hóa đơn");
        createBtn.setPrefWidth(200);
        UITheme.applyAccentButton(createBtn);

        Label resultLabel = new Label();

        createBtn.setOnAction(e -> {
            try {
                // Lấy resident ID từ ComboBox
                String selectedResident = residentComboBox.getValue();
                if (selectedResident == null || selectedResident.isEmpty()) {
                    resultLabel.setText("❌ Vui lòng chọn cư dân");
                    resultLabel.getStyleClass().setAll("label", "label-danger");
                    return;
                }

                // Parse resident ID từ string "ID - Tên - Phòng"
                Integer residentId = Integer.parseInt(selectedResident.split(" - ")[0]);

                String serviceName = serviceNameField.getText();
                Integer amount = Integer.parseInt(amountField.getText());
                String description = descriptionArea.getText();

                // Validate
                if (serviceName == null || serviceName.trim().isEmpty()) {
                    resultLabel.setText("❌ Vui lòng nhập tên dịch vụ");
                    resultLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 14px;");
                    return;
                }

                // Gọi service để tạo hóa đơn
                try {
                    service.createInvoice(
                            residentId,
                            serviceName,
                            amount,
                            description);
                } catch (Exception ex) {
                    resultLabel.setText("❌ Lỗi: " + ex.getMessage());
                    resultLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 14px;");
                    return;
                }

                resultLabel.setText("✅ Tạo hóa đơn thành công cho cư dân ID " + residentId + "!");
                resultLabel.setStyle("-fx-text-fill: #16a34a; -fx-font-size: 14px;");
                // Clear form
                serviceNameField.clear();
                amountField.clear();
                descriptionArea.clear();
                // Refresh table
                refreshInvoicesTable(invoicesTable);
            } catch (NumberFormatException ex) {
                resultLabel.setText("❌ Vui lòng nhập số hợp lệ");
                resultLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 14px;");
            } catch (Exception ex) {
                resultLabel.setText("❌ Lỗi: " + ex.getMessage());
                resultLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 14px;");
            }
        });

        form.getChildren().addAll(formTitle,
                residentLabel, residentComboBox,
                createStyledLabel("Tên dịch vụ:"), serviceNameField,
                createStyledLabel("Số tiền:"), amountField,
                createStyledLabel("Mô tả:"), descriptionArea,
                createBtn, resultLabel);

        // Table hiển thị tất cả hóa đơn (đã khai báo ở trên)
        Label tableTitle = new Label("Danh sách hóa đơn");
        tableTitle.getStyleClass().add("label-subtitle");

        TableColumn<Invoice, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(50);

        TableColumn<Invoice, Integer> residentIdCol = new TableColumn<>("Resident ID");
        residentIdCol.setCellValueFactory(new PropertyValueFactory<>("residentId"));
        residentIdCol.setPrefWidth(100);

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

        TableColumn<Invoice, String> statusCol = new TableColumn<>("Trạng thái");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("paymentStatus"));
        statusCol.setPrefWidth(120);

        TableColumn<Invoice, String> dateCol = new TableColumn<>("Ngày tạo");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("invoiceDate"));
        dateCol.setPrefWidth(150);

        invoicesTable.getColumns().addAll(idCol, residentIdCol, serviceCol, amountCol, statusCol, dateCol);

        // Load all invoices (cần API endpoint mới hoặc dùng endpoint hiện có)
        // Tạm thời để trống, có thể thêm sau

        Button refreshTableBtn = new Button("Làm mới danh sách");
        UITheme.applyPrimaryButton(refreshTableBtn);
        refreshTableBtn.setOnAction(e -> refreshInvoicesTable(invoicesTable));

        content.getChildren().addAll(title, form, tableTitle, invoicesTable, refreshTableBtn);
        contentArea.getChildren().clear();
        contentArea.getChildren().add(content);
    }

    private void showParkingPage(StackPane contentArea) {
        VBox content = new VBox(20);
        content.setPadding(new Insets(40));
        content.getStyleClass().add("content-area");

        Label title = new Label("Quản lý gửi xe");
        title.getStyleClass().add("label-title");

        // Form đăng ký gửi xe
        VBox form = new VBox(10);
        form.setPadding(new Insets(20));
        form.getStyleClass().add("card");

        Label formTitle = new Label("Đăng ký gửi xe");
        formTitle.getStyleClass().add("label-subtitle");

        // Table hiển thị danh sách gửi xe (khai báo trước để dùng trong lambda)
        TableView<Parking> parkingTable = new TableView<>();
        parkingTable.setPrefHeight(400);
        UITheme.styleTable(parkingTable);

        // Vì chỉ có 1 user, không cần nhập Resident ID (luôn là 1)
        Label residentIdLabel = new Label("Resident ID: 1 (Mặc định)");
        residentIdLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #64748b;");

        TextField licensePlateField = new TextField();
        licensePlateField.setPromptText("Biển số xe");
        licensePlateField.setPrefWidth(300);
        UITheme.styleTextField(licensePlateField);

        ComboBox<String> vehicleTypeCombo = new ComboBox<>();
        vehicleTypeCombo.getItems().addAll("MOTORBIKE", "CAR", "BICYCLE");
        vehicleTypeCombo.setValue("MOTORBIKE");
        vehicleTypeCombo.setPrefWidth(300);
        UITheme.styleComboBox(vehicleTypeCombo);

        Button registerBtn = new Button("Đăng ký");
        registerBtn.setPrefWidth(200);
        UITheme.applyPrimaryButton(registerBtn);

        Label resultLabel = new Label();

        registerBtn.setOnAction(e -> {
            try {
                // Vì chỉ có 1 user, luôn dùng resident_id = 1
                String licensePlate = licensePlateField.getText();
                String vehicleType = vehicleTypeCombo.getValue();

                // ✅ Gọi trực tiếp service để đăng ký gửi xe (resident_id = 1)
                try {
                    service.registerParking(
                            1, // Luôn là 1
                            licensePlate,
                            vehicleType);
                } catch (Exception ex) {
                    resultLabel.setText("❌ Lỗi: " + ex.getMessage());
                    resultLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 14px;");
                    return;
                }

                resultLabel.setText("✅ Đăng ký gửi xe thành công!");
                resultLabel.setStyle("-fx-text-fill: #16a34a; -fx-font-size: 14px;");
                // Clear form
                licensePlateField.clear();
                vehicleTypeCombo.setValue("MOTORBIKE");
                // Refresh table
                refreshParkingTable(parkingTable);
            } catch (NumberFormatException ex) {
                resultLabel.setText("Vui lòng nhập số hợp lệ");
                resultLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 14px;");
            }
        });

        form.getChildren().addAll(formTitle, residentIdLabel,
                createStyledLabel("Biển số xe:"), licensePlateField,
                createStyledLabel("Loại xe:"), vehicleTypeCombo,
                registerBtn, resultLabel);

        // Table hiển thị danh sách gửi xe (đã khai báo ở trên)
        Label tableTitle = new Label("Danh sách gửi xe");
        tableTitle.getStyleClass().add("label-subtitle");

        TableColumn<Parking, String> licenseCol = new TableColumn<>("Biển số");
        licenseCol.setCellValueFactory(new PropertyValueFactory<>("licensePlate"));
        licenseCol.setPrefWidth(120);

        TableColumn<Parking, String> vehicleTypeCol = new TableColumn<>("Loại xe");
        vehicleTypeCol.setCellValueFactory(new PropertyValueFactory<>("vehicleType"));
        vehicleTypeCol.setPrefWidth(100);

        TableColumn<Parking, Integer> feeCol = new TableColumn<>("Phí tháng");
        feeCol.setCellValueFactory(new PropertyValueFactory<>("monthlyFee"));
        feeCol.setPrefWidth(150);
        feeCol.setCellFactory(column -> new TableCell<Parking, Integer>() {
            @Override
            protected void updateItem(Integer fee, boolean empty) {
                super.updateItem(fee, empty);
                if (empty || fee == null) {
                    setText(null);
                } else {
                    setText(String.format("%,d VND", fee));
                }
            }
        });

        TableColumn<Parking, String> timestampCol = new TableColumn<>("Thời gian");
        timestampCol.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        timestampCol.setPrefWidth(180);

        parkingTable.getColumns().addAll(licenseCol, vehicleTypeCol, feeCol, timestampCol);

        Button refreshTableBtn = new Button("Làm mới danh sách");
        UITheme.applyPrimaryButton(refreshTableBtn);
        refreshTableBtn.setOnAction(e -> refreshParkingTable(parkingTable));

        content.getChildren().addAll(title, form, tableTitle, parkingTable, refreshTableBtn);
        contentArea.getChildren().clear();
        contentArea.getChildren().add(content);
    }

    private void refreshParkingTable(TableView<Parking> table) {
        try {
            table.getItems().clear();
            List<Parking> backendParkings = service.getAllParking();
            if (backendParkings == null) {
                return;
            }
            List<Parking> desktopParkings = ModelConverter.toDesktopParkings(backendParkings);
            if (desktopParkings != null && !desktopParkings.isEmpty()) {
                table.getItems().addAll(desktopParkings);
            }
        } catch (Exception e) {
            System.err.println("Error refreshing parking table: " + e.getMessage());
            showAlert("Lỗi", "Không thể tải danh sách gửi xe: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void refreshInvoicesTable(TableView<Invoice> table) {
        try {
            table.getItems().clear();
            List<Transaction> backendInvoices = service.getAllInvoices();
            if (backendInvoices == null) {
                return;
            }
            List<Invoice> desktopInvoices = ModelConverter.transactionsToDesktopInvoices(backendInvoices);
            if (desktopInvoices != null && !desktopInvoices.isEmpty()) {
                table.getItems().addAll(desktopInvoices);
            }
        } catch (Exception e) {
            System.err.println("Error refreshing invoices table: " + e.getMessage());
            showAlert("Lỗi", "Không thể tải danh sách hóa đơn: " + e.getMessage(), Alert.AlertType.ERROR);
        }
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
