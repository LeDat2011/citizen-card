package com.citizencard.desktop.ui;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import com.citizencard.desktop.model.Resident;
import com.citizencard.desktop.model.Invoice;
import com.citizencard.desktop.model.Parking;
import com.citizencard.desktop.util.ModelConverter;
import com.citizencard.backend.service.CitizenCardService;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.TableCell;
import java.util.List;

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
        root.setCenter(contentArea);

        VBox sidebar = createSidebar();
        root.setLeft(sidebar);

        showHomePage(contentArea);

        Scene scene = new Scene(root, 1400, 900);
        stage.setTitle("Dashboard Admin");
        stage.setScene(scene);
        stage.show();
    }

    private VBox createSidebar() {
        VBox sidebar = new VBox(8);
        sidebar.setPadding(new Insets(25));
        sidebar.setStyle("-fx-background-color: linear-gradient(to bottom, #2c3e50 0%, #34495e 100%); " +
                "-fx-min-width: 240px; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 10, 0, 0, 0);");

        VBox header = new VBox(5);
        header.setPadding(new Insets(0, 0, 20, 0));
        Label title = new Label("🔐 Admin Panel");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 22px; -fx-font-weight: bold;");
        Label subtitle = new Label("Quản lý hệ thống");
        subtitle.setStyle("-fx-text-fill: rgba(255,255,255,0.7); -fx-font-size: 12px;");
        header.getChildren().addAll(title, subtitle);

        Separator separator = new Separator();
        separator.setStyle("-fx-background-color: rgba(255,255,255,0.2);");

        Button homeBtn = createMenuButton("🏠 Trang chủ");
        Button initCardBtn = createMenuButton("✨ Khởi tạo thẻ");
        Button clearCardBtn = createMenuButton("🗑️ Xóa thẻ");
        Button changePinBtn = createMenuButton("🔑 Đổi PIN thẻ");
        Button unblockPinBtn = createMenuButton("🔓 Mở khóa thẻ");
        Button residentsBtn = createMenuButton("👥 Quản lý cư dân");
        Button invoicesBtn = createMenuButton("📄 Quản lý hóa đơn");
        Button parkingBtn = createMenuButton("🚗 Quản lý gửi xe");

        Button logoutBtn = new Button("🚪 Đăng xuất");
        logoutBtn.setPrefWidth(200);
        logoutBtn.setPrefHeight(45);
        logoutBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; " +
                "-fx-font-size: 14px; -fx-font-weight: bold; " +
                "-fx-background-radius: 8; -fx-cursor: hand;");
        logoutBtn.setOnMouseEntered(e -> logoutBtn.setStyle(
                "-fx-background-color: #c0392b; -fx-text-fill: white; " +
                        "-fx-font-size: 14px; -fx-font-weight: bold; " +
                        "-fx-background-radius: 8; -fx-cursor: hand; " +
                        "-fx-effect: dropshadow(three-pass-box, rgba(231,76,60,0.4), 8, 0, 0, 2);"));
        logoutBtn.setOnMouseExited(e -> logoutBtn.setStyle(
                "-fx-background-color: #e74c3c; -fx-text-fill: white; " +
                        "-fx-font-size: 14px; -fx-font-weight: bold; " +
                        "-fx-background-radius: 8; -fx-cursor: hand;"));

        homeBtn.setOnAction(e -> showHomePage(contentArea));
        initCardBtn.setOnAction(e -> showInitCardPage(contentArea));
        clearCardBtn.setOnAction(e -> showClearCardPage(contentArea));
        changePinBtn.setOnAction(e -> showChangePinPage(contentArea));
        unblockPinBtn.setOnAction(e -> showUnblockPinPage(contentArea));
        residentsBtn.setOnAction(e -> showResidentsPage(contentArea));
        invoicesBtn.setOnAction(e -> showInvoicesPage(contentArea));
        parkingBtn.setOnAction(e -> showParkingPage(contentArea));
        logoutBtn.setOnAction(e -> {
            LoginView loginView = new LoginView(stage, service);
            loginView.show();
        });

        VBox spacer = new VBox();
        VBox.setVgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        sidebar.getChildren().addAll(header, separator, homeBtn, initCardBtn, clearCardBtn,
                changePinBtn, unblockPinBtn, residentsBtn, invoicesBtn, parkingBtn, spacer, logoutBtn);

        return sidebar;
    }

    private Button createMenuButton(String text) {
        Button btn = new Button(text);
        btn.setPrefWidth(200);
        btn.setPrefHeight(45);
        btn.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        btn.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-text-fill: white; " +
                "-fx-font-size: 14px; -fx-font-weight: 500; " +
                "-fx-background-radius: 8; -fx-cursor: hand; " +
                "-fx-padding: 0 15 0 15;");
        btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color: rgba(255,255,255,0.2); -fx-text-fill: white; " +
                        "-fx-font-size: 14px; -fx-font-weight: 600; " +
                        "-fx-background-radius: 8; -fx-cursor: hand; " +
                        "-fx-padding: 0 15 0 15; " +
                        "-fx-effect: dropshadow(three-pass-box, rgba(255,255,255,0.2), 5, 0, 0, 1);"));
        btn.setOnMouseExited(e -> btn.setStyle(
                "-fx-background-color: rgba(255,255,255,0.1); -fx-text-fill: white; " +
                        "-fx-font-size: 14px; -fx-font-weight: 500; " +
                        "-fx-background-radius: 8; -fx-cursor: hand; " +
                        "-fx-padding: 0 15 0 15;"));
        return btn;
    }

    private void showHomePage(StackPane contentArea) {
        VBox content = new VBox(30);
        content.setPadding(new Insets(50));
        content.setStyle("-fx-background-color: #f8f9fa;");

        // Header Card
        VBox headerCard = new VBox(15);
        headerCard.setPadding(new Insets(30));
        headerCard.setStyle("-fx-background-color: linear-gradient(to right, #667eea 0%, #764ba2 100%); " +
                "-fx-background-radius: 15; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(102,126,234,0.3), 15, 0, 0, 5);");

        Label title = new Label("👋 Dashboard Admin");
        title.setStyle("-fx-font-size: 36px; -fx-font-weight: bold; -fx-text-fill: white;");

        Label welcomeLabel = new Label("Chào mừng đến với hệ thống quản lý thẻ cư dân");
        welcomeLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: rgba(255,255,255,0.95);");

        headerCard.getChildren().addAll(title, welcomeLabel);

        // Stats Cards
        HBox statsBox = new HBox(20);
        statsBox.setPadding(new Insets(20, 0, 0, 0));

        VBox statCard1 = createStatCard("📊 Tổng cư dân", "0", "#3498db");
        VBox statCard2 = createStatCard("💰 Tổng số dư", "0 VND", "#2ecc71");
        VBox statCard3 = createStatCard("📄 Hóa đơn chưa thanh toán", "0", "#e74c3c");
        VBox statCard4 = createStatCard("🚗 Xe đã đăng ký", "0", "#f39c12");

        statsBox.getChildren().addAll(statCard1, statCard2, statCard3, statCard4);

        content.getChildren().addAll(headerCard, statsBox);
        contentArea.getChildren().clear();
        contentArea.getChildren().add(content);
    }

    private VBox createStatCard(String title, String value, String color) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(25));
        card.setPrefWidth(200);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 2);");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d;");

        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");

        card.getChildren().addAll(titleLabel, valueLabel);
        return card;
    }

    private void showInitCardPage(StackPane contentArea) {
        VBox content = new VBox(25);
        content.setPadding(new Insets(50));
        content.setStyle("-fx-background-color: #f8f9fa;");

        // Header
        Label title = new Label("✨ Khởi tạo thẻ mới");
        title.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        // Info label
        Label infoLabel = new Label(
                "⚠️ Lưu ý: Mỗi lần build lại applet = thẻ trắng. Ghi dữ liệu vào thẻ trắng để demo.");
        infoLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d; -fx-font-style: italic; -fx-wrap-text: true;");

        // Form Card
        VBox formCard = new VBox(20);
        formCard.setPadding(new Insets(40));
        formCard.setStyle("-fx-background-color: white; -fx-background-radius: 15; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 15, 0, 0, 5);");
        formCard.setMaxWidth(600);

        // Grid layout for form fields
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(15);
        grid.setPadding(new Insets(10));

        // Row 1
        Label cardIdLabel = new Label("Card ID:");
        cardIdLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #34495e;");
        TextField cardIdField = createStyledTextField("Card ID (16 bytes hex)");
        grid.add(cardIdLabel, 0, 0);
        grid.add(cardIdField, 1, 0);

        // Row 2
        Label nameLabel = new Label("Họ tên:");
        nameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #34495e;");
        TextField nameField = createStyledTextField("Họ tên đầy đủ");
        grid.add(nameLabel, 0, 1);
        grid.add(nameField, 1, 1);

        // Row 3
        Label dobLabel = new Label("Ngày sinh:");
        dobLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #34495e;");
        TextField dobField = createStyledTextField("YYYY-MM-DD");
        grid.add(dobLabel, 0, 2);
        grid.add(dobField, 1, 2);

        // Row 4
        Label roomLabel = new Label("Số phòng:");
        roomLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #34495e;");
        TextField roomField = createStyledTextField("Số phòng/căn hộ");
        grid.add(roomLabel, 0, 3);
        grid.add(roomField, 1, 3);

        // Row 5
        Label phoneLabel = new Label("Số điện thoại:");
        phoneLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #34495e;");
        TextField phoneField = createStyledTextField("Số điện thoại");
        grid.add(phoneLabel, 0, 4);
        grid.add(phoneField, 1, 4);

        // Row 6
        Label emailLabel = new Label("Email:");
        emailLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #34495e;");
        TextField emailField = createStyledTextField("Email");
        grid.add(emailLabel, 0, 5);
        grid.add(emailField, 1, 5);

        // Row 7
        Label idLabel = new Label("CMND/CCCD:");
        idLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #34495e;");
        TextField idNumberField = createStyledTextField("CMND/CCCD");
        grid.add(idLabel, 0, 6);
        grid.add(idNumberField, 1, 6);

        // Row 8
        Label pinLabel = new Label("PIN:");
        pinLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #34495e;");
        PasswordField pinField = createStyledPasswordField("PIN (6 chữ số)");
        grid.add(pinLabel, 0, 7);
        grid.add(pinField, 1, 7);

        // Button
        Button initBtn = createPrimaryButton("✨ Khởi tạo thẻ", "#667eea");
        initBtn.setPrefWidth(200);

        Label resultLabel = new Label();
        resultLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 500;");

        initBtn.setOnAction(e -> {
            // Lấy các giá trị trực tiếp từ fields
            String cardId = cardIdField.getText();
            String fullName = nameField.getText();
            String dateOfBirth = dobField.getText();
            String roomNumber = roomField.getText();
            String phoneNumber = phoneField.getText();
            String email = emailField.getText();
            String idNumber = idNumberField.getText();
            String pin = pinField.getText();

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
                resultLabel.setStyle("-fx-text-fill: #2ecc71; -fx-font-size: 14px;");
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
                resultLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 14px;");
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
        field.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #dee2e6; " +
                "-fx-border-radius: 8; -fx-background-radius: 8; " +
                "-fx-border-width: 1.5; -fx-padding: 10; " +
                "-fx-font-size: 14px;");
        field.setOnMouseEntered(e -> field.setStyle(
                "-fx-background-color: white; -fx-border-color: #667eea; " +
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

    private PasswordField createStyledPasswordField(String prompt) {
        PasswordField field = new PasswordField();
        field.setPromptText(prompt);
        field.setPrefHeight(40);
        field.setPrefWidth(300);
        field.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #dee2e6; " +
                "-fx-border-radius: 8; -fx-background-radius: 8; " +
                "-fx-border-width: 1.5; -fx-padding: 10; " +
                "-fx-font-size: 14px;");
        field.setOnMouseEntered(e -> field.setStyle(
                "-fx-background-color: white; -fx-border-color: #667eea; " +
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
        btn.setPrefHeight(45);
        btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; " +
                "-fx-font-size: 16px; -fx-font-weight: bold; " +
                "-fx-background-radius: 10; -fx-cursor: hand;");
        btn.setOnMouseEntered(e -> {
            String darkerColor = darkenColor(color);
            btn.setStyle("-fx-background-color: " + darkerColor + "; -fx-text-fill: white; " +
                    "-fx-font-size: 16px; -fx-font-weight: bold; " +
                    "-fx-background-radius: 10; -fx-cursor: hand; " +
                    "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 10, 0, 0, 3);");
        });
        btn.setOnMouseExited(e -> btn.setStyle(
                "-fx-background-color: " + color + "; -fx-text-fill: white; " +
                        "-fx-font-size: 16px; -fx-font-weight: bold; " +
                        "-fx-background-radius: 10; -fx-cursor: hand;"));
        return btn;
    }

    private String darkenColor(String color) {
        // Simple darkening for common colors
        switch (color) {
            case "#667eea":
                return "#5568d3";
            case "#e74c3c":
                return "#c0392b";
            case "#3498db":
                return "#2980b9";
            case "#2ecc71":
                return "#27ae60";
            default:
                return color;
        }
    }

    private void showClearCardPage(StackPane contentArea) {
        VBox content = new VBox(25);
        content.setPadding(new Insets(50));
        content.setStyle("-fx-background-color: #f8f9fa;");

        Label title = new Label("🗑️ Xóa thẻ");
        title.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        VBox formCard = new VBox(25);
        formCard.setPadding(new Insets(40));
        formCard.setStyle("-fx-background-color: white; -fx-background-radius: 15; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 15, 0, 0, 5);");
        formCard.setMaxWidth(500);
        formCard.setAlignment(javafx.geometry.Pos.CENTER);

        Label cardIdLabel = new Label("Card ID:");
        cardIdLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #34495e;");
        TextField cardIdField = createStyledTextField("Card ID");
        cardIdField.setPrefWidth(400);

        Button clearBtn = createPrimaryButton("🗑️ Xóa thẻ", "#e74c3c");
        clearBtn.setPrefWidth(200);

        Label resultLabel = new Label();
        resultLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 500;");

        clearBtn.setOnAction(e -> {
            try {
                // ✅ Gọi trực tiếp service để xóa thẻ
                boolean success = service.clearCard(cardIdField.getText());

                if (success) {
                    resultLabel.setText("✅ Xóa thẻ thành công!");
                    resultLabel.setStyle("-fx-text-fill: #2ecc71; -fx-font-size: 14px;");
                    cardIdField.clear();
                } else {
                    resultLabel.setText("❌ Lỗi: Không thể xóa thẻ");
                    resultLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 14px;");
                }
            } catch (Exception ex) {
                resultLabel.setText("❌ Lỗi: " + ex.getMessage());
                resultLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 14px;");
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
        content.setStyle("-fx-background-color: #f8f9fa;");

        Label title = new Label("🔑 Đổi PIN thẻ");
        title.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        VBox formCard = new VBox(25);
        formCard.setPadding(new Insets(40));
        formCard.setStyle("-fx-background-color: white; -fx-background-radius: 15; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 15, 0, 0, 5);");
        formCard.setMaxWidth(500);
        formCard.setAlignment(javafx.geometry.Pos.CENTER);

        Label infoLabel = new Label("Admin có toàn quyền - Không cần nhập PIN cũ");
        infoLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d; -fx-font-style: italic;");

        Label pinLabel = new Label("PIN mới (6 chữ số):");
        pinLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #34495e;");
        PasswordField pinField = createStyledPasswordField("Nhập PIN mới (6 chữ số)");
        pinField.setPrefWidth(400);

        // Giới hạn chỉ nhập số và tối đa 6 ký tự
        pinField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                pinField.setText(newValue.replaceAll("[^\\d]", ""));
            }
            if (pinField.getText().length() > 6) {
                pinField.setText(pinField.getText().substring(0, 6));
            }
        });

        Button changePinBtn = createPrimaryButton("🔑 Đổi PIN", "#3498db");
        changePinBtn.setPrefWidth(200);

        Label resultLabel = new Label();
        resultLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 500;");
        resultLabel.setWrapText(true);

        changePinBtn.setOnAction(e -> {
            try {
                String newPin = pinField.getText();

                // Validate PIN
                if (newPin == null || newPin.isEmpty()) {
                    resultLabel.setText("❌ Vui lòng nhập PIN mới");
                    resultLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 14px;");
                    return;
                }

                if (newPin.length() != 6) {
                    resultLabel.setText("❌ PIN phải là 6 chữ số");
                    resultLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 14px;");
                    return;
                }

                if (!newPin.matches("\\d{6}")) {
                    resultLabel.setText("❌ PIN chỉ được chứa số");
                    resultLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 14px;");
                    return;
                }

                // ✅ Gọi service để đổi PIN (Admin - không cần PIN cũ)
                boolean success = service.changePinByAdmin(newPin);

                if (success) {
                    resultLabel.setText(
                            "✅ Đổi PIN thành công!\nPIN mới: " + newPin + "\nCư dân sẽ dùng PIN này để đăng nhập.");
                    resultLabel.setStyle("-fx-text-fill: #2ecc71; -fx-font-size: 14px;");
                    pinField.clear();
                } else {
                    resultLabel.setText("❌ Lỗi: Không thể đổi PIN");
                    resultLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 14px;");
                }
            } catch (Exception ex) {
                resultLabel.setText("❌ Lỗi: " + ex.getMessage());
                resultLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 14px;");
            }
        });

        VBox fieldsBox = new VBox(15);
        fieldsBox.setAlignment(javafx.geometry.Pos.CENTER);
        fieldsBox.getChildren().addAll(infoLabel, pinLabel, pinField, changePinBtn, resultLabel);

        formCard.getChildren().addAll(fieldsBox);

        VBox mainContent = new VBox(20);
        mainContent.setAlignment(javafx.geometry.Pos.CENTER);
        mainContent.getChildren().addAll(title, formCard);

        content.getChildren().add(mainContent);
        contentArea.getChildren().clear();
        contentArea.getChildren().add(content);
    }

    private void showUnblockPinPage(StackPane contentArea) {
        VBox content = new VBox(25);
        content.setPadding(new Insets(50));
        content.setStyle("-fx-background-color: #f8f9fa;");

        Label title = new Label("🔓 Mở khóa thẻ");
        title.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        VBox formCard = new VBox(25);
        formCard.setPadding(new Insets(40));
        formCard.setStyle("-fx-background-color: white; -fx-background-radius: 15; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 15, 0, 0, 5);");
        formCard.setMaxWidth(500);
        formCard.setAlignment(javafx.geometry.Pos.CENTER);

        Label infoLabel = new Label("Mở khóa thẻ khi bị block do nhập sai PIN 5 lần");
        infoLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d; -fx-font-weight: 500;");
        infoLabel.setWrapText(true);

        Button unblockBtn = createPrimaryButton("🔓 Mở khóa thẻ", "#27ae60");
        unblockBtn.setPrefWidth(250);

        Label resultLabel = new Label();
        resultLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 500;");
        resultLabel.setWrapText(true);

        unblockBtn.setOnAction(e -> {
            try {
                // Gọi service để mở khóa thẻ
                boolean success = service.unblockPin();

                if (success) {
                    resultLabel.setText("✅ Mở khóa thẻ thành công!\nThẻ đã được reset và có thể đăng nhập lại.");
                    resultLabel.setStyle("-fx-text-fill: #2ecc71; -fx-font-size: 14px; -fx-font-weight: 600;");
                } else {
                    resultLabel.setText("❌ Lỗi: Không thể mở khóa thẻ");
                    resultLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 14px; -fx-font-weight: 600;");
                }
            } catch (Exception ex) {
                resultLabel.setText("❌ Lỗi: " + ex.getMessage());
                resultLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 14px; -fx-font-weight: 600;");
            }
        });

        formCard.getChildren().addAll(infoLabel, unblockBtn, resultLabel);

        content.getChildren().addAll(title, formCard);
        contentArea.getChildren().clear();
        contentArea.getChildren().add(content);
    }

    private void showResidentsPage(StackPane contentArea) {
        VBox content = new VBox(20);
        content.setPadding(new Insets(40));

        Label title = new Label("Quản lý cư dân");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        TableView<Resident> table = new TableView<>();
        table.setPrefHeight(500);

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
            List<com.citizencard.backend.model.Resident> backendResidents = service.getAllResidents();
            List<Resident> desktopResidents = ModelConverter.toDesktopResidents(backendResidents);
            if (desktopResidents != null) {
                table.getItems().addAll(desktopResidents);
            }
        } catch (Exception e) {
            System.err.println("Error loading residents: " + e.getMessage());
        }

        Button refreshBtn = new Button("Làm mới");
        refreshBtn.setOnAction(e -> {
            try {
                table.getItems().clear();
                List<com.citizencard.backend.model.Resident> backendResidents = service.getAllResidents();
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

        content.getChildren().addAll(
                new Label("Họ tên:"), nameField,
                new Label("Ngày sinh:"), dobField,
                new Label("Số phòng:"), roomField,
                new Label("Số điện thoại:"), phoneField,
                new Label("Email:"), emailField,
                new Label("CMND/CCCD:"), idNumberField);

        dialog.getDialogPane().setContent(content);

        ButtonType saveButtonType = new ButtonType("Lưu", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

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
                com.citizencard.backend.model.Resident backendResident = ModelConverter
                        .toBackendResident(updatedResident);
                List<com.citizencard.backend.model.Resident> allResidents = service.getAllResidents();
                com.citizencard.backend.model.Resident existingResident = allResidents.stream()
                        .filter(r -> r.getId().equals(updatedResident.getId()))
                        .findFirst()
                        .orElse(null);

                String pin = existingResident != null && existingResident.getPinHash() != null
                        ? existingResident.getPinHash()
                        : "123456";

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
                List<com.citizencard.backend.model.Resident> backendResidents = service.getAllResidents();
                List<Resident> desktopResidents = ModelConverter.toDesktopResidents(backendResidents);
                if (desktopResidents != null) {
                    table.getItems().addAll(desktopResidents);
                }
            } catch (Exception ex) {
                showAlert("Lỗi", "Lỗi cập nhật: " + ex.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }

    private void showInvoicesPage(StackPane contentArea) {
        VBox content = new VBox(20);
        content.setPadding(new Insets(40));

        Label title = new Label("Quản lý hóa đơn");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        // Form tạo hóa đơn
        VBox form = new VBox(10);
        form.setPadding(new Insets(20));
        form.setStyle("-fx-background-color: #f5f5f5; -fx-border-radius: 5;");

        Label formTitle = new Label("Tạo hóa đơn mới");
        formTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        // Table hiển thị tất cả hóa đơn (khai báo trước để dùng trong lambda)
        TableView<Invoice> invoicesTable = new TableView<>();
        invoicesTable.setPrefHeight(400);

        // ComboBox để chọn resident
        Label residentLabel = new Label("Chọn cư dân:");
        residentLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #34495e;");

        ComboBox<String> residentComboBox = new ComboBox<>();
        residentComboBox.setPromptText("-- Chọn cư dân --");
        residentComboBox.setPrefWidth(300);
        residentComboBox.setStyle("-fx-font-size: 14px;");

        // Load danh sách residents
        try {
            List<com.citizencard.backend.model.Resident> backendResidents = service.getAllResidents();
            for (com.citizencard.backend.model.Resident resident : backendResidents) {
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

        TextField amountField = new TextField();
        amountField.setPromptText("Số tiền (VND)");
        amountField.setPrefWidth(300);

        TextArea descriptionArea = new TextArea();
        descriptionArea.setPromptText("Mô tả");
        descriptionArea.setPrefWidth(300);
        descriptionArea.setPrefRowCount(3);

        Button createBtn = new Button("Tạo hóa đơn");
        createBtn.setPrefWidth(200);

        Label resultLabel = new Label();

        createBtn.setOnAction(e -> {
            try {
                // Lấy resident ID từ ComboBox
                String selectedResident = residentComboBox.getValue();
                if (selectedResident == null || selectedResident.isEmpty()) {
                    resultLabel.setText("❌ Vui lòng chọn cư dân");
                    resultLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 14px;");
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
                    resultLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 14px;");
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
                    resultLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 14px;");
                    return;
                }

                resultLabel.setText("✅ Tạo hóa đơn thành công cho cư dân ID " + residentId + "!");
                resultLabel.setStyle("-fx-text-fill: #2ecc71; -fx-font-size: 14px;");
                // Clear form
                serviceNameField.clear();
                amountField.clear();
                descriptionArea.clear();
                // Refresh table
                refreshInvoicesTable(invoicesTable);
            } catch (NumberFormatException ex) {
                resultLabel.setText("❌ Vui lòng nhập số hợp lệ");
                resultLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 14px;");
            } catch (Exception ex) {
                resultLabel.setText("❌ Lỗi: " + ex.getMessage());
                resultLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 14px;");
            }
        });

        form.getChildren().addAll(formTitle,
                residentLabel, residentComboBox,
                new Label("Tên dịch vụ:"), serviceNameField,
                new Label("Số tiền:"), amountField,
                new Label("Mô tả:"), descriptionArea,
                createBtn, resultLabel);

        // Table hiển thị tất cả hóa đơn (đã khai báo ở trên)
        Label tableTitle = new Label("Danh sách hóa đơn");
        tableTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

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
        refreshTableBtn.setOnAction(e -> refreshInvoicesTable(invoicesTable));

        content.getChildren().addAll(title, form, tableTitle, invoicesTable, refreshTableBtn);
        contentArea.getChildren().clear();
        contentArea.getChildren().add(content);
    }

    private void showParkingPage(StackPane contentArea) {
        VBox content = new VBox(20);
        content.setPadding(new Insets(40));

        Label title = new Label("Quản lý gửi xe");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        // Form đăng ký gửi xe
        VBox form = new VBox(10);
        form.setPadding(new Insets(20));
        form.setStyle("-fx-background-color: #f5f5f5; -fx-border-radius: 5;");

        Label formTitle = new Label("Đăng ký gửi xe");
        formTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        // Table hiển thị danh sách gửi xe (khai báo trước để dùng trong lambda)
        TableView<Parking> parkingTable = new TableView<>();
        parkingTable.setPrefHeight(400);

        // Vì chỉ có 1 user, không cần nhập Resident ID (luôn là 1)
        Label residentIdLabel = new Label("Resident ID: 1 (Mặc định)");
        residentIdLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #34495e;");

        TextField licensePlateField = new TextField();
        licensePlateField.setPromptText("Biển số xe");
        licensePlateField.setPrefWidth(300);

        ComboBox<String> vehicleTypeCombo = new ComboBox<>();
        vehicleTypeCombo.getItems().addAll("MOTORBIKE", "CAR", "BICYCLE");
        vehicleTypeCombo.setValue("MOTORBIKE");
        vehicleTypeCombo.setPrefWidth(300);

        Button registerBtn = new Button("Đăng ký");
        registerBtn.setPrefWidth(200);

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
                    resultLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 14px;");
                    return;
                }

                resultLabel.setText("✅ Đăng ký gửi xe thành công!");
                resultLabel.setStyle("-fx-text-fill: #2ecc71; -fx-font-size: 14px;");
                // Clear form
                licensePlateField.clear();
                vehicleTypeCombo.setValue("MOTORBIKE");
                // Refresh table
                refreshParkingTable(parkingTable);
            } catch (NumberFormatException ex) {
                resultLabel.setText("Vui lòng nhập số hợp lệ");
                resultLabel.setStyle("-fx-text-fill: red;");
            }
        });

        form.getChildren().addAll(formTitle, residentIdLabel,
                new Label("Biển số xe:"), licensePlateField,
                new Label("Loại xe:"), vehicleTypeCombo,
                registerBtn, resultLabel);

        // Table hiển thị danh sách gửi xe (đã khai báo ở trên)
        Label tableTitle = new Label("Danh sách gửi xe");
        tableTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

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
        refreshTableBtn.setOnAction(e -> refreshParkingTable(parkingTable));

        content.getChildren().addAll(title, form, tableTitle, parkingTable, refreshTableBtn);
        contentArea.getChildren().clear();
        contentArea.getChildren().add(content);
    }

    private void refreshParkingTable(TableView<Parking> table) {
        try {
            table.getItems().clear();
            // ✅ Gọi trực tiếp service để lấy danh sách parking
            List<com.citizencard.backend.model.Parking> backendParkings = service.getAllParking();
            List<Parking> desktopParkings = ModelConverter.toDesktopParkings(backendParkings);
            if (desktopParkings != null) {
                table.getItems().addAll(desktopParkings);
            }
        } catch (Exception e) {
            System.err.println("Error refreshing parking table: " + e.getMessage());
        }
    }

    private void refreshInvoicesTable(TableView<Invoice> table) {
        try {
            table.getItems().clear();
            // ✅ Gọi trực tiếp service để lấy danh sách invoices (giờ là Transaction với
            // type=INVOICE)
            List<com.citizencard.backend.model.Transaction> backendInvoices = service.getAllInvoices();
            List<Invoice> desktopInvoices = ModelConverter.transactionsToDesktopInvoices(backendInvoices);
            if (desktopInvoices != null) {
                table.getItems().addAll(desktopInvoices);
            }
        } catch (Exception e) {
            System.err.println("Error refreshing invoices table: " + e.getMessage());
        }
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
