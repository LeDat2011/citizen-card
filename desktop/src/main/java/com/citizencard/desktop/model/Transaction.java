package com.citizencard.desktop.model;

public class Transaction {
    private Integer id;
    private Integer residentId;
    private String cardId;
    private String transactionType;
    private Integer amount;
    private Integer balanceBefore;
    private Integer balanceAfter;
    private String description;
    private Integer referenceId;
    private String timestamp;
    
    public Transaction() {}
    
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    
    public Integer getResidentId() { return residentId; }
    public void setResidentId(Integer residentId) { this.residentId = residentId; }
    
    public String getCardId() { return cardId; }
    public void setCardId(String cardId) { this.cardId = cardId; }
    
    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }
    
    public Integer getAmount() { return amount; }
    public void setAmount(Integer amount) { this.amount = amount; }
    
    public Integer getBalanceBefore() { return balanceBefore; }
    public void setBalanceBefore(Integer balanceBefore) { this.balanceBefore = balanceBefore; }
    
    public Integer getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(Integer balanceAfter) { this.balanceAfter = balanceAfter; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Integer getReferenceId() { return referenceId; }
    public void setReferenceId(Integer referenceId) { this.referenceId = referenceId; }
    
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}







