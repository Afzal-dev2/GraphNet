package org.zenith.graphnet.api;

import org.zenith.graphnet.model.DependencyGraph;
import org.zenith.graphnet.model.GitDiffData;

/**
 * Extension point interface for microservice communication
 */
public interface MicroserviceCommunicator {

    /**
     * Get the name of this communicator
     */
    String getName();

    /**
     * Send dependency graph to microservice
     */
    void sendDependencyGraph(DependencyGraph graph) throws Exception;

    /**
     * Send git diff data to microservice
     */
    void sendGitDiff(GitDiffData diffData) throws Exception;

    /**
     * Test connection to microservice
     */
    boolean testConnection();

    /**
     * Get the base URL for this communicator
     */
    String getBaseUrl();

    /**
     * Set the base URL for this communicator
     */
    void setBaseUrl(String baseUrl);
}