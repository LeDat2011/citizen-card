package citizen;

import javacard.framework.*;

public class citizen extends Applet {

    private static final byte[] CITIZEN_CARD_AID = {
            (byte) 0x11, (byte) 0x22, (byte) 0x33, (byte) 0x44, (byte) 0x55, (byte) 0x00
    };

    private static final byte INS_SELECT = (byte) 0xA4;
    private static final byte INS_CHECK_CARD_CREATED = (byte) 0x29;
    private static final byte INS_CLEAR_CARD = (byte) 0x18;
    private static final byte INS_UPDATE_CUSTOMER_INFO = (byte) 0x20;
    private static final byte INS_GET_CUSTOMER_INFO = (byte) 0x13;
    private static final byte INS_GET

    private static final byte INS_UPDATE_CARD_ID = (byte) 0x26;
    private static final byte INS_GET_CARD_ID = (byt
            ate static final byte INS_UPDATE_PIN = (byte) 0x21;
    pr

    private static final byte INS_CHECK_PIN_STATUS = (byte) 0x28;
    private static final byte INS_UPDATE_PICTURE = (byte) 0x22;
    private static final byte INS_GET_PICTURE = (byte) 0x23;

    private static final byte CLA = (byte) 0x00;

    private static final short SW_SUCCESS = (short) 0x9000;
    private static final short SW_CARD_NOT_INITIALIZED = (short) 0x6300;
    private static final short SW_INSUFFICIENT_BALANCE = (short) 0x6301;
    private static final short SW_INVALID_PIN = (short) 0x6302;
    private static final short SW_CARD_LOCKED = (byte) 0x6400;
    private static final short SW_WRONG_LENGTH = (short) 0x6700;
    private static final short SW_WRONG_DATA = (short) 0x6A8

    private boolean initialized;
    private byte[] cardId;

    private byte[] balance;
    private byte[] pin;
    private byte[] picture;

    private static final short MAX_CARD_ID_LEN = 50;
    private static final short MAX_CUSTOMER_INFO_LEN = 200;
    private static final short MAX_PIN_LEN = 20;
    private static final short MAX_PICTURE_LEN = 5000;

    private short pictureLength;

    private OwnerPIN pinObject;
    private byte[] pinTriesRemaining; // Lưu số lần thử còn lại (persistent)
    private static final byte MAX_PIN_TRIES = 5;

    private citizen() {

        cardId = new byte[MAX_CARD_ID_LEN];
        customerInfo = new byte[MAX_CUSTOMER_INFO_LEN];
        balance = new byte[4];
        pin = new byte[MAX_PIN_LEN];
        picture = new byte[MAX_PICTURE_LEN];

    
        // Khởi tạo OwnerPIN

    

        pinTriesRemaining = new byte[1];
        pinTriesRemaining[0] = MAX_PIN_TRIES; // Bắt đầu với 5 lần thử

        Util.arrayFillNonAtomic(balance, (short) 0, (short) 4, (byte) 0);
    }

    public static void install(byte[] bArray, short bOffset, byte bLength) {
        new citizen().register(bArray, (short) (bOffset + 1), bArray[bOffset]);
    }

    public void process(AP

        
        if (selectingApplet()) {

            return;
        }


            ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);
            return;
        }

        byte ins = buffer[ISO7816.OFFSET_INS];


            switch (ins) {
                case INS_CHECK_CARD_CREATED:
                    checkCardCreated(apdu);
                    break;


                    clearCard(apdu);
                    break;

                case INS_UPDATE_CARD_ID:
         

        
                case INS_GET_CARD_ID:
                    getCardId(apdu);
         

                case INS_UPDATE_CUSTOMER_INFO:

                    break;

                case INS_GET_CUSTOMER_INFO:
                    ge

            
                case INS_UPDATE_BALANCE:
                    up

            
                case INS_GET_BALANCE:
                    ge

            
                case INS_UPDATE_PIN:
                    up

            
                case INS_VERIFY_PIN:
                    ve

                case INS_UNBLOCK_PIN:
                    unblockPin(apdu);
                    br

                    checkPinStatus(apdu);
                    break;


                    updatePicture(apdu);
                    break;


                    getPicture(apdu);
                    break;


                    ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
            }
        } catch (ISOEx

            tch (Exception e) {
            ISOException.throwIt(ISO7816.SW_UNKNOWN);
        }

            
    private void checkCardCreated(APDU apdu) {
        if (initialized && cardId[0] != 0) {
     

            ISOException.throwIt(SW_CARD_NOT_INITIALIZED);
        }
    }

    private void clearCard(APDU apdu) {
        Util.arrayFillNonAtomic(cardId, (short) 0, (short) cardId.length, (byte) 0);
        Util.arrayFillNonAtomic(customerInfo, (short) 0, (short) customerInfo.length, (byte) 0);
        Util.arrayFillNonAtomic(balance, (short) 0, (short) 4, (byte) 0);
        Util.arrayFillNonAtomic(pin, (short) 0, (short) pin.length, (byte) 0);
        Util.arrayFillNonAtomic(picture, (short) 0, (short) picture.length, (byte) 0);
        pictureLength = 0;

        i

        
        ISOException.throwIt(SW_SUCCESS);
    }

    private void updateCardId(APDU apdu) {
     

    
        if (lc == 0 || lc > MAX_CARD_ID_LEN) {
            ISOException.throwIt(SW_WRONG_LENGTH);
        }

        short bytesRead = apdu.setIncomingAndReceive();

        Util.arrayCopy(buffer, ISO7816.OFFSET_CDATA, cardId, (short) 0, lc);

        if (lc < MAX_CARD_ID_LEN) {
            Util.arrayFillNonAtomic(cardId, lc, (short) (MAX_CARD_ID_LEN - lc), (byte) 0);
        }

        ISOException.throwIt(SW_SUCCESS);
    }

        ate void getCardId(APDU apdu) {
     

        short len = 0;
        while (len < MAX_CARD_ID_LEN && cardId[len] != 0) {
            len++;
        }

        if (len == 0) {
            ISOException.throwIt(SW_CARD_NOT_INITIALIZED);

        
        Util.arrayCopy(cardId, (short) 0, buffer, (short) 0, len);
        a

        
    private void updateCustomerInfo(APDU apdu) {

        short lc = (short) (buffer[ISO7816.OFFSET_LC] & 0xFF);


            ISOException.throwIt(SW_WRONG_LENGTH);
        }

        s

        Util.arrayCopy(buffer, ISO7816.OFFSET_CDATA, customerInfo, (short) 0, lc);


            Util.arrayFillNonAtomic(customerInfo, lc, (short) (MAX_CUSTOMER_INFO_LEN - lc), (byte) 0);
        }

        if (cardId[0] != 0) {
            initialized = true;
        }

        ISOException.throwIt(SW_SUCCESS);
    }

    private void getCustomerInfo(APDU apdu) {
        b

        short len = 0;
        while (len < MAX_CUSTOMER_INFO_LEN && customerInfo[len] != 0) {
         

        
        if (len == 0) {
            ISOException.throwIt(SW_CARD_NOT_INITIALIZED);
     

        Util.arrayCopy(customerInfo, (short) 0, buffer, (short) 0, len);
        apdu.setOutgoingAndSend((short) 0, len);
    }
     * 

    private void updateBalance(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        short lc = (short) (buffer[ISO7816.OFFSET_LC] & 0xFF);

        if (lc != 4) {
            ISOException.throwIt(SW_WRONG_LENGTH);
        }

        short bytesRead = apdu.setIncomingAndReceive();


        
        ISOException.throwIt(SW_SUCCESS);

        
    private void getBalance(APDU apdu) {
        byte[] buffer = apdu.getBuffer();


        apdu.setOutgoingAndSend((short) 0, (short) 4);
    }

    priva

        short lc = (short) (buffer[ISO7816.OFFSET_LC] & 0xFF);


            ISOException.throwIt(SW_WRONG_LENGTH);
        }

        short bytesRead = apdu.setIncomingAndReceive();

        if (bytesRead != lc) {

        }

        Util.arrayCopy(buffer, ISO7816.OFFSET_CDATA, pin, (short) 0, lc);

        t

        } catch (Exception e) {
            ISOException.throwIt(SW_WRONG_DATA);
        }

        ISOException.throwIt(SW_SUCCESS);
    }

    p

        short lc = (short) (buffer[ISO7816.OFFSET_LC] & 0xFF);
        
        // Kiểm tra manual counter trước
        if (pinTriesRemaining[0] == 0) {
            ISOException.throwIt(SW_CARD_LOCKED);
        }
        
        if (lc > 0) {
            short bytesRead = apdu.setIncomingAndReceive();
            
            if (lc > MAX_PIN_LEN) {
                // PIN quá dài - giảm counter
                pinTriesRemaining[0]--;
                if (pinTriesRemaining[0] == 0) {
                    ISOException.throwIt(SW_CARD_LOCKED);
                } else {
                    short sw = (short) (0x6300 | (pinTriesRemaining[0] & 0x0F));
                    ISOException.throwIt(sw);
     

            }
            
            // Verify PIN với OwnerPIN
            boolean isValid = pinObject.check(buffer, ISO7816.OFFSET_CDATA, (byte) lc);
            
            if (isValid) {
                // PIN đúng - reset counter về 5
                pinTriesRemaining[0] = MAX_PIN_TRIES;
                pinObject.reset();
                ISOException.throwIt(SW_SUCCESS);
            } else {
                // PIN sai - giảm counter
                pinTriesRemaining[0]--;
     

                } else {
                    short sw = (short) (0x6300 | (pinTriesRemaining[0] & 0x0F));
                    ISOException.throwIt(sw);
                }
       
       }
    
    
    
    } else {

       if(pnrisRmining[0] == 
    )
            ISOException.throwIt(SW_CARD_LOCKE
     

        }
    }

    
    ate void checkPinStatu(APDU apdu) {

    if (triesRemaining =
        ISOException.throwIt(SW_CARD_LOCKED);
    } else {

    }
    }

    private void unblockPin(APDU apdu) {
        pinObject.reset();
        ISOException.throwIt(SW_SUCCESS);
    }

    private void updatePicture(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        short lc = (short) (buffer[ISO7816.OFFSET_LC] & 0xFF);

        if (lc == 0 || lc > MAX_PICTURE_LEN) {
            ISOException.throwIt(SW_WRONG_LENGTH);
        }

        Util.arrayFillNonAtomic(picture, (short) 0, (short) picture.length, (byte) 0);
        pictureLength = 0;

        short bytesReceived = apdu.setIncomingAndReceive();
        if (bytesReceived != lc) {
            ISOException.throwIt(SW_WRONG_LENGTH);
        }

        Util.arrayCopy(buffer, ISO7816.OFFSET_CDATA, picture, (short) 0, lc);

        pictureLength = lc;

        ISOException.throwIt(SW_SUCCESS);
    }

    p

            ISOException.throwIt(SW_CARD_NOT_INITIALIZED);
        }

        byte[] buffer = apdu.getBuffer();

        short lenToSend = (short) ((pictureLength > 256) ? 256 : pictureLength);
        Util.arrayCopy(picture, (short) 0, buffer, (short) 0, lenToSend);
        apdu.setOutgoingAndSend((short) 0, lenToSend);
    }
}

