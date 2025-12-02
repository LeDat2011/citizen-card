package com.citizencard.backend.dao;

import com.citizencard.backend.database.DatabaseManager;
import com.citizencard.backend.model.Transaction;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TransactionDAO {
    
    public Integer insert(Transaction transaction) throws SQLException {
        String sql = "INSERT INTO transactions_simple (resident_id, card_id, transaction_type, " +
                    "amount, balance_before, balance_after, description, reference_id) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, transaction.getResidentId());
            stmt.setString(2, transaction.getCardId());
            stmt.setString(3, transaction.getTransactionType());
            stmt.setInt(4, transaction.getAmount());
            stmt.setInt(5, transaction.getBalanceBefore());
            stmt.setInt(6, transaction.getBalanceAfter());
            stmt.setString(7, transaction.getDescription());
            if (transaction.getReferenceId() != null) {
                stmt.setInt(8, transaction.getReferenceId());
            } else {
                stmt.setNull(8, Types.INTEGER);
            }
            
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
        String sql = "SELECT * FROM transactions_simple WHERE resident_id = ? ORDER BY timestamp DESC";
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
        String sql = "SELECT * FROM transactions_simple WHERE card_id = ? ORDER BY timestamp DESC";
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
        transaction.setBalanceBefore(rs.getInt("balance_before"));
        transaction.setBalanceAfter(rs.getInt("balance_after"));
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
}








