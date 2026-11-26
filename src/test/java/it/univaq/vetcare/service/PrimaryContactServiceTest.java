package it.univaq.vetcare.service;

import it.univaq.vetcare.dao.AnimalDAO;
import it.univaq.vetcare.dao.AnimalOwnerDAO;
import it.univaq.vetcare.dao.OwnerDAO;
import it.univaq.vetcare.dao.PrimaryContactDAO;
import it.univaq.vetcare.db.DatabaseManager;
import it.univaq.vetcare.model.Animal;
import it.univaq.vetcare.model.Owner;
import it.univaq.vetcare.model.OwnershipDetail;
import it.univaq.vetcare.model.PreferredChannel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * US-03 (Sub-Task US03-2): Test automatici BDD JUnit 5 per il collegamento di un animale
 * a più proprietari (comproprietà o affido) e l'identificazione dell'unico Referente Primario.
 */
public class PrimaryContactServiceTest {

    private File tempDbFile;
    private AnimalService animalService;
    private OwnerService ownerService;
    private PrimaryContactService primaryContactService;

    @BeforeEach
    public void setUp() throws Exception {
        tempDbFile = File.createTempFile("vetcare_test_s2_us03_", ".db");
        DatabaseManager dbManager = new DatabaseManager("jdbc:sqlite:" + tempDbFile.getAbsolutePath());
        dbManager.initializeSchema();

        AnimalDAO animalDAO = new AnimalDAO(dbManager);
        OwnerDAO ownerDAO = new OwnerDAO(dbManager);
        AnimalOwnerDAO animalOwnerDAO = new AnimalOwnerDAO(dbManager);
        PrimaryContactDAO primaryContactDAO = new PrimaryContactDAO(dbManager);

        animalService = new AnimalService(animalDAO);
        ownerService = new OwnerService(ownerDAO, animalOwnerDAO);
        primaryContactService = new PrimaryContactService(primaryContactDAO);
    }

    @AfterEach
    public void tearDown() {
        if (tempDbFile != null && tempDbFile.exists()) {
            tempDbFile.delete();
        }
    }

    @Test
    public void testFirstLinkedOwnerAutomaticallyBecomesPrimaryContact() {
        Animal rex = animalService.registerAnimal(new Animal(
                "Rex", "Cane", "Pastore Tedesco", LocalDate.of(2021, 5, 14),
                "M", "380260000111222", 31.0, ""));
        Owner mario = ownerService.registerOwner(new Owner(
                "Mario", "Rossi", "RSSMRA80A01H501U", "+39 3331112233", "m@r.it", PreferredChannel.TELEFONO));

        primaryContactService.addCoOwner(rex.getId(), mario.getId(), "Proprietario", false);

        Optional<Owner> primary = primaryContactService.getPrimaryContact(rex.getId());
        assertTrue(primary.isPresent(), "Il primo proprietario associato deve diventare referente primario di default");
        assertEquals("RSSMRA80A01H501U", primary.get().getFiscalCode());
    }

    @Test
    public void testAddSecondOwnerAndSwitchPrimaryContactUniquely() {
        // Criterio BDD US-03: Dato un animale censito, quando la segretaria aggiunge un secondo
        // proprietario e seleziona il referente primario, allora mostra entrambi e un solo primario attivo
        Animal rex = animalService.registerAnimal(new Animal(
                "Rex", "Cane", "Pastore Tedesco", LocalDate.of(2021, 5, 14),
                "M", "380260000111333", 31.0, ""));
        Owner mario = ownerService.registerOwner(new Owner(
                "Mario", "Rossi", "RSSMRA80A01H501U", "+39 3331112233", "m@r.it", PreferredChannel.TELEFONO));
        Owner laura = ownerService.registerOwner(new Owner(
                "Laura", "Verdi", "VRDLRA82B41H501K", "+39 3409998877", "l@v.it", PreferredChannel.SMS));

        primaryContactService.addCoOwner(rex.getId(), mario.getId(), "Comproprietario Familiare", true);
        primaryContactService.addCoOwner(rex.getId(), laura.getId(), "Comproprietario Familiare", true);

        List<OwnershipDetail> details = primaryContactService.getOwnershipDetails(rex.getId());
        assertEquals(2, details.size(), "La scheda animale deve mostrare entrambi i comproprietari");
        assertEquals(1, details.stream().filter(OwnershipDetail::isPrimaryContact).count(),
                "Deve esistere esattamente un solo referente primario attivo");
        assertEquals("VRDLRA82B41H501K", primaryContactService.getPrimaryContact(rex.getId()).get().getFiscalCode());
    }

    @Test
    public void testDesignatePrimaryContactFailsIfOwnerNotLinked() {
        Animal luna = animalService.registerAnimal(new Animal(
                "Luna", "Gatto", "Europeo", LocalDate.of(2022, 4, 1),
                "F", "380260000333444", 4.1, ""));
        Owner estraneo = ownerService.registerOwner(new Owner(
                "Paolo", "Bianchi", "BNCPLA75C01H501Z", "+39 3387776655", "", PreferredChannel.EMAIL));

        assertThrows(ValidationException.class, () ->
                primaryContactService.designatePrimaryContact(luna.getId(), estraneo.getId()));
    }
}
