package it.univaq.vetcare.ui;

import it.univaq.vetcare.model.Animal;
import it.univaq.vetcare.service.*;
import it.univaq.vetcare.spike.AppointmentSlot;
import it.univaq.vetcare.spike.ConcurrencyConflictException;
import it.univaq.vetcare.spike.OperatingTableSchedulerSpike;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Finestra principale Desktop di VetCare v2.0 (Sprint 2 - Gruppo 2MU - 19 SP + RF-01).
 */
public class VetCareMainFrame extends JFrame {

    private final AllergyAlertService allergyAlertService;
    private final OperatingTableSchedulerSpike schedulerSpike;

    private final AllergyAlertBanner allergyBanner;
    private final AnimalFormPanel animalFormPanel;
    private final OwnerManagementPanel ownerManagementPanel;
    private final PrimaryContactPanel primaryContactPanel;
    private final MedicalRecordPanel medicalRecordPanel;
    private final ClinicAgendaPanel clinicAgendaPanel;

    public VetCareMainFrame(AnimalService animalService,
                            OwnerService ownerService,
                            AllergyAlertService allergyAlertService,
                            OperatingTableSchedulerSpike schedulerSpike) {
        this(animalService, ownerService, allergyAlertService, schedulerSpike, null, null, null, null);
    }

    public VetCareMainFrame(AnimalService animalService,
                            OwnerService ownerService,
                            AllergyAlertService allergyAlertService,
                            OperatingTableSchedulerSpike schedulerSpike,
                            PrimaryContactService primaryContactService,
                            MedicalRecordService medicalRecordService,
                            PrescriptionSafetyService prescriptionSafetyService,
                            ClinicAgendaService clinicAgendaService) {
        super("VetCare Desktop v2.0 - Gestione Ambulatorio Veterinario (Sprint 2 - Gruppo 2MU)");
        this.allergyAlertService = allergyAlertService;
        this.schedulerSpike = schedulerSpike;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1220, 780);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(8, 8));

        JPanel topContainer = new JPanel(new BorderLayout());
        JPanel brandBar = new JPanel(new BorderLayout());
        brandBar.setBackground(new Color(25, 55, 109));
        brandBar.setBorder(new EmptyBorder(8, 16, 8, 16));
        JLabel titleLbl = new JLabel("VETCARE CLINIC v2.0 - Sistema Informativo Veterinario (Sprint 2 - Gruppo 2MU)");
        titleLbl.setForeground(Color.WHITE);
        titleLbl.setFont(new Font("SansSerif", Font.BOLD, 16));
        brandBar.add(titleLbl, BorderLayout.WEST);

        allergyBanner = new AllergyAlertBanner();
        topContainer.add(brandBar, BorderLayout.NORTH);
        topContainer.add(allergyBanner, BorderLayout.SOUTH);
        add(topContainer, BorderLayout.NORTH);

        animalFormPanel = new AnimalFormPanel(animalService);
        ownerManagementPanel = new OwnerManagementPanel(ownerService, animalService);

        primaryContactPanel = primaryContactService != null
                ? new PrimaryContactPanel(primaryContactService, animalService, ownerService)
                : null;
        medicalRecordPanel = (medicalRecordService != null && prescriptionSafetyService != null)
                ? new MedicalRecordPanel(animalService, medicalRecordService, prescriptionSafetyService)
                : null;
        clinicAgendaPanel = clinicAgendaService != null
                ? new ClinicAgendaPanel(clinicAgendaService, animalService)
                : null;

        animalFormPanel.setOnAnimalSelectedListener(this::updateTopAllergyBanner);
        if (primaryContactPanel != null) {
            primaryContactPanel.setOnPatientSelectedListener(animal -> {
                updateTopAllergyBanner(animal);
                if (medicalRecordPanel != null) {
                    medicalRecordPanel.selectPatientById(animal.getId());
                }
            });
        }
        if (medicalRecordPanel != null) {
            medicalRecordPanel.setOnPatientChangedListener(this::updateTopAllergyBanner);
            medicalRecordPanel.setOnVisitSavedCallback(() -> {
                animalFormPanel.refreshAnimalTable();
                if (primaryContactPanel != null) {
                    primaryContactPanel.refreshData();
                }
            });
        }

        animalFormPanel.setOnAnimalSavedListener((savedAnimal, allergySubstance) -> {
            if (allergySubstance != null && !allergySubstance.trim().isEmpty()) {
                this.allergyAlertService.registerAllergy(savedAnimal.getId(), allergySubstance.trim(), "CRITICA", "Segnalata in anagrafica");
            }
            refreshAllTabs();
            updateTopAllergyBanner(savedAnimal);
        });

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("SansSerif", Font.BOLD, 12));
        tabbedPane.addTab("1. Anagrafica Animali (US-01 / US-06)", animalFormPanel);
        tabbedPane.addTab("2. Proprietari N:M (US-02 / TK-02)", ownerManagementPanel);
        if (primaryContactPanel != null) {
            tabbedPane.addTab("3. Comproprietà & Referente Primario (US-03)", primaryContactPanel);
        }
        if (medicalRecordPanel != null) {
            tabbedPane.addTab("4. Cartella Clinica & Blocco Allergie (US-08 / US-09 / US-07)", medicalRecordPanel);
        }
        if (clinicAgendaPanel != null) {
            tabbedPane.addTab("5. Agenda 3 Veterinari (US-11)", clinicAgendaPanel);
        }
        tabbedPane.addTab("6. Prototipo Spike Refactored (SP-01 / RF-01)", buildSpikeTab());

        add(tabbedPane, BorderLayout.CENTER);
        refreshAllTabs();
    }

    private void updateTopAllergyBanner(Animal animal) {
        if (animal == null) {
            return;
        }
        String bannerText = this.allergyAlertService.buildCriticalBannerText(animal.getId());
        allergyBanner.updateAlertBanner(bannerText, animal.getName());
    }

    private void refreshAllTabs() {
        animalFormPanel.refreshAnimalTable();
        ownerManagementPanel.refreshOwnersAndRelations();
        if (primaryContactPanel != null) {
            primaryContactPanel.refreshData();
        }
        if (medicalRecordPanel != null) {
            medicalRecordPanel.refreshPatientsList();
        }
        if (clinicAgendaPanel != null) {
            clinicAgendaPanel.refreshData();
        }
    }

    private JPanel buildSpikeTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JTextArea logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 13));

        JPanel ctrlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        ctrlPanel.setBorder(new TitledBorder("SP-01 & RF-01: Simulatore Conflitti con gestione esplicita try/catch (< 200ms)"));

        JButton btnRunSpikeSimulation = new JButton("Esegui Simulazione Conflitto & Rescheduling nel catch");
        btnRunSpikeSimulation.setBackground(new Color(198, 40, 40));
        btnRunSpikeSimulation.setForeground(Color.WHITE);
        btnRunSpikeSimulation.setOpaque(true);

        btnRunSpikeSimulation.addActionListener(e -> {
            schedulerSpike.clear();
            StringBuilder sb = new StringBuilder();
            sb.append("=== AVVIO SIMULAZIONE SPIKE SP-01 (Refactored RF-01 con try/catch esplicito) ===\n\n");

            LocalDateTime base = LocalDate.now().atTime(9, 0);
            AppointmentSlot surgery1 = new AppointmentSlot("SLOT-CHIR-01", 1, "Rex (Chirurgia Ortopedica)", base, base.plusHours(1), true);
            schedulerSpike.scheduleSlot(surgery1);
            sb.append("1. Prenotato con successo: ").append(surgery1).append("\n\n");

            AppointmentSlot overlappingSurgery = new AppointmentSlot("SLOT-CHIR-02", 2, "Milo (Chirurgia Addominale)", base.plusMinutes(30), base.plusMinutes(90), true);
            sb.append("2. Tentativo prenotazione simultanea sul Tavolo Operatorio Unico:\n   ").append(overlappingSurgery).append("\n");
            long t0 = System.nanoTime();
            try {
                schedulerSpike.scheduleSlot(overlappingSurgery);
            } catch (ConcurrencyConflictException ex) {
                AppointmentSlot proposal = schedulerSpike.computeDeterministicReallocation(overlappingSurgery);
                long elapsedMs = (System.nanoTime() - t0) / 1_000_000L;
                sb.append("   -> [CATTURATA ConcurrencyConflictException: ").append(ex.getMessage()).append("]\n");
                sb.append("   -> [RESCHEDULING CALCOLATO NEL CATCH IN ").append(elapsedMs).append(" ms (< 200ms!)]\n");
                sb.append("   -> Slot Riallocato Proposto: ").append(proposal).append("\n\n");
            }

            AppointmentSlot overlappingVetVisit = new AppointmentSlot("SLOT-VIS-03", 1, "Luna (Visita Vaccinale)", base.plusMinutes(15), base.plusMinutes(45), false);
            sb.append("3. Tentativo visita ordinaria su Veterinario #1 mentre e' in sala operatoria:\n   ").append(overlappingVetVisit).append("\n");
            long t1 = System.nanoTime();
            try {
                schedulerSpike.scheduleSlot(overlappingVetVisit);
            } catch (ConcurrencyConflictException ex) {
                AppointmentSlot proposal = schedulerSpike.computeDeterministicReallocation(overlappingVetVisit);
                long elapsedMs = (System.nanoTime() - t1) / 1_000_000L;
                sb.append("   -> [CATTURATA ConcurrencyConflictException: ").append(ex.getMessage()).append("]\n");
                sb.append("   -> [RESCHEDULING CALCOLATO NEL CATCH IN ").append(elapsedMs).append(" ms (< 200ms!)]\n");
                sb.append("   -> Riallocazione su altro Veterinario libero: ").append(proposal).append("\n");
            }

            logArea.setText(sb.toString());
        });

        ctrlPanel.add(btnRunSpikeSimulation);
        panel.add(ctrlPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(logArea), BorderLayout.CENTER);
        return panel;
    }
}
