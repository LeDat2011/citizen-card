package citizen_applet;

import javacard.framework.*;
import javacard.security.*;
import javacardx.crypto.*;
import javacardx.apdu.ExtendedLength;

/**
 * FULL CITIZEN CARD APPLET v3.0
 * 
 * Features:
 * - PIN Key + Master Key architecture for enhanced security
 * - PIN Key derived from PBKDF2(PIN, salt) ON APPLET
 * - Master Key (random AES-128) encrypts all user data
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
    private static final byte INS_GET_AVATAR_CHUNK = (byte) 0x04;
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

    // PBKDF2 Configuration
    private static final short PBKDF2_ITERATIONS = 1000; // Reduced for JavaCard performance
    private static final short PBKDF2_KEY_LENGTH = 16; // 128-bit AES key
    private static final short SHA1_BLOCK_SIZE = 64;
    private static final short SHA1_HASH_SIZE = 20;

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

    // Crypto components - New Master Key Architecture
    private AESKey pinKey; // Key sinh từ PBKDF2(PIN), dùng wrap/unwrap Master Key
    private AESKey masterKey; // Key ngẫu nhiên, dùng mã hóa dữ liệu thực tế
    private byte[] encryptedMasterKey; // Master Key đã được mã hóa bằng PIN Key (16 bytes)
    private Cipher aesCipher;
    private MessageDigest md5;
    private MessageDigest sha1; // For PBKDF2-HMAC-SHA1
    private RandomData randomData; // Dùng sinh Master Key ngẫu nhiên

    // PBKDF2 working buffers
    private byte[] hmacKey; // HMAC key buffer (64 bytes for SHA1 block)
    private byte[] hmacBuffer; // HMAC intermediate buffer
    private byte[] pbkdf2Buffer; // PBKDF2 output buffer

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
        tempBuffer = new byte[528]; // Increased to support Info re-encryption (512 + padding)
        tempBalance = new byte[16];

        // Master Key storage (encrypted by PIN Key)
        encryptedMasterKey = new byte[16];

        // PBKDF2 working buffers
        hmacKey = new byte[SHA1_BLOCK_SIZE]; // 64 bytes
        hmacBuffer = new byte[(short) (SHA1_BLOCK_SIZE + SHA1_HASH_SIZE)]; // 84 bytes
        pbkdf2Buffer = new byte[SHA1_HASH_SIZE]; // 20 bytes

        // Avatar storage (add 16 bytes for AES padding)
        avatar = new byte[(short) (MAX_AVATAR_SIZE + 16)];
        avatarBuffer = new byte[(short) (MAX_AVATAR_SIZE + 16)];

        // Crypto initialization - New Master Key Architecture
        md5 = MessageDigest.getInstance(MessageDigest.ALG_MD5, false);
        sha1 = MessageDigest.getInstance(MessageDigest.ALG_SHA, false);
        pinKey = (AESKey) KeyBuilder.buildKey(KeyBuilder.TYPE_AES, KeyBuilder.LENGTH_AES_128, false);
        masterKey = (AESKey) KeyBuilder.buildKey(KeyBuilder.TYPE_AES, KeyBuilder.LENGTH_AES_128, false);
        aesCipher = Cipher.getInstance(Cipher.ALG_AES_BLOCK_128_ECB_NOPAD, false);
        randomData = RandomData.getInstance(RandomData.ALG_SECURE_RANDOM);

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
            case INS_GET_AVATAR_CHUNK:
                getAvatarChunk(apdu);
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

        // FORMAT v3.0: Receive PIN (4 bytes), PBKDF2 done ON APPLET
        if (lc != PIN_LENGTH) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }

        // Derive PIN Key from received PIN using PBKDF2
        derivePinKey(buffer, ISO7816.OFFSET_CDATA, PIN_LENGTH, tempBuffer, (short) 0);

        // Compare derived PIN Key with stored PIN Key
        if (Util.arrayCompare(pin, (short) 0, tempBuffer, (short) 0, (short) 16) == 0) {
            pinVerified = true;
            pinTryCounter = MAX_PIN_TRIES;

            // Set PIN Key
            pinKey.setKey(tempBuffer, (short) 0);

            // === Decrypt Master Key using PIN Key ===
            aesCipher.init(pinKey, Cipher.MODE_DECRYPT);
            aesCipher.doFinal(encryptedMasterKey, (short) 0, (short) 16, tempBuffer, (short) 0);
            masterKey.setKey(tempBuffer, (short) 0);

            // Clear temp buffer for security
            Util.arrayFillNonAtomic(tempBuffer, (short) 0, (short) 16, (byte) 0x00);

            buffer[0] = (byte) 0x01; // Success
            buffer[1] = pinTryCounter;
            apdu.setOutgoingAndSend((short) 0, (short) 2);
        } else {
            // Clear temp buffer for security
            Util.arrayFillNonAtomic(tempBuffer, (short) 0, (short) 16, (byte) 0x00);

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
                // Check P2 with mask 0x7F (ignore bit 7 used for chunk flag)
                if ((p2 & 0x7F) == P2_AVATAR) {
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

        // FORMAT v3.0: [PIN:4][cardIdLength:1][cardId:N]
        // PIN is 4 bytes, PBKDF2 is done ON APPLET
        if (lc < (short) (PIN_LENGTH + 1)) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }

        // Get Card ID length and data from APDU (MUST do this FIRST for PBKDF2 salt)
        short idLen = (short) (buffer[(short) (ISO7816.OFFSET_CDATA + PIN_LENGTH)] & 0xFF);
        if (idLen > 50 || idLen < 1) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }

        // Copy Card ID from APDU data
        Util.arrayCopy(buffer, (short) (ISO7816.OFFSET_CDATA + PIN_LENGTH + 1), cardId, (short) 0, idLen);
        cardIdLength = idLen;

        // === PBKDF2 ON APPLET: Derive PIN Key from PIN ===
        // Uses cardId as salt for uniqueness per card
        derivePinKey(buffer, ISO7816.OFFSET_CDATA, PIN_LENGTH, pin, (short) 0);

        // Set PIN Key from PBKDF2-derived key
        pinKey.setKey(pin, (short) 0);

        // === Generate random Master Key ===
        randomData.generateData(tempBuffer, (short) 0, (short) 16);
        masterKey.setKey(tempBuffer, (short) 0);

        // Encrypt Master Key with PIN Key and store
        aesCipher.init(pinKey, Cipher.MODE_ENCRYPT);
        aesCipher.doFinal(tempBuffer, (short) 0, (short) 16, encryptedMasterKey, (short) 0);

        // Clear temp buffer for security
        Util.arrayFillNonAtomic(tempBuffer, (short) 0, (short) 16, (byte) 0x00);

        // Initialize balance to 0 (encrypted with Master Key)
        Util.arrayFillNonAtomic(tempBuffer, (short) 0, (short) 16, (byte) 0x00);
        aesCipher.init(masterKey, Cipher.MODE_ENCRYPT);
        aesCipher.doFinal(tempBuffer, (short) 0, (short) 16, encryptedBalance, (short) 0);

        // Activate card
        cardInitialized = true;
        pinVerified = true;
        cardActive = true;
        pinTryCounter = MAX_PIN_TRIES;

        // Return Card ID and Public Key
        Util.arrayCopy(cardId, (short) 0, buffer, (short) 0, cardIdLength);
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

            // Encrypt avatar with Master Key (v3.0)
            aesCipher.init(masterKey, Cipher.MODE_ENCRYPT);
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
        // Use Master Key for decryption (v3.0)
        aesCipher.init(masterKey, Cipher.MODE_DECRYPT);
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
        // Use Master Key for decryption (v3.0)
        aesCipher.init(masterKey, Cipher.MODE_DECRYPT);
        short len = aesCipher.doFinal(encryptedInfo, (short) 0, encryptedInfoLength, buffer, (short) 0);

        // Remove padding
        short actualLen = removePadding(buffer, len);
        apdu.setOutgoingAndSend((short) 0, actualLen);
    }

    /**
     * Get avatar with Multi-Command Chunked Download support
     * 
     * Mode 1 (Legacy): P1 = 0x00 - Uses sendBytesLong (single command)
     * Mode 2 (Chunked): P1 = 0x01 - Uses P1/P2 as offset for multi-command download
     * - P1 high byte = offset >> 8
     * - P2 low byte = offset & 0xFF
     * - Returns: [totalLen:2][chunkLen:2][chunkData:N]
     */
    /**
     * Get avatar (Legacy Single Command)
     */
    private void getAvatar(APDU apdu) {
        if (!cardInitialized || !pinVerified) {
            ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
        }

        if (avatarSize == 0) {
            ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
        }

        // Decrypt avatar with Master Key (v3.0)
        aesCipher.init(masterKey, Cipher.MODE_DECRYPT);
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

    /**
     * Get avatar chunk (New Multi-Command Protocol)
     * P1/P2 encode offset: (P1 << 8) | P2
     */
    private void getAvatarChunk(APDU apdu) {
        if (!cardInitialized || !pinVerified) {
            ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
        }

        if (avatarSize == 0) {
            ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
        }

        byte[] buf = apdu.getBuffer();
        byte p1 = buf[ISO7816.OFFSET_P1];
        byte p2 = buf[ISO7816.OFFSET_P2];

        // Decrypt avatar with Master Key (v3.0)
        aesCipher.init(masterKey, Cipher.MODE_DECRYPT);
        aesCipher.doFinal(avatar, (short) 0, avatarSize, avatarBuffer, (short) 0);

        // Get actual length
        short actualLen = getArrayLen(avatarBuffer, avatarSize);

        // Calculate offset directly from P1/P2
        short offset = (short) (((p1 & 0xFF) << 8) | (p2 & 0xFF));

        // Validate offset
        if (offset >= actualLen) {
            // Return empty response to indicate end
            buf[0] = (byte) ((actualLen >> 8) & 0xFF);
            buf[1] = (byte) (actualLen & 0xFF);
            buf[2] = 0;
            buf[3] = 0;
            apdu.setOutgoingAndSend((short) 0, (short) 4);
            return;
        }

        // Calculate chunk size (max 200 bytes per chunk)
        short remaining = (short) (actualLen - offset);
        short chunkLen = (remaining > 200) ? 200 : remaining;

        // Build response: [totalLen:2][chunkLen:2][data:N]
        buf[0] = (byte) ((actualLen >> 8) & 0xFF);
        buf[1] = (byte) (actualLen & 0xFF);
        buf[2] = (byte) ((chunkLen >> 8) & 0xFF);
        buf[3] = (byte) (chunkLen & 0xFF);

        // Copy chunk data
        Util.arrayCopy(avatarBuffer, offset, buf, (short) 4, chunkLen);

        apdu.setOutgoingAndSend((short) 0, (short) (4 + chunkLen));
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

        // FORMAT v3.0: [OLD_PIN:4][NEW_PIN:4]
        // PBKDF2 is done ON APPLET
        if (lc != (short) (PIN_LENGTH * 2)) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }

        // Derive old PIN Key and verify
        derivePinKey(buffer, ISO7816.OFFSET_CDATA, PIN_LENGTH, tempBuffer, (short) 0);
        if (Util.arrayCompare(pin, (short) 0, tempBuffer, (short) 0, (short) 16) != 0) {
            Util.arrayFillNonAtomic(tempBuffer, (short) 0, (short) 16, (byte) 0x00);
            ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
        }

        // === Only re-encrypt Master Key, NOT all data! ===

        // 1. Decrypt Master Key with OLD PIN Key
        aesCipher.init(pinKey, Cipher.MODE_DECRYPT);
        aesCipher.doFinal(encryptedMasterKey, (short) 0, (short) 16, tempBalance, (short) 0);

        // 2. Derive new PIN Key from new PIN
        derivePinKey(buffer, (short) (ISO7816.OFFSET_CDATA + PIN_LENGTH), PIN_LENGTH, pin, (short) 0);

        // 3. Set new PIN Key
        pinKey.setKey(pin, (short) 0);

        // 4. Re-encrypt Master Key with NEW PIN Key
        aesCipher.init(pinKey, Cipher.MODE_ENCRYPT);
        aesCipher.doFinal(tempBalance, (short) 0, (short) 16, encryptedMasterKey, (short) 0);

        // 5. Clear temp buffers for security
        Util.arrayFillNonAtomic(tempBuffer, (short) 0, (short) 16, (byte) 0x00);
        Util.arrayFillNonAtomic(tempBalance, (short) 0, (short) 16, (byte) 0x00);

        // Reset PIN try counter
        pinTryCounter = MAX_PIN_TRIES;

        // === NO need to re-encrypt Balance, Info, Avatar! ===
        // They are encrypted with Master Key, which remains the same!

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

        // Encrypt with Master Key (v3.0)
        aesCipher.init(masterKey, Cipher.MODE_ENCRYPT);
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

        // Decrypt current balance with Master Key (v3.0)
        aesCipher.init(masterKey, Cipher.MODE_DECRYPT);
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

        // Encrypt new balance with Master Key (v3.0)
        putInt(tempBuffer, (short) 0, newBalance);
        Util.arrayFillNonAtomic(tempBuffer, (short) 4, (short) 12, (byte) 0x00);
        aesCipher.init(masterKey, Cipher.MODE_ENCRYPT);
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

    // =====================================================
    // PBKDF2 IMPLEMENTATION (ON-CARD)
    // =====================================================

    /**
     * Derive PIN Key from PIN using PBKDF2-HMAC-SHA1
     * Uses cardId as salt for uniqueness per card
     * 
     * @param pinData   PIN bytes (4 bytes)
     * @param pinOffset Offset in pinData
     * @param pinLen    Length of PIN
     * @param output    Output buffer for derived key (16 bytes)
     * @param outOffset Offset in output buffer
     */
    private void derivePinKey(byte[] pinData, short pinOffset, short pinLen,
            byte[] output, short outOffset) {
        // Use cardId as salt (ensures unique keys per card)
        pbkdf2(pinData, pinOffset, pinLen,
                cardId, (short) 0, cardIdLength,
                PBKDF2_ITERATIONS,
                output, outOffset, PBKDF2_KEY_LENGTH);
    }

    /**
     * PBKDF2-HMAC-SHA1 implementation for JavaCard
     * 
     * @param password   Password bytes
     * @param passOff    Password offset
     * @param passLen    Password length
     * @param salt       Salt bytes
     * @param saltOff    Salt offset
     * @param saltLen    Salt length
     * @param iterations Number of iterations
     * @param output     Output buffer
     * @param outOff     Output offset
     * @param dkLen      Derived key length (must be <= 20 for single block)
     */
    private void pbkdf2(byte[] password, short passOff, short passLen,
            byte[] salt, short saltOff, short saltLen,
            short iterations,
            byte[] output, short outOff, short dkLen) {

        // For 16-byte key, we only need one block (SHA1 produces 20 bytes)
        // Block index = 1 (big-endian 4 bytes)

        // First iteration: U1 = HMAC(password, salt || INT(1))
        // Prepare salt || INT(1) in hmacBuffer
        Util.arrayCopy(salt, saltOff, hmacBuffer, (short) 0, saltLen);
        hmacBuffer[(short) (saltLen)] = 0x00;
        hmacBuffer[(short) (saltLen + 1)] = 0x00;
        hmacBuffer[(short) (saltLen + 2)] = 0x00;
        hmacBuffer[(short) (saltLen + 3)] = 0x01;

        // U1 = HMAC-SHA1(password, salt || 0x00000001)
        hmacSha1(password, passOff, passLen,
                hmacBuffer, (short) 0, (short) (saltLen + 4),
                pbkdf2Buffer, (short) 0);

        // Copy U1 to output (this will be XORed with subsequent U values)
        Util.arrayCopy(pbkdf2Buffer, (short) 0, output, outOff, dkLen);

        // Subsequent iterations: Ui = HMAC(password, U(i-1)), output ^= Ui
        for (short i = 1; i < iterations; i++) {
            // Ui = HMAC-SHA1(password, U(i-1))
            hmacSha1(password, passOff, passLen,
                    pbkdf2Buffer, (short) 0, SHA1_HASH_SIZE,
                    pbkdf2Buffer, (short) 0);

            // XOR with output
            for (short j = 0; j < dkLen; j++) {
                output[(short) (outOff + j)] ^= pbkdf2Buffer[j];
            }
        }
    }

    /**
     * HMAC-SHA1 implementation
     * HMAC(K, m) = H((K' XOR opad) || H((K' XOR ipad) || m))
     * 
     * @param key     HMAC key
     * @param keyOff  Key offset
     * @param keyLen  Key length
     * @param message Message to authenticate
     * @param msgOff  Message offset
     * @param msgLen  Message length
     * @param output  Output buffer (20 bytes)
     * @param outOff  Output offset
     */
    private void hmacSha1(byte[] key, short keyOff, short keyLen,
            byte[] message, short msgOff, short msgLen,
            byte[] output, short outOff) {

        // Step 1: Prepare K' (key padded/hashed to block size)
        if (keyLen > SHA1_BLOCK_SIZE) {
            // If key > block size, hash it first
            sha1.reset();
            sha1.doFinal(key, keyOff, keyLen, hmacKey, (short) 0);
            Util.arrayFillNonAtomic(hmacKey, SHA1_HASH_SIZE,
                    (short) (SHA1_BLOCK_SIZE - SHA1_HASH_SIZE), (byte) 0x00);
        } else {
            // Pad key with zeros
            Util.arrayCopy(key, keyOff, hmacKey, (short) 0, keyLen);
            Util.arrayFillNonAtomic(hmacKey, keyLen,
                    (short) (SHA1_BLOCK_SIZE - keyLen), (byte) 0x00);
        }

        // Step 2: Compute inner hash: H((K' XOR ipad) || message)
        // ipad = 0x36 repeated
        sha1.reset();
        for (short i = 0; i < SHA1_BLOCK_SIZE; i++) {
            hmacBuffer[i] = (byte) (hmacKey[i] ^ 0x36);
        }
        sha1.update(hmacBuffer, (short) 0, SHA1_BLOCK_SIZE);
        sha1.doFinal(message, msgOff, msgLen, hmacBuffer, (short) 0);

        // Step 3: Compute outer hash: H((K' XOR opad) || inner_hash)
        // opad = 0x5C repeated
        sha1.reset();
        for (short i = 0; i < SHA1_BLOCK_SIZE; i++) {
            hmacBuffer[(short) (SHA1_HASH_SIZE + i)] = (byte) (hmacKey[i] ^ 0x5C);
        }
        sha1.update(hmacBuffer, SHA1_HASH_SIZE, SHA1_BLOCK_SIZE);
        sha1.doFinal(hmacBuffer, (short) 0, SHA1_HASH_SIZE, output, outOff);
    }
}
