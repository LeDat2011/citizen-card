package com.citizencard.backend.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Invoice {
    private Integer id;
    private Integer residentId;
    private String serviceName;
    private String serviceCode; // ELECTRIC, WATER, MANAGEMENT, POOL, GYM
    private Integer amount;
    private String paymentStatus; // PENDING, PAID
    private LocalDate invoiceDate;
    private LocalDateTime paymentDate;
    private String description;
    
    public Invoice() {}
    
    public Invoice(Integer residentId, String serviceName, String serviceCode, 
                   Integer amount, LocalDate invoiceDate) {
        this.residentId = residentId;
        this.serviceName = serviceName;
        this.serviceCode = serviceCode;
        this.amount = amount;
        this.invoiceDate = invoiceDate;
        this.paymentStatus = "PENDING";
    }
    
    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    
    public Integer getResidentId() { return residentId; }
    public void setResidentId(Integer residentId) { this.residentId = residentId; }
    
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    
    public String getServiceCode() { return serviceCode; }
    public void setServiceCode(String serviceCode) { this.serviceCode = serviceCode; }
    
    public Integer getAmount() { return amount; }
    public void setAmount(Integer amount) { this.amount = amount; }
    
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    
    public LocalDate getInvoiceDate() { return invoiceDate; }
    public void setInvoiceDate(LocalDate invoiceDate) { this.invoiceDate = invoiceDate; }
    
    public LocalDateTime getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDateTime paymentDate) { this.paymentDate = paymentDate; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}


