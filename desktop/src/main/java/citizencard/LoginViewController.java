package citizencard;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;
import javafx.scene.paint.Color;

/**
 * Modern Login View Controller
 * 
 * Beautiful UI for Smart Card authentication
 * All logic is handled by the Smart Card Applet
 */
public class LoginViewController {
    
    private BorderPane root;
    private CardService cardService;
    private CardDAO cardDAO;
    private Label statusLabel;
    private Label connectionStatusLabel;
    private Button connectButton;
    private Button loginButton;
    private PasswordField pinField;
    private VBox loginSection;
    private ProgressIndicator loadingIndicator;
    
    public LoginViewController() {
        cardService = new CardService();
        cardDAO = CardDAO.getInstance();
        initializeUI();
    }
    
    private void initializeUI() {
        root = new BorderPane();
        root.getStyleClass().add("main-container");
        
        // Create main content
        VBox mainContent = createMainContent();
        root.setCenter(mainContent);
        
        // Create footer
        HBox footer = createFooter();
        root.setBottom(footer);
    }
    
    private VBox createMainContent() {
        VBox content = new VBox(30);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(60, 80, 60, 80));
        content.setMaxWidth(500);
        content.getStyleClass().add("main-content");
        
        // Header section with icon and title
        VBox headerSection = createHeaderSection();
        
        // Card status section
        VBox statusSection = createStatusSection();
        
        // Connection section
        VBox connectionSection = createConnectionSection();
        
        // Login section
        loginSection = createLoginSection();
        loginSection.setVisible(false);
        
        content.getChildren().addAll(
            headerSection,
            statusSection,
            connectionSection,
            loginSection
        );
        
        return content;
    }
    
    private VBox createHeaderSection() {
        VBox header = new VBox(15);
        header.setAlignment(Pos.CENTER);
        
        // App icon (using Unicode symbol)
        Label iconLabel = new Label("🏛️");
        iconLabel.setStyle("-fx-font-size: 64px;");
        
        // Title
        Label titleLabel = new Label("Citizen Card System");
        titleLabel.getStyleClass().add("app-title");
        
        // Subtitle
        Label subtitleLabel = new Label("Secure Smart Card Authentication");
        subtitleLabel.getStyleClass().add("app-subtitle");
        
        header.getChildren().addAll(iconLabel, titleLabel, subtitleLabel);
        return header;
    }
    
    private VBox createStatusSection() {
        VBox section = new VBox(10);
        section.setAlignment(Pos.CENTER);
        section.getStyleClass().add("status-section");
        
        // Status indicator
        HBox statusIndicator = new HBox(10);
        statusIndicator.setAlignment(Pos.CENTER);
        
        Circle statusCircle = new Circle(6);
        statusCircle.setFill(Color.web("#ef4444")); // Red by default
        statusCircle.getStyleClass().add("status-circle");
        
        connectionStatusLabel = new Label("Disconnected");
        connectionStatusLabel.getStyleClass().add("status-text");
        
        statusIndicator.getChildren().addAll(statusCircle, connectionStatusLabel);
        
        // Main status message
        statusLabel = new Label("Please connect your Smart Card to continue");
        statusLabel.getStyleClass().add("status-message");
        
        section.getChildren().addAll(statusIndicator, statusLabel);
        return section;
    }
    
    private VBox createConnectionSection() {
        VBox section = new VBox(20);
        section.setAlignment(Pos.CENTER);
        section.getStyleClass().add("connection-section");
        
        // Connection card
        VBox connectionCard = new VBox(20);
        connectionCard.setAlignment(Pos.CENTER);
        connectionCard.getStyleClass().add("connection-card");
        connectionCard.setPadding(new Insets(30));
        
        // Card icon
        Label cardIcon = new Label("💳");
        cardIcon.setStyle("-fx-font-size: 48px;");
        
        // Instructions
        Label instructionLabel = new Label("Insert your Smart Card");
        instructionLabel.getStyleClass().add("instruction-text");
        
        Label detailLabel = new Label("Ensure JCIDE terminal is running");
        detailLabel.getStyleClass().add("detail-text");
        
        // Connect button
        connectButton = new Button("Connect to Smart Card");
        connectButton.getStyleClass().addAll("btn", "btn-primary", "btn-large");
        connectButton.setOnAction(e -> connectToCard());
        
        // Loading indicator (hidden by default)
        loadingIndicator = new ProgressIndicator();
        loadingIndicator.setVisible(false);
        loadingIndicator.setPrefSize(30, 30);
        
        connectionCard.getChildren().addAll(
            cardIcon, 
            instructionLabel, 
            detailLabel, 
            connectButton,
            loadingIndicator
        );
        
        section.getChildren().add(connectionCard);
        return section;
    }
    
    private VBox createLoginSection() {
        VBox section = new VBox(25);
        section.setAlignment(Pos.CENTER);
        section.getStyleClass().add("login-section");
        
        // Login card
        VBox loginCard = new VBox(25);
        loginCard.setAlignment(Pos.CENTER);
        loginCard.getStyleClass().add("login-card");
        loginCard.setPadding(new Insets(35));
        
        // Lock icon
        Label lockIcon = new Label("🔐");
        lockIcon.setStyle("-fx-font-size: 42px;");
        
        // Title
        Label loginTitle = new Label("Enter Your PIN");
        loginTitle.getStyleClass().add("login-title");
        
        // PIN input section
        VBox pinSection = new VBox(15);
        pinSection.setAlignment(Pos.CENTER);
        
        Label pinLabel = new Label("4-Digit PIN Code");
        pinLabel.getStyleClass().add("pin-label");
        
        // PIN field with better styling
        pinField = new PasswordField();
        pinField.setPromptText("••••");
        pinField.getStyleClass().add("pin-input");
        pinField.setMaxWidth(200);
        pinField.setAlignment(Pos.CENTER);
        
        // Add enter key support
        pinField.setOnAction(e -> login());
        
        pinSection.getChildren().addAll(pinLabel, pinField);
        
        // Button section
        VBox buttonSection = new VBox(15);
        buttonSection.setAlignment(Pos.CENTER);
        
        // Login button
        loginButton = new Button("Authenticate");
        loginButton.getStyleClass().addAll("btn", "btn-success", "btn-large");
        loginButton.setOnAction(e -> login());
        loginButton.setPrefWidth(200);
        
        // Secondary actions
        HBox secondaryActions = new HBox(15);
        secondaryActions.setAlignment(Pos.CENTER);
        
        Button adminButton = new Button("Admin Access");
        adminButton.getStyleClass().addAll("btn", "btn-outline");
        adminButton.setOnAction(e -> adminLogin());
        
        Button helpButton = new Button("Help");
        helpButton.getStyleClass().addAll("btn", "btn-text");
        helpButton.setOnAction(e -> showHelp());
        
        secondaryActions.getChildren().addAll(adminButton, helpButton);
        
        buttonSection.getChildren().addAll(loginButton, secondaryActions);
        
        loginCard.getChildren().addAll(
            lockIcon,
            loginTitle,
            pinSection,
            buttonSection
        );
        
        section.getChildren().add(loginCard);
        return section;
    }
    
    private HBox createFooter() {
        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER);
        footer.setPadding(new Insets(20));
        footer.getStyleClass().add("footer");
        
        Label footerText = new Label("Citizen Card Management System v1.0 | Secure Smart Card Technology");
        footerText.getStyleClass().add("footer-text");
        
        footer.getChildren().add(footerText);
        return footer;
    }
    
    private void connectToCard() {
        // Show loading state
        connectButton.setDisable(true);
        connectButton.setText("Connecting...");
        loadingIndicator.setVisible(true);
        statusLabel.setText("Connecting to Smart Card...");
        connectionStatusLabel.setText("Connecting...");
        
        // Run in background thread
        new Thread(() -> {
            try {
                Thread.sleep(1000); // Small delay for better UX
                boolean connected = cardService.connectToCard();
                
                javafx.application.Platform.runLater(() -> {
                    loadingIndicator.setVisible(false);
                    
                    if (connected) {
                        // Update UI for successful connection
                        updateConnectionStatus(true);
                        connectButton.setText("✓ Connected");
                        connectButton.getStyleClass().removeAll("btn-primary");
                        connectButton.getStyleClass().add("btn-success");
                        
                        // Try to get card information
                        try {
                            String cardId = cardService.getCardId();
                            
                            // Check if card is registered
                            if (cardDAO.isCardRegistered(cardId)) {
                                statusLabel.setText("Card authenticated successfully. Please enter your PIN.");
                                showLoginSection();
                            } else {
                                statusLabel.setText("⚠️ Card not registered in system. Please contact administrator.");
                                showAlert("Card Not Registered", 
                                    "This card is not registered in the system.\n\n" +
                                    "Please contact your system administrator to register this card.\n\n" +
                                    "Card ID: " + cardId);
                            }
                        } catch (Exception e) {
                            statusLabel.setText("⚠️ Card connected but not properly initialized.");
                            showAlert("Card Error", 
                                "Card is connected but there was an error reading card information:\n\n" + 
                                e.getMessage());
                        }
                        
                    } else {
                        // Connection failed
                        updateConnectionStatus(false);
                        statusLabel.setText("Failed to connect to Smart Card. Please check your card and try again.");
                        connectButton.setText("Retry Connection");
                        connectButton.setDisable(false);
                    }
                });
                
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    loadingIndicator.setVisible(false);
                    updateConnectionStatus(false);
                    statusLabel.setText("Connection error: " + e.getMessage());
                    connectButton.setText("Retry Connection");
                    connectButton.setDisable(false);
                });
            }
        }).start();
    }
    
    private void updateConnectionStatus(boolean connected) {
        if (connected) {
            connectionStatusLabel.setText("Connected");
            connectionStatusLabel.getStyleClass().removeAll("status-text");
            connectionStatusLabel.getStyleClass().add("status-text-success");
        } else {
            connectionStatusLabel.setText("Disconnected");
            connectionStatusLabel.getStyleClass().removeAll("status-text-success");
            connectionStatusLabel.getStyleClass().add("status-text");
        }
    }
    
    private void showLoginSection() {
        loginSection.setVisible(true);
        pinField.requestFocus();
    }
    
    private void login() {
        String pin = pinField.getText().trim();
        
        // Validate PIN format
        if (pin.length() != 4) {
            showAlert("Invalid PIN Format", "PIN must be exactly 4 digits.\n\nPlease enter a valid 4-digit PIN code.");
            pinField.clear();
            pinField.requestFocus();
            return;
        }
        
        if (!pin.matches("\\d{4}")) {
            showAlert("Invalid PIN Format", "PIN must contain only numbers.\n\nPlease enter a valid 4-digit PIN code.");
            pinField.clear();
            pinField.requestFocus();
            return;
        }
        
        // Show loading state
        loginButton.setDisable(true);
        loginButton.setText("Authenticating...");
        statusLabel.setText("Verifying your PIN code...");
        pinField.setDisable(true);
        
        // Run in background thread
        new Thread(() -> {
            try {
                Thread.sleep(800); // Small delay for better UX
                boolean verified = cardService.verifyPin(pin);
                
                javafx.application.Platform.runLater(() -> {
                    if (verified) {
                        statusLabel.setText("✅ Authentication successful! Loading your account...");
                        
                        // Get card info and show dashboard
                        try {
                            String cardId = cardService.getCardId();
                            int balance = cardService.getBalance();
                            
                            // Update database
                            cardDAO.updateLastAccessed(cardId);
                            cardDAO.logTransaction(cardId, "LOGIN", true, null);
                            
                            // Show success and then dashboard
                            showSuccessMessage("Welcome!", "Authentication successful.\n\nLoading your dashboard...");
                            
                            // Delay before showing dashboard
                            new Thread(() -> {
                                try {
                                    Thread.sleep(1500);
                                    javafx.application.Platform.runLater(() -> {
                                        showDashboard(cardId, balance);
                                    });
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                }
                            }).start();
                            
                        } catch (Exception e) {
                            showAlert("System Error", "Authentication successful but failed to load account information:\n\n" + e.getMessage());
                            resetLoginForm();
                        }
                        
                    } else {
                        statusLabel.setText("❌ Invalid PIN code. Please check and try again.");
                        showAlert("Authentication Failed", 
                            "The PIN code you entered is incorrect.\n\n" +
                            "Please check your PIN and try again.\n\n" +
                            "⚠️ Too many failed attempts may lock your card.");
                        resetLoginForm();
                    }
                });
                
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    statusLabel.setText("❌ Authentication error occurred.");
                    showAlert("Connection Error", 
                        "There was an error communicating with your Smart Card:\n\n" + 
                        e.getMessage() + "\n\n" +
                        "Please ensure your card is properly inserted and try again.");
                    resetLoginForm();
                });
            }
        }).start();
    }
    
    private void resetLoginForm() {
        pinField.clear();
        pinField.setDisable(false);
        pinField.requestFocus();
        loginButton.setText("Authenticate");
        loginButton.setDisable(false);
    }
    
    private void adminLogin() {
        // Create admin login dialog
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Administrator Access");
        dialog.setHeaderText("System Administrator Login");
        
        // Set dialog content
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        
        Label infoLabel = new Label("Enter administrator credentials:");
        PasswordField adminPassword = new PasswordField();
        adminPassword.setPromptText("Admin password");
        
        content.getChildren().addAll(infoLabel, adminPassword);
        dialog.getDialogPane().setContent(content);
        
        // Add buttons
        ButtonType loginButtonType = new ButtonType("Login", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);
        
        // Show dialog
        dialog.showAndWait().ifPresent(result -> {
            showAlert("Admin Access", "Administrator functionality is not yet implemented.\n\nThis feature will be available in a future update.");
        });
    }
    
    private void showHelp() {
        Alert helpDialog = new Alert(Alert.AlertType.INFORMATION);
        helpDialog.setTitle("Help & Support");
        helpDialog.setHeaderText("How to use Citizen Card System");
        
        String helpContent = 
            "🔧 SETUP REQUIREMENTS:\n" +
            "• Smart Card reader connected to computer\n" +
            "• JCIDE terminal software running\n" +
            "• Valid Citizen Card inserted in reader\n\n" +
            
            "📋 STEP-BY-STEP GUIDE:\n" +
            "1. Insert your Citizen Card into the card reader\n" +
            "2. Ensure JCIDE terminal is running and connected\n" +
            "3. Click 'Connect to Smart Card' button\n" +
            "4. Wait for successful connection confirmation\n" +
            "5. Enter your 4-digit PIN code\n" +
            "6. Click 'Authenticate' to access your account\n\n" +
            
            "⚠️ TROUBLESHOOTING:\n" +
            "• Check card reader connection\n" +
            "• Verify JCIDE terminal is running\n" +
            "• Ensure card is properly inserted\n" +
            "• Contact administrator if card is not registered\n\n" +
            
            "🔒 SECURITY NOTES:\n" +
            "• Never share your PIN with others\n" +
            "• Multiple failed PIN attempts may lock your card\n" +
            "• Always remove card when finished";
        
        helpDialog.setContentText(helpContent);
        helpDialog.showAndWait();
    }
    
    private void showDashboard(String cardId, int balance) {
        // Create beautiful dashboard dialog
        Alert dashboard = new Alert(Alert.AlertType.INFORMATION);
        dashboard.setTitle("Citizen Card Dashboard");
        dashboard.setHeaderText("🎉 Welcome to your account!");
        
        String content = String.format(
            "💳 CARD INFORMATION:\n" +
            "Card ID: %s\n" +
            "Status: Active\n" +
            "Balance: %,d VND\n\n" +
            
            "🚀 AVAILABLE SERVICES:\n" +
            "• 💰 View Current Balance\n" +
            "• 💵 Top Up Balance\n" +
            "• 🛒 Make Payments\n" +
            "• 🔐 Change PIN Code\n" +
            "• 📊 View Transaction History\n" +
            "• ⚙️ Account Settings\n\n" +
            
            "📱 QUICK ACTIONS:\n" +
            "• Balance inquiries are free\n" +
            "• Secure PIN-protected transactions\n" +
            "• Real-time transaction processing\n\n" +
            
            "💡 TIP: Keep your card safe and never share your PIN!",
            cardId, balance
        );
        
        dashboard.setContentText(content);
        
        // Customize dialog buttons
        ButtonType continueButton = new ButtonType("Continue Using Card", ButtonBar.ButtonData.OK_DONE);
        ButtonType logoutButton = new ButtonType("Logout & Disconnect", ButtonBar.ButtonData.CANCEL_CLOSE);
        dashboard.getDialogPane().getButtonTypes().setAll(continueButton, logoutButton);
        
        dashboard.showAndWait().ifPresent(response -> {
            if (response == logoutButton) {
                logout();
            } else {
                // For now, just logout since full dashboard isn't implemented
                showAlert("Coming Soon", 
                    "Full dashboard functionality is being developed.\n\n" +
                    "Current features available:\n" +
                    "• Card connection and authentication ✓\n" +
                    "• PIN verification ✓\n" +
                    "• Balance inquiry ✓\n\n" +
                    "Coming soon:\n" +
                    "• Transaction management\n" +
                    "• Balance top-up\n" +
                    "• Payment processing\n" +
                    "• PIN change functionality");
                logout();
            }
        });
    }
    
    private void logout() {
        // Disconnect from card
        cardService.disconnect();
        
        // Reset UI state
        updateConnectionStatus(false);
        statusLabel.setText("Session ended. Please connect your Smart Card to continue.");
        connectButton.setText("Connect to Smart Card");
        connectButton.getStyleClass().removeAll("btn-success");
        connectButton.getStyleClass().add("btn-primary");
        connectButton.setDisable(false);
        
        // Hide login section
        loginSection.setVisible(false);
        pinField.clear();
        
        showSuccessMessage("Logged Out", "You have been safely logged out.\n\nThank you for using Citizen Card System!");
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        
        // Style the dialog
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        alert.getDialogPane().getStyleClass().add("alert-dialog");
        
        alert.showAndWait();
    }
    
    private void showSuccessMessage(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText("✅ " + title);
        alert.setContentText(message);
        
        // Style the dialog
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        alert.getDialogPane().getStyleClass().add("success-dialog");
        
        alert.showAndWait();
    }
    
    public Parent getRoot() {
        return root;
    }
}