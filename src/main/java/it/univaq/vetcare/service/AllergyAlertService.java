package it.univaq.vetcare.service;

import it.univaq.vetcare.dao.AllergyDAO;
import it.univaq.vetcare.model.Allergy;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * US-06 (Sub-Task US06-2): Servizio backend per il recupero delle allergie note
 * del paziente con cache in memoria e formattazione del banner rosso di allerta critica.
 */
public class AllergyAlertService {

    private final AllergyDAO allergyDAO;
    private final Map<Integer, List<Allergy>> allergyCache = new ConcurrentHashMap<>();

    public AllergyAlertService(AllergyDAO allergyDAO) {
        this.allergyDAO = allergyDAO;
    }

    /**
     * Registra una nuova allergia/intolleranza per il paziente e aggiorna la cache in memoria.
     */
    public Allergy registerAllergy(int animalId, String substance, String severity, String notes) {
        if (substance == null || substance.trim().isEmpty()) {
            throw new ValidationException("Specificare il principio attivo o la classe farmacologica dell'allergia.");
        }
        Allergy saved = allergyDAO.insert(new Allergy(
                animalId,
                substance.trim().toUpperCase(),
                severity != null ? severity : "CRITICA",
                notes
        ));
        // Invalida la cache per forzare il ricaricamento aggiornato
        allergyCache.remove(animalId);
        return saved;
    }

    /**
     * Recupera le allergie note dell'animale utilizzando la cache in memoria per accesso immediato.
     */
    public List<Allergy> getAllergiesForAnimal(int animalId) {
        return allergyCache.computeIfAbsent(animalId, id ->
                Collections.unmodifiableList(allergyDAO.findByAnimalId(id))
        );
    }

    public boolean hasCriticalAllergies(int animalId) {
        return !getAllergiesForAnimal(animalId).isEmpty();
    }

    /**
     * Costruisce il messaggio standard per il Banner Rosso Critico:
     * "ATTENZIONE: ANIMALE ALLERGICO A [FARMACO]" (Criterio di Accettazione US-06).
     */
    public String buildCriticalBannerText(int animalId) {
        List<Allergy> allergies = getAllergiesForAnimal(animalId);
        if (allergies.isEmpty()) {
            return "";
        }
        String substances = allergies.stream()
                .map(a -> a.getSubstance().toUpperCase())
                .distinct()
                .collect(Collectors.joining(", "));
        return "ATTENZIONE: ANIMALE ALLERGICO A " + substances;
    }

    public boolean isCached(int animalId) {
        return allergyCache.containsKey(animalId);
    }

    public void clearCache() {
        allergyCache.clear();
    }
}
