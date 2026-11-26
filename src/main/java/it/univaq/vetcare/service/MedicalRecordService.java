package it.univaq.vetcare.service;

import it.univaq.vetcare.dao.MedicalVisitDAO;
import it.univaq.vetcare.model.MedicalVisit;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * US-08 & US-09: Servizio per la consultazione pre-visita della storia clinica
 * (timeline visite e andamento peso) e per la compilazione e il consolidamento
 * immutabile del diario clinico integrato con il controllo allergie (US-07).
 */
public class MedicalRecordService {

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final MedicalVisitDAO medicalVisitDAO;
    private final PrescriptionSafetyService prescriptionSafetyService;

    public MedicalRecordService(MedicalVisitDAO medicalVisitDAO,
                                PrescriptionSafetyService prescriptionSafetyService) {
        this.medicalVisitDAO = medicalVisitDAO;
        this.prescriptionSafetyService = prescriptionSafetyService;
    }

    /**
     * US-09: Registra e consolida in modo immutabile una visita medica, eseguendo
     * preventivamente la verifica bloccante sulle allergie farmacologiche (US-07).
     */
    public MedicalVisit recordAndConsolidateVisit(MedicalVisit visit,
                                                  boolean allergyOverrideConfirmed,
                                                  String overrideClinicalJustification) {
        if (visit == null || visit.getAnimalId() == null || visit.getAnimalId() <= 0) {
            throw new ValidationException("Selezionare un paziente valido prima di compilare la visita.");
        }
        if (isBlank(visit.getVeterinarianName())) {
            throw new ValidationException("Indicare il Veterinario responsabile della visita.");
        }
        if (isBlank(visit.getReason())) {
            throw new ValidationException("Il motivo della visita è obbligatorio.");
        }
        if (isBlank(visit.getAnamnesis())) {
            throw new ValidationException("Il campo 'Anamnesi' è obbligatorio.");
        }
        if (isBlank(visit.getObjectiveExam())) {
            throw new ValidationException("Il campo 'Esame Obiettivo' è obbligatorio.");
        }
        if (visit.getWeightKg() <= 0) {
            throw new ValidationException("Il peso rilevato in visita deve essere maggiore di zero.");
        }
        if (visit.getTemperatureC() <= 30.0 || visit.getTemperatureC() >= 45.0) {
            throw new ValidationException("La temperatura corporea deve essere compresa tra 30.1°C e 44.9°C.");
        }
        if (isBlank(visit.getDiagnosis())) {
            throw new ValidationException("Il campo 'Diagnosi' è obbligatorio per consolidare la visita.");
        }

        // Controllo attivo sicurezza prescrizioni allergiche (US-07)
        prescriptionSafetyService.validateOrAuditPrescription(
                visit.getAnimalId(),
                visit.getPrescribedTherapy(),
                visit.getVeterinarianName(),
                allergyOverrideConfirmed,
                overrideClinicalJustification
        );

        LocalDateTime now = visit.getVisitDateTime() != null ? visit.getVisitDateTime() : LocalDateTime.now();
        visit.setVisitDateTime(now);
        visit.setConsolidated(true);
        String signature = "FIRMATO DIGITALMENTE: " + visit.getVeterinarianName().trim()
                + " [" + now.format(TS_FMT) + "]";
        visit.setDigitalSignature(signature);

        return medicalVisitDAO.insertConsolidatedVisit(visit);
    }

    /**
     * Criterio BDD US-09: Una visita consolidata nello storico non può essere modificata.
     */
    public void attemptModifyVisit(int visitId, String newDiagnosis) {
        MedicalVisit existing = medicalVisitDAO.findById(visitId)
                .orElseThrow(() -> new ValidationException("Visita clinica non trovata."));
        if (existing.isConsolidated()) {
            throw new ValidationException("Operazione negata: la visita #" + visitId
                    + " è in stato CONSOLIDATA (" + existing.getDigitalSignature()
                    + ") e non può essere modificata a posteriori.");
        }
    }

    /**
     * US-08: Restituisce la timeline completa delle visite passate (dalla più recente).
     */
    public List<MedicalVisit> getClinicalTimeline(int animalId) {
        return medicalVisitDAO.findTimelineByAnimalId(animalId);
    }

    /**
     * US-08: Sintetizza l'andamento storico del peso corporeo del paziente.
     */
    public String buildWeightProgressionSummary(int animalId) {
        List<MedicalVisit> timeline = medicalVisitDAO.findTimelineByAnimalId(animalId);
        if (timeline.isEmpty()) {
            return "Nessuna rilevazione storica di peso in cartella clinica.";
        }
        List<MedicalVisit> chronological = new ArrayList<>(timeline);
        Collections.reverse(chronological);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < chronological.size(); i++) {
            MedicalVisit v = chronological.get(i);
            if (i > 0) {
                sb.append(" -> ");
            }
            sb.append(String.format(Locale.US, "%.1f kg (%s)",
                    v.getWeightKg(), v.getVisitDateTime().toLocalDate()));
        }
        if (chronological.size() >= 2) {
            double first = chronological.get(0).getWeightKg();
            double last = chronological.get(chronological.size() - 1).getWeightKg();
            double diff = last - first;
            sb.append(String.format(Locale.US, " [Variazione netta: %+.1f kg]", diff));
        }
        return sb.toString();
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
