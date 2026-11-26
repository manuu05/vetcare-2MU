package it.univaq.vetcare.ui;

import it.univaq.vetcare.model.Animal;
import it.univaq.vetcare.model.ClinicAppointment;
import it.univaq.vetcare.model.Veterinarian;
import it.univaq.vetcare.model.VisitType;
import it.univaq.vetcare.service.AnimalService;
import it.univaq.vetcare.service.ClinicAgendaService;
import it.univaq.vetcare.service.ValidationException;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * US-11 (Sub-Task US11-4): Maschera GUI dell'Agenda Giornaliera e Settimanale
 * affiancata su 3 colonne per i 3 Veterinari dell'ambulatorio.
 * Nel blocco catch invoca esplicitamente suggestNextAvailableSlot() per proporre il nuovo orario.
 */
public class ClinicAgendaPanel extends JPanel {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final ClinicAgendaService agendaService;
    private final AnimalService animalService;

    private final JComboBox<Animal> cmbAnimal;
    private final JComboBox<Veterinarian> cmbVet;
    private final JComboBox<VisitType> cmbVisitType;
    private final JTextField txtDate;
    private final JTextField txtTime;
    private final JTextField txtNotes;

    private final JTextArea[] vetColumnAreas = new JTextArea[3];
    private final JTextArea weeklySummaryArea;

    public ClinicAgendaPanel(ClinicAgendaService agendaService, AnimalService animalService) {
        this.agendaService = agendaService;
        this.animalService = animalService;

        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel bookingPanel = new JPanel(new BorderLayout(8, 8));
        bookingPanel.setBorder(new TitledBorder(
                "US-11: Prenotazione Appuntamento su Agenda Unica (Durata Slot Automatica in base al Tipo di Visita)"));

        JPanel grid = new JPanel(new GridLayout(0, 4, 8, 6));
        cmbAnimal = new JComboBox<>();
        cmbVet = new JComboBox<>(Veterinarian.values());
        cmbVisitType = new JComboBox<>(VisitType.values());
        txtDate = new JTextField(LocalDate.now().toString());
        txtTime = new JTextField("10:00");
        txtNotes = new JTextField();

        grid.add(new JLabel("Paziente *:"));
        grid.add(cmbAnimal);
        grid.add(new JLabel("Veterinario *:"));
        grid.add(cmbVet);
        grid.add(new JLabel("Tipo di Visita & Durata *:"));
        grid.add(cmbVisitType);
        grid.add(new JLabel("Data (YYYY-MM-DD) *:"));
        grid.add(txtDate);
        grid.add(new JLabel("Ora Inizio (HH:MM) *:"));
        grid.add(txtTime);
        grid.add(new JLabel("Note Appuntamento:"));
        grid.add(txtNotes);

        JButton btnBook = new JButton("Prenota in Agenda");
        btnBook.setBackground(new Color(29, 78, 216));
        btnBook.setForeground(Color.WHITE);
        btnBook.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnBook.setOpaque(true);
        btnBook.addActionListener(e -> handleBooking());

        JButton btnRefreshDate = new JButton("Aggiorna Giorno / Settimana");
        btnRefreshDate.addActionListener(e -> refreshAgendaViews());

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 2));
        btnRow.add(btnRefreshDate);
        btnRow.add(btnBook);

        bookingPanel.add(grid, BorderLayout.CENTER);
        bookingPanel.add(btnRow, BorderLayout.SOUTH);

        JPanel threeColumnsPanel = new JPanel(new GridLayout(1, 3, 10, 0));
        Veterinarian[] vets = Veterinarian.values();
        for (int i = 0; i < 3; i++) {
            JPanel col = new JPanel(new BorderLayout());
            col.setBorder(new TitledBorder(vets[i].toString()));
            vetColumnAreas[i] = new JTextArea();
            vetColumnAreas[i].setEditable(false);
            vetColumnAreas[i].setFont(new Font("Monospaced", Font.PLAIN, 12));
            vetColumnAreas[i].setLineWrap(true);
            vetColumnAreas[i].setWrapStyleWord(true);
            col.add(new JScrollPane(vetColumnAreas[i]), BorderLayout.CENTER);
            threeColumnsPanel.add(col);
        }

        weeklySummaryArea = new JTextArea(5, 80);
        weeklySummaryArea.setEditable(false);
        weeklySummaryArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JPanel weeklyBox = new JPanel(new BorderLayout());
        weeklyBox.setBorder(new TitledBorder("Vista Settimanale dell'Ambulatorio (7 Giorni)"));
        weeklyBox.add(new JScrollPane(weeklySummaryArea), BorderLayout.CENTER);

        JSplitPane agendaSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, threeColumnsPanel, weeklyBox);
        agendaSplit.setResizeWeight(0.68);

        add(bookingPanel, BorderLayout.NORTH);
        add(agendaSplit, BorderLayout.CENTER);
    }

    private void handleBooking() {
        Animal selAnimal = (Animal) cmbAnimal.getSelectedItem();
        Veterinarian selVet = (Veterinarian) cmbVet.getSelectedItem();
        VisitType selType = (VisitType) cmbVisitType.getSelectedItem();
        if (selAnimal == null || selVet == null || selType == null) {
            return;
        }
        LocalDateTime requestedStart = null;
        try {
            LocalDate date = LocalDate.parse(txtDate.getText().trim());
            LocalTime time = LocalTime.parse(txtTime.getText().trim());
            requestedStart = LocalDateTime.of(date, time);

            ClinicAppointment saved = agendaService.bookAppointment(
                    selAnimal.getId(),
                    selVet.getId(),
                    selType,
                    requestedStart,
                    txtNotes.getText().trim()
            );
            refreshAgendaViews();
            JOptionPane.showMessageDialog(this,
                    "Appuntamento confermato per " + selAnimal.getName() + "!\n"
                            + selVet.getFullName() + " | Fascia occupata: "
                            + saved.getStartDateTime().format(TIME_FMT) + " - "
                            + saved.getEndDateTime().format(TIME_FMT),
                    "Prenotazione Registrata (US-11)", JOptionPane.INFORMATION_MESSAGE);
        } catch (ValidationException ex) {
            String suggestionMsg = "";
            if (requestedStart != null) {
                LocalDateTime nextFree = agendaService.suggestNextAvailableSlot(
                        selVet.getId(), requestedStart, selType);
                txtTime.setText(nextFree.format(TIME_FMT));
                suggestionMsg = "\n\nPrimo slot libero calcolato per " + selVet.getFullName()
                        + ": ore " + nextFree.format(TIME_FMT) + " (già preimpostato nel campo Ora).";
            }
            JOptionPane.showMessageDialog(this, ex.getMessage() + suggestionMsg,
                    "Sovrapposizione Oraria Rilevata (US-11)", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Formato data o ora non valido (usare YYYY-MM-DD e HH:MM).",
                    "Errore Input", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void refreshData() {
        cmbAnimal.removeAllItems();
        for (Animal a : animalService.getAllAnimals()) {
            cmbAnimal.addItem(a);
        }
        refreshAgendaViews();
    }

    public void refreshAgendaViews() {
        LocalDate targetDate;
        try {
            targetDate = LocalDate.parse(txtDate.getText().trim());
        } catch (Exception e) {
            targetDate = LocalDate.now();
            txtDate.setText(targetDate.toString());
        }

        Veterinarian[] vets = Veterinarian.values();
        for (int i = 0; i < 3; i++) {
            List<ClinicAppointment> slots = agendaService.getDailyAgendaForVet(targetDate, vets[i].getId());
            StringBuilder sb = new StringBuilder();
            sb.append("Agenda del ").append(targetDate).append("\n");
            sb.append("----------------------------------\n");
            if (slots.isEmpty()) {
                sb.append("(Nessun appuntamento programmato)\n");
            } else {
                for (ClinicAppointment a : slots) {
                    sb.append(a.getStartDateTime().format(TIME_FMT))
                      .append(" - ")
                      .append(a.getEndDateTime().format(TIME_FMT))
                      .append(" | ").append(a.getAnimalName()).append("\n")
                      .append("   ").append(a.getVisitType().getLabel()).append("\n");
                    if (a.getClinicalNotes() != null && !a.getClinicalNotes().isEmpty()) {
                        sb.append("   Note: ").append(a.getClinicalNotes()).append("\n");
                    }
                    sb.append("\n");
                }
            }
            vetColumnAreas[i].setText(sb.toString());
        }

        List<ClinicAppointment> weekly = agendaService.getWeeklyAgenda(targetDate);
        StringBuilder wsb = new StringBuilder();
        wsb.append("Appuntamenti nei 7 giorni da ").append(targetDate)
           .append(" a ").append(targetDate.plusDays(6))
           .append(" (Totale: ").append(weekly.size()).append("):\n");
        for (ClinicAppointment a : weekly) {
            Veterinarian v = Veterinarian.fromId(a.getVeterinarianId());
            wsb.append(" • ").append(a.getStartDateTime().toLocalDate())
               .append(" [").append(a.getStartDateTime().format(TIME_FMT))
               .append("-").append(a.getEndDateTime().format(TIME_FMT)).append("] ")
               .append(v.getFullName()).append(" -> Paziente: ").append(a.getAnimalName())
               .append(" | ").append(a.getVisitType().getLabel()).append("\n");
        }
        weeklySummaryArea.setText(wsb.toString());
    }
}
