package it.univaq.vetcare.model;

import java.time.LocalDateTime;

/**
 * US-07: Voce di audit per l'Override Primario motivato su prescrizione a rischio allergico.
 */
public class PrescriptionOverrideLog {

    private Integer id;
    private Integer animalId;
    private String prescribedDrug;
    private String matchedAllergen;
    private String veterinarianName;
    private String clinicalJustification;
    private LocalDateTime overrideTimestamp;

    public PrescriptionOverrideLog(Integer id, Integer animalId, String prescribedDrug,
                                   String matchedAllergen, String veterinarianName,
                                   String clinicalJustification, LocalDateTime overrideTimestamp) {
        this.id = id;
        this.animalId = animalId;
        this.prescribedDrug = prescribedDrug;
        this.matchedAllergen = matchedAllergen;
        this.veterinarianName = veterinarianName;
        this.clinicalJustification = clinicalJustification;
        this.overrideTimestamp = overrideTimestamp;
    }

    public PrescriptionOverrideLog(Integer animalId, String prescribedDrug,
                                   String matchedAllergen, String veterinarianName,
                                   String clinicalJustification, LocalDateTime overrideTimestamp) {
        this(null, animalId, prescribedDrug, matchedAllergen, veterinarianName,
                clinicalJustification, overrideTimestamp);
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

    public String getPrescribedDrug() {
        return prescribedDrug;
    }

    public String getMatchedAllergen() {
        return matchedAllergen;
    }

    public String getVeterinarianName() {
        return veterinarianName;
    }

    public String getClinicalJustification() {
        return clinicalJustification;
    }

    public LocalDateTime getOverrideTimestamp() {
        return overrideTimestamp;
    }
}
