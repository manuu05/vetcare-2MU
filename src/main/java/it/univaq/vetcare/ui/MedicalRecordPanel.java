package it.univaq.vetcare.ui;

import it.univaq.vetcare.model.Animal;
import it.univaq.vetcare.model.MedicalVisit;
import it.univaq.vetcare.model.PrescriptionOverrideLog;
import it.univaq.vetcare.model.Veterinarian;
import it.univaq.vetcare.service.AllergySafetyBlockException;
import it.univaq.vetcare.service.AnimalService;
import it.univaq.vetcare.service.MedicalRecordService;
import it.univaq.vetcare.service.PrescriptionSafetyService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

/**
 * US-07, US-08 & US-09 (Sub-Task US09-2): Maschera GUI della Cartella Clinica Elettronica.
 * - Parte sinistra (US-08): Riepilogo pre-visita con timeline storica e andamento del peso.
 * - Parte destra (US-09 + US-07): Compilazione visita attiva, blocco attivo prescrizioni
 *   incompatibili con allergie e richiesta di Override Primario motivato.
 */
public class MedicalRecordPanel extends JPanel {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final AnimalService animalService;
    private final MedicalRecordService medicalRecordService;
    private final PrescriptionSafetyService prescriptionSafetyService;

    private final JComboBox<Animal> cmbPatient;
    private final JLabel lblWeightTrend;
    private final JTextArea historyTimelineArea;

    private final JComboBox<Veterinarian> cmbVet;
    private final JTextField txtReason;
    private final JTextField txtWeight;
    private final JTextField txtTemp;
    private final JTextArea txtAnamnesis;
    private final JTextArea txtObjective;
    private final JTextField txtDiagnosis;
    private final JTextField txtTherapy;

    private Consumer<Animal> onPatientChangedListener;
    private Runnable onVisitSavedCallback;

    public MedicalRecordPanel(AnimalService animalService,
                              MedicalRecordService medicalRecordService,
                              PrescriptionSafetyService prescriptionSafetyService) {
        this.animalService = animalService;
        this.medicalRecordService = medicalRecordService;
        this.prescriptionSafetyService = prescriptionSafetyService;

        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        // BARRA SELEZIONE PAZIENTE IN SALA D'ATTESA
        JPanel topSelector = new JPanel(new BorderLayout(10, 6));
        topSelector.setBorder(new TitledBorder("Selezione Paziente per Consultazione Pre-Visita (US-08) e Compilazione Diario Clinico (US-09)"));
        cmbPatient = new JComboBox<>();
        cmbPatient.setFont(new Font("SansSerif", Font.BOLD, 13));
        topSelector.add(new JLabel("  Paziente Selezionato: "), BorderLayout.WEST);
        topSelector.add(cmbPatient, BorderLayout.CENTER);

        // COLONNA SINISTRA: US-08 STORIA CLINICA E ANDAMENTO PESO
        JPanel historyPanel = new JPanel(new BorderLayout(6, 6));
        historyPanel.setBorder(new TitledBorder("US-08: Riepilogo Storia Clinica Pregressa & Timeline Visite Consolidate"));

        lblWeightTrend = new JLabel("Andamento Peso: -");
        lblWeightTrend.setFont(new Font("SansSerif", Font.BOLD, 12));
        lblWeightTrend.setForeground(new Color(25, 55, 109));
        lblWeightTrend.setBorder(new EmptyBorder(4, 6, 4, 6));

        historyTimelineArea = new JTextArea();
        historyTimelineArea.setEditable(false);
        historyTimelineArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        historyTimelineArea.setLineWrap(true);
        historyTimelineArea.setWrapStyleWord(true);

        historyPanel.add(lblWeightTrend, BorderLayout.NORTH);
        historyPanel.add(new JScrollPane(historyTimelineArea), BorderLayout.CENTER);

        // COLONNA DESTRA: US-09 COMPILAZIONE VISITA + US-07 CONTROLLO ALLERGIE
        JPanel visitFormPanel = new JPanel(new BorderLayout(8, 8));
        visitFormPanel.setBorder(new TitledBorder("US-09 & US-07: Compilazione Nuova Visita Clinica (con Blocco Attivo Farmaci Allergici)"));

        JPanel fieldsGrid = new JPanel(new GridLayout(0, 2, 8, 8));
        cmbVet = new JComboBox<>(Veterinarian.values());
        txtReason = new JTextField("Controllo clinico / Infezione");
        txtWeight = new JTextField("32.0");
        txtTemp = new JTextField("38.6");
        txtAnamnesis = new JTextArea(2, 24);
        txtAnamnesis.setLineWrap(true);
        txtObjective = new JTextArea(2, 24);
        txtObjective.setLineWrap(true);
        txtDiagnosis = new JTextField();
        txtTherapy = new JTextField();

        fieldsGrid.add(new JLabel("Veterinario Firmatario *:"));
        fieldsGrid.add(cmbVet);
        fieldsGrid.add(new JLabel("Motivo della Visita *:"));
        fieldsGrid.add(txtReason);
        fieldsGrid.add(new JLabel("Peso Rilevato (kg) *:"));
        fieldsGrid.add(txtWeight);
        fieldsGrid.add(new JLabel("Temperatura (°C) *:"));
        fieldsGrid.add(txtTemp);
        fieldsGrid.add(new JLabel("Anamnesi *:"));
        fieldsGrid.add(new JScrollPane(txtAnamnesis));
        fieldsGrid.add(new JLabel("Esame Obiettivo *:"));
        fieldsGrid.add(new JScrollPane(txtObjective));
        fieldsGrid.add(new JLabel("Diagnosi Clinica *:"));
        fieldsGrid.add(txtDiagnosis);
        fieldsGrid.add(new JLabel("Terapia / Farmaci (es. Amoxicillina):"));
        fieldsGrid.add(txtTherapy);

        JButton btnConsolidate = new JButton("Firma e Consolida Visita in Cartella (Stato Immutabile)");
        btnConsolidate.setBackground(new Color(22, 101, 52));
        btnConsolidate.setForeground(Color.WHITE);
        btnConsolidate.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnConsolidate.setOpaque(true);

        btnConsolidate.addActionListener(e -> handleSaveVisit());

        cmbPatient.addActionListener(e -> {
            Animal sel = (Animal) cmbPatient.getSelectedItem();
            if (sel != null) {
                txtWeight.setText(String.valueOf(sel.getWeight()));
                refreshTimelineForSelectedPatient();
                if (onPatientChangedListener != null) {
                    onPatientChangedListener.accept(sel);
                }
            }
        });

        visitFormPanel.add(fieldsGrid, BorderLayout.CENTER);
        visitFormPanel.add(btnConsolidate, BorderLayout.SOUTH);

        JSplitPane centerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, historyPanel, visitFormPanel);
        centerSplit.setResizeWeight(0.52);

        add(topSelector, BorderLayout.NORTH);
        add(centerSplit, BorderLayout.CENTER);
    }

    private void handleSaveVisit() {
        Animal patient = (Animal) cmbPatient.getSelectedItem();
        if (patient == null) {
            return;
        }
        try {
            double weight = Double.parseDouble(txtWeight.getText().trim());
            double temp = Double.parseDouble(txtTemp.getText().trim());
            Veterinarian vet = (Veterinarian) cmbVet.getSelectedItem();

            MedicalVisit draft = new MedicalVisit(
                    patient.getId(),
                    LocalDateTime.now(),
                    vet != null ? vet.getFullName() : "Dott. Bianchi",
                    txtReason.getText().trim(),
                    txtAnamnesis.getText().trim(),
                    txtObjective.getText().trim(),
                    weight,
                    temp,
                    txtDiagnosis.getText().trim(),
                    txtTherapy.getText().trim()
            );

            try {
                MedicalVisit saved = medicalRecordService.recordAndConsolidateVisit(draft, false, null);
                onVisitConsolidatedSuccess(saved);
            } catch (AllergySafetyBlockException blockEx) {
                Toolkit.getDefaultToolkit().beep();
                String justification = JOptionPane.showInputDialog(
                        this,
                        "🚨 " + blockEx.getMessage()
                                + "\n\nPer annullare e cambiare farmaco premi 'Annulla'."
                                + "\nSOLO IN EMERGENZA SALVA-VITA, inserisci la Motivazione Clinica per 'Override Primario' (min. 10 caratteri):",
                        "BLOCCO SICUREZZA PRESCRIZIONE ALLERGICA (US-07)",
                        JOptionPane.ERROR_MESSAGE
                );
                if (justification != null) {
                    MedicalVisit savedWithOverride = medicalRecordService.recordAndConsolidateVisit(
                            draft, true, justification);
                    onVisitConsolidatedSuccess(savedWithOverride);
                }
            }
        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(this, "Inserire valori numerici validi per Peso e Temperatura.",
                    "Errore Formato", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Errore Validazione Cartella Clinica", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onVisitConsolidatedSuccess(MedicalVisit saved) {
        txtAnamnesis.setText("");
        txtObjective.setText("");
        txtDiagnosis.setText("");
        txtTherapy.setText("");
        refreshPatientsListAndSelect(saved.getAnimalId());
        if (onVisitSavedCallback != null) {
            onVisitSavedCallback.run();
        }
        JOptionPane.showMessageDialog(this,
                "Visita consolidata con successo!\n" + saved.getDigitalSignature()
                        + "\nStato: NON MODIFICABILE.",
                "Cartella Clinica Aggiornata (US-09)", JOptionPane.INFORMATION_MESSAGE);
    }

    public void refreshPatientsList() {
        Animal current = (Animal) cmbPatient.getSelectedItem();
        refreshPatientsListAndSelect(current != null ? current.getId() : null);
    }

    private void refreshPatientsListAndSelect(Integer targetAnimalId) {
        cmbPatient.removeAllItems();
        Animal toSelect = null;
        for (Animal a : animalService.getAllAnimals()) {
            cmbPatient.addItem(a);
            if (targetAnimalId != null && targetAnimalId.equals(a.getId())) {
                toSelect = a;
            }
        }
        if (toSelect != null) {
            cmbPatient.setSelectedItem(toSelect);
        } else if (cmbPatient.getItemCount() > 0) {
            cmbPatient.setSelectedIndex(0);
        }
        refreshTimelineForSelectedPatient();
    }

    public void selectPatientById(int animalId) {
        refreshPatientsListAndSelect(animalId);
    }

    public void refreshTimelineForSelectedPatient() {
        Animal sel = (Animal) cmbPatient.getSelectedItem();
        if (sel == null) {
            lblWeightTrend.setText("Andamento Peso: -");
            historyTimelineArea.setText("Nessun paziente selezionato.");
            return;
        }

        lblWeightTrend.setText("📈 Andamento Peso Corporeo: "
                + medicalRecordService.buildWeightProgressionSummary(sel.getId()));

        StringBuilder sb = new StringBuilder();
        sb.append("=== RIEPILOGO CLINICO PRE-VISITA: ").append(sel.getName().toUpperCase())
          .append(" (").append(sel.getSpecies()).append(" - ").append(sel.getMedicalRecordCode()).append(") ===\n");
        if (sel.getNotes() != null && !sel.getNotes().isEmpty()) {
            sb.append("Note Anagrafiche permanenti: ").append(sel.getNotes()).append("\n");
        }
        sb.append("--------------------------------------------------------------------------\n");

        List<MedicalVisit> visits = medicalRecordService.getClinicalTimeline(sel.getId());
        if (visits.isEmpty()) {
            sb.append("Nessuna visita pregressa registrata in cartella clinica.\n");
        } else {
            for (MedicalVisit v : visits) {
                sb.append("• [").append(v.getVisitDateTime().format(FMT)).append("] ")
                  .append(v.getReason().toUpperCase())
                  .append("  | Peso: ").append(v.getWeightKg()).append(" kg")
                  .append(" | Temp: ").append(v.getTemperatureC()).append(" °C\n");
                sb.append("  Anamnesi / E.O.: ").append(v.getAnamnesis())
                  .append(" / ").append(v.getObjectiveExam()).append("\n");
                sb.append("  Diagnosi: ").append(v.getDiagnosis()).append("\n");
                if (v.getPrescribedTherapy() != null && !v.getPrescribedTherapy().isEmpty()) {
                    sb.append("  Terapia Prescritta: ").append(v.getPrescribedTherapy()).append("\n");
                }
                sb.append("  Stato: [CONSOLIDATA IMMUTABILE] - ").append(v.getDigitalSignature()).append("\n\n");
            }
        }

        List<PrescriptionOverrideLog> overrides = prescriptionSafetyService.getOverrideHistory(sel.getId());
        if (!overrides.isEmpty()) {
            sb.append("=== REGISTRO AUDIT OVERRIDE ALLERGIE (US-07) ===\n");
            for (PrescriptionOverrideLog log : overrides) {
                sb.append("  [!] ").append(log.getOverrideTimestamp().format(FMT))
                  .append(" - Farmaco: ").append(log.getPrescribedDrug())
                  .append(" (Allergia: ").append(log.getMatchedAllergen()).append(")\n")
                  .append("      Medico: ").append(log.getVeterinarianName())
                  .append(" | Motivazione: ").append(log.getClinicalJustification()).append("\n");
            }
        }

        historyTimelineArea.setText(sb.toString());
        historyTimelineArea.setCaretPosition(0);
    }

    public void setOnPatientChangedListener(Consumer<Animal> listener) {
        this.onPatientChangedListener = listener;
    }

    public void setOnVisitSavedCallback(Runnable callback) {
        this.onVisitSavedCallback = callback;
    }
}
