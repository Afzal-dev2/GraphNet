package org.zenith.graphnet.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.zenith.graphnet.api.MicroserviceCommunicator;
import org.zenith.graphnet.model.DependencyGraph;
import org.zenith.graphnet.model.GitDiffData;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

public class HttpMicroserviceCommunicator implements MicroserviceCommunicator {

    private String baseUrl;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public HttpMicroserviceCommunicator() {
        this.baseUrl = "http://localhost:8080/dependency/push-dgraph";
        this.objectMapper = new ObjectMapper();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(200, TimeUnit.SECONDS)
                .writeTimeout(200, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public String getName() {
        return "HTTP Microservice Communicator";
    }

    @Override
    public void sendDependencyGraph() throws Exception {
        // read the current dependency graph from the specified source path
//        String sourcePath = System.getProperty("user.dir") + "/dependency-graph.json";
        String graphFile = "C:/Users/w191728/WGSGoogleHckathon/TestingImpactAnalysis/DemoCode/angular-springboot-ecommerce/backend/dependency-graph.md";
        Path graphFilePath = Paths.get(graphFile);
        String mimeTypeString = Files.probeContentType(graphFilePath);
        MediaType mediaType = MediaType.parse(mimeTypeString);

        // Convert Path to File for RequestBody.create
        File fileToUpload = graphFilePath.toFile();

        RequestBody body = RequestBody.create(
                fileToUpload,
               mediaType
        );

        // Build the MultipartBody
        MultipartBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM) // Specifies multipart/form-data
                // Add the file part
                // "graphFile" is the name of the form field on the server-side
                // fileToUpload.getName() is the original filename
                .addFormDataPart("graph_file", fileToUpload.getName(), body)
                .build();

        Request request = new Request.Builder()
                .url(baseUrl)
                .post(requestBody)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new Exception("Failed to send dependency graph. Status: " + response.code());
            }
        }
    }

    @Override
    public void sendGitDiff(GitDiffData diffData) throws Exception {
        // execute git diff command and capture output

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