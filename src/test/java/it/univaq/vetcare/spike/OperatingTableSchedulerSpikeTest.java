package it.univaq.vetcare.spike;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SP-01 & RF-01: Test del prototipo Spike aggiornati dopo il refactoring:
 * verifica la gestione pulita tramite blocco try/catch separando l'eccezione
 * dal metodo di rescheduling deterministico (< 200ms).
 */
public class OperatingTableSchedulerSpikeTest {

    private OperatingTableSchedulerSpike scheduler;
    private LocalDateTime baseTime;

    @BeforeEach
    public void setUp() {
        scheduler = new OperatingTableSchedulerSpike();
        baseTime = LocalDateTime.of(2026, 11, 10, 9, 0);
    }

    @Test
    public void testConflictBetweenSurgeryAndOrdinaryVisitHandledWithTryCatch() {
        AppointmentSlot surgery = new AppointmentSlot(
                "CHIR-01", 1, "Rex", baseTime, baseTime.plusHours(1), true
        );
        scheduler.scheduleSlot(surgery);

        AppointmentSlot overlappingVisit = new AppointmentSlot(
                "VIS-01", 1, "Luna", baseTime.plusMinutes(30), baseTime.plusHours(1), false
        );

        long startNs = System.nanoTime();
        AppointmentSlot proposal = null;
        try {
            scheduler.scheduleSlot(overlappingVisit);
            fail("Doveva essere sollevata ConcurrencyConflictException");
        } catch (ConcurrencyConflictException ex) {
            assertEquals("CHIR-01", ex.getConflictingSlot().getSlotId());
            proposal = scheduler.computeDeterministicReallocation(overlappingVisit);
        }
        long elapsedMs = (System.nanoTime() - startNs) / 1_000_000L;

        assertNotNull(proposal, "Il blocco catch deve ottenere una proposta di riallocazione valida");
        assertTrue(elapsedMs < 200, "Rilevamento e riallocazione entro 200ms");
        assertEquals(2, proposal.getVetId(),
                "La visita ordinaria viene riallocata sul Veterinario #2 libero nello stesso orario");
    }

    @Test
    public void testSingleOperatingTableExclusiveLockHandledWithTryCatch() {
        AppointmentSlot surgeryVet1 = new AppointmentSlot(
                "CHIR-01", 1, "Rex", baseTime, baseTime.plusMinutes(90), true
        );
        scheduler.scheduleSlot(surgeryVet1);

        AppointmentSlot surgeryVet2 = new AppointmentSlot(
                "CHIR-02", 2, "Milo", baseTime.plusMinutes(60), baseTime.plusMinutes(120), true
        );

        AppointmentSlot proposal = null;
        try {
            scheduler.scheduleSlot(surgeryVet2);
            fail("Doveva essere sollevata ConcurrencyConflictException sul tavolo operatorio unico");
        } catch (ConcurrencyConflictException ex) {
            assertEquals("CHIR-01", ex.getConflictingSlot().getSlotId());
            proposal = scheduler.computeDeterministicReallocation(surgeryVet2);
        }

        assertNotNull(proposal);
        assertEquals(baseTime.plusMinutes(90), proposal.getStartTime(),
                "Il secondo intervento chirurgico viene posticipato alle 10:30 (fine del primo)");
    }

    @RepeatedTest(5)
    public void testRepeatedBenchmarkCollisionAndTryCatchReschedulingUnder200ms() {
        scheduler.scheduleSlot(new AppointmentSlot("S1", 1, "Paziente1", baseTime, baseTime.plusHours(1), true));
        scheduler.scheduleSlot(new AppointmentSlot("S2", 2, "Paziente2", baseTime, baseTime.plusHours(1), false));

        AppointmentSlot colliding = new AppointmentSlot(
                "S3", 1, "PazienteUrgente", baseTime.plusMinutes(15), baseTime.plusMinutes(45), true
        );

        long t0 = System.nanoTime();
        try {
            scheduler.scheduleSlot(colliding);
            fail("Atteso conflitto");
        } catch (ConcurrencyConflictException ex) {
            AppointmentSlot reallocated = scheduler.computeDeterministicReallocation(colliding);
            assertNotNull(reallocated);
        }
        long elapsedMs = (System.nanoTime() - t0) / 1_000_000L;
        assertTrue(elapsedMs < 200, "Benchmark collisione + rescheduling nel catch deve restare < 200ms");
    }
}
