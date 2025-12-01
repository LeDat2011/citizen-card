package com.citizencard.desktop.model;

public class Parking {
    private Integer id;
    private Integer residentId;
    private String licensePlate;
    private String vehicleType;
    private String actionType;
    private Integer monthlyFee;
    private String expiredDate;
    private String gateLocation;
    private String timestamp;
    private String notes;
    
    public Parking() {}
    
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
    
    public String getExpiredDate() { return expiredDate; }
    public void setExpiredDate(String expiredDate) { this.expiredDate = expiredDate; }
    
    public String getGateLocation() { return gateLocation; }
    public void setGateLocation(String gateLocation) { this.gateLocation = gateLocation; }
    
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}







