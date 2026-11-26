package it.univaq.vetcare.dao;

import it.univaq.vetcare.db.DatabaseManager;
import it.univaq.vetcare.model.Owner;
import it.univaq.vetcare.model.OwnershipDetail;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * US-03 (Sub-Task US03-1): DAO per la gestione transazionale dei comproprietari / affido
 * e per la designazione dell'unico Referente Primario di contatto per ciascun animale.
 */
public class PrimaryContactDAO {

    private final DatabaseManager dbManager;

    public PrimaryContactDAO(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public void linkOwnerAndManagePrimary(int animalId, int ownerId, String role, boolean markAsPrimary) {
        try {
            dbManager.executeInTransaction(conn -> {
                String upsertLink = "INSERT INTO animal_owners (animal_id, owner_id, ownership_role) "
                        + "VALUES (?, ?, ?) "
                        + "ON CONFLICT(animal_id, owner_id) DO UPDATE SET ownership_role = excluded.ownership_role";
                try (PreparedStatement ps = conn.prepareStatement(upsertLink)) {
                    ps.setInt(1, animalId);
                    ps.setInt(2, ownerId);
                    ps.setString(3, role != null && !role.trim().isEmpty() ? role.trim() : "Comproprietario");
                    ps.executeUpdate();
                }

                if (markAsPrimary) {
                    setPrimaryOwnerInternal(conn, animalId, ownerId);
                }
            });
        } catch (Exception e) {
            throw new RuntimeException("Errore salvataggio comproprietà/referente primario: " + e.getMessage(), e);
        }
    }

    public void setPrimaryContact(int animalId, int ownerId) {
        try {
            dbManager.executeInTransaction(conn -> {
                String checkLink = "SELECT 1 FROM animal_owners WHERE animal_id = ? AND owner_id = ?";
                try (PreparedStatement checkPs = conn.prepareStatement(checkLink)) {
                    checkPs.setInt(1, animalId);
                    checkPs.setInt(2, ownerId);
                    try (ResultSet rs = checkPs.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalArgumentException(
                                    "Il proprietario selezionato non risulta associato all'animale.");
                        }
                    }
                }
                setPrimaryOwnerInternal(conn, animalId, ownerId);
            });
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Errore impostazione referente primario: " + e.getMessage(), e);
        }
    }

    private void setPrimaryOwnerInternal(Connection conn, int animalId, int ownerId) throws SQLException {
        String sql = "INSERT INTO animal_primary_contacts (animal_id, owner_id, updated_at) "
                + "VALUES (?, ?, ?) "
                + "ON CONFLICT(animal_id) DO UPDATE SET owner_id = excluded.owner_id, updated_at = excluded.updated_at";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, animalId);
            ps.setInt(2, ownerId);
            ps.setString(3, LocalDateTime.now().toString());
            ps.executeUpdate();
        }
    }

    public List<OwnershipDetail> findOwnershipDetailsByAnimal(int animalId) {
        String sql = "SELECT o.*, ao.ownership_role, "
                + "CASE WHEN apc.owner_id = o.id THEN 1 ELSE 0 END AS is_primary "
                + "FROM owners o "
                + "INNER JOIN animal_owners ao ON o.id = ao.owner_id "
                + "LEFT JOIN animal_primary_contacts apc ON apc.animal_id = ao.animal_id "
                + "WHERE ao.animal_id = ? "
                + "ORDER BY is_primary DESC, o.last_name ASC, o.first_name ASC";
        List<OwnershipDetail> result = new ArrayList<>();
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, animalId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Owner owner = OwnerDAO.mapRow(rs);
                    String role = rs.getString("ownership_role");
                    boolean isPrimary = rs.getInt("is_primary") == 1;
                    result.add(new OwnershipDetail(owner, role, isPrimary));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Errore recupero dettagli comproprietà: " + e.getMessage(), e);
        }
    }

    public Optional<Owner> findPrimaryContact(int animalId) {
        return findOwnershipDetailsByAnimal(animalId).stream()
                .filter(OwnershipDetail::isPrimaryContact)
                .map(OwnershipDetail::getOwner)
                .findFirst();
    }
}
