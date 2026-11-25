package it.univaq.vetcare.model;

/**
 * US-11: Tipologie di visita ambulatoriale con durata standard differenziata
 * per la pianificazione degli slot sull'agenda dei 3 veterinari.
 */
public enum VisitType {

    PROFILASSI_VACCINO("Profilassi / Vaccinazione (20 min)", 20),
    VISITA_BASE("Visita Clinica Ordinaria (30 min)", 30),
    CONTROLLO_POST_VISITA("Controllo / Medicazione (30 min)", 30),
    VISITA_SPECIALISTICA("Visita Specialistica (45 min)", 45),
    CONSULTO_PRE_OPERATORIO("Consulto Pre-Operatorio Approfondito (60 min)", 60);

    private final String label;
    private final int durationMinutes;

    VisitType(String label, int durationMinutes) {
        this.label = label;
        this.durationMinutes = durationMinutes;
    }

    public String getLabel() {
        return label;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public static VisitType fromNameOrDefault(String value) {
        if (value == null) {
            return VISITA_BASE;
        }
        for (VisitType vt : values()) {
            if (vt.name().equalsIgnoreCase(value.trim())) {
                return vt;
            }
        }
        return VISITA_BASE;
    }

    @Override
    public String toString() {
        return label;
    }
}
