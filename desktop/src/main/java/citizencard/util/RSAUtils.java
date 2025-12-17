package citizencard.util;

import java.math.BigInteger;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;

/**
 * RSA Utilities for Citizen Card
 * 
 * Handles RSA key reconstruction and signature verification
 * for card authentication using challenge-response protocol.
 */
public class RSAUtils {

    /**
     * Generate PublicKey from serialized bytes from card
     * 
     * Format: [expLen:2][exp:expLen][modLen:2][mod:modLen]
     * Typically: 2 + 3 + 2 + 128 = 135 bytes for RSA-1024
     * 
     * @param data Serialized public key bytes from card
     * @return PublicKey object or null if failed
     */
    public static PublicKey generatePublicKeyFromBytes(byte[] data) {
        try {
            if (data == null || data.length < 7) {
                System.err.println("[RSA] Invalid public key data: too short");
                return null;
            }

            System.out.println("[RSA] Parsing public key, data length: " + data.length);

            // Extract exponent length (2 bytes, big-endian)
            int expLen = ((data[0] & 0xFF) << 8) | (data[1] & 0xFF);

            if (expLen <= 0 || expLen > 10) {
                System.err.println("[RSA] Invalid exponent length: " + expLen);
                return null;
            }

            // Extract exponent bytes
            byte[] expBytes = new byte[expLen];
            System.arraycopy(data, 2, expBytes, 0, expLen);

            // Extract modulus length (2 bytes, big-endian)
            int modOffset = 2 + expLen;
            int modLen = ((data[modOffset] & 0xFF) << 8) | (data[modOffset + 1] & 0xFF);

            if (modLen <= 0 || modLen > 256) {
                System.err.println("[RSA] Invalid modulus length: " + modLen);
                return null;
            }

            // Extract modulus bytes
            byte[] modBytes = new byte[modLen];
            System.arraycopy(data, modOffset + 2, modBytes, 0, modLen);

            System.out.println("[RSA] Exponent length: " + expLen + ", Modulus length: " + modLen);

            // Create BigInteger from bytes (unsigned)
            BigInteger exponent = new BigInteger(1, expBytes);
            BigInteger modulus = new BigInteger(1, modBytes);

            // Create RSA public key spec
            RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(modulus, exponent);

            // Generate public key
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

            System.out.println("[RSA] Public key generated successfully");
            return publicKey;

        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            System.err.println("[RSA] Failed to generate public key: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Verify RSA signature using SHA1withRSA
     * 
     * @param signature Signature bytes from card
     * @param publicKey Public key for verification
     * @param challenge Original challenge that was signed
     * @return true if signature is valid
     */
    public static boolean verifySignature(byte[] signature, PublicKey publicKey, String challenge) {
        try {
            if (signature == null || publicKey == null || challenge == null) {
                return false;
            }

            Signature verifier = Signature.getInstance("SHA1withRSA");
            verifier.initVerify(publicKey);
            verifier.update(challenge.getBytes());

            boolean valid = verifier.verify(signature);
            System.out.println("[RSA] Signature verification: " + (valid ? "VALID" : "INVALID"));

            return valid;

        } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException e) {
            System.err.println("[RSA] Signature verification failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Verify RSA signature using SHA1withRSA with byte array challenge
     */
    public static boolean verifySignature(byte[] signature, PublicKey publicKey, byte[] challenge) {
        try {
            if (signature == null || publicKey == null || challenge == null) {
                return false;
            }

            Signature verifier = Signature.getInstance("SHA1withRSA");
            verifier.initVerify(publicKey);
            verifier.update(challenge);

            boolean valid = verifier.verify(signature);
            System.out.println("[RSA] Signature verification: " + (valid ? "VALID" : "INVALID"));

            return valid;

        } catch (Exception e) {
            System.err.println("[RSA] Signature verification failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Check if key pair is valid (for testing)
     */
    public static boolean areKeysPair(PrivateKey privateKey, PublicKey publicKey) {
        try {
            String testMessage = "Test message for key pair validation";
            byte[] messageBytes = testMessage.getBytes();

            // Sign with private key
            Signature signer = Signature.getInstance("SHA256withRSA");
            signer.initSign(privateKey);
            signer.update(messageBytes);
            byte[] signature = signer.sign();

            // Verify with public key
            signer.initVerify(publicKey);
            signer.update(messageBytes);

            return signer.verify(signature);

        } catch (Exception e) {
            System.err.println("[RSA] Key pair validation failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Convert public key bytes to hex string for logging/storage
     */
    public static String bytesToHex(byte[] bytes) {
        if (bytes == null)
            return "null";

        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }

    /**
     * Convert hex string to byte array
     */
    public static byte[] hexToBytes(String hex) {
        if (hex == null || hex.isEmpty()) {
            return new byte[0];
        }

        // Remove spaces
        hex = hex.replace(" ", "");

        if (hex.length() % 2 != 0) {
            throw new IllegalArgumentException("Invalid hex string length");
        }

        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < hex.length(); i += 2) {
            bytes[i / 2] = (byte) Integer.parseInt(hex.substring(i, i + 2), 16);
        }

        return bytes;
    }
}
