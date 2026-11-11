package it.univaq.vetcare.ui;

import it.univaq.vetcare.service.AllergyAlertService;
import it.univaq.vetcare.service.AnimalService;
import it.univaq.vetcare.service.OwnerService;
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
 * Finestra principale Desktop di VetCare (Sprint 1 - Gruppo 2MU).
 * Assembla il Banner Rosso Critico (US06-1), la maschera Animale (US01-1),
 * la maschera Proprietario (US02-1) e la simulazione Spike (SP-01).
 */
public class VetCareMainFrame extends JFrame {

    private final AllergyAlertService allergyAlertService;
    private final OperatingTableSchedulerSpike schedulerSpike;

    private final AllergyAlertBanner allergyBanner;
    private final AnimalFormPanel animalFormPanel;
    private final OwnerManagementPanel ownerManagementPanel;

    public VetCareMainFrame(AnimalService animalService,
                            OwnerService ownerService,
                            AllergyAlertService allergyAlertService,
                            OperatingTableSchedulerSpike schedulerSpike) {
        super("VetCare Desktop - Gestione Ambulatorio Veterinario (Sprint 1 - Gruppo 2MU)");
        this.allergyAlertService = allergyAlertService;
        this.schedulerSpike = schedulerSpike;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1120, 740);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(8, 8));

        // 1. HEADER & US-06 CRITICAL ALLERGY BANNER
        JPanel topContainer = new JPanel(new BorderLayout());
        JPanel brandBar = new JPanel(new BorderLayout());
        brandBar.setBackground(new Color(25, 55, 109));
        brandBar.setBorder(new EmptyBorder(8, 16, 8, 16));
        JLabel titleLbl = new JLabel("VETCARE CLINIC - Sistema Informativo Veterinario (Sprint 1 - Gruppo 2MU)");
        titleLbl.setForeground(Color.WHITE);
        titleLbl.setFont(new Font("SansSerif", Font.BOLD, 16));
        brandBar.add(titleLbl, BorderLayout.WEST);

        allergyBanner = new AllergyAlertBanner();
        topContainer.add(brandBar, BorderLayout.NORTH);
        topContainer.add(allergyBanner, BorderLayout.SOUTH);
        add(topContainer, BorderLayout.NORTH);

        // 2. PANNELLI MODULARI (US01-1 e US02-1)
        animalFormPanel = new AnimalFormPanel(animalService);
        ownerManagementPanel = new OwnerManagementPanel(ownerService, animalService);

        animalFormPanel.setOnAnimalSelectedListener(animal -> {
            String bannerText = this.allergyAlertService.buildCriticalBannerText(animal.getId());
            allergyBanner.updateAlertBanner(bannerText, animal.getName());
        });

        animalFormPanel.setOnAnimalSavedListener((savedAnimal, allergySubstance) -> {
            if (allergySubstance != null && !allergySubstance.trim().isEmpty()) {
                this.allergyAlertService.registerAllergy(savedAnimal.getId(), allergySubstance.trim(), "CRITICA", "Segnalata in anagrafica");
            }
            ownerManagementPanel.refreshOwnersAndRelations();
            String bannerText = this.allergyAlertService.buildCriticalBannerText(savedAnimal.getId());
            allergyBanner.updateAlertBanner(bannerText, savedAnimal.getName());
        });

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("SansSerif", Font.BOLD, 13));
        tabbedPane.addTab("1. Anagrafica Animali & Allergie (US-01 / US-06)", animalFormPanel);
        tabbedPane.addTab("2. Proprietari & Associazione N:M (US-02 / TK-02)", ownerManagementPanel);
        tabbedPane.addTab("3. Spike Tavolo Operatorio & 3 Medici (SP-01)", buildSpikeTab());

        add(tabbedPane, BorderLayout.CENTER);

        animalFormPanel.refreshAnimalTable();
        ownerManagementPanel.refreshOwnersAndRelations();
    }

    private JPanel buildSpikeTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JTextArea logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 13));

        JPanel ctrlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        ctrlPanel.setBorder(new TitledBorder("SP-01: Simulatore Rilevamento Conflitti Tavolo Operatorio Unico & 3 Medici (< 200ms)"));

        JButton btnRunSpikeSimulation = new JButton("Esegui Simulazione Conflitto & Riallocazione Deterministica");
        btnRunSpikeSimulation.setBackground(new Color(198, 40, 40));
        btnRunSpikeSimulation.setForeground(Color.WHITE);

        btnRunSpikeSimulation.addActionListener(e -> {
            schedulerSpike.clear();
            StringBuilder sb = new StringBuilder();
            sb.append("=== AVVIO SIMULAZIONE SPIKE SP-01 (Tavolo Operatorio Unico + 3 Veterinari) ===\n\n");

            LocalDateTime base = LocalDate.now().atTime(9, 0);
            AppointmentSlot surgery1 = new AppointmentSlot("SLOT-CHIR-01", 1, "Rex (Chirurgia Ortopedica)", base, base.plusHours(1), true);
            schedulerSpike.scheduleSlot(surgery1);
            sb.append("1. Prenotato con successo: ").append(surgery1).append("\n\n");

            AppointmentSlot overlappingSurgery = new AppointmentSlot("SLOT-CHIR-02", 2, "Milo (Chirurgia Addominale)", base.plusMinutes(30), base.plusMinutes(90), true);
            sb.append("2. Tentativo prenotazione simultanea sul Tavolo Operatorio Unico:\n   ").append(overlappingSurgery).append("\n");
            try {
                schedulerSpike.scheduleSlot(overlappingSurgery);
            } catch (ConcurrencyConflictException ex) {
                sb.append("   -> [CONFLITTO RILEVATO IN ").append(ex.getResolutionTimeMs()).append(" ms (< 200ms!)]\n");
                sb.append("   -> Messaggio: ").append(ex.getMessage()).append("\n");
                sb.append("   -> Slot Riallocato Proposto: ").append(ex.getProposedReallocation()).append("\n\n");
            }

            AppointmentSlot overlappingVetVisit = new AppointmentSlot("SLOT-VIS-03", 1, "Luna (Visita Vaccinale)", base.plusMinutes(15), base.plusMinutes(45), false);
            sb.append("3. Tentativo visita ordinaria su Veterinario #1 mentre e' in sala operatoria:\n   ").append(overlappingVetVisit).append("\n");
            try {
                schedulerSpike.scheduleSlot(overlappingVetVisit);
            } catch (ConcurrencyConflictException ex) {
                sb.append("   -> [CONFLITTO MEDICO RILEVATO IN ").append(ex.getResolutionTimeMs()).append(" ms (< 200ms!)]\n");
                sb.append("   -> Messaggio: ").append(ex.getMessage()).append("\n");
                sb.append("   -> Riallocazione Automatica su altro Veterinario libero: ").append(ex.getProposedReallocation()).append("\n");
            }

            logArea.setText(sb.toString());
        });

        ctrlPanel.add(btnRunSpikeSimulation);
        panel.add(ctrlPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(logArea), BorderLayout.CENTER);
        return panel;
    }
}
