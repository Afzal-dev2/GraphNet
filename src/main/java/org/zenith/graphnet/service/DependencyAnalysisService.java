package org.zenith.graphnet.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import okhttp3.*;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.zenith.graphnet.model.DependencyGraph;
import org.zenith.graphnet.model.FileNode;
import org.zenith.graphnet.model.GitDiffData;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service(Service.Level.PROJECT)
public final class DependencyAnalysisService {

    private final Project project;
    private final Map<String, FileNode> fileNodes;
    private final Map<String, Set<String>> dependencyGraph;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private boolean initialized = false;

    // Microservice configuration
    private static final String MICROSERVICE_URL = "http://localhost:8080/api/dependency-analysis";
    private static final String GIT_DIFF_ENDPOINT = "/git-diff";
    private static final String ANALYZE_ENDPOINT = "/analyze";

    // Regex patterns for dependency detection
    private static final Pattern IMPORT_PATTERN = Pattern.compile("^\\s*import\\s+([^;]+);.*$", Pattern.MULTILINE);
    private static final Pattern PACKAGE_PATTERN = Pattern.compile("^\\s*package\\s+([^;]+);.*$", Pattern.MULTILINE);
    private static final Pattern CLASS_PATTERN = Pattern.compile("\\b(class|interface|enum)\\s+([A-Za-z_][A-Za-z0-9_]*)");

    public DependencyAnalysisService(@NotNull Project project) {
        this.project = project;
        this.fileNodes = new HashMap<>();
        this.dependencyGraph = new HashMap<>();
        this.objectMapper = new ObjectMapper();

        // Configure HTTP client
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    public static DependencyAnalysisService getInstance(@NotNull Project project) {
        return project.getService(DependencyAnalysisService.class);
    }

    public void initialize() {
        if (initialized) return;

        System.out.println("Initializing dependency analysis for: " + project.getName());
        analyzeDependencies();
        initialized = true;
    }

    public void analyzeDependencies() {
        // Clear existing data
        fileNodes.clear();
        dependencyGraph.clear();

        System.out.println("Starting comprehensive dependency analysis...");

        try {
            // Get project base directory
            VirtualFile baseDir = project.getBaseDir();
            if (baseDir == null) {
                System.err.println("Project base directory not found");
                return;
            }

            // Find all Java files
            Collection<VirtualFile> javaFiles = findJavaFiles();
            System.out.println("Found " + javaFiles.size() + " Java files");

            // Parse each Java file
            for (VirtualFile file : javaFiles) {
                parseJavaFile(file);
            }

            // Build dependency relationships
            buildDependencyRelationships();

            System.out.println("Dependency analysis completed. Found " + fileNodes.size() + " files with dependencies");

        } catch (Exception e) {
            System.err.println("Error during dependency analysis: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Collection<VirtualFile> findJavaFiles() {
        FileType javaFileType = FileTypeManager.getInstance().getFileTypeByExtension("java");
        return ReadAction.compute(() -> {
            return FileTypeIndex.getFiles(javaFileType, GlobalSearchScope.projectScope(project));
        });
    }

    private void parseJavaFile(VirtualFile file) {
        try {
            String content = new String(file.contentsToByteArray(), StandardCharsets.UTF_8);
            String filePath = file.getPath();

            // Create file node
            FileNode fileNode = new FileNode();
            fileNode.setFilePath(filePath);
            fileNode.setFileName(file.getName());
            fileNode.setPackageName(extractPackageName(content));
            fileNode.setClassName(extractClassName(content));
            fileNode.setImports(extractImports(content));
            fileNode.setDependencies(new HashSet<>());
            fileNode.setLineCount(content.split("\n").length);

            // Store file node
            fileNodes.put(filePath, fileNode);

            // Initialize dependency set
            dependencyGraph.put(filePath, new HashSet<>());

        } catch (IOException e) {
            System.err.println("Error reading file: " + file.getPath() + " - " + e.getMessage());
        }
    }

    private String extractPackageName(String content) {
        Matcher matcher = PACKAGE_PATTERN.matcher(content);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }

    private String extractClassName(String content) {
        Matcher matcher = CLASS_PATTERN.matcher(content);
        if (matcher.find()) {
            return matcher.group(2).trim();
        }
        return "";
    }

    private Set<String> extractImports(String content) {
        Set<String> imports = new HashSet<>();
        Matcher matcher = IMPORT_PATTERN.matcher(content);

        while (matcher.find()) {
            String importStatement = matcher.group(1).trim();
            if (!importStatement.startsWith("java.lang")) { // Exclude java.lang imports
                imports.add(importStatement);
            }
        }

        return imports;
    }

    private void buildDependencyRelationships() {
        // Build mapping of class names to file paths
        Map<String, String> classToFileMap = new HashMap<>();
        Map<String, String> packageToFileMap = new HashMap<>();

        for (FileNode node : fileNodes.values()) {
            if (node.getClassName() != null && !node.getClassName().isEmpty()) {
                classToFileMap.put(node.getClassName(), node.getFilePath());
            }
            if (node.getPackageName() != null && !node.getPackageName().isEmpty()) {
                packageToFileMap.put(node.getPackageName(), node.getFilePath());
            }
        }

        // Analyze dependencies for each file
        for (FileNode sourceNode : fileNodes.values()) {
            Set<String> dependencies = new HashSet<>();

            // Check imports
            for (String importStatement : sourceNode.getImports()) {
                // Find matching files for this import
                String dependentFile = findDependentFile(importStatement, classToFileMap, packageToFileMap);
                if (dependentFile != null && !dependentFile.equals(sourceNode.getFilePath())) {
                    dependencies.add(dependentFile);
                }
            }

            // Update dependencies
            sourceNode.setDependencies(dependencies);
            dependencyGraph.put(sourceNode.getFilePath(), dependencies);
        }
    }

    private String findDependentFile(String importStatement, Map<String, String> classToFileMap, Map<String, String> packageToFileMap) {
        // Try exact class match
        if (importStatement.contains(".")) {
            String className = importStatement.substring(importStatement.lastIndexOf(".") + 1);
            if (classToFileMap.containsKey(className)) {
                return classToFileMap.get(className);
            }
        }

        // Try package match
        if (packageToFileMap.containsKey(importStatement)) {
            return packageToFileMap.get(importStatement);
        }

        // Try partial package match
        for (String packageName : packageToFileMap.keySet()) {
            if (importStatement.startsWith(packageName)) {
                return packageToFileMap.get(packageName);
            }
        }

        return null;
    }

    public void sendGitDiff() {
        try {
            System.out.println("Generating git diff...");

            // Get git diff
            String gitDiff = getGitDiff();
            if (gitDiff == null || gitDiff.trim().isEmpty()) {
                System.out.println("No git changes found");
                return;
            }

            // Create git diff data
            GitDiffData diffData = new GitDiffData();
            diffData.setProjectName(project.getName());
            diffData.setDiff(gitDiff);
            diffData.setTimestamp(System.currentTimeMillis());
            diffData.setDependencyGraph(getCurrentDependencyGraph());

            // Send to microservice
            sendDiffToMicroservice(diffData);

        } catch (Exception e) {
            System.err.println("Error sending git diff: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String getGitDiff() {
        try {
            Process process = new ProcessBuilder("git", "diff", "--name-only")
                    .directory(project.getBaseDir().toNioPath().toFile())
                    .start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder diff = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                diff.append(line).append("\n");
            }

            process.waitFor();
            return diff.toString();

        } catch (Exception e) {
            System.err.println("Error getting git diff: " + e.getMessage());
            return null;
        }
    }

    private DependencyGraph getCurrentDependencyGraph() {
        DependencyGraph graph = new DependencyGraph();
        graph.setNodes(new ArrayList<>(fileNodes.values()));
        graph.setEdges(dependencyGraph);
        graph.setProjectName(project.getName());
        graph.setGeneratedAt(System.currentTimeMillis());
        return graph;
    }

    private void sendDiffToMicroservice(GitDiffData diffData) {
        try {
            String json = objectMapper.writeValueAsString(diffData);

            RequestBody body = RequestBody.create(
                    json,
                    MediaType.get("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(MICROSERVICE_URL + GIT_DIFF_ENDPOINT)
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build();

            Response response = httpClient.newCall(request).execute();

            if (response.isSuccessful()) {
                System.out.println("Git diff sent successfully to microservice");
                System.out.println("Response: " + response.body().string());
            } else {
                System.err.println("Failed to send git diff. Status: " + response.code());
                System.err.println("Response: " + response.body().string());
            }

        } catch (Exception e) {
            System.err.println("Error sending data to microservice: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void sendDependencyAnalysis() {
        try {
            DependencyGraph graph = getCurrentDependencyGraph();
            String json = objectMapper.writeValueAsString(graph);

            RequestBody body = RequestBody.create(
                    json,
                    MediaType.get("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(MICROSERVICE_URL + ANALYZE_ENDPOINT)
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build();

            Response response = httpClient.newCall(request).execute();

            if (response.isSuccessful()) {
                System.out.println("Dependency analysis sent successfully to microservice");
                System.out.println("Response: " + response.body().string());
            } else {
                System.err.println("Failed to send analysis. Status: " + response.code());
                System.err.println("Response: " + response.body().string());
            }

        } catch (Exception e) {
            System.err.println("Error sending analysis to microservice: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Getter methods for UI
    public Map<String, Set<String>> getDependencyGraph() {
        return new HashMap<>(dependencyGraph);
    }

    public Set<String> getDependenciesFor(String filePath) {
        return dependencyGraph.getOrDefault(filePath, Collections.emptySet());
    }

    public List<String> getAllFiles() {
        return new ArrayList<>(dependencyGraph.keySet());
    }

    public List<FileNode> getAllFileNodes() {
        return new ArrayList<>(fileNodes.values());
    }

    public FileNode getFileNode(String filePath) {
        return fileNodes.get(filePath);
    }

    public void triggerAnalysis() {
        analyzeDependencies();

    }

    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalFiles", fileNodes.size());
        stats.put("totalDependencies", dependencyGraph.values().stream().mapToInt(Set::size).sum());
        stats.put("averageDependenciesPerFile",
                dependencyGraph.isEmpty() ? 0 :
                        dependencyGraph.values().stream().mapToInt(Set::size).average().orElse(0));

        // Find files with most dependencies
        String maxDepsFile = dependencyGraph.entrySet().stream()
                .max(Map.Entry.<String, Set<String>>comparingByValue((a, b) -> Integer.compare(a.size(), b.size())))
                .map(Map.Entry::getKey)
                .orElse("None");

        stats.put("fileWithMostDependencies", maxDepsFile);
        stats.put("maxDependencies", dependencyGraph.getOrDefault(maxDepsFile, Collections.emptySet()).size());

        return stats;
    }

    public Set<String> getFilesAffectedByChange(String changedFile) {
        Set<String> affected = new HashSet<>();

        // Find all files that depend on the changed file
        for (Map.Entry<String, Set<String>> entry : dependencyGraph.entrySet()) {
            if (entry.getValue().contains(changedFile)) {
                affected.add(entry.getKey());
            }
        }

        return affected;
    }
}