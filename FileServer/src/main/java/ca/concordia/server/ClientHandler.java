package ca.concordia.server;

import java.io.*;
import java.net.*;
import ca.concordia.filesystem.FileSystemManager;

public class ClientHandler implements Runnable {
    private Socket socket;
    private FileSystemManager fs;

    public ClientHandler(Socket socket, FileSystemManager fs) {
        this.socket = socket;
        this.fs = fs;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(
                 new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            String line;
            while ((line = in.readLine()) != null) {
                String[] parts = line.trim().split(" ", 3);
                String command = parts[0].toUpperCase();

                switch (command) {
                    case "CREATE":
                        fs.createFile(parts[1]);
                        out.println("OK: file created");
                        break;

                    case "WRITE":
                        fs.writeFile(parts[1], parts[2].getBytes());
                        out.println("OK: written");
                        break;

                    case "READ":
                        byte[] data = fs.readFile(parts[1]);
                        out.println("OK: " + new String(data));
                        break;

                    case "DELETE":
                        fs.deleteFile(parts[1]);
                        out.println("OK: file deleted");
                        break;

                    case "LIST":
                        String[] list = fs.listFiles();
                        out.println("OK: " + String.join(", ", list));
                        break;

                    default:
                        out.println("ERROR: Unknown command");
                }
            }
        } catch (Exception e) {
            System.err.println("Client error: " + e.getMessage());
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }
}
