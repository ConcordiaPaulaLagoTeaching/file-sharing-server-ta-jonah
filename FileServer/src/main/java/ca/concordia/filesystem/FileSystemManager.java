
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
            //TODO Initialize the file system
        } else {
            throw new IllegalStateException("FileSystemManager is already initialized.");
        }

    }

    //this function creates an empty file if possible
    public void createFile(String fileName) throws Exception {
        // TODO
        throw new UnsupportedOperationException("Method not implemented yet.");
    }


    // TODO: Add readFile, writeFile and other required methods,

    //deleteFile(String fileName) removes file and zeroes out its blocks
    //writeFile(String filename, byte[] data) replaces contents of a file; must be atomic (no partial writes!)
    //readFile(String filename) returns data stored inside a file
    //listFiles() returns names of all existing files
}
