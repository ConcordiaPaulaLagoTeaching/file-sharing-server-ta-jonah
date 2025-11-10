package ca.concordia.filesystem;

import ca.concordia.filesystem.datastructures.FEntry;
import ca.concordia.filesystem.datastructures.FNode;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.concurrent.locks.ReentrantLock;

public class FileSystemManager {

    private final int MAXFILES = 5;
    private int MAXBLOCKS = 10;
    private final static FileSystemManager instance = null;
    private RandomAccessFile disk = null;
    private final ReentrantLock globalLock = new ReentrantLock();

    private static final int BLOCK_SIZE = 128; // Example block size

    private FEntry[] inodeTable = new FEntry[MAXFILES]; // Array of inodes
    private boolean[] freeBlockList = new boolean[MAXBLOCKS]; // Bitmap for free blocks
    private FNode[] blockAddrTable = new FNode[MAXBLOCKS];

    public FileSystemManager(String filename, int totalSize) throws IOException {
        // Initialize the file system manager with a file
        if (instance == null) {
            //TODO Initialize the file system
            Arrays.fill(freeBlockList, true);
            for (int i = 0; i < MAXBLOCKS; i++) {
                blockAddrTable[i] = new FNode(-i);
            }
            disk = new RandomAccessFile(filename, "rw");
            for (int i = 0; i < MAXFILES * 15; i++) {
                disk.writeBytes("~");
            }
            disk.writeBytes("\n");
            for (int i = 0; i < MAXBLOCKS; i++) {
                disk.writeBytes(blockAddrTable[i].getBlockIndex() + "-1");
            }
            disk.writeBytes("\n");
            for (int i = 0; i < MAXBLOCKS; i++) {
                for (int j = 0; j < BLOCK_SIZE; j++) {
                    disk.writeBytes("~");
                }
                disk.writeBytes("\n");
            }
            disk.writeBytes("EOF\n");
            MAXBLOCKS = totalSize / BLOCK_SIZE;
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

    public void createFile(String fileName) throws Exception {
        System.out.println("Creating new file: " + fileName + "....");

        int freeBlockAddr = locateFreeBlock();
        if (freeBlockAddr == -1) {
            System.err.println("No available blocks to allocate for : " + fileName);
            return;
        }
        int freeFile = locateFreeFile();
        if (freeFile == -1) {
            System.err.println("No file was found named as : " + fileName);
            return;
        }
        else if(freeFile == -2) {
            System.err.println("Disk is full for file: " + fileName + " to be added to memory");
            return;
        }
        blockAddrTable[freeBlockAddr] = new FNode(freeBlockAddr + 1);

        disk.seek(freeFile * 15L + 14);
        disk.writeBytes(String.valueOf(blockAddrTable[freeBlockAddr].getBlockIndex()));
        System.out.println("Wrote into the file!: " + fileName);
        freeBlockList[freeBlockAddr] = false;

        disk.seek(MAXFILES * 15 + (blockAddrTable[freeBlockAddr].getBlockIndex()) * 4L);
        disk.writeBytes("~");
        freeFile = locateFreeFile();
        try {
            inodeTable[freeFile] = new FEntry(fileName, (short) 0, (short) (freeBlockAddr + 1));
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid filename length: " + fileName);
            return;
        }
        System.out.println(inodeTable[freeFile].getFilename());
        disk.seek(freeFile * 15L);
        disk.writeBytes(inodeTable[freeFile].getFilename());
        disk.seek(freeFile * 15L + 11);
        disk.writeBytes(String.valueOf(inodeTable[freeFile].getFilesize()));
//                disk[i].writeBytes("Created by means of File server! Yesss ----------------------------------------------------------------------|>\n");
        if (freeFile == MAXFILES - 1 && inodeTable[freeFile] != null) {
            System.out.println("No more space to write: " + fileName);
            return;
        }
        System.out.println("New file: " + fileName + " created");
    }

    public String readFile(String fileName) throws Exception {
        StringBuilder readString = new StringBuilder();
        System.out.println("Reading from file: " + fileName);
        int existingFile = fileExists(fileName);
        if(existingFile == -1) {
            System.err.println("File not found: " + fileName);
            return null;
        }
        String line;
        int first = inodeTable[existingFile].getFirstBlock() - 1;
        do {
            disk.seek(MAXFILES * 15 + MAXBLOCKS * 4L + (long) (first) * (BLOCK_SIZE + 1) + 1);
            line = disk.readLine().replace("~", "");
            readString.append(line);
            first = blockAddrTable[first].getNext();
        } while (first >= 0);
        System.out.println("File: " + fileName + " was read!");
        System.out.println(readString);
        return readString.toString();
    }

    public void writeFile(String fileName, String text) throws Exception {
        int existingFile = fileExists(fileName);
        if(existingFile == -1) {
            System.err.println("File not found: " + fileName);
            return;
        }
        disk.seek(existingFile * 15L + 11);
        disk.writeBytes(String.valueOf(text.length()));
        disk.seek(MAXFILES * 15 + MAXBLOCKS * 4L + (inodeTable[existingFile].getFirstBlock() - 1) * (BLOCK_SIZE + 1) + 1);
        if (text.length() <= BLOCK_SIZE) {
            disk.writeBytes(text);
        } else {
            int pos = 0, first = inodeTable[existingFile].getFirstBlock() - 1;
            boolean lastline = false;
            for (int n = 1; pos < text.length(); n++) {
                int freeBlockAddr = locateFreeBlock();
                if(freeBlockAddr == -1) {
                    System.err.println("No more blocks found for : " + fileName);
                    return;
                }
                for (int l = pos; l < BLOCK_SIZE * n && l < text.length(); l++) {
                    disk.write(text.charAt(l));
                    if (l > BLOCK_SIZE * ((int) text.length() / BLOCK_SIZE)) {
                        lastline = true;
                    }
                }
                pos += BLOCK_SIZE;

                disk.seek(MAXFILES * 15 + 4 + (first) * 4L);
                disk.writeBytes("~");
                if (!lastline) {
                    freeBlockList[freeBlockAddr] = false;
                    disk.seek(MAXFILES * 15 + 4 + (first) * 4L + 2);
                    disk.writeBytes("*" + String.valueOf(freeBlockAddr + 1));
                    blockAddrTable[first].setNext(freeBlockAddr);
                    first = freeBlockAddr;
                    disk.seek(MAXFILES * 15 + MAXBLOCKS * 4L + (long) (freeBlockAddr) * (BLOCK_SIZE + 1) + 1);
                }
            }
        }
        System.out.println("File: " + fileName + " was modified!");
    }

    public String list() throws Exception {
        String listFiles = "";
        System.out.println("Listing files: ");
        int fileIndex = 0;
        for (int i = 0; i < MAXFILES; i++) {
            if (inodeTable[i] == null) { continue; }
            listFiles += "    " + fileIndex++ + " - " + inodeTable[i].getFilename();
//            System.out.println("    " + i + " - " + inodeTable[i].getFilename());
        }
        System.out.println("Done Listing files: ");
        return listFiles;
    }

    public void deleteFile(String fileName) throws Exception {
        int existingFile = fileExists(fileName);
        if(existingFile == -1) {
            System.err.println("File not found: " + fileName);
            return;
        }
        disk.seek(existingFile * 15L);
        disk.writeBytes("~~~~~~~~~~~~~~~");
        disk.seek(MAXFILES * 15 + MAXBLOCKS * 4L + (inodeTable[existingFile].getFirstBlock() - 1) * (BLOCK_SIZE + 1) + 1);

        int pos = 0, first = inodeTable[existingFile].getFirstBlock() - 1;
        for (int p = 0; p < MAXBLOCKS; p++) {
            disk.seek(MAXFILES * 15 + MAXBLOCKS * 4L + (long) (first) * (BLOCK_SIZE + 1) + 1);
            for (int n = 0; n < BLOCK_SIZE; n++) { disk.writeBytes("~"); }
            freeBlockList[first] = true;
            disk.seek(MAXFILES * 15 + 4 + first * 4L);
            disk.writeBytes("-");
            disk.seek(MAXFILES * 15 + 4 + first * 4L + 2);
            disk.writeBytes("-1");
            first = blockAddrTable[first].getNext();
            if(first < 0) break;
        }
        System.out.println("File: " + fileName + " was deleted!");
        inodeTable[existingFile] = null;
    }  // TODO: Add readFile, writeFile and other required methods      DONE!(partially(~90%))
}

