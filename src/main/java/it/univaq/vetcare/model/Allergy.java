package it.univaq.vetcare.model;

import java.util.Objects;


public class Allergy {

    private Integer id;
    private Integer animalId;
    private String substance;
    private String severity;
    private String notes;

    public Allergy() {
    }

    public Allergy(Integer id, Integer animalId, String substance, String severity, String notes) {
        this.id = id;
        this.animalId = animalId;
        this.substance = substance;
        this.severity = severity != null ? severity : "CRITICA";
        this.notes = notes;
    }

    public Allergy(Integer animalId, String substance, String severity, String notes) {
        this(null, animalId, substance, severity, notes);
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

    public void setAnimalId(Integer animalId) {
        this.animalId = animalId;
    }

    public String getSubstance() {
        return substance;
    }

    public void setSubstance(String substance) {
        this.substance = substance;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Allergy)) return false;
        Allergy allergy = (Allergy) o;
        return Objects.equals(animalId, allergy.animalId)
                && Objects.equals(substance, allergy.substance);
    }

    @Override
    public int hashCode() {
        return Objects.hash(animalId, substance);
    }

    @Override
    public String toString() {
        return substance + " (" + severity + ")";
    }
}
