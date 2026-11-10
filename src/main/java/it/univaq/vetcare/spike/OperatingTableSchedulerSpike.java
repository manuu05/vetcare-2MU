package it.univaq.vetcare.spike;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * SP-01: Spike Architetturale per il controllo di concorrenza sul Tavolo Operatorio unico
 * e sulla disponibilità dei 3 medici veterinari, con riallocazione deterministica (< 200ms).
 */
public class OperatingTableSchedulerSpike {

    private final List<AppointmentSlot> scheduledSlots = new ArrayList<>();

    /**
     * Tenta di pianificare un nuovo slot. Se rileva una collisione sul tavolo operatorio
     * unico o sullo stesso veterinario, solleva ConcurrencyConflictException allegando
     * una proposta di riallocazione deterministica calcolata entro 200ms.
     */
    public synchronized void scheduleSlot(AppointmentSlot candidate) {
        long startNano = System.nanoTime();

        for (AppointmentSlot existing : scheduledSlots) {
            if (candidate.overlapsWith(existing)) {
                boolean tableConflict = candidate.isRequiresOperatingTable() && existing.isRequiresOperatingTable();
                boolean sameVetConflict = candidate.getVetId() == existing.getVetId();

                if (tableConflict || sameVetConflict) {
                    AppointmentSlot proposal = computeDeterministicReallocation(candidate);
                    long elapsedMs = (System.nanoTime() - startNano) / 1_000_000;

                    String reason = tableConflict
                            ? "Conflitto sul Tavolo Operatorio Unico con " + existing.getSlotId()
                            : "Conflitto agenda Veterinario #" + candidate.getVetId() + " con " + existing.getSlotId();

                    throw new ConcurrencyConflictException(
                            reason + ". Proposta riallocazione: " + proposal.getStartTime().toLocalTime()
                                    + " - " + proposal.getEndTime().toLocalTime() + " (Vet #" + proposal.getVetId() + ")",
                            existing,
                            proposal,
                            elapsedMs
                    );
                }
            }
        }

        scheduledSlots.add(candidate);
        scheduledSlots.sort(Comparator.comparing(AppointmentSlot::getStartTime));
    }

    /**
     * Strategia di riallocazione deterministica:
     * 1. Se è una visita ordinaria (non richiede il tavolo operatorio), verifica se un altro
     *    dei 3 veterinari è libero nello stesso orario.
     * 2. Altrimenti sposta lo slot al primo intervallo libero immediatamente successivo.
     */
    public AppointmentSlot computeDeterministicReallocation(AppointmentSlot candidate) {
        Duration duration = candidate.getDuration();

        // Passo 1: se non serve il tavolo operatorio, cerchiamo un altro medico libero allo stesso orario
        if (!candidate.isRequiresOperatingTable()) {
            for (int altVet = 1; altVet <= 3; altVet++) {
                if (altVet != candidate.getVetId()) {
                    AppointmentSlot sameTimeDiffVet = new AppointmentSlot(
                            candidate.getSlotId() + "-REALLOC",
                            altVet,
                            candidate.getPatientName(),
                            candidate.getStartTime(),
                            candidate.getEndTime(),
                            false
                    );
                    if (isSlotFree(sameTimeDiffVet)) {
                        return sameTimeDiffVet;
                    }
                }
            }
        }

        // Passo 2: ricerca del primo slot temporale libero successivo (step di 15 minuti)
        LocalDateTime cursor = candidate.getStartTime().plusMinutes(15);
        for (int i = 0; i < 96; i++) {
            AppointmentSlot shifted = new AppointmentSlot(
                    candidate.getSlotId() + "-REALLOC",
                    candidate.getVetId(),
                    candidate.getPatientName(),
                    cursor,
                    cursor.plus(duration),
                    candidate.isRequiresOperatingTable()
            );
            if (isSlotFree(shifted)) {
                return shifted;
            }
            cursor = cursor.plusMinutes(15);
        }

        throw new IllegalStateException("Nessuno slot libero trovato nelle prossime 24 ore.");
    }

    public boolean isSlotFree(AppointmentSlot candidate) {
        for (AppointmentSlot existing : scheduledSlots) {
            if (candidate.overlapsWith(existing)) {
                boolean tableConflict = candidate.isRequiresOperatingTable() && existing.isRequiresOperatingTable();
                boolean sameVetConflict = candidate.getVetId() == existing.getVetId();
                if (tableConflict || sameVetConflict) {
                    return false;
                }
            }
        }
        return true;
    }

    public List<AppointmentSlot> getScheduledSlots() {
        return Collections.unmodifiableList(scheduledSlots);
    }

    public void clear() {
        scheduledSlots.clear();
    }
}
