package it.univaq.vetcare.service;

/**
 * Eccezione di dominio sollevata quando fallisce la validazione dei dati
 * anagrafici o viene rilevato un duplicato bloccante (es. microchip già censito).
 */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }
}
