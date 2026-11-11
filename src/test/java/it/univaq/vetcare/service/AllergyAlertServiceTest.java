package it.univaq.vetcare.service;

import it.univaq.vetcare.dao.AllergyDAO;
import it.univaq.vetcare.dao.AnimalDAO;
import it.univaq.vetcare.db.DatabaseManager;
import it.univaq.vetcare.model.Animal;
import it.univaq.vetcare.ui.AllergyAlertBanner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * US-06 (Sub-Task US06-3): Test di accettazione BDD automatici sul caso del cane
 * allergico ad antibiotico (Penicilline), verifica cache in memoria e Banner Rosso UI.
 */
public class AllergyAlertServiceTest {

    private File tempDbFile;
    private AnimalService animalService;
    private AllergyAlertService allergyAlertService;

    @BeforeEach
    public void setUp() throws Exception {
        tempDbFile = File.createTempFile("vetcare_test_us06_", ".db");
        DatabaseManager dbManager = new DatabaseManager("jdbc:sqlite:" + tempDbFile.getAbsolutePath());
        dbManager.initializeSchema();

        animalService = new AnimalService(new AnimalDAO(dbManager));
        allergyAlertService = new AllergyAlertService(new AllergyDAO(dbManager));
    }

    @AfterEach
    public void tearDown() {
        if (tempDbFile != null && tempDbFile.exists()) {
            tempDbFile.delete();
        }
    }

    @Test
    public void testBddDogAllergicToPenicillinsDisplaysCriticalRedBanner() {
        // Dato un animale con allergia registrata a una classe farmacologica (es. Penicilline)
        Animal dog = animalService.registerAnimal(new Animal(
                "Rex", "Cane", "Pastore Tedesco",
                LocalDate.of(2020, 5, 10), "M",
                "380260000777888", 31.0, ""
        ));
        allergyAlertService.registerAllergy(dog.getId(), "Penicilline", "CRITICA", "Shock anafilattico");

        // Quando il veterinario seleziona l'animale dal sistema
        String bannerText = allergyAlertService.buildCriticalBannerText(dog.getId());
        AllergyAlertBanner bannerUi = new AllergyAlertBanner();
        bannerUi.updateAlertBanner(bannerText, dog.getName());

        // Allora l'interfaccia mostra un banner rosso permanente 'ATTENZIONE: ANIMALE ALLERGICO A PENICILLINE'
        assertEquals("ATTENZIONE: ANIMALE ALLERGICO A PENICILLINE", bannerText);
        assertTrue(bannerUi.isCriticalAlertActive(), "Il banner deve attivare lo stato di allerta critica");
        assertEquals(AllergyAlertBanner.CRITICAL_RED_BG, bannerUi.getBackground(), "Lo sfondo del banner deve essere rosso acceso");
        assertTrue(bannerUi.getDisplayedText().contains("ATTENZIONE: ANIMALE ALLERGICO A PENICILLINE"));
        assertTrue(allergyAlertService.isCached(dog.getId()), "Le allergie del paziente devono essere mantenute nella cache in memoria");
    }

    @Test
    public void testAnimalWithoutAllergiesShowsSafeGreenBanner() {
        Animal healthyCat = animalService.registerAnimal(new Animal(
                "Luna", "Gatto", "Europeo",
                LocalDate.of(2022, 9, 1), "F",
                "380260000111222", 4.1, ""
        ));

        String bannerText = allergyAlertService.buildCriticalBannerText(healthyCat.getId());
        AllergyAlertBanner bannerUi = new AllergyAlertBanner();
        bannerUi.updateAlertBanner(bannerText, healthyCat.getName());

        assertEquals("", bannerText);
        assertFalse(bannerUi.isCriticalAlertActive());
        assertEquals(AllergyAlertBanner.SAFE_GREEN_BG, bannerUi.getBackground());
    }
}
