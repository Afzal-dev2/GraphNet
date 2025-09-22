package org.zenith.graphnet.model;

public class GitDiffData {
    private String projectName;
    private String diff;
    private long timestamp;
    private DependencyGraph dependencyGraph;

    // Constructors
    public GitDiffData() {}

    // Getters and Setters
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public String getDiff() { return diff; }
    public void setDiff(String diff) { this.diff = diff; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public DependencyGraph getDependencyGraph() { return dependencyGraph; }
    public void setDependencyGraph(DependencyGraph dependencyGraph) { this.dependencyGraph = dependencyGraph; }
}