package it.univaq.vetcare.spike;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * SP-01 & RF-01 (Refactoring post Sprint 1 Review):
 * - Rimossa la duplicazione del controllo collisioni tramite Extract Method (findConflictingSlot).
 * - Disaccoppiato il lancio dell'eccezione dal calcolo della riallocazione: scheduleSlot()
 *   si limita a segnalare il conflitto, mentre computeDeterministicReallocation() viene
 *   invocato esplicitamente nel blocco try/catch del chiamante.
 */
public class OperatingTableSchedulerSpike {

    private final List<AppointmentSlot> scheduledSlots = new ArrayList<>();

    /**
     * Tenta di pianificare uno slot. Se rileva una collisione, solleva ConcurrencyConflictException
     * senza eseguire calcoli collaterali di riallocazione.
     */
    public synchronized void scheduleSlot(AppointmentSlot candidate) {
        Optional<AppointmentSlot> conflictOpt = findConflictingSlot(candidate);
        if (conflictOpt.isPresent()) {
            AppointmentSlot existing = conflictOpt.get();
            boolean tableConflict = candidate.isRequiresOperatingTable() && existing.isRequiresOperatingTable();
            String reason = tableConflict
                    ? "Conflitto sul Tavolo Operatorio Unico con " + existing.getSlotId()
                    : "Conflitto agenda Veterinario #" + candidate.getVetId() + " con " + existing.getSlotId();
            throw new ConcurrencyConflictException(reason, existing);
        }

        scheduledSlots.add(candidate);
        scheduledSlots.sort(Comparator.comparing(AppointmentSlot::getStartTime));
    }

    /**
     * Unico punto centralizzato per la verifica delle collisioni (tavolo unico o stesso veterinario).
     */
    public Optional<AppointmentSlot> findConflictingSlot(AppointmentSlot candidate) {
        for (AppointmentSlot existing : scheduledSlots) {
            if (candidate.overlapsWith(existing)) {
                boolean tableConflict = candidate.isRequiresOperatingTable() && existing.isRequiresOperatingTable();
                boolean sameVetConflict = candidate.getVetId() == existing.getVetId();
                if (tableConflict || sameVetConflict) {
                    return Optional.of(existing);
                }
            }
        }
        return Optional.empty();
    }

    public boolean isSlotFree(AppointmentSlot candidate) {
        return !findConflictingSlot(candidate).isPresent();
    }

    /**
     * Da invocare esplicitamente nel blocco catch quando si intercetta ConcurrencyConflictException:
     * 1. Per visite ordinarie, cerca prima un altro veterinario libero nello stesso orario.
     * 2. Altrimenti cerca il primo intervallo temporale libero successivo (step di 15 min).
     */
    public AppointmentSlot computeDeterministicReallocation(AppointmentSlot candidate) {
        if (!candidate.isRequiresOperatingTable()) {
            for (int altVet = 1; altVet <= 3; altVet++) {
                if (altVet != candidate.getVetId()) {
                    AppointmentSlot sameTimeAltVet = candidate.rescheduleTo(altVet, candidate.getStartTime());
                    if (isSlotFree(sameTimeAltVet)) {
                        return sameTimeAltVet;
                    }
                }
            }
        }

        LocalDateTime cursor = candidate.getStartTime().plusMinutes(15);
        for (int i = 0; i < 96; i++) {
            AppointmentSlot shifted = candidate.rescheduleTo(candidate.getVetId(), cursor);
            if (isSlotFree(shifted)) {
                return shifted;
            }
            cursor = cursor.plusMinutes(15);
        }

        throw new IllegalStateException("Nessuno slot libero trovato nelle prossime 24 ore.");
    }

    public List<AppointmentSlot> getScheduledSlots() {
        return Collections.unmodifiableList(scheduledSlots);
    }

    public void clear() {
        scheduledSlots.clear();
    }
}
