package com.scan.sensitiveinfo;


import org.eclipse.jgit.api.Git;

import java.io.File;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;

public class Scanner {

    public static void traverseRepository(String cloneDir, String outputFile) throws Exception {
        ConfigLoader.loadSensitivePatternsConfig();
        ConfigLoader.loadIgnorePathsConfig();
        ConfigLoader.loadFileTypesConfig();
        ConfigLoader.loadLogPatternsConfig();
        ConfigLoader.loadIgnoreVariablesConfig();

        List<String> ignorePaths = ConfigLoader.getIgnorePaths();
        List<String> fileTypes = ConfigLoader.getFileTypes(); // Load file extensions to scan

        Files.walk(Paths.get(cloneDir))
                .filter(Files::isRegularFile)
                .filter(path -> ignorePaths.stream().noneMatch(path.toString()::contains))
                .forEach(path -> {
                    try {
                        // Check if the file has a valid extension based on the property file
                        boolean isSupportedFileType = fileTypes.stream()
                                .anyMatch(ext -> path.toString().endsWith(ext));

                        if (isSupportedFileType) {
                            SensitiveInfoScanner.scanFile(path, outputFile);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
    }

    public static void main(String[] args) throws Exception {
        String clonedLocation = args[0];
        String outputFile = args[1];

        traverseRepository(clonedLocation, outputFile);
        ReportGenerator.generateReport(outputFile);
        /*
        //This is for if we need to work with git repos directly
        String branch = "releaseems700";
        cloneAndScanRepositories(branch);
        ReportGenerator.generateReport("sensitive_info_report.txt");

         */
    }

    /**
     * This is not getting used
     * @param branch
     * @throws Exception
     */
    public static void cloneAndScanRepositories(String... branch) throws Exception {
        ConfigLoader.loadGitReposConfig();
        List<String> gitRepos = ConfigLoader.getGitReposConfig();

        for (int i = 0; i < gitRepos.size(); i++) {
            String repoUrl = gitRepos.get(i);
            String cloneDir = "cloned_repo_" + (i + 1);

            System.out.println("Cloning repository: " + repoUrl);

            // Set up SSH session factory with the private key
            /*SshSessionFactory.setInstance(new OpenSshSessionFactory() {
                @Override
                protected void configure(OpenSshConfig.Host host, Session session) {
                    // Specify your private key file here
                    session.addIdentity("/Users/asgupta6/.ssh"); // Path to your private SSH key
                }
            });*/


            Git.cloneRepository()
                    .setURI(repoUrl)
                    .setBranchesToClone(Arrays.asList(branch))
                    .setDirectory(new File(cloneDir))
                    .call();

            System.out.println("Repository cloned to " + cloneDir);
            traverseRepository(cloneDir);
        }
    }

    /**
     * this is not getting used
     * @param cloneDir
     * @throws Exception
     */
    public static void traverseRepository(String cloneDir) throws Exception {
        ConfigLoader.loadSensitivePatternsConfig();
        ConfigLoader.loadIgnorePathsConfig();

        List<String> sensitivePatterns = ConfigLoader.getSensitivePatternsConfig();
        List<String> ignorePaths = ConfigLoader.getIgnorePaths();

        Files.walk(Paths.get(cloneDir))
                .filter(Files::isRegularFile)
                .filter(path -> ignorePaths.stream().noneMatch(path.toString()::contains))
                .forEach(path -> {
                    try {
                        if (path.toString().endsWith(".java")
                                || path.toString().endsWith(".properties")
                                || path.toString().endsWith(".yml")) {
                            SensitiveInfoScanner.scanFile(path, sensitivePatterns);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
    }
}
