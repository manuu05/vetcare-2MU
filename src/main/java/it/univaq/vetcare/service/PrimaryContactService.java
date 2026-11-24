package it.univaq.vetcare.service;

import it.univaq.vetcare.dao.PrimaryContactDAO;
import it.univaq.vetcare.model.Owner;
import it.univaq.vetcare.model.OwnershipDetail;

import java.util.List;
import java.util.Optional;

/**
 * US-03 (Sub-Task US03-1): Servizio di business per collegare un animale a più proprietari
 * (comproprietà o affido familiare) e contrassegnare l'unico Referente Primario di contatto.
 */
public class PrimaryContactService {

    private final PrimaryContactDAO primaryContactDAO;

    public PrimaryContactService(PrimaryContactDAO primaryContactDAO) {
        this.primaryContactDAO = primaryContactDAO;
    }

    public void addCoOwner(int animalId, int ownerId, String role, boolean setAsPrimaryContact) {
        if (animalId <= 0 || ownerId <= 0) {
            throw new ValidationException("Selezionare un animale e un proprietario validi.");
        }
        boolean effectivePrimary = setAsPrimaryContact
                || !primaryContactDAO.findPrimaryContact(animalId).isPresent();
        primaryContactDAO.linkOwnerAndManagePrimary(animalId, ownerId, role, effectivePrimary);
    }

    public void designatePrimaryContact(int animalId, int ownerId) {
        try {
            primaryContactDAO.setPrimaryContact(animalId, ownerId);
        } catch (IllegalArgumentException ex) {
            throw new ValidationException(ex.getMessage());
        }
    }

    public List<OwnershipDetail> getOwnershipDetails(int animalId) {
        return primaryContactDAO.findOwnershipDetailsByAnimal(animalId);
    }

    public Optional<Owner> getPrimaryContact(int animalId) {
        return primaryContactDAO.findPrimaryContact(animalId);
    }
}
