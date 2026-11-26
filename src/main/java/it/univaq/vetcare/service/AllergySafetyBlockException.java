package it.univaq.vetcare.service;

/**
 * US-07: Eccezione di sicurezza clinica sollevata quando il veterinario tenta di
 * prescrivere o somministrare un principio attivo incompatibile con le allergie
 * documentate del paziente senza un Override Primario motivato.
 */
public class AllergySafetyBlockException extends RuntimeException {

    private final int animalId;
    private final String prescribedDrug;
    private final String matchedAllergen;

    public AllergySafetyBlockException(int animalId, String prescribedDrug, String matchedAllergen) {
        super("BLOCCO SICUREZZA ALLERGIA CRITICA: Il farmaco '" + prescribedDrug
                + "' è incompatibile con l'allergia documentata a '" + matchedAllergen
                + "'. Prescrizione bloccata: per procedere in emergenza è obbligatorio un "
                + "'Override Primario' con motivazione clinica firmata.");
        this.animalId = animalId;
        this.prescribedDrug = prescribedDrug;
        this.matchedAllergen = matchedAllergen;
    }

    public int getAnimalId() {
        return animalId;
    }

    public String getPrescribedDrug() {
        return prescribedDrug;
    }

    public String getMatchedAllergen() {
        return matchedAllergen;
    }
}
