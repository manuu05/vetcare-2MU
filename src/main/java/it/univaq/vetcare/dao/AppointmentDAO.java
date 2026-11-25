package it.univaq.vetcare.dao;

import it.univaq.vetcare.db.DatabaseManager;
import it.univaq.vetcare.model.ClinicAppointment;
import it.univaq.vetcare.model.VisitType;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * US-11: DAO per la persistenza degli appuntamenti sull'agenda giornaliera
 * e settimanale affiancata per i 3 veterinari dell'ambulatorio.
 */
public class AppointmentDAO {

    private final DatabaseManager dbManager;

    public AppointmentDAO(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public ClinicAppointment insert(ClinicAppointment appt) {
        String sql = "INSERT INTO clinic_appointments "
                + "(animal_id, veterinarian_id, visit_type, start_datetime, end_datetime, clinical_notes, status) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, appt.getAnimalId());
            ps.setInt(2, appt.getVeterinarianId());
            ps.setString(3, appt.getVisitType().name());
            ps.setString(4, appt.getStartDateTime().toString());
            ps.setString(5, appt.getEndDateTime().toString());
            ps.setString(6, appt.getClinicalNotes());
            ps.setString(7, appt.getStatus());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    appt.setId(rs.getInt(1));
                }
            }
            return appt;
        } catch (SQLException e) {
            throw new RuntimeException("Errore salvataggio appuntamento in agenda: " + e.getMessage(), e);
        }
    }

    public List<ClinicAppointment> findByDateRange(LocalDate fromDate, LocalDate toDate) {
        String sql = "SELECT ca.*, a.name AS animal_name "
                + "FROM clinic_appointments ca "
                + "INNER JOIN animals a ON a.id = ca.animal_id "
                + "WHERE ca.start_datetime >= ? AND ca.start_datetime < ? "
                + "  AND ca.status <> 'ANNULLATO' "
                + "ORDER BY ca.start_datetime ASC";
        List<ClinicAppointment> result = new ArrayList<>();
        String fromStr = fromDate.atStartOfDay().toString();
        String toStr = toDate.plusDays(1).atStartOfDay().toString();

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, fromStr);
            ps.setString(2, toStr);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Errore lettura agenda per intervallo date: " + e.getMessage(), e);
        }
    }

    public List<ClinicAppointment> findByDay(LocalDate day) {
        return findByDateRange(day, day);
    }

    private ClinicAppointment mapRow(ResultSet rs) throws SQLException {
        return new ClinicAppointment(
                rs.getInt("id"),
                rs.getInt("animal_id"),
                rs.getString("animal_name"),
                rs.getInt("veterinarian_id"),
                VisitType.fromNameOrDefault(rs.getString("visit_type")),
                LocalDateTime.parse(rs.getString("start_datetime")),
                LocalDateTime.parse(rs.getString("end_datetime")),
                rs.getString("clinical_notes"),
                rs.getString("status")
        );
    }
}
