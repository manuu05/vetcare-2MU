package it.univaq.vetcare.db;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TK-01 (Sub-Task TK01-3): Test automatici di integrazione per connessione DBMS,
 * migrazione DDL, vincoli di integrità referenziale e rollback transazionale.
 */
public class DatabaseIntegrationTest {

    private File tempDbFile;
    private DatabaseManager dbManager;

    @BeforeEach
    public void setUp() throws Exception {
        tempDbFile = File.createTempFile("vetcare_test_tk01_", ".db");
        dbManager = new DatabaseManager("jdbc:sqlite:" + tempDbFile.getAbsolutePath());
        dbManager.initializeSchema();
    }

    @AfterEach
    public void tearDown() {
        if (tempDbFile != null && tempDbFile.exists()) {
            tempDbFile.delete();
        }
    }

    private int countAnimals() throws SQLException {
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM animals")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    @Test
    public void testDatabaseConnectionAndSchemaInitialization() throws SQLException {
        // Dato l'ambiente configurato e lo script di migrazione eseguito
        try (Connection conn = dbManager.getConnection()) {
            assertNotNull(conn, "La connessione JDBC non deve essere null");
            assertFalse(conn.isClosed(), "La connessione deve essere aperta");
        }
        assertEquals(0, countAnimals(), "La tabella animals deve essere inizialmente vuota");
    }

    @Test
    public void testForeignKeyConstraintEnforcement() {
        // Verifica che PRAGMA foreign_keys = ON blocchi riferimenti ad animali inesistenti
        assertThrows(SQLException.class, () -> {
            try (Connection conn = dbManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "INSERT INTO animal_allergies (animal_id, substance, severity) VALUES (9999, 'PENICILLINE', 'CRITICA')")) {
                ps.executeUpdate();
            }
        }, "Il DBMS deve sollevare SQLException se si viola il vincolo di chiave esterna");
    }

    @Test
    public void testTransactionalRollbackOnError() throws SQLException {
        // Quando una transazione fallisce a metà, nessuna modifica deve essere persistita
        assertThrows(RuntimeException.class, () -> {
            dbManager.executeInTransaction(conn -> {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO animals (medical_record_code, name, species, breed, birth_date, sex, microchip, weight, notes) "
                      + "VALUES ('VC-TEST-01', 'Bobi', 'Cane', 'Meticcio', '2022-01-01', 'M', '380260000000111', 15.0, '')")) {
                    ps.executeUpdate();
                }
                // Forziamo un errore durante la stessa transazione
                throw new RuntimeException("Errore simulato prima del commit");
            });
        });

        assertEquals(0, countAnimals(), "Il rollback transazionale deve aver annullato l'inserimento parziale");
    }
}
