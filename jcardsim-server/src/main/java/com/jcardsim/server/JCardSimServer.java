package com.jcardsim.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JCardSimServer - Smartcard Simulator Server
 * Mô phỏng smartcard qua TCP socket
 * Protocol: LENGTH(2 bytes BE) + APDU data/response
 */
public class JCardSimServer {
    private static final int PORT = 9025;
    private static final byte CLA = (byte) 0x00;
    
    // INS Codes
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
    
    // Card state (in-memory storage)
    private static class CardState {
        boolean initialized = false;
        String cardId = null;
        String customerInfo = null;
        int balance = 0;
        String pin = null;
        byte[] picture = null;
    }
    
    // Store card state by cardId (persistent across connections)
    private static final Map<String, CardState> cardStatesByCardId = new ConcurrentHashMap<>();
    
    // Temporary mapping: clientId -> cardId (until cardId is set)
    private static final Map<Long, String> clientToCardId = new ConcurrentHashMap<>();
    
    // Temporary CardState for clients without cardId yet
    private static final Map<Long, CardState> tempCardStates = new ConcurrentHashMap<>();
    
    public static void main(String[] args) {
        int port = PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port, using default: " + PORT);
            }
        }
        
        System.out.println("========================================");
        System.out.println("  JCardSimServer - Smartcard Simulator");
        System.out.println("  Port: " + port);
        System.out.println("========================================");
        
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started on port " + port);
            System.out.println("Waiting for connections...");
            
            while (true) {
                Socket clientSocket = serverSocket.accept();
                long clientId = System.currentTimeMillis();
                
                System.out.println("Client connected: " + clientSocket.getRemoteSocketAddress());
                
                // Handle client in separate thread
                new Thread(() -> handleClient(clientSocket, clientId)).start();
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void handleClient(Socket socket, long clientId) {
        try (DataInputStream input = new DataInputStream(socket.getInputStream());
             DataOutputStream output = new DataOutputStream(socket.getOutputStream())) {
            
            while (!socket.isClosed()) {
                try {
                    // Read: LENGTH (2 bytes BE) + APDU data
                    int length = input.readUnsignedShort();
                    byte[] apdu = new byte[length];
                    int totalRead = 0;
                    while (totalRead < length) {
                        int read = input.read(apdu, totalRead, length - totalRead);
                        if (read == -1) {
                            throw new IOException("Connection closed");
                        }
                        totalRead += read;
                    }
                    
                    // Process APDU (sẽ tự động get/create CardState bên trong)
                    byte[] response = processApdu(apdu, clientId);
                    
                    // Send: LENGTH (2 bytes BE) + Response data
                    output.writeShort((short) response.length);
                    output.write(response);
                    output.flush();
                    
                } catch (IOException e) {
                    System.out.println("Client disconnected: " + e.getMessage());
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("Error handling client: " + e.getMessage());
        } finally {
            // Clean up temporary mappings (card data persists by cardId)
            clientToCardId.remove(clientId);
            tempCardStates.remove(clientId);
            try {
                socket.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }
    
    /**
     * Lấy hoặc tạo CardState cho client
     * Nếu client đã có cardId, dùng cardId để lấy CardState (persistent)
     * Nếu chưa có, tạo CardState mới và map với clientId tạm thời
     */
    private static CardState getOrCreateCardState(long clientId) {
        String cardId = clientToCardId.get(clientId);
        if (cardId != null) {
            // Client đã có cardId, lấy CardState từ cardId (persistent)
            return cardStatesByCardId.computeIfAbsent(cardId, k -> new CardState());
        } else {
            // Client chưa có cardId, lấy hoặc tạo CardState tạm thời
            // CardState này sẽ được chuyển sang cardId khi UPDATE_CARD_ID được gọi
            return tempCardStates.computeIfAbsent(clientId, k -> new CardState());
        }
    }
    
    private static byte[] processApdu(byte[] apdu, long clientId) {
        if (apdu.length < 4) {
            return errorResponse(0x6A80); // Wrong length
        }
        
        byte cla = apdu[0];
        byte ins = apdu[1];
        byte p1 = apdu[2];
        byte p2 = apdu[3];
        
        if (cla != CLA) {
            return errorResponse(0x6E00); // Class not supported
        }
        
        // Get or create card state for this client
        CardState card = getOrCreateCardState(clientId);
        
        try {
            switch (ins) {
                case INS_SELECT:
                    return successResponse(new byte[0]);
                    
                case INS_CHECK_CARD_CREATED:
                    return card.initialized ? successResponse(new byte[]{(byte) 0x01}) : errorResponse(0x6A82);
                    
                case INS_CLEAR_CARD:
                    card.initialized = false;
                    card.cardId = null;
                    card.customerInfo = null;
                    card.balance = 0;
                    card.pin = null;
                    card.picture = null;
                    return successResponse(new byte[0]);
                    
                case INS_UPDATE_CUSTOMER_INFO:
                    if (apdu.length < 5) {
                        return errorResponse(0x6A80);
                    }
                    int infoLength = apdu[4] & 0xFF;
                    if (apdu.length < 5 + infoLength) {
                        return errorResponse(0x6A80);
                    }
                    byte[] infoBytes = new byte[infoLength];
                    System.arraycopy(apdu, 5, infoBytes, 0, infoLength);
                    card.customerInfo = new String(infoBytes, StandardCharsets.UTF_8);
                    card.initialized = true;
                    return successResponse(new byte[0]);
                    
                case INS_GET_CUSTOMER_INFO:
                    if (!card.initialized || card.customerInfo == null) {
                        return errorResponse(0x6A82);
                    }
                    return successResponse(card.customerInfo.getBytes(StandardCharsets.UTF_8));
                    
                case INS_GET_BALANCE:
                    if (!card.initialized) {
                        return errorResponse(0x6A82);
                    }
                    byte[] balanceBytes = new byte[4];
                    int balance = card.balance;
                    balanceBytes[0] = (byte) ((balance >> 24) & 0xFF);
                    balanceBytes[1] = (byte) ((balance >> 16) & 0xFF);
                    balanceBytes[2] = (byte) ((balance >> 8) & 0xFF);
                    balanceBytes[3] = (byte) (balance & 0xFF);
                    return successResponse(balanceBytes);
                    
                case INS_UPDATE_BALANCE:
                    if (apdu.length < 9) {
                        return errorResponse(0x6A80);
                    }
                    int newBalance = ((apdu[5] & 0xFF) << 24) | 
                                    ((apdu[6] & 0xFF) << 16) | 
                                    ((apdu[7] & 0xFF) << 8) | 
                                    (apdu[8] & 0xFF);
                    card.balance = newBalance;
                    
                    // Nếu đã có cardId, cập nhật CardState trong map (persistent)
                    if (card.cardId != null) {
                        cardStatesByCardId.put(card.cardId, card);
                    }
                    
                    return successResponse(new byte[0]);
                    
                case INS_UPDATE_CARD_ID:
                    if (apdu.length < 5) {
                        return errorResponse(0x6A80);
                    }
                    int idLength = apdu[4] & 0xFF;
                    if (apdu.length < 5 + idLength) {
                        return errorResponse(0x6A80);
                    }
                    byte[] idBytes = new byte[idLength];
                    System.arraycopy(apdu, 5, idBytes, 0, idLength);
                    String newCardId = new String(idBytes, StandardCharsets.UTF_8);
                    
                    // Nếu card đã có cardId khác, xóa CardState cũ
                    if (card.cardId != null && !card.cardId.equals(newCardId)) {
                        cardStatesByCardId.remove(card.cardId);
                    }
                    
                    // Set cardId mới
                    card.cardId = newCardId;
                    
                    // Lưu CardState vào map theo cardId (persistent)
                    cardStatesByCardId.put(newCardId, card);
                    
                    // Map clientId -> cardId để lần sau dùng cardId
                    clientToCardId.put(clientId, newCardId);
                    
                    // Xóa CardState tạm thời (đã chuyển sang cardId)
                    tempCardStates.remove(clientId);
                    
                    return successResponse(new byte[0]);
                    
                case INS_GET_CARD_ID:
                    if (card.cardId == null) {
                        return errorResponse(0x6A82);
                    }
                    return successResponse(card.cardId.getBytes(StandardCharsets.UTF_8));
                    
                case INS_UPDATE_PIN:
                    if (apdu.length < 5) {
                        return errorResponse(0x6A80);
                    }
                    int pinLength = apdu[4] & 0xFF;
                    if (apdu.length < 5 + pinLength) {
                        return errorResponse(0x6A80);
                    }
                    byte[] pinBytes = new byte[pinLength];
                    System.arraycopy(apdu, 5, pinBytes, 0, pinLength);
                    card.pin = new String(pinBytes, StandardCharsets.UTF_8);
                    
                    // Nếu đã có cardId, cập nhật CardState trong map (persistent)
                    if (card.cardId != null) {
                        cardStatesByCardId.put(card.cardId, card);
                    }
                    
                    return successResponse(new byte[0]);
                    
                case INS_UPDATE_PICTURE:
                    if (apdu.length < 5) {
                        return errorResponse(0x6A80);
                    }
                    int picLength = apdu[4] & 0xFF;
                    if (apdu.length < 5 + picLength) {
                        return errorResponse(0x6A80);
                    }
                    card.picture = new byte[picLength];
                    System.arraycopy(apdu, 5, card.picture, 0, picLength);
                    
                    // Nếu đã có cardId, cập nhật CardState trong map (persistent)
                    if (card.cardId != null) {
                        cardStatesByCardId.put(card.cardId, card);
                    }
                    
                    return successResponse(new byte[0]);
                    
                case INS_GET_PICTURE:
                    if (card.picture == null) {
                        return errorResponse(0x6A82);
                    }
                    return successResponse(card.picture);
                    
                default:
                    return errorResponse(0x6D00); // Instruction not supported
            }
        } catch (Exception e) {
            System.err.println("Error processing APDU: " + e.getMessage());
            return errorResponse(0x6F00); // Unknown error
        }
    }
    
    private static byte[] successResponse(byte[] data) {
        byte[] response = new byte[data.length + 2];
        System.arraycopy(data, 0, response, 0, data.length);
        response[data.length] = (byte) 0x90;
        response[data.length + 1] = 0x00;
        return response;
    }
    
    private static byte[] errorResponse(int sw) {
        return new byte[]{
            (byte) ((sw >> 8) & 0xFF),
            (byte) (sw & 0xFF)
        };
    }
}

