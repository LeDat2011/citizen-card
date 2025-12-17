package citizencard.util;

import citizencard.model.CitizenInfo;
import java.nio.charset.StandardCharsets;

/**
 * Parser for Citizen Information stored on smart card
 * 
 * Format: name|idNumber|roomNumber|dob|phone
 * Example: "Nguyen Van A|001234567890|A101|01/01/1990|0912345678"
 * 
 * Note: CitizenInfo model has fields: name, dob, idNumber, roomNumber, phone, email, pin, balance, photoPath, photoData
 * Card only stores: name, idNumber, roomNumber, dob, phone (other fields not stored on card)
 */
public class CitizenInfoParser {
    
    private static final String DELIMITER = "|";
    
    /**
     * Serialize CitizenInfo to bytes for card storage
     * Only stores: name|idNumber|roomNumber|dob|phone
     */
    public static byte[] serialize(CitizenInfo info) {
        if (info == null) {
            throw new IllegalArgumentException("CitizenInfo cannot be null");
        }
        
        String infoString = String.format("%s|%s|%s|%s|%s",
            info.name != null ? info.name : "",
            info.idNumber != null ? info.idNumber : "",
            info.roomNumber != null ? info.roomNumber : "",
            info.dob != null ? info.dob : "",
            info.phone != null ? info.phone : ""
        );
        
        return infoString.getBytes(StandardCharsets.UTF_8);
    }
    
    /**
     * Parse bytes from card to CitizenInfo object
     * Card format: name|idNumber|roomNumber|dob|phone
     * Creates CitizenInfo with: name, dob, idNumber, roomNumber, phone, email=null, pin=null, balance=0, photoPath=null, photoData=null
     */
    public static CitizenInfo parse(byte[] infoBytes) {
        if (infoBytes == null || infoBytes.length == 0) {
            throw new IllegalArgumentException("Info bytes cannot be null or empty");
        }
        
        try {
            // Debug: Show raw bytes
            System.out.println("[DEBUG] Raw info bytes length: " + infoBytes.length);
            System.out.println("[DEBUG] Raw info bytes (hex): " + bytesToHex(infoBytes));
            
            String infoString = new String(infoBytes, StandardCharsets.UTF_8).trim();
            
            // Debug: Show decoded string
            System.out.println("[DEBUG] Decoded info string: '" + infoString + "'");
            System.out.println("[DEBUG] String length: " + infoString.length());
            
            // Remove any null bytes or padding
            infoString = infoString.replaceAll("\u0000", "");
            
            // Debug: Show after null removal
            System.out.println("[DEBUG] After null removal: '" + infoString + "'");
            
            String[] parts = infoString.split("\\|", -1); // -1 to keep empty strings
            
            // Debug: Show parts
            System.out.println("[DEBUG] Split into " + parts.length + " parts:");
            for (int i = 0; i < parts.length; i++) {
                System.out.println("[DEBUG]   Part " + i + ": '" + parts[i] + "'");
            }
            
            if (parts.length < 5) {
                throw new IllegalArgumentException("Invalid info format: expected 5 parts, got " + parts.length);
            }
            
            // Extract fields from card
            String name = parts[0].isEmpty() ? null : parts[0];
            String idNumber = parts[1].isEmpty() ? null : parts[1];
            String roomNumber = parts[2].isEmpty() ? null : parts[2];
            String dob = parts[3].isEmpty() ? null : parts[3];
            String phone = parts[4].isEmpty() ? null : parts[4];
            
            // Create CitizenInfo with all required constructor parameters
            // Constructor: (name, dob, idNumber, roomNumber, phone, email, pin, balance, photoPath, photoData)
            return new CitizenInfo(
                name,           // name
                dob,            // dob
                idNumber,       // idNumber
                roomNumber,     // roomNumber
                phone,          // phone
                null,           // email - not stored on card
                null,           // pin - not stored on card (security!)
                0,              // balance - retrieved separately via getBalance()
                null,           // photoPath - not stored on card
                null            // photoData - retrieved separately via downloadAvatar()
            );
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse citizen info: " + e.getMessage(), e);
        }
    }
    
    /**
     * Validate CitizenInfo before serialization
     */
    public static boolean validate(CitizenInfo info) {
        if (info == null) return false;
        
        // Check required fields
        if (info.name == null || info.name.trim().isEmpty()) return false;
        if (info.idNumber == null || info.idNumber.trim().isEmpty()) return false;
        
        // Check for delimiter in fields (would break parsing)
        if (containsDelimiter(info.name)) return false;
        if (containsDelimiter(info.idNumber)) return false;
        if (containsDelimiter(info.roomNumber)) return false;
        if (containsDelimiter(info.dob)) return false;
        if (containsDelimiter(info.phone)) return false;
        
        return true;
    }
    
    private static boolean containsDelimiter(String value) {
        return value != null && value.contains(DELIMITER);
    }
    
    /**
     * Get info string for display/debugging
     */
    public static String toString(CitizenInfo info) {
        if (info == null) return "null";
        
        return String.format("CitizenInfo{name='%s', id='%s', room='%s', dob='%s', phone='%s'}",
            info.name, info.idNumber, info.roomNumber, info.dob, info.phone);
    }
    
    /**
     * Convert bytes to hex string for debugging
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }
}
