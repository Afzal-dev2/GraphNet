package org.zenith.graphnet.model;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class DependencyGraph {
    private List<FileNode> nodes;
    private Map<String, Set<String>> edges;
    private String projectName;
    private long generatedAt;

    // Constructors
    public DependencyGraph() {}

    // Getters and Setters
    public List<FileNode> getNodes() { return nodes; }
    public void setNodes(List<FileNode> nodes) { this.nodes = nodes; }

    public Map<String, Set<String>> getEdges() { return edges; }
    public void setEdges(Map<String, Set<String>> edges) { this.edges = edges; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public long getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(long generatedAt) { this.generatedAt = generatedAt; }
}