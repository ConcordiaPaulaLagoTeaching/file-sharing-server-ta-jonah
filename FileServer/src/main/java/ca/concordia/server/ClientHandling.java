package ca.concordia.server;

import ca.concordia.filesystem.FileSystemManager;

import java.io.*;
import java.net.Socket;

public class ClientHandling implements Runnable {
    private final Socket clientSocket;
    private final FileSystemManager fsManager;

    public ClientHandling(Socket socket, FileSystemManager fsManager) {
        this.clientSocket = socket;
        this.fsManager = fsManager;
    }

    @Override
    public void run() {
        try (
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("Received from client: " + line);
                String[] parts = line.split(" ", 3);
                String command = parts[0].toUpperCase();

                try {
                    switch (command) {
                        case "CREATE":
                            if (parts.length < 2) {
                                writer.println("ERROR: CREATE requires a filename.");
                                break;
                            }
                            fsManager.createFile(parts[1]);
                            writer.println("SUCCESS: File '" + parts[1] + "' created.");
                            break;

                        case "DELETE":
                            if (parts.length < 2) {
                                writer.println("ERROR: DELETE requires a filename.");
                                break;
                            }
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
                            if (parts.length < 2) {
                                writer.println("ERROR: READ requires a filename.");
                                break;
                            }
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
                } catch (Exception e) {
                    writer.println("ERROR: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException ignored) {}
        }
    }
}
