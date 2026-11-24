package it.univaq.vetcare.ui;

import it.univaq.vetcare.model.Animal;
import it.univaq.vetcare.model.Owner;
import it.univaq.vetcare.model.OwnershipDetail;
import it.univaq.vetcare.service.AnimalService;
import it.univaq.vetcare.service.OwnerService;
import it.univaq.vetcare.service.PrimaryContactService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * US-03 (Sub-Task US03-2): Maschera GUI per la gestione dei comproprietari / affido familiare
 * e per la selezione del Referente Primario di contatto per ciascun paziente animale.
 */
public class PrimaryContactPanel extends JPanel {

    private final PrimaryContactService primaryContactService;
    private final AnimalService animalService;
    private final OwnerService ownerService;

    private final DefaultTableModel animalModel;
    private final JTable animalTable;
    private final JComboBox<Owner> cmbOwners;
    private final JComboBox<String> cmbRole;
    private final JCheckBox chkPrimary;
    private final JTextArea detailsArea;

    private Animal selectedAnimal;
    private Consumer<Animal> onPatientSelectedListener;

    public PrimaryContactPanel(PrimaryContactService primaryContactService,
                               AnimalService animalService,
                               OwnerService ownerService) {
        this.primaryContactService = primaryContactService;
        this.animalService = animalService;
        this.ownerService = ownerService;

        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        // 1. TABELLA PAZIENTI CON INDICAZIONE DEL REFERENTE PRIMARIO
        JPanel topBox = new JPanel(new BorderLayout(6, 6));
        topBox.setBorder(new TitledBorder(
                "US-03: Elenco Pazienti e Referente Primario per le Comunicazioni Ordinarie"));

        animalModel = new DefaultTableModel(
                new Object[]{"ID", "Codice Cartella", "Paziente", "Specie/Razza", "Microchip", "N° Proprietari", "Referente Primario Designato (US-03)"},
                0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        animalTable = new JTable(animalModel);
        animalTable.setRowHeight(26);
        animalTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        animalTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && animalTable.getSelectedRow() >= 0) {
                int animalId = (Integer) animalModel.getValueAt(animalTable.getSelectedRow(), 0);
                animalService.findById(animalId).ifPresent(a -> {
                    selectedAnimal = a;
                    refreshSelectedAnimalDetails();
                    if (onPatientSelectedListener != null) {
                        onPatientSelectedListener.accept(a);
                    }
                });
            }
        });
        topBox.add(new JScrollPane(animalTable), BorderLayout.CENTER);

        // 2. PANNELLO ASSOCIAZIONE COMPROPRIETÀ E SCELTA REFERENTE PRIMARIO
        JPanel bottomBox = new JPanel(new BorderLayout(8, 8));
        bottomBox.setBorder(new TitledBorder(
                "Aggiunta Comproprietario / Affido e Selezione Referente Principale di Contatto"));

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        cmbOwners = new JComboBox<>();
        cmbRole = new JComboBox<>(new String[]{"Comproprietario Familiare", "Affidatario Temporaneo", "Proprietario Legale"});
        chkPrimary = new JCheckBox("Designa come Referente Primario", true);

        JButton btnSave = new JButton("Salva Comproprietà / Imposta Referente");
        btnSave.setBackground(new Color(25, 55, 109));
        btnSave.setForeground(Color.WHITE);
        btnSave.setOpaque(true);

        btnSave.addActionListener(e -> {
            if (selectedAnimal == null) {
                JOptionPane.showMessageDialog(this, "Seleziona prima un paziente nella tabella.",
                        "Attenzione", JOptionPane.WARNING_MESSAGE);
                return;
            }
            Owner selOwner = (Owner) cmbOwners.getSelectedItem();
            if (selOwner == null) {
                return;
            }
            try {
                primaryContactService.addCoOwner(
                        selectedAnimal.getId(),
                        selOwner.getId(),
                        (String) cmbRole.getSelectedItem(),
                        chkPrimary.isSelected()
                );
                refreshData();
                JOptionPane.showMessageDialog(this,
                        "Aggiornati i referenti di " + selectedAnimal.getName() + "!",
                        "US-03 Completata", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Errore US-03", JOptionPane.ERROR_MESSAGE);
            }
        });

        controls.add(new JLabel("Proprietario:"));
        controls.add(cmbOwners);
        controls.add(new JLabel("Ruolo:"));
        controls.add(cmbRole);
        controls.add(chkPrimary);
        controls.add(btnSave);

        detailsArea = new JTextArea(7, 70);
        detailsArea.setEditable(false);
        detailsArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

        bottomBox.add(controls, BorderLayout.NORTH);
        bottomBox.add(new JScrollPane(detailsArea), BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topBox, bottomBox);
        split.setResizeWeight(0.55);
        add(split, BorderLayout.CENTER);
    }

    public void refreshData() {
        Integer prevId = selectedAnimal != null ? selectedAnimal.getId() : null;
        cmbOwners.removeAllItems();
        for (Owner o : ownerService.getAllOwners()) {
            cmbOwners.addItem(o);
        }

        animalModel.setRowCount(0);
        int rowToSelect = 0;
        List<Animal> animals = animalService.getAllAnimals();
        for (int i = 0; i < animals.size(); i++) {
            Animal a = animals.get(i);
            if (prevId != null && prevId.equals(a.getId())) {
                rowToSelect = i;
            }
            List<OwnershipDetail> links = primaryContactService.getOwnershipDetails(a.getId());
            Optional<Owner> primary = primaryContactService.getPrimaryContact(a.getId());
            String primaryLabel = primary
                    .map(o -> "★ " + o.getFullName() + " (" + o.getPhone() + " - " + o.getPreferredChannel() + ")")
                    .orElse("(Non assegnato)");
            animalModel.addRow(new Object[]{
                    a.getId(), a.getMedicalRecordCode(), a.getName(),
                    a.getSpecies() + (a.getBreed() != null && !a.getBreed().isEmpty() ? " / " + a.getBreed() : ""),
                    a.getMicrochip(), links.size(), primaryLabel
            });
        }
        if (animalTable.getRowCount() > 0) {
            animalTable.setRowSelectionInterval(rowToSelect, rowToSelect);
        } else {
            refreshSelectedAnimalDetails();
        }
    }

    private void refreshSelectedAnimalDetails() {
        if (selectedAnimal == null) {
            detailsArea.setText("Seleziona un paziente per visualizzare tutti i comproprietari e il referente primario.");
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("=== DETTAGLIO COMPROPRIETÀ E REFERENTE PRIMARIO PER: ")
          .append(selectedAnimal.getName().toUpperCase())
          .append(" [").append(selectedAnimal.getMedicalRecordCode()).append("] ===\n\n");
        List<OwnershipDetail> details = primaryContactService.getOwnershipDetails(selectedAnimal.getId());
        if (details.isEmpty()) {
            sb.append("Nessun proprietario associato.\n");
        } else {
            for (OwnershipDetail d : details) {
                sb.append(" • ").append(d.toString()).append("\n");
            }
        }
        detailsArea.setText(sb.toString());
    }

    public void setOnPatientSelectedListener(Consumer<Animal> listener) {
        this.onPatientSelectedListener = listener;
    }
}
