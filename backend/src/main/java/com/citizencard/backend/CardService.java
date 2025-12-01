package com.citizencard.backend;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Service xử lý các lệnh thẻ theo INS code
 * Sử dụng RealCardClient để kết nối với JCIDE terminal qua javax.smartcardio
 */
public class CardService {
    private static final byte CLA = (byte) 0x00;
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
    
    private RealCardClient client;
    
    public CardService(RealCardClient client) {
        this.client = client;
    }
    
    /**
     * SELECT APPLET (INS A4)
     */
    public boolean selectApplet() throws IOException {
        byte[] apdu = new byte[] { CLA, INS_SELECT, 0x00, 0x00, 0x00 };
        byte[] response = client.sendApdu(apdu);
        return response.length >= 2 && response[response.length - 2] == (byte) 0x90 && response[response.length - 1] == 0x00;
    }
    
    /**
     * CHECK CARD CREATED (INS 29)
     */
    public boolean checkCardCreated() throws IOException {
        byte[] apdu = new byte[] { CLA, INS_CHECK_CARD_CREATED, 0x00, 0x00, 0x00 };
        byte[] response = client.sendApdu(apdu);
        if (response.length >= 2) {
            int sw = ((response[response.length - 2] & 0xFF) << 8) | (response[response.length - 1] & 0xFF);
            return sw == 0x9000; // Card đã được khởi tạo
        }
        return false;
    }
    
    /**
     * CLEAR CARD (INS 18)
     */
    public boolean clearCard() throws IOException {
        byte[] apdu = new byte[] { CLA, INS_CLEAR_CARD, 0x00, 0x00, 0x00 };
        byte[] response = client.sendApdu(apdu);
        return response.length >= 2 && response[response.length - 2] == (byte) 0x90 && response[response.length - 1] == 0x00;
    }
    
    /**
     * UPDATE CUSTOMER INFO (INS 20)
     * Gửi thông tin khởi tạo: full_name|date_of_birth|room_number|phone|email|id_number
     */
    public boolean updateCustomerInfo(String info) throws IOException {
        byte[] infoBytes = info.getBytes(StandardCharsets.UTF_8);
        byte[] apdu = new byte[5 + infoBytes.length];
        apdu[0] = CLA;
        apdu[1] = INS_UPDATE_CUSTOMER_INFO;
        apdu[2] = 0x00;
        apdu[3] = 0x00;
        apdu[4] = (byte) infoBytes.length;
        System.arraycopy(infoBytes, 0, apdu, 5, infoBytes.length);
        
        byte[] response = client.sendApdu(apdu);
        return response.length >= 2 && response[response.length - 2] == (byte) 0x90 && response[response.length - 1] == 0x00;
    }
    
    /**
     * GET CUSTOMER INFO (INS 13)
     */
    public String getCustomerInfo() throws IOException {
        byte[] apdu = new byte[] { CLA, INS_GET_CUSTOMER_INFO, 0x00, 0x00, 0x00 };
        byte[] response = client.sendApdu(apdu);
        if (response.length >= 2) {
            int sw = ((response[response.length - 2] & 0xFF) << 8) | (response[response.length - 1] & 0xFF);
            if (sw == 0x9000 && response.length > 2) {
                byte[] data = new byte[response.length - 2];
                System.arraycopy(response, 0, data, 0, data.length);
                return new String(data, StandardCharsets.UTF_8);
            }
        }
        return null;
    }
    
    /**
     * GET BALANCE (INS 14)
     */
    public int getBalance() throws IOException {
        byte[] apdu = new byte[] { CLA, INS_GET_BALANCE, 0x00, 0x00, 0x04 };
        byte[] response = client.sendApdu(apdu);
        if (response.length >= 6) {
            int sw = ((response[response.length - 2] & 0xFF) << 8) | (response[response.length - 1] & 0xFF);
            if (sw == 0x9000) {
                int balance = 0;
                for (int i = 0; i < 4; i++) {
                    balance = (balance << 8) | (response[i] & 0xFF);
                }
                return balance;
            }
        }
        return -1;
    }
    
    /**
     * UPDATE BALANCE (INS 16)
     */
    public boolean updateBalance(int newBalance) throws IOException {
        byte[] apdu = new byte[9];
        apdu[0] = CLA;
        apdu[1] = INS_UPDATE_BALANCE;
        apdu[2] = 0x00;
        apdu[3] = 0x00;
        apdu[4] = 0x04;
        apdu[5] = (byte) ((newBalance >> 24) & 0xFF);
        apdu[6] = (byte) ((newBalance >> 16) & 0xFF);
        apdu[7] = (byte) ((newBalance >> 8) & 0xFF);
        apdu[8] = (byte) (newBalance & 0xFF);
        
        byte[] response = client.sendApdu(apdu);
        return response.length >= 2 && response[response.length - 2] == (byte) 0x90 && response[response.length - 1] == 0x00;
    }
    
    /**
     * UPDATE CARD ID (INS 26)
     */
    public boolean updateCardId(String cardId) throws IOException {
        byte[] idBytes = cardId.getBytes(StandardCharsets.UTF_8);
        byte[] apdu = new byte[5 + idBytes.length];
        apdu[0] = CLA;
        apdu[1] = INS_UPDATE_CARD_ID;
        apdu[2] = 0x00;
        apdu[3] = 0x00;
        apdu[4] = (byte) idBytes.length;
        System.arraycopy(idBytes, 0, apdu, 5, idBytes.length);
        
        byte[] response = client.sendApdu(apdu);
        return response.length >= 2 && response[response.length - 2] == (byte) 0x90 && response[response.length - 1] == 0x00;
    }
    
    /**
     * GET CARD ID (INS 27)
     */
    public String getCardId() throws IOException {
        byte[] apdu = new byte[] { CLA, INS_GET_CARD_ID, 0x00, 0x00, 0x00 };
        byte[] response = client.sendApdu(apdu);
        if (response.length >= 2) {
            int sw = ((response[response.length - 2] & 0xFF) << 8) | (response[response.length - 1] & 0xFF);
            if (sw == 0x9000 && response.length > 2) {
                byte[] data = new byte[response.length - 2];
                System.arraycopy(response, 0, data, 0, data.length);
                return new String(data, StandardCharsets.UTF_8);
            }
        }
        return null;
    }
    
    /**
     * UPDATE PIN (INS 21)
     */
    public boolean updatePin(String pin) throws IOException {
        byte[] pinBytes = pin.getBytes(StandardCharsets.UTF_8);
        byte[] apdu = new byte[5 + pinBytes.length];
        apdu[0] = CLA;
        apdu[1] = INS_UPDATE_PIN;
        apdu[2] = 0x00;
        apdu[3] = 0x00;
        apdu[4] = (byte) pinBytes.length;
        System.arraycopy(pinBytes, 0, apdu, 5, pinBytes.length);
        
        byte[] response = client.sendApdu(apdu);
        return response.length >= 2 && response[response.length - 2] == (byte) 0x90 && response[response.length - 1] == 0x00;
    }
    
    /**
     * UPDATE PICTURE (INS 22)
     */
    public boolean updatePicture(byte[] pictureBytes) throws IOException {
        byte[] apdu = new byte[5 + pictureBytes.length];
        apdu[0] = CLA;
        apdu[1] = INS_UPDATE_PICTURE;
        apdu[2] = 0x00;
        apdu[3] = 0x00;
        apdu[4] = (byte) pictureBytes.length;
        System.arraycopy(pictureBytes, 0, apdu, 5, pictureBytes.length);
        
        byte[] response = client.sendApdu(apdu);
        return response.length >= 2 && response[response.length - 2] == (byte) 0x90 && response[response.length - 1] == 0x00;
    }
    
    /**
     * GET PICTURE (INS 23)
     */
    public byte[] getPicture() throws IOException {
        byte[] apdu = new byte[] { CLA, INS_GET_PICTURE, 0x00, 0x00, 0x00 };
        byte[] response = client.sendApdu(apdu);
        if (response.length >= 2) {
            int sw = ((response[response.length - 2] & 0xFF) << 8) | (response[response.length - 1] & 0xFF);
            if (sw == 0x9000 && response.length > 2) {
                byte[] data = new byte[response.length - 2];
                System.arraycopy(response, 0, data, 0, data.length);
                return data;
            }
        }
        return null;
    }
}


