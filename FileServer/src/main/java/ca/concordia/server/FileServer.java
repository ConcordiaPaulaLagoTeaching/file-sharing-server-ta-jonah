package ca.concordia.server;
import ca.concordia.filesystem.FileSystemManager;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class FileServer {

    private FileSystemManager fsManager;
    private int port;
    public FileServer(int port, String fileSystemName, int totalSize) throws IOException {
        // Initialize the FileSystemManager
        FileSystemManager fsManager = new FileSystemManager(fileSystemName,
                10*128 );
        this.fsManager = fsManager;
        this.port = port;
    }

    public void start(){
        try (ServerSocket serverSocket = new ServerSocket(12345)) {
            System.out.println("Server started. Listening on port 12345...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Handling client: " + clientSocket);
                try (
                        BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                        PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true)
                ) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println("Received from client: " + line);
                        String[] parts = line.split(" ");
                        String command = parts[0].toUpperCase();

                        switch (command) {
                            case "CREATE":
                                fsManager.createFile(parts[1]);
                                writer.println("SUCCESS: File '" + parts[1] + "' created.");
                                writer.flush();
                                break;
                            case "READ":
                                fsManager.readFile(parts[1]);
                                writer.println("SUCCESS: File '" + parts[1] + "' was read.");
                                writer.flush();
                                break;
                            case "WRITE":
                                fsManager.writeFile(parts[1], parts[2]);
                                writer.println("SUCCESS: File '" + parts[1] + "' written to with : " + parts[2]);
                                writer.flush();
                                break;
                            case "DELETE":
                                fsManager.deleteFile(parts[1]);
                                writer.println("SUCCESS: File '" + parts[1] + "' was deleted.");
                                writer.flush();
                                break;
                            case "LIST":
                                fsManager.list();
                                writer.println("SUCCESS: files listed!");
                                writer.flush();
                                break;
                            //TODO: Implement other commands READ, WRITE, DELETE, LIST

                            case "QUIT":
                                writer.println("SUCCESS: Disconnecting.");
                                return;
                            default:
                                writer.println("ERROR: Unknown command.");
                                break;
                        }
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
