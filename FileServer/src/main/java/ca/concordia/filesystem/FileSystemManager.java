package ca.concordia.filesystem;

import java.io.RandomAccessFile;
import java.util.concurrent.locks.ReentrantLock;

import ca.concordia.filesystem.datastructures.FEntry;

public class FileSystemManager {

    private final int MAXFILES = 5;
    private final int MAXBLOCKS = 10;
    private static FileSystemManager instance = null;
    private final RandomAccessFile disk;
    private final ReentrantLock globalLock = new ReentrantLock();

    private static final int BLOCK_SIZE = 128; // Example block size

    private FEntry[] inodeTable; // Array of inodes
    private boolean[] freeBlockList; // Bitmap for free blocks

    public FileSystemManager(String filename, int totalSize) {
        // Initialize the file system manager with a file
        if(instance == null) {
            //TODO Initialize the file system

            // We Create a Disk file which will be managed by Filesystem
            try {
                this.disk = new RandomAccessFile(filename, "rw");
                inodeTable = new FEntry[MAXFILES];
                freeBlockList = new boolean[MAXBLOCKS];
            } catch (Exception e) {
                throw new RuntimeException("Unable to open disk file");
            }
            instance = this;
        } else {
            throw new IllegalStateException("FileSystemManager is already initialized.");
        }

    }

    //CREATE FILE
    public void createFile(String fileName) throws Exception {
        try{

            //Check if file name exists
            for (int i=0; i<inodeTable.length; i++){
                FEntry entry = inodeTable[i];
                if (entry != null && entry.getFilename().equals(fileName)){
                    throw new Exception("FileName already exists. Try again.");
                    // return; //Stops here, to prevent duplicates of the existing file
                }
            }

            //Check the first available fentry
            int availableSpace = -1;
            for (int i=0; i<inodeTable.length; i++){
                if (inodeTable[i] == null){
                    availableSpace =i;
                    break;
                }
            }

            if (availableSpace ==-1){
                throw new Exception("No free file entries available.");
                // return;
            }

            // Check if free/occupied nodes
            short freeBlock=0;
            while (freeBlock < freeBlockList.length && freeBlockList[freeBlock]){
                freeBlock++;
            }
            if(freeBlock == freeBlockList.length){
                throw new Exception("No free blocks available.");

            }
            freeBlockList[freeBlock]= true;

            //Create the file
            FEntry newFile = new FEntry (fileName, (short)0, freeBlock);
            inodeTable[availableSpace] = newFile; //Store the new file

        }catch(IllegalArgumentException e){
            throw new Exception ("Unable to process request. Try again.");
        }

    }

    // TODO: Add readFile, writeFile and other required methods,

    public void writeFile(String fileName) throws Exception {

        try {
            checkFile(fileName);
            
        } catch (Exception e) {
            throw new Exception (e.getMessage());
        }
        
    }

    public void checkFile(String fileName) throws Exception {
        boolean fileExist = false;

        //Check if file exists
        for (int i=0; i<inodeTable.length; i++){
            FEntry entry = inodeTable[i];
            if (entry != null && entry.getFilename().equals(fileName)){
                fileExist = true;
            }
        }

        if(fileExist == false) {
            throw new Exception(fileName + " does not exist");
        }
    }
}
