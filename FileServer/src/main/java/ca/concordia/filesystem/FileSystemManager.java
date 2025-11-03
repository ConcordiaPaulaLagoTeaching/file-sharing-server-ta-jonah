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
    private final int MAXBLOCKS = 10;
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
                disk.writeBytes("^");
            }
            disk.writeBytes("\n");
            for (int i = 0; i < MAXBLOCKS; i++) {
                disk.writeBytes(blockAddrTable[i].getBlockIndex() + "-1");
            }
            disk.writeBytes("\n");
            for (int i = 0; i < MAXBLOCKS; i++) {
                for (int j = 0; j < BLOCK_SIZE; j++) {
                    disk.writeBytes("^");
                }
                disk.writeBytes("\n");
            }
            disk.writeBytes("EOF\n");
        } else {
            throw new IllegalStateException("FileSystemManager is already initialized.");
        }

    }

    public void createFile(String fileName) throws Exception {
        // TODO
//        throw new UnsupportedOperationException("Method ain't implemented yet boko.");
        System.out.println("Creating new file: " + fileName + "....");

        int freeBlockAddr = 0;

        for (int i = 0; i < MAXBLOCKS; i++) {
            if (freeBlockList[i]) {
                int freeFile = 0;
                blockAddrTable[i] = new FNode(i + 1);
                for (int j = 0; j < MAXFILES; j++) {
                    if (inodeTable[j] == null) {
                        freeFile = j;
                        break;
                    }
                }
                disk.seek(freeFile * 15L + 14);
                disk.writeBytes(String.valueOf(blockAddrTable[i].getBlockIndex()));
                System.out.println("Wrote into the file!: " + fileName);
                freeBlockList[i] = false;

                disk.seek(MAXFILES * 15 + (blockAddrTable[i].getBlockIndex()) * 4L);
                disk.writeBytes("^");
                freeBlockAddr = i;
                break;
            }
        }
        for (int i = 0; i < MAXFILES; i++) {
            if (inodeTable[i] == null) {
                try {
                    inodeTable[i] = new FEntry(fileName, (short) 0, (short) (freeBlockAddr + 1));
                } catch (IllegalArgumentException e) {
                    System.err.println("Invalid filename length: " + fileName);
                    return;
                }
                System.out.println(inodeTable[i].getFilename());
                disk.seek(i * 15);
                disk.writeBytes(inodeTable[i].getFilename());
                disk.seek(i * 15 + 11);
                disk.writeBytes(String.valueOf(inodeTable[i].getFilesize()));
//                disk[i].writeBytes("Created by means of File server! Yes ----------------------------------------------------------------------|>\n");
                break;
            }
            if (i == MAXFILES - 1 && inodeTable[i] != null) {
                System.out.println("No more space to write: " + fileName);
                return;
            }
        }


        System.out.println("New file: " + fileName + " created");
    }

    public void readFile(String fileName) throws Exception {
        // TODO
//        throw new UnsupportedOperationException("Method ain't implemented yet boko.");
        System.out.println("Reading from file: " + fileName);
        for (int i = 0; i < MAXFILES; i++) {
            System.out.println(i + " : get that");
            if (inodeTable[i] != null && inodeTable[i].getFilename().equals(fileName)) {
                String line;
                int first = inodeTable[i].getFirstBlock() - 1;
                for (int p = 0; p < MAXBLOCKS; p++) {
                    disk.seek(MAXFILES * 15 + MAXBLOCKS * 4L + (long) (first) * (BLOCK_SIZE + 1) + 1);
                    line = disk.readLine();
                    System.out.println(line);
                    first = blockAddrTable[first].getNext();
                    if (first < 0) break;
                }
                System.out.println("File: " + fileName + " was read!");
                break;
            }
        }
    }

    public void writeFile(String fileName, String text) throws Exception {
        // TODO
//        throw new UnsupportedOperationException("Method ain't implemented yet boko.");
        for (int i = 0; i < MAXFILES; i++) {
            System.out.println(i + " : get that");
            if (inodeTable[i] != null && inodeTable[i].getFilename().equals(fileName)) {
                disk.seek(i * 15 + 11);
                disk.writeBytes(String.valueOf(text.length()));
                System.out.println(inodeTable[i].getFirstBlock());
                System.out.println(inodeTable[i].getFilename());
                disk.seek(MAXFILES * 15 + MAXBLOCKS * 4L + (inodeTable[i].getFirstBlock() - 1) * (BLOCK_SIZE + 1) + 1);
                if (text.length() <= BLOCK_SIZE) {
                    disk.writeBytes(text);
                    break;
                } else {
                    int pos = 0, first = inodeTable[i].getFirstBlock() - 1;
                    boolean lastline = false;
                    for (int n = 1; pos < text.length(); n++) {
                        for (int l = pos; l < BLOCK_SIZE * n && l < text.length(); l++) {
                            disk.write(text.charAt(l));
                            if (l > BLOCK_SIZE * ((int) text.length() / BLOCK_SIZE)) {
                                lastline = true;
                            }
                        }
                        pos += BLOCK_SIZE;
                        for (int p = 0; p < MAXBLOCKS; p++) {
                            if (freeBlockList[p]) {
                                disk.seek(MAXFILES * 15 + 4 + (first) * 4L);
                                disk.writeBytes("^");
                                if (!lastline) {
                                    freeBlockList[p] = false;
                                    disk.seek(MAXFILES * 15 + 4 + (first) * 4L + 2);
                                    disk.writeBytes("*" + String.valueOf(p + 1));
                                    blockAddrTable[first].setNext(p);
                                    first = p;
//                                    asdfghjklzxcvbnmasdfghjklzxcbnmasdfghjklzxcvbnmasdfghjklzxcvbnmasdfghjklzxcvbnmasdfghjklzxcvbnmasdfghjklzxcvbnmasdfghjklzxcvbnmasdfghj
                                    disk.seek(MAXFILES * 15 + MAXBLOCKS * 4L + (p) * (BLOCK_SIZE + 1) + 1);
                                    break;
                                }
                            }
                        }
                    }
                    System.out.println("File: " + fileName + " was modified!");
                    break;
                }
            }
        }
    }

    public void list() throws Exception {
        // TODO
//        throw new UnsupportedOperationException("Method ain't implemented yet boko.");
        System.out.println("Listing files: ");
        for (int i = 0; i < MAXFILES; i++) {
            if (inodeTable[i] == null) {
                continue;
            }
            System.out.println("    " + i + " - " + inodeTable[i].getFilename());
        }
    }

    public void deleteFile(String fileName) throws Exception {
        for (int i = 0; i < MAXFILES; i++) {
            System.out.println(i + " : get that");
            if (inodeTable[i] != null && inodeTable[i].getFilename().equals(fileName)) {
                disk.seek(i * 15);
                disk.writeBytes("^^^^^^^^^^^^^^^");
                System.out.println(inodeTable[i].getFirstBlock());
                System.out.println(inodeTable[i].getFilename());
                disk.seek(MAXFILES * 15 + MAXBLOCKS * 4L + (inodeTable[i].getFirstBlock() - 1) * (BLOCK_SIZE + 1) + 1);

                int pos = 0, first = inodeTable[i].getFirstBlock() - 1;
                boolean lastline = false;
                for (int p = 0; p < MAXBLOCKS; p++) {
                    disk.seek(MAXFILES * 15 + MAXBLOCKS * 4L + (long) (first) * (BLOCK_SIZE + 1) + 1);
                    for (int n = 0; n < BLOCK_SIZE; n++) {
                        disk.writeBytes("^");
                    }
                    freeBlockList[first] = true;
                    first = blockAddrTable[first].getNext();
                    if(first < 0) break;
                }
                System.out.println("File: " + fileName + " was deleted!");
                inodeTable[i] = null;

                break;
            }
        }
    }
        // TODO: Add readFile, writeFile and other required methods.
}

