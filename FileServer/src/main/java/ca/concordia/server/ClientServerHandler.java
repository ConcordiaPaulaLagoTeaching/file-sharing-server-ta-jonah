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
                int status;
                switch (command) {
                    case "CREATE":
                        status = fsManager.createFile(parts[1]);
                        checkStatus(status, parts, writer);
                        writer.println("' was created.");
                        writer.flush();
                        break;
                    case "READ":
                        String readString = fsManager.readFile(parts[1]);
                        status = readString == null ? fsManager.FILEISEMPTY : readString.equals(String.valueOf(fsManager.FILENOTFOUND)) ? fsManager.FILENOTFOUND : fsManager.SUCCESS;
                        checkStatus(status, parts, writer);
                        writer.println("' contains : " + readString);
                        writer.flush();
                        break;
                    case "WRITE":
                        String writenLine = line.replace(parts[0] + " " + parts[1] + " ", "");
                        status = fsManager.writeFile(parts[1], writenLine);
                        checkStatus(status, parts, writer);
                        if (status == fsManager.SUCCESS) {
                            writer.println("SUCCESS: File '" + parts[1] + "' written to with : " + writenLine);
                            writer.flush();
                        }
                        break;
                    case "DELETE":
                        status = fsManager.deleteFile(parts[1]);
                        checkStatus(status, parts, writer);
                        if (status == fsManager.SUCCESS) {
                            writer.println("SUCCESS: File '" + parts[1] + "' was deleted.");
                            writer.flush();
                        }
                        break;
                    case "LIST":
                        String list = fsManager.list();
                        writer.println("SUCCESS: files listed! these are: " + list);
                        writer.flush();
                        break;
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

    private void checkStatus(int status, String[] parts, PrintWriter writer) {
        if(status == fsManager.FILENAMETOOLONG){
            writer.println("ERROR: File '" + parts[1] + "' has more than 11 characters.");
        }
        else if(status == fsManager.DISKFULL){
            writer.println("ERROR: Disk is full.");
        }
        else if(status == fsManager.FILESPACEFULL){
            writer.println("ERROR: Disk already has " + fsManager.MAXFILES + " files.");
        }
        else if(status == fsManager.FILEISEMPTY){
            writer.println("SUCCESS: File '" + parts[1] + "' is empty");
        }
        else if(status == fsManager.FILENOTFOUND){
            writer.println("ERROR: File '" + parts[1] + "' not found");
        }
        else {
            writer.print("SUCCESS: File '" + parts[1]);
        }
        writer.flush();
    }
}
