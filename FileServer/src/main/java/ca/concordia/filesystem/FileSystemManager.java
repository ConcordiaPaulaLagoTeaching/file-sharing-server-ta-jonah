package ca.concordia.filesystem;

import ca.concordia.filesystem.datastructures.FEntry;

import java.io.RandomAccessFile;
import java.util.concurrent.locks.ReentrantLock;

public class FileSystemManager {

    private final int MAXFILES = 5;
    private final int MAXBLOCKS = 10;
    private/*final*/  static FileSystemManager instance;
    private /*final*/ RandomAccessFile disk;
    private final ReentrantLock globalLock = new ReentrantLock();

    private static final int BLOCK_SIZE = 128; // Example block size

    private FEntry[] inodeTable; // Array of inodes
    private boolean[] freeBlockList; // Bitmap for free blocks

    public FileSystemManager(String filename, int totalSize) {
        // Initialize the file system manager with a file
        if(instance == null) {
            //TODO Initialize the file system

        } else {
            throw new IllegalStateException("FileSystemManager is already initialized.");
        }

    }

    private void initializeFileSystem(String filename, int totalSize){
        try{
            inodeTable = new FEntry[MAXFILES];
            freeBlockList= new boolean[MAXBLOCKS];
            disk = new RandomAccessFile(filename,"rw");
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    public void createFile(String fileName) throws Exception {
        // TODO
        if(fileName.length()>11)
            throw new Exception("ERROR: filename too large");
        if (fileName.length()==0)
            throw new Exception("ERROR: filename cannot be empty");

        for(FEntry fentry : inodeTable){
            if( fentry!=null && fileName.equals(fentry.getFilename()))
                throw new Exception("ERROR: filename already exists");
        }

        //find free FEntry slot
        int fentryIndex = -1;
        for(int i =0; i<MAXFILES;i++){
            if(inodeTable[i] == null || inodeTable[i].getFilename()==null ){
                fentryIndex=i;
                break;
            }
        }

        if (fentryIndex == -1)
            throw new Exception("ERROR: no free entries left.");

        FEntry newFEntry = new FEntry(fileName,(short)0,(short)-1); //add our empty file
        inodeTable[fentryIndex]=newFEntry;

        throw new UnsupportedOperationException("Method not implemented yet.");
    }


    // TODO: Add readFile, writeFile and other required methods,
}
