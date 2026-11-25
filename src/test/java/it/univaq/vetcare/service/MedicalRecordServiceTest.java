package it.univaq.vetcare.service;

import it.univaq.vetcare.dao.AllergyDAO;
import it.univaq.vetcare.dao.AnimalDAO;
import it.univaq.vetcare.dao.MedicalVisitDAO;
import it.univaq.vetcare.dao.PrescriptionOverrideDAO;
import it.univaq.vetcare.db.DatabaseManager;
import it.univaq.vetcare.model.Animal;
import it.univaq.vetcare.model.MedicalVisit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * US-08 & US-09 (Sub-Task US09-3): Test automatici JUnit 5 BDD sulla consultazione
 * pre-visita della storia clinica / andamento peso e sul consolidamento immutabile della visita.
 */
public class MedicalRecordServiceTest {

    private File tempDbFile;
    private AnimalService animalService;
    private AllergyAlertService allergyAlertService;
    private MedicalRecordService medicalRecordService;
    private Animal patient;

    @BeforeEach
    public void setUp() throws Exception {
        tempDbFile = File.createTempFile("vetcare_test_s2_medrec_", ".db");
        DatabaseManager dbManager = new DatabaseManager("jdbc:sqlite:" + tempDbFile.getAbsolutePath());
        dbManager.initializeSchema();

        animalService = new AnimalService(new AnimalDAO(dbManager));
        allergyAlertService = new AllergyAlertService(new AllergyDAO(dbManager));
        PrescriptionSafetyService safetyService = new PrescriptionSafetyService(
                allergyAlertService, new PrescriptionOverrideDAO(dbManager));
        medicalRecordService = new MedicalRecordService(new MedicalVisitDAO(dbManager), safetyService);

        patient = animalService.registerAnimal(new Animal(
                "Toby", "Cane", "Golden Retriever", LocalDate.of(2020, 2, 10),
                "M", "380260000555666", 28.0, ""));
    }

    @AfterEach
    public void tearDown() {
        if (tempDbFile != null && tempDbFile.exists()) {
            tempDbFile.delete();
        }
    }

    @Test
    public void testConsolidateVisitGeneratesSignatureAndUpdatesPatientWeight() {
        MedicalVisit visit = new MedicalVisit(
                patient.getId(), LocalDateTime.of(2026, 11, 20, 10, 15),
                "Dott.ssa Sara Verdi", "Controllo dermatologico",
                "Prurito auricolare da 3 giorni", "Eritema condotto uditivo esterno",
                29.4, 38.6, "Otite esterna eritematosa", "Gocce otologiche mattina e sera"
        );

        MedicalVisit saved = medicalRecordService.recordAndConsolidateVisit(visit, false, null);

        assertNotNull(saved.getId());
        assertTrue(saved.isConsolidated());
        assertTrue(saved.getDigitalSignature().contains("Dott.ssa Sara Verdi"));

        // Verifica allineamento transazionale del peso sull'anagrafica animale
        Animal updatedAnimal = animalService.findById(patient.getId()).orElseThrow(AssertionError::new);
        assertEquals(29.4, updatedAnimal.getWeight(), 0.01);
    }

    @Test
    public void testConsolidatedVisitCannotBeModified() {
        MedicalVisit saved = medicalRecordService.recordAndConsolidateVisit(new MedicalVisit(
                patient.getId(), LocalDateTime.now(), "Dott. Marco Bianchi",
                "Visita base", "Paziente vigile", "Normale",
                28.2, 38.4, "Soggetto clinicamente sano", ""
        ), false, null);

        // Criterio BDD US-09: stato non modificabile dopo il consolidamento
        assertThrows(ValidationException.class, () ->
                medicalRecordService.attemptModifyVisit(saved.getId(), "Diagnosi alterata a posteriori"));
    }

    @Test
    public void testClinicalTimelineOrderAndWeightProgressionSummary() {
        medicalRecordService.recordAndConsolidateVisit(new MedicalVisit(
                patient.getId(), LocalDateTime.of(2026, 9, 1, 9, 30), "Dott. Bianchi",
                "Prima visita", "Regolare", "Buono stato", 28.0, 38.5, "Controllo ok", ""
        ), false, null);

        medicalRecordService.recordAndConsolidateVisit(new MedicalVisit(
                patient.getId(), LocalDateTime.of(2026, 11, 15, 11, 0), "Dott.ssa Verdi",
                "Richiamo annuale", "Appetito aumentato", "Ottimo tono", 30.5, 38.4, "Idoneo a vaccino", ""
        ), false, null);

        List<MedicalVisit> timeline = medicalRecordService.getClinicalTimeline(patient.getId());
        assertEquals(2, timeline.size());
        assertEquals("Richiamo annuale", timeline.get(0).getReason(), "La visita più recente deve apparire in cima");

        String weightTrend = medicalRecordService.buildWeightProgressionSummary(patient.getId());
        assertTrue(weightTrend.contains("28.0 kg"));
        assertTrue(weightTrend.contains("30.5 kg"));
        assertTrue(weightTrend.contains("+2.5 kg"));
    }

    @Test
    public void testVisitBlocksAllergicTherapyIntegration() {
        allergyAlertService.registerAllergy(patient.getId(), "AMOXICILLINA", "CRITICA", "Orticaria grave");

        MedicalVisit riskyVisit = new MedicalVisit(
                patient.getId(), LocalDateTime.now(), "Dott. Bianchi",
                "Ferita cutanea", "Morso", "Lacerazione", 28.5, 38.9,
                "Infezione tessuti molli", "Amoxicillina 250mg"
        );

        assertThrows(AllergySafetyBlockException.class, () ->
                medicalRecordService.recordAndConsolidateVisit(riskyVisit, false, null));
        assertEquals(0, medicalRecordService.getClinicalTimeline(patient.getId()).size(),
                "Nessuna visita deve essere salvata se il controllo allergia blocca la prescrizione");
    }
}
