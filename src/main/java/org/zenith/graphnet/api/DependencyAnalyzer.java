package org.zenith.graphnet.api;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.zenith.graphnet.model.FileNode;

import java.util.Set;

/**
 * Extension point interface for custom dependency analyzers
 */
public interface DependencyAnalyzer {

    /**
     * Get the name of this analyzer
     */
    String getName();

    /**
     * Check if this analyzer can handle the given file
     */
    boolean canAnalyze(VirtualFile file);

    /**
     * Analyze dependencies for a single file
     */
    FileNode analyzeFile(VirtualFile file, Project project);

    /**
     * Get the file extensions this analyzer supports
     */
    Set<String> getSupportedExtensions();

    /**
     * Get the priority of this analyzer (higher = more priority)
     */
    int getPriority();
}