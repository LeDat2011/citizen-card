package com.citizencard.client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * SmartCard Client - Mô phỏng thẻ thông minh
 * Kết nối với JCardSimServer và gửi/nhận APDU commands
 * Có thể dùng như User hoặc Admin
 */
public class SmartCardClient {
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 9025;
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 10000;
    
    private String host;
    private int port;
    private Socket socket;
    private DataInputStream inputStream;
    private DataOutputStream outputStream;
    private boolean connected = false;
    private boolean cardInserted = false;
    
    // INS Codes
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
    
    public SmartCardClient() {
        this(DEFAULT_HOST, DEFAULT_PORT);
    }
    
    public SmartCardClient(String host, int port) {
        this.host = host;
        this.port = port;
    }
    
    /**
     * Kết nối đến JCardSimServer
     */
    public boolean connect() {
        try {
            socket = new Socket();
            socket.connect(new java.net.InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
            socket.setSoTimeout(READ_TIMEOUT_MS);
            inputStream = new DataInputStream(socket.getInputStream());
            outputStream = new DataOutputStream(socket.getOutputStream());
            connected = true;
            System.out.println("✅ Đã kết nối đến JCardSimServer tại " + host + ":" + port);
            return true;
        } catch (IOException e) {
            System.err.println("❌ Lỗi kết nối: " + e.getMessage());
            connected = false;
            return false;
        }
    }
    
    /**
     * Mô phỏng đưa thẻ vào (Card Insertion)
     */
    public void insertCard() {
        if (!connected) {
            System.out.println("❌ Chưa kết nối đến server. Vui lòng kết nối trước!");
            return;
        }
        
        if (cardInserted) {
            System.out.println("⚠️  Thẻ đã được đưa vào rồi!");
            return;
        }
        
        cardInserted = true;
        System.out.println("💳 Thẻ đã được đưa vào!");
        System.out.println("   Bạn có thể bắt đầu gửi APDU commands.");
    }
    
    /**
     * Mô phỏng rút thẻ ra (Card Removal)
     */
    public void removeCard() {
        if (!cardInserted) {
            System.out.println("⚠️  Không có thẻ nào được đưa vào!");
            return;
        }
        
        cardInserted = false;
        System.out.println("💳 Thẻ đã được rút ra!");
    }
    
    /**
     * Gửi APDU command và nhận response
     */
    public byte[] sendApdu(byte[] apdu) throws IOException {
        if (!connected) {
            throw new IOException("Chưa kết nối đến server");
        }
        
        if (!cardInserted) {
            throw new IOException("Thẻ chưa được đưa vào. Dùng lệnh 'insert' trước!");
        }
        
        // Gửi: LENGTH (2 bytes BE) + APDU data
        int length = apdu.length;
        outputStream.writeShort((short) length);
        outputStream.write(apdu);
        outputStream.flush();
        
        // Đọc: LENGTH (2 bytes BE) + Response data
        int responseLength = inputStream.readUnsignedShort();
        byte[] response = new byte[responseLength];
        int totalRead = 0;
        while (totalRead < responseLength) {
            int read = inputStream.read(response, totalRead, responseLength - totalRead);
            if (read == -1) {
                throw new IOException("Unexpected end of stream");
            }
            totalRead += read;
        }
        
        return response;
    }
    
    /**
     * Đóng kết nối
     */
    public void disconnect() {
        connected = false;
        cardInserted = false;
        try {
            if (inputStream != null) inputStream.close();
            if (outputStream != null) outputStream.close();
            if (socket != null && !socket.isClosed()) socket.close();
            System.out.println("🔌 Đã ngắt kết nối");
        } catch (IOException e) {
            System.err.println("Lỗi khi đóng kết nối: " + e.getMessage());
        }
    }
    
    /**
     * Helper: Tạo APDU command
     */
    private byte[] createApdu(byte ins, byte[] data) {
        if (data == null || data.length == 0) {
            return new byte[] { CLA, ins, 0x00, 0x00, 0x00 };
        }
        byte[] apdu = new byte[5 + data.length];
        apdu[0] = CLA;
        apdu[1] = ins;
        apdu[2] = 0x00;
        apdu[3] = 0x00;
        apdu[4] = (byte) data.length;
        System.arraycopy(data, 0, apdu, 5, data.length);
        return apdu;
    }
    
    /**
     * Helper: Parse response và hiển thị
     */
    private void printResponse(byte[] response) {
        if (response.length < 2) {
            System.out.println("❌ Response không hợp lệ");
            return;
        }
        
        int sw = ((response[response.length - 2] & 0xFF) << 8) | (response[response.length - 1] & 0xFF);
        
        if (sw == 0x9000) {
            System.out.println("✅ Success (SW: 0x" + String.format("%04X", sw) + ")");
            if (response.length > 2) {
                byte[] data = new byte[response.length - 2];
                System.arraycopy(response, 0, data, 0, data.length);
                System.out.println("📦 Data: " + bytesToHex(data));
                System.out.println("📝 Text: " + new String(data, StandardCharsets.UTF_8));
            }
        } else {
            System.out.println("❌ Error (SW: 0x" + String.format("%04X", sw) + ")");
        }
    }
    
    /**
     * Helper: Convert bytes to hex string
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }
    
    // ========== High-level Commands ==========
    
    /**
     * SELECT APPLET (INS A4)
     */
    public void selectApplet() {
        try {
            byte[] apdu = createApdu(INS_SELECT, null);
            System.out.print("📤 SELECT APPLET (A4): ");
            byte[] response = sendApdu(apdu);
            printResponse(response);
        } catch (IOException e) {
            System.err.println("❌ Lỗi: " + e.getMessage());
        }
    }
    
    /**
     * CHECK CARD CREATED (INS 29)
     */
    public void checkCardCreated() {
        try {
            byte[] apdu = createApdu(INS_CHECK_CARD_CREATED, null);
            System.out.print("📤 CHECK CARD CREATED (29): ");
            byte[] response = sendApdu(apdu);
            printResponse(response);
        } catch (IOException e) {
            System.err.println("❌ Lỗi: " + e.getMessage());
        }
    }
    
    /**
     * GET CARD ID (INS 27)
     */
    public void getCardId() {
        try {
            byte[] apdu = createApdu(INS_GET_CARD_ID, null);
            System.out.print("📤 GET CARD ID (27): ");
            byte[] response = sendApdu(apdu);
            printResponse(response);
        } catch (IOException e) {
            System.err.println("❌ Lỗi: " + e.getMessage());
        }
    }
    
    /**
     * GET BALANCE (INS 14)
     */
    public void getBalance() {
        try {
            byte[] apdu = createApdu(INS_GET_BALANCE, null);
            System.out.print("📤 GET BALANCE (14): ");
            byte[] response = sendApdu(apdu);
            if (response.length >= 6) {
                int sw = ((response[response.length - 2] & 0xFF) << 8) | (response[response.length - 1] & 0xFF);
                if (sw == 0x9000) {
                    int balance = ((response[0] & 0xFF) << 24) | 
                                 ((response[1] & 0xFF) << 16) | 
                                 ((response[2] & 0xFF) << 8) | 
                                 (response[3] & 0xFF);
                    System.out.println("💰 Số dư: " + String.format("%,d", balance) + " VND");
                } else {
                    printResponse(response);
                }
            } else {
                printResponse(response);
            }
        } catch (IOException e) {
            System.err.println("❌ Lỗi: " + e.getMessage());
        }
    }
    
    /**
     * GET CUSTOMER INFO (INS 13)
     */
    public void getCustomerInfo() {
        try {
            byte[] apdu = createApdu(INS_GET_CUSTOMER_INFO, null);
            System.out.print("📤 GET CUSTOMER INFO (13): ");
            byte[] response = sendApdu(apdu);
            printResponse(response);
        } catch (IOException e) {
            System.err.println("❌ Lỗi: " + e.getMessage());
        }
    }
    
    /**
     * UPDATE CARD ID (INS 26) - Admin only
     */
    public void updateCardId(String cardId) {
        try {
            byte[] data = cardId.getBytes(StandardCharsets.UTF_8);
            byte[] apdu = createApdu(INS_UPDATE_CARD_ID, data);
            System.out.print("📤 UPDATE CARD ID (26): ");
            byte[] response = sendApdu(apdu);
            printResponse(response);
        } catch (IOException e) {
            System.err.println("❌ Lỗi: " + e.getMessage());
        }
    }
    
    /**
     * UPDATE CUSTOMER INFO (INS 20) - Admin only
     */
    public void updateCustomerInfo(String info) {
        try {
            byte[] data = info.getBytes(StandardCharsets.UTF_8);
            byte[] apdu = createApdu(INS_UPDATE_CUSTOMER_INFO, data);
            System.out.print("📤 UPDATE CUSTOMER INFO (20): ");
            byte[] response = sendApdu(apdu);
            printResponse(response);
        } catch (IOException e) {
            System.err.println("❌ Lỗi: " + e.getMessage());
        }
    }
    
    /**
     * UPDATE BALANCE (INS 16) - Admin only
     */
    public void updateBalance(int balance) {
        try {
            byte[] data = new byte[4];
            data[0] = (byte) ((balance >> 24) & 0xFF);
            data[1] = (byte) ((balance >> 16) & 0xFF);
            data[2] = (byte) ((balance >> 8) & 0xFF);
            data[3] = (byte) (balance & 0xFF);
            byte[] apdu = createApdu(INS_UPDATE_BALANCE, data);
            System.out.print("📤 UPDATE BALANCE (16): ");
            byte[] response = sendApdu(apdu);
            printResponse(response);
        } catch (IOException e) {
            System.err.println("❌ Lỗi: " + e.getMessage());
        }
    }
    
    /**
     * UPDATE PIN (INS 21) - Admin only
     */
    public void updatePin(String pin) {
        try {
            byte[] data = pin.getBytes(StandardCharsets.UTF_8);
            byte[] apdu = createApdu(INS_UPDATE_PIN, data);
            System.out.print("📤 UPDATE PIN (21): ");
            byte[] response = sendApdu(apdu);
            printResponse(response);
        } catch (IOException e) {
            System.err.println("❌ Lỗi: " + e.getMessage());
        }
    }
    
    /**
     * CLEAR CARD (INS 18) - Admin only
     */
    public void clearCard() {
        try {
            byte[] apdu = createApdu(INS_CLEAR_CARD, null);
            System.out.print("📤 CLEAR CARD (18): ");
            byte[] response = sendApdu(apdu);
            printResponse(response);
        } catch (IOException e) {
            System.err.println("❌ Lỗi: " + e.getMessage());
        }
    }
    
    // ========== Main Interactive Shell ==========
    
    public static void main(String[] args) {
        SmartCardClient client = new SmartCardClient();
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("========================================");
        System.out.println("  SmartCard Client - JCardSimServer");
        System.out.println("  Mô phỏng thẻ thông minh");
        System.out.println("========================================");
        System.out.println();
        
        boolean running = true;
        
        while (running) {
            System.out.print("smartcard> ");
            String command = scanner.nextLine().trim();
            
            if (command.isEmpty()) continue;
            
            String[] parts = command.split("\\s+");
            String cmd = parts[0].toLowerCase();
            
            try {
                switch (cmd) {
                    case "connect":
                    case "conn":
                        client.connect();
                        break;
                        
                    case "disconnect":
                    case "disc":
                        client.disconnect();
                        break;
                        
                    case "insert":
                    case "i":
                        client.insertCard();
                        break;
                        
                    case "remove":
                    case "r":
                        client.removeCard();
                        break;
                        
                    case "select":
                        client.selectApplet();
                        break;
                        
                    case "check":
                        client.checkCardCreated();
                        break;
                        
                    case "getid":
                    case "id":
                        client.getCardId();
                        break;
                        
                    case "balance":
                    case "bal":
                        client.getBalance();
                        break;
                        
                    case "info":
                        client.getCustomerInfo();
                        break;
                        
                    case "setid":
                        if (parts.length < 2) {
                            System.out.println("❌ Usage: setid <card_id>");
                        } else {
                            client.updateCardId(parts[1]);
                        }
                        break;
                        
                    case "setinfo":
                        if (parts.length < 2) {
                            System.out.println("❌ Usage: setinfo <full_name|date_of_birth|room|phone|email|id_number>");
                        } else {
                            String info = command.substring(command.indexOf(' ') + 1);
                            client.updateCustomerInfo(info);
                        }
                        break;
                        
                    case "setbalance":
                    case "setbal":
                        if (parts.length < 2) {
                            System.out.println("❌ Usage: setbalance <amount>");
                        } else {
                            int amount = Integer.parseInt(parts[1]);
                            client.updateBalance(amount);
                        }
                        break;
                        
                    case "setpin":
                        if (parts.length < 2) {
                            System.out.println("❌ Usage: setpin <pin>");
                        } else {
                            client.updatePin(parts[1]);
                        }
                        break;
                        
                    case "clear":
                        client.clearCard();
                        break;
                        
                    case "help":
                    case "h":
                        printHelp();
                        break;
                        
                    case "exit":
                    case "quit":
                    case "q":
                        running = false;
                        break;
                        
                    default:
                        System.out.println("❌ Lệnh không hợp lệ. Gõ 'help' để xem danh sách lệnh.");
                }
            } catch (Exception e) {
                System.err.println("❌ Lỗi: " + e.getMessage());
            }
            
            System.out.println();
        }
        
        client.disconnect();
        scanner.close();
        System.out.println("👋 Tạm biệt!");
    }
    
    private static void printHelp() {
        System.out.println();
        System.out.println("📋 DANH SÁCH LỆNH:");
        System.out.println();
        System.out.println("🔌 Kết nối:");
        System.out.println("  connect, conn     - Kết nối đến JCardSimServer");
        System.out.println("  disconnect, disc  - Ngắt kết nối");
        System.out.println();
        System.out.println("💳 Thẻ:");
        System.out.println("  insert, i         - Đưa thẻ vào");
        System.out.println("  remove, r         - Rút thẻ ra");
        System.out.println();
        System.out.println("👤 User Commands:");
        System.out.println("  select            - SELECT APPLET (A4)");
        System.out.println("  check             - CHECK CARD CREATED (29)");
        System.out.println("  getid, id         - GET CARD ID (27)");
        System.out.println("  balance, bal      - GET BALANCE (14)");
        System.out.println("  info              - GET CUSTOMER INFO (13)");
        System.out.println();
        System.out.println("👨‍💼 Admin Commands:");
        System.out.println("  setid <id>        - UPDATE CARD ID (26)");
        System.out.println("  setinfo <info>    - UPDATE CUSTOMER INFO (20)");
        System.out.println("  setbalance <amt>  - UPDATE BALANCE (16)");
        System.out.println("  setpin <pin>      - UPDATE PIN (21)");
        System.out.println("  clear             - CLEAR CARD (18)");
        System.out.println();
        System.out.println("ℹ️  Khác:");
        System.out.println("  help, h           - Hiển thị trợ giúp");
        System.out.println("  exit, quit, q     - Thoát");
        System.out.println();
    }
}








