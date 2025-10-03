package org.zenith.graphnet.model;

public class ChangedFile {
    public String path;
    public String status;
    public int linesChanged;
    public int additions;
    public int deletions;
    public String diff;

    // Constructor, getters, setters
    public ChangedFile(String path, String status, int additions, int deletions, String diff) {
        this.path = path;
        this.status = status;
        this.additions = additions;
        this.deletions = deletions;
        this.linesChanged = additions + deletions;
        this.diff = diff;
    }
}
