package com.citizencard.backend.validation;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

public class ValidationService {

    public static class ValidationResult {
        private boolean isValid;
        private String message;

        public ValidationResult(boolean isValid, String message) {
            this.isValid = isValid;
            this.message = message;
        }

        public boolean isValid() {
            return isValid;
        }

        public String getMessage() {
            return message;
        }
    }

    private static final Pattern CARD_ID_PATTERN = Pattern.compile("^[A-Z0-9]{6,20}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^0\\d{9}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern ID_NUMBER_PATTERN = Pattern.compile("^\\d{9,12}$");
    private static final Pattern PIN_PATTERN = Pattern.compile("^\\d{6}$");
    private static final Pattern ROOM_NUMBER_PATTERN = Pattern.compile("^[A-Z0-9]{1,10}$");

    public static ValidationResult validateCardId(String cardId) {
        if (cardId == null || cardId.trim().isEmpty()) {
            return new ValidationResult(false, "Mã thẻ không được để trống");
        }
        if (!CARD_ID_PATTERN.matcher(cardId).matches()) {
            return new ValidationResult(false, "Mã thẻ phải có 6-20 ký tự chữ in hoa và số");
        }
        return new ValidationResult(true, "");
    }

    public static ValidationResult validateFullName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return new ValidationResult(false, "Họ tên không được để trống");
        }
        if (fullName.length() < 2 || fullName.length() > 100) {
            return new ValidationResult(false, "Họ tên phải có từ 2-100 ký tự");
        }
        return new ValidationResult(true, "");
    }

    public static ValidationResult validateDateOfBirth(String dateOfBirth) {
        if (dateOfBirth == null || dateOfBirth.trim().isEmpty()) {
            return new ValidationResult(false, "Ngày sinh không được để trống");
        }
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate dob = LocalDate.parse(dateOfBirth, formatter);
            LocalDate now = LocalDate.now();
            if (dob.isAfter(now)) {
                return new ValidationResult(false, "Ngày sinh không được là ngày tương lai");
            }
            if (dob.isBefore(now.minusYears(150))) {
                return new ValidationResult(false, "Ngày sinh không hợp lệ");
            }
            return new ValidationResult(true, "");
        } catch (DateTimeParseException e) {
            return new ValidationResult(false, "Định dạng ngày sinh không hợp lệ (yyyy-MM-dd)");
        }
    }

    public static ValidationResult validateRoomNumber(String roomNumber) {
        if (roomNumber == null || roomNumber.trim().isEmpty()) {
            return new ValidationResult(false, "Số phòng không được để trống");
        }
        if (!ROOM_NUMBER_PATTERN.matcher(roomNumber).matches()) {
            return new ValidationResult(false, "Số phòng phải có 1-10 ký tự chữ in hoa và số");
        }
        return new ValidationResult(true, "");
    }

    public static ValidationResult validatePhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return new ValidationResult(true, "");
        }
        if (!PHONE_PATTERN.matcher(phoneNumber).matches()) {
            return new ValidationResult(false, "Số điện thoại phải có 10 chữ số và bắt đầu bằng 0");
        }
        return new ValidationResult(true, "");
    }

    public static ValidationResult validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return new ValidationResult(true, "");
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return new ValidationResult(false, "Email không hợp lệ");
        }
        return new ValidationResult(true, "");
    }

    public static ValidationResult validateIdNumber(String idNumber) {
        if (idNumber == null || idNumber.trim().isEmpty()) {
            return new ValidationResult(true, "");
        }
        if (!ID_NUMBER_PATTERN.matcher(idNumber).matches()) {
            return new ValidationResult(false, "CMND/CCCD phải có 9-12 chữ số");
        }
        return new ValidationResult(true, "");
    }

    public static ValidationResult validatePin(String pin) {
        if (pin == null || pin.trim().isEmpty()) {
            return new ValidationResult(false, "PIN không được để trống");
        }
        if (!PIN_PATTERN.matcher(pin).matches()) {
            return new ValidationResult(false, "PIN phải có đúng 6 chữ số");
        }
        return new ValidationResult(true, "");
    }

    public static ValidationResult validateBalance(int balance) {
        if (balance < 0) {
            return new ValidationResult(false, "Số dư không được âm");
        }
        if (balance > 1000000000) {
            return new ValidationResult(false, "Số dư không được vượt quá 1 tỷ VND");
        }
        return new ValidationResult(true, "");
    }

    public static ValidationResult validateResidentForInit(String cardId, String fullName, String dateOfBirth,
            String roomNumber, String phoneNumber, String email, String idNumber, String pin) {
        ValidationResult result;

        result = validateCardId(cardId);
        if (!result.isValid()) return result;

        result = validateFullName(fullName);
        if (!result.isValid()) return result;

        result = validateDateOfBirth(dateOfBirth);
        if (!result.isValid()) return result;

        result = validateRoomNumber(roomNumber);
        if (!result.isValid()) return result;

        result = validatePhoneNumber(phoneNumber);
        if (!result.isValid()) return result;

        result = validateEmail(email);
        if (!result.isValid()) return result;

        result = validateIdNumber(idNumber);
        if (!result.isValid()) return result;

        result = validatePin(pin);
        if (!result.isValid()) return result;

        return new ValidationResult(true, "");
    }

    public static ValidationResult validateResidentForUpdate(String fullName, String dateOfBirth,
            String roomNumber, String phoneNumber, String email, String idNumber) {
        ValidationResult result;

        result = validateFullName(fullName);
        if (!result.isValid()) return result;

        result = validateDateOfBirth(dateOfBirth);
        if (!result.isValid()) return result;

        result = validateRoomNumber(roomNumber);
        if (!result.isValid()) return result;

        result = validatePhoneNumber(phoneNumber);
        if (!result.isValid()) return result;

        result = validateEmail(email);
        if (!result.isValid()) return result;

        result = validateIdNumber(idNumber);
        if (!result.isValid()) return result;

        return new ValidationResult(true, "");
    }
}

