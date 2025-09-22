package org.zenith.graphnet.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.util.NlsContexts;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;
import org.zenith.graphnet.service.GraphNetSettingsService;

import javax.swing.*;
import java.awt.*;

public class GraphNetConfigurable implements Configurable {

    private JTextField microserviceUrlField;
    private JCheckBox autoAnalysisCheckBox;
    private JCheckBox gitIntegrationCheckBox;
    private JSpinner maxDepthSpinner;
    private JCheckBox showOnlyProjectFilesCheckBox;
    private JPanel mainPanel;

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "GraphNet";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        mainPanel = new JPanel(new BorderLayout());

        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        // Microservice URL
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST;
        formPanel.add(new JLabel("Microservice URL:"), gbc);

        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        microserviceUrlField = new JTextField(30);
        formPanel.add(microserviceUrlField, gbc);

        // Auto analysis
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.NONE;
        autoAnalysisCheckBox = new JCheckBox("Enable automatic dependency analysis");
        formPanel.add(autoAnalysisCheckBox, gbc);

        // Git integration
        gbc.gridy = 2;
        gitIntegrationCheckBox = new JCheckBox("Enable Git integration");
        formPanel.add(gitIntegrationCheckBox, gbc);

        // Max dependency depth
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 1; gbc.anchor = GridBagConstraints.WEST;
        formPanel.add(new JLabel("Max dependency depth:"), gbc);

        gbc.gridx = 1;
        maxDepthSpinner = new JSpinner(new SpinnerNumberModel(5, 1, 20, 1));
        formPanel.add(maxDepthSpinner, gbc);

        // Show only project files
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        showOnlyProjectFilesCheckBox = new JCheckBox("Show only project files in analysis");
        formPanel.add(showOnlyProjectFilesCheckBox, gbc);

        mainPanel.add(formPanel, BorderLayout.NORTH);

        return mainPanel;
    }

    @Override
    public boolean isModified() {
        GraphNetSettingsService settings = GraphNetSettingsService.getInstance();

        return !microserviceUrlField.getText().equals(settings.getMicroserviceUrl()) ||
                autoAnalysisCheckBox.isSelected() != settings.isAutoAnalysisEnabled() ||
                gitIntegrationCheckBox.isSelected() != settings.isGitIntegrationEnabled() ||
                !maxDepthSpinner.getValue().equals(settings.getMaxDependencyDepth()) ||
                showOnlyProjectFilesCheckBox.isSelected() != settings.isShowOnlyProjectFiles();
    }

    @Override
    public void apply() throws ConfigurationException {
        GraphNetSettingsService settings = GraphNetSettingsService.getInstance();

        settings.setMicroserviceUrl(microserviceUrlField.getText());
        settings.setAutoAnalysisEnabled(autoAnalysisCheckBox.isSelected());
        settings.setGitIntegrationEnabled(gitIntegrationCheckBox.isSelected());
        settings.setMaxDependencyDepth((Integer) maxDepthSpinner.getValue());
        settings.setShowOnlyProjectFiles(showOnlyProjectFilesCheckBox.isSelected());
    }

    @Override
    public void reset() {
        GraphNetSettingsService settings = GraphNetSettingsService.getInstance();

        microserviceUrlField.setText(settings.getMicroserviceUrl());
        autoAnalysisCheckBox.setSelected(settings.isAutoAnalysisEnabled());
        gitIntegrationCheckBox.setSelected(settings.isGitIntegrationEnabled());
        maxDepthSpinner.setValue(settings.getMaxDependencyDepth());
        showOnlyProjectFilesCheckBox.setSelected(settings.isShowOnlyProjectFiles());
    }
}