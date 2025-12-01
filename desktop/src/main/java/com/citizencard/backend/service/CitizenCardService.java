package com.citizencard.backend.service;

import com.citizencard.backend.CardService;
import com.citizencard.backend.RealCardClient;
import com.citizencard.backend.dao.*;
import com.citizencard.backend.model.*;
import com.citizencard.backend.security.EncryptionService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Base64;
import java.util.List;

public class CitizenCardService {
    private CardService cardService;
    private ResidentDAO residentDAO;
    private TransactionDAO transactionDAO;
    private ParkingDAO parkingDAO;
    private EncryptionService encryptionService;
    
    public CitizenCardService() {
        RealCardClient client = new RealCardClient();
        this.cardService = new CardService(client);
        this.residentDAO = new ResidentDAO();
        this.transactionDAO = new TransactionDAO();
        this.parkingDAO = new ParkingDAO();
        this.encryptionService = new EncryptionService();
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
            if (!cardService.updateCardId(cardId)) {
                throw new Exception("Failed to update card ID");
            }
            
            String customerInfo = String.join("|", fullName, dateOfBirth, roomNumber, 
                                              phoneNumber != null ? phoneNumber : "", 
                                              email != null ? email : "", 
                                              idNumber != null ? idNumber : "");
            if (!cardService.updateCustomerInfo(customerInfo)) {
                throw new Exception("Failed to update customer info");
            }
            
            if (!cardService.updatePin(pin)) {
                throw new Exception("Failed to update PIN");
            }
            
            if (!cardService.updateBalance(0)) {
                throw new Exception("Failed to initialize balance");
            }
            
            Resident resident = residentDAO.findById(1);
            
            if (resident == null) {
                resident = new Resident();
                resident.setId(1);
                resident.setCardId(encryptionService.encryptWithAES(cardId, pin));
                resident.setFullName(encryptionService.encryptWithAES(fullName, pin));
                resident.setDateOfBirth(encryptionService.encryptWithAES(dateOfBirth, pin));
                resident.setRoomNumber(encryptionService.encryptWithAES(roomNumber, pin));
                resident.setPhoneNumber(encryptionService.encryptWithAES(
                    phoneNumber != null ? phoneNumber : "", pin));
                resident.setEmail(encryptionService.encryptWithAES(
                    email != null ? email : "", pin));
                resident.setIdNumber(encryptionService.encryptWithAES(
                    idNumber != null ? idNumber : "", pin));
                resident.setPinHash(encryptionService.encryptWithAES(pin, pin));
                resident.setBalance(0);
                resident.setStatus(encryptionService.encryptWithAES("ACTIVE", pin));
                residentDAO.insert(resident);
            } else {
                resident.setCardId(encryptionService.encryptWithAES(cardId, pin));
                resident.setFullName(encryptionService.encryptWithAES(fullName, pin));
                resident.setDateOfBirth(encryptionService.encryptWithAES(dateOfBirth, pin));
                resident.setRoomNumber(encryptionService.encryptWithAES(roomNumber, pin));
                resident.setPhoneNumber(encryptionService.encryptWithAES(
                    phoneNumber != null ? phoneNumber : "", pin));
                resident.setEmail(encryptionService.encryptWithAES(
                    email != null ? email : "", pin));
                resident.setIdNumber(encryptionService.encryptWithAES(
                    idNumber != null ? idNumber : "", pin));
                resident.setPinHash(encryptionService.encryptWithAES(pin, pin));
                resident.setBalance(0);
                resident.setStatus(encryptionService.encryptWithAES("ACTIVE", pin));
                residentDAO.update(resident);
            }
            
            return resident;
            
        } catch (IOException | SQLException e) {
            throw new Exception("Error initializing card: " + e.getMessage(), e);
        }
    }
    
    public boolean clearCard(String cardId) throws Exception {
        try {
            if (!cardService.clearCard()) {
                throw new Exception("Failed to clear card");
            }
            
            Resident resident = residentDAO.findByCardId(cardId);
            if (resident != null) {
                residentDAO.delete(resident.getId());
            }
            
            return true;
        } catch (IOException | SQLException e) {
            throw new Exception("Error clearing card: " + e.getMessage(), e);
        }
    }
    
    public boolean checkCardCreated() throws Exception {
        try {
            return cardService.checkCardCreated();
        } catch (IOException e) {
            throw new Exception("Error checking card: " + e.getMessage(), e);
        }
    }
    
    public Resident loginByCard() throws Exception {
        try {
            Resident resident = residentDAO.findById(1);
            if (resident == null) {
                resident = new Resident();
                resident.setId(1);
                resident.setCardId("CARD001");
                resident.setFullName("Nguyễn Văn A");
                resident.setDateOfBirth("1990-01-01");
                resident.setRoomNumber("101");
                resident.setPhoneNumber("0901234567");
                resident.setEmail("nguyenvana@example.com");
                resident.setIdNumber("001234567890");
                resident.setBalance(0);
                resident.setPinHash("123456");
                residentDAO.insert(resident);
            }
            
            return resident;
        } catch (SQLException e) {
            throw new Exception("Error accessing database: " + e.getMessage(), e);
        }
    }
    
    public Resident loginAsAdmin() throws Exception {
        try {
            Resident resident = residentDAO.findById(1);
            if (resident == null) {
                resident = new Resident();
                resident.setId(1);
                resident.setCardId("CARD001");
                resident.setFullName("Nguyễn Văn A");
                resident.setDateOfBirth("1990-01-01");
                resident.setRoomNumber("101");
                resident.setPhoneNumber("0901234567");
                resident.setEmail("nguyenvana@example.com");
                resident.setIdNumber("001234567890");
                resident.setBalance(0);
                resident.setPinHash("123456");
                residentDAO.insert(resident);
            }
            
            return resident;
        } catch (SQLException e) {
            throw new Exception("Error accessing database: " + e.getMessage(), e);
        }
    }
    
    public boolean isCardBlocked() throws Exception {
        try {
            return cardService.checkPinStatus();
        } catch (IOException e) {
            return false;
        }
    }
    
    public CardService.PinVerificationResult verifyPin(String cardId, String pin) throws Exception {
        try {
            if (pin == null || pin.isEmpty()) {
                return new CardService.PinVerificationResult(false, (byte) 0, false);
            }
            return cardService.verifyPin(pin);
        } catch (IOException e) {
            throw new Exception("Error verifying PIN from card: " + e.getMessage(), e);
        }
    }
    
    public boolean unblockPin() throws Exception {
        try {
            return cardService.unblockPin();
        } catch (IOException e) {
            throw new Exception("Error unblocking PIN: " + e.getMessage(), e);
        }
    }
    
    public boolean changePin(String cardId, String oldPin, String newPin) throws Exception {
        try {
            CardService.PinVerificationResult result = verifyPin(cardId, oldPin);
            if (!result.isValid()) {
                throw new Exception("PIN cũ không đúng");
            }
            
            if (!cardService.updatePin(newPin)) {
                throw new Exception("Failed to update PIN on card");
            }
            
            Resident resident = residentDAO.findById(1);
            if (resident == null) {
                throw new Exception("Resident not found");
            }
            
            resident.setPinHash(newPin);
            residentDAO.update(resident);
            
            return true;
        } catch (SQLException e) {
            throw new Exception("Error changing PIN: " + e.getMessage(), e);
        }
    }
    
    public Transaction topUp(String cardId, int amount, String pin) throws Exception {
        try {
            CardService.PinVerificationResult pinResult = verifyPin(cardId, pin);
            if (!pinResult.isValid()) {
                throw new Exception("PIN không đúng. Còn " + pinResult.getTriesRemaining() + " lần thử.");
            }
            
            Resident resident = residentDAO.findById(1);
            if (resident == null) {
                throw new Exception("Resident not found");
            }
            
            int balanceBefore = resident.getBalance();
            int balanceAfter = balanceBefore + amount;
            
            if (!cardService.updateBalance(balanceAfter)) {
                throw new Exception("Failed to update balance on card");
            }
            
            resident.setBalance(balanceAfter);
            residentDAO.update(resident);
            
            Transaction transaction = new Transaction();
            transaction.setResidentId(resident.getId());
            transaction.setCardId(encryptionService.encryptWithAES(cardId, pin));
            transaction.setTransactionType(encryptionService.encryptWithAES("TOPUP", pin));
            transaction.setAmount(amount);
            transaction.setBalanceBefore(balanceBefore);
            transaction.setBalanceAfter(balanceAfter);
            transaction.setDescription(encryptionService.encryptWithAES(
                "Nạp tiền: " + amount + " VND", pin));
            transactionDAO.insert(transaction);
            
            return transaction;
        } catch (IOException | SQLException e) {
            throw new Exception("Error topping up: " + e.getMessage(), e);
        }
    }
    
    public Transaction payService(String cardId, int amount, String description, String pin) throws Exception {
        try {
            CardService.PinVerificationResult pinResult = verifyPin(cardId, pin);
            if (!pinResult.isValid()) {
                throw new Exception("PIN không đúng. Còn " + pinResult.getTriesRemaining() + " lần thử.");
            }
            
            Resident resident = residentDAO.findById(1);
            if (resident == null) {
                throw new Exception("Resident not found");
            }
            
            int balanceBefore = resident.getBalance();
            if (balanceBefore < amount) {
                throw new Exception("Insufficient balance");
            }
            
            int balanceAfter = balanceBefore - amount;
            
            if (!cardService.updateBalance(balanceAfter)) {
                throw new Exception("Failed to update balance on card");
            }
            
            resident.setBalance(balanceAfter);
            residentDAO.update(resident);
            
            Transaction transaction = new Transaction();
            transaction.setResidentId(resident.getId());
            transaction.setCardId(encryptionService.encryptWithAES(cardId, pin));
            transaction.setTransactionType(encryptionService.encryptWithAES("PAYMENT", pin));
            transaction.setAmount(amount);
            transaction.setBalanceBefore(balanceBefore);
            transaction.setBalanceAfter(balanceAfter);
            transaction.setDescription(encryptionService.encryptWithAES(description, pin));
            transactionDAO.insert(transaction);
            
            return transaction;
        } catch (IOException | SQLException e) {
            throw new Exception("Error paying service: " + e.getMessage(), e);
        }
    }
    
    public Transaction payInvoice(String cardId, Integer invoiceId, String pin) throws Exception {
        try {
            Transaction invoice = transactionDAO.findInvoiceById(invoiceId);
            if (invoice == null) {
                throw new Exception("Invoice not found");
            }
            
            String invoicePaymentStatus = invoice.getPaymentStatus();
            String serviceName = invoice.getServiceName();
            try {
                String decryptedPin = "123456";
                Resident resident = residentDAO.findById(1);
                if (resident != null && resident.getPinHash() != null) {
                    try {
                        decryptedPin = encryptionService.decryptWithAES(resident.getPinHash(), "123456");
                    } catch (Exception e) {
                        decryptedPin = "123456";
                    }
                }
                if (invoicePaymentStatus != null && !invoicePaymentStatus.isEmpty()) {
                    invoicePaymentStatus = encryptionService.decryptWithAES(invoice.getPaymentStatus(), decryptedPin);
                }
                if (serviceName != null && !serviceName.isEmpty()) {
                    serviceName = encryptionService.decryptWithAES(invoice.getServiceName(), decryptedPin);
                }
            } catch (Exception e) {
            }
            
            if ("PAID".equals(invoicePaymentStatus)) {
                throw new Exception("Invoice already paid");
            }
            
            Transaction transaction = payService(cardId, invoice.getAmount(), 
                                                "Thanh toán hóa đơn: " + serviceName, pin);
            
            String pinForUpdate = "123456";
            try {
                Resident resident = residentDAO.findById(1);
                if (resident != null && resident.getPinHash() != null) {
                    try {
                        pinForUpdate = encryptionService.decryptWithAES(resident.getPinHash(), "123456");
                    } catch (Exception e) {
                        pinForUpdate = "123456";
                    }
                }
            } catch (Exception e) {
            }
            transactionDAO.updateInvoicePaymentStatus(invoiceId, encryptionService.encryptWithAES("PAID", pinForUpdate));
            
            return transaction;
        } catch (SQLException e) {
            throw new Exception("Error paying invoice: " + e.getMessage(), e);
        }
    }
    
    public int getBalance(String cardId) throws Exception {
        try {
            return cardService.getBalance();
        } catch (IOException e) {
            throw new Exception("Error getting balance: " + e.getMessage(), e);
        }
    }
    
    public boolean updatePicture(String cardId, byte[] pictureBytes) throws Exception {
        try {
            if (!cardService.updatePicture(pictureBytes)) {
                throw new Exception("Failed to update picture on card");
            }
            
            String base64Image = Base64.getEncoder().encodeToString(pictureBytes);
            Resident resident = residentDAO.findById(1);
            if (resident != null) {
                String pin = "123456";
                try {
                    pin = encryptionService.decryptWithAES(resident.getPinHash(), "123456");
                } catch (Exception e) {
                    pin = "123456";
                }
                resident.setPhotoPath(encryptionService.encryptWithAES(base64Image, pin));
                residentDAO.update(resident);
            }
            
            return true;
        } catch (IOException | SQLException e) {
            throw new Exception("Error updating picture: " + e.getMessage(), e);
        }
    }
    
    public String getPicture(String cardId) throws Exception {
        try {
            byte[] pictureBytes = cardService.getPicture();
            if (pictureBytes == null || pictureBytes.length == 0) {
                Resident resident = residentDAO.findById(1);
                if (resident != null && resident.getPhotoPath() != null) {
                    try {
                        String pin = "123456";
                        try {
                            pin = encryptionService.decryptWithAES(resident.getPinHash(), "123456");
                        } catch (Exception e) {
                            pin = "123456";
                        }
                        return encryptionService.decryptWithAES(resident.getPhotoPath(), pin);
                    } catch (Exception e) {
                        return resident.getPhotoPath();
                    }
                }
                return null;
            }
            
            return Base64.getEncoder().encodeToString(pictureBytes);
        } catch (IOException | SQLException e) {
            throw new Exception("Error getting picture: " + e.getMessage(), e);
        }
    }
    
    public Resident updateResidentInfo(Integer residentId, String fullName, String dateOfBirth, 
                                      String roomNumber, String phoneNumber, String email, 
                                      String idNumber, String pin) throws Exception {
        try {
            Resident resident = residentDAO.findById(residentId);
            if (resident == null) {
                throw new Exception("Resident not found");
            }
            
            String decryptedCardId = resident.getCardId();
            try {
                String tempPin = "123456";
                try {
                    tempPin = encryptionService.decryptWithAES(resident.getPinHash(), "123456");
                } catch (Exception e) {
                    tempPin = "123456";
                }
                decryptedCardId = encryptionService.decryptWithAES(resident.getCardId(), tempPin);
            } catch (Exception e) {
            }
            
            CardService.PinVerificationResult pinResult = verifyPin(decryptedCardId, pin);
            if (!pinResult.isValid()) {
                throw new Exception("PIN không đúng. Còn " + pinResult.getTriesRemaining() + " lần thử.");
            }
            
            resident.setCardId(encryptionService.encryptWithAES(decryptedCardId, pin));
            resident.setFullName(encryptionService.encryptWithAES(fullName, pin));
            resident.setDateOfBirth(encryptionService.encryptWithAES(dateOfBirth, pin));
            resident.setRoomNumber(encryptionService.encryptWithAES(roomNumber, pin));
            resident.setPhoneNumber(encryptionService.encryptWithAES(
                phoneNumber != null ? phoneNumber : "", pin));
            resident.setEmail(encryptionService.encryptWithAES(
                email != null ? email : "", pin));
            resident.setIdNumber(encryptionService.encryptWithAES(
                idNumber != null ? idNumber : "", pin));
            resident.setStatus(encryptionService.encryptWithAES("ACTIVE", pin));
            
            residentDAO.update(resident);
            
            if (resident.getCardId() != null) {
                String customerInfo = String.join("|", fullName, dateOfBirth, roomNumber, 
                                                  phoneNumber != null ? phoneNumber : "", 
                                                  email != null ? email : "", 
                                                  idNumber != null ? idNumber : "");
                cardService.updateCustomerInfo(customerInfo);
            }
            
            return resident;
        } catch (SQLException | IOException e) {
            throw new Exception("Error updating resident info: " + e.getMessage(), e);
        }
    }
    
    public Resident updateResidentInfoByAdmin(Integer residentId, String fullName, String dateOfBirth, 
                                             String roomNumber, String phoneNumber, String email, 
                                             String idNumber, String pin) throws Exception {
        try {
            Resident resident = residentDAO.findById(residentId);
            if (resident == null) {
                throw new Exception("Resident not found");
            }
            
            if (pin == null || pin.isEmpty()) {
                try {
                    pin = encryptionService.decryptWithAES(resident.getPinHash(), "123456");
                } catch (Exception e) {
                    pin = "123456";
                }
            }
            
            resident.setCardId(encryptionService.encryptWithAES(
                resident.getCardId() != null ? encryptionService.decryptWithAES(resident.getCardId(), pin) : "", pin));
            resident.setFullName(encryptionService.encryptWithAES(fullName, pin));
            resident.setDateOfBirth(encryptionService.encryptWithAES(dateOfBirth, pin));
            resident.setRoomNumber(encryptionService.encryptWithAES(roomNumber, pin));
            resident.setPhoneNumber(encryptionService.encryptWithAES(
                phoneNumber != null ? phoneNumber : "", pin));
            resident.setEmail(encryptionService.encryptWithAES(
                email != null ? email : "", pin));
            resident.setIdNumber(encryptionService.encryptWithAES(
                idNumber != null ? idNumber : "", pin));
            resident.setStatus(encryptionService.encryptWithAES("ACTIVE", pin));
            
            residentDAO.update(resident);
            
            if (resident.getCardId() != null) {
                String customerInfo = String.join("|", fullName, dateOfBirth, roomNumber, 
                                                  phoneNumber != null ? phoneNumber : "", 
                                                  email != null ? email : "", 
                                                  idNumber != null ? idNumber : "");
                cardService.updateCustomerInfo(customerInfo);
            }
            
            return resident;
        } catch (SQLException | IOException e) {
            throw new Exception("Error updating resident info: " + e.getMessage(), e);
        }
    }
    
    public Resident getResidentDecrypted(Integer residentId, String pin) throws Exception {
        try {
            Resident resident = residentDAO.findById(residentId);
            if (resident == null) {
                return null;
            }
            
            try {
                if (resident.getCardId() != null && !resident.getCardId().isEmpty()) {
                    resident.setCardId(encryptionService.decryptWithAES(resident.getCardId(), pin));
                }
            } catch (Exception e) {
                resident.setCardId("");
            }
            
            try {
                if (resident.getFullName() != null && !resident.getFullName().isEmpty()) {
                    resident.setFullName(encryptionService.decryptWithAES(resident.getFullName(), pin));
                }
            } catch (Exception e) {
                resident.setFullName("");
            }
            
            try {
                if (resident.getDateOfBirth() != null && !resident.getDateOfBirth().isEmpty()) {
                    resident.setDateOfBirth(encryptionService.decryptWithAES(resident.getDateOfBirth(), pin));
                }
            } catch (Exception e) {
                resident.setDateOfBirth("");
            }
            
            try {
                if (resident.getRoomNumber() != null && !resident.getRoomNumber().isEmpty()) {
                    resident.setRoomNumber(encryptionService.decryptWithAES(resident.getRoomNumber(), pin));
                }
            } catch (Exception e) {
                resident.setRoomNumber("");
            }
            
            try {
                if (resident.getPhoneNumber() != null && !resident.getPhoneNumber().isEmpty()) {
                    resident.setPhoneNumber(encryptionService.decryptWithAES(resident.getPhoneNumber(), pin));
                }
            } catch (Exception e) {
                resident.setPhoneNumber("");
            }
            
            try {
                if (resident.getEmail() != null && !resident.getEmail().isEmpty()) {
                    resident.setEmail(encryptionService.decryptWithAES(resident.getEmail(), pin));
                }
            } catch (Exception e) {
                resident.setEmail("");
            }
            
            try {
                if (resident.getIdNumber() != null && !resident.getIdNumber().isEmpty()) {
                    resident.setIdNumber(encryptionService.decryptWithAES(resident.getIdNumber(), pin));
                }
            } catch (Exception e) {
                resident.setIdNumber("");
            }
            
            try {
                if (resident.getPhotoPath() != null && !resident.getPhotoPath().isEmpty()) {
                    resident.setPhotoPath(encryptionService.decryptWithAES(resident.getPhotoPath(), pin));
                }
            } catch (Exception e) {
                resident.setPhotoPath("");
            }
            
            try {
                if (resident.getStatus() != null && !resident.getStatus().isEmpty()) {
                    resident.setStatus(encryptionService.decryptWithAES(resident.getStatus(), pin));
                }
            } catch (Exception e) {
                resident.setStatus("ACTIVE");
            }
            
            return resident;
        } catch (SQLException e) {
            throw new Exception("Error getting resident: " + e.getMessage(), e);
        }
    }
    
    public List<Transaction> getTransactionHistoryDecrypted(String cardId, String pin) throws Exception {
        try {
            List<Transaction> transactions = transactionDAO.findByResidentId(1);
            for (Transaction t : transactions) {
                try {
                    if (t.getCardId() != null && !t.getCardId().isEmpty()) {
                        t.setCardId(encryptionService.decryptWithAES(t.getCardId(), pin));
                    }
                } catch (Exception e) {
                    t.setCardId("");
                }
                
                try {
                    if (t.getTransactionType() != null && !t.getTransactionType().isEmpty()) {
                        t.setTransactionType(encryptionService.decryptWithAES(t.getTransactionType(), pin));
                    }
                } catch (Exception e) {
                    t.setTransactionType("");
                }
                
                try {
                    if (t.getDescription() != null && !t.getDescription().isEmpty()) {
                        t.setDescription(encryptionService.decryptWithAES(t.getDescription(), pin));
                    }
                } catch (Exception e) {
                    t.setDescription("");
                }
                
                try {
                    if (t.getServiceName() != null && !t.getServiceName().isEmpty()) {
                        t.setServiceName(encryptionService.decryptWithAES(t.getServiceName(), pin));
                    }
                } catch (Exception e) {
                    t.setServiceName("");
                }
                
                try {
                    if (t.getPaymentStatus() != null && !t.getPaymentStatus().isEmpty()) {
                        t.setPaymentStatus(encryptionService.decryptWithAES(t.getPaymentStatus(), pin));
                    }
                } catch (Exception e) {
                    t.setPaymentStatus("");
                }
            }
            return transactions;
        } catch (SQLException e) {
            throw new Exception("Error getting transaction history: " + e.getMessage(), e);
        }
    }
    
    public String decryptTransactionDescription(String encryptedDescription, String pin) throws Exception {
        try {
            return encryptionService.decryptWithAES(encryptedDescription, pin);
        } catch (Exception e) {
            return encryptedDescription;
        }
    }
    
    public Parking registerParking(Integer residentId, String licensePlate, String vehicleType) throws Exception {
        try {
            Parking parking = new Parking(residentId, licensePlate, vehicleType, null);
            
            if ("CAR".equals(vehicleType)) {
                parking.setMonthlyFee(500000);
            } else if ("MOTORBIKE".equals(vehicleType)) {
                parking.setMonthlyFee(200000);
            } else {
                parking.setMonthlyFee(100000);
            }
            
            Integer id = parkingDAO.insert(parking);
            parking.setId(id);
            
            return parking;
        } catch (SQLException e) {
            throw new Exception("Error registering parking: " + e.getMessage(), e);
        }
    }
    
    public List<Transaction> getTransactionHistory(String cardId) throws Exception {
        try {
            return transactionDAO.findByResidentId(1);
        } catch (SQLException e) {
            throw new Exception("Error getting transaction history: " + e.getMessage(), e);
        }
    }
    
    public List<Transaction> getPendingInvoices(Integer residentId) throws Exception {
        try {
            return transactionDAO.findPendingInvoicesByResidentId(1);
        } catch (SQLException e) {
            throw new Exception("Error getting pending invoices: " + e.getMessage(), e);
        }
    }
    
    public List<Transaction> getPendingInvoicesDecrypted(Integer residentId, String pin) throws Exception {
        try {
            List<Transaction> invoices = transactionDAO.findPendingInvoicesByResidentId(1);
            for (Transaction invoice : invoices) {
                try {
                    if (invoice.getCardId() != null && !invoice.getCardId().isEmpty()) {
                        invoice.setCardId(encryptionService.decryptWithAES(invoice.getCardId(), pin));
                    }
                } catch (Exception e) {
                    invoice.setCardId("");
                }
                
                try {
                    if (invoice.getTransactionType() != null && !invoice.getTransactionType().isEmpty()) {
                        invoice.setTransactionType(encryptionService.decryptWithAES(invoice.getTransactionType(), pin));
                    }
                } catch (Exception e) {
                    invoice.setTransactionType("");
                }
                
                try {
                    if (invoice.getServiceName() != null && !invoice.getServiceName().isEmpty()) {
                        invoice.setServiceName(encryptionService.decryptWithAES(invoice.getServiceName(), pin));
                    }
                } catch (Exception e) {
                    invoice.setServiceName("");
                }
                
                try {
                    if (invoice.getPaymentStatus() != null && !invoice.getPaymentStatus().isEmpty()) {
                        invoice.setPaymentStatus(encryptionService.decryptWithAES(invoice.getPaymentStatus(), pin));
                    }
                } catch (Exception e) {
                    invoice.setPaymentStatus("");
                }
                
                try {
                    if (invoice.getDescription() != null && !invoice.getDescription().isEmpty()) {
                        invoice.setDescription(encryptionService.decryptWithAES(invoice.getDescription(), pin));
                    }
                } catch (Exception e) {
                    invoice.setDescription("");
                }
            }
            return invoices;
        } catch (SQLException e) {
            throw new Exception("Error getting pending invoices: " + e.getMessage(), e);
        }
    }
    
    public Transaction createInvoice(Integer residentId, String serviceName, int amount, String description) throws Exception {
        try {
            Resident resident = residentDAO.findById(1);
            if (resident == null) {
                throw new Exception("Resident not found");
            }
            
            String pin = "123456";
            try {
                pin = encryptionService.decryptWithAES(resident.getPinHash(), "123456");
            } catch (Exception e) {
                pin = "123456";
            }
            
            Transaction invoice = new Transaction();
            invoice.setResidentId(1);
            invoice.setCardId(encryptionService.encryptWithAES(
                resident.getCardId() != null ? encryptionService.decryptWithAES(resident.getCardId(), pin) : "", pin));
            invoice.setTransactionType(encryptionService.encryptWithAES("INVOICE", pin));
            invoice.setAmount(amount);
            invoice.setBalanceAfter(resident.getBalance());
            invoice.setPaymentStatus(encryptionService.encryptWithAES("PENDING", pin));
            invoice.setServiceName(encryptionService.encryptWithAES(serviceName, pin));
            invoice.setDescription(encryptionService.encryptWithAES(description, pin));
            
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
            throw new Exception("Error getting residents: " + e.getMessage(), e);
        }
    }
    
    public List<Transaction> getAllInvoices() throws Exception {
        try {
            return transactionDAO.findAllInvoices();
        } catch (SQLException e) {
            throw new Exception("Error getting all invoices: " + e.getMessage(), e);
        }
    }
    
    public List<Parking> getAllParking() throws Exception {
        try {
            return parkingDAO.findAll();
        } catch (SQLException e) {
            throw new Exception("Error getting all parking records: " + e.getMessage(), e);
        }
    }
    
    public boolean changePinByAdmin(String newPin) throws Exception {
        try {
            if (newPin == null || newPin.length() != 6 || !newPin.matches("\\d{6}")) {
                throw new Exception("PIN phải là 6 chữ số");
            }
            
            if (!cardService.updatePin(newPin)) {
                throw new Exception("Failed to update PIN");
            }
            
            return true;
        } catch (IOException e) {
            throw new Exception("Error changing PIN: " + e.getMessage(), e);
        }
    }
    
    public void disconnect() {
        cardService.disconnect();
    }
}

