package com.citizencard.util;

import com.citizencard.model.Resident;
import com.citizencard.model.Transaction;
import com.citizencard.model.Invoice;
import com.citizencard.model.Parking;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Converter giữa models (hiện tại chỉ có 1 bộ model, converter chủ yếu để tương thích)
 */
public class ModelConverter {
    
    // ========== Resident ==========
    
    public static Resident toDesktopResident(Resident backend) {
        if (backend == null) return null;
        
        Resident desktop = new Resident();
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
        desktop.setPublicKey(backend.getPublicKey());
        return desktop;
    }
    
    public static Resident toBackendResident(Resident desktop) {
        if (desktop == null) return null;
        
        Resident backend = new Resident();
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
        backend.setPublicKey(desktop.getPublicKey());
        return backend;
    }
    
    public static List<Resident> toDesktopResidents(List<Resident> backendList) {
        if (backendList == null) return null;
        return backendList.stream()
            .map(ModelConverter::toDesktopResident)
            .collect(Collectors.toList());
    }
    
    // ========== Transaction ==========
    
    public static Transaction toDesktopTransaction(Transaction backend) {
        if (backend == null) return null;
        
        Transaction desktop = new Transaction();
        desktop.setId(backend.getId());
        desktop.setResidentId(backend.getResidentId());
        desktop.setCardId(backend.getCardId());
        desktop.setTransactionType(backend.getTransactionType());
        desktop.setAmount(backend.getAmount());
        desktop.setBalanceBefore(backend.getBalanceBefore());
        desktop.setBalanceAfter(backend.getBalanceAfter());
        desktop.setDescription(backend.getDescription());
        desktop.setReferenceId(backend.getReferenceId());
        desktop.setTimestamp(backend.getTimestamp());
        return desktop;
    }
    
    public static List<Transaction> toDesktopTransactions(List<Transaction> backendList) {
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
    public static Invoice transactionToDesktopInvoice(Transaction transaction) {
        if (transaction == null || !"INVOICE".equals(transaction.getTransactionType())) return null;
        
        Invoice desktop = new Invoice();
        desktop.setId(transaction.getId());
        desktop.setResidentId(transaction.getResidentId());
        desktop.setServiceName(transaction.getServiceName());
        desktop.setServiceCode(null); // Không còn trong DB
        desktop.setAmount(transaction.getAmount());
        desktop.setPaymentStatus(transaction.getPaymentStatus());
        if (transaction.getTimestamp() != null) {
            desktop.setInvoiceDate(transaction.getTimestamp().toLocalDate());
        }
        desktop.setPaymentDate(null); // Không còn trong DB
        desktop.setDescription(transaction.getDescription());
        return desktop;
    }
    
    public static List<Invoice> transactionsToDesktopInvoices(List<Transaction> transactionList) {
        if (transactionList == null) return null;
        return transactionList.stream()
            .map(ModelConverter::transactionToDesktopInvoice)
            .filter(invoice -> invoice != null)
            .collect(Collectors.toList());
    }
    
    // Giữ lại method cũ để tương thích (nếu có code cũ dùng)
    public static Invoice toDesktopInvoice(Invoice backend) {
        if (backend == null) return null;
        
        Invoice desktop = new Invoice();
        desktop.setId(backend.getId());
        desktop.setResidentId(backend.getResidentId());
        desktop.setServiceName(backend.getServiceName());
        desktop.setServiceCode(backend.getServiceCode());
        desktop.setAmount(backend.getAmount());
        desktop.setPaymentStatus(backend.getPaymentStatus());
        desktop.setInvoiceDate(backend.getInvoiceDate());
        desktop.setPaymentDate(backend.getPaymentDate());
        desktop.setDescription(backend.getDescription());
        return desktop;
    }
    
    public static List<Invoice> toDesktopInvoices(List<Invoice> backendList) {
        if (backendList == null) return null;
        return backendList.stream()
            .map(ModelConverter::toDesktopInvoice)
            .collect(Collectors.toList());
    }
    
    // ========== Parking ==========
    
    public static Parking toDesktopParking(Parking backend) {
        if (backend == null) return null;
        
        Parking desktop = new Parking();
        desktop.setId(backend.getId());
        desktop.setResidentId(backend.getResidentId());
        desktop.setLicensePlate(backend.getLicensePlate());
        desktop.setVehicleType(backend.getVehicleType());
        desktop.setActionType(backend.getActionType());
        desktop.setMonthlyFee(backend.getMonthlyFee());
        desktop.setExpiredDate(backend.getExpiredDate());
        desktop.setGateLocation(backend.getGateLocation());
        desktop.setTimestamp(backend.getTimestamp());
        desktop.setNotes(backend.getNotes());
        return desktop;
    }
    
    public static List<Parking> toDesktopParkings(List<Parking> backendList) {
        if (backendList == null) return null;
        return backendList.stream()
            .map(ModelConverter::toDesktopParking)
            .collect(Collectors.toList());
    }
}

