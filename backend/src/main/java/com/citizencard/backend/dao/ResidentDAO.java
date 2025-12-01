package com.citizencard.backend.dao;

import com.citizencard.backend.database.DatabaseManager;
import com.citizencard.backend.model.Resident;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ResidentDAO {
    
    public Resident findByCardId(String cardId) throws SQLException {
        String sql = "SELECT * FROM residents WHERE card_id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, cardId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToResident(rs);
                }
            }
        }
        return null;
    }
    
    public Resident findById(Integer id) throws SQLException {
        String sql = "SELECT * FROM residents WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToResident(rs);
                }
            }
        }
        return null;
    }
    
    public List<Resident> findAll() throws SQLException {
        List<Resident> residents = new ArrayList<>();
        String sql = "SELECT * FROM residents ORDER BY created_at DESC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                residents.add(mapResultSetToResident(rs));
            }
        }
        return residents;
    }
    
    public Integer insert(Resident resident) throws SQLException {
        String sql = "INSERT INTO residents (card_id, full_name, date_of_birth, room_number, " +
                    "phone_number, email, id_number, balance, photo_path, status, pin_hash) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, resident.getCardId());
            stmt.setString(2, resident.getFullName());
            stmt.setString(3, resident.getDateOfBirth());
            stmt.setString(4, resident.getRoomNumber());
            stmt.setString(5, resident.getPhoneNumber());
            stmt.setString(6, resident.getEmail());
            stmt.setString(7, resident.getIdNumber());
            stmt.setInt(8, resident.getBalance() != null ? resident.getBalance() : 0);
            stmt.setString(9, resident.getPhotoPath());
            stmt.setString(10, resident.getStatus() != null ? resident.getStatus() : "ACTIVE");
            stmt.setString(11, resident.getPinHash());
            
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return null;
    }
    
    public void update(Resident resident) throws SQLException {
        String sql = "UPDATE residents SET full_name = ?, date_of_birth = ?, room_number = ?, " +
                    "phone_number = ?, email = ?, id_number = ?, balance = ?, photo_path = ?, " +
                    "status = ?, pin_hash = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, resident.getFullName());
            stmt.setString(2, resident.getDateOfBirth());
            stmt.setString(3, resident.getRoomNumber());
            stmt.setString(4, resident.getPhoneNumber());
            stmt.setString(5, resident.getEmail());
            stmt.setString(6, resident.getIdNumber());
            stmt.setInt(7, resident.getBalance() != null ? resident.getBalance() : 0);
            stmt.setString(8, resident.getPhotoPath());
            stmt.setString(9, resident.getStatus());
            stmt.setString(10, resident.getPinHash());
            stmt.setInt(11, resident.getId());
            stmt.executeUpdate();
        }
    }
    
    public void delete(Integer id) throws SQLException {
        String sql = "DELETE FROM residents WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }
    
    private Resident mapResultSetToResident(ResultSet rs) throws SQLException {
        Resident resident = new Resident();
        resident.setId(rs.getInt("id"));
        resident.setCardId(rs.getString("card_id"));
        resident.setFullName(rs.getString("full_name"));
        resident.setDateOfBirth(rs.getString("date_of_birth"));
        resident.setRoomNumber(rs.getString("room_number"));
        resident.setPhoneNumber(rs.getString("phone_number"));
        resident.setEmail(rs.getString("email"));
        resident.setIdNumber(rs.getString("id_number"));
        resident.setBalance(rs.getInt("balance"));
        resident.setPhotoPath(rs.getString("photo_path"));
        resident.setStatus(rs.getString("status"));
        resident.setPinHash(rs.getString("pin_hash"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            resident.setCreatedAt(createdAt.toLocalDateTime());
        }
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            resident.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        return resident;
    }
}







