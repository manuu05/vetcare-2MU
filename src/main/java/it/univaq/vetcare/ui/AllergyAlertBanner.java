package it.univaq.vetcare.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * US-06 (Sub-Task US06-1): Componente visivo permanente 'Banner Rosso Allergia Critica'
 * posizionato in primo piano prima di ogni tab operativo della cartella clinica.
 */
public class AllergyAlertBanner extends JPanel {

    public static final Color CRITICAL_RED_BG = new Color(211, 47, 47);
    public static final Color SAFE_GREEN_BG = new Color(46, 125, 50);

    private final JLabel alertLabel;
    private boolean criticalAlertActive;

    public AllergyAlertBanner() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 16, 10, 16));
        setBackground(SAFE_GREEN_BG);

        alertLabel = new JLabel("Nessuna allergia critica rilevata per il paziente selezionato", SwingConstants.CENTER);
        alertLabel.setForeground(Color.WHITE);
        alertLabel.setFont(new Font("SansSerif", Font.BOLD, 15));
        add(alertLabel, BorderLayout.CENTER);
        this.criticalAlertActive = false;
    }

    /**
     * Aggiorna lo stato del banner in base al testo restituito da AllergyAlertService.
     */
    public void updateAlertBanner(String bannerMessage, String animalName) {
        if (bannerMessage != null && !bannerMessage.trim().isEmpty()) {
            setBackground(CRITICAL_RED_BG);
            alertLabel.setText("⚠ " + bannerMessage + " (Paziente: " + animalName + ")");
            this.criticalAlertActive = true;
        } else {
            setBackground(SAFE_GREEN_BG);
            String labelText = (animalName != null && !animalName.isEmpty())
                    ? "✔ Paziente " + animalName + ": Nessuna allergia farmacologica nota"
                    : "Seleziona un paziente animale per visualizzare eventuali alert clinici";
            alertLabel.setText(labelText);
            this.criticalAlertActive = false;
        }
        revalidate();
        repaint();
    }

    public boolean isCriticalAlertActive() {
        return criticalAlertActive;
    }

    public String getDisplayedText() {
        return alertLabel.getText();
    }
}
