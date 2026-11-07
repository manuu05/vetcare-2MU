package it.univaq.vetcare.dao;

import it.univaq.vetcare.db.DatabaseManager;
import it.univaq.vetcare.model.Animal;
import it.univaq.vetcare.model.Owner;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/*
 * TK-02: Repository per la relazione N:M (Molti-a-Molti) tra Animali e Proprietari
 * con supporto transazionale e navigabilità bidirezionale.
 */
public class AnimalOwnerDAO {

    private final DatabaseManager dbManager;

    public AnimalOwnerDAO(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    /*
     * Associa un proprietario a un animale nella tabella junction animal_owners.
     */
    public void linkAnimalToOwner(int animalId, int ownerId, String role) {
        String sql = "INSERT OR IGNORE INTO animal_owners (animal_id, owner_id, ownership_role) VALUES (?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, animalId);
            ps.setInt(2, ownerId);
            ps.setString(3, role != null ? role : "Comproprietario");
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Errore associazione N:M Animale-Proprietario: " + e.getMessage(), e);
        }
    }

    /*
     * Associa più proprietari a un animale all'interno di un'unica transazione
     * atomica.
     */
    public void linkMultipleOwnersTransactionally(int animalId, List<Integer> ownerIds, String role) throws Exception {
        dbManager.executeInTransaction(conn -> {
            String sql = "INSERT INTO animal_owners (animal_id, owner_id, ownership_role) VALUES (?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (Integer ownerId : ownerIds) {
                    ps.setInt(1, animalId);
                    ps.setInt(2, ownerId);
                    ps.setString(3, role != null ? role : "Comproprietario");
                    ps.executeUpdate();
                }
            }
        });
    }

    /*
     * Navigabilità Animale -> Proprietari (un animale può avere più proprietari).
     */
    public List<Owner> findOwnersByAnimalId(int animalId) {
        String sql = "SELECT o.* FROM owners o "
                + "INNER JOIN animal_owners ao ON o.id = ao.owner_id "
                + "WHERE ao.animal_id = ? ORDER BY o.last_name, o.first_name";
        List<Owner> owners = new ArrayList<>();
        try (Connection conn = dbManager.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, animalId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    owners.add(OwnerDAO.mapRow(rs));
                }
            }
            return owners;
        } catch (SQLException e) {
            throw new RuntimeException("Errore navigazione Animale -> Proprietari: " + e.getMessage(), e);
        }
    }

    /*
     * Navigabilità Proprietario -> Animali (un proprietario può avere più animali).
     */
    public List<Animal> findAnimalsByOwnerId(int ownerId) {
        String sql = "SELECT a.* FROM animals a "
                + "INNER JOIN animal_owners ao ON a.id = ao.animal_id "
                + "WHERE ao.owner_id = ? ORDER BY a.name";
        List<Animal> animals = new ArrayList<>();
        try (Connection conn = dbManager.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ownerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    animals.add(AnimalDAO.mapRow(rs));
                }
            }
            return animals;
        } catch (SQLException e) {
            throw new RuntimeException("Errore navigazione Proprietario -> Animali: " + e.getMessage(), e);
        }
    }
}
