package it.univaq.vetcare.spike;

import java.time.Duration;
import java.time.LocalDateTime;

/*SP-01: Modello matematico intervallare di uno slot in agenda
(visita ordinaria o intervento chirurgico sul tavolo operatorio unico).*/
public class AppointmentSlot {

    private final String slotId;
    private final int vetId; // Veterinario 1, 2 o 3
    private final String patientName;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final boolean requiresOperatingTable;

    public AppointmentSlot(String slotId, int vetId, String patientName,
            LocalDateTime startTime, LocalDateTime endTime,
            boolean requiresOperatingTable) {
        if (vetId < 1 || vetId > 3) {
            throw new IllegalArgumentException("L'ambulatorio VetCare dispone di 3 veterinari (ID 1..3).");
        }
        if (startTime == null || endTime == null || !endTime.isAfter(startTime)) {
            throw new IllegalArgumentException(
                    "Intervallo temporale non valido: fine deve essere successiva a inizio.");
        }
        this.slotId = slotId;
        this.vetId = vetId;
        this.patientName = patientName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.requiresOperatingTable = requiresOperatingTable;
    }

    /*
     * Verifica la sovrapposizione matematica tra due intervalli semi-aperti [start,
     * end).
     */
    public boolean overlapsWith(AppointmentSlot other) {
        return this.startTime.isBefore(other.endTime) && other.startTime.isBefore(this.endTime);
    }

    public Duration getDuration() {
        return Duration.between(startTime, endTime);
    }

    public String getSlotId() {
        return slotId;
    }

    public int getVetId() {
        return vetId;
    }

    public String getPatientName() {
        return patientName;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public boolean isRequiresOperatingTable() {
        return requiresOperatingTable;
    }

    @Override
    public String toString() {
        String tipo = requiresOperatingTable ? "CHIRURGIA [Tavolo Operatorio]" : "VISITA ORDINARIA";
        return String.format("[%s] Vet #%d - Paziente: %s (%s -> %s) - %s",
                slotId, vetId, patientName, startTime.toLocalTime(), endTime.toLocalTime(), tipo);
    }
}
