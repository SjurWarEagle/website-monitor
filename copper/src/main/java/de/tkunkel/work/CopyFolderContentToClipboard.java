package de.tkunkel.work;

import java.awt.GraphicsEnvironment; // Still useful for initial check, though native bypasses it
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
// ... other imports ...
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
import java.nio.file.StandardOpenOption; // Added for Files.readString options

public class CopyFolderContentToClipboard {

    public static void main(String[] args) {
        // --- 1. Handle command-line arguments ---
        if (args.length != 1) {
            System.err.println("Usage: java CopyFolderContentToClipboard <folder_path>");
            System.exit(1);
        }

        Path startFolderPath = Paths.get(args[0]);

        // --- 2. Validate the input path ---
        if (!Files.exists(startFolderPath)) {
            System.err.println("Error: Folder not found at " + startFolderPath);
            System.exit(1);
        }
        if (!Files.isDirectory(startFolderPath)) {
            System.err.println("Error: The provided path is not a directory: " + startFolderPath);
            System.exit(1);
        }

        System.out.println("Processing folder: " + startFolderPath.toAbsolutePath());

        StringBuilder combinedContent = new StringBuilder();
        final String FILE_SEPARATOR = "\n\n--x--x--x-- File: "; // Separator before each file's info
        final String CONTENT_SEPARATOR = "--x--x--x--"; // Separator before content starts

        // --- 3. Traverse the directory and collect file content ---
        try (Stream<Path> walkStream = Files.walk(startFolderPath)) {
            walkStream
                    .filter(Files::isRegularFile) // Only process regular files
                    .forEach(filePath -> {
                        try {
                            System.out.println("Processing file: " + filePath.toAbsolutePath());
                            // Append file name and path
                            combinedContent.append(CONTENT_SEPARATOR)
                                    .append("\n");
                            combinedContent.append("Filename:")
                                    .append(filePath.toAbsolutePath())
                                    .append("\n");
                            combinedContent.append("content:\n")
                                    .append(CONTENT_SEPARATOR)
                                    .append("  \n");

                            // Read and append file content
                            // Assumes text files and default charset (usually UTF-8)
                            String content = Files.readString(filePath); // Explicitly use READ
                            combinedContent.append(content);
                            combinedContent.append("\n");
                            combinedContent.append(CONTENT_SEPARATOR);
                            combinedContent.append("\n");

                        } catch (IOException e) {
                            System.err.println("Error reading file: " + filePath + " - " + e.getMessage());
                            // Continue with the next file
                        }
                    });

        } catch (IOException e) {
            System.err.println("Error during directory traversal: " + e.getMessage());
            System.exit(1);
        }

        // --- 4. Copy the combined content to the clipboard using native tools ---
        String finalString = combinedContent.toString();

        if (finalString.isEmpty() || finalString.trim().equals(FILE_SEPARATOR.trim())) {
            System.out.println("No files found or processed in the directory.");
        } else {
            System.out.println("Successfully collected content. Total size: " + finalString.length() + " characters.");
            try {
                copyToClipboardNative(finalString); // Use the native method
                System.out.println("Content copied to clipboard.");
            } catch (IOException | InterruptedException e) {
                System.err.println("Error copying content to clipboard using native tools: " + e.getMessage());
                System.err.println("Please ensure 'wl-copy', 'xclip', or 'xsel' is installed and accessible in your PATH.");
            }
        }
    }

    /**
     * Copies the given string to the system clipboard using native command-line tools (wl-copy, xclip, xsel).
     * Tries wl-copy first, then xclip, then xsel.
     *
     * @param text The string to copy.
     * @throws IOException          if there's an I/O error executing or writing to the process.
     * @throws InterruptedException if the process is interrupted.
     * @throws RuntimeException     if no suitable clipboard tool is found or all attempts fail.
     */
    private static void copyToClipboardNative(String text) throws IOException, InterruptedException {
        // Define the potential clipboard commands and their arguments
        // Order matters: Wayland first, then X tools
        String[][] commands = {
                {"wl-copy"},               // For Wayland
                {"xclip", "-selection", "clipboard"}, // For X (CLIPBOARD selection)
                {"xsel", "--clipboard"}    // For X (CLIPBOARD selection)
        };

        for (String[] command : commands) {
            try {
                System.out.println("Attempting to use command: " + String.join(" ", command));
                ProcessBuilder pb = new ProcessBuilder(command);

                // Start the process
                Process process = pb.start();

                // Get the standard input stream of the command and write the text to it
                // Use try-with-resources to ensure streams are closed
                try (OutputStream os = process.getOutputStream();
                     OutputStreamWriter osw = new OutputStreamWriter(os, StandardCharsets.UTF_8)) { // Use UTF-8 for encoding
                    osw.write(text);
                    // Ensure the output stream is flushed and closed, indicating end of input
                } // os and osw are automatically closed here

                // Wait for the process to complete
                int exitCode = process.waitFor();

                // Check the exit code. 0 typically indicates success.
                // Different tools might have different success codes, but 0 is standard.
                if (exitCode == 0) {
                    System.out.println("Command succeeded.");
                    return; // Success! Exit the method.
                } else {
                    System.err.println("Command failed with exit code: " + exitCode);
                    // You might read the process's stderr here for more info
                    // process.getErrorStream()
                }

            } catch (IOException e) {
                System.err.println("IOException when attempting command " + command[0] + ": " + e.getMessage());
                // This often means the command (e.g., "wl-copy") was not found in the PATH
                // Continue to the next command attempt
            }
            // InterruptedException is caught by the method signature
        }

        // If the loop finishes without returning, none of the commands worked
        throw new RuntimeException("Failed to copy to clipboard using any available native tool (wl-copy, xclip, xsel).");
    }
}
