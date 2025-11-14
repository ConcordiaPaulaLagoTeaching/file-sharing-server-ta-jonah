package ca.concordia.server;

import ca.concordia.filesystem.FileSystemManager;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final FileSystemManager fsManager;

    public ClientHandler(Socket clientSocket, FileSystemManager fsManager) {
        this.clientSocket = clientSocket;
        this.fsManager = fsManager;
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