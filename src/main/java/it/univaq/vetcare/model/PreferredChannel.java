package it.univaq.vetcare.model;


public enum PreferredChannel {
    TELEFONO("Telefono"),
    SMS("SMS"),
    EMAIL("Email");

    private final String label;

    PreferredChannel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static PreferredChannel fromString(String value) {
        if (value == null) {
            return TELEFONO;
        }
        for (PreferredChannel channel : values()) {
            if (channel.name().equalsIgnoreCase(value.trim()) || channel.label.equalsIgnoreCase(value.trim())) {
                return channel;
            }
        }
        return TELEFONO;
    }

    @Override
    public String toString() {
        return label;
    }
}
