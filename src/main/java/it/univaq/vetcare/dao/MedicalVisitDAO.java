package it.univaq.vetcare.dao;

import it.univaq.vetcare.db.DatabaseManager;
import it.univaq.vetcare.model.MedicalVisit;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * US-08 & US-09: DAO per la persistenza transazionale delle visite cliniche consolidate,
 * aggiornamento automatico del peso in anagrafica e recupero della timeline storica.
 */
public class MedicalVisitDAO {

    private final DatabaseManager dbManager;

    public MedicalVisitDAO(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    /**
     * Salva atomicamente la nuova visita consolidata e allinea il peso corrente dell'animale.
     */
    public MedicalVisit insertConsolidatedVisit(MedicalVisit visit) {
        try {
            dbManager.executeInTransaction(conn -> {
                String sqlVisit = "INSERT INTO medical_visits "
                        + "(animal_id, visit_datetime, veterinarian_name, reason, anamnesis, objective_exam, "
                        + "weight_kg, temperature_c, diagnosis, prescribed_therapy, consolidated, digital_signature) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sqlVisit, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, visit.getAnimalId());
                    ps.setString(2, visit.getVisitDateTime().toString());
                    ps.setString(3, visit.getVeterinarianName());
                    ps.setString(4, visit.getReason());
                    ps.setString(5, visit.getAnamnesis());
                    ps.setString(6, visit.getObjectiveExam());
                    ps.setDouble(7, visit.getWeightKg());
                    ps.setDouble(8, visit.getTemperatureC());
                    ps.setString(9, visit.getDiagnosis());
                    ps.setString(10, visit.getPrescribedTherapy());
                    ps.setInt(11, visit.isConsolidated() ? 1 : 0);
                    ps.setString(12, visit.getDigitalSignature());
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            visit.setId(rs.getInt(1));
                        }
                    }
                }

                String sqlUpdateWeight = "UPDATE animals SET weight = ? WHERE id = ?";
                try (PreparedStatement psWeight = conn.prepareStatement(sqlUpdateWeight)) {
                    psWeight.setDouble(1, visit.getWeightKg());
                    psWeight.setInt(2, visit.getAnimalId());
                    psWeight.executeUpdate();
                }
            });
            return visit;
        } catch (Exception e) {
            throw new RuntimeException("Errore salvataggio visita clinica consolidata: " + e.getMessage(), e);
        }
    }

    public Optional<MedicalVisit> findById(int visitId) {
        String sql = "SELECT * FROM medical_visits WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, visitId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Errore lettura visita per ID: " + e.getMessage(), e);
        }
    }

    /**
     * US-08: Restituisce la timeline cronologica delle visite (dalla più recente alla meno recente).
     */
    public List<MedicalVisit> findTimelineByAnimalId(int animalId) {
        String sql = "SELECT * FROM medical_visits WHERE animal_id = ? ORDER BY visit_datetime DESC, id DESC";
        List<MedicalVisit> visits = new ArrayList<>();
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, animalId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    visits.add(mapRow(rs));
                }
            }
            return visits;
        } catch (SQLException e) {
            throw new RuntimeException("Errore recupero storia clinica animale: " + e.getMessage(), e);
        }
    }

    private MedicalVisit mapRow(ResultSet rs) throws SQLException {
        return new MedicalVisit(
                rs.getInt("id"),
                rs.getInt("animal_id"),
                LocalDateTime.parse(rs.getString("visit_datetime")),
                rs.getString("veterinarian_name"),
                rs.getString("reason"),
                rs.getString("anamnesis"),
                rs.getString("objective_exam"),
                rs.getDouble("weight_kg"),
                rs.getDouble("temperature_c"),
                rs.getString("diagnosis"),
                rs.getString("prescribed_therapy"),
                rs.getInt("consolidated") == 1,
                rs.getString("digital_signature")
        );
    }
}
