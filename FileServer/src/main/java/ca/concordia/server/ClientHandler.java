package ca.concordia.server;

import ca.concordia.filesystem.FileSystemManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private final Socket clientSocket;
    private final FileSystemManager fsManager;

    public ClientHandler(Socket clientSocket, FileSystemManager fsManager) {
        this.clientSocket = clientSocket;
        this.fsManager = fsManager;
    }

    @Override
    public void run() {
        System.out.println("Handling client in thread: " + Thread.currentThread().getName()
                + " - " + clientSocket);

        try (
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter writer = new PrintWriter(
                        clientSocket.getOutputStream(), true)
        ) {

            writer.println("CONNECTED: You are connected to FileServer on port " + clientSocket.getLocalPort());
            writer.println("Available commands:");
            writer.println("  CREATE <filename>");
            writer.println("  WRITE <filename> <text>");
            writer.println("  READ <filename>");
            writer.println("  DELETE <filename>");
            writer.println("  LIST");
            writer.println("  QUIT");
            writer.println("--------------------------------------------");
            writer.flush();

            String line;

            while ((line = reader.readLine()) != null) {
                System.out.println("[" + clientSocket + "] Received: " + line);

                String[] parts = line.split(" ");
                if (parts.length == 0 || parts[0].isBlank()) {
                    writer.println("ERROR: Empty command.");
                    writer.flush();
                    continue;
                }

                String command = parts[0].toUpperCase();

                try {
                    switch (command) {

                        case "CREATE":
                            if (parts.length < 2) {
                                writer.println("ERROR: CREATE requires a filename");
                                break;
                            }
                            fsManager.createFile(parts[1]);
                            writer.println("SUCCESS: File '" + parts[1] + "' created.");
                            break;

                        case "WRITE":
                            if (parts.length < 3) {
                                writer.println("ERROR: WRITE requires a filename and content");
                                break;
                            }

                            String filename = parts[1];
                            String content = line.substring(line.indexOf(filename) + filename.length()).trim();

                            if (content.startsWith("\"") && content.endsWith("\"") && content.length() >= 2) {
                                content = content.substring(1, content.length() - 1);
                            }

                            try {
                                fsManager.writeFile(filename, content);
                                writer.println("SUCCESS: File written.");
                            } catch (Exception ex) {
                                //for concurrent test
                                if (ex.getMessage().equals("BUSY_WRITING")) {
                                    writer.println("Another user is writing. Retrying in 3 seconds...");
                                    writer.flush();

                                    Thread.sleep(15000);
                                    // try again automatically after delay
                                    fsManager.writeFile(filename, content);
                                    writer.println("SUCCESS: File written after retry.");
                                } else {
                                    writer.println("ERROR: " + ex.getMessage());
                                }
                            }

                            break;

                        case "READ":
                            if (parts.length < 2) {
                                writer.println("ERROR: READ requires a filename");
                                break;
                            }
                            try {
                                String result = fsManager.readFile(parts[1]);
                                writer.println("CONTENT inside this file:");
                                writer.println(result);
                                writer.println("END");
                            } catch (Exception ex) {
                                writer.println("ERROR: " + ex.getMessage());
                            }
                            break;

                        case "DELETE":
                            if (parts.length < 2) {
                                writer.println("ERROR: DELETE requires a filename");
                                break;
                            }
                            try {
                                fsManager.deleteFile(parts[1]);
                                writer.println("SUCCESS: File '" + parts[1] + "' deleted.");
                            } catch (Exception ex) {
                                writer.println("ERROR: " + ex.getMessage());
                            }
                            break;

                        case "LIST":
                            try {
                                String listing = fsManager.listFiles();
                                if (listing.isEmpty()) {
                                    writer.println("No files on server.");
                                } else {
                                    writer.println("FILES:");
                                    writer.println(listing);
                                }
                            } catch (Exception ex) {
                                writer.println("ERROR: " + ex.getMessage());
                            }
                            break;

                        case "QUIT":
                            writer.println("SUCCESS: Disconnecting.");
                            writer.flush();
                            return;

                        default:
                            writer.println("ERROR: Unknown command.");
                            break;
                    }
                } catch (Exception cmdError) {
                    writer.println("ERROR: " + cmdError.getMessage());
                }

                writer.flush();
            }

        } catch (Exception e) {
            System.err.println("Client handler error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try { clientSocket.close(); } catch (Exception ignored) {}
            System.out.println("Client disconnected: " + clientSocket);
        }
    }
}
