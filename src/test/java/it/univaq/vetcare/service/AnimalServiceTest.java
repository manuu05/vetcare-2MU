package it.univaq.vetcare.service;

import it.univaq.vetcare.dao.AnimalDAO;
import it.univaq.vetcare.db.DatabaseManager;
import it.univaq.vetcare.model.Animal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * US-01 (Sub-Task US01-3): Unit & Integration Test JUnit 5 per validazione
 * anagrafica animale, microchip a 15 cifre ISO e blocco duplicati a database.
 */
public class AnimalServiceTest {

    private File tempDbFile;
    private AnimalService animalService;

    @BeforeEach
    public void setUp() throws Exception {
        tempDbFile = File.createTempFile("vetcare_test_us01_", ".db");
        DatabaseManager dbManager = new DatabaseManager("jdbc:sqlite:" + tempDbFile.getAbsolutePath());
        dbManager.initializeSchema();
        animalService = new AnimalService(new AnimalDAO(dbManager));
    }

    @AfterEach
    public void tearDown() {
        if (tempDbFile != null && tempDbFile.exists()) {
            tempDbFile.delete();
        }
    }

    @Test
    public void testSaveValidAnimalGeneratesUniqueMedicalRecordId() {
        // Dato che la segretaria inserisce tutti i campi obbligatori e un microchip valido a 15 cifre
        Animal rocky = new Animal(
                "Rocky", "Cane", "Boxer",
                LocalDate.of(2022, 6, 15), "M",
                "380260000999888", 28.5, "Sano"
        );

        // Quando salva la scheda
        Animal saved = animalService.registerAnimal(rocky);

        // Allora il sistema salva la scheda paziente e genera l'ID cartella clinica univoco
        assertNotNull(saved.getId(), "L'ID database deve essere generato");
        assertNotNull(saved.getMedicalRecordCode(), "Il codice univoco della cartella clinica deve essere generato");
        assertTrue(saved.getMedicalRecordCode().startsWith("VC-2026-"), "Formato ID cartella clinica conforme");
        assertEquals("380260000999888", saved.getMicrochip());
    }

    @Test
    public void testDuplicateMicrochipIsBlockedWithError() {
        // Dato un microchip già presente a sistema per un altro animale
        Animal first = new Animal(
                "Fido", "Cane", "Beagle",
                LocalDate.of(2021, 3, 10), "M",
                "380260123456789", 14.0, ""
        );
        animalService.registerAnimal(first);

        // Quando la segretaria tenta il salvataggio di un altro animale con lo stesso microchip
        Animal duplicate = new Animal(
                "Pluto", "Cane", "Dalmata",
                LocalDate.of(2023, 1, 20), "M",
                "380260123456789", 22.0, ""
        );

        // Allora il sistema blocca l'operazione segnalando l'errore di duplicato
        ValidationException ex = assertThrows(ValidationException.class, () -> {
            animalService.registerAnimal(duplicate);
        });
        assertTrue(ex.getMessage().toLowerCase().contains("duplicato"),
                "Il messaggio d'errore deve segnalare il microchip duplicato");
    }

    @Test
    public void testInvalidMicrochipLengthOrCharactersThrowsException() {
        Animal invalidChip = new Animal(
                "Micio", "Gatto", "Persiano",
                LocalDate.of(2023, 2, 1), "M",
                "12345ABC", 4.0, ""
        );

        assertThrows(ValidationException.class, () -> {
            animalService.registerAnimal(invalidChip);
        }, "Microchip non conforme alle 15 cifre numeriche ISO deve essere rifiutato");
    }
}
