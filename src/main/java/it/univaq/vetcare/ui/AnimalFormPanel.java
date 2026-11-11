package it.univaq.vetcare.ui;

import it.univaq.vetcare.model.Animal;
import it.univaq.vetcare.service.AnimalService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * US-01 (Sub-Task US01-1): Maschera grafica GUI per l'inserimento dei dati
 * anagrafici dell'animale (specie, razza, nome, nascita, sesso, microchip 15 cifre, peso, note)
 * e tabella di consultazione pazienti.
 * Assegnazione Sprint 1: Dev 3 (Solo) + Review Dev 2.
 */
public class AnimalFormPanel extends JPanel {

    private final AnimalService animalService;
    private final DefaultTableModel animalTableModel;
    private final JTable animalTable;

    private Consumer<Animal> onAnimalSelectedListener;
    private BiConsumer<Animal, String> onAnimalSavedListener;

    public AnimalFormPanel(AnimalService animalService) {
        this.animalService = animalService;
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel formPanel = new JPanel(new GridLayout(0, 4, 8, 8));
        formPanel.setBorder(new TitledBorder("Registrazione Nuova Scheda Paziente Animale (US-01)"));

        JTextField txtName = new JTextField();
        JComboBox<String> cmbSpecies = new JComboBox<>(new String[]{"Cane", "Gatto", "Coniglio", "Furetto", "Altro"});
        JTextField txtBreed = new JTextField();
        JTextField txtBirth = new JTextField(LocalDate.now().minusYears(2).toString());
        JComboBox<String> cmbSex = new JComboBox<>(new String[]{"M", "F"});
        JTextField txtMicrochip = new JTextField("380260000000009");
        JTextField txtWeight = new JTextField("12.5");
        JTextField txtAllergy = new JTextField();
        JTextField txtNotes = new JTextField();

        formPanel.add(new JLabel("Nome Paziente *:"));
        formPanel.add(txtName);
        formPanel.add(new JLabel("Specie *:"));
        formPanel.add(cmbSpecies);

        formPanel.add(new JLabel("Razza:"));
        formPanel.add(txtBreed);
        formPanel.add(new JLabel("Data Nascita (YYYY-MM-DD) *:"));
        formPanel.add(txtBirth);

        formPanel.add(new JLabel("Sesso *:"));
        formPanel.add(cmbSex);
        formPanel.add(new JLabel("Microchip ISO (15 cifre) *:"));
        formPanel.add(txtMicrochip);

        formPanel.add(new JLabel("Peso (kg) *:"));
        formPanel.add(txtWeight);
        formPanel.add(new JLabel("Allergia Nota (opz. es. Penicilline):"));
        formPanel.add(txtAllergy);

        formPanel.add(new JLabel("Note Cliniche:"));
        formPanel.add(txtNotes);

        JButton btnSaveAnimal = new JButton("Salva Scheda Animale");
        btnSaveAnimal.setBackground(new Color(25, 118, 210));
        btnSaveAnimal.setForeground(Color.WHITE);
        btnSaveAnimal.setFont(new Font("SansSerif", Font.BOLD, 12));
        formPanel.add(new JLabel(""));
        formPanel.add(btnSaveAnimal);

        animalTableModel = new DefaultTableModel(
                new Object[]{"ID", "Codice Cartella", "Nome", "Specie", "Razza", "Nascita", "Sesso", "Microchip (15 cifre)", "Peso (kg)"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        animalTable = new JTable(animalTableModel);
        animalTable.setRowHeight(26);
        animalTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        btnSaveAnimal.addActionListener(e -> {
            try {
                double weight = Double.parseDouble(txtWeight.getText().trim());
                LocalDate birthDate = LocalDate.parse(txtBirth.getText().trim());
                Animal animal = new Animal(
                        txtName.getText().trim(),
                        (String) cmbSpecies.getSelectedItem(),
                        txtBreed.getText().trim(),
                        birthDate,
                        (String) cmbSex.getSelectedItem(),
                        txtMicrochip.getText().trim(),
                        weight,
                        txtNotes.getText().trim()
                );
                Animal saved = animalService.registerAnimal(animal);
                if (onAnimalSavedListener != null) {
                    onAnimalSavedListener.accept(saved, txtAllergy.getText().trim());
                }
                refreshAnimalTable();
                JOptionPane.showMessageDialog(this,
                        "Paziente registrato con successo!\nID Cartella Univoco: " + saved.getMedicalRecordCode(),
                        "Registrazione Completata (US-01)", JOptionPane.INFORMATION_MESSAGE);
                txtName.setText("");
                txtBreed.setText("");
                txtAllergy.setText("");
                txtNotes.setText("");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Errore Validazione (US-01)", JOptionPane.ERROR_MESSAGE);
            }
        });

        animalTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && animalTable.getSelectedRow() >= 0) {
                int row = animalTable.getSelectedRow();
                int animalId = (Integer) animalTableModel.getValueAt(row, 0);
                animalService.findById(animalId).ifPresent(animal -> {
                    if (onAnimalSelectedListener != null) {
                        onAnimalSelectedListener.accept(animal);
                    }
                });
            }
        });

        JPanel centerPanel = new JPanel(new BorderLayout(4, 4));
        centerPanel.setBorder(new TitledBorder("Elenco Pazienti Censiti (Seleziona un paziente per visualizzare la Cartella e gli Alert)"));
        centerPanel.add(new JScrollPane(animalTable), BorderLayout.CENTER);

        add(formPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
    }

    public void refreshAnimalTable() {
        animalTableModel.setRowCount(0);
        List<Animal> animals = animalService.getAllAnimals();
        for (Animal a : animals) {
            animalTableModel.addRow(new Object[]{
                    a.getId(), a.getMedicalRecordCode(), a.getName(), a.getSpecies(),
                    a.getBreed(), a.getBirthDate(), a.getSex(), a.getMicrochip(), a.getWeight()
            });
        }
        if (animalTable.getRowCount() > 0) {
            animalTable.setRowSelectionInterval(0, 0);
        }
    }

    public void setOnAnimalSelectedListener(Consumer<Animal> listener) {
        this.onAnimalSelectedListener = listener;
    }

    public void setOnAnimalSavedListener(BiConsumer<Animal, String> listener) {
        this.onAnimalSavedListener = listener;
    }
}
