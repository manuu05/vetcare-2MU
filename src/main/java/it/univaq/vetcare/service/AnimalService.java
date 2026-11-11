package it.univaq.vetcare.service;

import it.univaq.vetcare.dao.AnimalDAO;
import it.univaq.vetcare.model.Animal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * US-01 (Sub-Task US01-2): Servizio di business per la validazione dell'anagrafica animale,
 * validatore microchip ISO a 15 cifre, blocco duplicati e generazione ID cartella univoco.
 */
public class AnimalService {

    private final AnimalDAO animalDAO;

    public AnimalService(AnimalDAO animalDAO) {
        this.animalDAO = animalDAO;
    }

    /**
     * Valida il formato standard ISO del microchip (esattamente 15 cifre numeriche).
     */
    public boolean isValidIsoMicrochip(String microchip) {
        if (microchip == null) {
            return false;
        }
        return microchip.trim().matches("^\\d{15}$");
    }

    /**
     * Registra un nuovo paziente animale verificando i campi obbligatori,
     * il formato microchip a 15 cifre e l'assenza di duplicati nel database.
     */
    public Animal registerAnimal(Animal animal) {
        if (animal == null) {
            throw new ValidationException("Scheda animale non può essere nulla.");
        }
        if (animal.getName() == null || animal.getName().trim().isEmpty()) {
            throw new ValidationException("Il campo 'Nome' dell'animale è obbligatorio.");
        }
        if (animal.getSpecies() == null || animal.getSpecies().trim().isEmpty()) {
            throw new ValidationException("Il campo 'Specie' dell'animale è obbligatorio.");
        }
        if (animal.getBirthDate() == null) {
            throw new ValidationException("Il campo 'Data di nascita' è obbligatorio.");
        }
        if (animal.getWeight() <= 0) {
            throw new ValidationException("Il campo 'Peso' deve essere maggiore di zero.");
        }
        if (!isValidIsoMicrochip(animal.getMicrochip())) {
            throw new ValidationException("Il codice microchip deve essere composto da esattamente 15 cifre numeriche (standard ISO).");
        }

        String cleanMicrochip = animal.getMicrochip().trim();
        animal.setMicrochip(cleanMicrochip);

        // Criterio BDD US-01: Blocco se il microchip è già presente a sistema
        Optional<Animal> existing = animalDAO.findByMicrochip(cleanMicrochip);
        if (existing.isPresent()) {
            throw new ValidationException("Errore di duplicato: il microchip " + cleanMicrochip
                    + " è già presente a sistema per il paziente '" + existing.get().getName() + "'.");
        }

        if (animal.getSex() == null || animal.getSex().trim().isEmpty()) {
            animal.setSex("M");
        }

        // Generazione ID Cartella Clinica Univoco (es. VC-2026-0001)
        int nextSeq = animalDAO.countAll() + 1;
        String uniqueRecordCode = String.format("VC-2026-%04d-%s",
                nextSeq, UUID.randomUUID().toString().substring(0, 4).toUpperCase());
        animal.setMedicalRecordCode(uniqueRecordCode);

        return animalDAO.insert(animal);
    }

    public List<Animal> getAllAnimals() {
        return animalDAO.findAll();
    }

    public Optional<Animal> findByMicrochip(String microchip) {
        return animalDAO.findByMicrochip(microchip);
    }

    public Optional<Animal> findById(int id) {
        return animalDAO.findById(id);
    }
}
