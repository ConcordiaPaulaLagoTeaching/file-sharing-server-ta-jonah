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
    
    public FileServer(int port, String fileSystemName, int totalSize){
        this.port = port;
        FileSystemManager fsManager = new FileSystemManager(fileSystemName, totalSize); // should pass totalSize here rather than hardcoding
        this.fsManager = fsManager;;
    }

    public void start(){
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started. Listening on port " + port + "...");

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
                        String[] parts = line.split(" ", 2);
                        String command = parts[0].toUpperCase();

                        try {
                            switch (command) {
                                case "CREATE":
                                    //Error handling for missing filename
                                    if (parts.length < 2) {
                                        writer.println("ERROR: filename required");
                                        break;
                                    }
                                    //Create file
                                    fsManager.createFile(parts[1]);
                                    writer.println("SUCCESS: File '" + parts[1] + "' created.");
                                    break;

                                //TODO: Implement other commands READ, WRITE, DELETE, LIST              
                                case "QUIT":
                                    writer.println("SUCCESS: Disconnecting.");
                                    return;
                                    
                                default:
                                    writer.println("ERROR: Unknown command.");
                                    break;
                            }
                        } catch (Exception e) {
                            writer.println(e.getMessage()); //better error reporting to client
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