package ca.concordia.filesystem;

import ca.concordia.filesystem.datastructures.FEntry;
import ca.concordia.filesystem.datastructures.FNode;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Collections;
import java.util.List;

public class FileSystemManager {

    private final int MAXFILES = 5;
    private int MAXBLOCKS = 10;
    private final static FileSystemManager instance = null;
    private RandomAccessFile disk = null;
    private final ReentrantLock globalLock = new ReentrantLock();
    public final int FILEISEMPTY = -5, FILENAMETOOLONG = -4, FILESPACEFULL = -3, DISKFULL = -2, FILENOTFOUND = -1, SUCCESS = 0 ;

    private static final int BLOCK_SIZE = 128; // Example block size

    private FEntry[] inodeTable = new FEntry[MAXFILES]; // Array of inodes
    private boolean[] freeBlockList = new boolean[MAXBLOCKS]; // Bitmap for free blocks
    private FNode[] blockAddrTable = new FNode[MAXBLOCKS];

    private final LockManager lockManager = new LockManager();
    public FileSystemManager(String filename, int totalSize) throws IOException {
        // Initialize the file system manager with a file
        if (instance == null) {
            //TODO Initialize the file system
            Arrays.fill(freeBlockList, true);
            for (int i = 0; i < MAXBLOCKS; i++) {
                blockAddrTable[i] = new FNode(-i);
            }

            disk = new RandomAccessFile(filename, "rw");



            for (int i = 0; i < MAXFILES * 16; i++) {
                disk.writeBytes("~");
            }
            disk.writeBytes("\n");
            for (int i = 0; i < MAXBLOCKS; i++) {
                if(freeBlockList[i])
                    disk.writeBytes(blockAddrTable[i].getBlockIndex() + "-1");
            }
            disk.writeBytes("\n");
            for (int i = 0; i < MAXBLOCKS; i++) {
                for (int j = 0; j < BLOCK_SIZE; j++) {
                    if(freeBlockList[i])
                        disk.writeBytes("~");
                }
                disk.writeBytes("\n");
            }
            disk.writeBytes("EOF\n");
//            MAXBLOCKS = totalSize / BLOCK_SIZE;
        } else {
            throw new IllegalStateException("FileSystemManager is already initialized.");
        }
    }

    private int locateFreeBlock() {
        for (int i = 0; i < MAXBLOCKS; i++) {
            if (freeBlockList[i]) {
                return i;
            }
        }
        return -1;
    }
    private int locateFreeFile(){
        for (int j = 0; j < MAXFILES; j++) {
            if(j == MAXFILES - 1 && inodeTable[j] != null) { return -2; }
            if (inodeTable[j] == null) {
                return j;
            }
        }
        return -1;
    }
    private int fileExists(String filename) {
        for (int i = 0; i < MAXFILES; i++) {
            if (inodeTable[i] != null && inodeTable[i].getFilename().equals(filename)) {
                return i;
            }
        }
        return -1;
    }

    // Building lock handles for single-file commands
    private List<LockManager.LockHandle> locksForFileOp(int inodeIndex) {
        List<LockManager.LockHandle> locks = new ArrayList<>();
        // GLOBAL rank 0
        locks.add(new LockManager.LockHandle(LockLevel.GLOBAL.getRank(), -1, lockManager.getGlobalLock()));
        // FILE rank 1
        locks.add(new LockManager.LockHandle(LockLevel.FILE.getRank(), inodeIndex, lockManager.fileLock(inodeIndex)));
        return locks;
    }

    public int createFile(String fileName) throws Exception {
        List<LockManager.LockHandle> globalOnly = Collections.singletonList(new LockManager.LockHandle(LockLevel.GLOBAL.getRank(), -1, lockManager.getGlobalLock()));
        lockManager.acquireOrdered(globalOnly);
        try {
            System.out.println("Creating new file: " + fileName + "....");
            int freeFile = locateFreeFile();
            if (freeFile == -1) {
                System.err.println("No file was found named as : " + fileName);
                return FILENOTFOUND;
            } else if (freeFile == -2) {
                System.err.println("Disk is full for file: " + fileName + " to be added to memory");
                return FILESPACEFULL;
            }
            int freeBlockAddr = locateFreeBlock();
            if (freeBlockAddr == -1) {
                System.err.println("No available blocks to allocate for : " + fileName);
                return DISKFULL;
            }
            List<LockManager.LockHandle> locks = new ArrayList<>();
            locks.add(new LockManager.LockHandle(LockLevel.GLOBAL.getRank(), -1, lockManager.getGlobalLock()));
            locks.add(new LockManager.LockHandle(LockLevel.FILE.getRank(), freeFile, lockManager.fileLock(freeFile)));

            lockManager.acquireOrdered(locks);
            try {
                if (inodeTable[freeFile] != null) {
                    System.err.println("Race condition -> inode already in use: " + freeFile);
                    return FILENOTFOUND;
                }
            blockAddrTable[freeBlockAddr] = new FNode(freeBlockAddr + 1);

            disk.seek(freeFile * 16L + 14);
            disk.writeBytes(String.valueOf(blockAddrTable[freeBlockAddr].getBlockIndex()));
            System.out.println("Wrote into the file!: " + fileName);
            freeBlockList[freeBlockAddr] = false;

            disk.seek(MAXFILES * 16 + (blockAddrTable[freeBlockAddr].getBlockIndex()) * 4L);
            disk.writeBytes("~");

            try {
                inodeTable[freeFile] = new FEntry(fileName, (short) 0, (short) (freeBlockAddr + 1));
            } catch (IllegalArgumentException e) {
                System.err.println("Invalid filename length: " + fileName);
                return FILENAMETOOLONG;
            }
            System.out.println(inodeTable[freeFile].getFilename());
            disk.seek(freeFile * 16L);
            disk.writeBytes(inodeTable[freeFile].getFilename());
            disk.seek(freeFile * 16L + 11);
//            byte filesize = (byte) inodeTable[freeFile].getFilesize();
            disk.writeBytes(String.valueOf(inodeTable[freeFile].getFilesize()));
            if (freeFile == MAXFILES - 1 && inodeTable[freeFile] != null) {
                System.out.println("No more space to write: " + fileName);
                return DISKFULL;
            }
            System.out.println("New file: " + fileName + " created");
        } finally {
                lockManager.releaseOrdered(locks);
            }
        } finally {
            try {
                if (lockManager.getGlobalLock().isHeldByCurrentThread()) lockManager.getGlobalLock().unlock();
            } catch (Exception ignored) {}
        }
        return SUCCESS;
    }

    public String readFile(String fileName) throws Exception {
//        globalLock.lock();
        List<LockManager.LockHandle> locks = locksForFileOp(existingFile);
        lockManager.acquireOrdered(locks);
        try {
            StringBuilder readString = new StringBuilder();
            System.out.println("Reading from file: " + fileName);
            int existingFile = fileExists(fileName);
            if (existingFile == -1) {
                System.err.println("File not found: " + fileName);
                return String.valueOf(FILENOTFOUND);
            }
            String line;
            int first = inodeTable[existingFile].getFirstBlock() - 1;
            do {
                disk.seek(MAXFILES * 16 + MAXBLOCKS * 4L + (long) (first) * (BLOCK_SIZE + 1) + 1);
                line = disk.readLine() != null ? disk.readLine().replace("~", "") : null;
                readString.append(line);
                first = blockAddrTable[first].getNext();
            } while (first >= 0);
            System.out.println("File: " + fileName + " was read!");
            System.out.println(readString);
            return readString.toString();
        } finally {
            lockManager.releaseOrdered(locks);
        }
    }

    public int writeFile(String fileName, String text) throws Exception {
        List<LockManager.LockHandle> locks = locksForFileOp(existingFile);
        lockManager.acquireOrdered(locks);
        try {
            int existingFile = fileExists(fileName);
            if (existingFile == -1) {
                System.err.println("File not found: " + fileName);
                return FILENOTFOUND;
            }

            List<LockManager.LockHandle> locks = locksForFileOp(existingFile);
            lockManager.acquireOrdered(locks);
            disk.seek(existingFile * 16L + 11);
            disk.writeBytes(String.valueOf(text.length()));
            disk.seek(MAXFILES * 16 + MAXBLOCKS * 4L + (inodeTable[existingFile].getFirstBlock() - 1) * (BLOCK_SIZE + 1) + 1);
            if (text.length() <= BLOCK_SIZE) {
                disk.writeBytes(text);
            } else {
                int pos = 0, first = inodeTable[existingFile].getFirstBlock() - 1;
                boolean lastline = false;
                for (int n = 1; pos < text.length(); n++) {
                    int freeBlockAddr = locateFreeBlock();
                    if (freeBlockAddr == -1) {
                        System.err.println("No more blocks found for : " + fileName);
                        return DISKFULL;
                    }
                    for (int l = pos; l < BLOCK_SIZE * n && l < text.length(); l++) {
                        disk.write(text.charAt(l));
                        if (l > BLOCK_SIZE * ((int) text.length() / BLOCK_SIZE)) {
                            lastline = true;
                        }
                    }
                    pos += BLOCK_SIZE;

                    disk.seek(MAXFILES * 16 + 4 + (first) * 4L);
                    disk.writeBytes("~");
                    if (!lastline) {
                        freeBlockList[freeBlockAddr] = false;
                        disk.seek(MAXFILES * 16 + 4 + (first) * 4L + 2);
                        disk.writeBytes("*" + String.valueOf(freeBlockAddr + 1));
                        blockAddrTable[first].setNext(freeBlockAddr);
                        first = freeBlockAddr;
                        disk.seek(MAXFILES * 16 + MAXBLOCKS * 4L + (long) (freeBlockAddr) * (BLOCK_SIZE + 1) + 1);
                    }
                }
            }
            System.out.println("File: " + fileName + " was modified!");
        } finally {
            lockManager.releaseOrdered(locks);
        }
        return SUCCESS;
    }

    public String list() throws Exception {
        List<LockManager.LockHandle> globalLock = Collections.singletonList(new LockManager.LockHandle(LockLevel.GLOBAL.getRank(), -1, lockManager.getGlobalLock()));

        lockManager.acquireOrdered(globalLock);
        try {
            String listFiles = "";
            System.out.println("Listing files: ");
            int fileIndex = 0;
            for (int i = 0; i < MAXFILES; i++) {
                if (inodeTable[i] == null) {
                    continue;
                }
                listFiles += "    " + fileIndex++ + " - " + inodeTable[i].getFilename();
//            System.out.println("    " + i + " - " + inodeTable[i].getFilename());
            }
            System.out.println("Done Listing files: ");
            return listFiles;
        }  finally {
            lockManager.releaseOrdered(globalLock);
        }
    }

    public void deleteFile(String fileName) throws Exception {
        try {
            int existingFile = fileExists(fileName);
            if (existingFile == -1) {
                System.err.println("File not found: " + fileName);
                return;
            }
            disk.seek(existingFile * 16L);
            disk.writeBytes("~~~~~~~~~~~~~~~");
            disk.seek(MAXFILES * 16 + MAXBLOCKS * 4L + (inodeTable[existingFile].getFirstBlock() - 1) * (BLOCK_SIZE + 1) + 1);

            int pos = 0, first = inodeTable[existingFile].getFirstBlock() - 1;
            for (int p = 0; p < MAXBLOCKS; p++) {
                disk.seek(MAXFILES * 16 + MAXBLOCKS * 4L + (long) (first) * (BLOCK_SIZE + 1) + 1);
                for (int n = 0; n < BLOCK_SIZE; n++) {
                    disk.writeBytes("~");
                }
                freeBlockList[first] = true;
                disk.seek(MAXFILES * 16 + 4 + first * 4L);
                disk.writeBytes("-");
                disk.seek(MAXFILES * 16 + 4 + first * 4L + 2);
                disk.writeBytes("-1");
                first = blockAddrTable[first].getNext();
                if (first < 0) break;
            }
            System.out.println("File: " + fileName + " was deleted!");
            inodeTable[existingFile] = null;
        } finally {
            lockManager.releaseOrdered(locks);
        }
    }  // TODO: Add readFile, writeFile and other required methods      DONE!(partially(~90%))
}

