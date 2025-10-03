package org.zenith.graphnet.model;

import java.util.List;

public class GitDiffData {
    private String mrId;
    private String author;
    private String repository;
    private String sourceBranch;
    private String targetBranch;
    private List<ChangedFile> changedFiles;

    public List<ChangedFile> getChangedFiles() {
        return changedFiles;
    }

    public void setChangedFiles(List<ChangedFile> changedFiles) {
        this.changedFiles = changedFiles;
    }

    public String getMrId() {
        return mrId;
    }

    public void setMrId(String mrId) {
        this.mrId = mrId;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getSourceBranch() {
        return sourceBranch;
    }

    public void setSourceBranch(String sourceBranch) {
        this.sourceBranch = sourceBranch;
    }

    public String getTargetBranch() {
        return targetBranch;
    }

    public void setTargetBranch(String targetBranch) {
        this.targetBranch = targetBranch;
    }


    // Constructors
    public GitDiffData() {}

    // Getters and Setters
    public String getRepository() { return repository; }
    public void setRepository(String repository) { this.repository = repository; }




}