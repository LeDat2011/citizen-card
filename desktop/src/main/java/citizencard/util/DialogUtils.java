package citizencard.util;

import javafx.geometry.Insets;
import javafx.scene.control.Dialog;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

/**
 * Dialog Utilities
 * 
 * Helper methods for creating responsive dialogs with proper sizing
 */
public class DialogUtils {
    
    /**
     * Create a responsive dialog with ScrollPane
     * 
     * @param title Dialog title
     * @param headerText Dialog header text
     * @param content Content VBox
     * @param maxHeight Maximum height (default 600)
     * @return Configured Dialog
     */
    public static <T> Dialog<T> createScrollableDialog(String title, String headerText, VBox content, int maxHeight) {
        Dialog<T> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(headerText);
        
        // Wrap content in ScrollPane
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setMaxHeight(maxHeight);
        scrollPane.setPrefHeight(maxHeight);
        scrollPane.getStyleClass().add("scroll-pane");
        
        dialog.getDialogPane().setContent(scrollPane);
        
        // Apply stylesheet
        try {
            dialog.getDialogPane().getStylesheets().add(
                DialogUtils.class.getResource("/css/styles.css").toExternalForm()
            );
        } catch (Exception e) {
            System.err.println("Warning: Could not load stylesheet for dialog");
        }
        
        return dialog;
    }
    
    /**
     * Create a responsive dialog with default max height (600px)
     */
    public static <T> Dialog<T> createScrollableDialog(String title, String headerText, VBox content) {
        return createScrollableDialog(title, headerText, content, 600);
    }
    
    /**
     * Create a standard form container with proper sizing
     * 
     * @param width Preferred width (default 500)
     * @param spacing Vertical spacing between elements (default 12)
     * @param padding Padding around container (default 15)
     * @return Configured VBox
     */
    public static VBox createFormContainer(int width, int spacing, int padding) {
        VBox container = new VBox(spacing);
        container.setPadding(new Insets(padding));
        container.setPrefWidth(width);
        container.setMaxWidth(width);
        return container;
    }
    
    /**
     * Create a standard form container with default settings
     */
    public static VBox createFormContainer() {
        return createFormContainer(500, 12, 15);
    }
    
    /**
     * Create a compact form container (smaller width)
     */
    public static VBox createCompactFormContainer() {
        return createFormContainer(400, 10, 12);
    }
    
    /**
     * Create a wide form container (larger width)
     */
    public static VBox createWideFormContainer() {
        return createFormContainer(600, 15, 20);
    }
}
