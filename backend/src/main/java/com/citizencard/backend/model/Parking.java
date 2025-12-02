package com.citizencard.backend.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Parking {
    private Integer id;
    private Integer residentId;
    private String licensePlate;
    private String vehicleType; // MOTORBIKE, CAR, BICYCLE
    private String actionType; // REGISTER, CHECK_IN, CHECK_OUT
    private Integer monthlyFee;
    private LocalDate expiredDate;
    private String gateLocation;
    private LocalDateTime timestamp;
    private String notes;
    
    public Parking() {}
    
    public Parking(Integer residentId, String licensePlate, String vehicleType, String actionType) {
        this.residentId = residentId;
        this.licensePlate = licensePlate;
        this.vehicleType = vehicleType;
        this.actionType = actionType;
        this.monthlyFee = 200000; // Default
    }
    
    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    
    public Integer getResidentId() { return residentId; }
    public void setResidentId(Integer residentId) { this.residentId = residentId; }
    
    public String getLicensePlate() { return licensePlate; }
    public void setLicensePlate(String licensePlate) { this.licensePlate = licensePlate; }
    
    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }
    
    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }
    
    public Integer getMonthlyFee() { return monthlyFee; }
    public void setMonthlyFee(Integer monthlyFee) { this.monthlyFee = monthlyFee; }
    
    public LocalDate getExpiredDate() { return expiredDate; }
    public void setExpiredDate(LocalDate expiredDate) { this.expiredDate = expiredDate; }
    
    public String getGateLocation() { return gateLocation; }
    public void setGateLocation(String gateLocation) { this.gateLocation = gateLocation; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}








