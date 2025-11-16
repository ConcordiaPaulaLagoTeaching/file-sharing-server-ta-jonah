package ca.concordia.filesystem;

import ca.concordia.filesystem.datastructures.FEntry;
import ca.concordia.filesystem.datastructures.FNode;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Collections;
import java.util.List;

public class FileSystemManager {

    public int MAXFILES = 10;
    private int MAXBLOCKS = 30;
    private final int nodeChars = ((MAXBLOCKS / MAXBLOCKS) + 1), nextNodeChars = ((MAXBLOCKS / MAXBLOCKS) + 2);
    private final int totalChars = nodeChars + nextNodeChars;
    private static FileSystemManager instance;
    private final RandomAccessFile disk;
    private final ReentrantLock globalLock = new ReentrantLock();
    public final int FILEALREADYEXISTS = -6, FILEISEMPTY = -5, FILENAMETOOLONG = -4, FILESPACEFULL = -3, DISKFULL = -2, FILENOTFOUND = -1, SUCCESS = 0 ;

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
            parseDisk();
            parseNodes();
            disk.seek((long) 16 * MAXFILES + 1);
            for (int i = 0; i < MAXBLOCKS; i++) {
                disk.seek(16L * MAXFILES + 1 + (long) (i) * totalChars);
                int firstBlock = blockAddrTable[i].getBlockIndex();
                String firstString;
                if(firstBlock <= 0) {
                    firstString = String.valueOf(firstBlock);
                }
                else{
                    firstString = "~" + firstBlock;
                }
                disk.writeBytes(firstString);
                int nextBlock = blockAddrTable[i].getNext();
                String nextString;
                if(nextBlock < 0) {
                    nextString = String.valueOf(nextBlock);
                }
                else{
                    nextString = "*" + nextBlock;
                }
                disk.writeBytes(nextString);
            }
            disk.seek(16L * MAXFILES + 1 + (long) totalChars * MAXBLOCKS);
            disk.writeBytes("\n");
            parseBlocks();

//            MAXBLOCKS = totalSize / BLOCK_SIZE;
        } else {
            throw new IllegalStateException("FileSystemManager is already initialized.");
        }
    }

    private void parseDisk() throws FileNotFoundException, IOException {
        for(int i = 0; i < MAXFILES; i++) {
            disk.seek((long) i * 16);
            byte[] filedata = new byte[16];
            int filedatalength = disk.read(filedata);
            if(filedatalength <= 0) {
                for (int k = 0; k < 16; k++) {
                    disk.writeBytes("~");
                }
                continue;
            }
            String file = new String(filedata, 0, filedatalength, StandardCharsets.UTF_8 );
            String filename, filesize, fileFirstBlock;
            filename = file.substring(0, 10).replace("~", "");
            filesize = file.substring(11, 13).replace("~", "");
            fileFirstBlock = file.substring(14, 15).replace("~", "");
            if(!fileFirstBlock.isEmpty() && !filename.isEmpty() && (isNumeric(filesize) && isNumeric(fileFirstBlock))) {
                inodeTable[i] = new FEntry(filename,
                                (short) Integer.parseInt(filesize),
                                (short) Integer.parseInt(fileFirstBlock));
            }
            else continue;
            int firstBlock = Integer.parseInt(fileFirstBlock);
            freeBlockList[firstBlock] = false;
        }
        disk.writeBytes("\n");
    }
    private void parseNodes() throws FileNotFoundException, IOException {
        disk.seek(16L * MAXFILES + 1);
        String nodesLine = disk.readLine();
        if(nodesLine == null) return;
        for(int i = 0; i < MAXBLOCKS; i++) {
            
            disk.seek(16L * MAXFILES + 1 + (long) (i) * totalChars);
            byte[] nodesDataBytes = new byte[totalChars];
            int nodesDataLength = disk.read(nodesDataBytes);
            if(nodesDataLength <= 0) {
                disk.writeBytes("~" + blockAddrTable[i].getBlockIndex() + blockAddrTable[i].getNext());
                continue;
            }
            String nodesdata = new String(nodesDataBytes, 0, nodesDataLength, StandardCharsets.UTF_8);
            String nodeString = nodesdata.substring(0, nodeChars).replace("~", ""),
                    nextNodeString = nodesdata.substring(nodeChars, nodesdata.length() - 1).replace("*", "");
            System.out.println(nodeString + " : " + nextNodeString);
            if(!isNumeric(nodeString) || !isNumeric(nextNodeString)) continue;
            int firstBlock = Integer.parseInt(nodeString), nextBlock = Integer.parseInt(nextNodeString);
            blockAddrTable[i] = new FNode(firstBlock);
            blockAddrTable[i].setNext(nextBlock);
            if(firstBlock > 0) {
                freeBlockList[firstBlock - 1] = false;
            }
            System.out.println(blockAddrTable[i].getBlockIndex() + " : " + blockAddrTable[i].getNext());
        }
    }

    private void parseBlocks() throws FileNotFoundException, IOException {
        for (int i = 0; i < MAXBLOCKS; i++) {
            disk.seek(16L * MAXFILES + 1 + (long) (MAXBLOCKS) * totalChars + 1 + (long) i * (BLOCK_SIZE + 1));
            if(freeBlockList[i]) {
                for (int j = 0; j < BLOCK_SIZE; j++) {
                    disk.writeBytes("~");
                }
                disk.writeBytes("\n");
            }
        }
    }

    private boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) return false;

        int start = 0;
        if (str.charAt(0) == '-') {
            if (str.length() == 1) return false;
            start = 1;
        }

        for (int i = start; i < str.length(); i++) {
            if (!Character.isDigit(str.charAt(i))) {
                return false;
            }
        }
        return true;
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
        globalLock.lock();
//        List<LockManager.LockHandle> globalOnly = Collections.singletonList(new LockManager.LockHandle(LockLevel.GLOBAL.getRank(), -1, lockManager.getGlobalLock()));
//        lockManager.acquireOrdered(globalOnly);
        try {
            System.out.println("Creating new file: '" + fileName + "'....");
            int freeFile = locateFreeFile();
            if (freeFile == -1) {
                System.err.println("No file was found named as : '" + fileName + "'");
                return FILENOTFOUND;
            } else if (freeFile == -2) {
                System.err.println("Disk is full for file: '" + fileName + "' to be added to memory");
                return FILESPACEFULL;
            }
            else if(fileExists(fileName) != -1) {
                System.err.println("File : '" + fileName + "' already exists");
                return FILEALREADYEXISTS;
            }
            int freeBlockAddr = locateFreeBlock() + 1;
            if (freeBlockAddr == 0 || freeBlockAddr >= MAXBLOCKS) {
                System.err.println("No available blocks to allocate for : " + fileName);
                return DISKFULL;
            }
//            List<LockManager.LockHandle> locks = new ArrayList<>();
//            locks.add(new LockManager.LockHandle(LockLevel.GLOBAL.getRank(), -1, lockManager.getGlobalLock()));
//            locks.add(new LockManager.LockHandle(LockLevel.FILE.getRank(), freeFile, lockManager.fileLock(freeFile)));

//            lockManager.acquireOrdered(locks);
            try {
                if (inodeTable[freeFile] != null) {
                    System.err.println("Race condition -> inode already in use: " + freeFile);
                    return FILENOTFOUND;
                }
            blockAddrTable[freeBlockAddr] = new FNode(freeBlockAddr);

            disk.seek(freeFile * 16L + 14);
            disk.writeBytes(String.valueOf(blockAddrTable[freeBlockAddr].getBlockIndex()));
            System.out.println("Wrote into the file!: " + fileName);
            freeBlockList[freeBlockAddr - 1] = false;

            disk.seek(MAXFILES * 16L + 1 + (long) (blockAddrTable[freeBlockAddr].getBlockIndex()) * totalChars);
            disk.writeBytes("~");

            try {
                inodeTable[freeFile] = new FEntry(fileName, (short) 0, (short) (freeBlockAddr));
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
//                lockManager.releaseOrdered(locks);
            }
        } finally {
            try {
                globalLock.unlock();
//                if (lockManager.getGlobalLock().isHeldByCurrentThread()) lockManager.getGlobalLock().unlock();
            } catch (Exception ignored) {}
        }
        return SUCCESS;
    }

    public String readFile(String fileName) throws Exception {
        int existingFile = fileExists(fileName);
        if (existingFile == -1) {
            System.err.println("File not found: " + fileName);
            return String.valueOf(FILENOTFOUND);
        }
//        List<LockManager.LockHandle> locks = locksForFileOp(existingFile);
//        lockManager.acquireOrdered(locks);
        try {
            StringBuilder readString = new StringBuilder();
            System.out.println("Reading from file: " + fileName);
            String line;
            int first = inodeTable[existingFile].getFirstBlock();
            do {
                System.out.println("First block: " + first);
                disk.seek(MAXFILES * 16L + 1 + (long) MAXBLOCKS * totalChars + 1 + (long) (first - 1) * (BLOCK_SIZE + 1));
                line = disk.readLine().replace("~", "");
//                        != null ? disk.readLine().replace("~", "") : null;
                readString.append(line);
                first = blockAddrTable[first].getNext();
            } while (first > 0);
            System.out.println("File: " + fileName + " was read!");
            System.out.println(readString);
            return readString.toString();
        } finally {
//            lockManager.releaseOrdered(locks);
        }
    }

    public int writeFile(String fileName, String text) throws Exception {
        globalLock.lock();
        int existingFile = fileExists(fileName);
        if (existingFile == -1) {
            System.err.println("File not found: " + fileName);
            return FILENOTFOUND;
        }
//        List<LockManager.LockHandle> locks = locksForFileOp(existingFile);
//        lockManager.acquireOrdered(locks);
        try {
            disk.seek(existingFile * 16L + 11);
            disk.writeBytes(String.valueOf(text.length()));
            disk.seek(MAXFILES * 16L + 1 + (long) MAXBLOCKS * totalChars + 1 + (inodeTable[existingFile].getFirstBlock() - 1) * (BLOCK_SIZE + 1));
            if (text.length() <= BLOCK_SIZE) {
                disk.writeBytes(text);
            } else {
                int pos = 0, first = inodeTable[existingFile].getFirstBlock() - 1;
                boolean lastline = false;
                for (int n = 1; pos < text.length(); n++) {
                    for (int l = pos; l < BLOCK_SIZE * n && l < text.length(); l++) { // From the beginning to the end of each line : n = line index, l = index of character to be transfered, pos beginning of current line
                        disk.write(text.charAt(l));
                        if (l > BLOCK_SIZE * ((int) text.length() / BLOCK_SIZE)) { // if the index of the character to be transfered is higher than the index of the first element on the last line
                            lastline = true;
                        }
                    }
                    pos += BLOCK_SIZE;
                    disk.seek(MAXFILES * 16L + 1 + (long) (first + 1) * totalChars);
                    disk.writeBytes("~");
                    if (!lastline) {
                        int freeBlockAddr = locateFreeBlock();//5
                        if (freeBlockAddr == -1) {
                            System.err.println("No more blocks found for : " + fileName);
                            return DISKFULL;
                        }
                        freeBlockList[freeBlockAddr] = false; // only the next block will be occupied
                        disk.seek(MAXFILES * 16L + 1 + (long) (first + 1) * totalChars + 2);
                        disk.writeBytes("*" + String.valueOf(freeBlockAddr + 1));
                        blockAddrTable[first + 1].setNext(freeBlockAddr + 1);
                        first = freeBlockAddr;
                        disk.seek(MAXFILES * 16L + 1 + (long) MAXBLOCKS * totalChars + 1 + (long) (freeBlockAddr) * (BLOCK_SIZE + 1));
                    }
                }
            }
            System.out.println("File: " + fileName + " was modified!");
        } finally {
            globalLock.unlock();
//            lockManager.releaseOrdered(locks);
        }
        return SUCCESS;
    }

    public String list() throws Exception {
        globalLock.lock();
//        List<LockManager.LockHandle> globalLock = Collections.singletonList(new LockManager.LockHandle(LockLevel.GLOBAL.getRank(), -1, lockManager.getGlobalLock()));
//        lockManager.acquireOrdered(globalLock);
        try {
            String listFiles = "";
            System.out.println("Listing files: ");
            int fileIndex = 0;
            for (int i = 0; i < MAXFILES; i++) {
                if (inodeTable[i] == null) {
                    continue;
                }
                listFiles += "    " + fileIndex++ + " - " + inodeTable[i].getFilename();
            }
            System.out.println("Done Listing files: ");
            return listFiles;
        }  finally {
            globalLock.unlock();
//            lockManager.releaseOrdered(globalLock);
        }
    }

    public int deleteFile(String fileName) throws Exception {
        globalLock.lock();
        int existingFile = fileExists(fileName);
        if (existingFile == -1) {
            System.err.println("File not found: " + fileName);
            return FILENOTFOUND;
        }
//        List<LockManager.LockHandle> locks = locksForFileOp(existingFile);
//        lockManager.acquireOrdered(locks);
        try {
            disk.seek(existingFile * 16L);
            disk.writeBytes("~~~~~~~~~~~~~~~");
            disk.seek(MAXFILES * 16L + 1 + MAXBLOCKS * totalChars + 1 + (inodeTable[existingFile].getFirstBlock() - 1) * (BLOCK_SIZE + 1));

            int pos = 0, first = inodeTable[existingFile].getFirstBlock() - 1;
            for (int p = 0; p < MAXBLOCKS; p++) {
                if (first < 0) {
                    break;
                }
                System.out.println("fast" + first);
                disk.seek(MAXFILES * 16 + 1 + MAXBLOCKS * totalChars + 1 + (long) (first) * (BLOCK_SIZE + 1));
                for (int n = 0; n < BLOCK_SIZE; n++) {
                    disk.writeBytes("~");
                }
                freeBlockList[first] = true;
                disk.seek(MAXFILES * 16 + 1 + (first + 1) * totalChars);
                disk.writeBytes("-");
                disk.seek(MAXFILES * 16 + 1 + (first + 1) * totalChars + 2);
                disk.writeBytes("-1");
                first = blockAddrTable[first + 1].getNext() - 1;
            }
            System.out.println("File: " + fileName + " was deleted!");
            inodeTable[existingFile] = null;
        } finally {
            globalLock.unlock();
//            lockManager.releaseOrdered(locks);
        }
        return SUCCESS;
    }  // TODO: Add readFile, writeFile and other required methods      DONE!(partially(~100%))
}

