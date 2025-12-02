package com.citizencard.model;

import java.time.LocalDateTime;

public class Resident {
    private Integer id;
    private String cardId;
    private String fullName;
    private String dateOfBirth;
    private String roomNumber;
    private String phoneNumber;
    private String email;
    private String idNumber;
    private Integer balance;
    private String photoPath;
    private String status;
    private String pinHash;
    private String publicKey;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public Resident() {}
    
    public Resident(String cardId, String fullName, String dateOfBirth, String roomNumber) {
        this.cardId = cardId;
        this.fullName = fullName;
        this.dateOfBirth = dateOfBirth;
        this.roomNumber = roomNumber;
        this.balance = 0;
        this.status = "ACTIVE";
    }
    
    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    
    public String getCardId() { return cardId; }
    public void setCardId(String cardId) { this.cardId = cardId; }
    
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    
    public String getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    
    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }
    
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getIdNumber() { return idNumber; }
    public void setIdNumber(String idNumber) { this.idNumber = idNumber; }
    
    public Integer getBalance() { return balance; }
    public void setBalance(Integer balance) { this.balance = balance; }
    
    public String getPhotoPath() { return photoPath; }
    public void setPhotoPath(String photoPath) { this.photoPath = photoPath; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getPinHash() { return pinHash; }
    public void setPinHash(String pinHash) { this.pinHash = pinHash; }
    
    public String getPublicKey() { return publicKey; }
    public void setPublicKey(String publicKey) { this.publicKey = publicKey; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}


