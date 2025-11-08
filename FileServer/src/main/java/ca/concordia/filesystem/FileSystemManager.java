
package ca.concordia.filesystem;

import ca.concordia.filesystem.datastructures.FEntry;
import ca.concordia.filesystem.datastructures.FNode; // needed for creating and managing blocks

import java.io.File; // needed for checking if a file exists
import java.io.RandomAccessFile;
import java.util.concurrent.locks.ReentrantReadWriteLock; // needed for different read/write locks, better concurrency

public class FileSystemManager {

    private final int MAXFILES = 5;
    private final int MAXBLOCKS = 10;
    // singleton instance no longer needed, this implementation is better for multithreading

    private final RandomAccessFile disk;
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock(); // for concurent reads and exclusive writes

    private static final int BLOCK_SIZE = 128; // Example block size

    private FEntry[] inodeTable; // Array of inodes
    private FNode[] fnodeTable; // array of data blocks, for storage
    private boolean[] freeBlockList; // Bitmap for free blocks

    private int firstDataBlock; // for tracking where data blocks start

    public FileSystemManager(String filename, int totalSize) {
        // Initialize the file system manager with a file
        try{
            // first: calculate metadata size and keep track of where data blocks start
            int fnodeBytes = MAXBLOCKS * 8;
            int fentryBytes = MAXFILES * 15;
            int metadataBytes = fentryBytes + fnodeBytes;
            this.firstDataBlock = (metadataBytes + BLOCK_SIZE - 1) / BLOCK_SIZE; // calculate index of first data block

            // next: open or create disk file
            File diskFile = new File(filename); // the disk file

            if (diskFile.exists()) {
                disk = new RandomAccessFile(diskFile, "rw"); // if file exists call function to populate memory
                initializeFileSystem();
            } else {
                disk = new RandomAccessFile(diskFile, "rw"); // create new file
                disk.setLength(totalSize); // set the size of the disk file
                initializeFileSystem(); // initialize file system
            }
        }
            catch (Exception e) {
                throw new RuntimeException("Failed to initialize file system", e);
            }
    }
    
    private void initializeFileSystem() throws Exception {
        // initialize inode table, fnode table and free block list
        inodeTable = new FEntry[MAXFILES];
        fnodeTable = new FNode[MAXBLOCKS];
        freeBlockList = new boolean[MAXBLOCKS];

        // for each index, create a Fnode object (Fnodes represent disk block) 
        for (int i = 0; i < MAXBLOCKS; i++) {
            fnodeTable[i] = new FNode(i);
        }

        // reserve metadata blocks to preserve then from being allocated to files
        for (int i = 0; i < firstDataBlock; i++) {
            freeBlockList[i] = true; 
        }

        // all blocks after the metadata blocks are free to be used for file data, so mark them as free
        for (int i = firstDataBlock; i < MAXBLOCKS; i++) {
            freeBlockList[i] = false;
        }

        // print initialization complete
        System.out.println("File system initialized. Metadata size: " + (MAXFILES * 15 + MAXBLOCKS * 8) + " bytes.");      
    }
    //this function creates an empty file if possible
    public void createFile(String fileName) throws Exception {
        // TODO
        throw new UnsupportedOperationException("Method not implemented yet.");
    }
    public void deleteFile(String fileName) throws Exception {
        // TODO
        throw new UnsupportedOperationException("Method not implemented yet.");
    }

    public void writeFile(String fileName) throws Exception {
        // TODO
        throw new UnsupportedOperationException("Method not implemented yet.");
    }
    public byte[] readFile(String fileName) throws Exception {
        // TODO
        throw new UnsupportedOperationException("Method not implemented yet.");
    }
    public String[] listFiles() throws Exception {
        // TODO
        throw new UnsupportedOperationException("Method not implemented yet.");
    }

    // TODO: Add readFile, writeFile and other required methods,

    //deleteFile(String fileName) removes file and zeroes out its blocks
    //writeFile(String filename, byte[] data) replaces contents of a file; must be atomic (no partial writes!)
    //readFile(String filename) returns data stored inside a file
    //listFiles() returns names of all existing files
}