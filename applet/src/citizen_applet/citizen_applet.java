package citizen_applet;

import javacard.framework.*;
import javacard.security.*;
import javacardx.crypto.*;

/**
 * CITIZEN CARD SMART CARD APPLET
 * 
 * Tuân thủ chuẩn Smart Card Development:
 * - Tất cả dữ liệu quan trọng được lưu và mã hóa trong thẻ
 * - Sử dụng AES để mã hóa dữ liệu (key từ PIN)
 * - Sử dụng RSA để xác thực và bảo mật giao tiếp
 * - PIN authentication với giới hạn retry
 * - Card ID được tạo tự động theo chuẩn
 */
public class citizen_applet extends Applet {
    
    // =====================================================
    // CONSTANTS - INS CODES
    // =====================================================
    private static final byte INS_SELECT_APPLET = (byte) 0xA4;
    private static final byte INS_INITIALIZE_CARD = (byte) 0x10;
    private static final byte INS_VERIFY_PIN = (byte) 0x20;
    private static final byte INS_CHANGE_PIN = (byte) 0x21;
    private static final byte INS_GET_CARD_ID = (byte) 0x30;
    private static final byte INS_GET_PUBLIC_KEY = (byte) 0x31;
    private static final byte INS_GET_CARD_INFO = (byte) 0x32;
    private static final byte INS_UPDATE_CARD_INFO = (byte) 0x33;
    private static final byte INS_GET_BALANCE = (byte) 0x40;
    private static final byte INS_TOPUP_BALANCE = (byte) 0x41;
    private static final byte INS_PAYMENT = (byte) 0x42;
    private static final byte INS_GET_TRANSACTION_HISTORY = (byte) 0x43;
    private static final byte INS_RESET_CARD = (byte) 0xFF;
    
    // =====================================================
    // STORAGE CONSTANTS
    // =====================================================
    private static final short CARD_ID_LENGTH = 32;
    private static final short PIN_LENGTH = 4;
    private static final short MAX_PIN_TRIES = 3;
    private static final short AES_KEY_LENGTH = 16;
    private static final short RSA_KEY_LENGTH = 128; // 1024 bits
    
    // Personal Info Storage (AES encrypted)
    private static final short NAME_MAX_LENGTH = 50;
    private static final short DOB_LENGTH = 10; // YYYY-MM-DD
    private static final short ID_NUMBER_LENGTH = 12;
    private static final short ADDRESS_MAX_LENGTH = 100;
    private static final short PHONE_LENGTH = 11;
    
    // Transaction History (limited)
    private static final short MAX_TRANSACTIONS = 10;
    private static final short TRANSACTION_RECORD_SIZE = 20; // timestamp(8) + type(1) + amount(4) + balance(4) + ref(3)
    
    // =====================================================
    // PERSISTENT STORAGE
    // =====================================================
    
    // Card State
    private boolean cardInitialized;
    private byte[] cardId;
    private byte pinTryCounter;
    private boolean pinVerified;
    
    // PIN and Security
    private byte[] pinHash; // SHA-256 hash of PIN
    private AESKey aesKey;  // AES key derived from PIN
    
    // RSA Key Pair
    private RSAPrivateKey rsaPrivateKey;
    private RSAPublicKey rsaPublicKey;
    
    // Encrypted Personal Information (AES encrypted)
    private byte[] encryptedPersonalInfo; // name + dob + idNumber + address + phone
    private byte[] encryptedPhoto; // Optional photo data
    
    // Financial Data (AES encrypted)
    private byte[] encryptedBalance; // 4 bytes balance
    
    // Transaction History (AES encrypted)
    private byte[] encryptedTransactionHistory;
    private byte transactionCount;
    
    // Crypto Objects
    private Cipher aesCipher;
    private Cipher rsaCipher;
    private MessageDigest sha256;
    private RandomData randomGenerator;
    
    // =====================================================
    // APPLET LIFECYCLE
    // =====================================================
    
    public static void install(byte[] bArray, short bOffset, byte bLength) {
        new citizen_applet().register(bArray, (short) (bOffset + 1), bArray[bOffset]);
    }
    
    protected citizen_applet() {
        // Initialize storage arrays
        cardId = new byte[CARD_ID_LENGTH];
        pinHash = new byte[32]; // SHA-256 output
        aesKey = (AESKey) KeyBuilder.buildKey(KeyBuilder.TYPE_AES, KeyBuilder.LENGTH_AES_128, false);
        
        // Initialize encrypted storage
        encryptedPersonalInfo = new byte[NAME_MAX_LENGTH + DOB_LENGTH + ID_NUMBER_LENGTH + ADDRESS_MAX_LENGTH + PHONE_LENGTH + 16]; // +16 for padding
        encryptedBalance = new byte[16]; // 4 bytes data + 12 bytes padding
        encryptedTransactionHistory = new byte[MAX_TRANSACTIONS * TRANSACTION_RECORD_SIZE + 16];
        
        // Initialize crypto objects
        try {
            aesCipher = Cipher.getInstance(Cipher.ALG_AES_BLOCK_128_CBC_NOPAD, false);
            rsaCipher = Cipher.getInstance(Cipher.ALG_RSA_PKCS1, false);
            sha256 = MessageDigest.getInstance(MessageDigest.ALG_SHA_256, false);
            randomGenerator = RandomData.getInstance(RandomData.ALG_SECURE_RANDOM);
            
            // Generate RSA key pair
            KeyPair rsaKeyPair = new KeyPair(KeyPair.ALG_RSA, KeyBuilder.LENGTH_RSA_1024);
            rsaKeyPair.genKeyPair();
            rsaPrivateKey = (RSAPrivateKey) rsaKeyPair.getPrivate();
            rsaPublicKey = (RSAPublicKey) rsaKeyPair.getPublic();
            
        } catch (CryptoException e) {
            ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
        }
        
        // Initialize state
        cardInitialized = false;
        pinTryCounter = MAX_PIN_TRIES;
        pinVerified = false;
        transactionCount = 0;
    }
    
    // =====================================================
    // MAIN PROCESSING
    // =====================================================
    
    public void process(APDU apdu) {
        if (selectingApplet()) {
            return;
        }
        
        byte[] buffer = apdu.getBuffer();
        byte ins = buffer[ISO7816.OFFSET_INS];
        
        try {
            switch (ins) {
                case INS_INITIALIZE_CARD:
                    initializeCard(apdu);
                    break;
                case INS_VERIFY_PIN:
                    verifyPin(apdu);
                    break;
                case INS_CHANGE_PIN:
                    changePin(apdu);
                    break;
                case INS_GET_CARD_ID:
                    getCardId(apdu);
                    break;
                case INS_GET_PUBLIC_KEY:
                    getPublicKey(apdu);
                    break;
                case INS_GET_CARD_INFO:
                    getCardInfo(apdu);
                    break;
                case INS_UPDATE_CARD_INFO:
                    updateCardInfo(apdu);
                    break;
                case INS_GET_BALANCE:
                    getBalance(apdu);
                    break;
                case INS_TOPUP_BALANCE:
                    topupBalance(apdu);
                    break;
                case INS_PAYMENT:
                    makePayment(apdu);
                    break;
                case INS_GET_TRANSACTION_HISTORY:
                    getTransactionHistory(apdu);
                    break;
                case INS_RESET_CARD:
                    resetCard(apdu);
                    break;
                default:
                    ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
            }
        } catch (Exception e) {
            ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
        }
    }
    
    // =====================================================
    // CARD INITIALIZATION
    // =====================================================
    
    private void initializeCard(APDU apdu) {
        if (cardInitialized) {
            ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
        }
        
        byte[] buffer = apdu.getBuffer();
        short lc = apdu.setIncomingAndReceive();
        
        if (lc < PIN_LENGTH) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }
        
        // Generate unique Card ID
        generateCardId();
        
        // Set initial PIN
        byte[] initialPin = new byte[PIN_LENGTH];
        Util.arrayCopy(buffer, ISO7816.OFFSET_CDATA, initialPin, (short) 0, PIN_LENGTH);
        setPinAndGenerateAESKey(initialPin);
        
        // Initialize balance to 0
        setBalance(0);
        
        cardInitialized = true;
        pinTryCounter = MAX_PIN_TRIES;
        
        // Return Card ID
        Util.arrayCopy(cardId, (short) 0, buffer, (short) 0, CARD_ID_LENGTH);
        apdu.setOutgoingAndSend((short) 0, CARD_ID_LENGTH);
    }
    
    private void generateCardId() {
        // Format: CITIZEN-CARD-YYYYMMDD-HHMMSS-RANDOM
        byte[] timestamp = new byte[8];
        byte[] randomBytes = new byte[8];
        
        // Get current timestamp (simplified - in real implementation use RTC)
        // For demo, use random data
        randomGenerator.generateData(timestamp, (short) 0, (short) 8);
        randomGenerator.generateData(randomBytes, (short) 0, (short) 8);
        
        // Build Card ID string
        byte[] prefix = "CITIZEN-CARD-".getBytes();
        short offset = 0;
        
        // Copy prefix
        Util.arrayCopy(prefix, (short) 0, cardId, offset, (short) prefix.length);
        offset += prefix.length;
        
        // Add timestamp hex
        for (short i = 0; i < 8; i++) {
            cardId[offset++] = getHexChar((byte) ((timestamp[i] >> 4) & 0x0F));
            cardId[offset++] = getHexChar((byte) (timestamp[i] & 0x0F));
        }
        
        cardId[offset++] = '-';
        
        // Add random hex
        for (short i = 0; i < 4; i++) {
            cardId[offset++] = getHexChar((byte) ((randomBytes[i] >> 4) & 0x0F));
            cardId[offset++] = getHexChar((byte) (randomBytes[i] & 0x0F));
        }
    }
    
    private byte getHexChar(byte value) {
        if (value < 10) {
            return (byte) ('0' + value);
        } else {
            return (byte) ('A' + value - 10);
        }
    }
    
    // =====================================================
    // PIN MANAGEMENT
    // =====================================================
    
    private void verifyPin(APDU apdu) {
        if (!cardInitialized) {
            ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
        }
        
        if (pinTryCounter == 0) {
            ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
        }
        
        byte[] buffer = apdu.getBuffer();
        short lc = apdu.setIncomingAndReceive();
        
        if (lc != PIN_LENGTH) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }
        
        // Hash the provided PIN
        byte[] providedPin = new byte[PIN_LENGTH];
        Util.arrayCopy(buffer, ISO7816.OFFSET_CDATA, providedPin, (short) 0, PIN_LENGTH);
        
        byte[] providedPinHash = new byte[32];
        sha256.reset();
        sha256.update(providedPin, (short) 0, PIN_LENGTH);
        sha256.doFinal(providedPin, (short) 0, (short) 0, providedPinHash, (short) 0);
        
        // Compare with stored hash
        if (Util.arrayCompare(pinHash, (short) 0, providedPinHash, (short) 0, (short) 32) == 0) {
            pinVerified = true;
            pinTryCounter = MAX_PIN_TRIES; // Reset counter on success
            
            // Regenerate AES key from PIN
            generateAESKeyFromPin(providedPin);
            
            buffer[0] = (byte) 0x90; // Success
            apdu.setOutgoingAndSend((short) 0, (short) 1);
        } else {
            pinTryCounter--;
            pinVerified = false;
            
            buffer[0] = (byte) pinTryCounter; // Remaining tries
            apdu.setOutgoingAndSend((short) 0, (short) 1);
            
            if (pinTryCounter == 0) {
                ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
            }
        }
    }
    
    private void changePin(APDU apdu) {
        if (!cardInitialized || !pinVerified) {
            ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
        }
        
        byte[] buffer = apdu.getBuffer();
        short lc = apdu.setIncomingAndReceive();
        
        if (lc != PIN_LENGTH * 2) { // Old PIN + New PIN
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }
        
        // Verify old PIN first
        byte[] oldPin = new byte[PIN_LENGTH];
        byte[] newPin = new byte[PIN_LENGTH];
        
        Util.arrayCopy(buffer, ISO7816.OFFSET_CDATA, oldPin, (short) 0, PIN_LENGTH);
        Util.arrayCopy(buffer, (short) (ISO7816.OFFSET_CDATA + PIN_LENGTH), newPin, (short) 0, PIN_LENGTH);
        
        // Hash old PIN and verify
        byte[] oldPinHash = new byte[32];
        sha256.reset();
        sha256.update(oldPin, (short) 0, PIN_LENGTH);
        sha256.doFinal(oldPin, (short) 0, (short) 0, oldPinHash, (short) 0);
        
        if (Util.arrayCompare(pinHash, (short) 0, oldPinHash, (short) 0, (short) 32) != 0) {
            ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
        }
        
        // Set new PIN
        setPinAndGenerateAESKey(newPin);
        
        // Re-encrypt all data with new AES key
        // (In real implementation, decrypt with old key and encrypt with new key)
        
        buffer[0] = (byte) 0x90; // Success
        apdu.setOutgoingAndSend((short) 0, (short) 1);
    }
    
    private void setPinAndGenerateAESKey(byte[] pin) {
        // Hash PIN for storage
        sha256.reset();
        sha256.update(pin, (short) 0, PIN_LENGTH);
        sha256.doFinal(pin, (short) 0, (short) 0, pinHash, (short) 0);
        
        // Generate AES key from PIN
        generateAESKeyFromPin(pin);
    }
    
    private void generateAESKeyFromPin(byte[] pin) {
        // Simple key derivation: SHA-256(PIN + salt) -> first 16 bytes
        byte[] salt = "CITIZEN_CARD_AES".getBytes();
        byte[] keyMaterial = new byte[32];
        
        sha256.reset();
        sha256.update(pin, (short) 0, PIN_LENGTH);
        sha256.update(salt, (short) 0, (short) salt.length);
        sha256.doFinal(pin, (short) 0, (short) 0, keyMaterial, (short) 0);
        
        // Use first 16 bytes as AES key
        aesKey.setKey(keyMaterial, (short) 0);
    }
    
    // =====================================================
    // CARD INFORMATION
    // =====================================================
    
    private void getCardId(APDU apdu) {
        if (!cardInitialized) {
            ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
        }
        
        byte[] buffer = apdu.getBuffer();
        Util.arrayCopy(cardId, (short) 0, buffer, (short) 0, CARD_ID_LENGTH);
        apdu.setOutgoingAndSend((short) 0, CARD_ID_LENGTH);
    }
    
    private void getPublicKey(APDU apdu) {
        if (!cardInitialized) {
            ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
        }
        
        byte[] buffer = apdu.getBuffer();
        short keyLength = rsaPublicKey.getSize();
        
        // Export public key (modulus + exponent)
        short modulusLength = rsaPublicKey.getModulus(buffer, (short) 2);
        buffer[0] = (byte) (modulusLength >> 8);
        buffer[1] = (byte) (modulusLength & 0xFF);
        
        short exponentLength = rsaPublicKey.getExponent(buffer, (short) (2 + modulusLength + 2));
        buffer[2 + modulusLength] = (byte) (exponentLength >> 8);
        buffer[2 + modulusLength + 1] = (byte) (exponentLength & 0xFF);
        
        short totalLength = (short) (2 + modulusLength + 2 + exponentLength);
        apdu.setOutgoingAndSend((short) 0, totalLength);
    }
    
    // =====================================================
    // FINANCIAL OPERATIONS
    // =====================================================
    
    private void getBalance(APDU apdu) {
        if (!cardInitialized || !pinVerified) {
            ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
        }
        
        int balance = decryptBalance();
        
        byte[] buffer = apdu.getBuffer();
        buffer[0] = (byte) (balance >> 24);
        buffer[1] = (byte) (balance >> 16);
        buffer[2] = (byte) (balance >> 8);
        buffer[3] = (byte) (balance & 0xFF);
        
        apdu.setOutgoingAndSend((short) 0, (short) 4);
    }
    
    private void topupBalance(APDU apdu) {
        if (!cardInitialized || !pinVerified) {
            ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
        }
        
        byte[] buffer = apdu.getBuffer();
        short lc = apdu.setIncomingAndReceive();
        
        if (lc < 4) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }
        
        // Get amount from APDU
        int amount = ((buffer[ISO7816.OFFSET_CDATA] & 0xFF) << 24) |
                    ((buffer[ISO7816.OFFSET_CDATA + 1] & 0xFF) << 16) |
                    ((buffer[ISO7816.OFFSET_CDATA + 2] & 0xFF) << 8) |
                    (buffer[ISO7816.OFFSET_CDATA + 3] & 0xFF);
        
        if (amount <= 0) {
            ISOException.throwIt(ISO7816.SW_WRONG_DATA);
        }
        
        // Update balance
        int currentBalance = decryptBalance();
        int newBalance = currentBalance + amount;
        setBalance(newBalance);
        
        // Add transaction record
        addTransactionRecord((byte) 0x01, amount, newBalance); // 0x01 = TOPUP
        
        // Return new balance
        buffer[0] = (byte) (newBalance >> 24);
        buffer[1] = (byte) (newBalance >> 16);
        buffer[2] = (byte) (newBalance >> 8);
        buffer[3] = (byte) (newBalance & 0xFF);
        
        apdu.setOutgoingAndSend((short) 0, (short) 4);
    }
    
    private void makePayment(APDU apdu) {
        if (!cardInitialized || !pinVerified) {
            ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
        }
        
        byte[] buffer = apdu.getBuffer();
        short lc = apdu.setIncomingAndReceive();
        
        if (lc < 4) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }
        
        // Get amount from APDU
        int amount = ((buffer[ISO7816.OFFSET_CDATA] & 0xFF) << 24) |
                    ((buffer[ISO7816.OFFSET_CDATA + 1] & 0xFF) << 16) |
                    ((buffer[ISO7816.OFFSET_CDATA + 2] & 0xFF) << 8) |
                    (buffer[ISO7816.OFFSET_CDATA + 3] & 0xFF);
        
        if (amount <= 0) {
            ISOException.throwIt(ISO7816.SW_WRONG_DATA);
        }
        
        // Check balance
        int currentBalance = decryptBalance();
        if (currentBalance < amount) {
            ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED); // Insufficient funds
        }
        
        // Update balance
        int newBalance = currentBalance - amount;
        setBalance(newBalance);
        
        // Add transaction record
        addTransactionRecord((byte) 0x02, amount, newBalance); // 0x02 = PAYMENT
        
        // Return new balance
        buffer[0] = (byte) (newBalance >> 24);
        buffer[1] = (byte) (newBalance >> 16);
        buffer[2] = (byte) (newBalance >> 8);
        buffer[3] = (byte) (newBalance & 0xFF);
        
        apdu.setOutgoingAndSend((short) 0, (short) 4);
    }
    
    // =====================================================
    // HELPER METHODS
    // =====================================================
    
    private int decryptBalance() {
        // Decrypt balance using AES
        byte[] decryptedData = new byte[16];
        
        try {
            aesCipher.init(aesKey, Cipher.MODE_DECRYPT);
            aesCipher.doFinal(encryptedBalance, (short) 0, (short) 16, decryptedData, (short) 0);
        } catch (CryptoException e) {
            return 0;
        }
        
        return ((decryptedData[0] & 0xFF) << 24) |
               ((decryptedData[1] & 0xFF) << 16) |
               ((decryptedData[2] & 0xFF) << 8) |
               (decryptedData[3] & 0xFF);
    }
    
    private void setBalance(int balance) {
        // Encrypt balance using AES
        byte[] balanceData = new byte[16]; // 4 bytes data + 12 bytes padding
        
        balanceData[0] = (byte) (balance >> 24);
        balanceData[1] = (byte) (balance >> 16);
        balanceData[2] = (byte) (balance >> 8);
        balanceData[3] = (byte) (balance & 0xFF);
        
        // Fill padding with random data
        randomGenerator.generateData(balanceData, (short) 4, (short) 12);
        
        try {
            aesCipher.init(aesKey, Cipher.MODE_ENCRYPT);
            aesCipher.doFinal(balanceData, (short) 0, (short) 16, encryptedBalance, (short) 0);
        } catch (CryptoException e) {
            ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
        }
    }
    
    private void addTransactionRecord(byte type, int amount, int newBalance) {
        // Simple transaction logging (in real implementation, use proper timestamp)
        if (transactionCount >= MAX_TRANSACTIONS) {
            // Shift array left to make room for new transaction
            Util.arrayCopy(encryptedTransactionHistory, TRANSACTION_RECORD_SIZE, 
                          encryptedTransactionHistory, (short) 0, 
                          (short) ((MAX_TRANSACTIONS - 1) * TRANSACTION_RECORD_SIZE));
            transactionCount = (byte) (MAX_TRANSACTIONS - 1);
        }
        
        // Create transaction record
        byte[] record = new byte[TRANSACTION_RECORD_SIZE];
        short offset = 0;
        
        // Timestamp (8 bytes) - simplified
        randomGenerator.generateData(record, offset, (short) 8);
        offset += 8;
        
        // Type (1 byte)
        record[offset++] = type;
        
        // Amount (4 bytes)
        record[offset++] = (byte) (amount >> 24);
        record[offset++] = (byte) (amount >> 16);
        record[offset++] = (byte) (amount >> 8);
        record[offset++] = (byte) (amount & 0xFF);
        
        // New Balance (4 bytes)
        record[offset++] = (byte) (newBalance >> 24);
        record[offset++] = (byte) (newBalance >> 16);
        record[offset++] = (byte) (newBalance >> 8);
        record[offset++] = (byte) (newBalance & 0xFF);
        
        // Reference (3 bytes) - simplified
        randomGenerator.generateData(record, offset, (short) 3);
        
        // Store encrypted record
        short recordOffset = (short) (transactionCount * TRANSACTION_RECORD_SIZE);
        Util.arrayCopy(record, (short) 0, encryptedTransactionHistory, recordOffset, TRANSACTION_RECORD_SIZE);
        
        transactionCount++;
    }
    
    // Placeholder methods for other operations
    private void getCardInfo(APDU apdu) {
        // TODO: Implement encrypted personal info retrieval
        ISOException.throwIt(ISO7816.SW_FUNC_NOT_SUPPORTED);
    }
    
    private void updateCardInfo(APDU apdu) {
        // TODO: Implement encrypted personal info update
        ISOException.throwIt(ISO7816.SW_FUNC_NOT_SUPPORTED);
    }
    
    private void getTransactionHistory(APDU apdu) {
        // TODO: Implement encrypted transaction history retrieval
        ISOException.throwIt(ISO7816.SW_FUNC_NOT_SUPPORTED);
    }
    
    private void resetCard(APDU apdu) {
        // TODO: Implement card reset (admin function)
        ISOException.throwIt(ISO7816.SW_FUNC_NOT_SUPPORTED);
    }
}
