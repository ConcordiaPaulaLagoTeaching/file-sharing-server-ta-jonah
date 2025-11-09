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

    public FileServer(int port, String fileSystemName, int totalSize) {
        // Correct singleton usage — no variable shadowing
        this.fsManager = FileSystemManager.getInstance(fileSystemName, totalSize);
        this.port = port;
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started. Listening on port " + port + "...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Handling client: " + clientSocket);

                try (
                    BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true)
                ) {
                    try {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            System.out.println("Received from client: " + line);
                            String[] parts = line.split(" ", 3); // allow for WRITE with content
                            String command = parts[0].toUpperCase();

                            switch (command) {
                                case "CREATE":
                                    fsManager.createFile(parts[1]);
                                    writer.println("SUCCESS: File '" + parts[1] + "' created.");
                                    writer.flush();
                                    break;

                                case "DELETE":
                                    fsManager.deleteFile(parts[1]);
                                    writer.println("SUCCESS: File '" + parts[1] + "' deleted.");
                                    break;

                                case "WRITE":
                                    if (parts.length < 3) {
                                        writer.println("ERROR: WRITE requires filename and content.");
                                        break;
                                    }
                                    fsManager.writeFile(parts[1], parts[2].getBytes());
                                    writer.println("SUCCESS: Written to file '" + parts[1] + "'.");
                                    break;

                                case "READ":
                                    byte[] data = fsManager.readFile(parts[1]);
                                    writer.println("SUCCESS: Read from file '" + parts[1] + "': " + new String(data));
                                    break;

                                case "LIST":
                                    String[] files = fsManager.listFiles();
                                    writer.println("SUCCESS: Files on server:");
                                    for (String file : files) {
                                        writer.println(" - " + file);
                                    }
                                    break;

                                case "QUIT":
                                    writer.println("SUCCESS: Disconnecting.");
                                    return;

                                default:
                                    writer.println("ERROR: Unknown command.");
                                    break;
                            }
                        }
                    } catch (Exception e) {
                        writer.println("ERROR: " + e.getMessage());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        clientSocket.close();
                    } catch (Exception e) {
                        // Ignore
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Could not start server on port " + port);
        }
    }
}
