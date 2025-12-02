package com.citizencard.card;

import javax.smartcardio.*;
import java.io.IOException;
import java.util.List;

public class RealCardClient {
    private TerminalFactory factory;
    private CardTerminal terminal;
    private Card card;
    private CardChannel channel;
    private boolean connected = false;
    
    private static final byte[] AID_APPLET = {
        (byte) 0x11, (byte) 0x22, (byte) 0x33, (byte) 0x44, (byte) 0x55, (byte) 0x00
    };
    
    public RealCardClient() {
        factory = TerminalFactory.getDefault();
    }
    
    public synchronized void connect() throws IOException {
        if (connected) {
            return;
        }
        
        try {
            List<CardTerminal> terminals = factory.terminals().list();
            
            if (terminals.isEmpty()) {
                throw new IOException("Không tìm thấy card terminal nào. " +
                                    "Vui lòng đảm bảo JCIDE đang chạy và terminal đã được mở.");
            }
            
            terminal = terminals.get(0);
            System.out.println("✅ Tìm thấy terminal: " + terminal.getName());
            
            if (!terminal.isCardPresent()) {
                throw new IOException("Không có thẻ trong terminal. " +
                                    "Vui lòng đảm bảo thẻ đã được đưa vào trong JCIDE.");
            }
            
            card = terminal.connect("T=1");
            System.out.println("✅ Đã kết nối với thẻ (Protocol: T=1)");
            
            channel = card.getBasicChannel();
            if (channel == null) {
                throw new IOException("Không thể lấy card channel");
            }
            
            connected = true;
            System.out.println("✅ Đã kết nối với terminal: " + terminal.getName());
            
        } catch (CardException e) {
            connected = false;
            close();
            throw new IOException("Lỗi kết nối với thẻ: " + e.getMessage(), e);
        } catch (Exception e) {
            connected = false;
            close();
            if (e instanceof IOException) {
                throw (IOException) e;
            }
            throw new IOException("Lỗi không xác định: " + e.getMessage(), e);
        }
    }
    
    public synchronized void selectApplet() throws IOException {
        if (!connected || channel == null) {
            connect();
        }
        
        try {
            CommandAPDU selectCmd = new CommandAPDU(
                0x00,
                0xA4,
                0x04,
                0x00,
                AID_APPLET
            );
            
            ResponseAPDU response = channel.transmit(selectCmd);
            int sw = response.getSW();
            
            if (sw == 0x9000) {
                System.out.println("✅ SELECT APPLET thành công (SW: 0x" + 
                                 String.format("%04X", sw) + ")");
            } else if (sw == 0x6400) {
                throw new IOException("Thẻ đã bị vô hiệu hóa (SW: 0x6400)");
            } else {
                throw new IOException("SELECT APPLET thất bại (SW: 0x" + 
                                    String.format("%04X", sw) + ")");
            }
        } catch (CardException e) {
            throw new IOException("Lỗi SELECT APPLET: " + e.getMessage(), e);
        }
    }
    
    public synchronized byte[] sendApdu(byte[] apdu) throws IOException {
        if (!connected || channel == null) {
            connect();
        }
        
        try {
            if (!terminal.isCardPresent()) {
                connected = false;
                throw new IOException("Thẻ đã bị rút ra khỏi terminal");
            }
            
            CommandAPDU command = new CommandAPDU(apdu);
            
            ResponseAPDU response = channel.transmit(command);
            
            byte[] responseBytes = response.getBytes();
            
            return responseBytes;
            
        } catch (CardException e) {
            connected = false;
            throw new IOException("Lỗi gửi APDU: " + e.getMessage(), e);
        }
    }
    
    public synchronized void close() {
        connected = false;
        channel = null;
        
        try {
            if (card != null) {
                card.disconnect(false);
                System.out.println("✅ Đã ngắt kết nối với thẻ");
            }
        } catch (CardException e) {
            System.err.println("Lỗi khi đóng kết nối: " + e.getMessage());
        } finally {
            card = null;
            terminal = null;
        }
    }
    
    public boolean isConnected() {
        if (!connected || card == null || channel == null) {
            return false;
        }
        
        try {
            if (terminal != null && !terminal.isCardPresent()) {
                connected = false;
                return false;
            }
            return true;
        } catch (CardException e) {
            connected = false;
            return false;
        }
    }
    
    public String getTerminalName() {
        return terminal != null ? terminal.getName() : null;
    }
    
    public boolean isCardPresent() {
        try {
            return terminal != null && terminal.isCardPresent();
        } catch (CardException e) {
            return false;
        }
    }
}
