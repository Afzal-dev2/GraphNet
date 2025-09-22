package org.zenith.graphnet.util;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.impl.ActionButton;
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;


public final class DiagramAutomationWithExportUtil {
    private static final Logger LOG = Logger.getInstance(DiagramAutomationWithExportUtil.class);
    private static final int MAX_WAIT_TIME_MS = 30000;
    private static final int CHECK_INTERVAL_MS = 100;
    private static final int RENDER_STABILIZATION_DELAY_MS = 1000; // Wait for rendering to stabilize

    private DiagramAutomationWithExportUtil() {}

    /**
     * Configuration for the automation workflow
     */
    public static class AutomationConfig {
        public final boolean toggleDependencies;
        public final boolean exportToMermaid;
        public final File mermaidOutputFile;
        public final Runnable onComplete;
        public final Runnable onError;

        public AutomationConfig(boolean toggleDependencies,
                                boolean exportToMermaid,
                                File mermaidOutputFile,
                                Runnable onComplete,
                                Runnable onError) {
            this.toggleDependencies = toggleDependencies;
            this.exportToMermaid = exportToMermaid;
            this.mermaidOutputFile = mermaidOutputFile;
            this.onComplete = onComplete;
            this.onError = onError;
        }
    }

    /**
     * Main entry point with full automation
     */
    public static void automatedDiagramWorkflow(@NotNull Project project,
                                                @NotNull String folderPath,
                                                @NotNull AutomationConfig config) {
        ApplicationManager.getApplication().invokeLater(() -> {
            VirtualFile folder = LocalFileSystem.getInstance().findFileByPath(folderPath);
            if (folder == null || !folder.isDirectory()) {
                Messages.showErrorDialog(project, "Invalid folder path: " + folderPath, "Error");
                if (config.onError != null) config.onError.run();
                return;
            }

            PsiDirectory psiDirectory = PsiManager.getInstance(project).findDirectory(folder);
            if (psiDirectory == null) {
                Messages.showErrorDialog(project, "Cannot find PSI directory for: " + folderPath, "Error");
                if (config.onError != null) config.onError.run();
                return;
            }

            // Set up the workflow listener
            DiagramWorkflowListener listener = new DiagramWorkflowListener(project, config);
            listener.startListening(() -> {
                openDiagram(project, psiDirectory);
            });
        });
    }

    /**
     * Enhanced listener that handles the complete workflow
     */
    private static class DiagramWorkflowListener {
        private final Project project;
        private final AutomationConfig config;
        private final AtomicBoolean diagramFullyLoaded = new AtomicBoolean(false);
        private final AtomicBoolean workflowCompleted = new AtomicBoolean(false);
        private final AtomicReference<FileEditor> diagramEditor = new AtomicReference<>();
        private MessageBusConnection connection;

        DiagramWorkflowListener(Project project, AutomationConfig config) {
            this.project = project;
            this.config = config;
        }
        private static  boolean isDependenciesButton(String accessibleName, String text, Icon icon) {
            // Check tooltip
            if (accessibleName != null) {
                String lowerTooltip = accessibleName.toLowerCase();
                if (lowerTooltip.contains("show dependencies") ||
                        lowerTooltip.contains("toggle dependencies")) {
                    return true;
                }
            }

            // Check text
            if (text != null) {
                String lowerText = text.toLowerCase();
                if (lowerText.contains("dependencies")) {
                    return true;
                }
            }

            // Check icon description if available
            if (icon != null) {
                String iconString = icon.toString();
                if (iconString.contains("dependencies")) {
                    return true;
                }
            }

            return false;
        }
        void startListening(Runnable openDiagramAction) {
            connection = project.getMessageBus().connect();
            connection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerListener() {
                @Override
                public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
                    if (isDiagramFile(file)) {
                        System.out.println("Diagram file detected: " + file.getName());
                        FileEditor editor = source.getSelectedEditor(file);
                        if (editor != null) {
                            diagramEditor.set(editor);
                            waitForDiagramInitialization(editor);
                        }
                    }
                }

                @Override
                public void selectionChanged(@NotNull FileEditorManagerEvent event) {
                    FileEditor newEditor = event.getNewEditor();
                    if (newEditor != null && isDiagramEditor(newEditor)) {
                        System.out.println("Diagram editor selected");
                        diagramEditor.set(newEditor);
                        waitForDiagramInitialization(newEditor);
                    }
                }
            });

            openDiagramAction.run();
        }

        private void waitForDiagramInitialization(FileEditor editor) {
            JComponent editorComponent = editor.getComponent();

            HierarchyListener hierarchyListener = new HierarchyListener() {
                @Override
                public void hierarchyChanged(HierarchyEvent e) {
                    if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 &&
                            editorComponent.isShowing()) {
                        editorComponent.removeHierarchyListener(this);
                        waitForFullDiagramRender(editorComponent);
                    }
                }
            };

            editorComponent.addHierarchyListener(hierarchyListener);

            if (editorComponent.isShowing()) {
                System.out.println("Editor component is showing");
                editorComponent.removeHierarchyListener(hierarchyListener);
                waitForFullDiagramRender(editorComponent);
            }
        }

        private void waitForFullDiagramRender(JComponent editorComponent) {
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                long startTime = System.currentTimeMillis();
                boolean previousLoadState = false;
                int stableCount = 0;
                System.out.println("Waiting for diagram to fully load...");
                while (System.currentTimeMillis() - startTime < MAX_WAIT_TIME_MS) {
                    boolean currentLoadState = isDiagramFullyLoaded(editorComponent);
                    System.out.println("Diagram load state: " + currentLoadState);
                    // Check if the state has been stable
                    if (currentLoadState && currentLoadState == previousLoadState) {
                        System.out.println("Diagram load state stable for " + (stableCount) + " checks");
                        stableCount++;

                        // If stable for multiple checks, consider it fully loaded
                        if (stableCount >= 3) {
                            System.out.println("Diagram rendering is stable and complete");
                            diagramFullyLoaded.set(true);

                            // Wait a bit more for any final rendering
                            try {
                                Thread.sleep(RENDER_STABILIZATION_DELAY_MS);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                return;
                            }

                            // Execute workflow on EDT
                            ApplicationManager.getApplication().invokeLater(this::executeWorkflow);

                            return;
                        }
                    } else {
                        stableCount = 0;
                    }

                    previousLoadState = currentLoadState;

                    try {
                        Thread.sleep(CHECK_INTERVAL_MS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }


                cleanup();
                if (config.onError != null) {
                    ApplicationManager.getApplication().invokeLater(config.onError);
                }
            });
        }

        private void executeWorkflow() {
            if (workflowCompleted.getAndSet(true)) {
                return; // Prevent duplicate execution
            }

            try {
                System.out.println("Executing diagram automation workflow");
                FileEditor editor = diagramEditor.get();
                if (editor == null) {
                    LOG.error("Diagram editor is null");
                    if (config.onError != null) config.onError.run();
                    return;
                }

                JComponent editorComponent = editor.getComponent();

                // Step 1: Toggle dependencies if requested
                if (config.toggleDependencies) {
                    System.out.println("Toggling dependencies");
                    boolean toggled = toggleDependenciesInDiagram(editorComponent);
                    if (toggled) {
                        // Wait for the diagram to re-render after toggling
                        System.out.println("Waiting for diagram to re-render after dependency toggle");
                        waitForReRender(() -> {
                            proceedToExport(editor);
                        });
                        return;
                    } else {
                        LOG.warn("Failed to toggle dependencies");
                    }
                }

                // If no toggle needed or toggle failed, proceed directly to export
                proceedToExport(editor);

            } finally {
                cleanup();
            }
        }

        private void proceedToExport(FileEditor editor) {
            // Step 2: Export to Mermaid if requested
            if (config.exportToMermaid) {
                System.out.println("Exporting diagram to Mermaid");
                exportDiagramToMermaid(editor);
            }

            // Step 3: Call completion callback
            if (config.onComplete != null) {
                config.onComplete.run();
            }
        }

        private void waitForReRender(Runnable onComplete) {
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                try {
                    // Wait for re-rendering to complete
                    Thread.sleep(2000); // Give it time to re-render

                    ApplicationManager.getApplication().invokeLater(onComplete);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        private void exportDiagramToMermaid(FileEditor editor) {
            DataContext dataContext = createDiagramExportContext(project, editor, config.mermaidOutputFile);

            ActionManager actionManager = ActionManager.getInstance();
            String actionId = "Diagram.ExportToFileGroup.Mermaid";
            AnAction exportAction = actionManager.getAction(actionId);

            if (exportAction == null) {
                // Try alternative IDs
                String[] alternativeIds = {
                        "ExportToMermaid",
                        "Diagram.ExportToMermaid",
                        "Diagrams.ExportToMermaid"
                };

                for (String altId : alternativeIds) {
                    exportAction = actionManager.getAction(altId);
                    if (exportAction != null) break;
                }
            }

            if (exportAction == null) {
                LOG.error("Could not find Mermaid export action");
                return;
            }

            AnActionEvent event = new AnActionEvent(
                    null,
                    dataContext,
                    ActionPlaces.TOOLBAR,
                    new Presentation(),
                    actionManager,
                    0
            );

            exportAction.update(event);

            if (event.getPresentation().isEnabledAndVisible()) {
                System.out.println("Executing Mermaid export");
                exportAction.actionPerformed(event);
            } else {
                LOG.error("Mermaid export action is not enabled");
            }
        }

        private boolean toggleDependenciesInDiagram(JComponent diagramComponent) {
            List<ActionToolbarImpl> toolbars = UIUtil.findComponentsOfType(diagramComponent, ActionToolbarImpl.class);

            for (ActionToolbar toolbar : toolbars) {
                for (Component component : toolbar.getComponent().getComponents()) {
                    if (component instanceof ActionButton button) {

                        String tooltip = button.getToolTipText();
                        Icon icon = button.getIcon();
                        String accessibleName = button.getAccessibleContext().getAccessibleName();
                        System.out.println("Button properties - Tooltip: " + tooltip + ", Accessible Name: " + accessibleName + ", Icon: " + (icon != null ? icon.toString() : "null"));
                        // Check if this is the dependencies button
                        if (isDependenciesButton(accessibleName, accessibleName, icon)) {
                            System.out.println("Found dependencies button");
                            // toggle the button
                            button.click();
                            return true;
                        }
                    }
                }
            }

            return false;
        }

        private void cleanup() {
            if (connection != null) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    connection.disconnect();
                });
            }
        }

        private boolean isDiagramFile(VirtualFile file) {
            String name = file.getName();
            return name.contains("src") || name.contains("main") ||
                    file.getPath().contains("java");
        }

        private boolean isDiagramEditor(FileEditor editor) {
            String className = editor.getClass().getName();
            return className.contains("BorderLayoutPanel");
        }

        private boolean isDiagramFullyLoaded(JComponent editorComponent) {
            AtomicBoolean hasToolbar = new AtomicBoolean(false);

            SwingUtilities.invokeLater(() -> {
                // Check for toolbar with buttons
                List<ActionToolbarImpl> toolbars = UIUtil.findComponentsOfType(editorComponent, ActionToolbarImpl.class);
                for (ActionToolbar toolbar : toolbars) {
                    if (toolbar.getComponent().getComponents().length > 0) {
                        hasToolbar.set(true);
                        break;
                    }
                }
            });

            try {
                SwingUtilities.invokeAndWait(() -> {});
            } catch (Exception e) {
                // Ignore
            }

            return hasToolbar.get();
        }
    }

    /**
     * Create data context for export
     */
    private static DataContext createDiagramExportContext(@NotNull Project project,
                                                          @NotNull FileEditor diagramEditor,
                                                          @Nullable File outputFile) {
        Component diagramComponent = diagramEditor.getComponent();
        VirtualFile virtualFile = diagramEditor.getFile();

        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put(CommonDataKeys.PROJECT.getName(), project);
        dataMap.put(CommonDataKeys.VIRTUAL_FILE.getName(), virtualFile);
        dataMap.put(PlatformDataKeys.FILE_EDITOR.getName(), diagramEditor);
        dataMap.put("diagram.editor", diagramEditor);
        dataMap.put("diagram.component", diagramComponent);

        if (outputFile != null) {
            dataMap.put("export.target.file", outputFile);
        }

        return new DataContext() {
            @Override
            @Nullable
            public Object getData(@NotNull String dataId) {
                Object data = dataMap.get(dataId);
                if (data != null) return data;

                DataContext componentContext = DataManager.getInstance().getDataContext(diagramComponent);
                return componentContext.getData(dataId);
            }
        };
    }

    /**
     * Opens the UML diagram
     */
    private static void openDiagram(@NotNull Project project, @NotNull PsiDirectory psiDirectory) {
        DataContext dataContext = new DataContext() {
            @Override
            public Object getData(@NotNull String dataId) {
                if (CommonDataKeys.PROJECT.is(dataId)) return project;
                if (CommonDataKeys.PSI_ELEMENT.is(dataId)) return psiDirectory;
                if (LangDataKeys.PSI_ELEMENT_ARRAY.is(dataId)) return new PsiElement[]{psiDirectory};
                if (PlatformDataKeys.VIRTUAL_FILE.is(dataId)) return psiDirectory.getVirtualFile();
                return null;
            }
        };

        ActionManager actionManager = ActionManager.getInstance();
        List<String> actionIds = Arrays.asList(
                "ShowUmlDiagram",
                "Diagrams.ShowDiagram",
                "ShowDiagram"
        );

        AnAction diagramAction = null;
        for (String actionId : actionIds) {
            diagramAction = actionManager.getAction(actionId);
            if (diagramAction != null) break;
        }

        if (diagramAction == null) {
            Messages.showErrorDialog(project, "Cannot find Show Diagram action", "Error");
            return;
        }

        AnActionEvent event = new AnActionEvent(
                null,
                dataContext,
                ActionPlaces.PROJECT_VIEW_POPUP,
                new Presentation(),
                actionManager,
                0
        );

        diagramAction.update(event);
        if (event.getPresentation().isEnabledAndVisible()) {
            diagramAction.actionPerformed(event);
        }
    }

}