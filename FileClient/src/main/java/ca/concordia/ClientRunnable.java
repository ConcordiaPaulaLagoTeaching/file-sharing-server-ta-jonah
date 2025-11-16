package ca.concordia;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientRunnable implements Runnable {

    private final int id;
    private final int mode;

    public ClientRunnable(int id, int mode) {
        this.id = id;
        this.mode = mode;
    }

    @Override
    public void run() {

        try (Socket clientSocket = new Socket("localhost", 12345);
             BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true)) {

            Thread.sleep(10);
            while (reader.ready()) reader.readLine();

            String filename = "file" + ((id % 5) + 1) + ".txt";

            switch (mode) {
                case 1 -> doCreate(writer, reader, filename);
                case 2 -> doWrite(writer, reader, filename);
                case 3 -> doRead(writer, reader, filename);
                case 4 -> doDelete(writer, reader, filename);
                case 5 -> doList(writer, reader);
            }

            writer.println("QUIT");

        } catch (Exception e) {
            System.err.println("[Client " + id + "] Error: " + e.getMessage());
        }
    }

    // MODE 1: CREATE
    private void doCreate(PrintWriter writer, BufferedReader reader, String filename) throws Exception {
        writer.println("CREATE " + filename);
        System.out.println("[CREATE] Thread " + id + " -> " + filename);
        System.out.println(reader.readLine());
    }

    // MODE 2: WRITE
    private void doWrite(PrintWriter writer, BufferedReader reader, String filename) throws Exception {
        //Random time for testing
        Thread.sleep(50+(int)(Math.random()*1000));

        String data = "\"Thread " + id + " is writing to " + filename + "\"";
        writer.println("WRITE " + filename + " " + data);

        System.out.println("[WRITE] Thread " + id + " sent: " + data);
        System.out.println("[WRITE] Server -> " + reader.readLine());
    }

    // MODE 3: READ
    private void doRead(PrintWriter writer, BufferedReader reader, String filename) throws Exception {
        writer.println("READ " + filename);
        System.out.println("[READ] Thread " + id + " reading " + filename);

        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println("[READ] " + id + ": " + line);
            if (line.equals("END")) break;
        }
    }

    // MODE 4: DELETE
    private void doDelete(PrintWriter writer, BufferedReader reader, String filename) throws Exception {
        writer.println("DELETE " + filename);
        System.out.println("[DELETE] Thread " + id + " deleting " + filename);
        System.out.println("[DELETE] Server -> " + reader.readLine());
    }

    //MODE 5: LIST
    private void doList(PrintWriter writer, BufferedReader reader) throws Exception {
        writer.println("LIST");
        System.out.println("[LIST] Thread " + id + " listing files");

        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println("[LIST] " + id + ": " + line);
            if (!reader.ready()) break;
        }
    }
}
