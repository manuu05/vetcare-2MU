package it.univaq.vetcare.dao;

import it.univaq.vetcare.db.DatabaseManager;
import it.univaq.vetcare.model.Animal;
import it.univaq.vetcare.model.Owner;
import it.univaq.vetcare.model.PreferredChannel;
import it.univaq.vetcare.service.AnimalService;
import it.univaq.vetcare.service.OwnerService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TK-02: Test per la relazione N:M tra Animali e Proprietari
 * (comproprietà, affido, referenti multipli e navigabilità bidirezionale).
 */
public class AnimalOwnerRelationTest {

    private File tempDbFile;
    private AnimalService animalService;
    private OwnerService ownerService;
    private AnimalOwnerDAO animalOwnerDAO;

    @BeforeEach
    public void setUp() throws Exception {
        tempDbFile = File.createTempFile("vetcare_test_tk02_", ".db");
        DatabaseManager dbManager = new DatabaseManager("jdbc:sqlite:" + tempDbFile.getAbsolutePath());
        dbManager.initializeSchema();

        AnimalDAO animalDAO = new AnimalDAO(dbManager);
        OwnerDAO ownerDAO = new OwnerDAO(dbManager);
        animalOwnerDAO = new AnimalOwnerDAO(dbManager);

        animalService = new AnimalService(animalDAO);
        ownerService = new OwnerService(ownerDAO, animalOwnerDAO);
    }

    @AfterEach
    public void tearDown() {
        if (tempDbFile != null && tempDbFile.exists()) {
            tempDbFile.delete();
        }
    }

    @Test
    public void testBidirectionalManyToManyRelationship() throws Exception {
        // Dato il database inizializzato con 2 proprietari e 3 animali
        Owner owner1 = ownerService.registerOwner(new Owner("Mario", "Rossi", "RSSMRA80A01H501U", "3331112223", "m.rossi@mail.it", PreferredChannel.TELEFONO));
        Owner owner2 = ownerService.registerOwner(new Owner("Anna", "Verdi", "VRDNNA82B41H501K", "3334445556", "a.verdi@mail.it", PreferredChannel.SMS));

        Animal a1 = animalService.registerAnimal(new Animal("Rex", "Cane", "Pastore", LocalDate.of(2020, 1, 10), "M", "380260000000101", 30.0, ""));
        Animal a2 = animalService.registerAnimal(new Animal("Milo", "Gatto", "Siamese", LocalDate.of(2021, 4, 15), "M", "380260000000102", 4.5, ""));
        Animal a3 = animalService.registerAnimal(new Animal("kira", "Cane", "Labrador", LocalDate.of(2022, 8, 20), "F", "380260000000103", 25.0, ""));

        // Quando si associano 2 proprietari diversi allo stesso animale (a1)
        animalOwnerDAO.linkMultipleOwnersTransactionally(a1.getId(), Arrays.asList(owner1.getId(), owner2.getId()), "Comproprietario");

        // E si associano 3 animali allo stesso proprietario (owner1)
        ownerService.associateAnimalToOwner(a2.getId(), owner1.getId(), "Proprietario");
        ownerService.associateAnimalToOwner(a3.getId(), owner1.getId(), "Proprietario");

        // Allora il database memorizza correttamente le relazioni senza duplicazione e con navigabilità bidirezionale
        List<Owner> ownersOfA1 = ownerService.getOwnersOfAnimal(a1.getId());
        assertEquals(2, ownersOfA1.size(), "L'animale Rex deve avere esattamente 2 comproprietari associati");

        List<Animal> animalsOfOwner1 = ownerService.getAnimalsOfOwner(owner1.getId());
        assertEquals(3, animalsOfOwner1.size(), "Il proprietario Mario Rossi deve risultare associato a 3 animali");

        assertEquals(2, ownerService.getAllOwners().size(), "Non deve esserci alcuna duplicazione nella tabella anagrafica proprietari");
    }
}
