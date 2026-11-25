package it.univaq.vetcare.model;

import java.time.LocalDateTime;

/**
 * US-08 & US-09: Voce della Cartella Clinica Elettronica (visita medica consolidata).
 */
public class MedicalVisit {

    private Integer id;
    private Integer animalId;
    private LocalDateTime visitDateTime;
    private String veterinarianName;
    private String reason;
    private String anamnesis;
    private String objectiveExam;
    private double weightKg;
    private double temperatureC;
    private String diagnosis;
    private String prescribedTherapy;
    private boolean consolidated;
    private String digitalSignature;

    public MedicalVisit() {
    }

    public MedicalVisit(Integer id, Integer animalId, LocalDateTime visitDateTime,
                        String veterinarianName, String reason, String anamnesis,
                        String objectiveExam, double weightKg, double temperatureC,
                        String diagnosis, String prescribedTherapy,
                        boolean consolidated, String digitalSignature) {
        this.id = id;
        this.animalId = animalId;
        this.visitDateTime = visitDateTime;
        this.veterinarianName = veterinarianName;
        this.reason = reason;
        this.anamnesis = anamnesis;
        this.objectiveExam = objectiveExam;
        this.weightKg = weightKg;
        this.temperatureC = temperatureC;
        this.diagnosis = diagnosis;
        this.prescribedTherapy = prescribedTherapy;
        this.consolidated = consolidated;
        this.digitalSignature = digitalSignature;
    }

    public MedicalVisit(Integer animalId, LocalDateTime visitDateTime,
                        String veterinarianName, String reason, String anamnesis,
                        String objectiveExam, double weightKg, double temperatureC,
                        String diagnosis, String prescribedTherapy) {
        this(null, animalId, visitDateTime, veterinarianName, reason, anamnesis,
                objectiveExam, weightKg, temperatureC, diagnosis, prescribedTherapy, true, null);
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

    public LocalDateTime getVisitDateTime() {
        return visitDateTime;
    }

    public void setVisitDateTime(LocalDateTime visitDateTime) {
        this.visitDateTime = visitDateTime;
    }

    public String getVeterinarianName() {
        return veterinarianName;
    }

    public void setVeterinarianName(String veterinarianName) {
        this.veterinarianName = veterinarianName;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getAnamnesis() {
        return anamnesis;
    }

    public void setAnamnesis(String anamnesis) {
        this.anamnesis = anamnesis;
    }

    public String getObjectiveExam() {
        return objectiveExam;
    }

    public void setObjectiveExam(String objectiveExam) {
        this.objectiveExam = objectiveExam;
    }

    public double getWeightKg() {
        return weightKg;
    }

    public void setWeightKg(double weightKg) {
        this.weightKg = weightKg;
    }

    public double getTemperatureC() {
        return temperatureC;
    }

    public void setTemperatureC(double temperatureC) {
        this.temperatureC = temperatureC;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
    }

    public String getPrescribedTherapy() {
        return prescribedTherapy;
    }

    public void setPrescribedTherapy(String prescribedTherapy) {
        this.prescribedTherapy = prescribedTherapy;
    }

    public boolean isConsolidated() {
        return consolidated;
    }

    public void setConsolidated(boolean consolidated) {
        this.consolidated = consolidated;
    }

    public String getDigitalSignature() {
        return digitalSignature;
    }

    public void setDigitalSignature(String digitalSignature) {
        this.digitalSignature = digitalSignature;
    }
}
