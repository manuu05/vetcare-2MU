package it.univaq.vetcare.service;

import it.univaq.vetcare.dao.PrescriptionOverrideDAO;
import it.univaq.vetcare.model.Allergy;
import it.univaq.vetcare.model.PrescriptionOverrideLog;

import java.time.LocalDateTime;
import java.util.*;

/**
 * US-07 (Sub-Task US07-1 - Mob Programming): Motore di sicurezza clinica attiva.
 * Intercetta e blocca la prescrizione di principi attivi incompatibili con le allergie
 * del paziente (inclusa la cross-reattività di famiglia, es. PENICILLINE <-> Amoxicillina),
 * consentendo lo sblocco solo tramite Override Primario tracciato con motivazione clinica.
 */
public class PrescriptionSafetyService {

    private static final Map<String, List<String>> PHARMACOLOGICAL_FAMILIES = createFamiliesMap();

    private final AllergyAlertService allergyAlertService;
    private final PrescriptionOverrideDAO overrideDAO;

    public PrescriptionSafetyService(AllergyAlertService allergyAlertService,
                                     PrescriptionOverrideDAO overrideDAO) {
        this.allergyAlertService = allergyAlertService;
        this.overrideDAO = overrideDAO;
    }

    private static Map<String, List<String>> createFamiliesMap() {
        Map<String, List<String>> map = new HashMap<>();
        map.put("PENICILLIN", Arrays.asList("AMOXICILLINA", "AMPICILLINA", "AUGMENTIN", "PENICILLINA", "BENZILPENICILLINA"));
        map.put("AMOXICILLIN", Arrays.asList("AMOXICILLINA", "PENICILLINA", "PENICILLINE", "AUGMENTIN"));
        map.put("CEFALOSPORIN", Arrays.asList("CEFALEXINA", "CEFTRIAXONE", "CEFAZOLINA"));
        map.put("FANS", Arrays.asList("MELOXICAM", "CARPROFEN", "KETOPROFENE", "IBUPROFENE"));
        return Collections.unmodifiableMap(map);
    }

    /**
     * Verifica se la terapia/farmaco prescritto collide con le allergie note dell'animale.
     * Restituisce l'allergene in conflitto se presente.
     */
    public Optional<String> detectAllergyConflict(int animalId, String prescribedTherapy) {
        if (prescribedTherapy == null || prescribedTherapy.trim().isEmpty()) {
            return Optional.empty();
        }
        String upperTherapy = prescribedTherapy.toUpperCase();
        List<Allergy> allergies = allergyAlertService.getAllergiesForAnimal(animalId);

        for (Allergy allergy : allergies) {
            String allergen = allergy.getSubstance().trim().toUpperCase();
            if (upperTherapy.contains(allergen) || allergen.contains(upperTherapy.trim())) {
                return Optional.of(allergy.getSubstance());
            }
            for (Map.Entry<String, List<String>> entry : PHARMACOLOGICAL_FAMILIES.entrySet()) {
                if (allergen.contains(entry.getKey())) {
                    for (String relatedDrug : entry.getValue()) {
                        if (upperTherapy.contains(relatedDrug)) {
                            return Optional.of(allergy.getSubstance());
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Valida la prescrizione: se rileva incompatibilità allergica e non è fornito un
     * Override Primario valido, solleva AllergySafetyBlockException bloccante.
     */
    public void validateOrAuditPrescription(int animalId,
                                            String prescribedTherapy,
                                            String veterinarianName,
                                            boolean primaryOverrideRequested,
                                            String clinicalJustification) {
        Optional<String> conflict = detectAllergyConflict(animalId, prescribedTherapy);
        if (!conflict.isPresent()) {
            return;
        }

        if (!primaryOverrideRequested) {
            throw new AllergySafetyBlockException(animalId, prescribedTherapy.trim(), conflict.get());
        }

        if (veterinarianName == null || veterinarianName.trim().isEmpty()) {
            throw new ValidationException("Per eseguire un Override Primario è obbligatorio indicare il Veterinario responsabile.");
        }
        if (clinicalJustification == null || clinicalJustification.trim().length() < 10) {
            throw new ValidationException("Override Primario rifiutato: inserire una motivazione clinica dettagliata (minimo 10 caratteri).");
        }

        overrideDAO.insert(new PrescriptionOverrideLog(
                animalId,
                prescribedTherapy.trim(),
                conflict.get(),
                veterinarianName.trim(),
                clinicalJustification.trim(),
                LocalDateTime.now()
        ));
    }

    public List<PrescriptionOverrideLog> getOverrideHistory(int animalId) {
        return overrideDAO.findByAnimalId(animalId);
    }
}
