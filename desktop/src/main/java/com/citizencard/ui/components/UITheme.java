package com.citizencard.ui.components;

import javafx.scene.control.*;
import javafx.scene.layout.*;

public class UITheme {

    // Button Styles
    public static void applyPrimaryButton(Button button) {
        button.getStyleClass().addAll("button", "button-primary");
    }

    public static void applyAccentButton(Button button) {
        button.getStyleClass().addAll("button", "button-accent");
    }

    public static void applyDangerButton(Button button) {
        button.getStyleClass().addAll("button", "button-danger");
    }

    public static void applySidebarButton(Button button) {
        button.getStyleClass().addAll("button", "button-sidebar");
    }

    // Card Styles
    public static void applyCard(Region region) {
        region.getStyleClass().add("card");
    }

    public static void applyStatCard(Region region) {
        region.getStyleClass().add("stat-card");
    }

    // Dialog Styles
    public static void styleDialogPane(DialogPane pane) {
        pane.getStyleClass().add("dialog-pane");

        pane.getButtonTypes().forEach(type -> {
            Button button = (Button) pane.lookupButton(type);
            if (button == null)
                return;

            button.getStyleClass().add("button");

            if (type.getButtonData() == ButtonBar.ButtonData.OK_DONE ||
                    type.getButtonData() == ButtonBar.ButtonData.YES) {
                applyPrimaryButton(button);
            } else if (type.getButtonData() == ButtonBar.ButtonData.CANCEL_CLOSE ||
                    type.getButtonData() == ButtonBar.ButtonData.NO) {
                // Default button style
            } else {
                applyAccentButton(button);
            }
        });
    }

    // Table Styles
    public static void styleTable(TableView<?> table) {
        table.getStyleClass().add("table-view");
    }

    // Text Input Styles
    public static void styleTextField(TextField field) {
        field.getStyleClass().add("text-field");
    }

    public static void styleTextArea(TextArea area) {
        area.getStyleClass().add("text-area");
    }

    public static void stylePasswordField(PasswordField field) {
        field.getStyleClass().add("password-field");
    }

    public static void styleComboBox(ComboBox<?> comboBox) {
        comboBox.getStyleClass().add("combo-box");
    }
}
