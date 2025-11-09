package ca.concordia.server;
import ca.concordia.filesystem.ClientHandler;
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
                System.out.println("Server handling client: " + clientSocket);
                // Calling new thread to handle client
                new Thread(new ClientHandler(clientSocket, fsManager)).start();
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
                                String readString = fsManager.readFile(parts[1]);
                                writer.println("SUCCESS: File '" + parts[1] + "' contains : " + readString);
                                writer.flush();
                                break;
                            case "WRITE":
                                String writenLine = line.replace(parts[0] + " " + parts[1] + " ", "");
                                fsManager.writeFile(parts[1], writenLine);
                                writer.println("SUCCESS: File '" + parts[1] + "' written to with : " + writenLine);
                                writer.flush();
                                break;
                            case "DELETE":
                                fsManager.deleteFile(parts[1]);
                                writer.println("SUCCESS: File '" + parts[1] + "' was deleted.");
                                writer.flush();
                                break;
                            case "LIST":
                                String list = fsManager.list();
                                writer.println("SUCCESS: files listed! these are: " + list);
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
