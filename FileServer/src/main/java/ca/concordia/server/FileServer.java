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

                // Multithreading implementation
                // Each client is handled in its own thread
                Thread t = new Thread(new ClientHandler(clientSocket, fsManager));
                t.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Could not start server on port 12345");
        }
    }
}