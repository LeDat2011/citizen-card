package citizencard.service;

import javax.smartcardio.*;
import java.util.List;

/**
 * Smart Card Communication Service
 * 
 * All-in-one service for Smart Card communication
 * Includes APDU commands and card operations
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
    private static final byte INS_RESET_TRY_PIN = (byte) 0x10;
    
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

    private CardChannel channel;
    private boolean connected = false;

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
            CommandAPDU commandAPDU = new CommandAPDU(command);
            ResponseAPDU response = channel.transmit(commandAPDU);
            
            // Return full response (data + SW)
            return response.getBytes();
            
        } catch (Exception e) {
            throw new RuntimeException("Error sending command to card: " + e.getMessage(), e);
        }
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
            command[1] = ins;         // INS
            command[2] = p1;          // P1
            command[3] = p2;          // P2
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
        if (response.length < 2) return false;
        int sw = ((response[response.length - 2] & 0xFF) << 8) | 
                 (response[response.length - 1] & 0xFF);
        return sw == 0x9000;
    }
    
    private byte[] getResponseData(byte[] response) {
        if (response.length <= 2) return new byte[0];
        byte[] data = new byte[response.length - 2];
        System.arraycopy(response, 0, data, 0, data.length);
        return data;
    }

    /**
     * Initialize new card with PIN (v2.0)
     */
    public String initializeCard(String pin) {
        byte[] pinData = buildPinData(pin);
        byte[] response = sendCommand(INS_CREATE, P1_PIN, (byte) 0x00, pinData);
        
        if (isSuccess(response)) {
            byte[] cardIdData = getResponseData(response);
            return new String(cardIdData).trim();
        } else {
            throw new RuntimeException("Failed to initialize card");
        }
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
    // PHOTO MANAGEMENT METHODS v2.0 (Chunked Transfer)
    // =====================================================
    
    // Photo Transfer INS Codes (match applet)
    private static final byte INS_PHOTO_START = (byte) 0x50;
    private static final byte INS_PHOTO_DATA = (byte) 0x51;
    private static final byte INS_PHOTO_END = (byte) 0x52;
    private static final byte INS_PHOTO_GET_SIZE = (byte) 0x53;
    private static final byte INS_PHOTO_GET_DATA = (byte) 0x54;
    
    private static final int CHUNK_SIZE = 200;  // Match applet
    
    /**
     * Upload avatar to card (v2.0 - Chunked transfer, max 8KB)
     * @param avatarData Avatar bytes (should be compressed JPEG, max 8KB)
     * @return true if successful
     */
    public boolean uploadAvatar(byte[] avatarData) {
        if (!connected || channel == null) {
            throw new RuntimeException("Not connected to card");
        }
        
        if (avatarData == null || avatarData.length == 0) {
            throw new IllegalArgumentException("Avatar data is empty");
        }
        
        if (avatarData.length > 8192) { // 8KB max for JavaCard 2.2.1
            throw new IllegalArgumentException("Avatar too large (max 8KB)");
        }
        
        try {
            System.out.println("[PHOTO] Starting chunked upload, size: " + avatarData.length + " bytes");
            
            // Step 1: Start upload - send total size
            byte[] sizeData = new byte[2];
            sizeData[0] = (byte) (avatarData.length >> 8);
            sizeData[1] = (byte) (avatarData.length & 0xFF);
            
            byte[] response = sendCommand(INS_PHOTO_START, (byte) 0x00, (byte) 0x00, sizeData);
            if (!isSuccess(response)) {
                throw new RuntimeException("Failed to start photo upload");
            }
            
            // Step 2: Send chunks
            int totalChunks = (avatarData.length + CHUNK_SIZE - 1) / CHUNK_SIZE;
            System.out.println("[PHOTO] Sending " + totalChunks + " chunks...");
            
            for (int i = 0; i < totalChunks; i++) {
                int offset = i * CHUNK_SIZE;
                int chunkLen = Math.min(CHUNK_SIZE, avatarData.length - offset);
                
                // Prepare chunk data
                byte[] chunkData = new byte[chunkLen];
                System.arraycopy(avatarData, offset, chunkData, 0, chunkLen);
                
                // Send chunk with index in P1P2
                byte p1 = (byte) (i >> 8);
                byte p2 = (byte) (i & 0xFF);
                response = sendCommand(INS_PHOTO_DATA, p1, p2, chunkData);
                
                if (!isSuccess(response)) {
                    throw new RuntimeException("Failed to upload chunk " + i);
                }
                
                System.out.println("[PHOTO] Chunk " + (i + 1) + "/" + totalChunks + " sent");
            }
            
            // Step 3: Finish upload
            response = sendCommand(INS_PHOTO_END, (byte) 0x00, (byte) 0x00, null);
            if (!isSuccess(response)) {
                throw new RuntimeException("Failed to finish photo upload");
            }
            
            System.out.println("[PHOTO] Upload completed successfully!");
            return true;
            
        } catch (Exception e) {
            System.err.println("[PHOTO] Upload failed: " + e.getMessage());
            throw new RuntimeException("Photo upload failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Download avatar from card (v2.0 - Chunked transfer)
     * @return Avatar bytes or null if no avatar
     */
    public byte[] downloadAvatar() {
        if (!connected || channel == null) {
            throw new RuntimeException("Not connected to card");
        }
        
        try {
            // Step 1: Get photo size
            byte[] response = sendCommand(INS_PHOTO_GET_SIZE, (byte) 0x00, (byte) 0x00, null);
            if (!isSuccess(response)) {
                throw new RuntimeException("Failed to get photo size");
            }
            
            byte[] data = getResponseData(response);
            if (data.length < 2) {
                return null; // No photo
            }
            
            int photoSize = ((data[0] & 0xFF) << 8) | (data[1] & 0xFF);
            if (photoSize == 0) {
                return null; // No photo
            }
            
            System.out.println("[PHOTO] Downloading, size: " + photoSize + " bytes");
            
            // Step 2: Download chunks
            byte[] photoData = new byte[photoSize];
            int totalChunks = (photoSize + CHUNK_SIZE - 1) / CHUNK_SIZE;
            
            for (int i = 0; i < totalChunks; i++) {
                byte p1 = (byte) (i >> 8);
                byte p2 = (byte) (i & 0xFF);
                
                response = sendCommand(INS_PHOTO_GET_DATA, p1, p2, null);
                if (!isSuccess(response)) {
                    throw new RuntimeException("Failed to download chunk " + i);
                }
                
                data = getResponseData(response);
                int offset = i * CHUNK_SIZE;
                System.arraycopy(data, 0, photoData, offset, data.length);
                
                System.out.println("[PHOTO] Chunk " + (i + 1) + "/" + totalChunks + " received");
            }
            
            System.out.println("[PHOTO] Download completed!");
            return photoData;
            
        } catch (Exception e) {
            System.err.println("[PHOTO] Download failed: " + e.getMessage());
            throw new RuntimeException("Photo download failed: " + e.getMessage(), e);
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