package org.zenith.graphnet;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import org.zenith.graphnet.service.DependencyAnalysisService;
import org.zenith.graphnet.service.GraphNetSettingsService;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ProjectStartupActivity implements ProjectActivity {

    @Nullable
    @Override
    public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        try {
            System.out.println("GraphNet plugin starting for project: " + project.getName());

            // Initialize services
            DependencyAnalysisService analysisService = DependencyAnalysisService.getInstance(project);
            GraphNetSettingsService settingsService = GraphNetSettingsService.getInstance();

            // Initialize dependency analysis if auto-analysis is enabled
            if (settingsService.isAutoAnalysisEnabled()) {
                analysisService.initialize();
            }

            // Show tool window if it exists
            ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
            ToolWindow toolWindow = toolWindowManager.getToolWindow("GraphNet Analysis");
            if (toolWindow != null) {
                // Don't auto-show, let user open it manually
                System.out.println("GraphNet tool window registered successfully");
            }

            System.out.println("GraphNet plugin initialized successfully for project: " + project.getName());

        } catch (Exception e) {
            System.err.println("Error initializing GraphNet plugin: " + e.getMessage());
            e.printStackTrace();
        }

        return Unit.INSTANCE;
    }
}