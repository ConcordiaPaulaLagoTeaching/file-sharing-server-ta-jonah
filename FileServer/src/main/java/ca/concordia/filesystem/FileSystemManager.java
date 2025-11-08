package ca.concordia.filesystem;

import ca.concordia.filesystem.datastructures.FEntry;

import java.io.RandomAccessFile;
import java.util.concurrent.locks.ReentrantLock;

public class FileSystemManager {

    private final int MAXFILES = 5;
    private final int MAXBLOCKS = 10;
    private final static FileSystemManager instance;
    private final RandomAccessFile disk;
    private final ReentrantLock globalLock = new ReentrantLock();

    private static final int BLOCK_SIZE = 128; // Example block size

    private FEntry[] inodeTable; // Array of inodes
    private boolean[] freeBlockList; // Bitmap for free blocks

    public FileSystemManager(String filename, int totalSize) {
        // Initialize the file system manager with a file
        if(instance == null) {
            //TODO Initialize the file system: >Progress below
            this.disk = new RandomAccessFile(diskfile, "rw");   //initialize 'virtual disk' for managing files
            this.inodeTable = new FEntry[MAXFILES]; //initialize array of file entries
            this.freeBlockList = new boolean[MAXBLOCKS]; //initializes array of free block (all blocks start free)
            Arrays.fill(freeBlockList, true);

            instance = this;

        } else {
            throw new IllegalStateException("FileSystemManager is already initialized.");
        }

    }

    public void createFile(String fileName) throws Exception {
        // TODO
        //throw new UnsupportedOperationException("Method not implemented yet.");
        //FEntry newFile = new FEntry(fileName, (short) 0, (short) -1);

    }


    // TODO: Add readFile, writeFile and other required methods,
}
