package ca.concordia;

import java.util.Scanner;

public class MultiClientMain {
    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);

        System.out.println("\nMULTIPLE THREADING TEST\n");

        System.out.print("How many threads do you want to launch? ");
        int count = sc.nextInt();

        System.out.println("Choose a mode:");
        System.out.println("1 — CREATE");
        System.out.println("2 — WRITE");
        System.out.println("3 — READ");
        System.out.println("4 — DELETE");
        System.out.println("5 — LIST");
        System.out.print("Enter mode: ");
        int mode = sc.nextInt();

        System.out.println("\nLaunching " + count + " clients...\n");

        for (int i = 0; i < count; i++) {
            Thread t = new Thread(new ClientRunnable(i, mode));
            t.start();
            System.out.println("[Main] Started client " + i + " on thread " + t.getId());
        }

       sc.close();
    }
}
