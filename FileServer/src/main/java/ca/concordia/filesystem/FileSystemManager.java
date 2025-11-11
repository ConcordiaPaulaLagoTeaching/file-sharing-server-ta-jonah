package ca.concordia.filesystem;

import java.io.RandomAccessFile;
import java.util.concurrent.locks.ReentrantLock;

import ca.concordia.filesystem.datastructures.FEntry;

public class FileSystemManager {

    private final int MAXFILES = 5;
    private final int MAXBLOCKS = 10;
    private static FileSystemManager instance;
    private final RandomAccessFile disk;
    private final ReentrantLock globalLock = new ReentrantLock();

    private static final int BLOCK_SIZE = 128; // Example block size

    private FEntry[] inodeTable; // Array of inodes
    private boolean[] freeBlockList; // Bitmap for free blocks

    // constructor
    public FileSystemManager(String filename, int totalSize) {
        // Initialize the file system manager with a file
        if (instance == null) {
            instance = this;
            inodeTable = new FEntry[MAXFILES];
            for (int i = 0; i < MAXFILES; i++) {
                inodeTable[i] = new FEntry("", (short) 0, (short) -1);
            }
            freeBlockList = new boolean[MAXBLOCKS];
            for (int i = 0; i < MAXBLOCKS; i++) {
                freeBlockList[i] = true;
            }
            try {
                disk = new RandomAccessFile(filename, "rw");
                disk.setLength(totalSize);
            } catch (Exception e) {
                throw new RuntimeException("Unable to create virtual disk file", e);
            }
        } else {
            throw new IllegalStateException("FileSystemManager is already initialized.");
        }

    }

    // create file
    public void createFile(String fileName) throws Exception {
        // TODO
        throw new UnsupportedOperationException("Method not implemented yet.");
    }

    // TODO: Add readFile, writeFile and other required methods,
}
