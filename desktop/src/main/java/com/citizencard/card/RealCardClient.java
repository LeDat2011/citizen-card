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

    // Protocol constant - chỉ sử dụng T=1
    private static final String REQUIRED_PROTOCOL = "T=1";

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
                throw new IOException("No card terminal found. " +
                        "Please ensure JCIDE is running and the terminal is open.");
            }

            terminal = terminals.get(0);
            System.out.println("[OK] Found terminal: " + terminal.getName());

            boolean cardPresent = terminal.isCardPresent();
            System.out.println("[INFO] Card present in terminal: " + (cardPresent ? "Yes" : "No"));

            if (!cardPresent) {
                throw new IOException("No card in terminal. " +
                        "Please ensure a card is inserted in JCIDE.");
            }

            System.out.println("[INFO] Connecting to card using protocol " + REQUIRED_PROTOCOL + "...");
            try {
                card = terminal.connect(REQUIRED_PROTOCOL);
            } catch (CardException e) {
                throw new IOException("Failed to connect with protocol " + REQUIRED_PROTOCOL +
                        ". Please ensure JCIDE is configured to use T=1. " +
                        "Error: " + e.getMessage(), e);
            }

            String protocol = card.getProtocol();
            System.out.println("[OK] Connected to card (Protocol: " + protocol + ")");

            // Nghiêm ngặt: chỉ chấp nhận T=1
            if (!protocol.equals(REQUIRED_PROTOCOL)) {
                String errorMsg = String.format(
                        "❌ ERROR: Required protocol is %s but terminal is using %s. " +
                                "Please configure JCIDE to use T=1 (select 'T=1' in dropdown, not 'T=0 | T=1').",
                        REQUIRED_PROTOCOL, protocol);
                System.err.println(errorMsg);
                close();
                throw new IOException(errorMsg);
            }

            System.out.println("[OK] Protocol " + REQUIRED_PROTOCOL + " activated successfully!");

            channel = card.getBasicChannel();
            if (channel == null) {
                throw new IOException("Failed to get card channel");
            }

            connected = true;
            System.out.println("[OK] Connected to terminal: " + terminal.getName());

        } catch (CardException e) {
            connected = false;
            close();
            throw new IOException("Error connecting to card: " + e.getMessage(), e);
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
                    AID_APPLET);

            ResponseAPDU response = channel.transmit(selectCmd);
            int sw = response.getSW();

            if (sw == 0x9000) {
                System.out.println("[OK] SELECT APPLET successful (SW: 0x" +
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
                System.out.println("[OK] Disconnected from card");
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
