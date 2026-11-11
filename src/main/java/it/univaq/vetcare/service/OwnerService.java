package it.univaq.vetcare.service;

import it.univaq.vetcare.dao.AnimalOwnerDAO;
import it.univaq.vetcare.dao.OwnerDAO;
import it.univaq.vetcare.model.Animal;
import it.univaq.vetcare.model.Owner;
import it.univaq.vetcare.model.PreferredChannel;

import java.util.List;
import java.util.Optional;

/**
 * US-02 (Sub-Task US02-2) & TK-02: Servizio di business per la registrazione
 * e l'aggiornamento dei Proprietari, validazione recapiti e associazione N:M.
 */
public class OwnerService {

    private final OwnerDAO ownerDAO;
    private final AnimalOwnerDAO animalOwnerDAO;

    public OwnerService(OwnerDAO ownerDAO, AnimalOwnerDAO animalOwnerDAO) {
        this.ownerDAO = ownerDAO;
        this.animalOwnerDAO = animalOwnerDAO;
    }

    public boolean isValidPhone(String phone) {
        if (phone == null) {
            return false;
        }
        String cleaned = phone.trim();
        return cleaned.matches("^\\+?[0-9\\s\\-]{7,16}$");
    }

    public boolean isValidFiscalCode(String fiscalCode) {
        if (fiscalCode == null) {
            return false;
        }
        return fiscalCode.trim().toUpperCase().matches("^[A-Z0-9]{16}$");
    }

    /**
     * Registra un nuovo proprietario verificando i dati anagrafici,
     * almeno un recapito telefonico valido e il canale di contatto preferito.
     */
    public Owner registerOwner(Owner owner) {
        if (owner == null) {
            throw new ValidationException("Anagrafica proprietario non può essere nulla.");
        }
        if (owner.getFirstName() == null || owner.getFirstName().trim().isEmpty()) {
            throw new ValidationException("Il campo 'Nome' del proprietario è obbligatorio.");
        }
        if (owner.getLastName() == null || owner.getLastName().trim().isEmpty()) {
            throw new ValidationException("Il campo 'Cognome' del proprietario è obbligatorio.");
        }
        if (!isValidFiscalCode(owner.getFiscalCode())) {
            throw new ValidationException("Il Codice Fiscale deve essere composto da 16 caratteri alfanumerici.");
        }
        if (!isValidPhone(owner.getPhone())) {
            throw new ValidationException("È obbligatorio inserire almeno un recapito telefonico valido (7-16 cifre).");
        }
        if (owner.getPreferredChannel() == null) {
            owner.setPreferredChannel(PreferredChannel.TELEFONO);
        }

        String normalizedCf = owner.getFiscalCode().trim().toUpperCase();
        owner.setFiscalCode(normalizedCf);

        Optional<Owner> existing = ownerDAO.findByFiscalCode(normalizedCf);
        if (existing.isPresent()) {
            throw new ValidationException("Proprietario con Codice Fiscale " + normalizedCf + " già registrato.");
        }

        return ownerDAO.insert(owner);
    }

    public void updateContactInfo(int ownerId, String newPhone, String newEmail, PreferredChannel channel) {
        if (!isValidPhone(newPhone)) {
            throw new ValidationException("Recapito telefonico non valido.");
        }
        ownerDAO.updateContactInfo(ownerId, newPhone.trim(), newEmail, channel != null ? channel : PreferredChannel.TELEFONO);
    }

    public void associateAnimalToOwner(int animalId, int ownerId, String role) {
        animalOwnerDAO.linkAnimalToOwner(animalId, ownerId, role);
    }

    public List<Owner> getOwnersOfAnimal(int animalId) {
        return animalOwnerDAO.findOwnersByAnimalId(animalId);
    }

    public List<Animal> getAnimalsOfOwner(int ownerId) {
        return animalOwnerDAO.findAnimalsByOwnerId(ownerId);
    }

    public List<Owner> getAllOwners() {
        return ownerDAO.findAll();
    }
}
