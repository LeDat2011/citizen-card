package com.citizencard.backend.dao;

import com.citizencard.backend.database.DatabaseManager;
import com.citizencard.backend.model.Transaction;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TransactionDAO {
    
    public Integer insert(Transaction transaction) throws SQLException {
        String sql = "INSERT INTO transactions (resident_id, card_id, transaction_type, " +
                    "amount, balance_after, payment_status, service_name, description) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, transaction.getResidentId());
            stmt.setString(2, transaction.getCardId());
            stmt.setString(3, transaction.getTransactionType());
            stmt.setInt(4, transaction.getAmount());
            stmt.setInt(5, transaction.getBalanceAfter());
            stmt.setString(6, transaction.getPaymentStatus());
            stmt.setString(7, transaction.getServiceName());
            stmt.setString(8, transaction.getDescription());
            
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return null;
    }
    
    public List<Transaction> findByResidentId(Integer residentId) throws SQLException {
        List<Transaction> transactions = new ArrayList<>();
        String sql = "SELECT * FROM transactions WHERE resident_id = ? ORDER BY timestamp DESC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, residentId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    transactions.add(mapResultSetToTransaction(rs));
                }
            }
        }
        return transactions;
    }
    
    public List<Transaction> findByCardId(String cardId) throws SQLException {
        List<Transaction> transactions = new ArrayList<>();
        String sql = "SELECT * FROM transactions WHERE card_id = ? ORDER BY timestamp DESC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, cardId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    transactions.add(mapResultSetToTransaction(rs));
                }
            }
        }
        return transactions;
    }
    
    private Transaction mapResultSetToTransaction(ResultSet rs) throws SQLException {
        Transaction transaction = new Transaction();
        transaction.setId(rs.getInt("id"));
        transaction.setResidentId(rs.getInt("resident_id"));
        transaction.setCardId(rs.getString("card_id"));
        transaction.setTransactionType(rs.getString("transaction_type"));
        transaction.setAmount(rs.getInt("amount"));
        // balance_before không còn trong DB, tính từ balance_after - amount
        int balanceAfter = rs.getInt("balance_after");
        int amount = rs.getInt("amount");
        transaction.setBalanceBefore(balanceAfter - amount);
        transaction.setBalanceAfter(balanceAfter);
        transaction.setPaymentStatus(rs.getString("payment_status"));
        transaction.setServiceName(rs.getString("service_name"));
        transaction.setDescription(rs.getString("description"));
        int refId = rs.getInt("reference_id");
        if (!rs.wasNull()) {
            transaction.setReferenceId(refId);
        }
        Timestamp timestamp = rs.getTimestamp("timestamp");
        if (timestamp != null) {
            transaction.setTimestamp(timestamp.toLocalDateTime());
        }
        return transaction;
    }
    
    /**
     * Tìm invoices (transactions với type = 'INVOICE')
     */
    public List<Transaction> findInvoicesByResidentId(Integer residentId) throws SQLException {
        List<Transaction> invoices = new ArrayList<>();
        String sql = "SELECT * FROM transactions WHERE resident_id = ? AND transaction_type = 'INVOICE' ORDER BY timestamp DESC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, residentId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    invoices.add(mapResultSetToTransaction(rs));
                }
            }
        }
        return invoices;
    }
    
    /**
     * Tìm pending invoices
     */
    public List<Transaction> findPendingInvoicesByResidentId(Integer residentId) throws SQLException {
        List<Transaction> invoices = new ArrayList<>();
        String sql = "SELECT * FROM transactions WHERE resident_id = ? AND transaction_type = 'INVOICE' AND payment_status = 'PENDING' ORDER BY timestamp DESC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, residentId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    invoices.add(mapResultSetToTransaction(rs));
                }
            }
        }
        return invoices;
    }
    
    /**
     * Tìm tất cả invoices
     */
    public List<Transaction> findAllInvoices() throws SQLException {
        List<Transaction> invoices = new ArrayList<>();
        String sql = "SELECT * FROM transactions WHERE transaction_type = 'INVOICE' ORDER BY timestamp DESC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                invoices.add(mapResultSetToTransaction(rs));
            }
        }
        return invoices;
    }
    
    /**
     * Tìm invoice theo ID
     */
    public Transaction findInvoiceById(Integer id) throws SQLException {
        String sql = "SELECT * FROM transactions WHERE id = ? AND transaction_type = 'INVOICE'";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToTransaction(rs);
                }
            }
        }
        return null;
    }
    
    /**
     * Cập nhật payment status của invoice
     */
    public void updateInvoicePaymentStatus(Integer id, String status) throws SQLException {
        String sql = "UPDATE transactions SET payment_status = ? WHERE id = ? AND transaction_type = 'INVOICE'";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setInt(2, id);
            stmt.executeUpdate();
        }
    }
}


