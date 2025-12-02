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
    private static final byte INS_GET_BALANCE = (byte) 0x14;
    private static final byte INS_UPDATE_BALANCE = (byte) 0x16;
    private static final byte INS_UPDATE_CARD_ID = (byte) 0x26;
    private static final byte INS_GET_CARD_ID = (byte) 0x27;
    private static final byte INS_UPDATE_PIN = (byte) 0x21;
    private static final byte INS_VERIFY_PIN = (byte) 0x24;
    private static final byte INS_UNBLOCK_PIN = (byte) 0x25;
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
    private static final short SW_WRONG_DATA = (short) 0x6A80;

    private boolean initialized;
    private byte[] cardId;
    private byte[] customerInfo;
    private byte[] balance;
    private byte[] pin;
    private byte[] picture;

    private static final short MAX_CARD_ID_LEN = 50;
    private static final short MAX_CUSTOMER_INFO_LEN = 200;
    private static final short MAX_PIN_LEN = 20;
    private static final short MAX_PICTURE_LEN = 5000;

    private short pictureLength;

    private OwnerPIN pinObject;
    private byte[] pinTriesRemaining;
    private static final byte MAX_PIN_TRIES = 5;

    private citizen() {
        initialized = false;
        cardId = new byte[MAX_CARD_ID_LEN];
        customerInfo = new byte[MAX_CUSTOMER_INFO_LEN];
        balance = new byte[4];
        pin = new byte[MAX_PIN_LEN];
        picture = new byte[MAX_PICTURE_LEN];
        pictureLength = 0;

        pinObject = new OwnerPIN((byte) 5, (byte) 6);
        // Set default PIN to "000000"
        byte[] defaultPin = { (byte) 0x30, (byte) 0x30, (byte) 0x30, (byte) 0x30, (byte) 0x30, (byte) 0x30 };
        pinObject.update(defaultPin, (short) 0, (byte) 6);

        pinTriesRemaining = new byte[1];
        pinTriesRemaining[0] = MAX_PIN_TRIES;

        Util.arrayFillNonAtomic(balance, (short) 0, (short) 4, (byte) 0);
    }

    public static void install(byte[] bArray, short bOffset, byte bLength) {
        new citizen().register(bArray, (short) (bOffset + 1), bArray[bOffset]);
    }

    public void process(APDU apdu) {
        byte[] buffer = apdu.getBuffer();

        if (selectingApplet()) {
            ISOException.throwIt(SW_SUCCESS);
            return;
        }

        if (buffer[ISO7816.OFFSET_CLA] != CLA) {
            ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);
            return;
        }

        byte ins = buffer[ISO7816.OFFSET_INS];

        try {
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

                case INS_VERIFY_PIN:
                    verifyPin(apdu);
                    break;
                case INS_UNBLOCK_PIN:
                    unblockPin(apdu);
                    break;
                case INS_CHECK_PIN_STATUS:
                    checkPinStatus(apdu);
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
        } catch (ISOException e) {
            throw e;
        } catch (Exception e) {
            ISOException.throwIt(ISO7816.SW_UNKNOWN);
        }
    }

    private void checkCardCreated(APDU apdu) {
        if (initialized && cardId[0] != 0) {
            ISOException.throwIt(SW_SUCCESS);
        } else {
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
        initialized = false;
        pinObject.reset();
        pinTriesRemaining[0] = MAX_PIN_TRIES;
        ISOException.throwIt(SW_SUCCESS);
    }

    private void updateCardId(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        short lc = (short) (buffer[ISO7816.OFFSET_LC] & 0xFF);

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

    private void getCardId(APDU apdu) {
        byte[] buffer = apdu.getBuffer();

        short len = 0;
        while (len < MAX_CARD_ID_LEN && cardId[len] != 0) {
            len++;
        }

        if (len == 0) {
            ISOException.throwIt(SW_CARD_NOT_INITIALIZED);
        }

        Util.arrayCopy(cardId, (short) 0, buffer, (short) 0, len);
        apdu.setOutgoingAndSend((short) 0, len);
    }

    private void updateCustomerInfo(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        short lc = (short) (buffer[ISO7816.OFFSET_LC] & 0xFF);

        if (lc == 0 || lc > MAX_CUSTOMER_INFO_LEN) {
            ISOException.throwIt(SW_WRONG_LENGTH);
        }

        short bytesRead = apdu.setIncomingAndReceive();
        Util.arrayCopy(buffer, ISO7816.OFFSET_CDATA, customerInfo, (short) 0, lc);

        if (lc < MAX_CUSTOMER_INFO_LEN) {
            Util.arrayFillNonAtomic(customerInfo, lc, (short) (MAX_CUSTOMER_INFO_LEN - lc), (byte) 0);
        }

        initialized = true;
        ISOException.throwIt(SW_SUCCESS);
    }

    private void getCustomerInfo(APDU apdu) {
        byte[] buffer = apdu.getBuffer();

        short len = 0;
        while (len < MAX_CUSTOMER_INFO_LEN && customerInfo[len] != 0) {
            len++;
        }

        if (len > 0) {
            Util.arrayCopy(customerInfo, (short) 0, buffer, (short) 0, len);
            apdu.setOutgoingAndSend((short) 0, len);
        } else {
            ISOException.throwIt(SW_CARD_NOT_INITIALIZED);
        }
    }

    private void updateBalance(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        short lc = (short) (buffer[ISO7816.OFFSET_LC] & 0xFF);

        if (lc != 4) {
            ISOException.throwIt(SW_WRONG_LENGTH);
        }

        short bytesRead = apdu.setIncomingAndReceive();
        Util.arrayCopy(buffer, ISO7816.OFFSET_CDATA, balance, (short) 0, (short) 4);

        ISOException.throwIt(SW_SUCCESS);
    }

    private void getBalance(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        Util.arrayCopy(balance, (short) 0, buffer, (short) 0, (short) 4);
        apdu.setOutgoingAndSend((short) 0, (short) 4);
    }

    private void updatePin(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        short lc = (short) (buffer[ISO7816.OFFSET_LC] & 0xFF);

        if (lc == 0 || lc > MAX_PIN_LEN) {
            ISOException.throwIt(SW_WRONG_LENGTH);
        }

        short bytesRead = apdu.setIncomingAndReceive();

        if (bytesRead != lc) {
            ISOException.throwIt(SW_WRONG_LENGTH);
        }

        // Security check: new PIN must be different from current PIN
        // Check if the new PIN matches the current PIN
        boolean isSameAsOldPin = pinObject.check(buffer, ISO7816.OFFSET_CDATA, (byte) lc);
        if (isSameAsOldPin) {
            // Reset the PIN object state after check
            pinObject.reset();
            // New PIN is the same as old PIN - reject the change
            ISOException.throwIt(SW_WRONG_DATA);
        }

        Util.arrayCopy(buffer, ISO7816.OFFSET_CDATA, pin, (short) 0, lc);

        try {
            pinObject.update(buffer, ISO7816.OFFSET_CDATA, (byte) lc);
        } catch (Exception e) {
            ISOException.throwIt(SW_WRONG_DATA);
        }

        ISOException.throwIt(SW_SUCCESS);
    }

    private void verifyPin(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        short lc = (short) (buffer[ISO7816.OFFSET_LC] & 0xFF);

        if (pinTriesRemaining[0] == 0) {
            ISOException.throwIt(SW_CARD_LOCKED);
        }

        if (lc > 0) {
            short bytesRead = apdu.setIncomingAndReceive();

            if (lc > MAX_PIN_LEN) {
                pinTriesRemaining[0]--;
                if (pinTriesRemaining[0] == 0) {
                    ISOException.throwIt(SW_CARD_LOCKED);
                } else {
                    short sw = (short) (0x6300 | (pinTriesRemaining[0] & 0x0F));
                    ISOException.throwIt(sw);
                }
                return;
            }

            boolean isValid = pinObject.check(buffer, ISO7816.OFFSET_CDATA, (byte) lc);

            if (isValid) {
                pinTriesRemaining[0] = MAX_PIN_TRIES;
                pinObject.reset();
                ISOException.throwIt(SW_SUCCESS);
            } else {
                pinTriesRemaining[0]--;
                if (pinTriesRemaining[0] == 0) {
                    ISOException.throwIt(SW_CARD_LOCKED);
                } else {
                    short sw = (short) (0x6300 | (pinTriesRemaining[0] & 0x0F));
                    ISOException.throwIt(sw);
                }
            }
        } else {
            if (pinTriesRemaining[0] == 0) {
                ISOException.throwIt(SW_CARD_LOCKED);
            } else {
                ISOException.throwIt(SW_INVALID_PIN);
            }
        }
    }

    private void checkPinStatus(APDU apdu) {
        if (pinTriesRemaining[0] == 0) {
            ISOException.throwIt(SW_CARD_LOCKED);
        } else {
            ISOException.throwIt(SW_SUCCESS);
        }
    }

    private void unblockPin(APDU apdu) {
        pinTriesRemaining[0] = MAX_PIN_TRIES;
        pinObject.reset();
        ISOException.throwIt(SW_SUCCESS);
    }

    private void updatePicture(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        short lc = (short) (buffer[ISO7816.OFFSET_LC] & 0xFF);

        if (lc == 0 || lc > MAX_PICTURE_LEN) {
            ISOException.throwIt(SW_WRONG_LENGTH);
        }

        short bytesRead = apdu.setIncomingAndReceive();

        Util.arrayCopy(buffer, ISO7816.OFFSET_CDATA, picture, (short) 0, lc);
        pictureLength = lc;

        ISOException.throwIt(SW_SUCCESS);
    }

    private void getPicture(APDU apdu) {
        byte[] buffer = apdu.getBuffer();

        if (pictureLength > 0) {
            Util.arrayCopy(picture, (short) 0, buffer, (short) 0, pictureLength);
            apdu.setOutgoingAndSend((short) 0, pictureLength);
        } else {
            ISOException.throwIt(SW_CARD_NOT_INITIALIZED);
        }
    }
}
