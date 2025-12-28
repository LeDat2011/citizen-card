package citizencard.service;

import citizencard.util.RSAUtils;
import javax.smartcardio.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.List;

/**
 * Smart Card Communication Service v2.0
 * 
 * All-in-one service for Smart Card communication
 * Includes APDU commands, card operations, and RSA authentication
 */
public class CardService {

    // =====================================================
    // APDU COMMAND STRUCTURE v2.0 - Must match Applet
    // =====================================================

    // INS CODES
    private static final byte INS_VERIFY = (byte) 0x00;
    private static final byte INS_CREATE = (byte) 0x01;
    private static final byte INS_GET = (byte) 0x02;
    private static final byte INS_UPDATE = (byte) 0x03;
    private static final byte INS_GET_AVATAR_CHUNK = (byte) 0x04; // For chunked avatar download
    private static final byte INS_RESET_TRY_PIN = (byte) 0x10;
    private static final byte INS_CLEAR_CARD = (byte) 0x11;

    // P1 PARAMETERS (Command Type)
    private static final byte P1_PIN = (byte) 0x04;
    private static final byte P1_CITIZEN_INFO = (byte) 0x05;
    private static final byte P1_SIGNATURE = (byte) 0x06;
    private static final byte P1_FORGET_PIN = (byte) 0x0A;
    private static final byte P1_ACTIVATE_CARD = (byte) 0x0B;
    private static final byte P1_DEACTIVATE_CARD = (byte) 0x0C;

    // P2 PARAMETERS (Data Type)
    private static final byte P2_INFORMATION = (byte) 0x07;
    private static final byte P2_TRY_REMAINING = (byte) 0x08;
    private static final byte P2_AVATAR = (byte) 0x09;
    private static final byte P2_CARD_ID = (byte) 0x0A;
    private static final byte P2_PUBLIC_KEY = (byte) 0x0B;
    private static final byte P2_BALANCE = (byte) 0x0C;

    // BALANCE UPDATE TYPES
    private static final byte BALANCE_TYPE_TOPUP = (byte) 0x01;
    private static final byte BALANCE_TYPE_PAYMENT = (byte) 0x02;

    // AID của Citizen Card Applet (phải khớp với applet)
    private static final byte[] APPLET_AID = {
            (byte) 0x11, (byte) 0x22, (byte) 0x33, (byte) 0x44, (byte) 0x55, (byte) 0x00
    };

    // Singleton instance
    private static CardService instance;

    private CardChannel channel;
    private boolean connected = false;

    /**
     * Get singleton instance
     */
    public static CardService getInstance() {
        if (instance == null) {
            instance = new CardService();
        }
        return instance;
    }

    /**
     * Constructor - use getInstance() instead
     */
    public CardService() {
        // Allow direct construction for backward compatibility
    }

    /**
     * Connect to smart card and select applet
     */
    public boolean connectToCard() {
        try {
            TerminalFactory factory = TerminalFactory.getDefault();
            List<CardTerminal> terminals = factory.terminals().list();

            if (terminals.isEmpty()) {
                System.out.println("❌ No card terminals found");
                return false;
            }

            CardTerminal terminal = terminals.get(0);
            if (!terminal.isCardPresent()) {
                System.out.println("❌ No card present in terminal");
                return false;
            }

            Card card = terminal.connect("T=1");
            channel = card.getBasicChannel();

            // Select Citizen Card Applet
            byte[] selectCommand = buildSelectCommand(APPLET_AID);
            ResponseAPDU response = channel.transmit(new CommandAPDU(selectCommand));

            if (response.getSW() == 0x9000) {
                connected = true;
                System.out.println("✅ Connected to Citizen Card successfully");
                return true;
            } else {
                System.out.println("❌ Failed to select Citizen Card applet: " +
                        String.format("0x%04X", response.getSW()));
                return false;
            }
        } catch (Exception e) {
            System.out.println("❌ Error connecting to card: " + e.getMessage());
            return false;
        }
    }

    /**
     * Send APDU command to card (v2.0 format)
     */
    public byte[] sendCommand(byte ins, byte p1, byte p2, byte[] data) {
        if (!connected || channel == null) {
            throw new RuntimeException("Not connected to card");
        }

        try {
            byte[] command = buildCommandV2(ins, p1, p2, data);

            // LOG APDU COMMAND BEFORE SENDING
            System.out.println("\n" + "=".repeat(60));
            System.out.println("📤 SENDING APDU COMMAND TO JCIDE:");
            System.out.println("=".repeat(60));
            System.out.println("🎯 FUNCTION: " + getFunctionDescription(command[1], command[2], command[3]));
            System.out.println("-".repeat(60));
            System.out.println("CLA: " + String.format("0x%02X", command[0]));
            System.out.println("INS: " + String.format("0x%02X", command[1]) + " (" + getInsName(command[1]) + ")");
            System.out.println("P1:  " + String.format("0x%02X", command[2]) + " (" + getP1Name(command[2]) + ")");
            System.out.println("P2:  " + String.format("0x%02X", command[3]) + " (" + getP2Name(command[3]) + ")");
            System.out.println("Lc:  " + String.format("0x%02X", command[4]) + " (" + (command.length - 5) + " bytes)");

            if (command.length > 5) {
                System.out.print("Data: ");
                for (int i = 5; i < command.length; i++) {
                    System.out.print(String.format("%02X ", command[i]));
                    if ((i - 4) % 16 == 0 && i < command.length - 1) {
                        System.out.print("\n      ");
                    }
                }
                System.out.println();
            }

            System.out.print("Full APDU: ");
            for (byte b : command) {
                System.out.print(String.format("%02X ", b));
            }
            System.out.println("\n" + "=".repeat(60));

            CommandAPDU commandAPDU = new CommandAPDU(command);
            ResponseAPDU response = channel.transmit(commandAPDU);

            // LOG RESPONSE
            System.out.println("📥 RECEIVED RESPONSE FROM JCIDE:");
            System.out.println("=".repeat(60));
            System.out.println("SW:   " + String.format("0x%04X", response.getSW()) + " ("
                    + getSwDescription(response.getSW()) + ")");
            System.out.println("SW1:  " + String.format("0x%02X", response.getSW1()));
            System.out.println("SW2:  " + String.format("0x%02X", response.getSW2()));

            byte[] responseData = response.getData();
            if (responseData.length > 0) {
                System.out.println("Data Length: " + responseData.length + " bytes");
                System.out.print("Data: ");
                for (int i = 0; i < responseData.length; i++) {
                    System.out.print(String.format("%02X ", responseData[i]));
                    if ((i + 1) % 16 == 0 && i < responseData.length - 1) {
                        System.out.print("\n      ");
                    }
                }
                System.out.println();
            } else {
                System.out.println("Data: (empty)");
            }
            System.out.println("=".repeat(60) + "\n");

            // Return full response (data + SW)
            return response.getBytes();

        } catch (Exception e) {
            System.err.println("❌ ERROR sending command: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error sending command to card: " + e.getMessage(), e);
        }
    }

    /**
     * Get INS command name for logging
     */
    private String getInsName(byte ins) {
        switch (ins) {
            case INS_VERIFY:
                return "VERIFY";
            case INS_CREATE:
                return "CREATE";
            case INS_GET:
                return "GET";
            case INS_UPDATE:
                return "UPDATE";
            case INS_RESET_TRY_PIN:
                return "RESET_TRY_PIN";
            case INS_CLEAR_CARD:
                return "CLEAR_CARD";
            default:
                return "UNKNOWN";
        }
    }

    /**
     * Get P1 parameter name for logging
     */
    private String getP1Name(byte p1) {
        switch (p1) {
            case P1_PIN:
                return "PIN";
            case P1_CITIZEN_INFO:
                return "CITIZEN_INFO";
            case P1_SIGNATURE:
                return "SIGNATURE";
            case P1_FORGET_PIN:
                return "FORGET_PIN";
            case P1_ACTIVATE_CARD:
                return "ACTIVATE_CARD";
            case P1_DEACTIVATE_CARD:
                return "DEACTIVATE_CARD";
            default:
                return "UNKNOWN";
        }
    }

    /**
     * Get P2 parameter name for logging
     */
    private String getP2Name(byte p2) {
        switch (p2) {
            case P2_INFORMATION:
                return "INFORMATION";
            case P2_TRY_REMAINING:
                return "TRY_REMAINING";
            case P2_AVATAR:
                return "AVATAR";
            case P2_CARD_ID:
                return "CARD_ID";
            case P2_PUBLIC_KEY:
                return "PUBLIC_KEY";
            case P2_BALANCE:
                return "BALANCE";
            default:
                return "UNKNOWN";
        }
    }

    /**
     * Get status word description for logging
     */
    private String getSwDescription(int sw) {
        switch (sw) {
            case 0x9000:
                return "SUCCESS";
            case 0x6300:
                return "VERIFICATION_FAILED";
            case 0x6983:
                return "AUTHENTICATION_BLOCKED";
            case 0x6985:
                return "CONDITIONS_NOT_SATISFIED";
            case 0x6A80:
                return "INCORRECT_DATA";
            case 0x6A86:
                return "INCORRECT_P1_P2";
            case 0x6D00:
                return "INS_NOT_SUPPORTED";
            case 0x6E00:
                return "CLA_NOT_SUPPORTED";
            case 0x6F00:
                return "UNHANDLED_EXCEPTION (Internal applet error)";
            default:
                return "ERROR";
        }
    }

    /**
     * Get function description for logging
     */
    private String getFunctionDescription(byte ins, byte p1, byte p2) {
        // INS_VERIFY (0x00)
        if (ins == INS_VERIFY && p1 == P1_PIN) {
            return "Verify PIN - Authenticate user with PIN";
        }

        // INS_CREATE (0x01)
        if (ins == INS_CREATE && p1 == P1_PIN) {
            return "Initialize Card - Create new card with PIN";
        }
        if (ins == INS_CREATE && p1 == P1_CITIZEN_INFO && p2 == P2_AVATAR) {
            return "Upload Avatar - Store encrypted photo";
        }

        // INS_GET (0x02)
        if (ins == INS_GET && p2 == P2_CARD_ID) {
            return "Get Card ID - Retrieve unique card identifier";
        }
        if (ins == INS_GET && p2 == P2_PUBLIC_KEY) {
            return "Get Public Key - Export RSA public key";
        }
        if (ins == INS_GET && p2 == P2_BALANCE) {
            return "Get Balance - Retrieve decrypted balance";
        }
        if (ins == INS_GET && p2 == P2_TRY_REMAINING) {
            return "Get Remaining Tries - Check PIN attempts left";
        }
        if (ins == INS_GET && p1 == P1_CITIZEN_INFO && p2 == P2_INFORMATION) {
            return "Get Personal Info - Retrieve decrypted citizen data";
        }
        if (ins == INS_GET && p1 == P1_CITIZEN_INFO && p2 == P2_AVATAR) {
            return "Get Avatar - Download decrypted photo";
        }

        // INS_UPDATE (0x03)
        if (ins == INS_UPDATE && p1 == P1_PIN) {
            return "Change PIN - Update PIN with old/new verification";
        }
        if (ins == INS_UPDATE && p1 == P1_CITIZEN_INFO && p2 == P2_INFORMATION) {
            return "Update Personal Info - Store encrypted citizen data";
        }
        if (ins == INS_UPDATE && p1 == P1_CITIZEN_INFO && p2 == P2_AVATAR) {
            return "Update Avatar - Replace encrypted photo";
        }
        if (ins == INS_UPDATE && p1 == P1_CITIZEN_INFO && p2 == P2_BALANCE) {
            return "Update Balance - Top-up or Payment";
        }
        if (ins == INS_UPDATE && p1 == P1_ACTIVATE_CARD) {
            return "Activate Card - Enable card with PIN";
        }
        if (ins == INS_UPDATE && p1 == P1_DEACTIVATE_CARD) {
            return "Deactivate Card - Disable card";
        }
        if (ins == INS_UPDATE && p1 == P1_FORGET_PIN) {
            return "Forget PIN - Admin reset PIN";
        }

        // INS_RESET_TRY_PIN (0x10)
        if (ins == INS_RESET_TRY_PIN) {
            return "Reset PIN Tries - Admin unlock blocked card";
        }

        // INS_CLEAR_CARD (0x11)
        if (ins == INS_CLEAR_CARD) {
            return "Clear Card - Factory reset (keep RSA keys)";
        }

        return "Unknown Function";
    }

    // =====================================================
    // APDU BUILDERS v2.0
    // =====================================================

    private byte[] buildCommandV2(byte ins, byte p1, byte p2, byte[] data) {
        if (data == null || data.length == 0) {
            return new byte[] { (byte) 0x00, ins, p1, p2, (byte) 0x00 };
        } else {
            byte[] command = new byte[5 + data.length];
            command[0] = (byte) 0x00; // CLA
            command[1] = ins; // INS
            command[2] = p1; // P1
            command[3] = p2; // P2
            command[4] = (byte) data.length; // Lc
            System.arraycopy(data, 0, command, 5, data.length);
            return command;
        }
    }

    private byte[] buildPinData(String pin) {
        if (pin.length() != 4) {
            throw new IllegalArgumentException("PIN must be 4 digits");
        }
        return pin.getBytes();
    }

    private byte[] buildAmountData(int amount) {
        return new byte[] {
                (byte) (amount >> 24),
                (byte) (amount >> 16),
                (byte) (amount >> 8),
                (byte) (amount & 0xFF)
        };
    }

    private int parseAmount(byte[] response) {
        if (response.length < 4) {
            throw new IllegalArgumentException("Response too short for amount");
        }
        return ((response[0] & 0xFF) << 24) |
                ((response[1] & 0xFF) << 16) |
                ((response[2] & 0xFF) << 8) |
                (response[3] & 0xFF);
    }

    private boolean isSuccess(byte[] response) {
        if (response.length < 2)
            return false;
        int sw = ((response[response.length - 2] & 0xFF) << 8) |
                (response[response.length - 1] & 0xFF);
        return sw == 0x9000;
    }

    private byte[] getResponseData(byte[] response) {
        if (response.length <= 2)
            return new byte[0];
        byte[] data = new byte[response.length - 2];
        System.arraycopy(response, 0, data, 0, data.length);
        return data;
    }

    /**
     * Initialize new card with PIN and Card ID (v2.0)
     * Format: [PIN:4][cardIdLength:1][cardId:N]
     */
    public String initializeCard(String pin, String cardId) {
        byte[] pinData = buildPinData(pin);
        byte[] cardIdBytes = cardId.getBytes(java.nio.charset.StandardCharsets.UTF_8);

        // Build data: [PIN:4][length:1][cardId:N]
        byte[] data = new byte[pinData.length + 1 + cardIdBytes.length];
        System.arraycopy(pinData, 0, data, 0, pinData.length);
        data[pinData.length] = (byte) cardIdBytes.length;
        System.arraycopy(cardIdBytes, 0, data, pinData.length + 1, cardIdBytes.length);

        System.out.println("[CARD] Initializing with ID: " + cardId + " (" + cardIdBytes.length + " bytes)");

        byte[] response = sendCommand(INS_CREATE, P1_PIN, (byte) 0x00, data);

        if (isSuccess(response)) {
            byte[] responseData = getResponseData(response);
            // Response contains Card ID + Public Key, extract Card ID
            String returnedId = new String(responseData, 0, Math.min(cardIdBytes.length, responseData.length));
            System.out.println("[CARD] Card initialized with ID: " + returnedId);
            return returnedId.trim();
        } else {
            throw new RuntimeException("Failed to initialize card");
        }
    }

    /**
     * Legacy method for backward compatibility (generates default ID)
     */
    public String initializeCard(String pin) {
        return initializeCard(pin, "CITIZEN-0001");
    }

    /**
     * Verify PIN (v2.0) - Returns result with remaining tries
     */
    public PinVerificationResult verifyPin(String pin) {
        byte[] pinData = buildPinData(pin);
        byte[] response = sendCommand(INS_VERIFY, P1_PIN, (byte) 0x00, pinData);

        if (isSuccess(response)) {
            byte[] data = getResponseData(response);
            if (data.length >= 2) {
                boolean success = data[0] == (byte) 0x01;
                int remainingTries = data[1] & 0xFF;
                return new PinVerificationResult(success, remainingTries);
            }
        }

        // If we get here, something went wrong
        return new PinVerificationResult(false, 0);
    }

    /**
     * Legacy method for backward compatibility
     */
    public boolean verifyPinSimple(String pin) {
        PinVerificationResult result = verifyPin(pin);
        if (!result.success && result.remainingTries > 0) {
            System.out.println("PIN verification failed. Remaining tries: " + result.remainingTries);
        }
        return result.success;
    }

    /**
     * PIN verification result class
     */
    public static class PinVerificationResult {
        public final boolean success;
        public final int remainingTries;

        public PinVerificationResult(boolean success, int remainingTries) {
            this.success = success;
            this.remainingTries = remainingTries;
        }
    }

    /**
     * Get Card ID (v2.0)
     */
    public String getCardId() {
        byte[] response = sendCommand(INS_GET, (byte) 0x00, P2_CARD_ID, null);

        if (isSuccess(response)) {
            byte[] cardIdData = getResponseData(response);
            return new String(cardIdData).trim();
        } else {
            throw new RuntimeException("Failed to get card ID");
        }
    }

    /**
     * Get Public Key (v2.0)
     */
    public byte[] getPublicKey() {
        byte[] response = sendCommand(INS_GET, (byte) 0x00, P2_PUBLIC_KEY, null);

        if (isSuccess(response)) {
            return getResponseData(response);
        } else {
            throw new RuntimeException("Failed to get public key");
        }
    }

    /**
     * Get Balance (v2.0) - Requires PIN verification
     */
    public int getBalance() {
        byte[] response = sendCommand(INS_GET, (byte) 0x00, P2_BALANCE, null);

        if (isSuccess(response)) {
            byte[] balanceData = getResponseData(response);
            return parseAmount(balanceData);
        } else {
            throw new RuntimeException("Failed to get balance - PIN verification required");
        }
    }

    /**
     * Update balance (v2.0) - Combined topup/payment method
     */
    public int updateBalance(byte type, int amount) {
        byte[] data = new byte[5];
        data[0] = type; // 0x01 = topup, 0x02 = payment
        data[1] = (byte) (amount >> 24);
        data[2] = (byte) (amount >> 16);
        data[3] = (byte) (amount >> 8);
        data[4] = (byte) (amount & 0xFF);

        byte[] response = sendCommand(INS_UPDATE, P1_CITIZEN_INFO, P2_BALANCE, data);

        if (isSuccess(response)) {
            byte[] newBalanceData = getResponseData(response);
            return parseAmount(newBalanceData);
        } else {
            String operation = (type == BALANCE_TYPE_TOPUP) ? "top up" : "payment";
            throw new RuntimeException("Failed to " + operation + " balance");
        }
    }

    /**
     * Top up balance (v2.0)
     */
    public int topupBalance(int amount) {
        return updateBalance(BALANCE_TYPE_TOPUP, amount);
    }

    /**
     * Make payment (v2.0)
     */
    public int makePayment(int amount) {
        return updateBalance(BALANCE_TYPE_PAYMENT, amount);
    }

    /**
     * Change PIN (v2.0)
     */
    public boolean changePin(String oldPin, String newPin) {
        byte[] pinData = new byte[8]; // 4 bytes old + 4 bytes new
        System.arraycopy(buildPinData(oldPin), 0, pinData, 0, 4);
        System.arraycopy(buildPinData(newPin), 0, pinData, 4, 4);

        byte[] response = sendCommand(INS_UPDATE, P1_PIN, (byte) 0x00, pinData);
        return isSuccess(response);
    }

    /**
     * Disconnect from card
     */
    public void disconnect() {
        if (channel != null) {
            try {
                channel.getCard().disconnect(false);
                connected = false;
                channel = null;
                System.out.println("✅ Disconnected from card");
            } catch (Exception e) {
                System.out.println("⚠️ Error disconnecting from card: " + e.getMessage());
            }
        }
    }

    /**
     * Check if connected to card
     */
    public boolean isConnected() {
        return connected && channel != null;
    }

    // =====================================================
    // ADDITIONAL v2.0 METHODS
    // =====================================================

    /**
     * Get remaining PIN tries (v2.0)
     */
    public int getRemainingPinTries() {
        byte[] response = sendCommand(INS_GET, (byte) 0x00, P2_TRY_REMAINING, null);

        if (isSuccess(response)) {
            byte[] data = getResponseData(response);
            if (data.length > 0) {
                return data[0] & 0xFF;
            }
        }

        throw new RuntimeException("Failed to get remaining PIN tries");
    }

    /**
     * Reset PIN tries (v2.0) - Admin function
     */
    public int resetPinTries() {
        byte[] response = sendCommand(INS_RESET_TRY_PIN, (byte) 0x00, (byte) 0x00, null);

        if (isSuccess(response)) {
            byte[] data = getResponseData(response);
            if (data.length > 0) {
                return data[0] & 0xFF;
            }
        }

        throw new RuntimeException("Failed to reset PIN tries");
    }

    /**
     * Clear card data (v2.0) - Admin function to reset card
     * Clears all data and resets card to initial state
     */
    public boolean clearCard() {
        byte[] response = sendCommand(INS_CLEAR_CARD, (byte) 0x00, (byte) 0x00, null);

        if (isSuccess(response)) {
            byte[] data = getResponseData(response);
            return data.length > 0 && data[0] == 0x01;
        }

        return false;
    }

    /**
     * Activate card (v2.0) - Requires PIN
     */
    public boolean activateCard(String pin) {
        byte[] pinData = buildPinData(pin);
        byte[] response = sendCommand(INS_UPDATE, P1_ACTIVATE_CARD, (byte) 0x00, pinData);

        return isSuccess(response);
    }

    /**
     * Deactivate card (v2.0) - Requires PIN verification first
     */
    public boolean deactivateCard() {
        byte[] response = sendCommand(INS_UPDATE, P1_DEACTIVATE_CARD, (byte) 0x00, null);

        return isSuccess(response);
    }

    /**
     * Sign data with RSA private key (v2.0) - Requires PIN verification
     */
    public byte[] signData(byte[] data) {
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Data to sign is empty");
        }

        byte[] response = sendCommand(INS_UPDATE, P1_SIGNATURE, (byte) 0x00, data);

        if (isSuccess(response)) {
            return getResponseData(response);
        } else {
            throw new RuntimeException("Failed to sign data - PIN verification required");
        }
    }

    /**
     * Update personal information (v2.0) - Requires PIN verification
     */
    public boolean updatePersonalInfo(byte[] infoData) {
        if (infoData == null || infoData.length == 0) {
            throw new IllegalArgumentException("Info data is empty");
        }

        if (infoData.length > 256) {
            throw new IllegalArgumentException("Info data too large (max 256 bytes)");
        }

        byte[] response = sendCommand(INS_UPDATE, P1_CITIZEN_INFO, P2_INFORMATION, infoData);

        return isSuccess(response);
    }

    /**
     * Get personal information (v2.0) - Requires PIN verification
     */
    public byte[] getPersonalInfo() {
        byte[] response = sendCommand(INS_GET, P1_CITIZEN_INFO, P2_INFORMATION, null);

        if (isSuccess(response)) {
            return getResponseData(response);
        } else {
            throw new RuntimeException("Failed to get personal info - PIN verification required");
        }
    }

    /**
     * Forget PIN (v2.0) - Admin function to reset PIN
     */
    public boolean forgetPin(String newPin) {
        byte[] pinData = buildPinData(newPin);
        byte[] response = sendCommand(INS_UPDATE, P1_FORGET_PIN, (byte) 0x00, pinData);

        return isSuccess(response);
    }

    // =====================================================
    // CHALLENGE-RESPONSE AUTHENTICATION
    // =====================================================

    private static final SecureRandom secureRandom = new SecureRandom();
    private PublicKey cachedPublicKey = null;

    /**
     * Challenge card with random data and verify signature
     * This authenticates that the card has the correct private key
     * 
     * @return true if card is authentic
     */
    public boolean challengeCard() {
        try {
            // Generate random challenge
            byte[] challenge = new byte[16];
            secureRandom.nextBytes(challenge);
            String challengeStr = bytesToHex(challenge);

            System.out.println("[AUTH] Challenge: " + challengeStr);

            // Get public key if not cached
            if (cachedPublicKey == null) {
                byte[] pubKeyData = getPublicKey();
                cachedPublicKey = RSAUtils.generatePublicKeyFromBytes(pubKeyData);

                if (cachedPublicKey == null) {
                    System.err.println("[AUTH] Failed to parse public key");
                    return false;
                }
            }

            // Send challenge to card for signing
            byte[] response = sendCommand(INS_CREATE, P1_SIGNATURE, (byte) 0x00, challengeStr.getBytes());

            if (!isSuccess(response)) {
                System.err.println("[AUTH] Card did not sign challenge");
                return false;
            }

            byte[] signature = getResponseData(response);
            System.out.println("[AUTH] Received signature: " + signature.length + " bytes");

            // Verify signature
            boolean valid = RSAUtils.verifySignature(signature, cachedPublicKey, challengeStr);

            if (valid) {
                System.out.println("[AUTH] Card authentication SUCCESSFUL");
            } else {
                System.err.println("[AUTH] Card authentication FAILED - Invalid signature");
            }

            return valid;

        } catch (Exception e) {
            System.err.println("[AUTH] Challenge failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Clear cached public key (call when switching cards)
     */
    public void clearPublicKeyCache() {
        cachedPublicKey = null;
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    // =====================================================
    // PHOTO MANAGEMENT METHODS v2.0 (Extended APDU)
    // =====================================================

    private static final int MAX_AVATAR_SIZE = 15360; // 15KB

    /**
     * Upload avatar to card using Extended APDU (supports up to 15KB)
     * 
     * @param avatarData Avatar bytes (max 15KB)
     * @return true if successful
     */
    public boolean uploadAvatar(byte[] avatarData) {
        if (!connected || channel == null) {
            throw new RuntimeException("Not connected to card");
        }

        if (avatarData == null || avatarData.length == 0) {
            throw new IllegalArgumentException("Avatar data is empty");
        }

        if (avatarData.length > MAX_AVATAR_SIZE) {
            throw new IllegalArgumentException("Avatar too large (max " + MAX_AVATAR_SIZE + " bytes)");
        }

        try {
            System.out.println("============================================================");
            System.out.println("[AVATAR UPLOAD] Starting chunked transfer...");
            System.out.println("  Total size: " + avatarData.length + " bytes");
            System.out.println("============================================================");

            // Use chunked transfer (max 200 bytes per chunk)
            final int CHUNK_SIZE = 200;
            int offset = 0;
            int chunkNum = 0;
            int totalChunks = (avatarData.length + CHUNK_SIZE - 1) / CHUNK_SIZE;

            while (offset < avatarData.length) {
                int remaining = avatarData.length - offset;
                int chunkLen = Math.min(CHUNK_SIZE, remaining);
                boolean isLastChunk = (offset + chunkLen >= avatarData.length);

                // Build chunk: [totalLen:2][offset:2][data:N]
                byte[] chunk = new byte[chunkLen + 4];

                // Header
                chunk[0] = (byte) ((avatarData.length >> 8) & 0xFF);
                chunk[1] = (byte) (avatarData.length & 0xFF);
                chunk[2] = (byte) ((offset >> 8) & 0xFF);
                chunk[3] = (byte) (offset & 0xFF);

                // Copy chunk data
                System.arraycopy(avatarData, offset, chunk, 4, chunkLen);

                // P2: bit 7 = 1 if more chunks coming
                byte p2 = isLastChunk ? P2_AVATAR : (byte) (P2_AVATAR | 0x80);

                System.out.println("------------------------------------------------------------");
                System.out.println("[AVATAR] Sending chunk #" + (chunkNum + 1) + "/" + totalChunks);
                System.out.println("[AVATAR] -> INS: 0x01 (CREATE)");
                System.out.println("[AVATAR] -> P1: 0x05 (CITIZEN_INFO)");
                System.out.println("[AVATAR] -> P2: 0x" + String.format("%02X", p2 & 0xFF) +
                        (isLastChunk ? " (AVATAR - LAST CHUNK)" : " (AVATAR | 0x80 - MORE CHUNKS)"));
                System.out.println("[AVATAR] -> Chunk header: totalLen=" + avatarData.length + ", offset=" + offset);
                System.out.println("[AVATAR] -> Chunk data size: " + chunkLen + " bytes");

                byte[] response = sendCommand(INS_CREATE, P1_CITIZEN_INFO, p2, chunk);

                if (!isSuccess(response)) {
                    int sw = ((response[response.length - 2] & 0xFF) << 8) | (response[response.length - 1] & 0xFF);
                    System.err.println("[AVATAR] <- SW: 0x" + String.format("%04X", sw) + " (FAILED)");
                    System.err.println("[AVATAR] Chunk " + (chunkNum + 1) + " upload failed!");
                    return false;
                }

                byte[] responseData = getResponseData(response);
                System.out.println("[AVATAR] <- SW: 0x9000 (SUCCESS)");
                if (responseData.length > 0) {
                    System.out.println("[AVATAR] <- Response data: " + responseData.length + " bytes");
                }

                offset += chunkLen;
                chunkNum++;

                int progress = (offset * 100) / avatarData.length;
                System.out.println(
                        "[AVATAR] Progress: " + offset + "/" + avatarData.length + " bytes (" + progress + "%)");
            }

            System.out.println("============================================================");
            System.out.println("[AVATAR UPLOAD] Summary:");
            System.out.println("  Total chunks sent: " + chunkNum);
            System.out.println("  Total bytes uploaded: " + avatarData.length);
            System.out.println("  Status: SUCCESS");
            System.out.println("============================================================");
            return true;

        } catch (Exception e) {
            System.err.println("[AVATAR] Upload failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Download avatar from card using chunked transfer
     * Uses INS_GET_AVATAR_CHUNK (0x04) with P1|P2 = offset
     * Response format: [totalLen:2][chunkLen:2][data:N]
     * 
     * @return Avatar bytes or null if no avatar
     */
    public byte[] downloadAvatar() {
        if (!connected || channel == null) {
            throw new RuntimeException("Not connected to card");
        }

        try {
            System.out.println("============================================================");
            System.out.println("[AVATAR DOWNLOAD] Starting chunked transfer...");
            System.out.println("============================================================");

            ByteArrayOutputStream fullAvatar = new ByteArrayOutputStream();
            int offset = 0;
            int totalExpectedSize = 0;
            final int MAX_CHUNKS = 100; // Safety limit

            for (int chunkNum = 0; chunkNum < MAX_CHUNKS; chunkNum++) {
                // Calculate P1, P2 from offset
                byte p1 = (byte) ((offset >> 8) & 0xFF);
                byte p2 = (byte) (offset & 0xFF);

                System.out.println("------------------------------------------------------------");
                System.out.println("[AVATAR] Requesting chunk #" + chunkNum);
                System.out.println("[AVATAR] -> INS: 0x04 (GET_AVATAR_CHUNK)");
                System.out.println("[AVATAR] -> P1|P2 offset: " + offset + " (P1=0x" + String.format("%02X", p1 & 0xFF)
                        + ", P2=0x" + String.format("%02X", p2 & 0xFF) + ")");

                // Send GET_AVATAR_CHUNK command with offset in P1|P2
                byte[] response = sendCommand(INS_GET_AVATAR_CHUNK, p1, p2, null);

                if (!isSuccess(response)) {
                    int sw = ((response[response.length - 2] & 0xFF) << 8) | (response[response.length - 1] & 0xFF);
                    System.err.println("[AVATAR] <- SW: 0x" + String.format("%04X", sw) + " (FAILED)");
                    if (offset == 0) {
                        System.out.println("[AVATAR] No avatar stored on card");
                        return null;
                    }
                    break;
                }

                byte[] data = getResponseData(response);
                System.out.println("[AVATAR] <- Response length: " + data.length + " bytes");

                // Parse response: [totalLen:2][chunkLen:2][data:N]
                if (data.length < 4) {
                    System.err.println("[AVATAR] Invalid response - too short");
                    break;
                }

                int totalLen = ((data[0] & 0xFF) << 8) | (data[1] & 0xFF);
                int chunkLen = ((data[2] & 0xFF) << 8) | (data[3] & 0xFF);

                System.out.println("[AVATAR] <- totalLen: " + totalLen + " bytes");
                System.out.println("[AVATAR] <- chunkLen: " + chunkLen + " bytes");

                // Store expected size on first chunk
                if (chunkNum == 0) {
                    totalExpectedSize = totalLen;
                    System.out.println("[AVATAR] Total avatar size: " + totalExpectedSize + " bytes");
                }

                // Check if we're done (chunkLen == 0)
                if (chunkLen == 0) {
                    System.out.println("[AVATAR] End of data reached");
                    break;
                }

                // Validate chunk data
                if (data.length < 4 + chunkLen) {
                    System.err.println(
                            "[AVATAR] Chunk data incomplete: expected " + chunkLen + ", got " + (data.length - 4));
                    break;
                }

                // Extract chunk data (skip 4-byte header)
                fullAvatar.write(data, 4, chunkLen);
                offset += chunkLen;

                System.out.println("[AVATAR] Progress: " + offset + "/" + totalExpectedSize + " bytes (" +
                        (totalExpectedSize > 0 ? (offset * 100 / totalExpectedSize) : 0) + "%)");

                // Check if download complete
                if (offset >= totalExpectedSize) {
                    System.out.println("[AVATAR] Download complete!");
                    break;
                }
            }

            byte[] photoData = fullAvatar.toByteArray();

            System.out.println("============================================================");
            System.out.println("[AVATAR DOWNLOAD] Summary:");
            System.out.println("  Total bytes received: " + photoData.length);
            System.out.println("  Expected size: " + totalExpectedSize);

            if (photoData.length == 0) {
                System.out.println("[AVATAR] No avatar data received");
                System.out.println("============================================================");
                return null;
            }

            // Debug: show first bytes to verify JPEG header
            StringBuilder firstBytes = new StringBuilder("  First 16 bytes: ");
            for (int i = 0; i < Math.min(16, photoData.length); i++) {
                firstBytes.append(String.format("%02X ", photoData[i] & 0xFF));
            }
            System.out.println(firstBytes.toString());

            // JPEG should start with FF D8 FF
            if (photoData.length >= 2 &&
                    (photoData[0] & 0xFF) == 0xFF &&
                    (photoData[1] & 0xFF) == 0xD8) {
                System.out.println("  Format: Valid JPEG header detected!");
            } else {
                System.err.println("  WARNING: Data does not have JPEG header!");
            }
            System.out.println("============================================================");

            return photoData;

        } catch (Exception e) {
            System.err.println("[AVATAR] Download failed: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Update existing avatar (v2.0) - Same as upload with chunked transfer
     */
    public boolean updateAvatar(byte[] avatarData) {
        // For chunked transfer, update is same as upload
        return uploadAvatar(avatarData);
    }

    /**
     * Legacy method for backward compatibility
     */
    public boolean uploadPhoto(byte[] photoData) {
        return uploadAvatar(photoData);
    }

    /**
     * Legacy method for backward compatibility
     * Uses chunked transfer for reliable download
     */
    public byte[] downloadPhoto() {
        return downloadAvatar();
    }

    /**
     * Build SELECT APDU command
     */
    private byte[] buildSelectCommand(byte[] aid) {
        byte[] command = new byte[5 + aid.length];
        command[0] = (byte) 0x00; // CLA
        command[1] = (byte) 0xA4; // INS (SELECT)
        command[2] = (byte) 0x04; // P1 (Select by name)
        command[3] = (byte) 0x00; // P2
        command[4] = (byte) aid.length; // Lc
        System.arraycopy(aid, 0, command, 5, aid.length);
        return command;
    }
}