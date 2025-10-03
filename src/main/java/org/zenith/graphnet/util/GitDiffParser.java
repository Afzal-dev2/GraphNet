package org.zenith.graphnet.util;

import org.zenith.graphnet.model.ChangedFile;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GitDiffParser {

    public static List<ChangedFile> parseGitDiff(String gitDiffOutput) {
        List<ChangedFile> changedFiles = new ArrayList<>();

        // Split by file headers (diff --git)
        String[] fileBlocks = gitDiffOutput.split("(?=diff --git)");

        for (String fileBlock : fileBlocks) {
            if (fileBlock.trim().isEmpty()) continue;

            ChangedFile file = parseFileBlock(fileBlock);
            if (file != null) {
                changedFiles.add(file);
            }
        }

        return changedFiles;
    }

    private static ChangedFile parseFileBlock(String fileBlock) {
        // Extract file path
        Pattern pathPattern = Pattern.compile("diff --git a/(.*?) b/.*");
        Matcher pathMatcher = pathPattern.matcher(fileBlock);
        String filePath = "";
        if (pathMatcher.find()) {
            filePath = pathMatcher.group(1);
        }

        // Determine status
        String status = determineStatus(fileBlock);

        // Count additions and deletions
        int additions = countLines(fileBlock, "^\\+(?!\\+\\+)");
        int deletions = countLines(fileBlock, "^-(?!---)");

        // Extract meaningful diff (just the actual changes)
        String meaningfulDiff = extractMeaningfulDiff(fileBlock);

        return new ChangedFile(filePath, status, additions, deletions, fileBlock);
    }

    private static String determineStatus(String fileBlock) {
        if (fileBlock.contains("new file mode")) return "added";
        if (fileBlock.contains("deleted file mode")) return "deleted";
        if (fileBlock.contains("rename from")) return "renamed";
        return "modified";
    }

    private static int countLines(String content, String pattern) {
        Pattern p = Pattern.compile(pattern, Pattern.MULTILINE);
        Matcher m = p.matcher(content);
        int count = 0;
        while (m.find()) {
            count++;
        }
        return count;
    }

    private static String extractMeaningfulDiff(String fileBlock) {
        StringBuilder diff = new StringBuilder();
        String[] lines = fileBlock.split("\n");
        boolean inDiffSection = false;

        for (String line : lines) {
            if (line.startsWith("@@")) {
                diff.append(line).append("\n");
                inDiffSection = true;
            } else if (inDiffSection && (line.startsWith("+") || line.startsWith("-") || line.startsWith(" "))) {
                diff.append(line).append("\n");
            }
        }

        return diff.toString().trim();
    }

//    // Usage example
//    public static void main(String[] args) {
//        String gitDiffOutput = """
//            diff --git a/backend/src/main/java/com/angularspringbootecommerce/backend/controllers/AuthenticationController.java b/backend/src/main/java/com/angularspringbootecommerce/backend/controllers/AuthenticationController.java
//            index 3113c71..9b29691 100644
//            --- a/backend/src/main/java/com/angularspringbootecommerce/backend/controllers/AuthenticationController.java
//            +++ b/backend/src/main/java/com/angularspringbootecommerce/backend/controllers/AuthenticationController.java
//            @@ -31,7 +31,7 @@ public class AuthenticationController {
//                     UserLoginDto userLoginDto = authenticationService.login(user.getEmail(), user.getPassword());
//
//                     if (userLoginDto.getUser() == null) {
//            -            throw new AppException("Invalid credentials Given!.", HttpStatus.NOT_FOUND);
//            +            throw new AppException("Invalid credentials!.", HttpStatus.NOT_FOUND);
//                     }
//
//                     return userLoginDto;
//            diff --git a/backend/src/main/java/com/angularspringbootecommerce/backend/services/OrderService.java b/backend/src/main/java/com/angularspringbootecommerce/backend/services/OrderService.java
//            index a374a1c..76c6622 100644
//            --- a/backend/src/main/java/com/angularspringbootecommerce/backend/services/OrderService.java
//            +++ b/backend/src/main/java/com/angularspringbootecommerce/backend/services/OrderService.java
//            @@ -24,7 +24,7 @@ public class OrderService {
//
//                 public List<OrderDto> getOrdersByUserId(Long userId, Authentication authentication) {
//                     User user = userRepository.findById(userId)
//            -                .orElseThrow(() -> new AppException("User not found.", HttpStatus.NOT_FOUND));
//            +                .orElseThrow(() -> new AppException("User not found. Please use valid ID", HttpStatus.NOT_FOUND));
//
//                     if (authentication == null || !user.getEmail().equals(authentication.getName())) {
//                         throw new AppException("Access denied.", HttpStatus.BAD_REQUEST);
//            """;
//
//        List<ChangedFile> files = parseGitDiff(gitDiffOutput);
//
//        // Print results in JSON-like format
//        System.out.println("\"changed_files\": [");
//        for (int i = 0; i < files.size(); i++) {
//            ChangedFile file = files.get(i);
//            System.out.printf("""
//                {
//                  "path": "%s",
//                  "status": "%s",
//                  "lines_changed": %d,
//                  "additions": %d,
//                  "deletions": %d,
//                  "diff": "%s"
//                }%s
//                """,
//                    file.path,
//                    file.status,
//                    file.linesChanged,
//                    file.additions,
//                    file.deletions,
//                    file.diff.replace("\n", "\\n").replace("\"", "\\\""),
//                    i < files.size() - 1 ? "," : ""
//            );
//        }
//        System.out.println("]");
    }
