package com.citizencard.desktop.ui.components;

import javafx.geometry.Pos;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.input.KeyCode;

public class PinInputComponent extends HBox {
    private TextField[] pinFields;
    private static final int PIN_LENGTH = 6;

    public PinInputComponent() {
        super(10);
        setAlignment(Pos.CENTER);
        pinFields = new TextField[PIN_LENGTH];

        for (int i = 0; i < PIN_LENGTH; i++) {
            TextField field = createPinField(i);
            pinFields[i] = field;
            getChildren().add(field);
        }

        pinFields[0].requestFocus();
    }

    private TextField createPinField(int index) {
        TextField field = new TextField();
        field.setPrefWidth(50);
        field.setPrefHeight(60);
        field.setAlignment(Pos.CENTER);
        field.setStyle(
            "-fx-font-size: 24px; " +
            "-fx-font-weight: bold; " +
            "-fx-background-color: white; " +
            "-fx-border-color: #3498db; " +
            "-fx-border-width: 2; " +
            "-fx-border-radius: 8; " +
            "-fx-background-radius: 8; " +
            "-fx-text-fill: #2c3e50;"
        );

        field.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.length() > 1) {
                field.setText(newVal.substring(0, 1));
            }
            if (!newVal.matches("\\d*")) {
                field.setText(oldVal);
            }
            if (newVal.length() == 1 && index < PIN_LENGTH - 1) {
                pinFields[index + 1].requestFocus();
            }
        });

        field.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.BACK_SPACE && field.getText().isEmpty() && index > 0) {
                pinFields[index - 1].requestFocus();
                pinFields[index - 1].clear();
            }
        });

        field.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (isNowFocused) {
                field.setStyle(
                    "-fx-font-size: 24px; " +
                    "-fx-font-weight: bold; " +
                    "-fx-background-color: #ecf0f1; " +
                    "-fx-border-color: #e74c3c; " +
                    "-fx-border-width: 3; " +
                    "-fx-border-radius: 8; " +
                    "-fx-background-radius: 8; " +
                    "-fx-text-fill: #2c3e50; " +
                    "-fx-effect: dropshadow(three-pass-box, rgba(231,76,60,0.4), 10, 0, 0, 3);"
                );
            } else {
                field.setStyle(
                    "-fx-font-size: 24px; " +
                    "-fx-font-weight: bold; " +
                    "-fx-background-color: white; " +
                    "-fx-border-color: #3498db; " +
                    "-fx-border-width: 2; " +
                    "-fx-border-radius: 8; " +
                    "-fx-background-radius: 8; " +
                    "-fx-text-fill: #2c3e50;"
                );
            }
        });

        return field;
    }

    public String getPin() {
        StringBuilder pin = new StringBuilder();
        for (TextField field : pinFields) {
            pin.append(field.getText());
        }
        return pin.toString();
    }

    public void clear() {
        for (TextField field : pinFields) {
            field.clear();
        }
        pinFields[0].requestFocus();
    }

    public boolean isComplete() {
        return getPin().length() == PIN_LENGTH;
    }
}

