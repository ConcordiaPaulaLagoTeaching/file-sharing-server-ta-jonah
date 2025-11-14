package ca.concordia;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        //Socket CLient
        ClientRunnable[] clientRunnable = new ClientRunnable[4];
        for(int i = 0; i < clientRunnable.length; i++) {
            clientRunnable[i] = new ClientRunnable();
            new Thread(clientRunnable[i]).start();
            System.out.println("[" + Thread.currentThread().getId() + "] Client " + i + " started");
        }
    }
}