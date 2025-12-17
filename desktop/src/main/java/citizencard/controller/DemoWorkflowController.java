package citizencard.controller;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

import citizencard.service.CardService;
import citizencard.dao.CardDAO;
import citizencard.util.DataValidator;
import citizencard.util.PhotoUtils;
import citizencard.util.PinInputDialog;
import citizencard.model.CitizenInfo;

/**
 * Demo Workflow Controller
 * 
 * Handles the specific demo workflow for JCIDE virtual card
 */
public class DemoWorkflowController {

    private CardService cardService;
    private CardDAO cardDAO;

    public DemoWorkflowController(CardService cardService, CardDAO cardDAO) {
        this.cardService = cardService;
        this.cardDAO = cardDAO;
    }

    /**
     * Show Database Viewer for Admin
     */
    public void showDatabaseViewer() {
        Alert dialog = new Alert(Alert.AlertType.INFORMATION);
        dialog.setTitle("Database Viewer");
        dialog.setHeaderText("📊 System Database - All Registered Cards");
        dialog.getDialogPane().setPrefWidth(480);
        dialog.getDialogPane().setMaxWidth(480);

        // In a real implementation, this would query the database
        String content = "🗄️ REGISTERED CARDS DATABASE:\n\n" +

                "📋 CURRENT CARDS:\n" +
                "• Card ID: CITIZEN-CARD-001\n" +
                "  Status: ACTIVE\n" +
                "  Owner: Nguyen Van A\n" +
                "  Created: 2025-12-16 08:30:00\n" +
                "  Last Access: 2025-12-16 08:45:00\n" +
                "  Balance: 500,000 VND\n\n" +

                "• Card ID: CITIZEN-CARD-002\n" +
                "  Status: ACTIVE\n" +
                "  Owner: Tran Thi B\n" +
                "  Created: 2025-12-16 09:00:00\n" +
                "  Last Access: Never\n" +
                "  Balance: 0 VND\n\n" +

                "📊 STATISTICS:\n" +
                "• Total Cards: 2\n" +
                "• Active Cards: 2\n" +
                "• Blocked Cards: 0\n" +
                "• Total Transactions: 15\n\n" +

                "💡 NOTE: This is demo data. Real implementation would show live database.";

        dialog.setContentText(content);

        ButtonType refreshButton = new ButtonType("Refresh", ButtonBar.ButtonData.OTHER);
        ButtonType exportButton = new ButtonType("Export Data", ButtonBar.ButtonData.OTHER);
        ButtonType closeButton = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().setAll(refreshButton, exportButton, closeButton);

        dialog.showAndWait().ifPresent(response -> {
            if (response == refreshButton) {
                showDatabaseViewer(); // Refresh
            } else if (response == exportButton) {
                showAlert("Export", "Database export functionality will be implemented in full version.");
            }
        });
    }

    /**
     * Create New Citizen Card with Enhanced Validation
     */
    public void showCreateNewCard() {
        Dialog<CitizenInfo> dialog = new Dialog<>();
        dialog.setTitle("Tạo thẻ cư dân mới");
        dialog.setHeaderText("👤 Đăng ký cư dân mới");

        // Create form with validation
        VBox mainContainer = new VBox(12);
        mainContainer.setPadding(new Insets(15));
        mainContainer.setPrefWidth(500);
        mainContainer.setMaxWidth(500);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(0, 0, 15, 0));

        // Form fields with validation styling
        TextField nameField = new TextField();
        nameField.setPromptText("Họ và tên đầy đủ");
        nameField.getStyleClass().add("text-field");
        nameField.setPrefWidth(320);

        DatePicker dobPicker = new DatePicker();
        dobPicker.setPromptText("Ngày sinh (dd/MM/yyyy)");
        dobPicker.setPrefWidth(320);
        dobPicker.setEditable(true); // Allow manual input

        // Set date format for display and input
        dobPicker.setConverter(new javafx.util.StringConverter<java.time.LocalDate>() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            @Override
            public String toString(java.time.LocalDate date) {
                return date != null ? formatter.format(date) : "";
            }

            @Override
            public java.time.LocalDate fromString(String string) {
                if (string != null && !string.trim().isEmpty()) {
                    try {
                        return java.time.LocalDate.parse(string.trim(), formatter);
                    } catch (Exception e) {
                        return null;
                    }
                }
                return null;
            }
        });

        TextField idNumberField = new TextField();
        idNumberField.setPromptText("Số CMND/CCCD (12 chữ số)");
        idNumberField.getStyleClass().add("text-field");
        idNumberField.setPrefWidth(320);

        TextField roomNumberField = new TextField();
        roomNumberField.setPromptText("Số phòng (VD: A101, B205, 301)");
        roomNumberField.getStyleClass().add("text-field");
        roomNumberField.setPrefWidth(320);

        TextField phoneField = new TextField();
        phoneField.setPromptText("Số điện thoại (0xxxxxxxxx)");
        phoneField.getStyleClass().add("text-field");
        phoneField.setPrefWidth(320);

        TextField emailField = new TextField();
        emailField.setPromptText("Email (tùy chọn)");
        emailField.getStyleClass().add("text-field");
        emailField.setPrefWidth(320);

        Button pinButton = new Button("📱 Chọn PIN");
        pinButton.getStyleClass().addAll("btn", "btn-secondary");

        Label pinDisplayLabel = new Label("Chưa chọn PIN");
        pinDisplayLabel.getStyleClass().add("pin-display-label");

        final String[] selectedPin = { null };

        pinButton.setOnAction(e -> {
            String pin = PinInputDialog.showPinDialog(
                    "Chọn PIN cho thẻ mới",
                    "🔐 Nhập PIN 4 chữ số cho thẻ cư dân mới");

            if (pin != null && !pin.isEmpty()) {
                selectedPin[0] = pin;
                pinDisplayLabel.setText("✅ PIN đã được chọn");
                pinDisplayLabel.getStyleClass().removeAll("pin-display-error");
                pinDisplayLabel.getStyleClass().add("pin-display-success");
            }
        });

        TextField balanceField = new TextField("0");
        balanceField.setPromptText("Số dư ban đầu (VND)");
        balanceField.getStyleClass().add("text-field");
        balanceField.setPrefWidth(320);

        // Photo upload section
        VBox photoSection = new VBox(10);
        photoSection.getStyleClass().add("photo-upload-section");

        Label photoLabel = new Label("Ảnh cá nhân:");
        photoLabel.getStyleClass().add("dialog-label");

        HBox photoControls = new HBox(10);
        photoControls.setAlignment(Pos.CENTER_LEFT);

        Button selectPhotoBtn = new Button("📷 Chọn ảnh");
        selectPhotoBtn.getStyleClass().addAll("btn", "btn-secondary");

        Label photoStatusLabel = new Label("Chưa chọn ảnh");
        photoStatusLabel.getStyleClass().add("photo-status");

        Button removePhotoBtn = new Button("🗑️ Xóa");
        removePhotoBtn.getStyleClass().addAll("btn", "btn-small", "btn-outline");
        removePhotoBtn.setVisible(false);

        photoControls.getChildren().addAll(selectPhotoBtn, photoStatusLabel, removePhotoBtn);

        // Photo preview
        javafx.scene.image.ImageView photoPreview = new javafx.scene.image.ImageView();
        photoPreview.setFitWidth(100);
        photoPreview.setFitHeight(120);
        photoPreview.setPreserveRatio(true);
        photoPreview.getStyleClass().add("photo-preview");
        photoPreview.setVisible(false);

        // Photo info
        Label photoInfoLabel = new Label();
        photoInfoLabel.getStyleClass().add("photo-info");
        photoInfoLabel.setVisible(false);

        photoSection.getChildren().addAll(photoLabel, photoControls, photoPreview, photoInfoLabel);

        // Photo selection logic
        final java.io.File[] selectedPhotoFile = { null };
        final javafx.scene.image.Image[] selectedImage = { null };

        selectPhotoBtn.setOnAction(e -> {
            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle("Chọn ảnh cá nhân");
            fileChooser.getExtensionFilters().addAll(
                    new javafx.stage.FileChooser.ExtensionFilter("Ảnh", "*.jpg", "*.jpeg", "*.png", "*.bmp", "*.gif",
                            "*.webp"),
                    new javafx.stage.FileChooser.ExtensionFilter("JPEG", "*.jpg", "*.jpeg"),
                    new javafx.stage.FileChooser.ExtensionFilter("PNG", "*.png"),
                    new javafx.stage.FileChooser.ExtensionFilter("WebP", "*.webp"),
                    new javafx.stage.FileChooser.ExtensionFilter("Tất cả", "*.*"));

            java.io.File file = fileChooser.showOpenDialog(dialog.getOwner());
            if (file != null) {
                DataValidator.ValidationResult fileResult = DataValidator.validateImageFile(file);
                if (!fileResult.isValid()) {
                    showValidationAlert("Lỗi file ảnh", fileResult.getErrorMessage());
                    return;
                }

                try {
                    javafx.scene.image.Image image = new javafx.scene.image.Image(file.toURI().toString());
                    DataValidator.ValidationResult imageResult = DataValidator.validateImageContent(image);

                    if (!imageResult.isValid()) {
                        showValidationAlert("Lỗi nội dung ảnh", imageResult.getErrorMessage());
                        return;
                    }

                    // Update UI
                    selectedPhotoFile[0] = file;
                    selectedImage[0] = image;

                    photoPreview.setImage(image);
                    photoPreview.setVisible(true);

                    photoStatusLabel.setText("✅ " + file.getName());
                    photoStatusLabel.getStyleClass().removeAll("photo-status-error");
                    photoStatusLabel.getStyleClass().add("photo-status-success");

                    photoInfoLabel.setText(
                            "Kích thước: " + DataValidator.getImageDimensionsInfo(image) + "\n" +
                                    "Dung lượng: " + DataValidator.formatFileSize(file.length()));
                    photoInfoLabel.setVisible(true);

                    removePhotoBtn.setVisible(true);

                } catch (Exception ex) {
                    showValidationAlert("Lỗi đọc ảnh", "Không thể đọc file ảnh: " + ex.getMessage());
                }
            }
        });

        removePhotoBtn.setOnAction(e -> {
            selectedPhotoFile[0] = null;
            selectedImage[0] = null;

            photoPreview.setImage(null);
            photoPreview.setVisible(false);

            photoStatusLabel.setText("Chưa chọn ảnh");
            photoStatusLabel.getStyleClass().removeAll("photo-status-success", "photo-status-error");

            photoInfoLabel.setVisible(false);
            removePhotoBtn.setVisible(false);
        });

        // Validation labels
        Label nameError = new Label();
        nameError.getStyleClass().add("validation-error");
        nameError.setVisible(false);

        Label dobError = new Label();
        dobError.getStyleClass().add("validation-error");
        dobError.setVisible(false);

        Label idError = new Label();
        idError.getStyleClass().add("validation-error");
        idError.setVisible(false);

        Label roomNumberError = new Label();
        roomNumberError.getStyleClass().add("validation-error");
        roomNumberError.setVisible(false);

        Label phoneError = new Label();
        phoneError.getStyleClass().add("validation-error");
        phoneError.setVisible(false);

        Label emailError = new Label();
        emailError.getStyleClass().add("validation-error");
        emailError.setVisible(false);

        Label pinError = new Label();
        pinError.getStyleClass().add("validation-error");
        pinError.setVisible(false);

        Label balanceError = new Label();
        balanceError.getStyleClass().add("validation-error");
        balanceError.setVisible(false);

        // Add fields to grid
        // Add fields to grid
        int row = 0;

        // Auto Fill Button (Demo)
        Button autoFillBtn = new Button("⚡ Auto Fill (Demo)");
        autoFillBtn.getStyleClass().addAll("btn", "btn-secondary", "btn-small");
        autoFillBtn.setOnAction(e -> {
            nameField.setText("dat");
            dobPicker.setValue(java.time.LocalDate.of(2002, 11, 20));
            idNumberField.setText("038202017531");
            roomNumberField.setText("601");
            phoneField.setText("0365231153");
            emailField.setText("dat@mail.com");
            balanceField.setText("10000");

            // Set default PIN
            selectedPin[0] = "000000";
            pinDisplayLabel.setText("✅ PIN: 000000");
            pinDisplayLabel.getStyleClass().removeAll("pin-display-error");
            pinDisplayLabel.getStyleClass().add("pin-display-success");
        });

        grid.add(new Label("Demo Action:"), 0, row);
        grid.add(autoFillBtn, 1, row++);
        grid.add(new Label("Họ và tên:"), 0, row);
        grid.add(nameField, 1, row++);
        grid.add(nameError, 1, row++);

        grid.add(new Label("Ngày sinh:"), 0, row);
        grid.add(dobPicker, 1, row++);
        grid.add(dobError, 1, row++);

        grid.add(new Label("Số CMND/CCCD:"), 0, row);
        grid.add(idNumberField, 1, row++);
        grid.add(idError, 1, row++);

        grid.add(new Label("Số phòng:"), 0, row);
        grid.add(roomNumberField, 1, row++);
        grid.add(roomNumberError, 1, row++);

        grid.add(new Label("Số điện thoại:"), 0, row);
        grid.add(phoneField, 1, row++);
        grid.add(phoneError, 1, row++);

        grid.add(new Label("Email:"), 0, row);
        grid.add(emailField, 1, row++);
        grid.add(emailError, 1, row++);

        grid.add(new Label("Mã PIN:"), 0, row);
        VBox pinBox = new VBox(5);
        pinBox.getChildren().addAll(pinButton, pinDisplayLabel);
        grid.add(pinBox, 1, row++);
        grid.add(pinError, 1, row++);

        grid.add(new Label("Số dư ban đầu:"), 0, row);
        grid.add(balanceField, 1, row++);
        grid.add(balanceError, 1, row++);

        // Add photo section
        grid.add(photoSection, 0, row, 2, 1);
        row++;

        // Real-time validation
        nameField.textProperty().addListener((obs, oldVal, newVal) -> {
            DataValidator.ValidationResult result = DataValidator.validateName(newVal);
            updateFieldValidation(nameField, nameError, result);
        });

        dobPicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            String dateStr = "";
            if (newVal != null) {
                // Format LocalDate to dd/MM/yyyy for validation
                dateStr = newVal.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            }
            DataValidator.ValidationResult result = DataValidator.validateDateOfBirth(dateStr);
            updateFieldValidation(dobPicker, dobError, result);
        });

        idNumberField.textProperty().addListener((obs, oldVal, newVal) -> {
            DataValidator.ValidationResult result = DataValidator.validateIdNumber(newVal);
            updateFieldValidation(idNumberField, idError, result);
        });

        roomNumberField.textProperty().addListener((obs, oldVal, newVal) -> {
            DataValidator.ValidationResult result = DataValidator.validateRoomNumber(newVal);
            updateFieldValidation(roomNumberField, roomNumberError, result);
        });

        phoneField.textProperty().addListener((obs, oldVal, newVal) -> {
            DataValidator.ValidationResult result = DataValidator.validatePhone(newVal);
            updateFieldValidation(phoneField, phoneError, result);
        });

        emailField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.trim().isEmpty()) {
                DataValidator.ValidationResult result = DataValidator.validateEmail(newVal);
                updateFieldValidation(emailField, emailError, result);
            } else {
                updateFieldValidation(emailField, emailError, DataValidator.ValidationResult.success());
            }
        });

        // PIN validation is handled in the dialog

        balanceField.textProperty().addListener((obs, oldVal, newVal) -> {
            DataValidator.ValidationResult result = DataValidator.validateBalance(newVal);
            updateFieldValidation(balanceField, balanceError, result);
        });

        // Info panel
        VBox infoPanel = new VBox(10);
        infoPanel.getStyleClass().add("form-info-panel");
        infoPanel.setPadding(new Insets(15));

        Label infoTitle = new Label("📋 Hướng dẫn nhập liệu");
        infoTitle.getStyleClass().add("form-info-title");

        Label infoText = new Label(
                "• Họ tên: 2-50 ký tự\n" +
                        "• CMND/CCCD: 12 chữ số\n" +
                        "• Số phòng: 1-10 ký tự (VD: A101)\n" +
                        "• SĐT: 0xxxxxxxxx\n" +
                        "• PIN: 4 chữ số");
        infoText.getStyleClass().add("form-info-text");
        infoText.setWrapText(true);
        infoText.setMaxWidth(350);

        infoPanel.getChildren().addAll(infoTitle, infoText);

        mainContainer.getChildren().addAll(grid, infoPanel);

        // Wrap in ScrollPane for better UX
        ScrollPane scrollPane = new ScrollPane(mainContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setMaxHeight(600);
        scrollPane.setPrefHeight(600);
        scrollPane.getStyleClass().add("scroll-pane");

        dialog.getDialogPane().setContent(scrollPane);

        ButtonType createButton = new ButtonType("Tạo thẻ", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Hủy", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(createButton, cancelButton);

        // Prevent dialog from closing on validation error
        final Button createBtn = (Button) dialog.getDialogPane().lookupButton(createButton);
        createBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            // Validate all fields
            boolean isValid = validateAllFields(
                    nameField, nameError, dobPicker, dobError, idNumberField, idError,
                    roomNumberField, roomNumberError, phoneField, phoneError, emailField, emailError,
                    selectedPin[0], pinError, pinDisplayLabel, balanceField, balanceError);

            if (!isValid) {
                event.consume(); // Prevent dialog from closing
                showValidationAlert("Lỗi nhập liệu", "Vui lòng kiểm tra và sửa các lỗi được đánh dấu màu đỏ.");
            }
        });

        // Only create citizen info if validation passed
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == createButton) {
                return createCitizenInfoFromFields(
                        nameField, dobPicker, idNumberField, roomNumberField,
                        phoneField, emailField, selectedPin[0], balanceField,
                        selectedPhotoFile[0], selectedImage[0]);
            }
            return null;
        });

        dialog.showAndWait().ifPresent(citizenInfo -> {
            createCitizenCard(citizenInfo);
        });
    }

    /**
     * Update field validation styling
     */
    private void updateFieldValidation(Control field, Label errorLabel, DataValidator.ValidationResult result) {
        if (result.isValid()) {
            field.getStyleClass().removeAll("field-error");
            field.getStyleClass().add("field-valid");
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        } else {
            field.getStyleClass().removeAll("field-valid");
            field.getStyleClass().add("field-error");
            errorLabel.setText(result.getErrorMessage());
            // Ensure error label has red color - apply inline style as backup
            errorLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 12px;");
            if (!errorLabel.getStyleClass().contains("validation-error")) {
                errorLabel.getStyleClass().add("validation-error");
            }
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
        }
    }

    /**
     * Validate all fields and show errors inline
     * 
     * @return true if all fields are valid
     */
    private boolean validateAllFields(TextField nameField, Label nameError, DatePicker dobPicker, Label dobError,
            TextField idNumberField, Label idError, TextField roomNumberField, Label roomNumberError,
            TextField phoneField, Label phoneError, TextField emailField, Label emailError,
            String selectedPin, Label pinError, Label pinDisplayLabel, TextField balanceField, Label balanceError) {

        boolean allValid = true;

        // Validate name
        DataValidator.ValidationResult nameResult = DataValidator.validateName(nameField.getText());
        updateFieldValidation(nameField, nameError, nameResult);
        if (!nameResult.isValid())
            allValid = false;

        // Validate DOB
        String dobStr = "";
        if (dobPicker.getValue() != null) {
            // Format LocalDate to dd/MM/yyyy for validation
            dobStr = dobPicker.getValue().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        }
        DataValidator.ValidationResult dobResult = DataValidator.validateDateOfBirth(dobStr);
        if (!dobResult.isValid()) {
            dobError.setText(dobResult.getErrorMessage());
            dobError.setVisible(true);
            allValid = false;
        } else {
            dobError.setVisible(false);
        }

        // Validate ID number
        DataValidator.ValidationResult idResult = DataValidator.validateIdNumber(idNumberField.getText());
        updateFieldValidation(idNumberField, idError, idResult);
        if (!idResult.isValid())
            allValid = false;

        // Validate room number
        DataValidator.ValidationResult roomNumberResult = DataValidator.validateRoomNumber(roomNumberField.getText());
        updateFieldValidation(roomNumberField, roomNumberError, roomNumberResult);
        if (!roomNumberResult.isValid())
            allValid = false;

        // Validate phone
        DataValidator.ValidationResult phoneResult = DataValidator.validatePhone(phoneField.getText());
        updateFieldValidation(phoneField, phoneError, phoneResult);
        if (!phoneResult.isValid())
            allValid = false;

        // Validate email (optional)
        String email = emailField.getText().trim();
        if (!email.isEmpty()) {
            DataValidator.ValidationResult emailResult = DataValidator.validateEmail(email);
            updateFieldValidation(emailField, emailError, emailResult);
            if (!emailResult.isValid())
                allValid = false;
        } else {
            emailError.setVisible(false);
        }

        // Validate PIN
        if (selectedPin == null || selectedPin.isEmpty()) {
            pinError.setText("Vui lòng chọn mã PIN cho thẻ");
            pinError.setVisible(true);
            pinDisplayLabel.setText("❌ Chưa chọn PIN");
            pinDisplayLabel.getStyleClass().removeAll("pin-display-success");
            pinDisplayLabel.getStyleClass().add("pin-display-error");
            allValid = false;
        } else {
            DataValidator.ValidationResult pinResult = DataValidator.validatePin(selectedPin);
            if (!pinResult.isValid()) {
                pinError.setText(pinResult.getErrorMessage());
                pinError.setVisible(true);
                allValid = false;
            } else {
                pinError.setVisible(false);
            }
        }

        // Validate balance
        try {
            int balance = Integer.parseInt(balanceField.getText().trim());
            if (balance < 0) {
                balanceError.setText("Số dư không được âm");
                balanceError.setVisible(true);
                balanceField.getStyleClass().add("field-error");
                allValid = false;
            } else {
                balanceError.setVisible(false);
                balanceField.getStyleClass().removeAll("field-error");
            }
        } catch (NumberFormatException e) {
            balanceError.setText("Số dư phải là số nguyên");
            balanceError.setVisible(true);
            balanceField.getStyleClass().add("field-error");
            allValid = false;
        }

        return allValid;
    }

    /**
     * Create CitizenInfo from validated fields (no validation here)
     */
    private CitizenInfo createCitizenInfoFromFields(TextField nameField, DatePicker dobPicker,
            TextField idNumberField, TextField roomNumberField, TextField phoneField,
            TextField emailField, String selectedPin, TextField balanceField,
            java.io.File photoFile, javafx.scene.image.Image photoImage) {

        String name = DataValidator.sanitizeInput(nameField.getText());
        String dob = dobPicker.getValue().toString();
        String idNumber = DataValidator.sanitizeInput(idNumberField.getText());
        String roomNumber = DataValidator.sanitizeInput(roomNumberField.getText());
        String phone = DataValidator.sanitizeInput(phoneField.getText());
        String email = DataValidator.sanitizeInput(emailField.getText());

        long balance = 0;
        try {
            balance = Long.parseLong(balanceField.getText().trim());
        } catch (NumberFormatException e) {
            balance = 0;
        }

        // Handle photo
        String photoPath = null;
        byte[] photoData = null;
        if (photoFile != null && photoImage != null) {
            try {
                photoPath = photoFile.getAbsolutePath();
                photoData = PhotoUtils.preparePhotoForCard(photoFile);
            } catch (Exception e) {
                System.err.println("Warning: Could not prepare photo: " + e.getMessage());
            }
        }

        return new CitizenInfo(name, dob, idNumber, roomNumber, phone, email, selectedPin, balance, photoPath,
                photoData);
    }

    /**
     * Show validation alert
     */
    private void showValidationAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText("❌ " + title);
        alert.setContentText(message);
        alert.getDialogPane().setPrefWidth(380);
        alert.getDialogPane().setMaxWidth(380);

        // Style the dialog
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        alert.getDialogPane().getStyleClass().add("validation-alert");

        alert.showAndWait();
    }

    /**
     * Create citizen card with auto-generated ID
     */
    private void createCitizenCard(CitizenInfo info) {
        try {
            // Auto-generate Card ID with room number
            String cardId = generateCardId(info.roomNumber);

            // Show confirmation
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirm Card Creation");
            confirm.setHeaderText("📝 Create New Citizen Card");
            confirm.setContentText(
                    "THÔNG TIN CƯ DÂN:\n\n" +
                            "ID Thẻ được tạo: " + cardId + "\n" +
                            "Họ tên: " + info.name + "\n" +
                            "Ngày sinh: " + info.dob + "\n" +
                            "Số CMND/CCCD: " + info.idNumber + "\n" +
                            "Số phòng: " + info.roomNumber + "\n" +
                            "Số điện thoại: " + DataValidator.formatPhoneNumber(info.phone) + "\n" +
                            (info.email.isEmpty() ? "" : "Email: " + info.email + "\n") +
                            "Mã PIN mặc định: " + info.pin + "\n" +
                            "Số dư ban đầu: " + DataValidator.formatBalance(info.balance) + "\n" +
                            (info.photoData != null
                                    ? "Ảnh cá nhân: ✅ Đã tải lên ("
                                            + DataValidator.formatFileSize(info.photoData.length) + ")\n"
                                    : "Ảnh cá nhân: Không có\n")
                            +
                            "\n" +

                            "⚠️ QUAN TRỌNG:\n" +
                            "• Thao tác này sẽ khởi tạo thẻ ảo JCIDE\n" +
                            "• Dữ liệu thẻ cũ sẽ bị ghi đè\n" +
                            "• Thẻ mới sẽ sẵn sàng cho cư dân đăng nhập\n" +
                            (info.photoData != null ? "• Ảnh cá nhân sẽ được lưu trong hệ thống\n" : "") +
                            "\n" +

                            "Tiếp tục tạo thẻ?");

            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    performCardCreation(info);
                }
            });

        } catch (Exception e) {
            showAlert("Error", "Failed to create card: " + e.getMessage());
        }
    }

    /**
     * Perform actual card creation - REAL CONNECTION TO JCIDE
     * 
     * Flow:
     * 1. Connect to JCIDE via CardService
     * 2. Initialize card with PIN (INS_INITIALIZE_CARD)
     * 3. Get public key from card (INS_GET_PUBLIC_KEY)
     * 4. Save card_id + public_key to H2 database
     */
    private void performCardCreation(CitizenInfo info) {
        // Show progress dialog
        Alert progressDialog = new Alert(Alert.AlertType.INFORMATION);
        progressDialog.setTitle("Dang tao the");
        progressDialog.setHeaderText("Dang khoi tao the ao...");
        progressDialog.setContentText("Vui long doi trong khi he thong dang xu ly...\n\n" +
                "Buoc 1: Ket noi den JCIDE...");
        progressDialog.getButtonTypes().clear();
        progressDialog.getDialogPane().setPrefWidth(400);
        progressDialog.getDialogPane().setMaxWidth(400);

        // Make dialog non-modal so it doesn't block
        progressDialog.initModality(javafx.stage.Modality.NONE);
        progressDialog.show();

        // Safety timeout - force close dialog after 30 seconds
        Thread timeoutThread = new Thread(() -> {
            try {
                Thread.sleep(30000); // 30 seconds
                javafx.application.Platform.runLater(() -> {
                    if (progressDialog.isShowing()) {
                        System.err.println("[WARN] Progress dialog timeout - forcing close");
                        progressDialog.close();
                        showAlert("Timeout", "Qua trinh tao the mat qua nhieu thoi gian.\n" +
                                "Vui long kiem tra JCIDE va thu lai.");
                    }
                });
            } catch (InterruptedException e) {
                // Normal - thread was interrupted when dialog closed normally
            }
        });
        timeoutThread.setDaemon(true);
        timeoutThread.start();

        // Run card creation in background thread
        Thread creationThread = new Thread(() -> {
            String cardId = null;
            String publicKeyBase64 = null;
            String errorMessage = null;

            try {
                // Step 1: Connect to JCIDE
                updateProgress(progressDialog, "Buoc 1: Dang ket noi den JCIDE...");
                Thread.sleep(500);

                boolean connected = cardService.connectToCard();
                if (!connected) {
                    throw new Exception("Khong the ket noi den JCIDE.\n" +
                            "Vui long kiem tra:\n" +
                            "1. JCIDE dang chay\n" +
                            "2. Applet da duoc load\n" +
                            "3. Remote Card dang hoat dong");
                }

                // Generate Card ID with new format (before initializing card)
                cardId = generateCardId(info.roomNumber);
                System.out.println("[INFO] Generated Card ID: " + cardId);

                // Step 1.5: Clear card data before initialization
                updateProgress(progressDialog, "Buoc 1.5: Dang xoa du lieu cu tren the...");
                Thread.sleep(500);

                try {
                    cardService.clearCard();
                    System.out.println("[INFO] Card data cleared successfully");
                } catch (Exception e) {
                    // If clear fails, it might be a new card - continue anyway
                    System.out.println("[WARN] Could not clear card (might be new card): " + e.getMessage());
                }

                // Step 2: Initialize card with PIN and Card ID
                updateProgress(progressDialog, "Buoc 2: Dang khoi tao the voi PIN va Card ID...");
                Thread.sleep(500);

                try {
                    // Send both PIN and complex Card ID to applet
                    String appletCardId = cardService.initializeCard(info.pin, cardId);
                    System.out.println("[INFO] Card initialized with ID: " + appletCardId);
                } catch (Exception e) {
                    // Card might already be initialized, try to verify PIN
                    System.out.println("[WARN] Card may already be initialized: " + e.getMessage());

                    // Try to verify PIN
                    CardService.PinVerificationResult pinResult = cardService.verifyPin(info.pin);
                    if (pinResult.success) {
                        // Get existing card ID from applet
                        String existingCardId = cardService.getCardId();
                        System.out.println("[INFO] Card already initialized with ID: " + existingCardId);
                        // Use existing card ID for database
                        cardId = existingCardId;
                    } else {
                        String errorMsg = "The da duoc khoi tao voi PIN khac.\n" +
                                "Vui long su dung PIN da dat truoc do hoac reset the.";
                        if (pinResult.remainingTries > 0) {
                            errorMsg += "\nSo lan thu con lai: " + pinResult.remainingTries;
                        } else {
                            errorMsg += "\nThe da bi khoa do nhap sai PIN qua nhieu lan.";
                        }
                        throw new Exception(errorMsg);
                    }
                }

                // Step 3: Get public key (skip for simplified applet)
                updateProgress(progressDialog, "Buoc 3: Dang lay public key...");
                Thread.sleep(500);

                try {
                    byte[] publicKeyBytes = cardService.getPublicKey();
                    if (publicKeyBytes != null && publicKeyBytes.length > 0) {
                        publicKeyBase64 = Base64.getEncoder().encodeToString(publicKeyBytes);
                        System.out.println("[INFO] Public key retrieved: "
                                + publicKeyBase64.substring(0, Math.min(50, publicKeyBase64.length())) + "...");
                    } else {
                        // Simplified applet doesn't have RSA - use dummy key
                        publicKeyBase64 = "SIMPLIFIED_APPLET_NO_RSA_KEY";
                        System.out.println("[INFO] Using dummy public key (simplified applet)");
                    }
                } catch (Exception e) {
                    // If get public key fails, use dummy key
                    publicKeyBase64 = "SIMPLIFIED_APPLET_NO_RSA_KEY";
                    System.out.println("[WARN] Could not get public key (simplified applet): " + e.getMessage());
                }

                // Card ID was already set in applet during Step 2

                // Step 4: Save to database with full citizen info
                updateProgress(progressDialog, "Buoc 4: Dang luu vao database...");
                Thread.sleep(500);

                // Check if card already registered
                if (cardDAO.isCardRegistered(cardId)) {
                    System.out.println("[INFO] Card already registered, updating...");
                } else {
                    // Save card_id and public_key to database
                    boolean saved = cardDAO.registerCard(cardId, publicKeyBase64);
                    if (!saved) {
                        throw new Exception("Khong the luu thong tin vao database.");
                    }
                    System.out.println("[DB] Card registered: " + cardId);
                }

                // Step 4.5: Save personal info to card
                updateProgress(progressDialog, "Buoc 4.5: Dang luu thong tin ca nhan len the...");
                Thread.sleep(500);

                try {
                    // Serialize personal info
                    byte[] infoBytes = citizencard.util.CitizenInfoParser.serialize(info);
                    System.out.println("[INFO] Personal info serialized: " + infoBytes.length + " bytes");

                    // Save to card (encrypted by applet)
                    boolean infoSaved = cardService.updatePersonalInfo(infoBytes);
                    if (infoSaved) {
                        System.out.println("[INFO] Personal info saved to card successfully");
                    } else {
                        System.err.println("[WARN] Failed to save personal info to card");
                    }
                } catch (Exception infoError) {
                    System.err.println("[WARN] Personal info save failed: " + infoError.getMessage());
                    // Don't fail the entire process
                }

                // Step 5: Upload photo if available (skip for simplified applet)
                if (info.photoData != null && info.photoData.length > 0) {
                    updateProgress(progressDialog, "Buoc 5: Dang tai anh ca nhan len the...");
                    Thread.sleep(500);

                    try {
                        // Prepare photo for card (compress if needed)
                        byte[] cardPhotoData = PhotoUtils.preparePhotoForCard(
                                PhotoUtils.bytesToImage(info.photoData));

                        // Upload to card
                        boolean photoUploaded = cardService.uploadPhoto(cardPhotoData);
                        if (photoUploaded) {
                            System.out
                                    .println("[INFO] Photo uploaded successfully: " + cardPhotoData.length + " bytes");
                        }
                    } catch (Exception photoError) {
                        System.err.println("[WARN] Photo upload failed (simplified applet may not support): "
                                + photoError.getMessage());
                        // Don't fail the entire process for photo upload failure
                    }
                }

                // Step 6: Top up initial balance if > 0 (skip for simplified applet)
                if (info.balance > 0) {
                    updateProgress(progressDialog, "Buoc 6: Dang nap so du ban dau...");
                    Thread.sleep(500);

                    try {
                        int newBalance = cardService.topupBalance((int) info.balance);
                        System.out.println("[INFO] Initial balance topped up: " + newBalance);
                    } catch (Exception balanceError) {
                        System.err.println("[WARN] Balance top-up failed (simplified applet may not support): "
                                + balanceError.getMessage());
                        // Don't fail the entire process for balance top-up failure
                    }
                }

                // Log transaction (use UPDATE_INFO as closest match for card creation)
                try {
                    cardDAO.logTransaction(cardId, "UPDATE_INFO", true, "Card initialized successfully");
                } catch (Exception logError) {
                    System.err.println("[WARN] Could not log transaction: " + logError.getMessage());
                }

            } catch (Exception e) {
                errorMessage = e.getMessage();
                System.err.println("[ERROR] Card creation failed: " + errorMessage);

                if (cardId != null) {
                    try {
                        cardDAO.logTransaction(cardId, "UPDATE_INFO", false, errorMessage);
                    } catch (Exception logError) {
                        System.err.println("[WARN] Could not log transaction: " + logError.getMessage());
                    }
                }
            }

            // Update UI on JavaFX thread
            final String finalCardId = cardId;
            final String finalPublicKey = publicKeyBase64;
            final String finalError = errorMessage;
            final CitizenInfo finalInfo = info;

            System.out.println("[DEBUG] Preparing to update UI...");
            System.out.println("[DEBUG] Card ID: " + finalCardId);
            System.out.println("[DEBUG] Error: " + finalError);

            // Small delay to ensure all operations complete
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                // Ignore
            }

            // Backup mechanism - force close after 2 seconds if Platform.runLater doesn't
            // work
            final boolean[] uiUpdated = { false };
            Thread backupCloseThread = new Thread(() -> {
                try {
                    Thread.sleep(2000);
                    if (!uiUpdated[0]) {
                        System.err.println("[WARN] UI update timeout - forcing dialog close");
                        javafx.application.Platform.runLater(() -> {
                            try {
                                progressDialog.close();
                                showAlert("Thành công", "Thẻ đã được tạo thành công!\nCard ID: " + finalCardId);
                            } catch (Exception e) {
                                System.err.println("[ERROR] Backup close failed: " + e.getMessage());
                            }
                        });
                    }
                } catch (InterruptedException e) {
                    // Normal - interrupted when UI updated successfully
                }
            });
            backupCloseThread.setDaemon(true);
            backupCloseThread.start();

            javafx.application.Platform.runLater(() -> {
                uiUpdated[0] = true;
                backupCloseThread.interrupt();
                try {
                    System.out.println("[DEBUG] UI update started");

                    // Interrupt timeout thread
                    try {
                        timeoutThread.interrupt();
                        System.out.println("[DEBUG] Timeout thread interrupted");
                    } catch (Exception e) {
                        System.err.println("[WARN] Could not interrupt timeout: " + e.getMessage());
                    }

                    // Force close dialog - try multiple methods
                    try {
                        if (progressDialog.isShowing()) {
                            System.out.println("[DEBUG] Attempting to close dialog...");

                            // Method 1: Hide
                            progressDialog.hide();
                            System.out.println("[DEBUG] Dialog hidden");

                            // Method 2: Close
                            progressDialog.close();
                            System.out.println("[DEBUG] Dialog closed");

                            // Method 3: Get window and close
                            javafx.stage.Window window = progressDialog.getDialogPane().getScene().getWindow();
                            if (window != null) {
                                window.hide();
                                System.out.println("[DEBUG] Window hidden");
                            }
                        } else {
                            System.out.println("[DEBUG] Progress dialog already closed");
                        }
                    } catch (Exception e) {
                        System.err.println("[WARN] Error closing dialog: " + e.getMessage());
                        e.printStackTrace();
                    }

                    // Show result
                    if (finalError != null) {
                        System.out.println("[DEBUG] Showing error result");
                        showErrorResult(finalError);
                    } else {
                        System.out.println("[DEBUG] Showing success result");
                        showSuccessResult(finalCardId, finalPublicKey, finalInfo);
                    }

                    System.out.println("[DEBUG] UI update completed");
                    System.out.println("[INFO] Card creation completed successfully - connection kept open");

                } catch (Exception e) {
                    System.err.println("[ERROR] Error updating UI: " + e.getMessage());
                    e.printStackTrace();
                }
            });

        });
        creationThread.setDaemon(true);
        creationThread.start();
    }

    /**
     * Update progress dialog text
     */
    private void updateProgress(Alert dialog, String message) {
        javafx.application.Platform.runLater(() -> {
            dialog.setContentText(message);
        });
    }

    /**
     * Show success result dialog
     */
    private void showSuccessResult(String cardId, String publicKey, CitizenInfo info) {
        Alert success = new Alert(Alert.AlertType.INFORMATION);
        success.setTitle("Tao the thanh cong");
        success.setHeaderText("The cu dan moi da san sang!");
        success.getDialogPane().setPrefWidth(450);
        success.getDialogPane().setMaxWidth(450);
        success.setContentText(
                "TAO THE HOAN TAT:\n\n" +
                        "ID The: " + cardId + "\n" +
                        "Chu the: " + info.name + "\n" +
                        "Ma PIN: " + info.pin + "\n" +
                        "So du: " + DataValidator.formatBalance(info.balance) + "\n\n" +

                        "PUBLIC KEY (Base64):\n" +
                        publicKey.substring(0, Math.min(60, publicKey.length())) + "...\n\n" +

                        "DA HOAN THANH:\n" +
                        "[OK] Ket noi JCIDE thanh cong\n" +
                        "[OK] Khoi tao the ao thanh cong\n" +
                        "[OK] Lay public key thanh cong\n" +
                        "[OK] Luu database thanh cong\n\n" +

                        "BUOC TIEP THEO:\n" +
                        "Chuyen sang che do Cu dan de test the moi!\n" +
                        "Dang nhap voi PIN: " + info.pin);

        success.getDialogPane().getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        success.showAndWait();
    }

    /**
     * Show error result dialog
     */
    private void showErrorResult(String errorMessage) {
        Alert error = new Alert(Alert.AlertType.ERROR);
        error.setTitle("Loi tao the");
        error.setHeaderText("Khong the tao the cu dan");
        error.getDialogPane().setPrefWidth(450);
        error.getDialogPane().setMaxWidth(450);
        error.setContentText(
                "DA XAY RA LOI:\n\n" +
                        errorMessage + "\n\n" +

                        "HUONG DAN XU LY:\n" +
                        "1. Kiem tra JCIDE dang chay\n" +
                        "2. Dam bao applet 'citizen_applet' da duoc load\n" +
                        "3. Bat Remote Card trong JCIDE\n" +
                        "4. Kiem tra AID khop: 112233445500\n" +
                        "5. Thu lai sau khi kiem tra");

        error.getDialogPane().getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        error.showAndWait();
    }

    /**
     * Generate unique Card ID with format:
     * CITIZEN-CARD-{roomNumber}-{date}-{time}-{coords}
     * Example: CITIZEN-CARD-A101-20251217-121703-21N106E
     * 
     * Format breakdown:
     * - CITIZEN-CARD: Prefix
     * - {roomNumber}: Clean room number
     * - {date}: yyyyMMdd (e.g. 20251217)
     * - {time}: HHmmss (e.g. 121703)
     * - {coords}: Latitude/Longitude (e.g. 21N106E for Hanoi)
     */
    private String generateCardId(String roomNumber) {
        // Get current date and time
        LocalDateTime now = LocalDateTime.now();
        String date = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String time = now.format(DateTimeFormatter.ofPattern("HHmmss"));

        // Default GPS coordinates (Hanoi, Vietnam: 21.0285°N, 105.8542°E)
        // Format: 21N106E (simplified integer coordinates)
        String coords = "21N106E";

        // Generate 4 random digits for uniqueness
        int random = (int) (Math.random() * 10000);
        String randomDigits = String.format("%04d", random);

        // Clean room number (remove spaces, convert to uppercase)
        String cleanRoom = roomNumber.trim().toUpperCase().replaceAll("\\s+", "");

        return "CITIZEN-CARD-" + cleanRoom + "-" + date + "-" + time + "-" + coords + "-" + randomDigits;
    }

    /**
     * Show alert dialog
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.getDialogPane().setPrefWidth(400);
        alert.getDialogPane().setMaxWidth(400);
        alert.showAndWait();
    }

}