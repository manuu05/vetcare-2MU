package it.univaq.vetcare.model;

import java.util.Objects;

/**
 * US-03: Rappresenta il legame di comproprietà/affido tra un Animale e un Proprietario,
 * includendo il flag che identifica il Referente Primario per le comunicazioni.
 */
public class OwnershipDetail {

    private final Owner owner;
    private final String ownershipRole;
    private final boolean primaryContact;

    public OwnershipDetail(Owner owner, String ownershipRole, boolean primaryContact) {
        this.owner = Objects.requireNonNull(owner, "Owner non può essere null");
        this.ownershipRole = ownershipRole != null ? ownershipRole : "Comproprietario";
        this.primaryContact = primaryContact;
    }

    public Owner getOwner() {
        return owner;
    }

    public String getOwnershipRole() {
        return ownershipRole;
    }

    public boolean isPrimaryContact() {
        return primaryContact;
    }

    @Override
    public String toString() {
        String badge = primaryContact ? "[REFERENTE PRIMARIO] " : "";
        return badge + owner.getFullName() + " (" + ownershipRole + ") - Tel: "
                + owner.getPhone() + " [" + owner.getPreferredChannel() + "]";
    }
}
