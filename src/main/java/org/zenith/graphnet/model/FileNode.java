package org.zenith.graphnet.model;

import java.util.Set;

public class FileNode {
    private String filePath;
    private String fileName;
    private String packageName;
    private String className;
    private Set<String> imports;
    private Set<String> dependencies;
    private int lineCount;

    // Constructors
    public FileNode() {}

    // Getters and Setters
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getPackageName() { return packageName; }
    public void setPackageName(String packageName) { this.packageName = packageName; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public Set<String> getImports() { return imports; }
    public void setImports(Set<String> imports) { this.imports = imports; }

    public Set<String> getDependencies() { return dependencies; }
    public void setDependencies(Set<String> dependencies) { this.dependencies = dependencies; }

    public int getLineCount() { return lineCount; }
    public void setLineCount(int lineCount) { this.lineCount = lineCount; }
}