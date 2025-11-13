package ca.concordia;

import ca.concordia.server.FileServer;
import ca.concordia.filesystem.FileSystemManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

public class Main {

    public static void main(String[] args) throws Exception {
        System.out.printf("Hello and welcome!");

        FileServer server = new FileServer(12345, "filesystem.dat", 10 * 128);
        server.start();

        // Start the file server
        // totalSize = metadataSize + (MAXBLOCKS × BLOCKSIZE)

        String file_entry = args[0]; // command user enter
        int totalsize = Integer.parseInt(args[1]);

        try{
            FileSystemManager fsm = new FileSystemManager(file_entry, totalsize);

            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            while(true){
                System.out.println("ENTER COMMANDS >>");
                String line = br.readLine();
                if(line == null){
                    System.out.println("COMMAND NOT DETECTED.");
                    break;
                }
                else{
                    String[] parts = line.split("\\s+",3);
                    String cmd = parts[0].toUpperCase();


                    System.out.print("Please select 1 of the following: \n");
                    System.out.print("CREATE <file> \n");
                    System.out.print("WRITE <file> <...> \n");
                    System.out.print("READ <files> \n");
                    System.out.print("DELETE <file> \n");
                    System.out.print("LIST  \n");

                    try{
                        switch (cmd){
                            case "CREATE": {
                                String filename = parts[1];
                                if(!isNameValid(filename)){
                                    break;
                                }
                                fsm.createFile(filename);
                                System.out.println("File created successfully");
                                break;
                            }
                            case "WRITE": {
                                String filename = parts[1];
                                if(!isNameValid(filename)){
                                    break;
                                }
                                String content = parts[2];
                                byte[] data = content.getBytes();
                                fsm.writeFile(filename,data);
                                System.out.println("Wrote " + data.length + " bytes");
                            }
                            case "READ": {
                                String filename = parts[1];
                                if(!isNameValid(filename)){
                                    break;
                                }
                                byte[] data = fsm.readFile(filename);
                                String filetext = new String(data);
                                System.out.println("File Contents: " + filetext);
                            }
                            case "DELETE": {
                                String filename = parts[1];
                                if(!isNameValid(filename)){
                                    break;
                                }
                                fsm.deleteFile(filename);
                                System.out.println("File " + filename + " successfully deleted");
                            }
                            case "LIST": {
                                List<String> names = Arrays.asList(fsm.listFiles());
                            }
                            }
                    } catch (Exception e) {
                        System.out.println("Incorrect command entered");
                        throw new RuntimeException(e);
                    }
                }

            }
        } catch (Exception e) { // exception handling
            throw new RuntimeException(e);
        }
    }

    // Modularity
    private static boolean isNameValid(String name){
        boolean flag = true;
        if(name.length() > 11){
            System.out.println("Name of file should be less than 11 characters");
             flag = false;
        }
        return flag;
    }
}