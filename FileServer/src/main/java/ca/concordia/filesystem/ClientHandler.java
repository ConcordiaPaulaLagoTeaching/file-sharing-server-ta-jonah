package ca.concordia.filesystem;

import ca.concordia.filesystem.FileSystemManager;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final FileSystemManager fsm;

    public ClientHandler(Socket clientSocket, FileSystemManager fsm) {
        this.clientSocket = clientSocket;
        this.fsm = fsm;
    }
    @Override
    public void run() {
        System.out.println("[" + Thread.currentThread().getName() + "] Handling client: " + clientSocket);
        try (
                BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {
            String line;
            while ((line = reader.readLine()) != null){
                System.out.println("SUCCESS: Client '" + clientSocket + "' connected!");
                String[] parts = line.split("");
                String command = parts[0].toUpperCase();

                try {
                    switch (command) {
                        case "CREATE":
                            fsm.createFile(parts[1]);
                            writer.println("SUCCESS: File '" + parts[1] + "' created!");
                            break;
                        case "READ":
                            fsm.readFile(parts[1]);
                            writer.println("SUCCESS: File '" + parts[1] + "' was read!");
                            break;
                        case "WRITE":
                            fsm.writeFile(parts[1], parts[2]);
                            writer.println("SUCCESS: File '" + parts[1] + "' written!");
                            break;
                        case "DELETE":
                            fsm.deleteFile(parts[1]);
                            writer.println("SUCCESS: File '" + parts[1] + "' deleted!");
                            break;
                        case "LIST":
                            fsm.list();
                            writer.println("SUCCESS: Files listed!");
                            break;
                        case "QUIT":
                            writer.println("Disconnecting...");
                            return;
                        default:
                            writer.println("ERROR: Unknown Command!");
                    }
                } catch (Exception e) {
                    writer.println("ERROR: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } catch (IOException e){
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } catch (IOException ignored) {}
        }
    }
}
