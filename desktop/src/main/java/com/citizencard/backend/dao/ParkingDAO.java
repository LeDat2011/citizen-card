package com.citizencard.backend.dao;

import com.citizencard.backend.database.DatabaseManager;
import com.citizencard.backend.model.Parking;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ParkingDAO {
    
    public Integer insert(Parking parking) throws SQLException {
        String sql = "INSERT INTO parking (resident_id, license_plate, vehicle_type, " +
                    "monthly_fee, expired_date) " +
                    "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, parking.getResidentId());
            stmt.setString(2, parking.getLicensePlate());
            stmt.setString(3, parking.getVehicleType());
            stmt.setInt(4, parking.getMonthlyFee() != null ? parking.getMonthlyFee() : 200000);
            if (parking.getExpiredDate() != null) {
                stmt.setDate(5, Date.valueOf(parking.getExpiredDate()));
            } else {
                stmt.setNull(5, Types.DATE);
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
    
    public List<Parking> findByResidentId(Integer residentId) throws SQLException {
        List<Parking> parkings = new ArrayList<>();
        String sql = "SELECT * FROM parking WHERE resident_id = ? ORDER BY timestamp DESC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, residentId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    parkings.add(mapResultSetToParking(rs));
                }
            }
        }
        return parkings;
    }
    
    public List<Parking> findByLicensePlate(String licensePlate) throws SQLException {
        List<Parking> parkings = new ArrayList<>();
        String sql = "SELECT * FROM parking WHERE license_plate = ? ORDER BY timestamp DESC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, licensePlate);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    parkings.add(mapResultSetToParking(rs));
                }
            }
        }
        return parkings;
    }
    
    public List<Parking> findAll() throws SQLException {
        List<Parking> parkings = new ArrayList<>();
        String sql = "SELECT * FROM parking ORDER BY timestamp DESC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                parkings.add(mapResultSetToParking(rs));
            }
        }
        return parkings;
    }
    
    private Parking mapResultSetToParking(ResultSet rs) throws SQLException {
        Parking parking = new Parking();
        parking.setId(rs.getInt("id"));
        parking.setResidentId(rs.getInt("resident_id"));
        parking.setLicensePlate(rs.getString("license_plate"));
        parking.setVehicleType(rs.getString("vehicle_type"));
        // action_type, gate_location, notes không còn trong DB, giữ trong model để tương thích
        parking.setActionType(null);
        parking.setMonthlyFee(rs.getInt("monthly_fee"));
        Date expiredDate = rs.getDate("expired_date");
        if (expiredDate != null) {
            parking.setExpiredDate(expiredDate.toLocalDate());
        }
        parking.setGateLocation(null);
        Timestamp timestamp = rs.getTimestamp("timestamp");
        if (timestamp != null) {
            parking.setTimestamp(timestamp.toLocalDateTime());
        }
        parking.setNotes(null);
        return parking;
    }
}

