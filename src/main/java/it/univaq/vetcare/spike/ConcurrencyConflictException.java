package it.univaq.vetcare.spike;

/**
 * SP-01: Eccezione sollevata quando viene rilevata una collisione oraria
 * sul tavolo operatorio unico o sull'agenda di uno dei 3 veterinari.
 * Contiene la proposta di riallocazione deterministica.
 */
public class ConcurrencyConflictException extends RuntimeException {

    private final AppointmentSlot conflictingSlot;
    private final AppointmentSlot proposedReallocation;
    private final long resolutionTimeMs;

    public ConcurrencyConflictException(String message,
                                        AppointmentSlot conflictingSlot,
                                        AppointmentSlot proposedReallocation,
                                        long resolutionTimeMs) {
        super(message);
        this.conflictingSlot = conflictingSlot;
        this.proposedReallocation = proposedReallocation;
        this.resolutionTimeMs = resolutionTimeMs;
    }

    public AppointmentSlot getConflictingSlot() {
        return conflictingSlot;
    }

    public AppointmentSlot getProposedReallocation() {
        return proposedReallocation;
    }

    public long getResolutionTimeMs() {
        return resolutionTimeMs;
    }
}
