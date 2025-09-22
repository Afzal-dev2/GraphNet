package org.zenith.graphnet.service;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Service(Service.Level.APP)
@State(name = "GraphNetSettings", storages = @Storage("graphnet-settings.xml"))
public final class GraphNetSettingsService implements PersistentStateComponent<GraphNetSettingsService.State> {

    public static class State {
        public String microserviceUrl = "http://localhost:8080/api/dependency-analysis";
        public boolean enableAutoAnalysis = true;
        public boolean enableGitIntegration = true;
        public int maxDependencyDepth = 5;
        public boolean showOnlyProjectFiles = true;
    }

    private State state = new State();

    public static GraphNetSettingsService getInstance() {
        return ApplicationManager.getApplication().getService(GraphNetSettingsService.class);
    }

    @Override
    public @Nullable State getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull State state) {
        this.state = state;
    }

    // Convenience methods
    public String getMicroserviceUrl() {
        return state.microserviceUrl;
    }

    public void setMicroserviceUrl(String url) {
        state.microserviceUrl = url;
    }

    public boolean isAutoAnalysisEnabled() {
        return state.enableAutoAnalysis;
    }

    public void setAutoAnalysisEnabled(boolean enabled) {
        state.enableAutoAnalysis = enabled;
    }

    public boolean isGitIntegrationEnabled() {
        return state.enableGitIntegration;
    }

    public void setGitIntegrationEnabled(boolean enabled) {
        state.enableGitIntegration = enabled;
    }

    public int getMaxDependencyDepth() {
        return state.maxDependencyDepth;
    }

    public void setMaxDependencyDepth(int depth) {
        state.maxDependencyDepth = depth;
    }

    public boolean isShowOnlyProjectFiles() {
        return state.showOnlyProjectFiles;
    }

    public void setShowOnlyProjectFiles(boolean showOnly) {
        state.showOnlyProjectFiles = showOnly;
    }
}