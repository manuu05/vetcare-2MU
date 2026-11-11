package it.univaq.vetcare.dao;

import it.univaq.vetcare.db.DatabaseManager;
import it.univaq.vetcare.model.Allergy;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * US-06: Data Access Object per le allergie e intolleranze farmacologiche del paziente.
 */
public class AllergyDAO {

    private final DatabaseManager dbManager;

    public AllergyDAO(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public Allergy insert(Allergy allergy) {
        String sql = "INSERT INTO animal_allergies (animal_id, substance, severity, notes) VALUES (?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, allergy.getAnimalId());
            ps.setString(2, allergy.getSubstance());
            ps.setString(3, allergy.getSeverity() != null ? allergy.getSeverity() : "CRITICA");
            ps.setString(4, allergy.getNotes());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    allergy.setId(rs.getInt(1));
                }
            }
            return allergy;
        } catch (SQLException e) {
            throw new RuntimeException("Errore salvataggio allergia paziente: " + e.getMessage(), e);
        }
    }

    public List<Allergy> findByAnimalId(int animalId) {
        String sql = "SELECT * FROM animal_allergies WHERE animal_id = ? ORDER BY id ASC";
        List<Allergy> list = new ArrayList<>();
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, animalId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Allergy(
                            rs.getInt("id"),
                            rs.getInt("animal_id"),
                            rs.getString("substance"),
                            rs.getString("severity"),
                            rs.getString("notes")
                    ));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Errore recupero allergie paziente: " + e.getMessage(), e);
        }
    }
}
