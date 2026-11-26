package it.univaq.vetcare.spike;

/**
 * SP-01 & RF-01 (Refactoring post Sprint 1 Review):
 * Eccezione pura e leggera sollevata quando viene rilevata una collisione oraria
 * sul tavolo operatorio unico o sull'agenda di uno dei 3 veterinari.
 * Non esegue né trasporta calcoli di riallocazione: è il blocco catch del chiamante
 * a decidere se invocare il rescheduling.
 */
public class ConcurrencyConflictException extends RuntimeException {

    private final AppointmentSlot conflictingSlot;

    public ConcurrencyConflictException(String message, AppointmentSlot conflictingSlot) {
        super(message);
        this.conflictingSlot = conflictingSlot;
    }

    public AppointmentSlot getConflictingSlot() {
        return conflictingSlot;
    }
}
