	package citizen_applet;

import javacard.framework.*;
import javacard.security.*;
import javacardx.crypto.*;

/**
 * CITIZEN CARD SMART CARD APPLET v2.0 (JavaCard 2.2.1 Compatible)
 * 
 * Features:
 * - MD5 PIN hashing with 5 retry attempts
 * - AES-128-ECB encryption
 * - RSA-1024 digital signature
 * - Card activation/deactivation
 * - Photo storage up to 8KB (chunked transfer)
 */
public class citizen_applet extends Applet {
    
    // =====================================================
    // INS CODES
    // =====================================================
    private static final byte INS_VERIFY = (byte) 0x00;
    private static final byte INS_CREATE = (byte) 0x01;
    private static final byte INS_GET = (byte) 0x02;
    private static final byte INS_UPDATE = (byte) 0x03;
    private static final byte INS_RESET_TRY_PIN = (byte) 0x10;
    
    // =====================================================
    // P1 PARAMETERS (Command Type)
    // =====================================================
    private static final byte P1_PIN = (byte) 0x04;
    private static final byte P1_CITIZEN_INFO = (byte) 0x05;
    private static final byte P1_SIGNATURE = (byte) 0x06;
    private static final byte P1_FORGET_PIN = (byte) 0x0A;
    private static final byte P1_ACTIVATE_CARD = (byte) 0x0B;
    private static final byte P1_DEACTIVATE_CARD = (byte) 0x0C;
    
    // =====================================================
    // P2 PARAMETERS (Data Type)
    // =====================================================
    private static final byte P2_INFORMATION = (byte) 0x07;
    private static final byte P2_TRY_REMAINING = (byte) 0x08;
    private static final byte P2_AVATAR = (byte) 0x09;
    private static final byte P2_CARD_ID = (byte) 0x0A;
    private static final byte P2_PUBLIC_KEY = (byte) 0x0B;
    private static final byte P2_BALANCE = (byte) 0x0C;
    
    // =====================================================
    // CONSTANTS
    // =====================================================
    private static final byte PIN_LENGTH = 4;
    private static final byte MAX_PIN_TRIES = 5;
    private static final short CARD_ID_LENGTH = 32;
    private static final short MAX_INFO_LENGTH = 256;
    private static final short MAX_AVATAR_SIZE = 8192;   // 8KB (JavaCard 2.2.1 limit)
    private static final short RSA_KEY_LENGTH = 1024;
    
    // =====================================================
    // PERSISTENT STORAGE
    // =====================================================
    
    // Card State
    private boolean cardInitialized;
    private boolean cardActive;
    private byte[] cardId;
    
    // PIN Management
    private byte[] pinHash;          // MD5 hash of PIN (16 bytes)
    private byte pinTryCounter;
    private boolean pinVerified;
    
    // Crypto Keys
    private AESKey aesKey;
    private RSAPrivateKey rsaPrivateKey;
    private RSAPublicKey rsaPublicKey;
    
    // Encrypted Data Storage
    private byte[] encryptedInfo;
    private short encryptedInfoLength;
    
    private byte[] avatar;
    private short avatarLength;
    
    private byte[] encryptedBalance;
    
    // Crypto helpers
    private Cipher aesCipher;
    private Signature rsaSignature;
    private MessageDigest md5;
    private RandomData randomGenerator;
    
    // Temporary buffers
    private byte[] tempBuffer;
    
    // =====================================================
    // APPLET LIFECYCLE
    // =====================================================
    
    public static void install(byte[] bArray, short bOffset, byte bLength) {
        new citizen_applet().register(bArray, (short) (bOffset + 1), bArray[bOffset]);
    }
    
    protected citizen_applet() {
        // Initialize storage
        cardId = new byte[CARD_ID_LENGTH];
        pinHash = new byte[16];  // MD5 = 16 bytes
        encryptedInfo = new byte[MAX_INFO_LENGTH + 16];  // + padding
        avatar = new byte[MAX_AVATAR_SIZE];
        encryptedBalance = new byte[16];
        tempBuffer = new byte[256];
        
        // Initialize crypto
        aesKey = (AESKey) KeyBuilder.buildKey(KeyBuilder.TYPE_AES, KeyBuilder.LENGTH_AES_128, false);
        aesCipher = Cipher.getInstance(Cipher.ALG_AES_BLOCK_128_ECB_NOPAD, false);
        rsaSignature = Signature.getInstance(Signature.ALG_RSA_SHA_PKCS1, false);
        md5 = MessageDigest.getInstance(MessageDigest.ALG_MD5, false);
        randomGenerator = RandomData.getInstance(RandomData.ALG_SECURE_RANDOM);
        
        // Generate RSA key pair
        KeyPair rsaKeyPair = new KeyPair(KeyPair.ALG_RSA, KeyBuilder.LENGTH_RSA_1024);
        rsaKeyPair.genKeyPair();
        rsaPrivateKey = (RSAPrivateKey) rsaKeyPair.getPrivate();
        rsaPublicKey = (RSAPublicKey) rsaKeyPair.getPublic();
        
        // Initialize state
        cardInitialized = false;
        cardActive = false;
        pinTryCounter = MAX_PIN_TRIES;
        pinVerified = false;
        encryptedInfoLength = 0;
        avatarLength = 0;
    }
    
    // =====================================================
    // MAIN APDU PROCESSING
    // =====================================================
    
    public void process(APDU apdu) {
        if (selectingApplet()) {
            return;
        }
        
        byte[] buffer = apdu.getBuffer();
        byte ins = buffer[ISO7816.OFFSET_INS];
        byte p1 = buffer[ISO7816.OFFSET_P1];
        byte p2 = buffer[ISO7816.OFFSET_P2];
        
        // Card activation check (except for activation command itself)
        if (!cardActive && ins != INS_CREATE && !(ins == INS_UPDATE && p1 == P1_ACTIVATE_CARD)) {
            if (cardInitialized) {
                ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
            }
        }
        
        switch (ins) {
            case INS_VERIFY:
                processVerify(apdu, p1, p2);
                break;
            case INS_CREATE:
                processCreate(apdu, p1, p2);
                break;
            case INS_GET:
                processGet(apdu, p1, p2);
                break;
            case INS_UPDATE:
                processUpdate(apdu, p1, p2);
                break;
            case INS_RESET_TRY_PIN:
                resetPinTries(apdu);
                break;
            default:
                ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
        }
    }
    
    // =====================================================
    // INS_VERIFY (0x00)
    // =====================================================
    
    private void processVerify(APDU apdu, byte p1, byte p2) {
        if (p1 == P1_PIN) {
            verifyPin(apdu);
        } else {
            ISOException.throwIt(ISO7816.SW_WRONG_P1P2);
        }
    }
    
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
        
        // Hash provided PIN with MD5
        byte[] providedPinHash = new byte[16];
        md5.reset();
        md5.doFinal(buffer, ISO7816.OFFSET_CDATA, PIN_LENGTH, providedPinHash, (short) 0);
        
        // Compare hashes
        if (Util.arrayCompare(pinHash, (short) 0, providedPinHash, (short) 0, (short) 16) == 0) {
            pinVerified = true;
            pinTryCounter = MAX_PIN_TRIES;
            
            // Generate AES key from PIN
            generateAesKeyFromPin(buffer, ISO7816.OFFSET_CDATA);
            
            // Return success
            buffer[0] = (byte) 0x01;  // Success
            buffer[1] = pinTryCounter;
            apdu.setOutgoingAndSend((short) 0, (short) 2);
        } else {
            pinTryCounter--;
            pinVerified = false;
            
            // Return failure with remaining tries
            buffer[0] = (byte) 0x00;  // Failure
            buffer[1] = pinTryCounter;
            apdu.setOutgoingAndSend((short) 0, (short) 2);
            
            if (pinTryCounter == 0) {
                cardActive = false;  // Deactivate card
            }
        }
    }
    
    // =====================================================
    // INS_CREATE (0x01)
    // =====================================================
    
    private void processCreate(APDU apdu, byte p1, byte p2) {
        if (p1 == P1_PIN) {
            initializeCard(apdu);
        } else if (p1 == P1_CITIZEN_INFO && p2 == P2_AVATAR) {
            createAvatar(apdu);
        } else {
            ISOException.throwIt(ISO7816.SW_WRONG_P1P2);
        }
    }
    
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
        
        // Hash PIN with MD5
        md5.reset();
        md5.doFinal(buffer, ISO7816.OFFSET_CDATA, PIN_LENGTH, pinHash, (short) 0);
        
        // Generate AES key from PIN
        generateAesKeyFromPin(buffer, ISO7816.OFFSET_CDATA);
        
        // Initialize balance to 0
        setBalance(0);
        
        // Activate card
        cardInitialized = true;
        cardActive = true;
        pinTryCounter = MAX_PIN_TRIES;
        pinVerified = true;
        
        // Return Card ID
        Util.arrayCopy(cardId, (short) 0, buffer, (short) 0, CARD_ID_LENGTH);
        apdu.setOutgoingAndSend((short) 0, CARD_ID_LENGTH);
    }
    
    private void createAvatar(APDU apdu) {
        if (!cardInitialized || !pinVerified) {
            ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
        }
        
        byte[] buffer = apdu.getBuffer();
        
        // Standard APDU - receive data
        short lc = apdu.setIncomingAndReceive();
        
        if (lc > MAX_AVATAR_SIZE) {
            ISOException.throwIt(ISO7816.SW_FILE_FULL);
        }
        
        // Copy received data
        Util.arrayCopy(buffer, ISO7816.OFFSET_CDATA, avatar, (short) 0, lc);
        avatarLength = lc;
        
        // Return avatar length
        Util.setShort(buffer, (short) 0, avatarLength);
        apdu.setOutgoingAndSend((short) 0, (short) 2);
    }
    
    // =====================================================
    // INS_GET (0x02)
    // =====================================================
    
    private void processGet(APDU apdu, byte p1, byte p2) {
        if (!cardInitialized) {
            ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
        }
        
        byte[] buffer = apdu.getBuffer();
        
        switch (p2) {
            case P2_CARD_ID:
                Util.arrayCopy(cardId, (short) 0, buffer, (short) 0, CARD_ID_LENGTH);
                apdu.setOutgoingAndSend((short) 0, CARD_ID_LENGTH);
                break;
                
            case P2_TRY_REMAINING:
                buffer[0] = pinTryCounter;
                apdu.setOutgoingAndSend((short) 0, (short) 1);
                break;
                
            case P2_PUBLIC_KEY:
                short keyLen = serializePublicKey(rsaPublicKey, buffer, (short) 0);
                apdu.setOutgoingAndSend((short) 0, keyLen);
                break;
                
            case P2_AVATAR:
                getAvatar(apdu);
                break;
                
            case P2_BALANCE:
                if (!pinVerified) {
                    ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
                }
                getBalance(apdu);
                break;
                
            case P2_INFORMATION:
                if (!pinVerified) {
                    ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
                }
                getInfo(apdu);
                break;
                
            default:
                ISOException.throwIt(ISO7816.SW_WRONG_P1P2);
        }
    }
    
    private void getAvatar(APDU apdu) {
        if (avatarLength == 0) {
            ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
        }
        
        byte[] buffer = apdu.getBuffer();
        
        // Standard APDU - send data (limited to 256 bytes)
        short sendLength = avatarLength > 256 ? (short) 256 : avatarLength;
        Util.arrayCopy(avatar, (short) 0, buffer, (short) 0, sendLength);
        apdu.setOutgoingAndSend((short) 0, sendLength);
    }
    
    private void getBalance(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        
        // Decrypt balance
        aesDecode(encryptedBalance, (short) 0, (short) 16, aesKey, buffer, (short) 0);
        apdu.setOutgoingAndSend((short) 0, (short) 4);
    }
    
    private void getInfo(APDU apdu) {
        if (encryptedInfoLength == 0) {
            ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
        }
        
        byte[] buffer = apdu.getBuffer();
        
        // Decrypt info
        short len = aesDecode(encryptedInfo, (short) 0, encryptedInfoLength, aesKey, buffer, (short) 0);
        apdu.setOutgoingAndSend((short) 0, len);
    }
    
    // =====================================================
    // INS_UPDATE (0x03)
    // =====================================================
    
    private void processUpdate(APDU apdu, byte p1, byte p2) {
        switch (p1) {
            case P1_PIN:
                updatePin(apdu);
                break;
            case P1_CITIZEN_INFO:
                if (p2 == P2_AVATAR) {
                    createAvatar(apdu);  // Same as create
                } else if (p2 == P2_INFORMATION) {
                    updateInfo(apdu);
                } else if (p2 == P2_BALANCE) {
                    updateBalance(apdu);
                }
                break;
            case P1_SIGNATURE:
                signData(apdu);
                break;
            case P1_ACTIVATE_CARD:
                activateCard(apdu);
                break;
            case P1_DEACTIVATE_CARD:
                deactivateCard(apdu);
                break;
            case P1_FORGET_PIN:
                forgetPin(apdu);
                break;
            default:
                ISOException.throwIt(ISO7816.SW_WRONG_P1P2);
        }
    }
    
    private void updatePin(APDU apdu) {
        if (!cardInitialized || !pinVerified) {
            ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
        }
        
        byte[] buffer = apdu.getBuffer();
        short lc = apdu.setIncomingAndReceive();
        
        if (lc != PIN_LENGTH * 2) {  // Old PIN + New PIN
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }
        
        // Verify old PIN
        byte[] oldPinHash = new byte[16];
        md5.reset();
        md5.doFinal(buffer, ISO7816.OFFSET_CDATA, PIN_LENGTH, oldPinHash, (short) 0);
        
        if (Util.arrayCompare(pinHash, (short) 0, oldPinHash, (short) 0, (short) 16) != 0) {
            ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
        }
        
        // Hash new PIN
        md5.reset();
        md5.doFinal(buffer, (short) (ISO7816.OFFSET_CDATA + PIN_LENGTH), PIN_LENGTH, pinHash, (short) 0);
        
        // Generate new AES key
        generateAesKeyFromPin(buffer, (short) (ISO7816.OFFSET_CDATA + PIN_LENGTH));
        
        // Return success
        buffer[0] = (byte) 0x01;
        apdu.setOutgoingAndSend((short) 0, (short) 1);
    }
    
    private void updateInfo(APDU apdu) {
        if (!cardInitialized || !pinVerified) {
            ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
        }
        
        byte[] buffer = apdu.getBuffer();
        short lc = apdu.setIncomingAndReceive();
        
        if (lc > MAX_INFO_LENGTH) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }
        
        // Encrypt info
        encryptedInfoLength = aesEncode(buffer, ISO7816.OFFSET_CDATA, lc, aesKey, encryptedInfo);
        
        // Return success
        buffer[0] = (byte) 0x01;
        apdu.setOutgoingAndSend((short) 0, (short) 1);
    }
    
    private void updateBalance(APDU apdu) {
        if (!cardInitialized || !pinVerified) {
            ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
        }
        
        byte[] buffer = apdu.getBuffer();
        short lc = apdu.setIncomingAndReceive();
        
        if (lc < 5) {  // 1 byte type + 4 bytes amount
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }
        
        byte type = buffer[ISO7816.OFFSET_CDATA];  // 0x01 = topup, 0x02 = payment
        int amount = getInt(buffer, (short) (ISO7816.OFFSET_CDATA + 1));
        
        // Get current balance
        aesDecode(encryptedBalance, (short) 0, (short) 16, aesKey, tempBuffer, (short) 0);
        int currentBalance = getInt(tempBuffer, (short) 0);
        
        int newBalance;
        if (type == 0x01) {  // Topup
            newBalance = currentBalance + amount;
        } else if (type == 0x02) {  // Payment
            if (currentBalance < amount) {
                ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
            }
            newBalance = currentBalance - amount;
        } else {
            ISOException.throwIt(ISO7816.SW_WRONG_DATA);
            return;
        }
        
        setBalance(newBalance);
        
        // Return new balance
        putInt(buffer, (short) 0, newBalance);
        apdu.setOutgoingAndSend((short) 0, (short) 4);
    }
    
    private void signData(APDU apdu) {
        if (!cardInitialized || !pinVerified) {
            ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
        }
        
        byte[] buffer = apdu.getBuffer();
        short lc = apdu.setIncomingAndReceive();
        
        // Sign data with RSA private key
        short sigLen = rsaSign(rsaPrivateKey, buffer, ISO7816.OFFSET_CDATA, lc, buffer, (short) 0);
        
        apdu.setOutgoingAndSend((short) 0, sigLen);
    }
    
    private void activateCard(APDU apdu) {
        if (!cardInitialized) {
            ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
        }
        
        // Require PIN verification
        byte[] buffer = apdu.getBuffer();
        short lc = apdu.setIncomingAndReceive();
        
        if (lc == PIN_LENGTH) {
            // Verify PIN first
            byte[] providedPinHash = new byte[16];
            md5.reset();
            md5.doFinal(buffer, ISO7816.OFFSET_CDATA, PIN_LENGTH, providedPinHash, (short) 0);
            
            if (Util.arrayCompare(pinHash, (short) 0, providedPinHash, (short) 0, (short) 16) == 0) {
                cardActive = true;
                pinVerified = true;
                pinTryCounter = MAX_PIN_TRIES;
                generateAesKeyFromPin(buffer, ISO7816.OFFSET_CDATA);
                
                buffer[0] = (byte) 0x01;
                apdu.setOutgoingAndSend((short) 0, (short) 1);
                return;
            }
        }
        
        ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
    }
    
    private void deactivateCard(APDU apdu) {
        if (!cardInitialized || !pinVerified) {
            ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
        }
        
        cardActive = false;
        pinVerified = false;
        
        byte[] buffer = apdu.getBuffer();
        buffer[0] = (byte) 0x01;
        apdu.setOutgoingAndSend((short) 0, (short) 1);
    }
    
    private void forgetPin(APDU apdu) {
        // Admin function - reset PIN with master key
        // In real implementation, would require admin authentication
        byte[] buffer = apdu.getBuffer();
        short lc = apdu.setIncomingAndReceive();
        
        if (lc != PIN_LENGTH) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }
        
        // Set new PIN
        md5.reset();
        md5.doFinal(buffer, ISO7816.OFFSET_CDATA, PIN_LENGTH, pinHash, (short) 0);
        
        pinTryCounter = MAX_PIN_TRIES;
        
        buffer[0] = (byte) 0x01;
        apdu.setOutgoingAndSend((short) 0, (short) 1);
    }
    
    // =====================================================
    // INS_RESET_TRY_PIN (0x10)
    // =====================================================
    
    private void resetPinTries(APDU apdu) {
        // Admin function - would require authentication in production
        pinTryCounter = MAX_PIN_TRIES;
        
        byte[] buffer = apdu.getBuffer();
        buffer[0] = pinTryCounter;
        apdu.setOutgoingAndSend((short) 0, (short) 1);
    }
    
    // =====================================================
    // HELPER METHODS
    // =====================================================
    
    private void generateCardId() {
        byte[] prefix = {
            (byte)'C', (byte)'I', (byte)'T', (byte)'I', (byte)'Z', (byte)'E', (byte)'N', (byte)'-'
        };
        
        short offset = 0;
        Util.arrayCopy(prefix, (short) 0, cardId, offset, (short) prefix.length);
        offset += prefix.length;
        
        // Generate random bytes for unique ID
        byte[] randomBytes = new byte[12];
        randomGenerator.generateData(randomBytes, (short) 0, (short) 12);
        
        // Convert to hex string
        for (short i = 0; i < 12; i++) {
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
    
    private void generateAesKeyFromPin(byte[] pin, short offset) {
        // Derive AES key from PIN using MD5
        byte[] keyMaterial = new byte[16];
        md5.reset();
        md5.update(pin, offset, PIN_LENGTH);
        
        // Add salt
        byte[] salt = {(byte)'C', (byte)'I', (byte)'T', (byte)'I', (byte)'Z', (byte)'E', (byte)'N'};
        md5.doFinal(salt, (short) 0, (short) salt.length, keyMaterial, (short) 0);
        
        aesKey.setKey(keyMaterial, (short) 0);
    }
    
    private void setBalance(int balance) {
        tempBuffer[0] = (byte) (balance >> 24);
        tempBuffer[1] = (byte) (balance >> 16);
        tempBuffer[2] = (byte) (balance >> 8);
        tempBuffer[3] = (byte) (balance & 0xFF);
        
        // Pad to 16 bytes
        Util.arrayFillNonAtomic(tempBuffer, (short) 4, (short) 12, (byte) 0x00);
        
        aesEncode(tempBuffer, (short) 0, (short) 16, aesKey, encryptedBalance);
    }
    
    private int getInt(byte[] buffer, short offset) {
        return ((buffer[offset] & 0xFF) << 24) |
               ((buffer[(short)(offset + 1)] & 0xFF) << 16) |
               ((buffer[(short)(offset + 2)] & 0xFF) << 8) |
               (buffer[(short)(offset + 3)] & 0xFF);
    }
    
    private void putInt(byte[] buffer, short offset, int value) {
        buffer[offset] = (byte) (value >> 24);
        buffer[(short)(offset + 1)] = (byte) (value >> 16);
        buffer[(short)(offset + 2)] = (byte) (value >> 8);
        buffer[(short)(offset + 3)] = (byte) (value & 0xFF);
    }
    
    // =====================================================
    // AES HELPER METHODS
    // =====================================================
    
    private short applyPadding(byte[] input, short offset, short length) {
        short paddedLength = (short) (length + (16 - (length % 16)));
        Util.arrayFillNonAtomic(input, (short) (length + offset), (short) (paddedLength - length), (byte) 0x00);
        return paddedLength;
    }
    
    private short removePadding(byte[] output, short length) {
        byte paddingValue = 0x00;
        short paddingLength = 0;
        for (short i = (short) (length - 1); i >= 0; i--) {
            if (output[i] != paddingValue) {
                break;
            }
            paddingLength++;
        }
        return (short) (length - paddingLength);
    }
    
    private short aesEncode(byte[] input, short offset, short length, AESKey key, byte[] output) {
        if (length < 1) {
            ISOException.throwIt(ISO7816.SW_DATA_INVALID);
        }
        short paddedLength = applyPadding(input, offset, length);
        aesCipher.init(key, Cipher.MODE_ENCRYPT);
        aesCipher.doFinal(input, offset, paddedLength, output, (short) 0);
        return paddedLength;
    }
    
    private short aesDecode(byte[] input, short inOffset, short inLength, AESKey key, byte[] output, short outOffset) {
        if ((short) (inLength % 16) != 0) {
            ISOException.throwIt(ISO7816.SW_DATA_INVALID);
        }
        aesCipher.init(key, Cipher.MODE_DECRYPT);
        aesCipher.doFinal(input, inOffset, inLength, output, outOffset);
        return removePadding(output, inLength);
    }
    
    // =====================================================
    // RSA HELPER METHODS
    // =====================================================
    
    private short rsaSign(RSAPrivateKey privateKey, byte[] input, short inputOffset, short inputLength, 
                         byte[] output, short outputOffset) {
        rsaSignature.init(privateKey, Signature.MODE_SIGN);
        return rsaSignature.sign(input, inputOffset, inputLength, output, outputOffset);
    }
    
    private static short serializePublicKey(RSAPublicKey publicKey, byte[] buffer, short offset) {
        short currentOffset = offset;
        byte[] tempBuffer = new byte[128];
        
        // Get exponent
        short expLength = publicKey.getExponent(tempBuffer, (short) 0);
        Util.setShort(buffer, currentOffset, expLength);
        currentOffset += 2;
        Util.arrayCopy(tempBuffer, (short) 0, buffer, currentOffset, expLength);
        currentOffset += expLength;
        
        // Get modulus
        short modLength = publicKey.getModulus(tempBuffer, (short) 0);
        Util.setShort(buffer, currentOffset, modLength);
        currentOffset += 2;
        Util.arrayCopy(tempBuffer, (short) 0, buffer, currentOffset, modLength);
        currentOffset += modLength;
        
        return (short) (currentOffset - offset);
    }
}
