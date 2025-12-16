package citizencard;

import javax.smartcardio.*;
import java.io.IOException;
import java.util.List;

/**
 * Real Card Client - Stub for compatibility
 * Main functionality moved to CardService
 */
public class RealCardClient {
    private TerminalFactory factory;
    private CardTerminal terminal;
    private Card card;
    private CardChannel channel;
    private boolean connected = false;

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
                throw new IOException("No card terminals found");
            }

            terminal = terminals.get(0);
            if (!terminal.isCardPresent()) {
                throw new IOException("No card present in terminal");
            }

            card = terminal.connect(REQUIRED_PROTOCOL);
            channel = card.getBasicChannel();
            connected = true;

        } catch (CardException e) {
            throw new IOException("Failed to connect to card: " + e.getMessage(), e);
        }
    }

    public void selectApplet() throws IOException {
        if (!connected) {
            throw new IOException("Not connected to card");
        }

        try {
            byte[] selectCommand = new byte[5 + AID_APPLET.length];
            selectCommand[0] = (byte) 0x00; // CLA
            selectCommand[1] = (byte) 0xA4; // INS (SELECT)
            selectCommand[2] = (byte) 0x04; // P1
            selectCommand[3] = (byte) 0x00; // P2
            selectCommand[4] = (byte) AID_APPLET.length; // Lc
            System.arraycopy(AID_APPLET, 0, selectCommand, 5, AID_APPLET.length);

            CommandAPDU command = new CommandAPDU(selectCommand);
            ResponseAPDU response = channel.transmit(command);

            if (response.getSW() != 0x9000) {
                throw new IOException("Failed to select applet: " + String.format("0x%04X", response.getSW()));
            }

        } catch (CardException e) {
            throw new IOException("Error selecting applet: " + e.getMessage(), e);
        }
    }

    public byte[] sendApdu(byte[] apduCommand) throws IOException {
        if (!connected) {
            throw new IOException("Not connected to card");
        }

        try {
            CommandAPDU command = new CommandAPDU(apduCommand);
            ResponseAPDU response = channel.transmit(command);
            return response.getBytes();

        } catch (CardException e) {
            throw new IOException("Error sending APDU: " + e.getMessage(), e);
        }
    }

    public boolean isConnected() {
        return connected;
    }

    public void close() {
        if (card != null) {
            try {
                card.disconnect(false);
            } catch (CardException e) {
                System.err.println("Error disconnecting card: " + e.getMessage());
            }
        }
        connected = false;
        card = null;
        channel = null;
        terminal = null;
    }
}