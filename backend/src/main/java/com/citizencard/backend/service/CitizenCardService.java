package com.citizencard.backend.service;

import com.citizencard.backend.CardService;
import com.citizencard.backend.RealCardClient;
import com.citizencard.backend.dao.*;
import com.citizencard.backend.model.*;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;

/**
 * Service nghiệp vụ xử lý các chức năng chính
 * Sử dụng RealCardClient để kết nối với JCIDE terminal qua javax.smartcardio
 */
public class CitizenCardService {
    private CardService cardService;
    private ResidentDAO residentDAO;
    private TransactionDAO transactionDAO;
    private InvoiceDAO invoiceDAO;
    private ParkingDAO parkingDAO;
    
    public CitizenCardService() {
        // ✅ Sử dụng RealCardClient để kết nối với JCIDE terminal
        // Khi chạy JCIDE, nó sẽ mở terminal và chương trình sẽ quét và kết nối
        RealCardClient client = new RealCardClient();
        this.cardService = new CardService(client);
        this.residentDAO = new ResidentDAO();
        this.transactionDAO = new TransactionDAO();
        this.invoiceDAO = new InvoiceDAO();
        this.parkingDAO = new ParkingDAO();
    }
    
    /**
     * Khởi tạo thẻ mới (Admin)
     */
    public Resident initializeCard(String cardId, String fullName, String dateOfBirth, 
                                   String roomNumber, String phoneNumber, String email, 
                                   String idNumber, String pin) throws Exception {
        try {
            // SELECT applet
            if (!cardService.selectApplet()) {
                throw new Exception("Failed to select applet");
            }
            
            // Kiểm tra thẻ đã được khởi tạo chưa
            if (cardService.checkCardCreated()) {
                throw new Exception("Card already initialized");
            }
            
            // Cập nhật Card ID
            if (!cardService.updateCardId(cardId)) {
                throw new Exception("Failed to update card ID");
            }
            
            // Cập nhật thông tin khách hàng
            String customerInfo = String.join("|", fullName, dateOfBirth, roomNumber, 
                                              phoneNumber != null ? phoneNumber : "", 
                                              email != null ? email : "", 
                                              idNumber != null ? idNumber : "");
            if (!cardService.updateCustomerInfo(customerInfo)) {
                throw new Exception("Failed to update customer info");
            }
            
            // Cập nhật PIN
            if (!cardService.updatePin(pin)) {
                throw new Exception("Failed to update PIN");
            }
            
            // Khởi tạo số dư = 0
            if (!cardService.updateBalance(0)) {
                throw new Exception("Failed to initialize balance");
            }
            
            // Lưu vào database
            Resident resident = new Resident(cardId, fullName, dateOfBirth, roomNumber);
            resident.setPhoneNumber(phoneNumber);
            resident.setEmail(email);
            resident.setIdNumber(idNumber);
            resident.setPinHash(pin); // Trong thực tế nên hash PIN
            resident.setBalance(0);
            
            Integer id = residentDAO.insert(resident);
            resident.setId(id);
            
            return resident;
            
        } catch (IOException | SQLException e) {
            throw new Exception("Error initializing card: " + e.getMessage(), e);
        }
    }
    
    /**
     * Xóa thẻ (Admin)
     */
    public boolean clearCard(String cardId) throws Exception {
        try {
            if (!cardService.selectApplet()) {
                throw new Exception("Failed to select applet");
            }
            
            if (!cardService.clearCard()) {
                throw new Exception("Failed to clear card");
            }
            
            // Xóa khỏi database
            Resident resident = residentDAO.findByCardId(cardId);
            if (resident != null) {
                residentDAO.delete(resident.getId());
            }
            
            return true;
        } catch (IOException | SQLException e) {
            throw new Exception("Error clearing card: " + e.getMessage(), e);
        }
    }
    
    /**
     * Kiểm tra thẻ đã khởi tạo chưa (Admin)
     */
    public boolean checkCardCreated() throws Exception {
        try {
            if (!cardService.selectApplet()) {
                return false;
            }
            return cardService.checkCardCreated();
        } catch (IOException e) {
            throw new Exception("Error checking card: " + e.getMessage(), e);
        }
    }
    
    /**
     * Đăng nhập bằng thẻ (Resident)
     */
    public Resident loginByCard() throws Exception {
        try {
            if (!cardService.selectApplet()) {
                throw new Exception("Failed to select applet");
            }
            
            String cardId = cardService.getCardId();
            if (cardId == null || cardId.isEmpty()) {
                throw new Exception("Card not initialized");
            }
            
            Resident resident = residentDAO.findByCardId(cardId);
            if (resident == null) {
                throw new Exception("Resident not found");
            }
            
            return resident;
        } catch (IOException | SQLException e) {
            throw new Exception("Error reading card: " + e.getMessage(), e);
        }
    }
    
    /**
     * Xác thực PIN
     */
    public boolean verifyPin(String cardId, String pin) throws Exception {
        try {
            if (!cardService.selectApplet()) {
                throw new Exception("Failed to select applet");
            }
            
            // Đọc PIN từ thẻ và so sánh (trong thực tế nên hash và so sánh)
            // Ở đây đơn giản hóa: lưu PIN hash trong DB và so sánh
            Resident resident = residentDAO.findByCardId(cardId);
            if (resident == null) {
                return false;
            }
            
            // So sánh PIN (trong thực tế nên dùng hash)
            return pin.equals(resident.getPinHash());
        } catch (SQLException e) {
            throw new Exception("Error verifying PIN: " + e.getMessage(), e);
        }
    }
    
    /**
     * Đổi PIN - Ghi vào cả thẻ và database
     * @param cardId Card ID của cư dân
     * @param oldPin PIN cũ để xác thực
     * @param newPin PIN mới
     * @return true nếu thành công
     */
    public boolean changePin(String cardId, String oldPin, String newPin) throws Exception {
        try {
            // 1. Xác thực PIN cũ
            if (!verifyPin(cardId, oldPin)) {
                throw new Exception("PIN cũ không đúng");
            }
            
            // 2. Select applet
            if (!cardService.selectApplet()) {
                throw new Exception("Failed to select applet");
            }
            
            // 3. Ghi PIN mới vào THẺ (qua JCIDE terminal)
            if (!cardService.updatePin(newPin)) {
                throw new Exception("Failed to update PIN on card");
            }
            
            // 4. Lưu PIN hash vào DATABASE
            Resident resident = residentDAO.findByCardId(cardId);
            if (resident == null) {
                throw new Exception("Resident not found");
            }
            
            resident.setPinHash(newPin); // Trong thực tế nên hash PIN
            residentDAO.update(resident);
            
            return true;
        } catch (SQLException e) {
            throw new Exception("Error changing PIN: " + e.getMessage(), e);
        }
    }
    
    /**
     * Nạp tiền
     */
    public Transaction topUp(String cardId, int amount) throws Exception {
        try {
            if (!cardService.selectApplet()) {
                throw new Exception("Failed to select applet");
            }
            
            Resident resident = residentDAO.findByCardId(cardId);
            if (resident == null) {
                throw new Exception("Resident not found");
            }
            
            int balanceBefore = cardService.getBalance();
            int balanceAfter = balanceBefore + amount;
            
            // Cập nhật số dư trên thẻ
            if (!cardService.updateBalance(balanceAfter)) {
                throw new Exception("Failed to update balance on card");
            }
            
            // Cập nhật trong database
            resident.setBalance(balanceAfter);
            residentDAO.update(resident);
            
            // Lưu transaction
            Transaction transaction = new Transaction(resident.getId(), cardId, "TOPUP", 
                                                     amount, balanceBefore, balanceAfter, 
                                                     "Nạp tiền: " + amount + " VND");
            transactionDAO.insert(transaction);
            
            return transaction;
        } catch (IOException | SQLException e) {
            throw new Exception("Error topping up: " + e.getMessage(), e);
        }
    }
    
    /**
     * Thanh toán dịch vụ
     */
    public Transaction payService(String cardId, int amount, String description) throws Exception {
        try {
            if (!cardService.selectApplet()) {
                throw new Exception("Failed to select applet");
            }
            
            Resident resident = residentDAO.findByCardId(cardId);
            if (resident == null) {
                throw new Exception("Resident not found");
            }
            
            int balanceBefore = cardService.getBalance();
            if (balanceBefore < amount) {
                throw new Exception("Insufficient balance");
            }
            
            int balanceAfter = balanceBefore - amount;
            
            // Cập nhật số dư trên thẻ
            if (!cardService.updateBalance(balanceAfter)) {
                throw new Exception("Failed to update balance on card");
            }
            
            // Cập nhật trong database
            resident.setBalance(balanceAfter);
            residentDAO.update(resident);
            
            // Lưu transaction
            Transaction transaction = new Transaction(resident.getId(), cardId, "PAYMENT", 
                                                     amount, balanceBefore, balanceAfter, 
                                                     description);
            transactionDAO.insert(transaction);
            
            return transaction;
        } catch (IOException | SQLException e) {
            throw new Exception("Error paying service: " + e.getMessage(), e);
        }
    }
    
    /**
     * Thanh toán hóa đơn
     */
    public Transaction payInvoice(String cardId, Integer invoiceId) throws Exception {
        try {
            Invoice invoice = invoiceDAO.findById(invoiceId);
            if (invoice == null) {
                throw new Exception("Invoice not found");
            }
            
            if ("PAID".equals(invoice.getPaymentStatus())) {
                throw new Exception("Invoice already paid");
            }
            
            // Thanh toán dịch vụ
            Transaction transaction = payService(cardId, invoice.getAmount(), 
                                                "Thanh toán hóa đơn: " + invoice.getServiceName());
            transaction.setReferenceId(invoiceId);
            transactionDAO.insert(transaction);
            
            // Cập nhật trạng thái hóa đơn
            invoiceDAO.updatePaymentStatus(invoiceId, "PAID");
            
            return transaction;
        } catch (SQLException e) {
            throw new Exception("Error paying invoice: " + e.getMessage(), e);
        }
    }
    
    /**
     * Xem số dư
     */
    public int getBalance(String cardId) throws Exception {
        try {
            if (!cardService.selectApplet()) {
                throw new Exception("Failed to select applet");
            }
            return cardService.getBalance();
        } catch (IOException e) {
            throw new Exception("Error getting balance: " + e.getMessage(), e);
        }
    }
    
    /**
     * Cập nhật ảnh đại diện
     */
    public boolean updatePicture(String cardId, byte[] pictureBytes) throws Exception {
        try {
            if (!cardService.selectApplet()) {
                throw new Exception("Failed to select applet");
            }
            
            if (!cardService.updatePicture(pictureBytes)) {
                throw new Exception("Failed to update picture on card");
            }
            
            // Lưu Base64 vào database
            String base64Image = Base64.getEncoder().encodeToString(pictureBytes);
            Resident resident = residentDAO.findByCardId(cardId);
            if (resident != null) {
                resident.setPhotoPath(base64Image);
                residentDAO.update(resident);
            }
            
            return true;
        } catch (IOException | SQLException e) {
            throw new Exception("Error updating picture: " + e.getMessage(), e);
        }
    }
    
    /**
     * Lấy ảnh đại diện
     */
    public String getPicture(String cardId) throws Exception {
        try {
            if (!cardService.selectApplet()) {
                throw new Exception("Failed to select applet");
            }
            
            byte[] pictureBytes = cardService.getPicture();
            if (pictureBytes == null || pictureBytes.length == 0) {
                // Thử lấy từ database
                Resident resident = residentDAO.findByCardId(cardId);
                if (resident != null && resident.getPhotoPath() != null) {
                    return resident.getPhotoPath();
                }
                return null;
            }
            
            return Base64.getEncoder().encodeToString(pictureBytes);
        } catch (IOException | SQLException e) {
            throw new Exception("Error getting picture: " + e.getMessage(), e);
        }
    }
    
    /**
     * Cập nhật thông tin cư dân
     */
    public Resident updateResidentInfo(Integer residentId, String fullName, String dateOfBirth, 
                                      String roomNumber, String phoneNumber, String email, 
                                      String idNumber) throws Exception {
        try {
            Resident resident = residentDAO.findById(residentId);
            if (resident == null) {
                throw new Exception("Resident not found");
            }
            
            resident.setFullName(fullName);
            resident.setDateOfBirth(dateOfBirth);
            resident.setRoomNumber(roomNumber);
            resident.setPhoneNumber(phoneNumber);
            resident.setEmail(email);
            resident.setIdNumber(idNumber);
            
            residentDAO.update(resident);
            
            // Cập nhật lên thẻ nếu cần
            if (resident.getCardId() != null) {
                if (cardService.selectApplet()) {
                    String customerInfo = String.join("|", fullName, dateOfBirth, roomNumber, 
                                                      phoneNumber != null ? phoneNumber : "", 
                                                      email != null ? email : "", 
                                                      idNumber != null ? idNumber : "");
                    cardService.updateCustomerInfo(customerInfo);
                }
            }
            
            return resident;
        } catch (SQLException | IOException e) {
            throw new Exception("Error updating resident info: " + e.getMessage(), e);
        }
    }
    
    /**
     * Gửi xe
     */
    public Parking registerParking(Integer residentId, String licensePlate, String vehicleType, 
                                   String actionType, String gateLocation) throws Exception {
        try {
            Parking parking = new Parking(residentId, licensePlate, vehicleType, actionType);
            parking.setGateLocation(gateLocation);
            
            // Tính phí theo loại xe
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
    
    /**
     * Lấy lịch sử giao dịch
     */
    public List<Transaction> getTransactionHistory(String cardId) throws Exception {
        try {
            return transactionDAO.findByCardId(cardId);
        } catch (SQLException e) {
            throw new Exception("Error getting transaction history: " + e.getMessage(), e);
        }
    }
    
    /**
     * Lấy hóa đơn chưa thanh toán
     */
    public List<Invoice> getPendingInvoices(Integer residentId) throws Exception {
        try {
            return invoiceDAO.findPendingByResidentId(residentId);
        } catch (SQLException e) {
            throw new Exception("Error getting pending invoices: " + e.getMessage(), e);
        }
    }
    
    /**
     * Tạo hóa đơn (Admin)
     */
    public Invoice createInvoice(Integer residentId, String serviceName, String serviceCode, 
                                int amount, String description) throws Exception {
        try {
            Invoice invoice = new Invoice(residentId, serviceName, serviceCode, amount, LocalDate.now());
            invoice.setDescription(description);
            
            Integer id = invoiceDAO.insert(invoice);
            invoice.setId(id);
            
            return invoice;
        } catch (SQLException e) {
            throw new Exception("Error creating invoice: " + e.getMessage(), e);
        }
    }
    
    /**
     * Lấy danh sách cư dân (Admin)
     */
    public List<Resident> getAllResidents() throws Exception {
        try {
            return residentDAO.findAll();
        } catch (SQLException e) {
            throw new Exception("Error getting residents: " + e.getMessage(), e);
        }
    }
    
    /**
     * Lấy tất cả hóa đơn (Admin)
     */
    public List<Invoice> getAllInvoices() throws Exception {
        try {
            return invoiceDAO.findAll();
        } catch (SQLException e) {
            throw new Exception("Error getting all invoices: " + e.getMessage(), e);
        }
    }
    
    /**
     * Lấy tất cả parking records (Admin)
     */
    public List<Parking> getAllParking() throws Exception {
        try {
            return parkingDAO.findAll();
        } catch (SQLException e) {
            throw new Exception("Error getting all parking records: " + e.getMessage(), e);
        }
    }
}

