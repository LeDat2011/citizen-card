package com.citizencard.desktop.util;

// Using fully qualified names to avoid import conflicts
import java.util.List;
import java.util.stream.Collectors;

/**
 * Converter giữa Backend models và Desktop models
 */
public class ModelConverter {
    
    // ========== Resident ==========
    
    public static com.citizencard.desktop.model.Resident toDesktopResident(com.citizencard.backend.model.Resident backend) {
        if (backend == null) return null;
        
        com.citizencard.desktop.model.Resident desktop = new com.citizencard.desktop.model.Resident();
        desktop.setId(backend.getId());
        desktop.setCardId(backend.getCardId());
        desktop.setFullName(backend.getFullName());
        desktop.setDateOfBirth(backend.getDateOfBirth());
        desktop.setRoomNumber(backend.getRoomNumber());
        desktop.setPhoneNumber(backend.getPhoneNumber());
        desktop.setEmail(backend.getEmail());
        desktop.setIdNumber(backend.getIdNumber());
        desktop.setBalance(backend.getBalance());
        desktop.setPhotoPath(backend.getPhotoPath());
        desktop.setStatus(backend.getStatus());
        return desktop;
    }
    
    public static com.citizencard.backend.model.Resident toBackendResident(com.citizencard.desktop.model.Resident desktop) {
        if (desktop == null) return null;
        
        com.citizencard.backend.model.Resident backend = new com.citizencard.backend.model.Resident();
        backend.setId(desktop.getId());
        backend.setCardId(desktop.getCardId());
        backend.setFullName(desktop.getFullName());
        backend.setDateOfBirth(desktop.getDateOfBirth());
        backend.setRoomNumber(desktop.getRoomNumber());
        backend.setPhoneNumber(desktop.getPhoneNumber());
        backend.setEmail(desktop.getEmail());
        backend.setIdNumber(desktop.getIdNumber());
        backend.setBalance(desktop.getBalance());
        backend.setPhotoPath(desktop.getPhotoPath());
        backend.setStatus(desktop.getStatus());
        return backend;
    }
    
    public static List<com.citizencard.desktop.model.Resident> toDesktopResidents(List<com.citizencard.backend.model.Resident> backendList) {
        if (backendList == null) return null;
        return backendList.stream()
            .map(ModelConverter::toDesktopResident)
            .collect(Collectors.toList());
    }
    
    // ========== Transaction ==========
    
    public static com.citizencard.desktop.model.Transaction toDesktopTransaction(com.citizencard.backend.model.Transaction backend) {
        if (backend == null) return null;
        
        com.citizencard.desktop.model.Transaction desktop = new com.citizencard.desktop.model.Transaction();
        desktop.setId(backend.getId());
        desktop.setResidentId(backend.getResidentId());
        desktop.setCardId(backend.getCardId());
        desktop.setTransactionType(backend.getTransactionType());
        desktop.setAmount(backend.getAmount());
        desktop.setBalanceBefore(backend.getBalanceBefore());
        desktop.setBalanceAfter(backend.getBalanceAfter());
        desktop.setDescription(backend.getDescription());
        desktop.setReferenceId(backend.getReferenceId());
        if (backend.getTimestamp() != null) {
            desktop.setTimestamp(backend.getTimestamp().toString());
        }
        return desktop;
    }
    
    public static List<com.citizencard.desktop.model.Transaction> toDesktopTransactions(List<com.citizencard.backend.model.Transaction> backendList) {
        if (backendList == null) return null;
        return backendList.stream()
            .map(ModelConverter::toDesktopTransaction)
            .collect(Collectors.toList());
    }
    
    // ========== Invoice ==========
    // Invoice giờ được lưu trong transactions với type = 'INVOICE'
    
    /**
     * Convert Transaction (type=INVOICE) sang Invoice model cho UI
     */
    public static com.citizencard.desktop.model.Invoice transactionToDesktopInvoice(com.citizencard.backend.model.Transaction transaction) {
        if (transaction == null || !"INVOICE".equals(transaction.getTransactionType())) return null;
        
        com.citizencard.desktop.model.Invoice desktop = new com.citizencard.desktop.model.Invoice();
        desktop.setId(transaction.getId());
        desktop.setResidentId(transaction.getResidentId());
        desktop.setServiceName(transaction.getServiceName());
        desktop.setServiceCode(null); // Không còn trong DB
        desktop.setAmount(transaction.getAmount());
        desktop.setPaymentStatus(transaction.getPaymentStatus());
        if (transaction.getTimestamp() != null) {
            desktop.setInvoiceDate(transaction.getTimestamp().toLocalDate().toString());
        }
        desktop.setPaymentDate(null); // Không còn trong DB
        desktop.setDescription(transaction.getDescription());
        return desktop;
    }
    
    public static List<com.citizencard.desktop.model.Invoice> transactionsToDesktopInvoices(List<com.citizencard.backend.model.Transaction> transactionList) {
        if (transactionList == null) return null;
        return transactionList.stream()
            .map(ModelConverter::transactionToDesktopInvoice)
            .filter(invoice -> invoice != null)
            .collect(Collectors.toList());
    }
    
    // Giữ lại method cũ để tương thích (nếu có code cũ dùng)
    public static com.citizencard.desktop.model.Invoice toDesktopInvoice(com.citizencard.backend.model.Invoice backend) {
        if (backend == null) return null;
        
        com.citizencard.desktop.model.Invoice desktop = new com.citizencard.desktop.model.Invoice();
        desktop.setId(backend.getId());
        desktop.setResidentId(backend.getResidentId());
        desktop.setServiceName(backend.getServiceName());
        desktop.setServiceCode(backend.getServiceCode());
        desktop.setAmount(backend.getAmount());
        desktop.setPaymentStatus(backend.getPaymentStatus());
        if (backend.getInvoiceDate() != null) {
            desktop.setInvoiceDate(backend.getInvoiceDate().toString());
        }
        if (backend.getPaymentDate() != null) {
            desktop.setPaymentDate(backend.getPaymentDate().toString());
        }
        desktop.setDescription(backend.getDescription());
        return desktop;
    }
    
    public static List<com.citizencard.desktop.model.Invoice> toDesktopInvoices(List<com.citizencard.backend.model.Invoice> backendList) {
        if (backendList == null) return null;
        return backendList.stream()
            .map(ModelConverter::toDesktopInvoice)
            .collect(Collectors.toList());
    }
    
    // ========== Parking ==========
    
    public static com.citizencard.desktop.model.Parking toDesktopParking(com.citizencard.backend.model.Parking backend) {
        if (backend == null) return null;
        
        com.citizencard.desktop.model.Parking desktop = new com.citizencard.desktop.model.Parking();
        desktop.setId(backend.getId());
        desktop.setResidentId(backend.getResidentId());
        desktop.setLicensePlate(backend.getLicensePlate());
        desktop.setVehicleType(backend.getVehicleType());
        desktop.setActionType(backend.getActionType());
        desktop.setMonthlyFee(backend.getMonthlyFee());
        if (backend.getExpiredDate() != null) {
            desktop.setExpiredDate(backend.getExpiredDate().toString());
        }
        desktop.setGateLocation(backend.getGateLocation());
        if (backend.getTimestamp() != null) {
            desktop.setTimestamp(backend.getTimestamp().toString());
        }
        desktop.setNotes(backend.getNotes());
        return desktop;
    }
    
    public static List<com.citizencard.desktop.model.Parking> toDesktopParkings(List<com.citizencard.backend.model.Parking> backendList) {
        if (backendList == null) return null;
        return backendList.stream()
            .map(ModelConverter::toDesktopParking)
            .collect(Collectors.toList());
    }
}

