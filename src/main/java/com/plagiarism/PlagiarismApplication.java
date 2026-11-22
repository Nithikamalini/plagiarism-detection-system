package com.plagiarism;

import com.plagiarism.server.WebServer;

/**
 * Main application bootstrap class.
 */
public class PlagiarismApplication {

    public static void main(String[] args) {
        int port = 8080;
        String envPort = System.getenv("PORT");
        if (envPort != null && !envPort.trim().isEmpty()) {
            try {
                port = Integer.parseInt(envPort.trim());
            } catch (NumberFormatException ignored) {}
        } else if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {}
        }

        try {
            WebServer webServer = new WebServer(port);
            webServer.start();

            // Keep main thread alive
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Shutting down Plagiarism Detection System...");
                webServer.stop();
            }));

        } catch (Exception e) {
            System.err.println("Failed to start application: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
