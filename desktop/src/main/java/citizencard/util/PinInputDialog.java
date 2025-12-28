package citizencard.util;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Professional PIN Input Dialog
 * 
 * 4 separate input boxes for each digit with auto-focus navigation
 */
public class PinInputDialog extends Dialog<String> {

    private PasswordField[] pinFields;
    private Label statusLabel;
    private Button confirmButton;
    private String title;
    private String headerText;
    private boolean isChangePin;

    public PinInputDialog(String title, String headerText) {
        this(title, headerText, false);
    }

    public PinInputDialog(String title, String headerText, boolean isChangePin) {
        this.title = title;
        this.headerText = headerText;
        this.isChangePin = isChangePin;

        setupDialog();
        createContent();
        setupEventHandlers();
    }

    private void setupDialog() {
        setTitle(title);
        setHeaderText(headerText);
        setResizable(false);

        // Add CSS styling
        getDialogPane().getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        getDialogPane().getStyleClass().add("pin-dialog");

        // Force center dialog on screen - multiple attempts
        setOnShowing(event -> centerDialog());
        setOnShown(event -> {
            centerDialog();
            // Force center again after a short delay
            javafx.application.Platform.runLater(() -> {
                try {
                    Thread.sleep(50);
                    javafx.application.Platform.runLater(() -> centerDialog());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        });
    }

    private void centerDialog() {
        try {
            javafx.stage.Window window = getDialogPane().getScene().getWindow();
            if (window != null && window instanceof javafx.stage.Stage) {
                javafx.stage.Stage stage = (javafx.stage.Stage) window;

                // Get screen bounds
                javafx.stage.Screen screen = javafx.stage.Screen.getPrimary();
                javafx.geometry.Rectangle2D bounds = screen.getVisualBounds();

                // Calculate center position
                double centerX = bounds.getMinX() + (bounds.getWidth() - stage.getWidth()) / 2;
                double centerY = bounds.getMinY() + (bounds.getHeight() - stage.getHeight()) / 2;

                // Set position
                stage.setX(centerX);
                stage.setY(centerY);

                System.out.println("[DEBUG] PIN dialog centered at: " + centerX + ", " + centerY);
            }
        } catch (Exception e) {
            System.err.println("[WARN] Could not center dialog: " + e.getMessage());
        }
    }

    private void createContent() {
        VBox mainContainer = new VBox(15);
        mainContainer.setPadding(new Insets(20, 25, 20, 25));
        mainContainer.setAlignment(Pos.CENTER);
        mainContainer.setPrefWidth(350);
        mainContainer.setMaxWidth(350);

        // PIN input section
        VBox pinSection = createPinInputSection();

        // Status label
        statusLabel = new Label();
        statusLabel.getStyleClass().add("pin-status");
        statusLabel.setVisible(false);

        // Instructions
        Label instructionLabel = createInstructionLabel();

        mainContainer.getChildren().addAll(pinSection, statusLabel, instructionLabel);
        getDialogPane().setContent(mainContainer);

        // Buttons
        ButtonType confirmButtonType = new ButtonType("Xác nhận", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Hủy", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(confirmButtonType, cancelButtonType);

        // Get confirm button reference
        confirmButton = (Button) getDialogPane().lookupButton(confirmButtonType);
        confirmButton.setDisable(true);
        confirmButton.getStyleClass().addAll("btn", "btn-primary");

        Button cancelButton = (Button) getDialogPane().lookupButton(cancelButtonType);
        cancelButton.getStyleClass().addAll("btn", "btn-outline");

        // Result converter
        setResultConverter(dialogButton -> {
            if (dialogButton == confirmButtonType) {
                return getPinValue();
            }
            return null;
        });
    }

    private VBox createPinInputSection() {
        VBox pinSection = new VBox(15);
        pinSection.setAlignment(Pos.CENTER);

        Label pinLabel = new Label("Nhập mã PIN (4 chữ số):");
        pinLabel.getStyleClass().add("pin-label");
        pinLabel.setFont(Font.font("System", FontWeight.BOLD, 14));

        // PIN input boxes
        HBox pinInputBox = createPinInputBoxes();

        pinSection.getChildren().addAll(pinLabel, pinInputBox);
        return pinSection;
    }

    private HBox createPinInputBoxes() {
        HBox pinInputBox = new HBox(10);
        pinInputBox.setAlignment(Pos.CENTER);

        pinFields = new PasswordField[4];

        for (int i = 0; i < 4; i++) {
            PasswordField pinField = new PasswordField();
            pinField.setPrefWidth(50);
            pinField.setPrefHeight(50);
            pinField.setAlignment(Pos.CENTER);
            pinField.getStyleClass().add("pin-field");
            pinField.setFont(Font.font("System", FontWeight.BOLD, 24));

            // Limit to 1 character and numbers only
            pinField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal.matches("\\d*")) {
                    pinField.setText(oldVal);
                } else if (newVal.length() > 1) {
                    pinField.setText(newVal.substring(0, 1));
                }

                // Auto-focus next field
                if (newVal.length() == 1) {
                    focusNextField(pinField);
                }

                updateConfirmButton();
                clearStatus();
            });

            pinFields[i] = pinField;
            pinInputBox.getChildren().add(pinField);
        }

        return pinInputBox;
    }

    private Label createInstructionLabel() {
        Label instructionLabel = new Label();
        instructionLabel.getStyleClass().add("pin-instruction");
        instructionLabel.setWrapText(true);
        instructionLabel.setMaxWidth(320);

        if (isChangePin) {
            instructionLabel.setText(
                    "🔐 Nhập mã PIN hiện tại\n" +
                            "• Số 0-9, tự động chuyển ô");
        } else {
            instructionLabel.setText(
                    "🔐 Nhập mã PIN 4 chữ số\n" +
                            "• Số 0-9, tự động chuyển ô");
        }

        return instructionLabel;
    }

    private void setupEventHandlers() {
        for (int i = 0; i < pinFields.length; i++) {
            final int index = i;
            TextField field = pinFields[i];

            // Key event handling
            field.setOnKeyPressed(event -> handleKeyPressed(event, index));

            // Focus handling
            field.focusedProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal) {
                    Platform.runLater(() -> field.selectAll());
                }
            });
        }

        // Focus first field when dialog opens
        Platform.runLater(() -> pinFields[0].requestFocus());
    }

    private void handleKeyPressed(KeyEvent event, int fieldIndex) {
        KeyCode code = event.getCode();
        TextField currentField = pinFields[fieldIndex];

        if (code == KeyCode.BACK_SPACE) {
            if (currentField.getText().isEmpty() && fieldIndex > 0) {
                // Move to previous field and clear it
                pinFields[fieldIndex - 1].clear();
                pinFields[fieldIndex - 1].requestFocus();
            }
        } else if (code == KeyCode.LEFT && fieldIndex > 0) {
            pinFields[fieldIndex - 1].requestFocus();
        } else if (code == KeyCode.RIGHT && fieldIndex < 3) {
            pinFields[fieldIndex + 1].requestFocus();
        } else if (code == KeyCode.ENTER) {
            if (isPinComplete()) {
                confirmButton.fire();
            }
        } else if (code.isDigitKey()) {
            // Clear field before new input
            currentField.clear();
        }
    }

    private void focusNextField(PasswordField currentField) {
        for (int i = 0; i < pinFields.length - 1; i++) {
            if (pinFields[i] == currentField) {
                final int nextIndex = i + 1;
                Platform.runLater(() -> pinFields[nextIndex].requestFocus());
                break;
            }
        }
    }

    private void updateConfirmButton() {
        confirmButton.setDisable(!isPinComplete());
    }

    private boolean isPinComplete() {
        for (PasswordField field : pinFields) {
            if (field.getText().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private String getPinValue() {
        StringBuilder pin = new StringBuilder();
        for (PasswordField field : pinFields) {
            pin.append(field.getText());
        }
        return pin.toString();
    }

    public void showError(String message) {
        statusLabel.setText("❌ " + message);
        statusLabel.getStyleClass().removeAll("pin-status-success");
        statusLabel.getStyleClass().add("pin-status-error");
        statusLabel.setVisible(true);

        // Shake animation effect
        shakeFields();

        // Clear all fields
        clearAllFields();
    }

    public void showSuccess(String message) {
        statusLabel.setText("✅ " + message);
        statusLabel.getStyleClass().removeAll("pin-status-error");
        statusLabel.getStyleClass().add("pin-status-success");
        statusLabel.setVisible(true);
    }

    private void clearStatus() {
        statusLabel.setVisible(false);
    }

    private void clearAllFields() {
        for (PasswordField field : pinFields) {
            field.clear();
        }
        Platform.runLater(() -> pinFields[0].requestFocus());
        updateConfirmButton();
    }

    private void shakeFields() {
        // Simple shake effect by adding/removing CSS class
        for (PasswordField field : pinFields) {
            field.getStyleClass().add("pin-field-error");

            // Remove error class after animation
            Platform.runLater(() -> {
                try {
                    Thread.sleep(500);
                    Platform.runLater(() -> field.getStyleClass().remove("pin-field-error"));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
    }

    /**
     * Static method to show PIN input dialog
     */
    public static String showPinDialog(String title, String headerText) {
        PinInputDialog dialog = new PinInputDialog(title, headerText);
        return dialog.showAndWait().orElse(null);
    }

    /**
     * Static method to show PIN change dialog
     */
    public static String showChangePinDialog(String title, String headerText) {
        PinInputDialog dialog = new PinInputDialog(title, headerText, true);
        return dialog.showAndWait().orElse(null);
    }

    /**
     * Show PIN verification with retry logic
     */
    public static String showPinVerification(String title, String headerText, int maxRetries) {
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            PinInputDialog dialog = new PinInputDialog(title,
                    headerText + (attempt > 1 ? "\n⚠️ Lần thử " + attempt + "/" + maxRetries : ""));

            String pin = dialog.showAndWait().orElse(null);
            if (pin != null) {
                return pin;
            }

            // User cancelled
            if (attempt < maxRetries) {
                Alert retryAlert = new Alert(Alert.AlertType.CONFIRMATION);
                retryAlert.setTitle("Thử lại?");
                retryAlert.setHeaderText("Bạn có muốn thử lại?");
                retryAlert.setContentText("Còn " + (maxRetries - attempt) + " lần thử.");

                if (retryAlert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
                    break;
                }
            }
        }

        return null;
    }
}