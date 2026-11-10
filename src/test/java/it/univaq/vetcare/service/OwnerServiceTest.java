package it.univaq.vetcare.service;

import it.univaq.vetcare.dao.AnimalOwnerDAO;
import it.univaq.vetcare.dao.OwnerDAO;
import it.univaq.vetcare.db.DatabaseManager;
import it.univaq.vetcare.model.Owner;
import it.univaq.vetcare.model.PreferredChannel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

/**
 * US-02: Test per registrazione e aggiornamento dei Proprietari con validazione
 * recapiti telefonici e canale di contatto preferito (Telefono/SMS/Email).
 */
public class OwnerServiceTest {

    private File tempDbFile;
    private OwnerService ownerService;

    @BeforeEach
    public void setUp() throws Exception {
        tempDbFile = File.createTempFile("vetcare_test_us02_", ".db");
        DatabaseManager dbManager = new DatabaseManager("jdbc:sqlite:" + tempDbFile.getAbsolutePath());
        dbManager.initializeSchema();
        ownerService = new OwnerService(new OwnerDAO(dbManager), new AnimalOwnerDAO(dbManager));
    }

    @AfterEach
    public void tearDown() {
        if (tempDbFile != null && tempDbFile.exists()) {
            tempDbFile.delete();
        }
    }

    @Test
    public void testRegisterValidOwnerWithPreferredChannel() {
        // Dato il form di inserimento anagrafica proprietario con recapito valido
        Owner owner = new Owner(
                "Laura", "Neri", "NRELRA90C45H501X",
                "+39 3401234567", "laura.neri@email.it", PreferredChannel.SMS
        );

        // Quando viene registrato
        Owner saved = ownerService.registerOwner(owner);

        // Allora il proprietario viene memorizzato e associato al canale preferito
        assertNotNull(saved.getId(), "L'ID del proprietario deve essere valorizzato");
        assertEquals(PreferredChannel.SMS, saved.getPreferredChannel());
        assertEquals("NRELRA90C45H501X", saved.getFiscalCode());
    }

    @Test
    public void testInvalidPhoneNumberIsRejected() {
        Owner badPhoneOwner = new Owner(
                "Luca", "Bianchi", "BNCLCU85A01H501Q",
                "abc-non-valido", "luca@email.it", PreferredChannel.TELEFONO
        );

        assertThrows(ValidationException.class, () -> {
            ownerService.registerOwner(badPhoneOwner);
        }, "Un proprietario senza recapito telefonico valido deve essere rifiutato");
    }

    @Test
    public void testUpdateOwnerContactInformation() {
        Owner saved = ownerService.registerOwner(new Owner(
                "Paolo", "Verdi", "VRDPLA75D12H501Y",
                "3339876543", "paolo@old.it", PreferredChannel.TELEFONO
        ));

        ownerService.updateContactInfo(saved.getId(), "3490001112", "paolo@new.it", PreferredChannel.EMAIL);
        Owner updated = ownerService.getAllOwners().get(0);

        assertEquals("3490001112", updated.getPhone());
        assertEquals(PreferredChannel.EMAIL, updated.getPreferredChannel());
    }
}
