package it.univaq.vetcare.model;

/**
 * US-11: Anagrafica dei 3 Veterinari dell'ambulatorio VetCare nell'agenda unica.
 */
public enum Veterinarian {

    VET_1(1, "Dott. Marco Bianchi", "Chirurgia & Ortopedia"),
    VET_2(2, "Dott.ssa Sara Verdi", "Medicina Interna & Cardiologia"),
    VET_3(3, "Dott. Luca Neri", "Diagnostica & Animali Non Convenzionali");

    private final int id;
    private final String fullName;
    private final String specialty;

    Veterinarian(int id, String fullName, String specialty) {
        this.id = id;
        this.fullName = fullName;
        this.specialty = specialty;
    }

    public int getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getSpecialty() {
        return specialty;
    }

    public static Veterinarian fromId(int id) {
        for (Veterinarian v : values()) {
            if (v.id == id) {
                return v;
            }
        }
        throw new IllegalArgumentException("Veterinario non valido: ID=" + id);
    }

    @Override
    public String toString() {
        return "Vet #" + id + " - " + fullName + " (" + specialty + ")";
    }
}
