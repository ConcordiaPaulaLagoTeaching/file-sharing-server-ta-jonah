package ca.concordia.server;
import ca.concordia.filesystem.FileSystemManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorsService;
import java.util.concurrent.Executors;
import java.util.concurrent.ReentrantReadWriteLock;

public class FileServer {

    private FileSystemManager fsManager;
    private int port;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final ExecutorsService;
    // Constructor
   public FileServer(int port, String fileSystemName, int totalSize) {
       this.fsManager = new FileSystemManager(fileSystemName, totalSize);
       this.port = port;
       this.pool = Executors.newFixedThreadPool(10); // supports 10 concurrent clients
   }


   // Start server
   public void start() {
       try (ServerSocket serverSocket = new ServerSocket(port)) {
           System.out.println("File Server started. Listening on port " + port + "...");


           while (true) {
               Socket clientSocket = serverSocket.accept();
               System.out.println("🧩 Connected: " + clientSocket);
               pool.execute(new ClientHandler(clientSocket, fsManager, rwLock));
           }


       } catch (Exception e) {
           e.printStackTrace();
           System.err.println("Could not start server on port " + port);
       }
   }


   // Client handler class
   private static class ClientHandler implements Runnable {


       private final Socket socket;
       private final FileSystemManager fs;
       private final ReentrantReadWriteLock lock;


       public ClientHandler(Socket socket, FileSystemManager fs, ReentrantReadWriteLock lock) {
           this.socket = socket;
           this.fs = fs;
           this.lock = lock;
       }


       @Override
       public void run() {
           try (
                   BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream())); PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {
               writer.println("Welcome to the File Server!");
               String line;


               while ((line = reader.readLine()) != null) {
                   System.out.println("Received: " + line);
                   String response = handleCommand(line.trim());
                   writer.println(response);
                   writer.flush();


                   if (response.equals("SUCCESS: Disconnecting.")) {
                       break;
                   }
               }


           } catch (Exception e) {
               e.printStackTrace();
           } finally {
               try {
                   socket.close();
               } catch (IOException ignored) {
               }
               System.out.println("Client disconnected: " + socket);
           }
       }


       // Handle client commands
       private String handleCommand(String commandLine) {
           try {
               if (commandLine.isEmpty()) {
                   return "ERROR: Empty command";
               }


               String[] parts = commandLine.split(" ", 3);
               String cmd = parts[0].toUpperCase();


               switch (cmd) {
                   case "CREATE":
                       if (parts.length < 2) {
                           return "ERROR: Missing filename";
                       }
                       lock.writeLock().lock();
                       try {
                           fs.createFile(parts[1]);
                           return "SUCCESS: File '" + parts[1] + "' created.";
                       } finally {
                           lock.writeLock().unlock();
                       }


                   case "WRITE":
                       if (parts.length < 3) {
                           return "ERROR: Missing filename or content";
                       }
                       lock.writeLock().lock();
                       try {
                           fs.writeFile(parts[1], parts[2].getBytes());
                           return "SUCCESS: File '" + parts[1] + "' written.";
                       } finally {
                           lock.writeLock().unlock();
                       }


                   case "READ":
                       if (parts.length < 2) {
                           return "ERROR: Missing filename";
                       }
                       lock.readLock().lock();
                       try {
                           byte[] data = fs.readFile(parts[1]);
                           return "SUCCESS: " + new String(data);
                       } finally {
                           lock.readLock().unlock();
                       }


                   case "DELETE":
                       if (parts.length < 2) {
                           return "ERROR: Missing filename";
                       }
                       lock.writeLock().lock();
                       try {
                           fs.deleteFile(parts[1]);
                           return "SUCCESS: File '" + parts[1] + "' deleted.";
                       } finally {
                           lock.writeLock().unlock();
                       }


                   case "LIST":
                       lock.readLock().lock();
                       try {
                           String[] files = fs.listFiles();
                           if (files.length == 0) {
                               return "No files found.";
                           }
                           return "Files: " + String.join(", ", files);
                       } finally {
                           lock.readLock().unlock();
                       }


                   case "QUIT":
                       return "SUCCESS: Disconnecting.";


                   default:
                       return "ERROR: Unknown command.";
               }


           } catch (Exception e) {
               return "ERROR: " + e.getMessage();
           }
       }
   }


   // Main method to start the server
   public static void main(String[] args) {
       FileServer server = new FileServer(12345, "server_disk.img", 128 * 10);
       server.start();
   }
}
