package com.citizencard.backend;

import javax.smartcardio.*;
import java.io.IOException;
import java.util.List;

/**
 * Client kết nối với thẻ thật qua javax.smartcardio
 * Kết nối với JCIDE terminal khi chạy JCIDE
 * 
 * Theo hướng dẫn trong PDF: sử dụng TerminalFactory để quét và kết nối terminal
 */
public class RealCardClient {
    private TerminalFactory factory;
    private CardTerminal terminal;
    private Card card;
    private CardChannel channel;
    private boolean connected = false;
    
    // AID của applet (AID thực tế từ JCIDE)
    private static final byte[] AID_APPLET = {
        (byte) 0x11, (byte) 0x22, (byte) 0x33, (byte) 0x44, (byte) 0x55, (byte) 0x00
    };
    
    public RealCardClient() {
        // Khởi tạo TerminalFactory mặc định
        factory = TerminalFactory.getDefault();
    }
    
    /**
     * Kết nối với terminal đầu tiên có sẵn (JCIDE terminal)
     * Theo PDF: quét các terminal và chọn terminal đầu tiên
     */
    public synchronized void connect() throws IOException {
        if (connected) {
            return;
        }
        
        try {
            // 1. Liệt kê các card terminal (card reader)
            List<CardTerminal> terminals = factory.terminals().list();
            
            if (terminals.isEmpty()) {
                throw new IOException("Không tìm thấy card terminal nào. " +
                                    "Vui lòng đảm bảo JCIDE đang chạy và terminal đã được mở.");
            }
            
            // 2. Chọn terminal đầu tiên (JCIDE terminal)
            terminal = terminals.get(0);
            System.out.println("✅ Tìm thấy terminal: " + terminal.getName());
            
            // 3. Kiểm tra thẻ có trong terminal không
            if (!terminal.isCardPresent()) {
                throw new IOException("Không có thẻ trong terminal. " +
                                    "Vui lòng đảm bảo thẻ đã được đưa vào trong JCIDE.");
            }
            
            // 4. Kết nối với thẻ (protocol T=0)
            card = terminal.connect("T=0");
            System.out.println("✅ Đã kết nối với thẻ (Protocol: T=0)");
            
            // 5. Lấy channel để gửi APDU
            channel = card.getBasicChannel();
            if (channel == null) {
                throw new IOException("Không thể lấy card channel");
            }
            
            // 6. SELECT APPLET (AID) - theo PDF
            CommandAPDU selectCmd = new CommandAPDU(
                0x00,           // CLA
                0xA4,           // INS (SELECT)
                0x04,           // P1
                0x00,           // P2
                AID_APPLET      // AID data
            );
            
            ResponseAPDU response = channel.transmit(selectCmd);
            int sw = response.getSW();
            
            // Kiểm tra response theo PDF
            if (sw == 0x9000) {
                // Thành công
                connected = true;
                System.out.println("✅ SELECT APPLET thành công (SW: 0x" + 
                                 String.format("%04X", sw) + ")");
            } else if (sw == 0x6400) {
                // Thẻ bị vô hiệu hóa
                throw new IOException("Thẻ đã bị vô hiệu hóa (SW: 0x6400)");
            } else {
                // Lỗi khác
                throw new IOException("SELECT APPLET thất bại (SW: 0x" + 
                                    String.format("%04X", sw) + ")");
            }
            
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
    
    /**
     * Gửi APDU command và nhận response
     * Chuyển đổi từ byte[] sang CommandAPDU và ResponseAPDU
     */
    public synchronized byte[] sendApdu(byte[] apdu) throws IOException {
        if (!connected || channel == null) {
            // Tự động reconnect nếu cần
            connect();
        }
        
        try {
            // Kiểm tra thẻ còn trong terminal không
            if (!terminal.isCardPresent()) {
                connected = false;
                throw new IOException("Thẻ đã bị rút ra khỏi terminal");
            }
            
            // Tạo CommandAPDU từ byte array
            CommandAPDU command = new CommandAPDU(apdu);
            
            // Gửi APDU và nhận response
            ResponseAPDU response = channel.transmit(command);
            
            // Chuyển ResponseAPDU thành byte array
            // Format: [Data] + [SW1] + [SW2]
            byte[] responseBytes = response.getBytes();
            
            return responseBytes;
            
        } catch (CardException e) {
            connected = false;
            throw new IOException("Lỗi gửi APDU: " + e.getMessage(), e);
        }
    }
    
    /**
     * Đóng kết nối với thẻ
     * Theo PDF: card.disconnect(false)
     */
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
    
    /**
     * Kiểm tra trạng thái kết nối
     */
    public boolean isConnected() {
        if (!connected || card == null || channel == null) {
            return false;
        }
        
        try {
            // Kiểm tra thẻ còn trong terminal không
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
    
    /**
     * Lấy tên terminal hiện tại
     */
    public String getTerminalName() {
        return terminal != null ? terminal.getName() : null;
    }
    
    /**
     * Kiểm tra thẻ có trong terminal không
     */
    public boolean isCardPresent() {
        try {
            return terminal != null && terminal.isCardPresent();
        } catch (CardException e) {
            return false;
        }
    }
}

