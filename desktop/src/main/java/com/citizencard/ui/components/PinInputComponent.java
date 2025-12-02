package com.citizencard.ui.components;

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
        field.setPrefWidth(58);
        field.setPrefHeight(68);
        field.setAlignment(Pos.CENTER);
        field.setStyle(
            "-fx-font-size: 26px; " +
            "-fx-font-weight: bold; " +
            "-fx-background-color: rgba(15,23,42,0.9); " +
            "-fx-border-color: #38bdf8; " +
            "-fx-border-width: 2.5; " +
            "-fx-border-radius: 12; " +
            "-fx-background-radius: 12; " +
            "-fx-text-fill: #e0f2fe; " +
            "-fx-effect: dropshadow(three-pass-box, rgba(56,189,248,0.4), 12, 0, 0, 4);"
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
                    "-fx-font-size: 26px; " +
                    "-fx-font-weight: bold; " +
                    "-fx-background-color: rgba(56,189,248,0.15); " +
                    "-fx-border-color: #fb7185; " +
                    "-fx-border-width: 3; " +
                    "-fx-border-radius: 12; " +
                    "-fx-background-radius: 12; " +
                    "-fx-text-fill: #e0f2fe; " +
                    "-fx-effect: dropshadow(three-pass-box, rgba(251,113,133,0.5), 16, 0, 0, 5);"
                );
            } else {
                field.setStyle(
                    "-fx-font-size: 26px; " +
                    "-fx-font-weight: bold; " +
                    "-fx-background-color: rgba(15,23,42,0.9); " +
                    "-fx-border-color: #38bdf8; " +
                    "-fx-border-width: 2.5; " +
                    "-fx-border-radius: 12; " +
                    "-fx-background-radius: 12; " +
                    "-fx-text-fill: #e0f2fe; " +
                    "-fx-effect: dropshadow(three-pass-box, rgba(56,189,248,0.4), 12, 0, 0, 4);"
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

    @Override
    public void requestFocus() {
        if (pinFields != null && pinFields.length > 0) {
            pinFields[0].requestFocus();
        } else {
            super.requestFocus();
        }
    }
}


