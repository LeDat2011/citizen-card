package com.citizencard.ui.views;

import javafx.animation.FadeTransition;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import com.citizencard.model.Resident;
import com.citizencard.util.ModelConverter;
import com.citizencard.service.CitizenCardService;
import com.citizencard.ui.components.PinInputComponent;
import com.citizencard.ui.components.NotificationService;
import com.citizencard.ui.components.UITheme;

public class LoginView {
    private Stage stage;
    private CitizenCardService service;
    private Button residentBtn; // Lưu reference để có thể disable/enable
    private StackPane rootLayer;

    public LoginView(Stage stage, CitizenCardService service) {
        this.stage = stage;
        this.service = service;
    }

    public void show() {
        rootLayer = new StackPane();
        rootLayer.setStyle(
                "-fx-background-color: linear-gradient(135deg, #020617 0%, #0b1224 35%, #0ea5e9 120%); " +
                        "-fx-padding: 40;" +
                        "-fx-background-radius: 24;");

        VBox root = new VBox(32);
        root.setPadding(new Insets(68, 60, 68, 60));
        root.setAlignment(Pos.CENTER);

        VBox titleBox = new VBox(10);
        titleBox.setAlignment(Pos.CENTER);

        Label titleLabel = new Label("🏠 Hệ thống Quản lý Thẻ Cư dân");
        titleLabel.setStyle("-fx-font-size: 36px; -fx-font-weight: 800; -fx-text-fill: #f8fafc; " +
                "-fx-background-color: transparent; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(14,165,233,0.45), 25, 0, 0, 6);");

        Label subtitleLabel = new Label("Citizen Card Management System");
        subtitleLabel.setStyle("-fx-font-size: 17px; -fx-text-fill: rgba(255,255,255,0.9); " +
                "-fx-background-color: transparent; " +
                "-fx-font-weight: 600;" +
                "-fx-letter-spacing: 0.3px;");

        titleBox.getChildren().addAll(titleLabel, subtitleLabel);

        VBox card = new VBox(28);
        card.setPadding(new Insets(54));
        card.setAlignment(Pos.CENTER);
        card.setStyle("-fx-background-color: rgba(15,23,42,0.86); -fx-background-radius: 28; " +
                "-fx-border-radius: 28; -fx-border-color: rgba(255,255,255,0.08); -fx-border-width: 1.2; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(8,47,73,0.45), 32, 0, 0, 14);" +
                "-fx-backdrop-filter: blur(12px);");
        card.setMaxWidth(520);

        residentBtn = new Button("👤 Đăng nhập Cư dân");
        residentBtn.setPrefWidth(370);
        residentBtn.setPrefHeight(64);
        UITheme.applyAccentButton(residentBtn);

        // Kiểm tra trạng thái khóa thẻ ngay khi hiển thị màn hình login
        try {
            if (service.selectAppletOnce()) {
                boolean isBlocked = service.isCardBlocked();
                if (isBlocked) {
                    disableResidentButton();
                } else {
                    // Đảm bảo nút ở trạng thái enabled nếu thẻ không bị khóa
                    enableResidentButton();
                }
            }
        } catch (Exception ex) {
            // Nếu không kết nối được thẻ, vẫn cho phép click nút (sẽ báo lỗi sau)
            // Giữ nút ở trạng thái enabled
        }

        Button adminBtn = new Button("🔐 Đăng nhập Admin");
        adminBtn.setPrefWidth(370);
        adminBtn.setPrefHeight(64);
        UITheme.applyDangerButton(adminBtn);

        residentBtn.setOnAction(e -> {
            try {
                if (service.selectAppletOnce()) {
                    loginAsResident();
                } else {
                    showAlert("Lỗi", "Không thể kết nối với thẻ. Vui lòng kiểm tra JCIDE và terminal.",
                            Alert.AlertType.ERROR);
                }
            } catch (Exception ex) {
                showAlert("Lỗi", "Không thể kết nối với thẻ: " + ex.getMessage(), Alert.AlertType.ERROR);
            }
        });

        adminBtn.setOnAction(e -> {
            try {
                if (service.selectAppletOnce()) {
                    loginAsAdmin();
                } else {
                    showAlert("Lỗi", "Không thể kết nối với thẻ. Vui lòng kiểm tra JCIDE và terminal.",
                            Alert.AlertType.ERROR);
                }
            } catch (Exception ex) {
                showAlert("Lỗi", "Không thể kết nối với thẻ: " + ex.getMessage(), Alert.AlertType.ERROR);
            }
        });

        card.getChildren().addAll(residentBtn, adminBtn);

        root.getChildren().addAll(titleBox, card);
        rootLayer.getChildren().add(root);
        StackPane.setAlignment(root, Pos.CENTER);

        Scene scene = new Scene(rootLayer, 580, 680);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);

        // Load global CSS
        try {
            String css = getClass().getResource("/css/styles.css").toExternalForm();
            scene.getStylesheets().add(css);
        } catch (Exception e) {
            System.err.println("❌ Error loading CSS: " + e.getMessage());
        }

        stage.setTitle("Đăng nhập - Citizen Card System");
        stage.setScene(scene);
        stage.show();

        // Fade in animation
        FadeTransition fadeIn = new FadeTransition(Duration.millis(500), rootLayer);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();
    }

    // Method public để refresh trạng thái nút (gọi khi Admin logout sau khi mở
    // khóa)
    public void refreshButtonState() {
        try {
            if (service.selectAppletOnce()) {
                boolean isBlocked = service.isCardBlocked();
                if (isBlocked) {
                    disableResidentButton();
                } else {
                    enableResidentButton();
                }
            }
        } catch (Exception ex) {
            // Nếu không kết nối được thẻ, giữ nguyên trạng thái hiện tại
        }
    }

    // Method để vô hiệu hóa nút đăng nhập cư dân
    private void disableResidentButton() {
        if (residentBtn != null) {
            residentBtn.setDisable(true);
            residentBtn.setText("🔒 Thẻ đã bị khóa");
            residentBtn.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; " +
                    "-fx-background-color: linear-gradient(to bottom, #95a5a6, #7f8c8d); " +
                    "-fx-text-fill: white; -fx-background-radius: 12; -fx-opacity: 0.7;");
            showLockNotification("Thẻ đã bị khóa vì nhập sai PIN quá số lần cho phép");
        }
    }

    // Method để kích hoạt lại nút đăng nhập cư dân (sau khi Admin mở khóa)
    public void enableResidentButton() {
        if (residentBtn != null) {
            residentBtn.setDisable(false);
            residentBtn.setText("👤 Đăng nhập Cư dân");
            UITheme.applyAccentButton(residentBtn);
        }
    }

    private void loginAsResident() {
        try {
            boolean isBlocked = service.isCardBlocked();
            if (isBlocked) {
                // Vô hiệu hóa nút đăng nhập cư dân
                disableResidentButton();
                showLockNotification("Thẻ đã bị khóa. Vui lòng nhờ Admin mở khóa.");
                showAlert("🔒 Thẻ đã bị khóa",
                        "Thẻ đã bị khóa do nhập sai PIN 5 lần.\n\n" +
                                "Vui lòng đăng nhập Admin để mở khóa thẻ.",
                        Alert.AlertType.ERROR);
                return;
            }

            com.citizencard.model.Resident backendResident = service.loginByCard();

            if (backendResident != null) {
                Resident resident = ModelConverter.toDesktopResident(backendResident);
                showPinDialog(resident);
            } else {
                showAlert("Lỗi", "Không tìm thấy cư dân trong database", Alert.AlertType.ERROR);
            }
        } catch (Exception e) {
            String errorMsg = e.getMessage();
            String helpText = "Vui lòng kiểm tra:\n" +
                    "1. JCIDE đang chạy và terminal đã được mở\n" +
                    "2. Thẻ đã được đưa vào terminal";

            showAlert("Lỗi", "Không thể đăng nhập: " + errorMsg + "\n\n" + helpText, Alert.AlertType.ERROR);
        }
    }

    private void showPinDialog(Resident resident) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("🔐 Xác thực PIN");
        dialog.setHeaderText("Nhập mã PIN");
        dialog.initStyle(StageStyle.UTILITY);
        dialog.initModality(Modality.APPLICATION_MODAL);

        Label pinLabel = new Label("🔑 Mã PIN:");
        pinLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Label hintLabel = new Label("(Nhập 6 số)");
        hintLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #7f8c8d; -fx-font-style: italic;");

        PinInputComponent pinInput = new PinInputComponent();

        Label triesLabel = new Label("");
        triesLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #e74c3c; -fx-font-weight: 600;");

        VBox content = new VBox(12);
        content.setPadding(new Insets(20));
        content.setAlignment(Pos.CENTER);
        content.getChildren().addAll(pinLabel, hintLabel, pinInput, triesLabel);

        dialog.getDialogPane().setContent(content);

        ButtonType loginButtonType = new ButtonType("✅ Đăng nhập", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);
        UITheme.styleDialogPane(dialog.getDialogPane());

        Button loginButton = (Button) dialog.getDialogPane().lookupButton(loginButtonType);
        loginButton.addEventFilter(ActionEvent.ACTION, event -> {
            if (!pinInput.isComplete()) {
                triesLabel.setText("⚠️ Vui lòng nhập đủ 6 số PIN");
                event.consume();
            }
        });

        // Request focus vào ô đầu tiên khi dialog hiện lên
        javafx.application.Platform.runLater(pinInput::requestFocus);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {
                return pinInput.getPin();
            }
            return null;
        });

        dialog.showAndWait().ifPresent(pin -> {
            try {
                com.citizencard.card.CardService.PinVerificationResult result = service.verifyPin(null, pin);

                if (result.isValid()) {
                    // PIN correct - login successful
                    // Reset trạng thái nút về enabled (để khi logout sẽ ở trạng thái đúng)
                    enableResidentButton();
                    ResidentDashboard dashboard = new ResidentDashboard(stage, resident, service);
                    dashboard.show();
                } else if (result.isBlocked()) {
                    // Card is blocked (0 tries remaining)
                    disableResidentButton(); // Vô hiệu hóa nút đăng nhập cư dân
                    showLockNotification("Thẻ đã bị khóa vì nhập sai PIN nhiều lần.");
                    showAlert("🔒 Thẻ đã bị khóa",
                            "Thẻ đã bị khóa do nhập sai PIN 5 lần.\n\n" +
                                    "Vui lòng đăng nhập Admin để mở khóa thẻ.",
                            Alert.AlertType.ERROR);
                } else {
                    // PIN incorrect but still has tries remaining
                    byte triesRemaining = result.getTriesRemaining();
                    if (triesRemaining > 0) {
                        // Still has tries - show error and close dialog
                        // User must click "Đăng nhập Cư dân" button again to retry
                        String message = "❌ Mã PIN không đúng!\n\n" +
                                "Bạn còn " + triesRemaining + " lần thử.\n\n" +
                                "Vui lòng nhấn nút 'Đăng nhập Cư dân' để thử lại.";
                        showAlert("❌ PIN không đúng", message, Alert.AlertType.WARNING);
                    } else {
                        // No tries remaining - card is now blocked
                        disableResidentButton(); // Vô hiệu hóa nút đăng nhập cư dân
                        String message = "🔒 Thẻ đã bị khóa!\n\n" +
                                "Bạn đã nhập sai PIN 5 lần.\n\n" +
                                "Vui lòng đăng nhập Admin để mở khóa thẻ.";
                        showLockNotification("Thẻ đã bị khóa. Vui lòng liên hệ Admin để mở khóa.");
                        showAlert("🔒 Thẻ đã bị khóa", message, Alert.AlertType.ERROR);
                    }
                }
            } catch (Exception e) {
                String errorMsg = e.getMessage();
                String helpText = "Vui lòng kiểm tra:\n" +
                        "1. JCIDE đang chạy và terminal đã được mở\n" +
                        "2. Thẻ đã được đưa vào terminal\n" +
                        "3. PIN đã được thiết lập (đăng nhập Admin để đặt PIN)";

                if (errorMsg != null && errorMsg.contains("Card not initialized")) {
                    helpText = "Thẻ chưa được khởi tạo!\n\n" +
                            "Vui lòng:\n" +
                            "1. Đăng nhập bằng Admin\n" +
                            "2. Sử dụng chức năng 'Khởi tạo thẻ' để tạo thẻ mới\n" +
                            "3. Sử dụng chức năng 'Đổi PIN thẻ' để đặt PIN";
                }

                showAlert("Lỗi", "Lỗi xác thực PIN: " + errorMsg + "\n\n" + helpText, Alert.AlertType.ERROR);
            }
        });
    }

    private void loginAsAdmin() {
        try {
            com.citizencard.model.Resident backendResident = service.loginAsAdmin();

            if (backendResident != null) {
                AdminDashboard dashboard = new AdminDashboard(stage, service);
                dashboard.show();
            } else {
                showAlert("Lỗi", "Không thể đăng nhập Admin", Alert.AlertType.ERROR);
            }
        } catch (Exception e) {
            String errorMsg = e.getMessage();
            String helpText = "Vui lòng kiểm tra:\n" +
                    "1. JCIDE đang chạy và terminal đã được mở\n" +
                    "2. Thẻ đã được đưa vào terminal";

            if (errorMsg != null && errorMsg.contains("terminal")) {
                helpText = "Vui lòng kiểm tra:\n" +
                        "1. JCIDE đang chạy\n" +
                        "2. Terminal đã được mở trong JCIDE\n" +
                        "3. Thẻ đã được đưa vào terminal";
            }

            showAlert("Lỗi", "Không thể kết nối với thẻ: " + errorMsg + "\n\n" + helpText, Alert.AlertType.ERROR);
        }
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initStyle(StageStyle.UTILITY);
        alert.initModality(Modality.APPLICATION_MODAL);

        String icon = "";
        String bgColor = "";
        String textColor = "";

        if (type == Alert.AlertType.ERROR) {
            icon = "❌ ";
            bgColor = "#fee";
            textColor = "#c0392b";
        } else if (type == Alert.AlertType.INFORMATION) {
            icon = "ℹ️ ";
            bgColor = "#e8f4f8";
            textColor = "#2980b9";
        } else if (type == Alert.AlertType.WARNING) {
            icon = "⚠️ ";
            bgColor = "#fff8e1";
            textColor = "#f39c12";
        } else {
            icon = "✅ ";
            bgColor = "#e8f5e9";
            textColor = "#27ae60";
        }

        alert.setTitle(icon + title);
        alert.getDialogPane().setStyle("-fx-background-color: " + bgColor + "; " +
                "-fx-background-radius: 15; " +
                "-fx-padding: 20;");
        alert.getDialogPane().lookup(".content.label").setStyle(
                "-fx-text-fill: " + textColor + "; " +
                        "-fx-font-size: 14px; " +
                        "-fx-font-weight: 500;");

        alert.getDialogPane().lookupButton(ButtonType.OK).setStyle(
                "-fx-background-color: linear-gradient(to bottom, #3498db, #2980b9); " +
                        "-fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; " +
                        "-fx-padding: 8 20; -fx-cursor: hand;");

        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), alert.getDialogPane());
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);

        alert.setOnShown(e -> fadeIn.play());
        alert.showAndWait();
    }

    private void showLockNotification(String message) {
        if (rootLayer != null) {
            NotificationService.showNotification(rootLayer, message, "error", Pos.CENTER);
        }
    }
}
