package com.citizencard.ui.components;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

public class NotificationService {

    public static void showNotification(StackPane parent, String message, String type) {
        showNotification(parent, message, type, Pos.TOP_CENTER);
    }

    public static void showNotification(StackPane parent, String message, String type, Pos position) {
        Label notification = new Label(message);
        notification.setMaxWidth(500);
        notification.setWrapText(true);
        notification.setAlignment(Pos.CENTER);
        notification.setPrefHeight(80);
        
        String backgroundColor;
        String textColor = "#f8fafc";
        String icon;

        switch (type.toLowerCase()) {
            case "success":
                backgroundColor = "linear-gradient(to right, #22c55e, #16a34a)";
                icon = "✅ ";
                break;
            case "error":
                backgroundColor = "linear-gradient(to right, #ef4444, #b91c1c)";
                icon = "❌ ";
                break;
            case "warning":
                backgroundColor = "linear-gradient(to right, #f59e0b, #ea580c)";
                icon = "⚠️ ";
                break;
            case "info":
                backgroundColor = "linear-gradient(to right, #0ea5e9, #6366f1)";
                icon = "ℹ️ ";
                break;
            default:
                backgroundColor = "linear-gradient(to right, #1f2937, #0f172a)";
                icon = "📢 ";
        }
        
        notification.setText(icon + message);
        notification.setStyle(
                "-fx-background-color: " + backgroundColor + "; " +
                "-fx-text-fill: " + textColor + "; " +
                "-fx-font-size: 18px; " +
                "-fx-font-weight: 800; " +
                "-fx-padding: 20 40; " +
                "-fx-background-radius: 18; " +
                "-fx-border-radius: 18; " +
                "-fx-border-color: rgba(255,255,255,0.18); " +
                "-fx-effect: dropshadow(three-pass-box, rgba(8,47,73,0.35), 22, 0, 0, 10);"
        );
        
        notification.setOpacity(0);
        notification.setScaleX(0.8);
        notification.setScaleY(0.8);
        notification.setTranslateY(-50);
        
        StackPane.setAlignment(notification, position);
        if (position == Pos.TOP_CENTER) {
            StackPane.setMargin(notification, new javafx.geometry.Insets(100, 0, 0, 0));
        } else {
            StackPane.setMargin(notification, javafx.geometry.Insets.EMPTY);
        }
        
        parent.getChildren().add(notification);
        
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), notification);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        
        ScaleTransition scaleIn = new ScaleTransition(Duration.millis(300), notification);
        scaleIn.setFromX(0.8);
        scaleIn.setFromY(0.8);
        scaleIn.setToX(1);
        scaleIn.setToY(1);
        
        TranslateTransition slideIn = new TranslateTransition(Duration.millis(300), notification);
        slideIn.setFromY(-50);
        slideIn.setToY(0);
        
        fadeIn.play();
        scaleIn.play();
        slideIn.play();
        
        double displaySeconds = position == Pos.CENTER ? 4.5 : 3.0;

        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), notification);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setDelay(Duration.seconds(displaySeconds));
        
        ScaleTransition scaleOut = new ScaleTransition(Duration.millis(300), notification);
        scaleOut.setFromX(1);
        scaleOut.setFromY(1);
        scaleOut.setToX(0.8);
        scaleOut.setToY(0.8);
        scaleOut.setDelay(Duration.seconds(displaySeconds));
        
        TranslateTransition slideOut = new TranslateTransition(Duration.millis(300), notification);
        slideOut.setFromY(0);
        slideOut.setToY(-50);
        slideOut.setDelay(Duration.seconds(displaySeconds));
        
        fadeOut.setOnFinished(e -> parent.getChildren().remove(notification));
        
        fadeOut.play();
        scaleOut.play();
        slideOut.play();
    }
}


