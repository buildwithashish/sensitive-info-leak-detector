package com.scan.sensitiveinfo;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class SensitiveInfoScanner {

    public static void scanFile(Path filePath, List<String> sensitivePatterns) throws IOException {
        Files.lines(filePath).forEach(line -> {
            for (String pattern : sensitivePatterns) {
                Pattern compiledPattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
                if (compiledPattern.matcher(line).find()) {
                    ReportGenerator.addToReport(pattern, filePath, line);
                }
            }
        });
    }

    public static void scanFile(Path filePath, String outputFile) throws IOException {

        List<String> sensitivePatterns = ConfigLoader.getSensitivePatternsConfig();
        List<Pattern> logPatterns = ConfigLoader.getLogPatterns();
        List<Pattern> ignoreVariables = ConfigLoader.getIgnoreVariables();

        // Check the file extension
        String fileName = filePath.getFileName().toString();
        boolean isJavaFile = fileName.endsWith(".java");

        // Analyze the Java source for getter methods returning sensitive fields (for Java files)
        Map<String, String> methodToFieldMap = new HashMap<>();
        if (isJavaFile) {
            CompilationUnit cu = parseJavaFile(filePath);
            methodToFieldMap = collectSensitiveGetters(cu, sensitivePatterns);
        }

        // Open file and stream lines
        try (Stream<String> lines = Files.lines(filePath)) {

            // Track line numbers for reporting
            final int[] lineNumber = {0};

            Map<String, String> finalMethodToFieldMap = methodToFieldMap;
            lines.forEach(line -> {
                lineNumber[0]++; // Increment the line number

                if (line != null) {
                    String trimmedLine = line.trim();

                    // For Java files, we perform additional checks
                    if (isJavaFile) {
                        // For Java files, check for sensitive attributes being printed/logged

                        // Step 1: Check if sensitive attributes are being printed/logged
                        boolean isSensitiveAttributeLogged = logPatterns.stream()
                                .anyMatch(pattern -> pattern.matcher(trimmedLine).find()) &&
                                sensitivePatterns.stream().anyMatch(attr -> trimmedLine.contains(attr));

                        // Step 2: Check if the log statement contains a method call that returns a sensitive field
                        boolean sensitiveMethodLogged = finalMethodToFieldMap.entrySet().stream()
                                .anyMatch(entry -> trimmedLine.contains(entry.getKey()) && logPatterns.stream()
                                        .anyMatch(pattern -> pattern.matcher(trimmedLine).find()));

                        // Step 3: Ignore if the sensitive word is part of a variable or method name
                        boolean isVariableOrMethod = ignoreVariables.stream()
                                .anyMatch(pattern -> pattern.matcher(trimmedLine).find());

                        // Step 4: Log the finding if sensitive information is printed
                        if ((isSensitiveAttributeLogged || sensitiveMethodLogged) && !isVariableOrMethod) {
                            String message = "Sensitive data logged at line " + lineNumber[0] + " in file " + filePath.toString();
                            ReportGenerator.addToReport(message, filePath, "" + lineNumber[0]);
                        }
                    } else {
                        // For non-Java files, we use the existing logic

                        // Step 1: Check for sensitive keywords directly in the code
                        boolean containsSensitiveInfo = sensitivePatterns.stream()
                                .anyMatch(sensitivePattern -> trimmedLine.contains(sensitivePattern));

                        // Step 2: Ignore if the sensitive word is part of a variable or method name
                        boolean isVariableOrMethod = ignoreVariables.stream().anyMatch(pattern -> pattern.matcher(trimmedLine).find());

                        if (containsSensitiveInfo && !isVariableOrMethod) {
                            // Add the line number and file path to the report instead of the line content
                            String message = "Sensitive info found at line " + lineNumber[0] + " in file " + filePath.toString();
                            ReportGenerator.addToReport(message, filePath, ""+lineNumber[0]);
                        }
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Step 4: Analyze the source code for sensitive attributes and add findings to the report for Java files
        if (isJavaFile) {
            analyzeSourceCode(filePath, sensitivePatterns);
        }
    }

    // Utility to parse a Java file into a CompilationUnit
    private static CompilationUnit parseJavaFile(Path filePath) throws IOException {
        JavaParser parser = new JavaParser();
        Optional<CompilationUnit> cuOptional = parser.parse(filePath.toFile()).getResult();
        return cuOptional.orElseThrow(() -> new IOException("Failed to parse " + filePath));
    }

    // Method to collect all getters that return sensitive fields globally
    private static Map<String, String> collectSensitiveGetters(CompilationUnit cu, List<String> sensitiveAttributes) {
        Map<String, String> methodToFieldMap = new HashMap<>();

        // Collect all field declarations and methods
        cu.findAll(FieldDeclaration.class).forEach(fieldDecl -> {
            fieldDecl.getVariables().forEach(variable -> {
                String fieldName = variable.getNameAsString();

                // If the field is sensitive, find getter methods for it
                if (sensitiveAttributes.contains(fieldName)) {
                    cu.findAll(MethodDeclaration.class).forEach(methodDecl -> {
                        methodDecl.getBody().ifPresent(body -> {
                            if (body.findAll(ReturnStmt.class).stream()
                                    .anyMatch(returnStmt -> returnStmt.toString().contains(fieldName))) {
                                // Store the method name and the field it returns
                                methodToFieldMap.put(methodDecl.getNameAsString(), fieldName);
                            }
                        });
                    });
                }
            });
        });

        return methodToFieldMap;
    }

    private static void analyzeSourceCode(Path sourceFilePath, List<String> sensitiveAttributes) {
        try {
            // Parse the source file using JavaParser
            JavaParser parser = new JavaParser();
            Optional<CompilationUnit> cuOptional = parser.parse(sourceFilePath.toFile()).getResult();

            if (cuOptional.isPresent()) {
                CompilationUnit cu = cuOptional.get();

                // Check all fields in the classes to find sensitive attributes
                cu.findAll(FieldDeclaration.class).forEach(fieldDecl -> {
                    fieldDecl.getVariables().forEach(variable -> {
                        String varName = variable.getNameAsString();
                        if (sensitiveAttributes.contains(varName)) {
                            // If sensitive attribute is found, now look for log statements
                            cu.findAll(MethodDeclaration.class).forEach(methodDecl -> {
                                methodDecl.getBody().ifPresent(body -> {
                                    body.findAll(ExpressionStmt.class).forEach(exprStmt -> {
                                        String statement = exprStmt.toString().trim();

                                        // Check if the log patterns exist and the sensitive attribute is logged
                                        try {
                                            if (isLogStatement(statement) && statement.contains(varName)) {
                                                // Get line number for the log statement
                                                int lineNumber = exprStmt.getBegin().map(pos -> pos.line).orElse(-1);

                                                // Add sensitive attribute found in log statement to the report
                                                String message = "Sensitive attribute '" + varName + "' found in log statement at line "
                                                        + lineNumber + " in file " + sourceFilePath.toString();
                                                ReportGenerator.addToReport(message, sourceFilePath, ""+lineNumber);
                                            }
                                        } catch (IOException e) {
                                            throw new RuntimeException(e);
                                        }
                                    });
                                });
                            });
                        }
                    });
                });
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * This helper method checks if a given statement matches log patterns.
     */
    private static boolean isLogStatement(String statement) throws IOException {
        List<Pattern> logPatterns = ConfigLoader.getLogPatterns();
        return logPatterns.stream().anyMatch(pattern -> pattern.matcher(statement).find());
    }
}
