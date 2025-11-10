package ca.concordia;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
 
// Press Shift twice to open the Search Everywhere dialog and type `show whitespaces`,
// then press Enter. You can now see whitespace characters in your code.
public class Main {
    public static void main(String[] args) {
        //Socket CLient
        ClientRunnable[] clientRunnable = new ClientRunnable[1];
        for(int i = 0; i < clientRunnable.length; i++) {
            clientRunnable[i] = new ClientRunnable();
            new Thread(clientRunnable[i]).start();
        }
    }
}