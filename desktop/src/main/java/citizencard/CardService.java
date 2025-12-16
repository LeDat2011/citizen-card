package citizencard;

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
    // INS CODES - Must match Applet
    // =====================================================
    private static final byte INS_INITIALIZE_CARD = (byte) 0x10;
    private static final byte INS_VERIFY_PIN = (byte) 0x20;
    private static final byte INS_CHANGE_PIN = (byte) 0x21;
    private static final byte INS_GET_CARD_ID = (byte) 0x30;
    private static final byte INS_GET_PUBLIC_KEY = (byte) 0x31;
    private static final byte INS_GET_BALANCE = (byte) 0x40;
    private static final byte INS_TOPUP_BALANCE = (byte) 0x41;
    private static final byte INS_PAYMENT = (byte) 0x42;
    
    // AID của Citizen Card Applet
    private static final byte[] APPLET_AID = {
        (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x04, (byte) 0x05,
        (byte) 0x06, (byte) 0x07, (byte) 0x08, (byte) 0x09, (byte) 0x00
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
     * Send APDU command to card
     */
    public byte[] sendCommand(byte ins, byte[] data) {
        if (!connected || channel == null) {
            throw new RuntimeException("Not connected to card");
        }
        
        try {
            byte[] command = buildCommand(ins, data);
            CommandAPDU commandAPDU = new CommandAPDU(command);
            ResponseAPDU response = channel.transmit(commandAPDU);
            
            // Return full response (data + SW)
            return response.getBytes();
            
        } catch (Exception e) {
            throw new RuntimeException("Error sending command to card: " + e.getMessage(), e);
        }
    }
    
    // =====================================================
    // APDU BUILDERS
    // =====================================================
    
    private byte[] buildCommand(byte ins, byte[] data) {
        if (data == null || data.length == 0) {
            return new byte[] { (byte) 0x00, ins, (byte) 0x00, (byte) 0x00 };
        } else {
            byte[] command = new byte[5 + data.length];
            command[0] = (byte) 0x00; // CLA
            command[1] = ins;         // INS
            command[2] = (byte) 0x00; // P1
            command[3] = (byte) 0x00; // P2
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
     * Initialize new card with PIN
     */
    public String initializeCard(String pin) {
        byte[] pinData = buildPinData(pin);
        byte[] response = sendCommand(INS_INITIALIZE_CARD, pinData);
        
        if (isSuccess(response)) {
            byte[] cardIdData = getResponseData(response);
            return new String(cardIdData).trim();
        } else {
            throw new RuntimeException("Failed to initialize card");
        }
    }

    /**
     * Verify PIN
     */
    public boolean verifyPin(String pin) {
        byte[] pinData = buildPinData(pin);
        byte[] response = sendCommand(INS_VERIFY_PIN, pinData);
        
        if (isSuccess(response)) {
            return true;
        } else {
            byte[] data = getResponseData(response);
            if (data.length > 0) {
                int remainingTries = data[0] & 0xFF;
                System.out.println("PIN verification failed. Remaining tries: " + remainingTries);
            }
            return false;
        }
    }

    /**
     * Get Card ID
     */
    public String getCardId() {
        byte[] response = sendCommand(INS_GET_CARD_ID, null);
        
        if (isSuccess(response)) {
            byte[] cardIdData = getResponseData(response);
            return new String(cardIdData).trim();
        } else {
            throw new RuntimeException("Failed to get card ID");
        }
    }

    /**
     * Get Public Key
     */
    public byte[] getPublicKey() {
        byte[] response = sendCommand(INS_GET_PUBLIC_KEY, null);
        
        if (isSuccess(response)) {
            return getResponseData(response);
        } else {
            throw new RuntimeException("Failed to get public key");
        }
    }

    /**
     * Get Balance
     */
    public int getBalance() {
        byte[] response = sendCommand(INS_GET_BALANCE, null);
        
        if (isSuccess(response)) {
            byte[] balanceData = getResponseData(response);
            return parseAmount(balanceData);
        } else {
            throw new RuntimeException("Failed to get balance");
        }
    }

    /**
     * Top up balance
     */
    public int topupBalance(int amount) {
        byte[] amountData = buildAmountData(amount);
        byte[] response = sendCommand(INS_TOPUP_BALANCE, amountData);
        
        if (isSuccess(response)) {
            byte[] newBalanceData = getResponseData(response);
            return parseAmount(newBalanceData);
        } else {
            throw new RuntimeException("Failed to top up balance");
        }
    }

    /**
     * Make payment
     */
    public int makePayment(int amount) {
        byte[] amountData = buildAmountData(amount);
        byte[] response = sendCommand(INS_PAYMENT, amountData);
        
        if (isSuccess(response)) {
            byte[] newBalanceData = getResponseData(response);
            return parseAmount(newBalanceData);
        } else {
            throw new RuntimeException("Payment failed - insufficient funds or other error");
        }
    }

    /**
     * Change PIN
     */
    public boolean changePin(String oldPin, String newPin) {
        byte[] pinData = new byte[8]; // 4 bytes old + 4 bytes new
        System.arraycopy(buildPinData(oldPin), 0, pinData, 0, 4);
        System.arraycopy(buildPinData(newPin), 0, pinData, 4, 4);
        
        byte[] response = sendCommand(INS_CHANGE_PIN, pinData);
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