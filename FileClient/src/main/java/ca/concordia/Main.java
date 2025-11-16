package ca.concordia;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

// Press Shift twice to open the Search Everywhere dialog and type `show whitespaces`,
// then press Enter. You can now see whitespace characters in your code.
public class Main {
    public static void main(String[] args) {
        //Socket CLient
        System.out.println("Hello and welcome!");
        Scanner scanner = new Scanner(System.in);

        try {
            Socket clientSocket = new Socket("localhost", 12345);
            System.out.println("Connected to the server at localhost:12345");

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(clientSocket.getInputStream())
            );
            PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);

            // Immediately read server's greeting header
//            Thread.sleep(10);
//            while (reader.ready()) {
//                System.out.println("Response from server: " + reader.readLine());
//            }

            String first = reader.readLine();
            System.out.println("Response from server: " + first);
            // drain all remaining lines that the server already sent
            while (reader.ready()) {
                System.out.println("Response from server: " + reader.readLine());
            }

            // MAIN LOOP
            while (true) {

                System.out.print("> ");
                String userInput = scanner.nextLine();

                if (userInput == null ||
                        userInput.equalsIgnoreCase("quit") ||
                        userInput.equalsIgnoreCase("exit")) {
                    writer.println("QUIT");
                    break;
                }

                // send command to server
                writer.println(userInput);

                // wait a tiny bit to allow server to send response
                Thread.sleep(10);

                // read ALL available lines from server
                boolean gotSomething = false;
                while (reader.ready()) {
                    String line = reader.readLine();
                    System.out.println("Response from server: " + line);
                    gotSomething = true;
                }

                // if server sent nothing (should not happen), warn
                if (!gotSomething) {
                    System.out.println("(No response received)");
                }
            }

            clientSocket.close();
            System.out.println("Connection closed.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
