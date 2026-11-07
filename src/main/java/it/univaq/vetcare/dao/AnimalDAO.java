package it.univaq.vetcare.dao;

import it.univaq.vetcare.db.DatabaseManager;
import it.univaq.vetcare.model.Animal;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * US-01: Data Access Object per la persistenza dell'anagrafica animale.
 */
public class AnimalDAO {

    private final DatabaseManager dbManager;

    public AnimalDAO(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public Animal insert(Animal animal) {
        String sql = "INSERT INTO animals (medical_record_code, name, species, breed, birth_date, sex, microchip, weight, notes) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, animal.getMedicalRecordCode());
            ps.setString(2, animal.getName());
            ps.setString(3, animal.getSpecies());
            ps.setString(4, animal.getBreed());
            ps.setString(5, animal.getBirthDate().toString());
            ps.setString(6, animal.getSex());
            ps.setString(7, animal.getMicrochip());
            ps.setDouble(8, animal.getWeight());
            ps.setString(9, animal.getNotes());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    animal.setId(rs.getInt(1));
                }
            }
            return animal;
        } catch (SQLException e) {
            throw new RuntimeException("Errore DB durante il salvataggio dell'animale: " + e.getMessage(), e);
        }
    }

    public Optional<Animal> findByMicrochip(String microchip) {
        String sql = "SELECT * FROM animals WHERE microchip = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, microchip);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Errore ricerca per microchip: " + e.getMessage(), e);
        }
    }

    public Optional<Animal> findById(int id) {
        String sql = "SELECT * FROM animals WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Errore ricerca animale per ID: " + e.getMessage(), e);
        }
    }

    public List<Animal> findAll() {
        String sql = "SELECT * FROM animals ORDER BY id ASC";
        List<Animal> list = new ArrayList<>();
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Errore recupero lista animali: " + e.getMessage(), e);
        }
    }

    public int countAll() {
        String sql = "SELECT COUNT(*) FROM animals";
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new RuntimeException("Errore conteggio animali: " + e.getMessage(), e);
        }
    }

    static Animal mapRow(ResultSet rs) throws SQLException {
        return new Animal(
                rs.getInt("id"),
                rs.getString("medical_record_code"),
                rs.getString("name"),
                rs.getString("species"),
                rs.getString("breed"),
                LocalDate.parse(rs.getString("birth_date")),
                rs.getString("sex"),
                rs.getString("microchip"),
                rs.getDouble("weight"),
                rs.getString("notes")
        );
    }
}
