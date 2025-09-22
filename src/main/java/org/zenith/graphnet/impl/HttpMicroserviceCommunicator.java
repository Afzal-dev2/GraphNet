package org.zenith.graphnet.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.zenith.graphnet.api.MicroserviceCommunicator;
import org.zenith.graphnet.model.DependencyGraph;
import org.zenith.graphnet.model.GitDiffData;

import java.util.concurrent.TimeUnit;

public class HttpMicroserviceCommunicator implements MicroserviceCommunicator {

    private String baseUrl;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public HttpMicroserviceCommunicator() {
        this.baseUrl = "http://localhost:8080/api/dependency-analysis";
        this.objectMapper = new ObjectMapper();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public String getName() {
        return "HTTP Microservice Communicator";
    }

    @Override
    public void sendDependencyGraph(DependencyGraph graph) throws Exception {
        String json = objectMapper.writeValueAsString(graph);

        RequestBody body = RequestBody.create(
                json,
                MediaType.get("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(baseUrl + "/analyze")
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new Exception("Failed to send dependency graph. Status: " + response.code());
            }
        }
    }

    @Override
    public void sendGitDiff(GitDiffData diffData) throws Exception {
        String json = objectMapper.writeValueAsString(diffData);

        RequestBody body = RequestBody.create(
                json,
                MediaType.get("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(baseUrl + "/git-diff")
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new Exception("Failed to send git diff. Status: " + response.code());
            }
        }
    }

    @Override
    public boolean testConnection() {
        try {
            Request request = new Request.Builder()
                    .url(baseUrl + "/health")
                    .get()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                return response.isSuccessful();
            }
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getBaseUrl() {
        return baseUrl;
    }

    @Override
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}