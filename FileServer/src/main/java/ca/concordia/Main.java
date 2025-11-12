package ca.concordia;

import ca.concordia.server.FileServer;

public class Main {
    public static void main(String[] args) {
        System.out.printf("Hello and welcome!");
        
        // port, file name, total size
        FileServer server = new FileServer(12345, "filesystem.dat", 4096);
        server.start();
    }
}
