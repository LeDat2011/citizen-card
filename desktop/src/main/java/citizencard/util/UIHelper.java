package citizencard.util;

import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.control.Alert.AlertType;

/**
 * Utility class for creating common UI elements and managing dialogs
 * Reduces duplication across controllers
 */
public class UIHelper {

    /**
     * Create a standardized statistics card
     * 
     * @param title Title of the card
     * @param value Value displayed (e.g. "123", "5,000,000 VND")
     * @param icon  Icon character/string
     * @param color Color hex code for the value
     * @return VBox representing the card
     */
    public static VBox createStatCard(String title, String value, String icon, String color, String styleClass) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(25));
        card.getStyleClass().add(styleClass != null ? styleClass : "stat-card");
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

    /**
     * Overload for simpler usage (default style class)
     */
    public static VBox createStatCard(String title, String value, String icon, String color) {
        return createStatCard(title, value, icon, color, "stat-card");
    }

    /**
     * Show an information alert
     */
    public static void showAlert(String title, String message) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        styleDialog(alert);
        alert.showAndWait();
    }

    /**
     * Show a successful action alert
     */
    public static void showSuccessAlert(String title, String message) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText("✅ " + title);
        alert.setContentText(message);
        styleDialog(alert);
        alert.showAndWait();
    }

    /**
     * Show an error/validation alert
     */
    public static void showErrorAlert(String title, String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText("❌ " + title);

        // Use styled label for red text
        Label contentLabel = new Label(message);
        contentLabel.setWrapText(true);
        contentLabel.getStyleClass().add("dialog-message-error");
        alert.getDialogPane().setContent(contentLabel);

        // alert.getDialogPane().getStyleClass().add("validation-alert"); // Optional
        styleDialog(alert);
        alert.showAndWait();
    }

    /**
     * Apply common stylesheets to a dialog
     */
    private static void styleDialog(Dialog<?> dialog) {
        try {
            dialog.getDialogPane().getStylesheets().add(
                    UIHelper.class.getResource("/css/styles.css").toExternalForm());
            dialog.getDialogPane().getStyleClass().add("alert-dialog");
        } catch (Exception e) {
            System.err.println("Warning: Could not load CSS for dialog: " + e.getMessage());
        }
    }

    /**
     * Create a styled menu button
     */
    public static Button createMenuButton(String text, String description, String styleClass) {
        Button button = new Button(text);
        button.getStyleClass().addAll(styleClass != null ? styleClass : "menu-button");
        button.setPrefWidth(250);
        button.setAlignment(Pos.CENTER_LEFT);

        if (description != null && !description.isEmpty()) {
            Tooltip tooltip = new Tooltip(description);
            button.setTooltip(tooltip);
        }

        return button;
    }
}
