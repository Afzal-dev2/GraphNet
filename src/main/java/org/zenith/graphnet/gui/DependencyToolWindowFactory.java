package org.zenith.graphnet.gui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

public class DependencyToolWindowFactory implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        try {
            DependencyToolWindowContent content = new DependencyToolWindowContent(project);
            Content uiContent = ContentFactory.getInstance().createContent(
                    content.getContentPanel(),
                    "",
                    false
            );
            toolWindow.getContentManager().addContent(uiContent);

            // Set tool window properties
            toolWindow.setTitle("GraphNet Analysis");
            toolWindow.setStripeTitle("GraphNet");

        } catch (Exception e) {
            System.err.println("Error creating GraphNet tool window: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public boolean shouldBeAvailable(@NotNull Project project) {
        // Only show for projects that have Java files
        return project.getBaseDir() != null;
    }
}