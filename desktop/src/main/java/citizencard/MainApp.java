package citizencard;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import citizencard.controller.LoginViewController;
import citizencard.dao.CardDAO;

/**
 * Main Application Entry Point
 * 
 * Simple JavaFX app that only handles UI
 * All logic is in the Smart Card Applet
 */
public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // Initialize minimal database
            CardDAO.getInstance();
            System.out.println("✅ Database initialized");

            // Fixed window size for all screens
            final double WINDOW_WIDTH = 1200;
            final double WINDOW_HEIGHT = 850;

            LoginViewController loginController = new LoginViewController();
            Scene scene = new Scene(loginController.getRoot(), WINDOW_WIDTH, WINDOW_HEIGHT);

            // Load CSS
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

            primaryStage.setTitle("Citizen Card Management System");
            primaryStage.setScene(scene);
            primaryStage.setResizable(false); // Fixed size - no resize
            primaryStage.setWidth(WINDOW_WIDTH);
            primaryStage.setHeight(WINDOW_HEIGHT);
            primaryStage.centerOnScreen();
            primaryStage.show();

            System.out.println("🚀 Citizen Card Management System started");
            System.out.println("📱 Please insert your Citizen Card and ensure JCIDE terminal is running");

        } catch (Exception e) {
            System.err.println("❌ Error starting application: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        System.out.println("👋 Application shutting down");
    }

    public static void main(String[] args) {
        System.out.println("🏛️ Starting Citizen Card Management System...");
        launch(args);
    }
}