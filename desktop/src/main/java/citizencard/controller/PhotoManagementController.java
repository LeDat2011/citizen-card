package citizencard.controller;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import java.io.File;

import citizencard.service.CardService;
import citizencard.util.PhotoUtils;
import citizencard.util.DataValidator;

/**
 * Photo Management Controller
 * 
 * Handles photo upload/download to/from smart card
 */
public class PhotoManagementController {

    private CardService cardService;

    public PhotoManagementController(CardService cardService) {
        this.cardService = cardService;
    }

    /**
     * Show photo management dialog
     */
    public void showPhotoManagement() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Quản lý ảnh cá nhân");
        dialog.setHeaderText("📷 Quản lý ảnh trong thẻ thông minh");

        VBox mainContainer = new VBox(15);
        mainContainer.setPadding(new Insets(15));
        mainContainer.setPrefWidth(400);
        mainContainer.setMaxWidth(400);

        // Current photo display
        VBox photoSection = createPhotoDisplaySection();

        // Action buttons
        HBox actionButtons = createActionButtons(photoSection);

        // Info panel
        VBox infoPanel = createInfoPanel();

        mainContainer.getChildren().addAll(photoSection, actionButtons, new Separator(), infoPanel);
        dialog.getDialogPane().setContent(mainContainer);

        ButtonType closeButton = new ButtonType("Đóng", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(closeButton);

        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        dialog.showAndWait();
    }

    private VBox createPhotoDisplaySection() {
        VBox photoSection = new VBox(10);
        photoSection.setAlignment(Pos.CENTER);
        photoSection.getStyleClass().add("photo-display-section");

        Label titleLabel = new Label("Ảnh hiện tại trong thẻ");
        titleLabel.getStyleClass().add("section-title");

        // Photo display
        ImageView photoView = new ImageView();
        photoView.setFitWidth(120);
        photoView.setFitHeight(150);
        photoView.setPreserveRatio(true);
        photoView.getStyleClass().add("photo-display");

        Label statusLabel = new Label("Đang tải...");
        statusLabel.getStyleClass().add("photo-status");

        // Load current photo from card
        loadCurrentPhoto(photoView, statusLabel);

        photoSection.getChildren().addAll(titleLabel, photoView, statusLabel);
        return photoSection;
    }

    private HBox createActionButtons(VBox photoSection) {
        HBox actionButtons = new HBox(15);
        actionButtons.setAlignment(Pos.CENTER);

        Button uploadBtn = new Button("📤 Tải ảnh lên thẻ");
        uploadBtn.getStyleClass().addAll("btn", "btn-primary");
        uploadBtn.setOnAction(e -> uploadPhotoToCard(photoSection));

        Button downloadBtn = new Button("📥 Tải ảnh từ thẻ");
        downloadBtn.getStyleClass().addAll("btn", "btn-secondary");
        downloadBtn.setOnAction(e -> downloadPhotoFromCard());

        Button refreshBtn = new Button("🔄 Làm mới");
        refreshBtn.getStyleClass().addAll("btn", "btn-outline");
        refreshBtn.setOnAction(e -> refreshPhotoDisplay(photoSection));

        actionButtons.getChildren().addAll(uploadBtn, downloadBtn, refreshBtn);
        return actionButtons;
    }

    private VBox createInfoPanel() {
        VBox infoPanel = new VBox(10);
        infoPanel.getStyleClass().add("info-panel");
        infoPanel.setPadding(new Insets(15));

        Label infoTitle = new Label("📋 Thông tin quan trọng");
        infoTitle.getStyleClass().add("info-title");

        Label infoText = new Label(
                "• Tối đa 8KB, tự động nén\n" +
                        "• Định dạng: JPEG, PNG\n" +
                        "• Lưu trực tiếp trong thẻ");
        infoText.getStyleClass().add("info-text");
        infoText.setWrapText(true);

        infoPanel.getChildren().addAll(infoTitle, infoText);
        return infoPanel;
    }

    private void loadCurrentPhoto(ImageView photoView, Label statusLabel) {
        Thread loadThread = new Thread(() -> {
            try {
                if (!cardService.isConnected()) {
                    javafx.application.Platform.runLater(() -> {
                        statusLabel.setText("❌ Chưa kết nối thẻ");
                        statusLabel.getStyleClass().add("status-error");
                    });
                    return;
                }

                // Add timeout protection - max 30 seconds
                final byte[][] photoDataHolder = { null };
                final Exception[] exceptionHolder = { null };

                Thread downloadThread = new Thread(() -> {
                    try {
                        photoDataHolder[0] = cardService.downloadPhoto();
                    } catch (Exception e) {
                        exceptionHolder[0] = e;
                    }
                });
                downloadThread.setDaemon(true);
                downloadThread.start();

                // Wait with timeout
                downloadThread.join(30000); // 30 second timeout

                if (downloadThread.isAlive()) {
                    // Timeout occurred
                    downloadThread.interrupt();
                    javafx.application.Platform.runLater(() -> {
                        statusLabel.setText("⏱️ Hết thời gian chờ - thử lại sau");
                        statusLabel.getStyleClass().add("status-error");
                    });
                    return;
                }

                if (exceptionHolder[0] != null) {
                    throw exceptionHolder[0];
                }

                byte[] photoData = photoDataHolder[0];

                javafx.application.Platform.runLater(() -> {
                    if (photoData != null && photoData.length > 0) {
                        Image image = PhotoUtils.bytesToImage(photoData);
                        if (image != null) {
                            photoView.setImage(image);
                            statusLabel.setText("✅ " + PhotoUtils.getPhotoInfo(photoData));
                            statusLabel.getStyleClass().removeAll("status-error");
                            statusLabel.getStyleClass().add("status-success");
                        } else {
                            statusLabel.setText("❌ Lỗi đọc ảnh");
                            statusLabel.getStyleClass().add("status-error");
                        }
                    } else {
                        photoView.setImage(null);
                        statusLabel.setText("📷 Chưa có ảnh trong thẻ");
                        statusLabel.getStyleClass().removeAll("status-error", "status-success");
                    }
                });

            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    statusLabel.setText("❌ Lỗi: " + e.getMessage());
                    statusLabel.getStyleClass().add("status-error");
                });
            }
        });
        loadThread.setDaemon(true); // Allow app to exit even if thread is running
        loadThread.start();
    }

    private void uploadPhotoToCard(VBox photoSection) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn ảnh để tải lên thẻ");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Ảnh", "*.jpg", "*.jpeg", "*.png", "*.bmp", "*.gif", "*.webp"),
                new FileChooser.ExtensionFilter("JPEG", "*.jpg", "*.jpeg"),
                new FileChooser.ExtensionFilter("PNG", "*.png"),
                new FileChooser.ExtensionFilter("WebP", "*.webp"),
                new FileChooser.ExtensionFilter("Tất cả", "*.*"));

        File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile == null) {
            return;
        }

        // Create progress dialog with Cancel button
        Dialog<Void> progressDialog = new Dialog<>();
        progressDialog.setTitle("Đang tải ảnh");
        progressDialog.setHeaderText("Đang xử lý và tải ảnh lên thẻ...");

        // Progress indicator and status label
        javafx.scene.control.ProgressIndicator progressIndicator = new javafx.scene.control.ProgressIndicator();
        progressIndicator.setProgress(-1); // Indeterminate
        Label statusLabel = new Label("Vui lòng đợi...");

        VBox progressContent = new VBox(15);
        progressContent.setAlignment(Pos.CENTER);
        progressContent.setPadding(new Insets(20));
        progressContent.getChildren().addAll(progressIndicator, statusLabel);
        progressDialog.getDialogPane().setContent(progressContent);

        // Add Cancel button so user can close if stuck
        ButtonType cancelButton = new ButtonType("Hủy", ButtonBar.ButtonData.CANCEL_CLOSE);
        progressDialog.getDialogPane().getButtonTypes().add(cancelButton);

        // Track if operation was cancelled
        final boolean[] cancelled = { false };
        final Thread[] uploadThread = { null };

        // Handle cancel button
        progressDialog.setOnCloseRequest(event -> {
            cancelled[0] = true;
            if (uploadThread[0] != null) {
                uploadThread[0].interrupt();
            }
        });

        // Show dialog non-blocking
        progressDialog.show();

        uploadThread[0] = new Thread(() -> {
            try {
                if (cancelled[0])
                    return;

                // Validate file
                PhotoUtils.validatePhotoFile(selectedFile);

                if (cancelled[0])
                    return;

                // Prepare photo for card
                javafx.application.Platform.runLater(() -> {
                    if (!cancelled[0]) {
                        statusLabel.setText("Đang nén và chuẩn bị ảnh...");
                    }
                });

                byte[] photoData = PhotoUtils.preparePhotoForCard(selectedFile);

                if (cancelled[0])
                    return;

                // Upload to card
                javafx.application.Platform.runLater(() -> {
                    if (!cancelled[0]) {
                        statusLabel.setText("Đang tải lên thẻ thông minh...");
                    }
                });

                boolean success = cardService.uploadPhoto(photoData);

                if (cancelled[0])
                    return;

                final byte[] finalPhotoData = photoData;
                javafx.application.Platform.runLater(() -> {
                    progressDialog.close();

                    if (success) {
                        showSuccessAlert("Tải ảnh thành công",
                                "Ảnh đã được tải lên thẻ thành công!\n\n" +
                                        "Kích thước: " + PhotoUtils.getPhotoInfo(finalPhotoData));

                        // Refresh display
                        refreshPhotoDisplay(photoSection);
                    } else {
                        showErrorAlert("Tải ảnh thất bại", "Không thể tải ảnh lên thẻ.");
                    }
                });

            } catch (InterruptedException e) {
                // Thread was interrupted (cancelled)
                System.out.println("[PHOTO] Upload cancelled by user");
                javafx.application.Platform.runLater(() -> {
                    progressDialog.close();
                });
            } catch (Exception e) {
                if (!cancelled[0]) {
                    javafx.application.Platform.runLater(() -> {
                        progressDialog.close();
                        showErrorAlert("Lỗi tải ảnh", "Không thể tải ảnh: " + e.getMessage());
                    });
                }
            }
        });
        uploadThread[0].setDaemon(true); // Allow app to exit even if thread is running
        uploadThread[0].start();
    }

    private void downloadPhotoFromCard() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Lưu ảnh từ thẻ");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JPEG", "*.jpg"));
        fileChooser.setInitialFileName("citizen_photo.jpg");

        File saveFile = fileChooser.showSaveDialog(null);
        if (saveFile == null) {
            return;
        }

        // Create progress dialog with Cancel button
        Dialog<Void> progressDialog = new Dialog<>();
        progressDialog.setTitle("Đang tải ảnh");
        progressDialog.setHeaderText("Đang tải ảnh từ thẻ...");

        javafx.scene.control.ProgressIndicator progressIndicator = new javafx.scene.control.ProgressIndicator();
        progressIndicator.setProgress(-1);
        Label statusLabel = new Label("Vui lòng đợi...");

        VBox progressContent = new VBox(15);
        progressContent.setAlignment(Pos.CENTER);
        progressContent.setPadding(new Insets(20));
        progressContent.getChildren().addAll(progressIndicator, statusLabel);
        progressDialog.getDialogPane().setContent(progressContent);

        ButtonType cancelButton = new ButtonType("Hủy", ButtonBar.ButtonData.CANCEL_CLOSE);
        progressDialog.getDialogPane().getButtonTypes().add(cancelButton);

        final boolean[] cancelled = { false };
        final Thread[] downloadThread = { null };

        progressDialog.setOnCloseRequest(event -> {
            cancelled[0] = true;
            if (downloadThread[0] != null) {
                downloadThread[0].interrupt();
            }
        });

        progressDialog.show();

        downloadThread[0] = new Thread(() -> {
            try {
                if (cancelled[0])
                    return;

                byte[] photoData = cardService.downloadPhoto();

                if (cancelled[0])
                    return;

                if (photoData == null || photoData.length == 0) {
                    javafx.application.Platform.runLater(() -> {
                        progressDialog.close();
                        showErrorAlert("Không có ảnh", "Thẻ chưa có ảnh để tải xuống.");
                    });
                    return;
                }

                PhotoUtils.savePhotoToFile(photoData, saveFile);

                if (cancelled[0])
                    return;

                javafx.application.Platform.runLater(() -> {
                    progressDialog.close();
                    showSuccessAlert("Tải xuống thành công",
                            "Ảnh đã được lưu vào:\n" + saveFile.getPath() + "\n\n" +
                                    "Kích thước: " + PhotoUtils.getPhotoInfo(photoData));
                });

            } catch (Exception e) {
                if (!cancelled[0]) {
                    javafx.application.Platform.runLater(() -> {
                        progressDialog.close();
                        showErrorAlert("Lỗi tải xuống", "Không thể tải ảnh: " + e.getMessage());
                    });
                } else {
                    System.out.println("[PHOTO] Download cancelled by user");
                    javafx.application.Platform.runLater(() -> progressDialog.close());
                }
            }
        });
        downloadThread[0].setDaemon(true);
        downloadThread[0].start();
    }

    private void refreshPhotoDisplay(VBox photoSection) {
        ImageView photoView = (ImageView) photoSection.getChildren().get(1);
        Label statusLabel = (Label) photoSection.getChildren().get(2);

        statusLabel.setText("Đang tải...");
        statusLabel.getStyleClass().removeAll("status-error", "status-success");

        loadCurrentPhoto(photoView, statusLabel);
    }

    private void showSuccessAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText("✅ " + title);
        alert.setContentText(message);
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        alert.showAndWait();
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText("❌ " + title);

        // Use styled label for red text
        Label contentLabel = new Label(message);
        contentLabel.setWrapText(true);
        contentLabel.getStyleClass().add("dialog-message-error");
        alert.getDialogPane().setContent(contentLabel);

        alert.getDialogPane().getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        alert.showAndWait();
    }
}