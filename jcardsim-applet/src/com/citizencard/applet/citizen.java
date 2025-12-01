package citizen;

import javacard.framework.*;
import javacard.security.*;

/**
 * Citizen Card Applet - JavaCard 2.2.2
 * 
 * Quản lý thẻ thông minh dân với các chức năng:
 * - Xác thực PIN
 * - Nạp/Trừ tiền
 * - Khóa/Mở khóa thẻ
 * - Quản lý thông tin dân
 * - Quản lý ảnh đại diện
 */
public class citizen extends Applet {
    
    // AID của applet
    private static final byte[] CITIZEN_CARD_AID = {
        (byte) 0x11, (byte) 0x22, (byte) 0x33, (byte) 0x44, (byte) 0x55, (byte) 0x00
    };
    
    // INS Commands (theo CardService.java)
    private static final byte INS_SELECT = (byte) 0xA4;
    private static final byte INS_CHECK_CARD_CREATED = (byte) 0x29;
    private static final byte INS_CLEAR_CARD = (byte) 0x18;
    private static final byte INS_UPDATE_CUSTOMER_INFO = (byte) 0x20;
    private static final byte INS_GET_CUSTOMER_INFO = (byte) 0x13;
    private static final byte INS_GET_BALANCE = (byte) 0x14;
    private static final byte INS_UPDATE_BALANCE = (byte) 0x16;
    private static final byte INS_UPDATE_CARD_ID = (byte) 0x26;
    private static final byte INS_GET_CARD_ID = (byte) 0x27;
    private static final byte INS_UPDATE_PIN = (byte) 0x21;
    private static final byte INS_UPDATE_PICTURE = (byte) 0x22;
    private static final byte INS_GET_PICTURE = (byte) 0x23;
    
    // CLA
    private static final byte CLA = (byte) 0x00;
    
    // Status Words
    private static final short SW_SUCCESS = (short) 0x9000;
    private static final short SW_CARD_NOT_INITIALIZED = (short) 0x6300;
    private static final short SW_INSUFFICIENT_BALANCE = (short) 0x6301;
    private static final short SW_INVALID_PIN = (short) 0x6302;
    private static final short SW_CARD_LOCKED = (byte) 0x6400;
    private static final short SW_WRONG_LENGTH = (short) 0x6700;
    private static final short SW_WRONG_DATA = (short) 0x6A80;
    
    // Card State
    private boolean initialized;
    private byte[] cardId;
    private byte[] customerInfo;
    private byte[] balance; // 4 bytes (int)
    private byte[] pin;
    private byte[] picture;
    
    // Maximum sizes
    private static final short MAX_CARD_ID_LEN = 50;
    private static final short MAX_CUSTOMER_INFO_LEN = 200;
    private static final short MAX_PIN_LEN = 20;
    private static final short MAX_PICTURE_LEN = 5000; // ~5KB
    
    // Picture length tracker
    private short pictureLength;
    
    private OwnerPIN pinObject;
    
    /**
     * Constructor
     */
    private citizen() {
        initialized = false;
        cardId = new byte[MAX_CARD_ID_LEN];
        customerInfo = new byte[MAX_CUSTOMER_INFO_LEN];
        balance = new byte[4];
        pin = new byte[MAX_PIN_LEN];
        picture = new byte[MAX_PICTURE_LEN];
        pictureLength = 0;
        
        // Initialize PIN object (max 3 tries, 4 digits)
        pinObject = new OwnerPIN((byte) 3, (byte) 4);
        
        // Initialize balance to 0
        Util.setShort(balance, (short) 0, (short) 0);
    }
    
    /**
     * Install applet
     */
    public static void install(byte[] bArray, short bOffset, byte bLength) {
        new citizen().register(bArray, (short) (bOffset + 1), bArray[bOffset]);
    }
    
    /**
     * Process APDU command
     */
    public void process(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        
        // SELECT APPLET (0xA4) - JCRE handles this, but we can respond
        if (selectingApplet()) {
            ISOException.throwIt(SW_SUCCESS);
            return;
        }
        
        // Check CLA
        if (buffer[ISO7816.OFFSET_CLA] != CLA) {
            ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);
        }
        
        byte ins = buffer[ISO7816.OFFSET_INS];
        
        switch (ins) {
            case INS_CHECK_CARD_CREATED:
                checkCardCreated(apdu);
                break;
                
            case INS_CLEAR_CARD:
                clearCard(apdu);
                break;
                
            case INS_UPDATE_CARD_ID:
                updateCardId(apdu);
                break;
                
            case INS_GET_CARD_ID:
                getCardId(apdu);
                break;
                
            case INS_UPDATE_CUSTOMER_INFO:
                updateCustomerInfo(apdu);
                break;
                
            case INS_GET_CUSTOMER_INFO:
                getCustomerInfo(apdu);
                break;
                
            case INS_UPDATE_BALANCE:
                updateBalance(apdu);
                break;
                
            case INS_GET_BALANCE:
                getBalance(apdu);
                break;
                
            case INS_UPDATE_PIN:
                updatePin(apdu);
                break;
                
            case INS_UPDATE_PICTURE:
                updatePicture(apdu);
                break;
                
            case INS_GET_PICTURE:
                getPicture(apdu);
                break;
                
            default:
                ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
        }
    }
    
    /**
     * CHECK CARD CREATED (INS 0x29)
     * Response: SW_SUCCESS if initialized, SW_CARD_NOT_INITIALIZED otherwise
     */
    private void checkCardCreated(APDU apdu) {
        // Check if cardId is set (card is initialized)
        boolean isInit = false;
        for (short i = 0; i < MAX_CARD_ID_LEN; i++) {
            if (cardId[i] != 0) {
                isInit = true;
                break;
            }
        }
        
        if (isInit && initialized) {
            ISOException.throwIt(SW_SUCCESS);
        } else {
            ISOException.throwIt(SW_CARD_NOT_INITIALIZED);
        }
    }
    
    /**
     * CLEAR CARD (INS 0x18)
     * Reset all card data
     */
    private void clearCard(APDU apdu) {
        // Clear all data
        Util.arrayFillNonAtomic(cardId, (short) 0, (short) cardId.length, (byte) 0);
        Util.arrayFillNonAtomic(customerInfo, (short) 0, (short) customerInfo.length, (byte) 0);
        Util.setShort(balance, (short) 0, (short) 0);
        Util.arrayFillNonAtomic(pin, (short) 0, (short) pin.length, (byte) 0);
        Util.arrayFillNonAtomic(picture, (short) 0, (short) picture.length, (byte) 0);
        pictureLength = 0;
        
        initialized = false;
        pinObject.reset();
        
        ISOException.throwIt(SW_SUCCESS);
    }
    
    /**
     * UPDATE CARD ID (INS 0x26)
     * Input: Card ID (string)
     */
    private void updateCardId(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        short lc = (short) (buffer[ISO7816.OFFSET_LC] & 0xFF);
        
        if (lc == 0 || lc > MAX_CARD_ID_LEN) {
            ISOException.throwIt(SW_WRONG_LENGTH);
        }
        
        // Receive data
        short bytesRead = apdu.setIncomingAndReceive();
        
        // Copy to cardId
        Util.arrayCopy(buffer, ISO7816.OFFSET_CDATA, cardId, (short) 0, lc);
        
        // Pad with zeros if needed
        if (lc < MAX_CARD_ID_LEN) {
            Util.arrayFillNonAtomic(cardId, lc, (short) (MAX_CARD_ID_LEN - lc), (byte) 0);
        }
        
        ISOException.throwIt(SW_SUCCESS);
    }
    
    /**
     * GET CARD ID (INS 0x27)
     * Response: Card ID string
     */
    private void getCardId(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        
        // Find actual length (until null terminator or end)
        short len = 0;
        while (len < MAX_CARD_ID_LEN && cardId[len] != 0) {
            len++;
        }
        
        if (len == 0) {
            ISOException.throwIt(SW_CARD_NOT_INITIALIZED);
        }
        
        // Send response
        Util.arrayCopy(cardId, (short) 0, buffer, (short) 0, len);
        apdu.setOutgoingAndSend((short) 0, len);
    }
    
    /**
     * UPDATE CUSTOMER INFO (INS 0x20)
     * Input: Customer info (format: full_name|date_of_birth|room|phone|email|id_number)
     */
    private void updateCustomerInfo(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        short lc = (short) (buffer[ISO7816.OFFSET_LC] & 0xFF);
        
        if (lc == 0 || lc > MAX_CUSTOMER_INFO_LEN) {
            ISOException.throwIt(SW_WRONG_LENGTH);
        }
        
        // Receive data
        short bytesRead = apdu.setIncomingAndReceive();
        
        // Copy to customerInfo
        Util.arrayCopy(buffer, ISO7816.OFFSET_CDATA, customerInfo, (short) 0, lc);
        
        // Pad with zeros if needed
        if (lc < MAX_CUSTOMER_INFO_LEN) {
            Util.arrayFillNonAtomic(customerInfo, lc, (short) (MAX_CUSTOMER_INFO_LEN - lc), (byte) 0);
        }
        
        // Mark as initialized if cardId is set
        if (cardId[0] != 0) {
            initialized = true;
        }
        
        ISOException.throwIt(SW_SUCCESS);
    }
    
    /**
     * GET CUSTOMER INFO (INS 0x13)
     * Response: Customer info string
     */
    private void getCustomerInfo(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        
        // Find actual length
        short len = 0;
        while (len < MAX_CUSTOMER_INFO_LEN && customerInfo[len] != 0) {
            len++;
        }
        
        if (len == 0) {
            ISOException.throwIt(SW_CARD_NOT_INITIALIZED);
        }
        
        // Send response
        Util.arrayCopy(customerInfo, (short) 0, buffer, (short) 0, len);
        apdu.setOutgoingAndSend((short) 0, len);
    }
    
    /**
     * UPDATE BALANCE (INS 0x16)
     * Input: 4 bytes (int) - new balance
     */
    private void updateBalance(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        short lc = (short) (buffer[ISO7816.OFFSET_LC] & 0xFF);
        
        if (lc != 4) {
            ISOException.throwIt(SW_WRONG_LENGTH);
        }
        
        // Receive data
        short bytesRead = apdu.setIncomingAndReceive();
        
        // Update balance
        Util.arrayCopy(buffer, ISO7816.OFFSET_CDATA, balance, (short) 0, (short) 4);
        
        ISOException.throwIt(SW_SUCCESS);
    }
    
    /**
     * GET BALANCE (INS 0x14)
     * Response: 4 bytes (int) - current balance
     */
    private void getBalance(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        
        // Send balance (4 bytes)
        Util.arrayCopy(balance, (short) 0, buffer, (short) 0, (short) 4);
        apdu.setOutgoingAndSend((short) 0, (short) 4);
    }
    
    /**
     * UPDATE PIN (INS 0x21)
     * Input: PIN (string)
     */
    private void updatePin(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        short lc = (short) (buffer[ISO7816.OFFSET_LC] & 0xFF);
        
        if (lc == 0 || lc > MAX_PIN_LEN) {
            ISOException.throwIt(SW_WRONG_LENGTH);
        }
        
        // Receive data
        short bytesRead = apdu.setIncomingAndReceive();
        
        // Copy to pin
        Util.arrayCopy(buffer, ISO7816.OFFSET_CDATA, pin, (short) 0, lc);
        
        // Update PIN object
        // OwnerPIN.update(byte[] pinData, short offset, byte length)
        pinObject.update(buffer, ISO7816.OFFSET_CDATA, (byte) lc);
        
        ISOException.throwIt(SW_SUCCESS);
    }
    
    /**
     * UPDATE PICTURE (INS 0x22)
     * Input: Picture bytes
     */
    private void updatePicture(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        short lc = (short) (buffer[ISO7816.OFFSET_LC] & 0xFF);
        
        if (lc == 0 || lc > MAX_PICTURE_LEN) {
            ISOException.throwIt(SW_WRONG_LENGTH);
        }
        
        // Clear old picture
        Util.arrayFillNonAtomic(picture, (short) 0, (short) picture.length, (byte) 0);
        pictureLength = 0;
        
        // Receive data in chunks if needed
        short totalReceived = 0;
        while (totalReceived < lc) {
            short bytesReceived = apdu.setIncomingAndReceive();
            Util.arrayCopy(buffer, ISO7816.OFFSET_CDATA, picture, totalReceived, bytesReceived);
            totalReceived += bytesReceived;
        }
        
        // Store actual length
        pictureLength = lc;
        
        ISOException.throwIt(SW_SUCCESS);
    }
    
    /**
     * GET PICTURE (INS 0x23)
     * Response: Picture bytes
     */
    private void getPicture(APDU apdu) {
        if (pictureLength == 0) {
            ISOException.throwIt(SW_CARD_NOT_INITIALIZED);
        }
        
        byte[] buffer = apdu.getBuffer();
        
        // Send in chunks if needed (max 256 bytes per APDU)
        short offset = 0;
        short maxChunk = (short) 256;
        
        while (offset < pictureLength) {
            short chunkLen = (short) ((pictureLength - offset > maxChunk) ? maxChunk : (pictureLength - offset));
            Util.arrayCopy(picture, offset, buffer, (short) 0, chunkLen);
            apdu.setOutgoingAndSend((short) 0, chunkLen);
            offset += chunkLen;
        }
    }
}

