package it.univaq.vetcare.db;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

/**
 * TK-01 & Sprint 2 Migration: Gestore centrale della connessione al DBMS relazionale,
 * esecuzione sequenziale degli script di migrazione DDL (schema.sql + schema_sprint2.sql)
 * e gestione delle transazioni atomiche con rollback.
 */
public class DatabaseManager {

    private static final String[] MIGRATION_SCRIPTS = {
            "schema.sql",
            "schema_sprint2.sql"
    };

    private final String jdbcUrl;

    @FunctionalInterface
    public interface TransactionalOperation {
        void execute(Connection connection) throws Exception;
    }

    public DatabaseManager() {
        this("jdbc:sqlite:vetcare.db");
    }

    public DatabaseManager(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    /**
     * Apre una connessione al DBMS attivando i vincoli di integrità referenziale.
     */
    public Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(jdbcUrl);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
        }
        return conn;
    }

    /**
     * Esegue gli script di migrazione DDL presenti nel classpath (Sprint 1 + Sprint 2).
     */
    public void initializeSchema() {
        try (Connection conn = getConnection()) {
            for (String resourceName : MIGRATION_SCRIPTS) {
                try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourceName)) {
                    if (is == null) {
                        if ("schema.sql".equals(resourceName)) {
                            throw new IllegalStateException("File schema.sql non trovato nel classpath.");
                        }
                        continue;
                    }
                    executeSqlStream(conn, is);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Errore durante l'inizializzazione dello schema DB: " + e.getMessage(), e);
        }
    }

    private void executeSqlStream(Connection conn, InputStream is) throws Exception {
        String sqlScript;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            sqlScript = reader.lines()
                    .filter(line -> !line.trim().startsWith("--"))
                    .collect(Collectors.joining("\n"));
        }
        String[] statements = sqlScript.split(";");
        try (Statement stmt = conn.createStatement()) {
            for (String statement : statements) {
                String trimmed = statement.trim();
                if (!trimmed.isEmpty()) {
                    stmt.execute(trimmed);
                }
            }
        }
    }

    /**
     * Esegue un'operazione all'interno di una transazione atomica.
     * In caso di errore esegue il rollback completo garantendo l'integrità dei dati.
     */
    public void executeInTransaction(TransactionalOperation operation) throws Exception {
        try (Connection conn = getConnection()) {
            boolean previousAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                operation.execute(conn);
                conn.commit();
            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(previousAutoCommit);
            }
        }
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }
}
