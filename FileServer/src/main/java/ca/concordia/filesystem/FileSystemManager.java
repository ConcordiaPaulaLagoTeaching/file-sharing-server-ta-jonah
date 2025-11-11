
package ca.concordia.filesystem;

import ca.concordia.filesystem.datastructures.FEntry;
import ca.concordia.filesystem.datastructures.FNode; // needed for creating and managing blocks

import java.io.File; // needed for checking if a file exists
import java.io.RandomAccessFile;
<<<<<<< HEAD
import java.util.concurrent.locks.ReentrantLock;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.io.IOException;
=======
import java.util.concurrent.locks.ReentrantReadWriteLock; // needed for different read/write locks, better concurrency
>>>>>>> c45f706e8eba40c13324caed3c19b545fc408857

public class FileSystemManager {

    private final int MAXFILES = 5;
    private final int MAXBLOCKS = 10;
<<<<<<< HEAD
    private static FileSystemManager instance = null;
=======
    // singleton instance no longer needed, this implementation is better for multithreading

>>>>>>> c45f706e8eba40c13324caed3c19b545fc408857
    private final RandomAccessFile disk;
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock(); // for concurent reads and exclusive writes

    private static final int BLOCK_SIZE = 128; // Example block size

<<<<<<< HEAD
    private final FEntry[] inodeTable; // Array of inodes
    private final boolean[] freeBlockList; // Bitmap for free blocks

    public FileSystemManager(String filename, int totalSize) throws IOException {
        if (instance != null) {
            throw new IllegalStateException("FileSystemManager is already initialized.");
        }

        instance = this;
        this.disk = new RandomAccessFile(filename, "rw");
        this.inodeTable = new FEntry[MAXFILES];
        this.freeBlockList = new boolean[MAXBLOCKS];
       
        // initialize file system
        initializeFileSystem(totalSize);
    }

    // initialize the file system structures 
    private void initializeFileSystem(int totalSize) throws IOException {
        // mark all blocks as free
        for (int i = 0; i < MAXBLOCKS; i++) {
            freeBlockList[i] = true;
        }

        // initialize file entries
        for (int i = 0; i < MAXFILES; i++) {
            inodeTable[i] = new FEntry();
        }

        // Set the file size
        if (disk.length() < totalSize) {
            disk.setLength(totalSize);
        }
    }

    // create a singleton empty file system
=======
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
>>>>>>> c45f706e8eba40c13324caed3c19b545fc408857
    public void createFile(String fileName) throws Exception {
        globalLock.lock();
        try {
            if (fileName.length() > 11) {
                throw new IllegalArgumentException("Filename cannot be longer than 11 characters.");
            }
            // Check for no duplicates
            for (FEntry entry : inodeTable) {
                if (entry.isInUse() && entry.getFilename().equals(fileName)) {
                    throw new Exception("File already exists.");
                }
            }

            // Find a free FEntry
            FEntry freeEntry = null;
            for (FEntry entry : inodeTable) {
                if (!entry.isInUse()) {
                    freeEntry = entry;
                    break;
                }
            }

            // initialize the new file 
            if (freeEntry != null) { //Unnecessary null check but just in case
                freeEntry.setFilename(fileName);
                freeEntry.setFilesize((short) 0);
                freeEntry.setFirstBlock((short) -1); // No blocks assigned yet
                freeEntry.setInUse(true);
            }
            } finally {
                globalLock.unlock();
            }
        }

        // Deletes existing file by overwriting data with zeros
    public void deleteFile(String fileName) throws Exception {
        globalLock.lock();
        try {
            FEntry targetEntry = null;
            for (FEntry entry : inodeTable) {
                if (entry.isInUse() && entry.getFilename().equals(fileName)) {
                    targetEntry = entry;
                    break;
                }
            }

            if (targetEntry == null) {
                throw new IllegalArgumentException("File " + fileName + " not found.");
            }

            // Free up blocks
            int blockIndex = targetEntry.getFirstBlock();
            if (blockIndex >= 0 && blockIndex < MAXBLOCKS) {
                try {
                    zeroOutBlock(blockIndex); // this throws IOException
                } catch (IOException e) { // handle it
                    throw new RuntimeException("Failed to zero out block " + blockIndex, e);
                }
                freeBlockList[blockIndex] = true;
        }

            // reset data
            targetEntry.clear();

        } finally {
            globalLock.unlock();
        }
    }

    // List all files in use
    public String[] listFiles() {
        globalLock.lock();
        try {
            List<String> fileList = new ArrayList<>();
            for (FEntry entry : inodeTable) {
                if (entry.isInUse()) {
                    fileList.add(entry.getFilename());
                }
            }
            return fileList.toArray(new String[0]);
        } finally {
            globalLock.unlock();
        }
    }

    // Overwrite block with zeros
    private void zeroOutBlock(int blockIndex) throws IOException {
        byte[] zeros = new byte[BLOCK_SIZE];
        long offset = (long) blockIndex * BLOCK_SIZE;
        disk.seek(offset);
        disk.write(zeros);
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


}

    // TODO: Add the following methods:

    //writeFile(String filename, byte[] data) replaces contents of a file; must be atomic (no partial writes!)
    //readFile(String filename) returns data stored inside a file
<<<<<<< HEAD
=======
    //listFiles() returns names of all existing files
}
>>>>>>> c45f706e8eba40c13324caed3c19b545fc408857
