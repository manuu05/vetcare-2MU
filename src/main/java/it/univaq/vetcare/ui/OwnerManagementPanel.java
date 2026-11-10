package it.univaq.vetcare.ui;

import it.univaq.vetcare.model.Animal;
import it.univaq.vetcare.model.Owner;
import it.univaq.vetcare.model.PreferredChannel;
import it.univaq.vetcare.service.AnimalService;
import it.univaq.vetcare.service.OwnerService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * US-02 (Sub-Task US02-1): Maschera GUI di gestione proprietario con recapiti
 * telefonici, canale di contatto preferito (Telefono/SMS/Email) e pannello N:M.
 * Assegnazione Sprint 1: Dev 1 (Solo) + Review Dev 3.
 */
public class OwnerManagementPanel extends JPanel {

    private final OwnerService ownerService;
    private final AnimalService animalService;

    private final DefaultTableModel ownerTableModel;
    private final JTable ownerTable;
    private final JComboBox<Animal> comboAnimalsForLink;
    private final JComboBox<Owner> comboOwnersForLink;
    private final JTextArea relationDetailsArea;

    public OwnerManagementPanel(OwnerService ownerService, AnimalService animalService) {
        this.ownerService = ownerService;
        this.animalService = animalService;

        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel formOwner = new JPanel(new GridLayout(0, 4, 8, 8));
        formOwner.setBorder(new TitledBorder("Registrazione Anagrafica Proprietario (US-02)"));

        JTextField txtFirst = new JTextField();
        JTextField txtLast = new JTextField();
        JTextField txtCf = new JTextField("RSSMRA80A01H501U");
        JTextField txtPhone = new JTextField("+39 3331234567");
        JTextField txtEmail = new JTextField("mario.rossi@email.it");
        JComboBox<PreferredChannel> cmbChannel = new JComboBox<>(PreferredChannel.values());

        formOwner.add(new JLabel("Nome *:"));
        formOwner.add(txtFirst);
        formOwner.add(new JLabel("Cognome *:"));
        formOwner.add(txtLast);

        formOwner.add(new JLabel("Codice Fiscale (16 car.) *:"));
        formOwner.add(txtCf);
        formOwner.add(new JLabel("Telefono Valido *:"));
        formOwner.add(txtPhone);

        formOwner.add(new JLabel("Email:"));
        formOwner.add(txtEmail);
        formOwner.add(new JLabel("Canale Contatto Preferito *:"));
        formOwner.add(cmbChannel);

        JButton btnSaveOwner = new JButton("Registra Proprietario");
        btnSaveOwner.setBackground(new Color(46, 125, 50));
        btnSaveOwner.setForeground(Color.WHITE);
        formOwner.add(new JLabel(""));
        formOwner.add(btnSaveOwner);

        ownerTableModel = new DefaultTableModel(
                new Object[]{"ID", "Nome", "Cognome", "Codice Fiscale", "Telefono", "Email", "Canale Preferito"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        ownerTable = new JTable(ownerTableModel);
        ownerTable.setRowHeight(24);

        btnSaveOwner.addActionListener(e -> {
            try {
                Owner owner = new Owner(
                        txtFirst.getText().trim(),
                        txtLast.getText().trim(),
                        txtCf.getText().trim(),
                        txtPhone.getText().trim(),
                        txtEmail.getText().trim(),
                        (PreferredChannel) cmbChannel.getSelectedItem()
                );
                Owner saved = ownerService.registerOwner(owner);
                refreshOwnersAndRelations();
                JOptionPane.showMessageDialog(this,
                        "Proprietario " + saved.getFullName() + " salvato con successo (Canale: " + saved.getPreferredChannel() + ")!",
                        "US-02 Completata", JOptionPane.INFORMATION_MESSAGE);
                txtFirst.setText("");
                txtLast.setText("");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Errore Validazione (US-02)", JOptionPane.ERROR_MESSAGE);
            }
        });

        JPanel linkPanel = new JPanel(new BorderLayout(8, 8));
        linkPanel.setBorder(new TitledBorder("Associazione N:M Animale <-> Proprietari (TK-02: Comproprietà e Navigabilità Bidirezionale)"));

        JPanel linkControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        comboAnimalsForLink = new JComboBox<>();
        comboOwnersForLink = new JComboBox<>();
        JButton btnLink = new JButton("Associa Proprietario all'Animale");
        btnLink.addActionListener(e -> {
            Animal selAnimal = (Animal) comboAnimalsForLink.getSelectedItem();
            Owner selOwner = (Owner) comboOwnersForLink.getSelectedItem();
            if (selAnimal != null && selOwner != null) {
                ownerService.associateAnimalToOwner(selAnimal.getId(), selOwner.getId(), "Comproprietario");
                refreshRelationSummary();
                JOptionPane.showMessageDialog(this,
                        "Associato " + selOwner.getFullName() + " al paziente " + selAnimal.getName() + "!",
                        "Relazione N:M Salvata", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        linkControls.add(new JLabel("Animale:"));
        linkControls.add(comboAnimalsForLink);
        linkControls.add(new JLabel("Proprietario:"));
        linkControls.add(comboOwnersForLink);
        linkControls.add(btnLink);

        relationDetailsArea = new JTextArea(6, 60);
        relationDetailsArea.setEditable(false);
        relationDetailsArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

        linkPanel.add(linkControls, BorderLayout.NORTH);
        linkPanel.add(new JScrollPane(relationDetailsArea), BorderLayout.CENTER);

        JPanel splitCenter = new JPanel(new GridLayout(2, 1, 8, 8));
        splitCenter.add(new JScrollPane(ownerTable));
        splitCenter.add(linkPanel);

        add(formOwner, BorderLayout.NORTH);
        add(splitCenter, BorderLayout.CENTER);
    }

    public void refreshOwnersAndRelations() {
        ownerTableModel.setRowCount(0);
        comboOwnersForLink.removeAllItems();
        comboAnimalsForLink.removeAllItems();

        for (Animal a : animalService.getAllAnimals()) {
            comboAnimalsForLink.addItem(a);
        }
        for (Owner o : ownerService.getAllOwners()) {
            ownerTableModel.addRow(new Object[]{
                    o.getId(), o.getFirstName(), o.getLastName(), o.getFiscalCode(),
                    o.getPhone(), o.getEmail(), o.getPreferredChannel()
            });
            comboOwnersForLink.addItem(o);
        }
        refreshRelationSummary();
    }

    public void refreshRelationSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("--- STATO RELAZIONI N:M (Animale <-> Proprietari) ---\n");
        for (Animal a : animalService.getAllAnimals()) {
            List<Owner> linkedOwners = ownerService.getOwnersOfAnimal(a.getId());
            String ownerNames = linkedOwners.isEmpty()
                    ? "(Nessun proprietario associato)"
                    : linkedOwners.stream().map(Owner::getFullName).collect(Collectors.joining(", "));
            sb.append(String.format("• Animale: %-12s [%s]  --> Proprietari (%d): %s%n",
                    a.getName(), a.getMedicalRecordCode(), linkedOwners.size(), ownerNames));
        }
        relationDetailsArea.setText(sb.toString());
    }
}
