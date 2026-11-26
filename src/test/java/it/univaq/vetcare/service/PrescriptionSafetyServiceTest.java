package it.univaq.vetcare.service;

import it.univaq.vetcare.dao.AllergyDAO;
import it.univaq.vetcare.dao.AnimalDAO;
import it.univaq.vetcare.dao.PrescriptionOverrideDAO;
import it.univaq.vetcare.db.DatabaseManager;
import it.univaq.vetcare.model.Animal;
import it.univaq.vetcare.model.PrescriptionOverrideLog;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * US-07 (Sub-Task US07-2): Test di accettazione BDD automatici sul blocco attivo
 * della prescrizione di farmaci incompatibili (es. Amoxicillina su allergia a Penicilline)
 * e sulla tracciatura dell'Override Primario con motivazione clinica.
 */
public class PrescriptionSafetyServiceTest {

    private File tempDbFile;
    private Animal allergicDog;
    private PrescriptionSafetyService safetyService;

    @BeforeEach
    public void setUp() throws Exception {
        tempDbFile = File.createTempFile("vetcare_test_s2_us07_", ".db");
        DatabaseManager dbManager = new DatabaseManager("jdbc:sqlite:" + tempDbFile.getAbsolutePath());
        dbManager.initializeSchema();

        AnimalService animalService = new AnimalService(new AnimalDAO(dbManager));
        AllergyAlertService allergyAlertService = new AllergyAlertService(new AllergyDAO(dbManager));
        PrescriptionOverrideDAO overrideDAO = new PrescriptionOverrideDAO(dbManager);
        safetyService = new PrescriptionSafetyService(allergyAlertService, overrideDAO);

        allergicDog = animalService.registerAnimal(new Animal(
                "Rex", "Cane", "Pastore Tedesco", LocalDate.of(2021, 5, 14),
                "M", "380260000777888", 32.0, ""));
        allergyAlertService.registerAllergy(allergicDog.getId(), "PENICILLINE", "CRITICA", "Shock anafilattico");
    }

    @AfterEach
    public void tearDown() {
        if (tempDbFile != null && tempDbFile.exists()) {
            tempDbFile.delete();
        }
    }

    @Test
    public void testAmoxicillinPrescriptionIsBlockedWithoutOverride() {
        // Dato un cane con allergia registrata a Penicilline
        // Quando il veterinario tenta di aggiungere in terapia 'Amoxicillina 500mg' senza override
        // Allora il sistema solleva un blocco di sicurezza attivo
        AllergySafetyBlockException ex = assertThrows(AllergySafetyBlockException.class, () ->
                safetyService.validateOrAuditPrescription(
                        allergicDog.getId(),
                        "Amoxicillina + Acido Clavulanico 500mg ogni 12h",
                        "Dott. Marco Bianchi",
                        false,
                        null
                ));
        assertEquals("PENICILLINE", ex.getMatchedAllergen());
        assertTrue(ex.getMessage().contains("BLOCCO SICUREZZA ALLERGIA CRITICA"));
    }

    @Test
    public void testPrimaryOverrideWithoutDetailedJustificationIsRejected() {
        assertThrows(ValidationException.class, () ->
                safetyService.validateOrAuditPrescription(
                        allergicDog.getId(),
                        "Amoxicillina iniettabile",
                        "Dott. Marco Bianchi",
                        true,
                        "ok" // motivazione troppo corta / non valida
                ));
    }

    @Test
    public void testPrimaryOverrideWithValidClinicalJustificationIsAllowedAndLogged() {
        // Quando il medico conferma l'Override Primario con motivazione clinica documentata
        safetyService.validateOrAuditPrescription(
                allergicDog.getId(),
                "Amoxicillina EV",
                "Dott. Marco Bianchi",
                true,
                "Sepsi acuta refrattaria in sala operatoria sotto copertura antistaminica/cortisonica"
        );

        // Allora l'override viene registrato in modo indelebile nell'audit log
        List<PrescriptionOverrideLog> logs = safetyService.getOverrideHistory(allergicDog.getId());
        assertEquals(1, logs.size());
        assertEquals("Dott. Marco Bianchi", logs.get(0).getVeterinarianName());
        assertEquals("PENICILLINE", logs.get(0).getMatchedAllergen());
    }

    @Test
    public void testCompatibleDrugPassesFreely() {
        assertDoesNotThrow(() ->
                safetyService.validateOrAuditPrescription(
                        allergicDog.getId(),
                        "Doxiciclina 100mg 1 compressa al giorno",
                        "Dott.ssa Sara Verdi",
                        false,
                        null
                ));
        assertTrue(safetyService.getOverrideHistory(allergicDog.getId()).isEmpty());
    }
}
