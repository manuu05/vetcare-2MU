package it.univaq.vetcare.dao;

import it.univaq.vetcare.db.DatabaseManager;
import it.univaq.vetcare.model.PrescriptionOverrideLog;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * US-07: DAO per la persistenza dell'audit log degli Override Primari motivati.
 */
public class PrescriptionOverrideDAO {

    private final DatabaseManager dbManager;

    public PrescriptionOverrideDAO(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public PrescriptionOverrideLog insert(PrescriptionOverrideLog log) {
        String sql = "INSERT INTO prescription_overrides "
                + "(animal_id, prescribed_drug, matched_allergen, veterinarian_name, clinical_justification, override_timestamp) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, log.getAnimalId());
            ps.setString(2, log.getPrescribedDrug());
            ps.setString(3, log.getMatchedAllergen());
            ps.setString(4, log.getVeterinarianName());
            ps.setString(5, log.getClinicalJustification());
            ps.setString(6, log.getOverrideTimestamp().toString());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    log.setId(rs.getInt(1));
                }
            }
            return log;
        } catch (SQLException e) {
            throw new RuntimeException("Errore salvataggio audit Override Allergia: " + e.getMessage(), e);
        }
    }

    public List<PrescriptionOverrideLog> findByAnimalId(int animalId) {
        String sql = "SELECT * FROM prescription_overrides WHERE animal_id = ? ORDER BY id DESC";
        List<PrescriptionOverrideLog> logs = new ArrayList<>();
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, animalId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    logs.add(new PrescriptionOverrideLog(
                            rs.getInt("id"),
                            rs.getInt("animal_id"),
                            rs.getString("prescribed_drug"),
                            rs.getString("matched_allergen"),
                            rs.getString("veterinarian_name"),
                            rs.getString("clinical_justification"),
                            LocalDateTime.parse(rs.getString("override_timestamp"))
                    ));
                }
            }
            return logs;
        } catch (SQLException e) {
            throw new RuntimeException("Errore lettura audit Override Allergia: " + e.getMessage(), e);
        }
    }
}
