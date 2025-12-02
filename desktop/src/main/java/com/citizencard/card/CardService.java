package com.citizencard.card;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class CardService {
    private static final byte CLA = (byte) 0x00;
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
    private static final int MAX_PICTURE_CHUNK = 200;
    
    private RealCardClient client;
    private boolean appletSelected = false;
    
    public CardService(RealCardClient client) {
        this.client = client;
    }
    
    public boolean selectApplet() throws IOException {
        if (appletSelected && client.isConnected()) {
            return true;
        }
        
        if (!client.isConnected()) {
            client.connect();
        }
        
        client.selectApplet();
        appletSelected = true;
        return true;
    }
    
    public void disconnect() {
        appletSelected = false;
        client.close();
    }
    
    public boolean checkCardCreated() throws IOException {
        byte[] apdu = new byte[] { CLA, INS_CHECK_CARD_CREATED, 0x00, 0x00, 0x00 };
        byte[] response = client.sendApdu(apdu);
        if (response.length >= 2) {
            int sw = ((response[response.length - 2] & 0xFF) << 8) | (response[response.length - 1] & 0xFF);
            if (sw == 0x9000) {
                return true;
            } else if (sw == 0x6300) {
                return false;
            } else {
                throw new IOException("Error checking card status (SW: 0x" + String.format("%04X", sw) + ")");
            }
        }
        throw new IOException("Invalid response from card");
    }
    
    public boolean clearCard() throws IOException {
        byte[] apdu = new byte[] { CLA, INS_CLEAR_CARD, 0x00, 0x00, 0x00 };
        byte[] response = client.sendApdu(apdu);
        return response.length >= 2 && response[response.length - 2] == (byte) 0x90 && response[response.length - 1] == 0x00;
    }
    
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
    
    public int getBalance() throws IOException {
        if (!appletSelected) {
            selectApplet();
        }
        byte[] apdu = new byte[] { CLA, INS_GET_BALANCE, 0x00, 0x00, 0x04 };
        byte[] response = client.sendApdu(apdu);
        if (response.length >= 6) {
            int sw = ((response[response.length - 2] & 0xFF) << 8) | (response[response.length - 1] & 0xFF);
            if (sw == 0x9000) {
                int balance = ((response[0] & 0xFF) << 24) | 
                             ((response[1] & 0xFF) << 16) | 
                             ((response[2] & 0xFF) << 8) | 
                             (response[3] & 0xFF);
                return balance;
            }
        }
        return -1;
    }
    
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
    
    public String getCardId() throws IOException {
        byte[] apdu = new byte[] { CLA, INS_GET_CARD_ID, 0x00, 0x00, 0x00 };
        byte[] response = client.sendApdu(apdu);
        if (response.length >= 2) {
            int sw = ((response[response.length - 2] & 0xFF) << 8) | (response[response.length - 1] & 0xFF);
            if (sw == 0x9000 && response.length > 2) {
                byte[] data = new byte[response.length - 2];
                System.arraycopy(response, 0, data, 0, data.length);
                String cardId = new String(data, StandardCharsets.UTF_8);
                cardId = cardId.trim();
                int nullIndex = cardId.indexOf('\0');
                if (nullIndex >= 0) {
                    cardId = cardId.substring(0, nullIndex);
                }
                return cardId.isEmpty() ? null : cardId;
            } else if (sw == 0x6300) {
                throw new IOException("Card not initialized (SW: 0x6300)");
            } else {
                throw new IOException("Failed to get card ID (SW: 0x" + String.format("%04X", sw) + ")");
            }
        }
        throw new IOException("Invalid response from card");
    }
    
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
    
    public PinVerificationResult verifyPin(String pin) throws IOException {
        byte[] pinBytes = pin.getBytes(StandardCharsets.UTF_8);
        byte[] apdu = new byte[5 + pinBytes.length];
        apdu[0] = CLA;
        apdu[1] = INS_VERIFY_PIN;
        apdu[2] = 0x00;
        apdu[3] = 0x00;
        apdu[4] = (byte) pinBytes.length;
        System.arraycopy(pinBytes, 0, apdu, 5, pinBytes.length);
        
        byte[] response = client.sendApdu(apdu);
        if (response.length >= 2) {
            int sw = ((response[response.length - 2] & 0xFF) << 8) | (response[response.length - 1] & 0xFF);
            
            if (sw == 0x9000) {
                return new PinVerificationResult(true, (byte) 5, false);
            } else if (sw == 0x6400) {
                return new PinVerificationResult(false, (byte) 0, true);
            } else if ((sw & 0xFF00) == 0x6300) {
                byte triesRemaining = (byte) (sw & 0x0F);
                boolean isBlocked = (triesRemaining == 0);
                return new PinVerificationResult(false, triesRemaining, isBlocked);
            } else {
                return new PinVerificationResult(false, (byte) 0, false);
            }
        }
        return new PinVerificationResult(false, (byte) 0, false);
    }
    
    public boolean checkPinStatus() throws IOException {
        byte[] apdu = new byte[] { CLA, INS_CHECK_PIN_STATUS, 0x00, 0x00, 0x00 };
        byte[] response = client.sendApdu(apdu);
        if (response.length >= 2) {
            int sw = ((response[response.length - 2] & 0xFF) << 8) | (response[response.length - 1] & 0xFF);
            return sw == 0x6400;
        }
        return false;
    }
    
    public boolean unblockPin() throws IOException {
        byte[] apdu = new byte[] { CLA, INS_UNBLOCK_PIN, 0x00, 0x00, 0x00 };
        byte[] response = client.sendApdu(apdu);
        if (response.length >= 2) {
            int sw = ((response[response.length - 2] & 0xFF) << 8) | (response[response.length - 1] & 0xFF);
            return sw == 0x9000;
        }
        return false;
    }
    
    public static class PinVerificationResult {
        private boolean isValid;
        private byte triesRemaining;
        private boolean isBlocked;
        
        public PinVerificationResult(boolean isValid, byte triesRemaining, boolean isBlocked) {
            this.isValid = isValid;
            this.triesRemaining = triesRemaining;
            this.isBlocked = isBlocked;
        }
        
        public boolean isValid() {
            return isValid;
        }
        
        public byte getTriesRemaining() {
            return triesRemaining;
        }
        
        public boolean isBlocked() {
            return isBlocked;
        }
    }
    
    public boolean updatePicture(byte[] pictureBytes) throws IOException {
        if (pictureBytes == null) {
            pictureBytes = new byte[0];
        }
        if (pictureBytes.length == 0) {
            return sendPictureChunk(new byte[0], 0, true);
        }
        int offset = 0;
        int chunkIndex = 0;
        while (offset < pictureBytes.length) {
            int chunkLength = Math.min(MAX_PICTURE_CHUNK, pictureBytes.length - offset);
            boolean isFinal = (offset + chunkLength) >= pictureBytes.length;
            byte[] chunk = new byte[chunkLength];
            System.arraycopy(pictureBytes, offset, chunk, 0, chunkLength);
            if (!sendPictureChunk(chunk, chunkIndex, isFinal)) {
                return false;
            }
            offset += chunkLength;
            chunkIndex++;
        }
        return true;
    }
    
    public byte[] getPicture() throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int offset = 0;
        while (true) {
            byte[] apdu = new byte[] {
                CLA,
                INS_GET_PICTURE,
                (byte) ((offset >> 8) & 0xFF),
                (byte) (offset & 0xFF),
                (byte) MAX_PICTURE_CHUNK
            };
            byte[] response = client.sendApdu(apdu);
            if (response.length < 2) {
                throw new IOException("Invalid response from card when reading picture");
            }
            int sw = ((response[response.length - 2] & 0xFF) << 8) | (response[response.length - 1] & 0xFF);
            if (sw == 0x6A82 && offset == 0) {
                return null;
            }
            if (sw != 0x9000) {
                throw new IOException("Failed to read picture (SW: 0x" + String.format("%04X", sw) + ")");
            }
            int dataLength = response.length - 2;
            if (dataLength > 0) {
                buffer.write(response, 0, dataLength);
                offset += dataLength;
            }
            if (dataLength < MAX_PICTURE_CHUNK) {
                break;
            }
        }
        return buffer.toByteArray();
    }

    private boolean sendPictureChunk(byte[] chunk, int chunkIndex, boolean isFinal) throws IOException {
        byte[] apdu = new byte[5 + chunk.length];
        apdu[0] = CLA;
        apdu[1] = INS_UPDATE_PICTURE;
        apdu[2] = (byte) chunkIndex;
        apdu[3] = (byte) (isFinal ? 0x01 : 0x00);
        apdu[4] = (byte) chunk.length;
        if (chunk.length > 0) {
            System.arraycopy(chunk, 0, apdu, 5, chunk.length);
        }
        byte[] response = client.sendApdu(apdu);
        return response.length >= 2 && response[response.length - 2] == (byte) 0x90 && response[response.length - 1] == 0x00;
    }
}
