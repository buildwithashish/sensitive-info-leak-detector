package com.scan.sensitiveinfo;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class ReportGenerator {

    private static final List<String> reportEntries = new ArrayList<>();

    public static void addToReport(String type, Path filePath, String line) {
        String entry = "[" + type + "]";
        reportEntries.add(entry);
        System.out.println(entry);
    }

    public static void generateReport(String outputFile) throws IOException {
        //Files.write(Paths.get(outputFile), reportEntries);

        /*try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(outputFile))) {
            for (String entry : reportEntries) {
                writer.write(entry);
                writer.newLine(); // Ensure each entry is written on a new line
            }
        }*/

        System.out.println("Generating report at " + outputFile);

        StringBuilder reportContent = new StringBuilder();
        reportEntries.parallelStream().forEach(entry -> {
            reportContent.append(entry).append(System.lineSeparator());
        });
        Files.write(Paths.get(outputFile), reportContent.toString().getBytes());

        // Check if the file exists after writing
        if (Files.exists(Paths.get(outputFile))) {
            // Print a success message once the file is generated
            System.out.println("Report successfully generated at " + outputFile);
        } else {
            // In case the file wasn't created, print an error message
            System.out.println("Error: Report generation failed.");
        }
    }
}
