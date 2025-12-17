package citizencard.model;

/**
 * Enhanced Citizen Information Data Class with Photo Support
 * 
 * Represents citizen information for card creation and management
 */
public class CitizenInfo {
    public final String name;
    public final String dob;
    public final String idNumber;
    public final String roomNumber;
    public final String phone;
    public final String email;
    public final String pin;
    public final long balance;
    public final String photoPath;
    public final byte[] photoData;
    
    public CitizenInfo(String name, String dob, String idNumber, String roomNumber, 
                      String phone, String email, String pin, long balance, 
                      String photoPath, byte[] photoData) {
        this.name = name;
        this.dob = dob;
        this.idNumber = idNumber;
        this.roomNumber = roomNumber;
        this.phone = phone;
        this.email = email;
        this.pin = pin;
        this.balance = balance;
        this.photoPath = photoPath;
        this.photoData = photoData;
    }
}