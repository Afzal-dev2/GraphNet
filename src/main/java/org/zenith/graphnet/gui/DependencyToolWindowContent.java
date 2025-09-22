package org.zenith.graphnet.gui;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import org.zenith.graphnet.service.DependencyAnalysisService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;

import com.intellij.ui.table.JBTable;
import org.zenith.graphnet.util.DiagramAutomationWithExportUtil;

public class DependencyToolWindowContent {
    private final JPanel contentPanel;
    private final Project project;
    private final DependencyAnalysisService service;
    private final JTextArea outputArea;
    private final JTable actionTable;
    private final DefaultTableModel tableModel;

    public DependencyToolWindowContent(Project project) {
        this.project = project;
        this.service = DependencyAnalysisService.getInstance(project);
        this.contentPanel = new JPanel(new BorderLayout());
        this.outputArea = new JTextArea();

        // Initialize table for discovered actions
        this.tableModel = new DefaultTableModel(new String[]{"Action ID", "Description", "Available"}, 0);
        this.actionTable = new JBTable(tableModel);

        initializeUI();
    }

    private void initializeUI() {
        // Create tabbed pane
        JBTabbedPane tabbedPane = new JBTabbedPane();

        // Tab 1: Dependency Analysis
        JPanel dependencyPanel = createDependencyPanel();
        tabbedPane.addTab("Dependency Analysis", dependencyPanel);

        // Tab 2: Action Discovery
        JPanel actionDiscoveryPanel = createActionDiscoveryPanel();
        tabbedPane.addTab("Action Discovery", actionDiscoveryPanel);

        contentPanel.add(tabbedPane, BorderLayout.CENTER);
    }

    private JPanel createDependencyPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // Title
        JLabel titleLabel = new JLabel("GraphNet Dependency Analysis");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));

        // Buttons panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton analyzeButton = new JButton("Create Dependency Graph");
        analyzeButton.addActionListener(e -> showDiagramForHardcodedPath());

        JButton gitDiffButton = new JButton("Send Git Diff");
        gitDiffButton.addActionListener(e -> sendGitDiff());

        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(e -> outputArea.setText(""));

        buttonPanel.add(analyzeButton);
        buttonPanel.add(gitDiffButton);
        buttonPanel.add(clearButton);

        // Output area
        outputArea.setEditable(false);
        outputArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        outputArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JBScrollPane scrollPane = new JBScrollPane(outputArea);
        scrollPane.setPreferredSize(new Dimension(400, 200));

        // Layout
        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(buttonPanel, BorderLayout.CENTER);
        panel.add(scrollPane, BorderLayout.SOUTH);

        // Initial status
        outputArea.setText("GraphNet Dependency Analysis Tool\n");
        outputArea.append("Project: " + project.getName() + "\n");

        service.triggerAnalysis();
        Map<String, Object> stats = service.getStatistics();

        for (Map.Entry<String, Object> entry : stats.entrySet()) {
            outputArea.append(entry.getKey() + ": " + entry.getValue() + "\n");
        }

        return panel;
    }

    private JPanel createActionDiscoveryPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // Title
        JLabel titleLabel = new JLabel("IntelliJ Action Discovery");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));

        // Buttons panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton discoverAllButton = new JButton("Discover All Actions");
        discoverAllButton.addActionListener(e -> discoverAllActions());

        JButton discoverDiagramButton = new JButton("Find Diagram Actions");
        discoverDiagramButton.addActionListener(e -> discoverDiagramActions());

        JButton discoverUMLButton = new JButton("Find UML Actions");
        discoverUMLButton.addActionListener(e -> discoverUMLActions());

        JButton clearTableButton = new JButton("Clear Table");
        clearTableButton.addActionListener(e -> tableModel.setRowCount(0));

        buttonPanel.add(discoverAllButton);
        buttonPanel.add(discoverDiagramButton);
        buttonPanel.add(discoverUMLButton);
        buttonPanel.add(clearTableButton);

        // Table for discovered actions
        actionTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        actionTable.getColumnModel().getColumn(0).setPreferredWidth(300);
        actionTable.getColumnModel().getColumn(1).setPreferredWidth(200);
        actionTable.getColumnModel().getColumn(2).setPreferredWidth(80);

        JBScrollPane tableScrollPane = new JBScrollPane(actionTable);
        tableScrollPane.setPreferredSize(new Dimension(600, 300));

        // Test action button
        JPanel testPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton testActionButton = new JButton("Test Selected Action");
        testActionButton.addActionListener(e -> testSelectedAction());
        testPanel.add(testActionButton);

        // 1. Create a new container panel for the bottom section.
        //    This panel will hold both the table and the test button.
        JPanel southContainer = new JPanel(new BorderLayout());

        // 2. Add the table to the CENTER of this new container.
        southContainer.add(tableScrollPane, BorderLayout.CENTER);

        // 3. Add the test button panel to the SOUTH of this new container.
        southContainer.add(testPanel, BorderLayout.SOUTH);

        // --- END OF THE FIX ---

        // Layout
        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(buttonPanel, BorderLayout.CENTER);

        // 4. Add the single container panel to the SOUTH of the main panel.
        panel.add(southContainer, BorderLayout.SOUTH);

        return panel;
    }

    private void discoverAllActions() {
        tableModel.setRowCount(0);

        try {
            ActionManager actionManager = ActionManager.getInstance();
            String[] actionIds = actionManager.getActionIds("");

            Arrays.sort(actionIds);

            for (String actionId : actionIds) {
                AnAction action = actionManager.getAction(actionId);
                String description = action != null ? getActionDescription(action) : "N/A";
                boolean available = action != null;

                tableModel.addRow(new Object[]{actionId, description, available});
            }

            Messages.showInfoMessage(project,
                    "Discovered " + actionIds.length + " total actions",
                    "Action Discovery");

        } catch (Exception e) {
            Messages.showErrorDialog(project,
                    "Error discovering actions: " + e.getMessage(),
                    "Error");
        }
    }

    private void discoverDiagramActions() {
        tableModel.setRowCount(0);

        try {
            ActionManager actionManager = ActionManager.getInstance();
            String[] actionIds = actionManager.getActionIds("");

            List<String> diagramActions = new ArrayList<>();

            for (String actionId : actionIds) {
                String lowerActionId = actionId.toLowerCase();
                if (lowerActionId.contains("diagram") ||
                        lowerActionId.contains("dependency") ||
                        lowerActionId.contains("dependencies") ||
                        lowerActionId.contains("graph") ||
                        lowerActionId.contains("visualize") ||
                        lowerActionId.contains("provider") ||
                        lowerActionId.contains("show") && (lowerActionId.contains("structure") || lowerActionId.contains("hierarchy"))) {
                    System.out.println("ActionId: " + actionId);

                    diagramActions.add(actionId);
                }
            }

            diagramActions.sort(String::compareToIgnoreCase);

            for (String actionId : diagramActions) {
                AnAction action = actionManager.getAction(actionId);
                String description = action != null ? getActionDescription(action) : "N/A";
                boolean available = action != null;

                tableModel.addRow(new Object[]{actionId, description, available});
            }

            Messages.showInfoMessage(project,
                    "Found " + diagramActions.size() + " diagram-related actions",
                    "Diagram Actions");

        } catch (Exception e) {
            Messages.showErrorDialog(project,
                    "Error discovering diagram actions: " + e.getMessage(),
                    "Error");
        }
    }

    private void discoverUMLActions() {
        tableModel.setRowCount(0);

        try {
            ActionManager actionManager = ActionManager.getInstance();
            String[] actionIds = actionManager.getActionIds("");

            List<String> umlActions = new ArrayList<>();

            for (String actionId : actionIds) {
                String lowerActionId = actionId.toLowerCase();
                if (lowerActionId.contains("uml") ||
                        lowerActionId.contains("class") && lowerActionId.contains("diagram") ||
                        lowerActionId.contains("sequence") ||
                        lowerActionId.contains("hierarchy")) {
                    System.out.println("ActionId: " + actionId);
                    umlActions.add(actionId);
                }
            }

            umlActions.sort(String::compareToIgnoreCase);

            for (String actionId : umlActions) {
                AnAction action = actionManager.getAction(actionId);
                String description = action != null ? getActionDescription(action) : "N/A";
                boolean available = action != null;

                tableModel.addRow(new Object[]{actionId, description, available});
            }

            Messages.showInfoMessage(project,
                    "Found " + umlActions.size() + " UML-related actions",
                    "UML Actions");

        } catch (Exception e) {
            Messages.showErrorDialog(project,
                    "Error discovering UML actions: " + e.getMessage(),
                    "Error");
        }
    }

    private String getActionDescription(AnAction action) {
        try {
            String text = action.getTemplatePresentation().getText();
            String description = action.getTemplatePresentation().getDescription();

            if (text != null && !text.isEmpty()) {
                return text;
            } else if (description != null && !description.isEmpty()) {
                return description;
            } else {
                return action.getClass().getSimpleName();
            }
        } catch (Exception e) {
            return "Unknown";
        }
    }

    private void testSelectedAction() {
        int selectedRow = actionTable.getSelectedRow();
        if (selectedRow == -1) {
            Messages.showWarningDialog(project, "Please select an action to test", "No Selection");
            return;
        }

        String actionId = (String) tableModel.getValueAt(selectedRow, 0);

        try {
            ActionManager actionManager = ActionManager.getInstance();
            AnAction action = actionManager.getAction(actionId);

            if (action == null) {
                Messages.showErrorDialog(project, "Action not found: " + actionId, "Error");
                return;
            }

            // For now, just show action info
            String info = "Action ID: " + actionId + "\n" +
                    "Class: " + action.getClass().getName() + "\n" +
                    "Text: " + action.getTemplatePresentation().getText() + "\n" +
                    "Description: " + action.getTemplatePresentation().getDescription();

            Messages.showInfoMessage(project, info, "Action Info");

            // Create a synthetic action event
            DataContext dataContext = DataManager.getInstance().getDataContext();
            AnActionEvent event = AnActionEvent.createFromDataContext(actionId, null, dataContext);

            // Execute the action
            action.actionPerformed(event);

            System.out.println("Successfully executed diagram action: " + actionId);

        } catch (Exception e) {
            Messages.showErrorDialog(project,
                    "Error testing action: " + e.getMessage(),
                    "Error");
        }
    }
    // In your DependencyToolWindowContent class, or another UI class

    private void showDiagramForHardcodedPath() {

//        String path = "C:/Users/w191728/WGSGoogleHckathon/TestingImpactAnalysis/DemoCode/angular-springboot-ecommerce/backend/src/main";

        Path currentWorkingDir = Paths.get("").toAbsolutePath();
        System.out.println("Current Working Directory: " + currentWorkingDir);

        // 2. Resolve the path to the desired subdirectory.
        // The resolve() method safely joins path components.
        Path mainDirPath = currentWorkingDir.resolve("src").resolve("main");

        System.out.println("Constructed path to 'main' folder: " + mainDirPath);

        // 3. IMPORTANT: Always validate that the path actually exists before using it.
        if (Files.exists(mainDirPath) && Files.isDirectory(mainDirPath)) {
            System.out.println("\nSUCCESS: The directory 'src/main' was found.");

//        DiagramAutomationUtil.showDiagramWithDependencies(project, path);
            DiagramAutomationWithExportUtil.AutomationConfig config = new DiagramAutomationWithExportUtil.AutomationConfig(true, true, new File("C:/Users/w191728/WGSGoogleHckathon/TestingImpactAnalysis/DemoCode/angular-springboot-ecommerce/backend/dependency.md"),  // Output file (null for file chooser)
                    () -> {
                        // On complete
                        Messages.showInfoMessage(project, "Diagram exported successfully!", "Success");
                    },
                    () -> {
                        // On error
                        Messages.showErrorDialog(project, "Failed to complete workflow", "Error");
                    }
            );
            DiagramAutomationWithExportUtil.automatedDiagramWorkflow(project, mainDirPath.toString(), config);
        }
        else {
            System.out.println("\nERROR: The directory 'src/main' does not exist.");
            Messages.showErrorDialog(project, "The directory 'src/main' does not exist in the current working directory:\n" + currentWorkingDir, "Directory Not Found");
        }
    }
    public void showDiagramForFolderPath(Project project, String folderPath) {
        // All UI and action-related operations must happen on the Event Dispatch Thread (EDT).
        ApplicationManager.getApplication().invokeLater(() -> {
            // All file and PSI access should be within a read action.
            ApplicationManager.getApplication().runReadAction(() -> {

                // --- Step 1: Convert the String Path to a PsiElement ---

                // Find the VirtualFile corresponding to the string path.
                VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByPath(folderPath);
                if (virtualFile == null || !virtualFile.isDirectory()) {
                    Messages.showErrorDialog(project, "Folder not found or is not a directory: " + folderPath, "Error");
                    return;
                }

                // Get the PsiDirectory from the VirtualFile. This is the PSI representation of a folder.
                PsiDirectory psiDirectory = PsiManager.getInstance(project).findDirectory(virtualFile);
                if (psiDirectory == null) {
                    Messages.showErrorDialog(project, "Could not find PSI directory for: " + folderPath, "Error");
                    return;
                }

                // --- Step 2: Create DataContext and Invoke the Action ---

                ActionManager actionManager = ActionManager.getInstance();
                AnAction diagramAction = actionManager.getAction("ShowUmlDiagram");

                if (diagramAction == null) {
                    Messages.showErrorDialog(project, "Action 'ShowUmlDiagram' not found. Is the UML plugin enabled?", "Action Not Found");
                    return;
                }

                // Create a DataContext that provides the necessary information to the action.
                // The action will query this context for the element to work on.
                DataContext dataContext = SimpleDataContext.builder()
                        .add(CommonDataKeys.PROJECT, project)
                        .add(CommonDataKeys.PSI_ELEMENT, psiDirectory) // The action primarily looks for a PSI_ELEMENT.
                        .add(CommonDataKeys.NAVIGATABLE, psiDirectory) // It's good practice to provide this as well.
                        .build();

                // Create an AnActionEvent to trigger the action.
                AnActionEvent actionEvent = AnActionEvent.createFromAnAction(diagramAction, null, ActionPlaces.UNKNOWN, dataContext);

                // Before performing the action, it's crucial to call update().
                // This method checks the DataContext and enables/disables the action's presentation.
                diagramAction.update(actionEvent);

                if (actionEvent.getPresentation().isEnabledAndVisible()) {
                    // If the action is enabled for our context, perform it.
                    diagramAction.actionPerformed(actionEvent);
                } else {
                    // This can happen if the context is not right (e.g., trying to diagram a non-source folder).
                    Messages.showWarningDialog(project, "The diagram action is not available for the selected package.", "Action Not Available");
                }
            });
        });
    }

    private void analyzeDependencies() {
        outputArea.append("Starting dependency analysis...\n");

        try {
            outputArea.append("Found " + service.getAllFiles().size() + " files\n");

            // Display some results

            outputArea.append("\nAnalysis finished.\n\n");

            outputArea.append("\nStats: \n");
            Map<String, Object> stats = service.getStatistics();

            for (Map.Entry<String, Object> entry : stats.entrySet()) {
                outputArea.append(entry.getKey() + ": " + entry.getValue() + "\n");
            }

        } catch (Exception e) {
            outputArea.append("Error during analysis: " + e.getMessage() + "\n");
        }

        // Auto-scroll to bottom
        outputArea.setCaretPosition(outputArea.getDocument().getLength());
    }

    private void sendGitDiff() {
        outputArea.append("Sending git diff to microservice...\n");

        try {
            service.sendGitDiff();
            outputArea.append("Git diff sent successfully!\n\n");

            Messages.showInfoMessage(project, "Git diff sent to microservice!", "GraphNet");

        } catch (Exception e) {
            outputArea.append("Error sending git diff: " + e.getMessage() + "\n");
        }

        // Auto-scroll to bottom
        outputArea.setCaretPosition(outputArea.getDocument().getLength());
    }

    public JPanel getContentPanel() {
        return contentPanel;
    }
}