package it.univaq.vetcare;

import it.univaq.vetcare.dao.*;
import it.univaq.vetcare.db.DatabaseManager;
import it.univaq.vetcare.model.*;
import it.univaq.vetcare.service.*;
import it.univaq.vetcare.spike.OperatingTableSchedulerSpike;
import it.univaq.vetcare.ui.VetCareMainFrame;

import javax.swing.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entry point dell'applicazione Desktop VetCare v2.0 (Sprint 2 - Gruppo 2MU - 19 SP).
 */
public class VetCareApp {

    public static void main(String[] args) {
        DatabaseManager dbManager = new DatabaseManager("jdbc:sqlite:vetcare.db");
        dbManager.initializeSchema();

        AnimalDAO animalDAO = new AnimalDAO(dbManager);
        OwnerDAO ownerDAO = new OwnerDAO(dbManager);
        AnimalOwnerDAO animalOwnerDAO = new AnimalOwnerDAO(dbManager);
        AllergyDAO allergyDAO = new AllergyDAO(dbManager);
        PrimaryContactDAO primaryContactDAO = new PrimaryContactDAO(dbManager);
        PrescriptionOverrideDAO overrideDAO = new PrescriptionOverrideDAO(dbManager);
        MedicalVisitDAO medicalVisitDAO = new MedicalVisitDAO(dbManager);
        AppointmentDAO appointmentDAO = new AppointmentDAO(dbManager);

        AnimalService animalService = new AnimalService(animalDAO);
        OwnerService ownerService = new OwnerService(ownerDAO, animalOwnerDAO);
        AllergyAlertService allergyAlertService = new AllergyAlertService(allergyDAO);
        PrimaryContactService primaryContactService = new PrimaryContactService(primaryContactDAO);
        PrescriptionSafetyService prescriptionSafetyService = new PrescriptionSafetyService(allergyAlertService, overrideDAO);
        MedicalRecordService medicalRecordService = new MedicalRecordService(medicalVisitDAO, prescriptionSafetyService);
        ClinicAgendaService clinicAgendaService = new ClinicAgendaService(appointmentDAO);
        OperatingTableSchedulerSpike schedulerSpike = new OperatingTableSchedulerSpike();

        seedDemoDataIfEmpty(animalService, ownerService, allergyAlertService,
                primaryContactService, medicalRecordService, clinicAgendaService);

        SwingUtilities.invokeLater(() -> {
            VetCareMainFrame frame = new VetCareMainFrame(
                    animalService, ownerService, allergyAlertService, schedulerSpike,
                    primaryContactService, medicalRecordService, prescriptionSafetyService, clinicAgendaService
            );
            frame.setVisible(true);
        });
    }

    private static void seedDemoDataIfEmpty(AnimalService animalService,
                                            OwnerService ownerService,
                                            AllergyAlertService allergyAlertService,
                                            PrimaryContactService primaryContactService,
                                            MedicalRecordService medicalRecordService,
                                            ClinicAgendaService clinicAgendaService) {
        if (!animalService.getAllAnimals().isEmpty()) {
            return;
        }

        // Pazienti Demo (US-01 & US-06)
        Animal rex = animalService.registerAnimal(new Animal(
                "Rex", "Cane", "Pastore Tedesco",
                LocalDate.of(2021, 5, 14), "M",
                "380260000000001", 30.5, "Paziente cardiopatico seguito dal Dott. Bianchi"
        ));
        allergyAlertService.registerAllergy(rex.getId(), "PENICILLINE", "CRITICA", "Shock anafilattico documentato ad Amoxicillina");

        Animal luna = animalService.registerAnimal(new Animal(
                "Luna", "Gatto", "Europeo",
                LocalDate.of(2023, 3, 10), "F",
                "380260000000002", 4.2, "Vaccinazioni regolari"
        ));

        Animal milo = animalService.registerAnimal(new Animal(
                "Milo", "Coniglio", "Ariete Nano",
                LocalDate.of(2024, 1, 20), "M",
                "380260000000003", 1.8, "Controllo dentale periodico"
        ));

        // Proprietari e Referente Primario (US-02 & US-03)
        Owner owner1 = ownerService.registerOwner(new Owner(
                "Marco", "Rossi", "RSSMRC85M01H501Z",
                "+39 3471122334", "marco.rossi@email.it", PreferredChannel.TELEFONO
        ));
        Owner owner2 = ownerService.registerOwner(new Owner(
                "Giulia", "Bianchi", "BNCGLI88A41H501W",
                "+39 3209988776", "giulia.bianchi@email.it", PreferredChannel.SMS
        ));

        primaryContactService.addCoOwner(rex.getId(), owner1.getId(), "Comproprietario Familiare", false);
        primaryContactService.addCoOwner(rex.getId(), owner2.getId(), "Comproprietario Familiare", true);
        primaryContactService.addCoOwner(luna.getId(), owner1.getId(), "Proprietario Unico", true);
        primaryContactService.addCoOwner(milo.getId(), owner2.getId(), "Affidatario Temporaneo", true);

        // Storico Visite Consolidate per Demo US-08 & US-09 (Andamento peso di Rex: 30.5 -> 32.0 kg)
        medicalRecordService.recordAndConsolidateVisit(new MedicalVisit(
                rex.getId(), LocalDateTime.now().minusMonths(4),
                Veterinarian.VET_2.getFullName(),
                "Visita cardiologica di controllo",
                "Lieve affaticamento dopo corsa al parco",
                "Soffio sistolico grado II/VI, mucose rosee",
                30.5, 38.5,
                "Insufficienza mitralica lieve compensata",
                "Benazepril 5mg 1 cpr/die"
        ), false, null);

        medicalRecordService.recordAndConsolidateVisit(new MedicalVisit(
                rex.getId(), LocalDateTime.now().minusDays(15),
                Veterinarian.VET_1.getFullName(),
                "Zoppia arto posteriore destro",
                "Dolore acuto dopo salto",
                "Articolazione ginocchio dx tumefatta",
                32.0, 38.7,
                "Distorsione legamento crociato craniale",
                "Meloxicam sospensione orale per 5 giorni"
        ), false, null);

        // Appuntamenti Demo Agenda 3 Veterinari (US-11)
        LocalDate today = LocalDate.now();
        clinicAgendaService.bookAppointment(
                rex.getId(), 1, VisitType.CONSULTO_PRE_OPERATORIO,
                today.atTime(10, 0), "Valutazione ortopedica ginocchio dx");
        clinicAgendaService.bookAppointment(
                luna.getId(), 2, VisitType.VISITA_SPECIALISTICA,
                today.atTime(10, 0), "Ecografia addominale di controllo (45 min)");
        clinicAgendaService.bookAppointment(
                milo.getId(), 3, VisitType.PROFILASSI_VACCINO,
                today.atTime(11, 0), "Vaccinazione MEV + Mixomatosi (20 min)");
    }
}
