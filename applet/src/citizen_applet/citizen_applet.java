package citizen_applet;

import javacard.framework.*;
import javacard.security.*;
import javacardx.crypto.*;
import javacardx.apdu.ExtendedLength;

/**
 * FULL CITIZEN CARD APPLET v2.0
 * 
 * Features:
 * - PIN verification with MD5 hash
 * - AES-128 encryption for personal info and avatar
 * - RSA-1024 digital signature
 * - Extended APDU support for large avatar (up to 15KB)
 * - Card activation/deactivation
 * - Balance management
 * 
 * Requires JavaCard SDK 3.0.4+ (Extended APDU support)
 */
public class citizen_applet extends Applet implements ExtendedLength {

    // =====================================================
    // CONSTANTS
    // =====================================================

    // INS codes
    private static final byte INS_VERIFY = (byte) 0x00;
    private static final byte INS_CREATE = (byte) 0x01;
    private static final byte INS_GET = (byte) 0x02;
    private static final byte INS_UPDATE = (byte) 0x03;
    private static final byte INS_RESET_TRY_PIN = (byte) 0x10;
    private static final byte INS_CLEAR_CARD = (byte) 0x11;

    // P1 codes
    private static final byte P1_PIN = (byte) 0x04;
    private static final byte P1_CITIZEN_INFO = (byte) 0x05;
    private static final byte P1_SIGNATURE = (byte) 0x06;
    private static final byte P1_FORGET_PIN = (byte) 0x0A;
    private static final byte P1_ACTIVATE_CARD = (byte) 0x0B;
    private static final byte P1_DEACTIVATE_CARD = (byte) 0x0C;

    // P2 codes
    private static final byte P2_INFORMATION = (byte) 0x07;
    private static final byte P2_TRY_REMAINING = (byte) 0x08;
    private static final byte P2_AVATAR = (byte) 0x09;
    private static final byte P2_CARD_ID = (byte) 0x0A;
    private static final byte P2_PUBLIC_KEY = (byte) 0x0B;
    private static final byte P2_BALANCE = (byte) 0x0C;

    // PIN Configuration
    private static final byte PIN_LENGTH = 4;
    private static final byte MAX_PIN_TRIES = 5;

    // Data sizes
    private static final short MAX_INFO_LENGTH = 512;
    private static final short MAX_AVATAR_SIZE = (short) 15360; // 15KB with Extended APDU

    // =====================================================
    // STORAGE
    // =====================================================

    // Card state
    private byte[] pin; // 16 bytes MD5 hash
    private byte[] cardId; // 50 bytes for complex ID
    private short cardIdLength; // actual length of card ID
    private byte[] createDate; // 10 bytes
    private byte pinTryCounter;
    private boolean pinVerified;
    private boolean cardInitialized;
    private boolean cardActive;

    // Crypto components
    private AESKey aesKey;
    private Cipher aesCipher;
    private MessageDigest md5;

    // RSA components
    private RSAPrivateKey rsaPrivateKey;
    private RSAPublicKey rsaPublicKey;
    private Signature rsaSignature;
    private byte[] signatureBuffer;

    // Encrypted data storage
    private byte[] encryptedBalance; // 16 bytes
    private byte[] encryptedInfo; // MAX_INFO_LENGTH + 16 for padding
    private short encryptedInfoLength;

    // Avatar storage
    private byte[] avatar; // MAX_AVATAR_SIZE
    private byte[] avatarBuffer; // Temporary buffer for processing
    private short avatarSize;

    // Working buffer
    private byte[] tempBuffer;
    private byte[] tempBalance; // Temporary buffer for balance re-encryption

    // Extended APDU data length
    private short dataLen;

    // =====================================================
    // INSTALLATION
    // =====================================================

    public static void install(byte[] bArray, short bOffset, byte bLength) {
        new citizen_applet().register(bArray, (short) (bOffset + 1), bArray[bOffset]);
    }

    protected citizen_applet() {
        // Initialize storage arrays
        pin = new byte[16];
        cardId = new byte[50]; // Support complex ID format
        cardIdLength = 0;
        createDate = new byte[10];
        encryptedBalance = new byte[16];
        encryptedInfo = new byte[(short) (MAX_INFO_LENGTH + 16)];
        encryptedInfo = new byte[(short) (MAX_INFO_LENGTH + 16)];
        tempBuffer = new byte[528]; // Increased to support Info re-encryption (512 + padding)
        tempBalance = new byte[16];

        // Avatar storage (add 16 bytes for AES padding)
        avatar = new byte[(short) (MAX_AVATAR_SIZE + 16)];
        avatarBuffer = new byte[(short) (MAX_AVATAR_SIZE + 16)];

        // Crypto initialization
        md5 = MessageDigest.getInstance(MessageDigest.ALG_MD5, false);
        aesKey = (AESKey) KeyBuilder.buildKey(KeyBuilder.TYPE_AES, KeyBuilder.LENGTH_AES_128, false);
        aesCipher = Cipher.getInstance(Cipher.ALG_AES_BLOCK_128_ECB_NOPAD, false);

        // RSA initialization
        rsaSignature = Signature.getInstance(Signature.ALG_RSA_SHA_PKCS1, false);
        signatureBuffer = JCSystem.makeTransientByteArray((short) (KeyBuilder.LENGTH_RSA_1024 / 8),
                JCSystem.CLEAR_ON_RESET);

        // Generate RSA key pair
        KeyPair rsaKeyPair = new KeyPair(KeyPair.ALG_RSA, KeyBuilder.LENGTH_RSA_1024);
        rsaKeyPair.genKeyPair();
        rsaPrivateKey = (RSAPrivateKey) rsaKeyPair.getPrivate();
        rsaPublicKey = (RSAPublicKey) rsaKeyPair.getPublic();

        // Initialize state
        pinTryCounter = MAX_PIN_TRIES;
        pinVerified = false;
        cardInitialized = false;
        cardActive = true;
        encryptedInfoLength = 0;
        avatarSize = 0;
    }

    // =====================================================
    // MAIN PROCESS
    // =====================================================

    public void process(APDU apdu) {
        if (selectingApplet()) {
            return;
        }

        byte[] buffer = apdu.getBuffer();
        byte ins = buffer[ISO7816.OFFSET_INS];
        byte p1 = buffer[ISO7816.OFFSET_P1];
        byte p2 = buffer[ISO7816.OFFSET_P2];

        switch (ins) {
            case INS_VERIFY:
                processVerify(apdu, p1);
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
            case INS_CLEAR_CARD:
                clearCard(apdu);
                break;
            default:
                ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
        }
    }

    // =====================================================
    // VERIFY COMMANDS
    // =====================================================

    private void processVerify(APDU apdu, byte p1) {
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

        if (!cardActive) {
            ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
        }

        if (pinTryCounter == 0) {
            ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
        }

        byte[] buffer = apdu.getBuffer();
        short lc = apdu.setIncomingAndReceive();

        if (lc != PIN_LENGTH) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }

        // Hash input PIN with MD5
        md5.reset();
        md5.doFinal(buffer, ISO7816.OFFSET_CDATA, PIN_LENGTH, tempBuffer, (short) 0);

        // Compare with stored hash
        if (Util.arrayCompare(pin, (short) 0, tempBuffer, (short) 0, (short) 16) == 0) {
            pinVerified = true;
            pinTryCounter = MAX_PIN_TRIES;

            // Regenerate AES key from PIN hash
            aesKey.setKey(pin, (short) 0);

            buffer[0] = (byte) 0x01; // Success
            buffer[1] = pinTryCounter;
            apdu.setOutgoingAndSend((short) 0, (short) 2);
        } else {
            pinTryCounter--;
            pinVerified = false;
            buffer[0] = (byte) 0x00; // Failure
            buffer[1] = pinTryCounter;
            apdu.setOutgoingAndSend((short) 0, (short) 2);
        }
    }

    // =====================================================
    // CREATE COMMANDS
    // =====================================================

    private void processCreate(APDU apdu, byte p1, byte p2) {
        switch (p1) {
            case P1_PIN:
                initializeCard(apdu);
                break;
            case P1_SIGNATURE:
                createSignature(apdu);
                break;
            case P1_CITIZEN_INFO:
                if (p2 == P2_AVATAR) {
                    createAvatar(apdu);
                } else {
                    ISOException.throwIt(ISO7816.SW_WRONG_P1P2);
                }
                break;
            default:
                ISOException.throwIt(ISO7816.SW_WRONG_P1P2);
        }
    }

    private void initializeCard(APDU apdu) {
        if (cardInitialized) {
            ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
        }

        byte[] buffer = apdu.getBuffer();
        short lc = apdu.setIncomingAndReceive();

        // Expected format: [PIN:4][cardIdLength:1][cardId:N]
        // Minimum: 4 bytes PIN + 1 byte length
        if (lc < (short) (PIN_LENGTH + 1)) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }

        // Get Card ID length and data from APDU
        short idLen = (short) (buffer[(short) (ISO7816.OFFSET_CDATA + PIN_LENGTH)] & 0xFF);
        if (idLen > 50 || idLen < 1) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }

        // Copy Card ID from APDU data
        Util.arrayCopy(buffer, (short) (ISO7816.OFFSET_CDATA + PIN_LENGTH + 1), cardId, (short) 0, idLen);
        cardIdLength = idLen;

        // Hash PIN with MD5
        md5.reset();
        md5.doFinal(buffer, ISO7816.OFFSET_CDATA, PIN_LENGTH, pin, (short) 0);

        // Create AES key from PIN hash
        aesKey.setKey(pin, (short) 0);

        // Initialize balance to 0 (encrypted)
        Util.arrayFillNonAtomic(tempBuffer, (short) 0, (short) 16, (byte) 0x00);
        aesCipher.init(aesKey, Cipher.MODE_ENCRYPT);
        aesCipher.doFinal(tempBuffer, (short) 0, (short) 16, encryptedBalance, (short) 0);

        // Activate card
        cardInitialized = true;
        pinVerified = true;
        cardActive = true;
        pinTryCounter = MAX_PIN_TRIES;

        // Return Card ID and Public Key
        // First copy Card ID
        Util.arrayCopy(cardId, (short) 0, buffer, (short) 0, cardIdLength);

        // Then append serialized public key
        short pubKeyLen = serializePublicKey(buffer, cardIdLength);

        apdu.setOutgoingAndSend((short) 0, (short) (cardIdLength + pubKeyLen));
    }

    /**
     * Create digital signature for card authentication
     * APDU: 00 01 06 00 [Lc] [challenge data]
     * Returns: signature bytes
     */
    private void createSignature(APDU apdu) {
        if (!cardInitialized || !pinVerified) {
            ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
        }

        byte[] buffer = apdu.getBuffer();
        short lc = apdu.setIncomingAndReceive();

        if (lc == 0) {
            ISOException.throwIt(ISO7816.SW_DATA_INVALID);
        }

        // Sign the data with RSA private key
        rsaSignature.init(rsaPrivateKey, Signature.MODE_SIGN);
        short sigLen = rsaSignature.sign(buffer, ISO7816.OFFSET_CDATA, lc, signatureBuffer, (short) 0);

        // Return signature
        Util.arrayCopy(signatureBuffer, (short) 0, buffer, (short) 0, sigLen);
        apdu.setOutgoingAndSend((short) 0, sigLen);
    }

    /**
     * Create/Upload avatar with chunked transfer support
     * Format: [totalLen:2][offset:2][chunkData:N]
     * P2 bit 7 = 1 means more chunks coming, 0 means last chunk
     */
    private void createAvatar(APDU apdu) {
        if (!cardInitialized || !pinVerified) {
            ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
        }

        byte[] buf = apdu.getBuffer();
        byte p2 = buf[ISO7816.OFFSET_P2];
        boolean moreChunks = (p2 & 0x80) != 0;

        // Receive chunk data
        short received = apdu.setIncomingAndReceive();

        if (received < 4) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }

        // Parse header: totalLen(2) + offset(2)
        short dataOffset = ISO7816.OFFSET_CDATA;
        short totalLen = Util.getShort(buf, dataOffset);
        short chunkOffset = Util.getShort(buf, (short) (dataOffset + 2));
        short chunkLen = (short) (received - 4);

        // Validate
        if (totalLen > MAX_AVATAR_SIZE || chunkOffset + chunkLen > totalLen) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }

        // Copy chunk data to avatar buffer
        Util.arrayCopy(buf, (short) (dataOffset + 4), avatarBuffer, chunkOffset, chunkLen);

        // If last chunk, encrypt and store
        if (!moreChunks) {
            // Calculate padded length for AES (multiple of 16)
            short paddedLen = (short) (totalLen + (16 - (totalLen % 16)));

            // Pad with zeros
            Util.arrayFillNonAtomic(avatarBuffer, totalLen, (short) (paddedLen - totalLen), (byte) 0x00);

            // Encrypt avatar
            aesCipher.init(aesKey, Cipher.MODE_ENCRYPT);
            aesCipher.doFinal(avatarBuffer, (short) 0, paddedLen, avatar, (short) 0);
            avatarSize = paddedLen;

            // Clear buffer
            Util.arrayFillNonAtomic(avatarBuffer, (short) 0, paddedLen, (byte) 0x00);

            // Return encrypted size
            Util.setShort(buf, (short) 0, avatarSize);
            apdu.setOutgoingAndSend((short) 0, (short) 2);
        } else {
            // Acknowledge chunk received
            buf[0] = 0x01;
            apdu.setOutgoingAndSend((short) 0, (short) 1);
        }
    }

    // =====================================================
    // GET COMMANDS
    // =====================================================

    private void processGet(APDU apdu, byte p1, byte p2) {
        switch (p2) {
            case P2_CARD_ID:
                getCardId(apdu);
                break;
            case P2_PUBLIC_KEY:
                getPublicKey(apdu);
                break;
            case P2_BALANCE:
                getBalance(apdu);
                break;
            case P2_TRY_REMAINING:
                getTryRemaining(apdu);
                break;
            case P2_INFORMATION:
                if (p1 == P1_CITIZEN_INFO) {
                    getInfo(apdu);
                } else {
                    ISOException.throwIt(ISO7816.SW_WRONG_P1P2);
                }
                break;
            case P2_AVATAR:
                if (p1 == P1_CITIZEN_INFO) {
                    getAvatar(apdu);
                } else {
                    ISOException.throwIt(ISO7816.SW_WRONG_P1P2);
                }
                break;
            default:
                ISOException.throwIt(ISO7816.SW_WRONG_P1P2);
        }
    }

    private void getCardId(APDU apdu) {
        if (!cardInitialized) {
            ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
        }

        byte[] buffer = apdu.getBuffer();
        Util.arrayCopy(cardId, (short) 0, buffer, (short) 0, cardIdLength);
        apdu.setOutgoingAndSend((short) 0, cardIdLength);
    }

    private void getPublicKey(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        short length = serializePublicKey(buffer, (short) 0);
        apdu.setOutgoingAndSend((short) 0, length);
    }

    private void getBalance(APDU apdu) {
        if (!cardInitialized || !pinVerified) {
            ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
        }

        byte[] buffer = apdu.getBuffer();
        aesCipher.init(aesKey, Cipher.MODE_DECRYPT);
        aesCipher.doFinal(encryptedBalance, (short) 0, (short) 16, buffer, (short) 0);
        apdu.setOutgoingAndSend((short) 0, (short) 4);
    }

    private void getTryRemaining(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        buffer[0] = pinTryCounter;
        apdu.setOutgoingAndSend((short) 0, (short) 1);
    }

    private void getInfo(APDU apdu) {
        if (!cardInitialized || !pinVerified) {
            ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
        }

        if (encryptedInfoLength == 0) {
            ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
        }

        byte[] buffer = apdu.getBuffer();
        aesCipher.init(aesKey, Cipher.MODE_DECRYPT);
        short len = aesCipher.doFinal(encryptedInfo, (short) 0, encryptedInfoLength, buffer, (short) 0);

        // Remove padding
        short actualLen = removePadding(buffer, len);
        apdu.setOutgoingAndSend((short) 0, actualLen);
    }

    /**
     * Get avatar with Extended APDU response
     */
    private void getAvatar(APDU apdu) {
        if (!cardInitialized || !pinVerified) {
            ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
        }

        if (avatarSize == 0) {
            ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
        }

        // Decrypt avatar
        aesCipher.init(aesKey, Cipher.MODE_DECRYPT);
        aesCipher.doFinal(avatar, (short) 0, avatarSize, avatarBuffer, (short) 0);

        // Get actual length (remove padding)
        short actualLen = getArrayLen(avatarBuffer, avatarSize);

        // Send using extended APDU
        short maxLen = apdu.setOutgoing();
        apdu.setOutgoingLength(actualLen);

        short pointer = 0;
        short remaining = actualLen;

        while (remaining > 0) {
            short chunkLen = (remaining < maxLen) ? remaining : maxLen;
            apdu.sendBytesLong(avatarBuffer, pointer, chunkLen);
            pointer += chunkLen;
            remaining -= chunkLen;
        }
    }

    // =====================================================
    // UPDATE COMMANDS
    // =====================================================

    private void processUpdate(APDU apdu, byte p1, byte p2) {
        switch (p1) {
            case P1_PIN:
                updatePin(apdu);
                break;
            case P1_CITIZEN_INFO:
                if (p2 == P2_INFORMATION) {
                    updateInfo(apdu);
                } else if (p2 == P2_BALANCE) {
                    updateBalance(apdu);
                } else if (p2 == P2_AVATAR) {
                    createAvatar(apdu); // Same as create
                } else {
                    ISOException.throwIt(ISO7816.SW_WRONG_P1P2);
                }
                break;
            case P1_FORGET_PIN:
                forgetPin(apdu);
                break;
            case P1_ACTIVATE_CARD:
                activateCard(apdu);
                break;
            case P1_DEACTIVATE_CARD:
                deactivateCard(apdu);
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

        if (lc != (short) (PIN_LENGTH * 2)) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }

        // Verify old PIN
        md5.reset();
        md5.doFinal(buffer, ISO7816.OFFSET_CDATA, PIN_LENGTH, tempBuffer, (short) 0);

        if (Util.arrayCompare(pin, (short) 0, tempBuffer, (short) 0, (short) 16) != 0) {
            ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
        }

        // --- RE-ENCRYPTION START ---

        // 1. Decrypt Balance (using OLD key)
        aesCipher.init(aesKey, Cipher.MODE_DECRYPT);
        aesCipher.doFinal(encryptedBalance, (short) 0, (short) 16, tempBalance, (short) 0);

        // 2. Decrypt Info (using OLD key) if exists
        if (encryptedInfoLength > 0) {
            aesCipher.init(aesKey, Cipher.MODE_DECRYPT);
            aesCipher.doFinal(encryptedInfo, (short) 0, encryptedInfoLength, tempBuffer, (short) 0);
        }

        // 3. Hash new PIN and update AES Key
        md5.reset();
        md5.doFinal(buffer, (short) (ISO7816.OFFSET_CDATA + PIN_LENGTH), PIN_LENGTH, pin, (short) 0);

        // Update AES key with new PIN
        aesKey.setKey(pin, (short) 0);
        pinTryCounter = MAX_PIN_TRIES; // Reset try counter only after success

        // 4. Re-encrypt Balance (using NEW key)
        aesCipher.init(aesKey, Cipher.MODE_ENCRYPT);
        aesCipher.doFinal(tempBalance, (short) 0, (short) 16, encryptedBalance, (short) 0);

        // 5. Re-encrypt Info (using NEW key) if exists
        if (encryptedInfoLength > 0) {
            aesCipher.init(aesKey, Cipher.MODE_ENCRYPT);
            aesCipher.doFinal(tempBuffer, (short) 0, encryptedInfoLength, encryptedInfo, (short) 0);
        }

        // --- RE-ENCRYPTION END ---

        buffer[0] = (byte) 0x01;
        apdu.setOutgoingAndSend((short) 0, (short) 1);
    }

    private void updateInfo(APDU apdu) {
        if (!cardInitialized || !pinVerified) {
            ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
        }

        byte[] buffer = apdu.getBuffer();
        short lc = apdu.setIncomingAndReceive();

        if (lc == 0 || lc > MAX_INFO_LENGTH) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }

        // Apply padding
        short paddedLen = (short) (lc + (16 - (lc % 16)));
        Util.arrayFillNonAtomic(buffer, (short) (ISO7816.OFFSET_CDATA + lc), (short) (paddedLen - lc), (byte) 0x00);

        // Encrypt
        aesCipher.init(aesKey, Cipher.MODE_ENCRYPT);
        aesCipher.doFinal(buffer, ISO7816.OFFSET_CDATA, paddedLen, encryptedInfo, (short) 0);
        encryptedInfoLength = paddedLen;

        buffer[0] = (byte) 0x01;
        apdu.setOutgoingAndSend((short) 0, (short) 1);
    }

    private void updateBalance(APDU apdu) {
        if (!cardInitialized || !pinVerified) {
            ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
        }

        byte[] buffer = apdu.getBuffer();
        short lc = apdu.setIncomingAndReceive();

        if (lc < 5) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }

        byte type = buffer[ISO7816.OFFSET_CDATA];
        int amount = getInt(buffer, (short) (ISO7816.OFFSET_CDATA + 1));

        // Decrypt current balance
        aesCipher.init(aesKey, Cipher.MODE_DECRYPT);
        aesCipher.doFinal(encryptedBalance, (short) 0, (short) 16, tempBuffer, (short) 0);
        int currentBalance = getInt(tempBuffer, (short) 0);

        // Calculate new balance
        int newBalance;
        if (type == 0x01) { // Topup
            newBalance = currentBalance + amount;
        } else if (type == 0x02) { // Payment
            if (currentBalance < amount) {
                ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
            }
            newBalance = currentBalance - amount;
        } else {
            ISOException.throwIt(ISO7816.SW_WRONG_DATA);
            return;
        }

        // Encrypt new balance
        putInt(tempBuffer, (short) 0, newBalance);
        Util.arrayFillNonAtomic(tempBuffer, (short) 4, (short) 12, (byte) 0x00);
        aesCipher.init(aesKey, Cipher.MODE_ENCRYPT);
        aesCipher.doFinal(tempBuffer, (short) 0, (short) 16, encryptedBalance, (short) 0);

        // Return new balance
        putInt(buffer, (short) 0, newBalance);
        apdu.setOutgoingAndSend((short) 0, (short) 4);
    }

    /**
     * Admin function: Reset PIN without knowing old PIN
     */
    private void forgetPin(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        short lc = apdu.setIncomingAndReceive();

        if (lc != PIN_LENGTH) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }

        // Hash new PIN
        md5.reset();
        md5.doFinal(buffer, ISO7816.OFFSET_CDATA, PIN_LENGTH, pin, (short) 0);

        // Update AES key
        aesKey.setKey(pin, (short) 0);
        pinTryCounter = MAX_PIN_TRIES;
        cardActive = true;

        buffer[0] = (byte) 0x01;
        apdu.setOutgoingAndSend((short) 0, (short) 1);
    }

    /**
     * Activate card - restore PIN tries
     */
    private void activateCard(APDU apdu) {
        pinTryCounter = MAX_PIN_TRIES;
        cardActive = true;

        byte[] buffer = apdu.getBuffer();
        buffer[0] = pinTryCounter;
        apdu.setOutgoingAndSend((short) 0, (short) 1);
    }

    /**
     * Deactivate card - block all operations
     */
    private void deactivateCard(APDU apdu) {
        pinTryCounter = 0;
        cardActive = false;
        pinVerified = false;

        byte[] buffer = apdu.getBuffer();
        buffer[0] = (byte) 0x00;
        apdu.setOutgoingAndSend((short) 0, (short) 1);
    }

    // =====================================================
    // ADMIN COMMANDS
    // =====================================================

    private void clearCard(APDU apdu) {
        cardInitialized = false;
        pinVerified = false;
        cardActive = true;
        pinTryCounter = MAX_PIN_TRIES;
        encryptedInfoLength = 0;
        avatarSize = 0;

        Util.arrayFillNonAtomic(pin, (short) 0, (short) 16, (byte) 0x00);
        Util.arrayFillNonAtomic(cardId, (short) 0, (short) 12, (byte) 0x00);
        Util.arrayFillNonAtomic(encryptedBalance, (short) 0, (short) 16, (byte) 0x00);
        Util.arrayFillNonAtomic(encryptedInfo, (short) 0, encryptedInfoLength, (byte) 0x00);
        Util.arrayFillNonAtomic(avatar, (short) 0, avatarSize, (byte) 0x00);

        byte[] buffer = apdu.getBuffer();
        buffer[0] = (byte) 0x01;
        apdu.setOutgoingAndSend((short) 0, (short) 1);
    }

    private void resetPinTries(APDU apdu) {
        pinTryCounter = MAX_PIN_TRIES;
        cardActive = true;

        byte[] buffer = apdu.getBuffer();
        buffer[0] = pinTryCounter;
        apdu.setOutgoingAndSend((short) 0, (short) 1);
    }

    // =====================================================
    // HELPER METHODS
    // =====================================================

    /**
     * Process APDU for data transfer
     * Auto-detects Standard vs Extended APDU and uses correct offset
     */
    private short receiveData(APDU apdu, byte[] destBuffer, short destOffset) {
        byte[] buf = apdu.getBuffer();
        short bytesRead = apdu.setIncomingAndReceive();
        short totalRead = 0;

        // Determine offset based on APDU type
        // For Extended APDU: LC at offset 4 is 0x00, followed by 2-byte length
        // For Standard APDU: LC at offset 4 is the actual length
        short dataOffset;
        if (buf[ISO7816.OFFSET_LC] == 0x00 && bytesRead > 0) {
            // Extended APDU: data starts at offset 7
            dataOffset = ISO7816.OFFSET_EXT_CDATA;
        } else {
            // Standard APDU: data starts at offset 5
            dataOffset = ISO7816.OFFSET_CDATA;
        }

        // Copy first chunk
        Util.arrayCopy(buf, dataOffset, destBuffer, destOffset, bytesRead);
        totalRead += bytesRead;
        destOffset += bytesRead;

        // Continue receiving if more data available
        while (apdu.getCurrentState() == APDU.STATE_PARTIAL_INCOMING) {
            bytesRead = apdu.receiveBytes((short) 0);
            Util.arrayCopy(buf, (short) 0, destBuffer, destOffset, bytesRead);
            totalRead += bytesRead;
            destOffset += bytesRead;
        }

        dataLen = totalRead;
        return totalRead;
    }

    /**
     * Serialize RSA public key
     * Format: [expLen:2][exp:3][modLen:2][mod:128]
     */
    private short serializePublicKey(byte[] buffer, short offset) {
        // Get exponent
        short expLen = rsaPublicKey.getExponent(tempBuffer, (short) 0);
        Util.setShort(buffer, offset, expLen);
        Util.arrayCopy(tempBuffer, (short) 0, buffer, (short) (offset + 2), expLen);

        // Get modulus
        short modLen = rsaPublicKey.getModulus(tempBuffer, (short) 0);
        Util.setShort(buffer, (short) (offset + 2 + expLen), modLen);
        Util.arrayCopy(tempBuffer, (short) 0, buffer, (short) (offset + 4 + expLen), modLen);

        return (short) (4 + expLen + modLen);
    }

    private int getInt(byte[] buffer, short offset) {
        return ((buffer[offset] & 0xFF) << 24) |
                ((buffer[(short) (offset + 1)] & 0xFF) << 16) |
                ((buffer[(short) (offset + 2)] & 0xFF) << 8) |
                (buffer[(short) (offset + 3)] & 0xFF);
    }

    private void putInt(byte[] buffer, short offset, int value) {
        buffer[offset] = (byte) (value >> 24);
        buffer[(short) (offset + 1)] = (byte) (value >> 16);
        buffer[(short) (offset + 2)] = (byte) (value >> 8);
        buffer[(short) (offset + 3)] = (byte) (value & 0xFF);
    }

    private short removePadding(byte[] output, short length) {
        short paddingLen = 0;
        for (short i = (short) (length - 1); i >= 0; i--) {
            if (output[i] != (byte) 0x00) {
                break;
            }
            paddingLen++;
        }
        return (short) (length - paddingLen);
    }

    private short getArrayLen(byte[] data, short maxLen) {
        short count = 0;
        for (short i = (short) (maxLen - 1); i >= 0; i--) {
            if (data[i] != 0x00) {
                break;
            }
            count++;
        }
        return (short) (maxLen - count);
    }
}
