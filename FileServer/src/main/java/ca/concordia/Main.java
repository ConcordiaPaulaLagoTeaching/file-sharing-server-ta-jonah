package ca.concordia;

import ca.concordia.server.FileServer;

public class Main {
    public static void main(String[] args) throws Exception {
        System.out.printf("Hello and welcome!");

        FileServer server = new FileServer(12345, "filesystem.dat", 10 * 128);
        // Start the file server
        System.out.print("Please select 1 of the following: \n");
        System.out.print("Create files \n");
        System.out.print("View files \n");
        System.out.print("Delete files \n");
        System.out.print("List all files \n");
        server.start();
    }
}