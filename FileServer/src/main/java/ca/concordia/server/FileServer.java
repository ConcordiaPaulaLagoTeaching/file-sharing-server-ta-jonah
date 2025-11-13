package ca.concordia.server;
import ca.concordia.filesystem.FileSystemManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class FileServer {

    private final FileSystemManager fsManager;
    private final int port;

    public FileServer(int port, String fileSystemName, int totalSize) throws Exception {
        // Initialize the FileSystemManager
        this.fsManager = new FileSystemManager(fileSystemName, 10*128 );
        this.port = port;
    }

    public void start(){
        try (ServerSocket serverSocket = new ServerSocket(12345)) {
            System.out.println("Server started. Listening on port 12345...");

            while (true) { // Connection with client established
                Socket clientSocket = serverSocket.accept();
                System.out.println("Handling client: " + clientSocket);
                ClientHandler clientHandler = new ClientHandler(clientSocket, fsManager);
                Thread client_thread = new Thread(new ClientHandler(clientSocket, fsManager));
                client_thread.start();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            System.err.println("Could not start server on port " + port);
        }
    }
    // One-to- one thread mapping, enables concurrency
    private static class ClientHandler implements Runnable{
           private final Socket clientSocket;
           private final FileSystemManager fsm ;

           ClientHandler(Socket clientSocket, FileSystemManager fsm){
               this.clientSocket = clientSocket;
               this.fsm = fsm;
           }
        @Override
        public void run() {
          System.out.println("Client " + clientSocket + " in thread "+ Thread.currentThread().getName());
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
                            fsm.createFile(parts[1]);
                            writer.println("SUCCESS: File '" + parts[1] + "' created.");
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
    }

}
