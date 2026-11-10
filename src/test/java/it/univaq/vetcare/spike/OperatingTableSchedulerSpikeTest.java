package it.univaq.vetcare.spike;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SP-01 (Sub-Task SP01-2): Test del prototipo Spike per rilevamento collisioni orarie
 * su Tavolo Operatorio unico e 3 Veterinari con benchmark < 200ms.
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
    public void testConflictBetweenSurgeryAndOrdinaryVisitOnSameVet() {
        // Dato il Veterinario #1 impegnato in chirurgia dalle 09:00 alle 10:00
        AppointmentSlot surgery = new AppointmentSlot(
                "CHIR-01", 1, "Rex", baseTime, baseTime.plusHours(1), true
        );
        scheduler.scheduleSlot(surgery);

        // Quando si simula la sovrapposizione con una visita ordinaria dalle 09:30 alle 10:00 sullo stesso Vet #1
        AppointmentSlot overlappingVisit = new AppointmentSlot(
                "VIS-01", 1, "Luna", baseTime.plusMinutes(30), baseTime.plusHours(1), false
        );

        // Allora il motore solleva ConcurrencyConflictException e propone una riallocazione entro 200ms
        ConcurrencyConflictException ex = assertThrows(ConcurrencyConflictException.class, () -> {
            scheduler.scheduleSlot(overlappingVisit);
        });

        assertNotNull(ex.getProposedReallocation(), "Deve essere proposta una riallocazione deterministica");
        assertTrue(ex.getResolutionTimeMs() < 200, "Il tempo di risoluzione deve essere inferiore a 200ms");
        assertEquals(2, ex.getProposedReallocation().getVetId(),
                "La visita ordinaria viene riallocata sul Veterinario #2 libero nello stesso orario");
    }

    @Test
    public void testSingleOperatingTableExclusiveLockAcrossDifferentVets() {
        // Dato il Tavolo Operatorio unico occupato dal Veterinario #1 (09:00 - 10:30)
        AppointmentSlot surgeryVet1 = new AppointmentSlot(
                "CHIR-01", 1, "Rex", baseTime, baseTime.plusMinutes(90), true
        );
        scheduler.scheduleSlot(surgeryVet1);

        // Quando il Veterinario #2 tenta di prenotare il Tavolo Operatorio unico alle 10:00
        AppointmentSlot surgeryVet2 = new AppointmentSlot(
                "CHIR-02", 2, "Milo", baseTime.plusMinutes(60), baseTime.plusMinutes(120), true
        );

        ConcurrencyConflictException ex = assertThrows(ConcurrencyConflictException.class, () -> {
            scheduler.scheduleSlot(surgeryVet2);
        });

        assertEquals(baseTime.plusMinutes(90), ex.getProposedReallocation().getStartTime(),
                "Il secondo intervento chirurgico deve essere posticipato alle 10:30 (fine del primo)");
        assertTrue(ex.getResolutionTimeMs() < 200, "Risoluzione garantita entro 200ms");
    }

    @RepeatedTest(5)
    public void testRepeatedBenchmarkCollisionDetectionUnder200ms() {
        // Test ripetuto (@RepeatedTest come da slide JUnit) per verificare stabilità e performance < 200ms
        scheduler.scheduleSlot(new AppointmentSlot("S1", 1, "Paziente1", baseTime, baseTime.plusHours(1), true));
        scheduler.scheduleSlot(new AppointmentSlot("S2", 2, "Paziente2", baseTime, baseTime.plusHours(1), false));

        AppointmentSlot colliding = new AppointmentSlot(
                "S3", 1, "PazienteUrgente", baseTime.plusMinutes(15), baseTime.plusMinutes(45), true
        );

        ConcurrencyConflictException ex = assertThrows(ConcurrencyConflictException.class, () -> {
            scheduler.scheduleSlot(colliding);
        });
        assertTrue(ex.getResolutionTimeMs() < 200, "Benchmark collisione deve restare < 200ms in ogni esecuzione");
    }
}
