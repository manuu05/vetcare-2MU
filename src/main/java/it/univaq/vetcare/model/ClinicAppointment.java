package it.univaq.vetcare.model;

import java.time.LocalDateTime;

/**
 * US-11: Appuntamento persistente sull'agenda giornaliera/settimanale dei 3 veterinari.
 */
public class ClinicAppointment {

    private Integer id;
    private Integer animalId;
    private String animalName;
    private int veterinarianId;
    private VisitType visitType;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private String clinicalNotes;
    private String status;

    public ClinicAppointment(Integer id, Integer animalId, String animalName,
                             int veterinarianId, VisitType visitType,
                             LocalDateTime startDateTime, LocalDateTime endDateTime,
                             String clinicalNotes, String status) {
        if (startDateTime == null || endDateTime == null || !endDateTime.isAfter(startDateTime)) {
            throw new IllegalArgumentException("Intervallo orario appuntamento non valido.");
        }
        this.id = id;
        this.animalId = animalId;
        this.animalName = animalName;
        this.veterinarianId = veterinarianId;
        this.visitType = visitType;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.clinicalNotes = clinicalNotes;
        this.status = status != null ? status : "PROGRAMMATO";
    }

    public ClinicAppointment(Integer animalId, int veterinarianId, VisitType visitType,
                             LocalDateTime startDateTime, String clinicalNotes) {
        this(null, animalId, null, veterinarianId, visitType,
                startDateTime, startDateTime.plusMinutes(visitType.getDurationMinutes()),
                clinicalNotes, "PROGRAMMATO");
    }

    /**
     * Verifica collisione su intervalli temporali semichiusi [start, end).
     */
    public boolean overlapsWith(LocalDateTime otherStart, LocalDateTime otherEnd) {
        return this.startDateTime.isBefore(otherEnd) && otherStart.isBefore(this.endDateTime);
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getAnimalId() {
        return animalId;
    }

    public String getAnimalName() {
        return animalName;
    }

    public void setAnimalName(String animalName) {
        this.animalName = animalName;
    }

    public int getVeterinarianId() {
        return veterinarianId;
    }

    public VisitType getVisitType() {
        return visitType;
    }

    public LocalDateTime getStartDateTime() {
        return startDateTime;
    }

    public LocalDateTime getEndDateTime() {
        return endDateTime;
    }

    public String getClinicalNotes() {
        return clinicalNotes;
    }

    public String getStatus() {
        return status;
    }
}
