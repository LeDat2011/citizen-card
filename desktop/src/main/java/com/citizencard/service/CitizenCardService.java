package com.citizencard.service;

import com.citizencard.card.CardService;
import com.citizencard.card.RealCardClient;
import com.citizencard.dao.ParkingDAO;
import com.citizencard.dao.ResidentDAO;
import com.citizencard.dao.TransactionDAO;
import com.citizencard.model.Parking;
import com.citizencard.model.Resident;
import com.citizencard.model.Transaction;
import com.citizencard.validation.ValidationService;
import com.citizencard.validation.ValidationService.ValidationResult;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Base64;
import java.util.List;

public class CitizenCardService {
    private final CardService cardService;
    private final ResidentDAO residentDAO;
    private final TransactionDAO transactionDAO;
    private final ParkingDAO parkingDAO;

    public CitizenCardService() {
        RealCardClient client = new RealCardClient();
        this.cardService = new CardService(client);
        this.residentDAO = new ResidentDAO();
        this.transactionDAO = new TransactionDAO();
        this.parkingDAO = new ParkingDAO();
    }

    public boolean selectAppletOnce() throws Exception {
        try {
            return cardService.selectApplet();
        } catch (IOException e) {
            throw new Exception("Error selecting applet: " + e.getMessage(), e);
        }
    }

    public Resident initializeCard(String cardId, String fullName, String dateOfBirth,
                                   String roomNumber, String phoneNumber, String email,
                                   String idNumber, String pin) throws Exception {
        try {
            cardService.selectApplet();

            ValidationResult validation = ValidationService.validateResidentForInit(
                    cardId, fullName, dateOfBirth, roomNumber, phoneNumber, email, idNumber, pin);
            if (!validation.isValid()) {
                throw new Exception(validation.getMessage());
            }

            if (cardService.checkCardCreated()) {
                throw new Exception("Card already initialized. Vui lòng xóa thẻ trước khi khởi tạo mới.");
            }

            if (!cardService.updateCardId(cardId)) {
                throw new Exception("Failed to update card ID");
            }

            String customerInfo = buildCustomerInfo(fullName, dateOfBirth, roomNumber, phoneNumber, email, idNumber);
            if (!cardService.updateCustomerInfo(customerInfo)) {
                throw new Exception("Failed to update customer info");
            }

            if (!cardService.updatePin(pin)) {
                throw new Exception("Failed to update PIN");
            }

            if (!cardService.updateBalance(0)) {
                throw new Exception("Failed to initialize balance");
            }

            try {
                Resident duplicated = residentDAO.findByCardId(cardId);
                if (duplicated != null) {
                    throw new Exception("Card ID này đã tồn tại trong hệ thống");
                }
            } catch (SQLException e) {
                throw new Exception("Error checking card ID: " + e.getMessage(), e);
            }

            Resident resident = new Resident();
            resident.setCardId(cardId);
            resident.setFullName(fullName);
            resident.setDateOfBirth(dateOfBirth);
            resident.setRoomNumber(roomNumber);
            resident.setPhoneNumber(phoneNumber);
            resident.setEmail(email);
            resident.setIdNumber(idNumber);
            resident.setBalance(0);
            resident.setStatus("ACTIVE");
            resident.setPinHash(pin);

            try {
                Integer id = residentDAO.insert(resident);
                resident.setId(id);
            } catch (SQLException e) {
                throw new Exception("Error saving resident: " + e.getMessage(), e);
            }

            return resident;
        } catch (IOException e) {
            throw new Exception("Error initializing card: " + e.getMessage(), e);
        }
    }

    public boolean clearCard(String cardId) throws Exception {
        try {
            cardService.selectApplet();

            if (!cardService.clearCard()) {
                throw new Exception("Failed to clear card");
            }

            try {
                Resident resident = residentDAO.findByCardId(cardId);
                if (resident != null) {
                    residentDAO.delete(resident.getId());
                }
            } catch (SQLException e) {
                throw new Exception("Error deleting resident: " + e.getMessage(), e);
            }

            return true;
        } catch (IOException e) {
            throw new Exception("Error clearing card: " + e.getMessage(), e);
        }
    }

    public boolean checkCardCreated() throws Exception {
        try {
            cardService.selectApplet();
            return cardService.checkCardCreated();
        } catch (IOException e) {
            throw new Exception("Error checking card: " + e.getMessage(), e);
        }
    }

    public Resident loginByCard() throws Exception {
        try {
            cardService.selectApplet();
            String cardId = cardService.getCardId();
            if (cardId == null || cardId.isEmpty()) {
                throw new Exception("Card not initialized");
            }
            return requireResidentByCardId(cardId);
        } catch (IOException e) {
            throw new Exception("Error reading card: " + e.getMessage(), e);
        }
    }

    public Resident loginAsAdmin() throws Exception {
        try {
            cardService.selectApplet();
            try {
                List<Resident> residents = residentDAO.findAll();
                return residents.isEmpty() ? null : residents.get(0);
            } catch (SQLException e) {
                throw new Exception("Error loading residents: " + e.getMessage(), e);
            }
        } catch (IOException e) {
            throw new Exception("Error connecting to card: " + e.getMessage(), e);
        }
    }

    public boolean isCardBlocked() throws Exception {
        try {
            cardService.selectApplet();
            return cardService.checkPinStatus();
        } catch (IOException e) {
            throw new Exception("Error checking PIN status: " + e.getMessage(), e);
        }
    }

    public CardService.PinVerificationResult verifyPin(String cardId, String pin) throws Exception {
        try {
            cardService.selectApplet();
            return cardService.verifyPin(pin);
        } catch (IOException e) {
            throw new Exception("Error verifying PIN: " + e.getMessage(), e);
        }
    }

    public boolean unblockPin() throws Exception {
        try {
            cardService.selectApplet();
            return cardService.unblockPin();
        } catch (IOException e) {
            throw new Exception("Error unblocking PIN: " + e.getMessage(), e);
        }
    }

    public boolean changePin(String cardId, String oldPin, String newPin) throws Exception {
        if (oldPin.equals(newPin)) {
            throw new Exception("Mã PIN mới phải khác mã PIN cũ");
        }

        CardService.PinVerificationResult result = verifyPin(cardId, oldPin);
        if (!result.isValid()) {
            throw new Exception("PIN cũ không đúng");
        }

        try {
            cardService.selectApplet();
            if (!cardService.updatePin(newPin)) {
                throw new Exception("Failed to update PIN on card");
            }
        } catch (IOException e) {
            throw new Exception("Error updating PIN on card: " + e.getMessage(), e);
        }

        Resident resident = requireResidentByCardId(cardId);
        resident.setPinHash(newPin);
        updateResident(resident);
        return true;
    }

    public Transaction topUp(String cardId, int amount, String pin) throws Exception {
        if (amount <= 0) {
            throw new Exception("Số tiền phải lớn hơn 0");
        }

        CardService.PinVerificationResult pinResult = verifyPin(cardId, pin);
        if (!pinResult.isValid()) {
            throw new Exception("PIN không đúng. Còn " + pinResult.getTriesRemaining() + " lần thử.");
        }

        Resident resident = requireResidentByCardId(cardId);

        try {
            cardService.selectApplet();
            int balanceBefore = cardService.getBalance();
            int balanceAfter = balanceBefore + amount;

            if (!cardService.updateBalance(balanceAfter)) {
                throw new Exception("Failed to update balance on card");
            }

            resident.setBalance(balanceAfter);
            updateResident(resident);

            Transaction transaction = new Transaction(resident.getId(), cardId, "TOPUP",
                    amount, balanceBefore, balanceAfter, "Nạp tiền: " + amount + " VND");
            transactionDAO.insert(transaction);
            return transaction;
        } catch (IOException e) {
            throw new Exception("Error topping up: " + e.getMessage(), e);
        } catch (SQLException e) {
            throw new Exception("Error saving transaction: " + e.getMessage(), e);
        }
    }

    public Transaction payService(String cardId, int amount, String description, String pin) throws Exception {
        if (amount <= 0) {
            throw new Exception("Số tiền phải lớn hơn 0");
        }

        CardService.PinVerificationResult pinResult = verifyPin(cardId, pin);
        if (!pinResult.isValid()) {
            throw new Exception("PIN không đúng. Còn " + pinResult.getTriesRemaining() + " lần thử.");
        }

        Resident resident = requireResidentByCardId(cardId);

        try {
            cardService.selectApplet();
            int balanceBefore = cardService.getBalance();
            if (balanceBefore < amount) {
                throw new Exception("Insufficient balance");
            }
            int balanceAfter = balanceBefore - amount;

            if (!cardService.updateBalance(balanceAfter)) {
                throw new Exception("Failed to update balance on card");
            }

            resident.setBalance(balanceAfter);
            updateResident(resident);

            Transaction transaction = new Transaction(resident.getId(), cardId, "PAYMENT",
                    amount, balanceBefore, balanceAfter, description);
            transactionDAO.insert(transaction);
            return transaction;
        } catch (IOException e) {
            throw new Exception("Error paying service: " + e.getMessage(), e);
        } catch (SQLException e) {
            throw new Exception("Error saving transaction: " + e.getMessage(), e);
        }
    }

    public Transaction payInvoice(String cardId, Integer invoiceId, String pin) throws Exception {
        Transaction invoice;
        try {
            invoice = transactionDAO.findInvoiceById(invoiceId);
        } catch (SQLException e) {
            throw new Exception("Error loading invoice: " + e.getMessage(), e);
        }

        if (invoice == null) {
            throw new Exception("Invoice not found");
        }

        if ("PAID".equalsIgnoreCase(invoice.getPaymentStatus())) {
            throw new Exception("Invoice already paid");
        }

        String serviceName = invoice.getServiceName() != null ? invoice.getServiceName() : "Dịch vụ";
        Transaction payment = payService(cardId, invoice.getAmount(),
                "Thanh toán hóa đơn: " + serviceName, pin);

        try {
            transactionDAO.updateInvoicePaymentStatus(invoiceId, "PAID");
        } catch (SQLException e) {
            throw new Exception("Error updating invoice status: " + e.getMessage(), e);
        }

        return payment;
    }

    public int getBalance(String cardId) throws Exception {
        try {
            cardService.selectApplet();
            return cardService.getBalance();
        } catch (IOException e) {
            throw new Exception("Error getting balance: " + e.getMessage(), e);
        }
    }

    public boolean updatePicture(String cardId, byte[] pictureBytes) throws Exception {
        try {
            cardService.selectApplet();
            if (!cardService.updatePicture(pictureBytes)) {
                throw new Exception("Failed to update picture on card");
            }

            Resident resident = requireResidentByCardId(cardId);
            String base64Image = Base64.getEncoder().encodeToString(pictureBytes);
            resident.setPhotoPath(base64Image);
            updateResident(resident);
            return true;
        } catch (IOException e) {
            throw new Exception("Error updating picture: " + e.getMessage(), e);
        }
    }

    public String getPicture(String cardId) throws Exception {
        try {
            cardService.selectApplet();
            byte[] pictureBytes = cardService.getPicture();
            if (pictureBytes != null && pictureBytes.length > 0) {
                return Base64.getEncoder().encodeToString(pictureBytes);
            }

            Resident resident = requireResidentByCardId(cardId);
            return resident.getPhotoPath();
        } catch (IOException e) {
            throw new Exception("Error getting picture: " + e.getMessage(), e);
        }
    }

    public Resident getResident(Integer residentId) throws Exception {
        return requireResident(residentId);
    }

    public Resident updateResidentInfo(Integer residentId, String fullName, String dateOfBirth,
                                       String roomNumber, String phoneNumber, String email,
                                       String idNumber, String pin) throws Exception {
        Resident resident = requireResident(residentId);
        ValidationResult validation = ValidationService.validateResidentForUpdate(
                fullName, dateOfBirth, roomNumber, phoneNumber, email, idNumber);
        if (!validation.isValid()) {
            throw new Exception(validation.getMessage());
        }
        CardService.PinVerificationResult pinResult = verifyPin(resident.getCardId(), pin);
        if (!pinResult.isValid()) {
            throw new Exception("PIN không đúng. Còn " + pinResult.getTriesRemaining() + " lần thử.");
        }

        resident.setFullName(fullName);
        resident.setDateOfBirth(dateOfBirth);
        resident.setRoomNumber(roomNumber);
        resident.setPhoneNumber(phoneNumber);
        resident.setEmail(email);
        resident.setIdNumber(idNumber);
        updateResident(resident);

        try {
            String customerInfo = buildCustomerInfo(fullName, dateOfBirth, roomNumber, phoneNumber, email, idNumber);
            cardService.updateCustomerInfo(customerInfo);
        } catch (IOException e) {
            throw new Exception("Error updating card info: " + e.getMessage(), e);
        }

        return resident;
    }

    public Resident updateResidentInfoByAdmin(Integer residentId, String fullName, String dateOfBirth,
                                              String roomNumber, String phoneNumber, String email,
                                              String idNumber, String pin) throws Exception {
        Resident resident = requireResident(residentId);
        ValidationResult validation = ValidationService.validateResidentForUpdate(
                fullName, dateOfBirth, roomNumber, phoneNumber, email, idNumber);
        if (!validation.isValid()) {
            throw new Exception(validation.getMessage());
        }

        resident.setFullName(fullName);
        resident.setDateOfBirth(dateOfBirth);
        resident.setRoomNumber(roomNumber);
        resident.setPhoneNumber(phoneNumber);
        resident.setEmail(email);
        resident.setIdNumber(idNumber);
        updateResident(resident);

        try {
            if (resident.getCardId() != null) {
                String customerInfo = buildCustomerInfo(fullName, dateOfBirth, roomNumber, phoneNumber, email, idNumber);
                cardService.updateCustomerInfo(customerInfo);
            }
        } catch (IOException e) {
            throw new Exception("Error updating card info: " + e.getMessage(), e);
        }

        return resident;
    }

    public Parking registerParking(Integer residentId, String licensePlate, String vehicleType) throws Exception {
        Parking parking = new Parking(residentId, licensePlate, vehicleType, null);
        if ("CAR".equalsIgnoreCase(vehicleType)) {
            parking.setMonthlyFee(500000);
        } else if ("MOTORBIKE".equalsIgnoreCase(vehicleType)) {
            parking.setMonthlyFee(200000);
        } else {
            parking.setMonthlyFee(100000);
        }

        try {
            Integer id = parkingDAO.insert(parking);
            parking.setId(id);
            return parking;
        } catch (SQLException e) {
            throw new Exception("Error registering parking: " + e.getMessage(), e);
        }
    }

    public List<Transaction> getTransactionHistory(String cardId) throws Exception {
        Resident resident = requireResidentByCardId(cardId);
        try {
            return transactionDAO.findByResidentId(resident.getId());
        } catch (SQLException e) {
            throw new Exception("Error loading transactions: " + e.getMessage(), e);
        }
    }

    public List<Transaction> getPendingInvoices(Integer residentId) throws Exception {
        try {
            return transactionDAO.findPendingInvoicesByResidentId(residentId);
        } catch (SQLException e) {
            throw new Exception("Error loading invoices: " + e.getMessage(), e);
        }
    }

    public Transaction createInvoice(Integer residentId, String serviceName, int amount, String description) throws Exception {
        Resident resident = requireResident(residentId);

        Transaction invoice = new Transaction();
        invoice.setResidentId(residentId);
        invoice.setCardId(resident.getCardId());
        invoice.setTransactionType("INVOICE");
        invoice.setAmount(amount);
        invoice.setBalanceBefore(resident.getBalance());
        invoice.setBalanceAfter(resident.getBalance());
        invoice.setPaymentStatus("PENDING");
        invoice.setServiceName(serviceName);
        invoice.setDescription(description);

        try {
            Integer id = transactionDAO.insert(invoice);
            invoice.setId(id);
            return invoice;
        } catch (SQLException e) {
            throw new Exception("Error creating invoice: " + e.getMessage(), e);
        }
    }

    public List<Resident> getAllResidents() throws Exception {
        try {
            return residentDAO.findAll();
        } catch (SQLException e) {
            throw new Exception("Error loading residents: " + e.getMessage(), e);
        }
    }

    public List<Transaction> getAllInvoices() throws Exception {
        try {
            return transactionDAO.findAllInvoices();
        } catch (SQLException e) {
            throw new Exception("Error loading invoices: " + e.getMessage(), e);
        }
    }

    public List<Parking> getAllParking() throws Exception {
        try {
            return parkingDAO.findAll();
        } catch (SQLException e) {
            throw new Exception("Error loading parking records: " + e.getMessage(), e);
        }
    }

    public boolean changePinByAdmin(String newPin) throws Exception {
        if (newPin == null || !newPin.matches("\\d{6}")) {
            throw new Exception("PIN phải gồm 6 chữ số");
        }

        try {
            cardService.selectApplet();
            if (!cardService.updatePin(newPin)) {
                throw new Exception("Failed to update PIN");
            }

            String cardId = cardService.getCardId();
            if (cardId != null) {
                try {
                    Resident resident = residentDAO.findByCardId(cardId.trim());
                    if (resident != null) {
                        resident.setPinHash(newPin);
                        residentDAO.update(resident);
                    }
                } catch (SQLException ignored) {
                }
            }
            return true;
        } catch (IOException e) {
            throw new Exception("Error changing PIN: " + e.getMessage(), e);
        }
    }

    public void disconnect() {
        cardService.disconnect();
    }

    private Resident requireResidentByCardId(String cardId) throws Exception {
        try {
            Resident resident = residentDAO.findByCardId(cardId);
            if (resident == null) {
                throw new Exception("Resident not found for card ID: " + cardId);
            }
            return resident;
        } catch (SQLException e) {
            throw new Exception("Error loading resident: " + e.getMessage(), e);
        }
    }

    private Resident requireResident(Integer residentId) throws Exception {
        try {
            Resident resident = residentDAO.findById(residentId);
            if (resident == null) {
                throw new Exception("Resident not found");
            }
            return resident;
        } catch (SQLException e) {
            throw new Exception("Error loading resident: " + e.getMessage(), e);
        }
    }

    private void updateResident(Resident resident) throws Exception {
        try {
            residentDAO.update(resident);
        } catch (SQLException e) {
            throw new Exception("Error updating resident: " + e.getMessage(), e);
        }
    }

    private String buildCustomerInfo(String fullName, String dateOfBirth, String roomNumber,
                                     String phoneNumber, String email, String idNumber) {
        return String.join("|",
                fullName != null ? fullName : "",
                dateOfBirth != null ? dateOfBirth : "",
                roomNumber != null ? roomNumber : "",
                phoneNumber != null ? phoneNumber : "",
                email != null ? email : "",
                idNumber != null ? idNumber : "");
    }
}
