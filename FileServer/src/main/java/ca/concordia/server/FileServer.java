package ca.concordia.server;

import ca.concordia.filesystem.FileSystemManager;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FileServer {

    private final FileSystemManager fsManager;
    private final int port;
    private final ExecutorService pool;

    // Constructor with 3 parameters
    public FileServer(int port, String fileSystemName, int totalSize) {
        this.fsManager = new FileSystemManager(fileSystemName, totalSize);
        this.port = port;
        this.pool = Executors.newFixedThreadPool(20); // up to 20 clients concurrently
    }

    // Start method
    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started on port " + port + "...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket);
                pool.execute(new ClientHandler(clientSocket, fsManager));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
