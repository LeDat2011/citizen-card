package com.citizencard.desktop.ui;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import com.citizencard.desktop.model.Resident;
import com.citizencard.desktop.util.ModelConverter;
import com.citizencard.backend.service.CitizenCardService;

public class LoginView {
    private Stage stage;
    private CitizenCardService service;
    private Button residentBtn; // Lưu reference để có thể disable/enable

    public LoginView(Stage stage, CitizenCardService service) {
        this.stage = stage;
        this.service = service;
    }

    public void show() {
        VBox root = new VBox(30);
        root.setPadding(new Insets(60));
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #667eea 0%, #764ba2 100%);");

        VBox titleBox = new VBox(10);
        titleBox.setAlignment(Pos.CENTER);

        Label titleLabel = new Label("🏠 Hệ thống Quản lý Thẻ Cư dân");
        titleLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: white;");

        Label subtitleLabel = new Label("Citizen Card Management System");
        subtitleLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: rgba(255,255,255,0.9);");

        titleBox.getChildren().addAll(titleLabel, subtitleLabel);

        VBox card = new VBox(25);
        card.setPadding(new Insets(40));
        card.setAlignment(Pos.CENTER);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 15; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 20, 0, 0, 5);");
        card.setMaxWidth(400);

        residentBtn = new Button("👤 Đăng nhập Cư dân");
        residentBtn.setPrefWidth(320);
        residentBtn.setPrefHeight(55);
        residentBtn.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; " +
                "-fx-background-color: linear-gradient(to bottom, #3498db, #2980b9); " +
                "-fx-text-fill: white; " +
                "-fx-background-radius: 12; " +
                "-fx-cursor: hand; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(52,152,219,0.4), 8, 0, 0, 2);");

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

        residentBtn.setOnMouseEntered(e -> {
            if (!residentBtn.isDisabled()) {
                ScaleTransition scale = new ScaleTransition(Duration.millis(150), residentBtn);
                scale.setToX(1.05);
                scale.setToY(1.05);
                scale.play();
                residentBtn.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; " +
                        "-fx-background-color: linear-gradient(to bottom, #2980b9, #1f6391); " +
                        "-fx-text-fill: white; " +
                        "-fx-background-radius: 12; " +
                        "-fx-cursor: hand; " +
                        "-fx-effect: dropshadow(three-pass-box, rgba(52,152,219,0.6), 12, 0, 0, 4);");
            }
        });
        residentBtn.setOnMouseExited(e -> {
            if (!residentBtn.isDisabled()) {
                ScaleTransition scale = new ScaleTransition(Duration.millis(150), residentBtn);
                scale.setToX(1.0);
                scale.setToY(1.0);
                scale.play();
                residentBtn.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; " +
                        "-fx-background-color: linear-gradient(to bottom, #3498db, #2980b9); " +
                        "-fx-text-fill: white; " +
                        "-fx-background-radius: 12; " +
                        "-fx-cursor: hand; " +
                        "-fx-effect: dropshadow(three-pass-box, rgba(52,152,219,0.4), 8, 0, 0, 2);");
            }
        });

        Button adminBtn = new Button("🔐 Đăng nhập Admin");
        adminBtn.setPrefWidth(320);
        adminBtn.setPrefHeight(55);
        adminBtn.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; " +
                "-fx-background-color: linear-gradient(to bottom, #e74c3c, #c0392b); " +
                "-fx-text-fill: white; " +
                "-fx-background-radius: 12; " +
                "-fx-cursor: hand; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(231,76,60,0.4), 8, 0, 0, 2);");

        adminBtn.setOnMouseEntered(e -> {
            ScaleTransition scale = new ScaleTransition(Duration.millis(150), adminBtn);
            scale.setToX(1.05);
            scale.setToY(1.05);
            scale.play();
            adminBtn.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; " +
                    "-fx-background-color: linear-gradient(to bottom, #c0392b, #a93226); " +
                    "-fx-text-fill: white; " +
                    "-fx-background-radius: 12; " +
                    "-fx-cursor: hand; " +
                    "-fx-effect: dropshadow(three-pass-box, rgba(231,76,60,0.6), 12, 0, 0, 4);");
        });
        adminBtn.setOnMouseExited(e -> {
            ScaleTransition scale = new ScaleTransition(Duration.millis(150), adminBtn);
            scale.setToX(1.0);
            scale.setToY(1.0);
            scale.play();
            adminBtn.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; " +
                    "-fx-background-color: linear-gradient(to bottom, #e74c3c, #c0392b); " +
                    "-fx-text-fill: white; " +
                    "-fx-background-radius: 12; " +
                    "-fx-cursor: hand; " +
                    "-fx-effect: dropshadow(three-pass-box, rgba(231,76,60,0.4), 8, 0, 0, 2);");
        });

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

        Scene scene = new Scene(root, 500, 600);
        stage.setTitle("Đăng nhập - Citizen Card System");
        stage.setScene(scene);
        stage.show();
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
            residentBtn.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; " +
                    "-fx-background-color: #95a5a6; " +
                    "-fx-text-fill: white; " +
                    "-fx-background-radius: 12; " +
                    "-fx-opacity: 0.6;");
        }
    }

    // Method để kích hoạt lại nút đăng nhập cư dân (sau khi Admin mở khóa)
    public void enableResidentButton() {
        if (residentBtn != null) {
            residentBtn.setDisable(false);
            residentBtn.setText("👤 Đăng nhập Cư dân");
            residentBtn.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; " +
                    "-fx-background-color: linear-gradient(to bottom, #3498db, #2980b9); " +
                    "-fx-text-fill: white; " +
                    "-fx-background-radius: 12; " +
                    "-fx-cursor: hand; " +
                    "-fx-effect: dropshadow(three-pass-box, rgba(52,152,219,0.4), 8, 0, 0, 2);");
        }
    }

    private void loginAsResident() {
        try {
            boolean isBlocked = service.isCardBlocked();
            if (isBlocked) {
                // Vô hiệu hóa nút đăng nhập cư dân
                disableResidentButton();
                showAlert("🔒 Thẻ đã bị khóa",
                        "Thẻ đã bị khóa do nhập sai PIN 5 lần.\n\n" +
                                "Vui lòng đăng nhập Admin để mở khóa thẻ.",
                        Alert.AlertType.ERROR);
                return;
            }

            com.citizencard.backend.model.Resident backendResident = service.loginByCard();

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

        dialog.getDialogPane().setStyle("-fx-background-color: #f8f9fa; " +
                "-fx-background-radius: 15; " +
                "-fx-padding: 20;");

        Label pinLabel = new Label("🔑 Mã PIN:");
        pinLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #2c3e50;");

        Label hintLabel = new Label("(Nhập 6 số)");
        hintLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #95a5a6; -fx-font-style: italic;");

        // Container cho 6 ô nhập PIN
        javafx.scene.layout.HBox pinBox = new javafx.scene.layout.HBox(10);
        pinBox.setAlignment(Pos.CENTER);

        PasswordField[] pinFields = new PasswordField[6];

        for (int i = 0; i < 6; i++) {
            PasswordField pf = new PasswordField();
            pf.setPrefWidth(45);
            pf.setPrefHeight(55);
            pf.setAlignment(Pos.CENTER);
            String baseStyle = "-fx-font-size: 24px; -fx-font-weight: bold; " +
                    "-fx-background-color: white; -fx-alignment: center; -fx-padding: 0; " +
                    "-fx-border-color: #bdc3c7; -fx-border-radius: 8; -fx-background-radius: 8;";
            String activeStyle = "-fx-font-size: 24px; -fx-font-weight: bold; " +
                    "-fx-background-color: white; -fx-alignment: center; -fx-padding: 0; " +
                    "-fx-border-color: #3498db; -fx-border-width: 2; -fx-border-radius: 8; -fx-background-radius: 8;";
            String focusedStyle = activeStyle
                    + "-fx-effect: dropshadow(three-pass-box, rgba(52,152,219,0.3), 5, 0, 0, 0);";

            pf.setStyle(baseStyle);

            final int index = i;

            // Xử lý nhập liệu: chỉ số, tự động chuyển focus
            pf.textProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal.length() > 1) {
                    pf.setText(newVal.substring(0, 1));
                    return;
                }

                if (!newVal.matches("\\d*")) {
                    pf.setText(newVal.replaceAll("[^\\d]", ""));
                    return;
                }

                if (!newVal.isEmpty() && index < 5) {
                    pinFields[index + 1].requestFocus();
                }

                // Update style based on state
                if (pf.isFocused()) {
                    pf.setStyle(focusedStyle);
                } else if (!newVal.isEmpty()) {
                    pf.setStyle(activeStyle);
                } else {
                    pf.setStyle(baseStyle);
                }
            });

            // Xử lý phím Backspace và điều hướng
            pf.setOnKeyPressed(event -> {
                if (event.getCode() == javafx.scene.input.KeyCode.BACK_SPACE) {
                    if (pf.getText().isEmpty() && index > 0) {
                        pinFields[index - 1].requestFocus();
                        pinFields[index - 1].setText(""); // Xóa ký tự ở ô trước đó
                    }
                } else if (event.getCode() == javafx.scene.input.KeyCode.LEFT && index > 0) {
                    pinFields[index - 1].requestFocus();
                } else if (event.getCode() == javafx.scene.input.KeyCode.RIGHT && index < 5) {
                    pinFields[index + 1].requestFocus();
                }
            });

            // Focus style
            pf.focusedProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal) {
                    pf.setStyle(focusedStyle);
                } else {
                    if (pf.getText().isEmpty()) {
                        pf.setStyle(baseStyle);
                    } else {
                        pf.setStyle(activeStyle);
                    }
                }
            });

            pinFields[i] = pf;
            pinBox.getChildren().add(pf);
        }

        Label triesLabel = new Label("");
        triesLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #e74c3c; -fx-font-weight: 600;");

        VBox content = new VBox(12);
        content.setPadding(new Insets(20));
        content.setAlignment(Pos.CENTER);
        content.getChildren().addAll(pinLabel, hintLabel, pinBox, triesLabel);

        dialog.getDialogPane().setContent(content);

        ButtonType loginButtonType = new ButtonType("✅ Đăng nhập", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

        dialog.getDialogPane().lookupButton(loginButtonType).setStyle(
                "-fx-background-color: linear-gradient(to bottom, #3498db, #2980b9); " +
                        "-fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; " +
                        "-fx-padding: 8 20; -fx-cursor: hand;");
        dialog.getDialogPane().lookupButton(ButtonType.CANCEL).setStyle(
                "-fx-background-color: #95a5a6; -fx-text-fill: white; " +
                        "-fx-font-weight: bold; -fx-background-radius: 8; " +
                        "-fx-padding: 8 20; -fx-cursor: hand;");

        // Request focus vào ô đầu tiên khi dialog hiện lên
        javafx.application.Platform.runLater(() -> pinFields[0].requestFocus());

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {
                StringBuilder pinBuilder = new StringBuilder();
                for (PasswordField pf : pinFields) {
                    pinBuilder.append(pf.getText());
                }
                return pinBuilder.toString();
            }
            return null;
        });

        dialog.showAndWait().ifPresent(pin -> {
            try {
                com.citizencard.backend.CardService.PinVerificationResult result = service.verifyPin(null, pin);

                if (result.isValid()) {
                    // PIN correct - login successful
                    // Reset trạng thái nút về enabled (để khi logout sẽ ở trạng thái đúng)
                    enableResidentButton();
                    ResidentDashboard dashboard = new ResidentDashboard(stage, resident, service);
                    dashboard.setCurrentPin(pin);
                    dashboard.show();
                } else if (result.isBlocked()) {
                    // Card is blocked (0 tries remaining)
                    disableResidentButton(); // Vô hiệu hóa nút đăng nhập cư dân
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
            com.citizencard.backend.model.Resident backendResident = service.loginAsAdmin();

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
}
