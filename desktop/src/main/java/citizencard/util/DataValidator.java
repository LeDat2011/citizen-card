package citizencard.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

/**
 * Data Validation Utility Class
 * 
 * Provides comprehensive validation for all input data types
 */
public class DataValidator {
    
    // Regex patterns for validation
    private static final Pattern PHONE_PATTERN = Pattern.compile("^(0[3|5|7|8|9])+([0-9]{8})$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$");
    private static final Pattern ID_NUMBER_PATTERN = Pattern.compile("^\\d{12}$");
    private static final Pattern PIN_PATTERN = Pattern.compile("^\\d{4}$");
    private static final Pattern CARD_ID_PATTERN = Pattern.compile("^CITIZEN-CARD-\\d{14}$");
    private static final Pattern NAME_PATTERN = Pattern.compile("^[\\p{L}\\s]{2,50}$");
    private static final Pattern ADDRESS_PATTERN = Pattern.compile("^[\\p{L}\\p{N}\\s,.-]{5,200}$");
    
    // Date formatters
    private static final DateTimeFormatter[] DATE_FORMATTERS = {
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("dd-MM-yyyy"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd")
    };
    
    /**
     * Validation Result Class
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;
        
        public ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }
        
        public boolean isValid() { return valid; }
        public String getErrorMessage() { return errorMessage; }
        
        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }
        
        public static ValidationResult error(String message) {
            return new ValidationResult(false, message);
        }
    }
    
    /**
     * Validate full name
     */
    public static ValidationResult validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return ValidationResult.error("Họ tên không được để trống");
        }
        
        String trimmedName = name.trim();
        
        if (trimmedName.length() < 2) {
            return ValidationResult.error("Họ tên phải có ít nhất 2 ký tự");
        }
        
        if (trimmedName.length() > 50) {
            return ValidationResult.error("Họ tên không được vượt quá 50 ký tự");
        }
        
        if (!NAME_PATTERN.matcher(trimmedName).matches()) {
            return ValidationResult.error("Họ tên chỉ được chứa chữ cái và khoảng trắng");
        }
        
        // Check for consecutive spaces
        if (trimmedName.contains("  ")) {
            return ValidationResult.error("Họ tên không được chứa khoảng trắng liên tiếp");
        }
        
        return ValidationResult.success();
    }
    
    /**
     * Validate phone number (Vietnamese format)
     */
    public static ValidationResult validatePhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return ValidationResult.error("Số điện thoại không được để trống");
        }
        
        String cleanPhone = phone.trim().replaceAll("\\s+", "");
        
        if (!PHONE_PATTERN.matcher(cleanPhone).matches()) {
            return ValidationResult.error("Số điện thoại không hợp lệ. Định dạng: 0xxxxxxxxx (10 số)");
        }
        
        return ValidationResult.success();
    }
    
    /**
     * Validate email address
     */
    public static ValidationResult validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return ValidationResult.error("Email không được để trống");
        }
        
        String trimmedEmail = email.trim();
        
        if (trimmedEmail.length() > 100) {
            return ValidationResult.error("Email không được vượt quá 100 ký tự");
        }
        
        if (!EMAIL_PATTERN.matcher(trimmedEmail).matches()) {
            return ValidationResult.error("Định dạng email không hợp lệ");
        }
        
        return ValidationResult.success();
    }
    
    /**
     * Validate ID number (12 digits)
     */
    public static ValidationResult validateIdNumber(String idNumber) {
        if (idNumber == null || idNumber.trim().isEmpty()) {
            return ValidationResult.error("Số CMND/CCCD không được để trống");
        }
        
        String cleanId = idNumber.trim().replaceAll("\\s+", "");
        
        if (!ID_NUMBER_PATTERN.matcher(cleanId).matches()) {
            return ValidationResult.error("Số CMND/CCCD phải có đúng 12 chữ số");
        }
        
        return ValidationResult.success();
    }
    
    /**
     * Validate PIN (4 digits)
     */
    public static ValidationResult validatePin(String pin) {
        if (pin == null || pin.trim().isEmpty()) {
            return ValidationResult.error("Mã PIN không được để trống");
        }
        
        String cleanPin = pin.trim();
        
        if (!PIN_PATTERN.matcher(cleanPin).matches()) {
            return ValidationResult.error("Mã PIN phải có đúng 4 chữ số");
        }
        
        // Check for weak PINs
        if (isWeakPin(cleanPin)) {
            return ValidationResult.error("Mã PIN quá yếu. Tránh dùng: 0000, 1111, 1234, 0123");
        }
        
        return ValidationResult.success();
    }
    
    /**
     * Validate room number
     */
    public static ValidationResult validateRoomNumber(String roomNumber) {
        if (roomNumber == null || roomNumber.trim().isEmpty()) {
            return ValidationResult.error("Số phòng không được để trống");
        }
        
        String trimmedRoom = roomNumber.trim();
        
        if (trimmedRoom.length() < 1) {
            return ValidationResult.error("Số phòng phải có ít nhất 1 ký tự");
        }
        
        if (trimmedRoom.length() > 10) {
            return ValidationResult.error("Số phòng không được vượt quá 10 ký tự");
        }
        
        // Allow alphanumeric room numbers (e.g., A101, B205, 301)
        if (!trimmedRoom.matches("^[A-Za-z0-9]{1,10}$")) {
            return ValidationResult.error("Số phòng chỉ được chứa chữ cái và số (VD: A101, B205, 301)");
        }
        
        return ValidationResult.success();
    }
    
    /**
     * Validate date of birth
     */
    public static ValidationResult validateDateOfBirth(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return ValidationResult.error("Ngày sinh không được để trống");
        }
        
        LocalDate date = parseDate(dateStr.trim());
        if (date == null) {
            return ValidationResult.error("Định dạng ngày sinh không hợp lệ. Sử dụng: dd/MM/yyyy");
        }
        
        LocalDate now = LocalDate.now();
        LocalDate minDate = now.minusYears(100);
        LocalDate maxDate = now.minusYears(16);
        
        if (date.isBefore(minDate)) {
            return ValidationResult.error("Ngày sinh không được quá 100 năm trước");
        }
        
        if (date.isAfter(maxDate)) {
            return ValidationResult.error("Tuổi phải từ 16 trở lên");
        }
        
        if (date.isAfter(now)) {
            return ValidationResult.error("Ngày sinh không được là tương lai");
        }
        
        return ValidationResult.success();
    }
    
    /**
     * Validate balance amount
     */
    public static ValidationResult validateBalance(String balanceStr) {
        if (balanceStr == null || balanceStr.trim().isEmpty()) {
            return ValidationResult.error("Số dư không được để trống");
        }
        
        try {
            String cleanBalance = balanceStr.trim().replaceAll("[,.]", "");
            long balance = Long.parseLong(cleanBalance);
            
            if (balance < 0) {
                return ValidationResult.error("Số dư không được âm");
            }
            
            if (balance > 100_000_000) { // 100 million VND
                return ValidationResult.error("Số dư không được vượt quá 100,000,000 VND");
            }
            
            return ValidationResult.success();
            
        } catch (NumberFormatException e) {
            return ValidationResult.error("Số dư phải là số nguyên hợp lệ");
        }
    }
    
    /**
     * Validate card ID format
     */
    public static ValidationResult validateCardId(String cardId) {
        if (cardId == null || cardId.trim().isEmpty()) {
            return ValidationResult.error("ID thẻ không được để trống");
        }
        
        String trimmedCardId = cardId.trim();
        
        if (!CARD_ID_PATTERN.matcher(trimmedCardId).matches()) {
            return ValidationResult.error("ID thẻ không đúng định dạng: CITIZEN-CARD-yyyyMMddHHmmss");
        }
        
        return ValidationResult.success();
    }
    
    /**
     * Validate search query
     */
    public static ValidationResult validateSearchQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            return ValidationResult.error("Từ khóa tìm kiếm không được để trống");
        }
        
        String trimmedQuery = query.trim();
        
        if (trimmedQuery.length() < 2) {
            return ValidationResult.error("Từ khóa tìm kiếm phải có ít nhất 2 ký tự");
        }
        
        if (trimmedQuery.length() > 50) {
            return ValidationResult.error("Từ khóa tìm kiếm không được vượt quá 50 ký tự");
        }
        
        // Check for SQL injection patterns
        String lowerQuery = trimmedQuery.toLowerCase();
        String[] sqlKeywords = {"select", "insert", "update", "delete", "drop", "union", "script"};
        for (String keyword : sqlKeywords) {
            if (lowerQuery.contains(keyword)) {
                return ValidationResult.error("Từ khóa tìm kiếm chứa ký tự không được phép");
            }
        }
        
        return ValidationResult.success();
    }
    
    /**
     * Parse date from string with multiple formats
     */
    private static LocalDate parseDate(String dateStr) {
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(dateStr, formatter);
            } catch (DateTimeParseException e) {
                // Try next format
            }
        }
        return null;
    }
    
    /**
     * Check if PIN is weak
     */
    private static boolean isWeakPin(String pin) {
        String[] weakPins = {"0000", "1111", "2222", "3333", "4444", "5555", 
                            "6666", "7777", "8888", "9999", "1234", "4321", 
                            "0123", "3210", "1122", "2211"};
        
        for (String weakPin : weakPins) {
            if (pin.equals(weakPin)) {
                return true;
            }
        }
        
        // Check for sequential numbers
        boolean ascending = true;
        boolean descending = true;
        
        for (int i = 1; i < pin.length(); i++) {
            int current = Character.getNumericValue(pin.charAt(i));
            int previous = Character.getNumericValue(pin.charAt(i - 1));
            
            if (current != previous + 1) ascending = false;
            if (current != previous - 1) descending = false;
        }
        
        return ascending || descending;
    }
    
    /**
     * Sanitize input string
     */
    public static String sanitizeInput(String input) {
        if (input == null) return "";
        
        return input.trim()
                   .replaceAll("\\s+", " ")  // Replace multiple spaces with single space
                   .replaceAll("[<>\"'&]", ""); // Remove potentially dangerous characters
    }
    
    /**
     * Format phone number for display
     */
    public static String formatPhoneNumber(String phone) {
        if (phone == null || phone.length() != 10) return phone;
        
        return phone.substring(0, 4) + " " + 
               phone.substring(4, 7) + " " + 
               phone.substring(7);
    }
    
    /**
     * Format balance for display
     */
    public static String formatBalance(long balance) {
        return String.format("%,d VND", balance);
    }
    
    /**
     * Parse balance from formatted string
     */
    public static long parseBalance(String balanceStr) {
        if (balanceStr == null) return 0;
        
        String cleanBalance = balanceStr.replaceAll("[^0-9]", "");
        try {
            return Long.parseLong(cleanBalance);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    /**
     * Validate image file
     */
    public static ValidationResult validateImageFile(java.io.File imageFile) {
        if (imageFile == null) {
            return ValidationResult.error("Chưa chọn file ảnh");
        }
        
        if (!imageFile.exists()) {
            return ValidationResult.error("File ảnh không tồn tại");
        }
        
        // Check file size (max 5MB)
        long maxSize = 5 * 1024 * 1024; // 5MB
        if (imageFile.length() > maxSize) {
            return ValidationResult.error("Kích thước ảnh không được vượt quá 5MB");
        }
        
        if (imageFile.length() == 0) {
            return ValidationResult.error("File ảnh không hợp lệ (kích thước 0 byte)");
        }
        
        // Check file extension
        String fileName = imageFile.getName().toLowerCase();
        String[] allowedExtensions = {".jpg", ".jpeg", ".png", ".bmp", ".gif", ".webp"};
        
        boolean validExtension = false;
        for (String ext : allowedExtensions) {
            if (fileName.endsWith(ext)) {
                validExtension = true;
                break;
            }
        }
        
        if (!validExtension) {
            return ValidationResult.error("Định dạng ảnh không được hỗ trợ. Chỉ chấp nhận: JPG, PNG, BMP, GIF, WebP");
        }
        
        return ValidationResult.success();
    }
    
    /**
     * Validate image dimensions and content
     */
    public static ValidationResult validateImageContent(javafx.scene.image.Image image) {
        if (image == null) {
            return ValidationResult.error("Không thể đọc file ảnh");
        }
        
        if (image.isError()) {
            return ValidationResult.error("File ảnh bị lỗi hoặc không hợp lệ");
        }
        
        // Check minimum dimensions
        double minWidth = 100;
        double minHeight = 100;
        
        if (image.getWidth() < minWidth || image.getHeight() < minHeight) {
            return ValidationResult.error("Kích thước ảnh tối thiểu: 100x100 pixels");
        }
        
        // Check maximum dimensions
        double maxWidth = 2000;
        double maxHeight = 2000;
        
        if (image.getWidth() > maxWidth || image.getHeight() > maxHeight) {
            return ValidationResult.error("Kích thước ảnh tối đa: 2000x2000 pixels");
        }
        
        return ValidationResult.success();
    }
    
    /**
     * Get recommended image dimensions for display
     */
    public static String getImageDimensionsInfo(javafx.scene.image.Image image) {
        if (image == null) return "Không xác định";
        
        return String.format("%.0f x %.0f pixels", image.getWidth(), image.getHeight());
    }
    
    /**
     * Format file size for display
     */
    public static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
}