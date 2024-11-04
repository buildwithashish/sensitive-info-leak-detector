package com.scan.sensitiveinfo;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ConfigLoader {
    private static Properties gitReposConfig = new Properties();
    private static Properties sensitivePatternsConfig = new Properties();
    private static Properties ignorePathsConfig = new Properties();
    private static Properties logPatternsConfig = new Properties();
    private static Properties ignoreVariablesConfig = new Properties();
    private static Properties scanFileTypesConfig = new Properties();


    public static void loadGitReposConfig() {
        try (InputStream input = ConfigLoader.class.getClassLoader().getResourceAsStream("git_repos.properties")) {
            if (input == null) {
                System.out.println("Sorry, unable to find git_repos.properties");
                return;
            }
            gitReposConfig.load(input);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static void loadSensitivePatternsConfig() {
        try (InputStream input = ConfigLoader.class.getClassLoader().getResourceAsStream("sensitive_patterns.properties")) {
            if (input == null) {
                System.out.println("Sorry, unable to find sensitive_patterns.properties");
                return;
            }
            sensitivePatternsConfig.load(input);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }


    public static void loadIgnorePathsConfig() throws IOException {
        try (InputStream input = ConfigLoader.class.getClassLoader().getResourceAsStream("ignore_paths.properties")) {
            if (input == null) {
                System.out.println("Sorry, unable to find ignore_paths.properties");
                return;
            }
            ignorePathsConfig.load(input);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static void loadLogPatternsConfig() throws IOException {
        try (InputStream input = ConfigLoader.class.getClassLoader().getResourceAsStream("log_patterns.properties")) {
            if (input == null) {
                System.out.println("Sorry, unable to find log_patterns.properties");
                return;
            }
            logPatternsConfig.load(input);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }

    public static void loadIgnoreVariablesConfig() throws IOException {
        try (InputStream input = ConfigLoader.class.getClassLoader().getResourceAsStream("ignore_variables.properties")) {
            if (input == null) {
                System.out.println("Sorry, unable to find ignore_variables.properties");
                return;
            }
            ignoreVariablesConfig.load(input);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }

    public static void loadFileTypesConfig() throws IOException {
        try (InputStream input = ConfigLoader.class.getClassLoader().getResourceAsStream("scan_file_types.properties")) {
            if (input == null) {
                System.out.println("Sorry, unable to find scan_file_types.properties");
                return;
            }
            scanFileTypesConfig.load(input);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }

    public static List<String> getGitReposConfig() {
        List<String> repos = new ArrayList<>();
        gitReposConfig.stringPropertyNames().forEach(key -> repos.add(gitReposConfig.getProperty(key)));
        return repos;
    }

    public static List<String> getSensitivePatternsConfig() {
        List<String> patterns = new ArrayList<>();
        sensitivePatternsConfig.stringPropertyNames().forEach(key -> patterns.add(sensitivePatternsConfig.getProperty(key)));
        return patterns;
        /*return sensitivePatternsConfig.stringPropertyNames()
                .stream()
                .map(key -> Pattern.quote(sensitivePatternsConfig.getProperty(key)))  // Escape special characters
                .collect(Collectors.toList());*/
    }

    /*
    List<String> patterns = new ArrayList<>();
        sensitivePatternsConfig.stringPropertyNames().forEach(key -> {
            // Add word boundaries to each sensitive pattern
            String pattern = sensitivePatternsConfig.getProperty(key);
            String wordBoundaryPattern = "\\b" + Pattern.quote(pattern) + "\\b";
            patterns.add(wordBoundaryPattern);
        });
     */

    public static List<String> getIgnorePaths() {
        return ignorePathsConfig.values().stream().map(Object::toString).collect(Collectors.toList());
    }

    public static List<Pattern> getLogPatterns() throws IOException  {
        String logPatterns = logPatternsConfig.getProperty("log.patterns");
        return Arrays.asList(logPatterns.split(","))
                .stream()
                .map(Pattern::compile)
                .collect(Collectors.toList());
    }

    public static List<Pattern> getIgnoreVariables() throws IOException {
        String ignoreVars = ignoreVariablesConfig.getProperty("ignore.variables");
        return Arrays.asList(ignoreVars.split(","))
                .stream()
                .map(Pattern::compile)
                .collect(Collectors.toList());
    }

    public static List<String> getFileTypes() throws IOException {
        String ignoreVars = scanFileTypesConfig.getProperty("scan.file.types");
        return Arrays.asList(ignoreVars.split(","))
                .stream()
                .collect(Collectors.toList());
    }
}
