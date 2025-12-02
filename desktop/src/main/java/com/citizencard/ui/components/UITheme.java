package com.citizencard.ui.components;

import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.layout.Region;

/**
 * Central place to keep UI styling consistent (buttons, dialogs, tables).
 */
public final class UITheme {

    private static final String PRIMARY_BUTTON_STYLE = "-fx-background-color: "
            + "linear-gradient(to bottom right, #22d3ee, #2563eb, #7c3aed); "
            + "-fx-text-fill: white; "
            + "-fx-font-size: 15px; "
            + "-fx-font-weight: bold; "
            + "-fx-background-radius: 14; "
            + "-fx-border-color: rgba(255,255,255,0.65); "
            + "-fx-border-width: 1.5; "
            + "-fx-border-radius: 14; "
            + "-fx-cursor: hand; "
            + "-fx-effect: dropshadow(three-pass-box, rgba(37,99,235,0.45), 16, 0, 0, 4);";

    private static final String PRIMARY_BUTTON_HOVER_STYLE = "-fx-background-color: "
            + "linear-gradient(to bottom right, #0ea5e9, #1d4ed8, #6d28d9); "
            + "-fx-text-fill: white; "
            + "-fx-font-size: 15px; "
            + "-fx-font-weight: bold; "
            + "-fx-background-radius: 14; "
            + "-fx-border-color: rgba(255,255,255,0.85); "
            + "-fx-border-width: 1.8; "
            + "-fx-border-radius: 14; "
            + "-fx-cursor: hand; "
            + "-fx-effect: dropshadow(three-pass-box, rgba(14,165,233,0.7), 18, 0, 0, 5);";

    private static final String ACCENT_BUTTON_STYLE = "-fx-background-color: "
            + "linear-gradient(to bottom right, #4ade80, #16a34a, #0d9488); "
            + "-fx-text-fill: white; "
            + "-fx-font-size: 15px; "
            + "-fx-font-weight: bold; "
            + "-fx-background-radius: 14; "
            + "-fx-border-color: rgba(255,255,255,0.6); "
            + "-fx-border-width: 1.5; "
            + "-fx-border-radius: 14; "
            + "-fx-cursor: hand; "
            + "-fx-effect: dropshadow(three-pass-box, rgba(16,185,129,0.45), 16, 0, 0, 4);";

    private static final String ACCENT_BUTTON_HOVER_STYLE = "-fx-background-color: "
            + "linear-gradient(to bottom right, #22c55e, #15803d, #0f766e); "
            + "-fx-text-fill: white; "
            + "-fx-font-size: 15px; "
            + "-fx-font-weight: bold; "
            + "-fx-background-radius: 14; "
            + "-fx-border-color: rgba(255,255,255,0.85); "
            + "-fx-border-width: 1.8; "
            + "-fx-border-radius: 14; "
            + "-fx-cursor: hand; "
            + "-fx-effect: dropshadow(three-pass-box, rgba(16,185,129,0.65), 18, 0, 0, 5);";

    private static final String DANGER_BUTTON_STYLE = "-fx-background-color: "
            + "linear-gradient(to bottom right, #f43f5e, #be123c, #f97316); "
            + "-fx-text-fill: white; "
            + "-fx-font-size: 15px; "
            + "-fx-font-weight: bold; "
            + "-fx-background-radius: 14; "
            + "-fx-border-color: rgba(255,255,255,0.65); "
            + "-fx-border-width: 1.5; "
            + "-fx-border-radius: 14; "
            + "-fx-cursor: hand; "
            + "-fx-effect: dropshadow(three-pass-box, rgba(244,63,94,0.55), 16, 0, 0, 4);";

    private static final String DANGER_BUTTON_HOVER_STYLE = "-fx-background-color: "
            + "linear-gradient(to bottom right, #be123c, #9f1239, #ea580c); "
            + "-fx-text-fill: white; "
            + "-fx-font-size: 15px; "
            + "-fx-font-weight: bold; "
            + "-fx-background-radius: 14; "
            + "-fx-border-color: rgba(255,255,255,0.85); "
            + "-fx-border-width: 1.8; "
            + "-fx-border-radius: 14; "
            + "-fx-cursor: hand; "
            + "-fx-effect: dropshadow(three-pass-box, rgba(244,63,94,0.75), 18, 0, 0, 5);";

    private static final String SIDEBAR_BUTTON_STYLE = "-fx-background-color: rgba(15,23,42,0.35); "
            + "-fx-text-fill: white; "
            + "-fx-font-size: 15px; "
            + "-fx-font-weight: 600; "
            + "-fx-background-radius: 14; "
            + "-fx-border-color: rgba(148,163,184,0.7); "
            + "-fx-border-width: 1.4; "
            + "-fx-border-radius: 14; "
            + "-fx-padding: 0 18 0 18;";

    private static final String SIDEBAR_BUTTON_HOVER_STYLE = "-fx-background-color: rgba(14,165,233,0.35); "
            + "-fx-text-fill: white; "
            + "-fx-font-size: 15px; "
            + "-fx-font-weight: 700; "
            + "-fx-background-radius: 14; "
            + "-fx-border-color: rgba(255,255,255,0.85); "
            + "-fx-border-width: 1.8; "
            + "-fx-border-radius: 14; "
            + "-fx-padding: 0 18 0 18; "
            + "-fx-effect: dropshadow(three-pass-box, rgba(14,165,233,0.5), 14, 0, 0, 3);";

    private UITheme() {
        // Utility
    }

    public static void applyPrimaryButton(Button button) {
        attachButtonStyle(button, PRIMARY_BUTTON_STYLE, PRIMARY_BUTTON_HOVER_STYLE);
    }

    public static void applyAccentButton(Button button) {
        attachButtonStyle(button, ACCENT_BUTTON_STYLE, ACCENT_BUTTON_HOVER_STYLE);
    }

    public static void applyDangerButton(Button button) {
        attachButtonStyle(button, DANGER_BUTTON_STYLE, DANGER_BUTTON_HOVER_STYLE);
    }

    public static void applySidebarButton(Button button) {
        attachButtonStyle(button, SIDEBAR_BUTTON_STYLE, SIDEBAR_BUTTON_HOVER_STYLE);
        button.setPrefHeight(48);
        button.setPrefWidth(220);
        button.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
    }

    private static void attachButtonStyle(Button button, String baseStyle, String hoverStyle) {
        button.setStyle(baseStyle);
        button.setOnMouseEntered(e -> {
            button.setStyle(hoverStyle);
            button.setScaleX(1.04);
            button.setScaleY(1.04);
        });
        button.setOnMouseExited(e -> {
            button.setStyle(baseStyle);
            button.setScaleX(1.0);
            button.setScaleY(1.0);
        });
    }

    public static void styleDialogPane(DialogPane pane) {
        pane.setStyle("-fx-background-color: linear-gradient(to bottom, #ffffff, #f7f9fb); "
                + "-fx-background-radius: 20; "
                + "-fx-padding: 25; "
                + "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 20, 0, 0, 5);");
        pane.getButtonTypes().forEach(type -> {
            Button button = (Button) pane.lookupButton(type);
            if (button == null) {
                return;
            }
            if (type.getButtonData() == ButtonBar.ButtonData.OK_DONE
                    || type.getButtonData() == ButtonBar.ButtonData.YES) {
                applyPrimaryButton(button);
            } else if (type.getButtonData() == ButtonBar.ButtonData.CANCEL_CLOSE
                    || type.getButtonData() == ButtonBar.ButtonData.NO) {
                applySidebarButton(button);
            } else {
                applyAccentButton(button);
            }
        });
    }

    public static void applyCardBackground(Region card) {
        card.setStyle("-fx-background-color: rgba(255,255,255,0.95); "
                + "-fx-background-radius: 24; "
                + "-fx-border-color: rgba(148,163,184,0.6); "
                + "-fx-border-width: 2; "
                + "-fx-border-radius: 24; "
                + "-fx-effect: dropshadow(three-pass-box, rgba(15,23,42,0.2), 25, 0, 0, 6);");
    }

    public static <T> void applyTableStyle(TableView<T> tableView) {
        tableView.setStyle("-fx-background-color: white; "
                + "-fx-background-radius: 18; "
                + "-fx-border-color: rgba(15,23,42,0.15); "
                + "-fx-border-width: 2; "
                + "-fx-border-radius: 18; "
                + "-fx-table-cell-border-color: rgba(0,0,0,0.02); "
                + "-fx-padding: 10;");

        tableView.setRowFactory(tv -> {
            TableRow<T> row = new TableRow<T>() {
                @Override
                protected void updateItem(T item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setStyle("");
                    } else if (getIndex() % 2 == 0) {
                        setStyle("-fx-background-color: rgba(102,126,234,0.04);");
                    } else {
                        setStyle("-fx-background-color: rgba(255,255,255,0.95);");
                    }
                }
            };
            row.setOnMouseEntered(e -> {
                if (!row.isEmpty()) {
                    row.setStyle("-fx-background-color: rgba(52,152,219,0.18);");
                }
            });
            row.setOnMouseExited(e -> {
                if (!row.isEmpty()) {
                    if (row.getIndex() % 2 == 0) {
                        row.setStyle("-fx-background-color: rgba(102,126,234,0.04);");
                    } else {
                        row.setStyle("-fx-background-color: rgba(255,255,255,0.95);");
                    }
                }
            });
            return row;
        });

        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        tableView.setPlaceholder(new Label("Không có dữ liệu để hiển thị"));
    }
}

