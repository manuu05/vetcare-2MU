package it.univaq.vetcare.service;

import it.univaq.vetcare.dao.AnimalDAO;
import it.univaq.vetcare.dao.AppointmentDAO;
import it.univaq.vetcare.db.DatabaseManager;
import it.univaq.vetcare.model.Animal;
import it.univaq.vetcare.model.ClinicAppointment;
import it.univaq.vetcare.model.VisitType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * US-11 (Sub-Task US11-3): Test automatici JUnit 5 BDD sull'agenda giornaliera
 * e settimanale affiancata per i 3 veterinari con gestione conflitti tramite try/catch.
 */
public class ClinicAgendaServiceTest {

    private File tempDbFile;
    private ClinicAgendaService agendaService;
    private Animal rex;
    private Animal luna;

    @BeforeEach
    public void setUp() throws Exception {
        tempDbFile = File.createTempFile("vetcare_test_s2_us11_", ".db");
        DatabaseManager dbManager = new DatabaseManager("jdbc:sqlite:" + tempDbFile.getAbsolutePath());
        dbManager.initializeSchema();

        AnimalService animalService = new AnimalService(new AnimalDAO(dbManager));
        agendaService = new ClinicAgendaService(new AppointmentDAO(dbManager));

        rex = animalService.registerAnimal(new Animal(
                "Rex", "Cane", "Pastore Tedesco", LocalDate.of(2021, 5, 14),
                "M", "380260000121212", 32.0, ""));
        luna = animalService.registerAnimal(new Animal(
                "Luna", "Gatto", "Europeo", LocalDate.of(2023, 3, 10),
                "F", "380260000343434", 4.2, ""));
    }

    @AfterEach
    public void tearDown() {
        if (tempDbFile != null && tempDbFile.exists()) {
            tempDbFile.delete();
        }
    }

    @Test
    public void testSpecialistVisit45MinBlocksOverlappingSlotAndSuggestsInCatch() {
        LocalDate day = LocalDate.of(2026, 11, 25);
        ClinicAppointment booked = agendaService.bookAppointment(
                rex.getId(), 1, VisitType.VISITA_SPECIALISTICA, day.atTime(10, 0), "Controllo cardiologico");

        assertEquals(day.atTime(10, 45), booked.getEndDateTime());

        LocalDateTime suggestedSlot = null;
        try {
            agendaService.bookAppointment(luna.getId(), 1, VisitType.VISITA_BASE, day.atTime(10, 30), "");
            fail("Attesa ValidationException per sovrapposizione sullo stesso veterinario");
        } catch (ValidationException conflictEx) {
            assertTrue(conflictEx.getMessage().contains("CONFLITTO AGENDA VETERINARIO"));
            suggestedSlot = agendaService.suggestNextAvailableSlot(1, day.atTime(10, 30), VisitType.VISITA_BASE);
        }

        assertEquals(day.atTime(10, 45), suggestedSlot,
                "Nel blocco catch deve essere suggerito il primo slot libero alle 10:45");
    }

    @Test
    public void testDifferentVeterinariansCanVisitSimultaneously() {
        LocalDate day = LocalDate.of(2026, 11, 25);
        agendaService.bookAppointment(rex.getId(), 1, VisitType.VISITA_SPECIALISTICA, day.atTime(10, 0), "");

        assertDoesNotThrow(() ->
                agendaService.bookAppointment(luna.getId(), 2, VisitType.PROFILASSI_VACCINO, day.atTime(10, 15), ""));
        assertEquals(1, agendaService.getDailyAgendaForVet(day, 1).size());
        assertEquals(1, agendaService.getDailyAgendaForVet(day, 2).size());
        assertEquals(0, agendaService.getDailyAgendaForVet(day, 3).size());
    }

    @Test
    public void testBackToBackAdjacentAppointmentsOnSameVetAreAllowed() {
        LocalDate day = LocalDate.of(2026, 11, 25);
        agendaService.bookAppointment(rex.getId(), 1, VisitType.VISITA_SPECIALISTICA, day.atTime(10, 0), "10:00-10:45");

        ClinicAppointment next = assertDoesNotThrow(() ->
                agendaService.bookAppointment(luna.getId(), 1, VisitType.PROFILASSI_VACCINO, day.atTime(10, 45), "10:45-11:05"));
        assertEquals(day.atTime(11, 5), next.getEndDateTime());
    }

    @Test
    public void testWeeklyAgendaAggregationAcrossSevenDays() {
        LocalDate monday = LocalDate.of(2026, 11, 23);
        agendaService.bookAppointment(rex.getId(), 1, VisitType.VISITA_BASE, LocalDateTime.of(monday, LocalTime.of(9, 0)), "");
        agendaService.bookAppointment(luna.getId(), 3, VisitType.PROFILASSI_VACCINO, LocalDateTime.of(monday.plusDays(4), LocalTime.of(16, 0)), "");

        List<ClinicAppointment> weekly = agendaService.getWeeklyAgenda(monday);
        assertEquals(2, weekly.size());
    }
}
