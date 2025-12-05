package it.denzosoft.javadecompilermodule.options;

import it.denzosoft.javadecompilermodule.decompiler.DecompilerEngine;
import it.denzosoft.javadecompilermodule.decompiler.DecompilerRegistry;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.Enumeration;
import java.util.List;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;

/**
 * Options panel for selecting the decompiler engine.
 */
public class DecompilerOptionsPanel extends JPanel {

    private final ButtonGroup buttonGroup;
    private final JCheckBox preserveLineNumbersCheckbox;
    private String selectedEngineId;
    private boolean preserveLineNumbers;

    public DecompilerOptionsPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        // Title for decompiler selection
        JLabel titleLabel = new JLabel("Select Decompiler Engine:");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainPanel.add(titleLabel);
        mainPanel.add(Box.createVerticalStrut(15));

        // Radio buttons for each decompiler
        buttonGroup = new ButtonGroup();
        List<DecompilerEngine> engines = DecompilerRegistry.getAvailableEngines();

        for (DecompilerEngine engine : engines) {
            JPanel enginePanel = createEnginePanel(engine);
            enginePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            mainPanel.add(enginePanel);
            mainPanel.add(Box.createVerticalStrut(10));
        }

        // Separator
        mainPanel.add(Box.createVerticalStrut(10));
        JSeparator separator = new JSeparator();
        separator.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainPanel.add(separator);
        mainPanel.add(Box.createVerticalStrut(15));

        // Options section title
        JLabel optionsLabel = new JLabel("Decompilation Options:");
        optionsLabel.setFont(optionsLabel.getFont().deriveFont(Font.BOLD));
        optionsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainPanel.add(optionsLabel);
        mainPanel.add(Box.createVerticalStrut(10));

        // Preserve line numbers checkbox
        preserveLineNumbersCheckbox = new JCheckBox("Preserve original line numbers");
        preserveLineNumbersCheckbox.setAlignmentX(Component.LEFT_ALIGNMENT);
        preserveLineNumbersCheckbox.addActionListener(e -> preserveLineNumbers = preserveLineNumbersCheckbox.isSelected());
        mainPanel.add(preserveLineNumbersCheckbox);

        JLabel lineNumbersDesc = new JLabel("    Align decompiled code to original source line numbers (requires debug info)");
        lineNumbersDesc.setFont(lineNumbersDesc.getFont().deriveFont(Font.ITALIC));
        lineNumbersDesc.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainPanel.add(lineNumbersDesc);

        add(mainPanel, BorderLayout.NORTH);

        // Load current selection
        load();
    }

    private JPanel createEnginePanel(DecompilerEngine engine) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JRadioButton radioButton = new JRadioButton(engine.getDisplayName());
        radioButton.setActionCommand(engine.getId());
        radioButton.addActionListener(e -> selectedEngineId = e.getActionCommand());

        JLabel descLabel = new JLabel("    " + engine.getDescription());
        descLabel.setFont(descLabel.getFont().deriveFont(Font.ITALIC));

        buttonGroup.add(radioButton);

        JPanel radioPanel = new JPanel();
        radioPanel.setLayout(new BoxLayout(radioPanel, BoxLayout.X_AXIS));
        radioPanel.add(radioButton);
        radioPanel.add(Box.createHorizontalGlue());
        radioPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(radioPanel);
        panel.add(descLabel);

        return panel;
    }

    /**
     * Loads the current settings.
     */
    public void load() {
        selectedEngineId = DecompilerRegistry.getSelectedEngineId();
        preserveLineNumbers = DecompilerRegistry.isPreserveLineNumbers();

        // Select the correct radio button
        Enumeration<AbstractButton> elements = buttonGroup.getElements();
        while (elements.hasMoreElements()) {
            AbstractButton rb = elements.nextElement();
            if (rb.getActionCommand().equals(selectedEngineId)) {
                rb.setSelected(true);
                break;
            }
        }

        // Set checkbox state
        preserveLineNumbersCheckbox.setSelected(preserveLineNumbers);
    }

    /**
     * Stores the current settings.
     */
    public void store() {
        if (selectedEngineId != null) {
            DecompilerRegistry.setSelectedEngine(selectedEngineId);
        }
        DecompilerRegistry.setPreserveLineNumbers(preserveLineNumbers);
    }

    /**
     * Returns true if settings have been changed.
     */
    public boolean isChanged() {
        return !DecompilerRegistry.getSelectedEngineId().equals(selectedEngineId)
                || DecompilerRegistry.isPreserveLineNumbers() != preserveLineNumbers;
    }
}
