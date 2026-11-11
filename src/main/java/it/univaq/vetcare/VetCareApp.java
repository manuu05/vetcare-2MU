package it.univaq.vetcare;

import it.univaq.vetcare.dao.AllergyDAO;
import it.univaq.vetcare.dao.AnimalDAO;
import it.univaq.vetcare.dao.AnimalOwnerDAO;
import it.univaq.vetcare.dao.OwnerDAO;
import it.univaq.vetcare.db.DatabaseManager;
import it.univaq.vetcare.model.Animal;
import it.univaq.vetcare.model.Owner;
import it.univaq.vetcare.model.PreferredChannel;
import it.univaq.vetcare.service.AllergyAlertService;
import it.univaq.vetcare.service.AnimalService;
import it.univaq.vetcare.service.OwnerService;
import it.univaq.vetcare.spike.OperatingTableSchedulerSpike;
import it.univaq.vetcare.ui.VetCareMainFrame;

import javax.swing.*;
import java.time.LocalDate;

/**
 * Entry point dell'applicazione Desktop VetCare (Sprint 1 - Gruppo 2MU).
 */
public class VetCareApp {

    public static void main(String[] args) {
        DatabaseManager dbManager = new DatabaseManager("jdbc:sqlite:vetcare.db");
        dbManager.initializeSchema();

        AnimalDAO animalDAO = new AnimalDAO(dbManager);
        OwnerDAO ownerDAO = new OwnerDAO(dbManager);
        AnimalOwnerDAO animalOwnerDAO = new AnimalOwnerDAO(dbManager);
        AllergyDAO allergyDAO = new AllergyDAO(dbManager);

        AnimalService animalService = new AnimalService(animalDAO);
        OwnerService ownerService = new OwnerService(ownerDAO, animalOwnerDAO);
        AllergyAlertService allergyAlertService = new AllergyAlertService(allergyDAO);
        OperatingTableSchedulerSpike schedulerSpike = new OperatingTableSchedulerSpike();

        seedDemoDataIfEmpty(animalService, ownerService, allergyAlertService);

        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
            VetCareMainFrame frame = new VetCareMainFrame(
                    animalService, ownerService, allergyAlertService, schedulerSpike
            );
            frame.setVisible(true);
        });
    }

    private static void seedDemoDataIfEmpty(AnimalService animalService,
                                             OwnerService ownerService,
                                             AllergyAlertService allergyAlertService) {
        if (!animalService.getAllAnimals().isEmpty()) {
            return;
        }
        // Paziente 1: Rex (con allergia mortale alle Penicilline per demo US-06)
        Animal rex = animalService.registerAnimal(new Animal(
                "Rex", "Cane", "Pastore Tedesco",
                LocalDate.of(2021, 5, 14), "M",
                "380260000000001", 32.0, "Paziente cardiopatico seguito dal Dott. Bianchi"
        ));
        allergyAlertService.registerAllergy(rex.getId(), "PENICILLINE", "CRITICA", "Shock anafilattico pregresso");

        // Paziente 2: Luna (nessuna allergia)
        Animal luna = animalService.registerAnimal(new Animal(
                "Luna", "Gatto", "Europeo",
                LocalDate.of(2023, 3, 10), "F",
                "380260000000002", 4.2, "Vaccinazioni regolari"
        ));

        // Proprietari per demo US-02 e relazione N:M TK-02
        Owner owner1 = ownerService.registerOwner(new Owner(
                "Marco", "Rossi", "RSSMRC85M01H501Z",
                "+39 3471122334", "marco.rossi@email.it", PreferredChannel.TELEFONO
        ));
        Owner owner2 = ownerService.registerOwner(new Owner(
                "Giulia", "Bianchi", "BNCGLI88A41H501W",
                "+39 3209988776", "giulia.bianchi@email.it", PreferredChannel.SMS
        ));

        // Comproprietà su Rex (2 proprietari per 1 animale)
        ownerService.associateAnimalToOwner(rex.getId(), owner1.getId(), "Comproprietario");
        ownerService.associateAnimalToOwner(rex.getId(), owner2.getId(), "Comproprietario");
        ownerService.associateAnimalToOwner(luna.getId(), owner1.getId(), "Proprietario Unico");
    }
}
