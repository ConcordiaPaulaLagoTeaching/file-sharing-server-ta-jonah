package ca.concordia.server;
import ca.concordia.filesystem.FileSystemManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class FileServer {

    private FileSystemManager fsManager;
    private int port;

    public FileServer(int port, String fileSystemName, int totalSize) throws Exception {
        this.fsManager = new FileSystemManager(fileSystemName, 10 * 128);
        this.port = port;
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(12345)) {
            System.out.println("Server started. Listening on port 12345...");

            while (true) {

                Socket clientSocket = serverSocket.accept();
                System.out.println("Handling client: " + clientSocket);

                try (
                        BufferedReader reader = new BufferedReader(
                                new InputStreamReader(clientSocket.getInputStream()));
                        PrintWriter writer = new PrintWriter(
                                clientSocket.getOutputStream(), true)
                ) {

                    writer.println("CONNECTED: You are connected to FileServer on port 12345");
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
                        System.out.println("Received from client: " + line);

                        String[] parts = line.split(" ");
                        String command = parts[0].toUpperCase();

                        // WRAP EVERYTHING IN TRY/CATCH
                        try {

                            switch (command) {

                                case "CREATE":
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

                                    if (content.startsWith("\"") && content.endsWith("\"")) {
                                        content = content.substring(1, content.length() - 1);
                                    }

                                    fsManager.writeFile(filename, content);
                                    writer.println("SUCCESS: File written.");
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


                                case "QUIT":
                                    writer.println("SUCCESS: Disconnecting.");
                                    return;   // <-- ONLY QUIT closes the client

                                default:
                                    writer.println("ERROR: Unknown command.");
                                    break;
                            }

                        } catch (Exception cmdError) {
                            // Send filesystem/command error back to the client
                            writer.println("ERROR: " + cmdError.getMessage());
                        }

                        writer.flush();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try { clientSocket.close(); } catch (Exception ignored) {}
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Could not start server on port 12345");
        }
    }
}
