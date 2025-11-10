package ca.concordia.server;

import ca.concordia.filesystem.FileSystemManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientServerHandler implements Runnable{
    private FileSystemManager fsManager;
    private Socket clientSocket;
    public ClientServerHandler(FileSystemManager fsManager, Socket clientSocket) throws IOException {
        // Initialize the FileSystemManager
        this.clientSocket = clientSocket;
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
}
