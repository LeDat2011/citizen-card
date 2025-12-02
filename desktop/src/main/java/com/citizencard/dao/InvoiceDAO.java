package com.citizencard.dao;

import com.citizencard.database.DatabaseManager;
import com.citizencard.model.Invoice;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class InvoiceDAO {
    
    public Integer insert(Invoice invoice) throws SQLException {
        String sql = "INSERT INTO invoices (resident_id, service_name, " +
                    "amount, payment_status, invoice_date, description) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, invoice.getResidentId());
            stmt.setString(2, invoice.getServiceName());
            stmt.setInt(3, invoice.getAmount());
            stmt.setString(4, invoice.getPaymentStatus() != null ? invoice.getPaymentStatus() : "PENDING");
            stmt.setDate(5, Date.valueOf(invoice.getInvoiceDate()));
            stmt.setString(6, invoice.getDescription());
            
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return null;
    }
    
    public Invoice findById(Integer id) throws SQLException {
        String sql = "SELECT * FROM invoices WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToInvoice(rs);
                }
            }
        }
        return null;
    }
    
    public List<Invoice> findByResidentId(Integer residentId) throws SQLException {
        List<Invoice> invoices = new ArrayList<>();
        String sql = "SELECT * FROM invoices WHERE resident_id = ? ORDER BY invoice_date DESC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, residentId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    invoices.add(mapResultSetToInvoice(rs));
                }
            }
        }
        return invoices;
    }
    
    public List<Invoice> findPendingByResidentId(Integer residentId) throws SQLException {
        List<Invoice> invoices = new ArrayList<>();
        String sql = "SELECT * FROM invoices WHERE resident_id = ? AND payment_status = 'PENDING' ORDER BY invoice_date DESC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, residentId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    invoices.add(mapResultSetToInvoice(rs));
                }
            }
        }
        return invoices;
    }
    
    public List<Invoice> findAll() throws SQLException {
        List<Invoice> invoices = new ArrayList<>();
        String sql = "SELECT * FROM invoices ORDER BY invoice_date DESC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                invoices.add(mapResultSetToInvoice(rs));
            }
        }
        return invoices;
    }
    
    public void updatePaymentStatus(Integer id, String status) throws SQLException {
        String sql = "UPDATE invoices SET payment_status = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setInt(2, id);
            stmt.executeUpdate();
        }
    }
    
    private Invoice mapResultSetToInvoice(ResultSet rs) throws SQLException {
        Invoice invoice = new Invoice();
        invoice.setId(rs.getInt("id"));
        invoice.setResidentId(rs.getInt("resident_id"));
        invoice.setServiceName(rs.getString("service_name"));
        // service_code không còn trong DB, giữ trong model để tương thích
        invoice.setServiceCode(null);
        invoice.setAmount(rs.getInt("amount"));
        invoice.setPaymentStatus(rs.getString("payment_status"));
        Date invoiceDate = rs.getDate("invoice_date");
        if (invoiceDate != null) {
            invoice.setInvoiceDate(invoiceDate.toLocalDate());
        }
        // payment_date không còn trong DB
        invoice.setPaymentDate(null);
        invoice.setDescription(rs.getString("description"));
        return invoice;
    }
}

