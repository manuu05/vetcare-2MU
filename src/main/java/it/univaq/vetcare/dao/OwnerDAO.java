package it.univaq.vetcare.dao;

import it.univaq.vetcare.db.DatabaseManager;
import it.univaq.vetcare.model.Owner;
import it.univaq.vetcare.model.PreferredChannel;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/*
 * US-02: Data Access Object per la gestione anagrafica dei Proprietari.
 */
public class OwnerDAO {

    private final DatabaseManager dbManager;

    public OwnerDAO(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public Owner insert(Owner owner) {
        String sql = "INSERT INTO owners (first_name, last_name, fiscal_code, phone, email, preferred_channel) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, owner.getFirstName());
            ps.setString(2, owner.getLastName());
            ps.setString(3, owner.getFiscalCode());
            ps.setString(4, owner.getPhone());
            ps.setString(5, owner.getEmail());
            ps.setString(6, owner.getPreferredChannel().name());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    owner.setId(rs.getInt(1));
                }
            }
            return owner;
        } catch (SQLException e) {
            throw new RuntimeException("Errore DB durante il salvataggio del proprietario: " + e.getMessage(), e);
        }
    }

    public void updateContactInfo(int ownerId, String phone, String email, PreferredChannel channel) {
        String sql = "UPDATE owners SET phone = ?, email = ?, preferred_channel = ? WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, phone);
            ps.setString(2, email);
            ps.setString(3, channel.name());
            ps.setInt(4, ownerId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Errore aggiornamento recapiti proprietario: " + e.getMessage(), e);
        }
    }

    public Optional<Owner> findByFiscalCode(String fiscalCode) {
        String sql = "SELECT * FROM owners WHERE fiscal_code = ?";
        try (Connection conn = dbManager.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, fiscalCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Errore ricerca proprietario per CF: " + e.getMessage(), e);
        }
    }

    public List<Owner> findAll() {
        String sql = "SELECT * FROM owners ORDER BY last_name, first_name";
        List<Owner> list = new ArrayList<>();
        try (Connection conn = dbManager.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Errore recupero lista proprietari: " + e.getMessage(), e);
        }
    }

    static Owner mapRow(ResultSet rs) throws SQLException {
        return new Owner(
                rs.getInt("id"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("fiscal_code"),
                rs.getString("phone"),
                rs.getString("email"),
                PreferredChannel.fromString(rs.getString("preferred_channel")));
    }
}
