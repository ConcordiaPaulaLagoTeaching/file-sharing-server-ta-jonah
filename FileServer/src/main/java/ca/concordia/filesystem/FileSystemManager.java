package ca.concordia.filesystem;

import ca.concordia.filesystem.datastructures.FEntry;
import ca.concordia.filesystem.datastructures.FNode;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.RandomAccess;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class FileSystemManager {

    private final int MAXFILES = 5;
    private final int MAXBLOCKS = 10;
    private /*final*/ static FileSystemManager instance = null; // Preserved as-is
    private final RandomAccessFile disk;
    private final ReentrantReadWriteLock globalLock = new ReentrantReadWriteLock();

    private static final int BLOCK_SIZE = 128;

    private FEntry[] inodeTable;           // Array of file entries
    private FNode[] fnodes;                // Array of file nodes
    private boolean[] freeBlockList;       // Tracks free blocks
    private byte[][] blocks;               // Simulated block storage
    /**
     * @param filename
     * @param totalSize
     */
    private FileSystemManager(String filename, int totalSize) { //changed to private
        if (instance == null) {
            try {
                this.disk = new RandomAccessFile(filename, "rw");

                inodeTable = new FEntry[MAXFILES];
                fnodes = new FNode[MAXBLOCKS];
                freeBlockList = new boolean[MAXBLOCKS];
                blocks = new byte[MAXBLOCKS][BLOCK_SIZE];
                loadFromDisk();

                for (int i = 0; i < MAXFILES; i++) {
                    inodeTable[i] = new FEntry("", (short) 0, (short) -1); // changed and added placeholder values
                }

                for (int i = 0; i < MAXBLOCKS; i++) {
                    fnodes[i] = new FNode(i); // pass index or block ID
                    freeBlockList[i] = true;
                }

                freeBlockList[0] = false; // Reserve block 0 for metadata
            } catch (Exception e) {
                throw new RuntimeException("Failed to initialize file system: " + e.getMessage());
            }
        } else {
            throw new IllegalStateException("FileSystemManager is already initialized.");
        }
    }

    public static synchronized FileSystemManager getInstance(String filename, int totalSize) {
        if (instance == null) {
            instance = new FileSystemManager(filename, totalSize);
        }
        return instance;
    }

    // CREATE operation
    public void createFile(String fileName) throws Exception {
        globalLock.writeLock().lock();
        try {
            if (fileName.length() > 11) throw new Exception("ERROR: filename too large");

            for (FEntry entry : inodeTable) {
                if (!entry.isFree() && entry.getFilename().equals(fileName)) {
                    throw new Exception("ERROR: file already exists");
                }
            }

            for (FEntry entry : inodeTable) {
                if (entry.isFree()) {
                    entry.setFilename(fileName);
                    entry.setFilesize((short) 0);
                    entry.setFirstBlock((short) -1);
                    return;
                }
            }
            saveToDisk();
            throw new Exception("ERROR: no space for new file");
        } finally {
            globalLock.writeLock().unlock();
        }
    }

    // DELETE operation
    public void deleteFile(String fileName) throws Exception {
        globalLock.writeLock().lock();
        try {
            for (FEntry entry : inodeTable) {
                if (!entry.isFree() && entry.getFilename().equals(fileName)) {
                    int current = entry.getFirstBlock();
                    while (current != -1) {
                        FNode node = fnodes[current];
                        int blockIndex = node.getBlockIndex();
                        freeBlockList[blockIndex] = true;
                        blocks[blockIndex] = new byte[BLOCK_SIZE]; // Zero out
                        int next = node.getNext();
                        node.setBlockIndex(-1);
                        node.setNext(-1);
                        current = next;
                    }
                    entry.setFilename(null);
                    entry.setFilesize((short) 0);
                    entry.setFirstBlock((short) -1);
                    return;
                }
            }
            saveToDisk();
            throw new Exception("ERROR: file " + fileName + " does not exist");
        } finally {
            globalLock.writeLock().unlock();
        }
    }

    // WRITE operation
    public void writeFile(String fileName, byte[] contents) throws Exception {
        globalLock.writeLock().lock();
        try {
            int blocksNeeded = (contents.length + BLOCK_SIZE - 1) / BLOCK_SIZE;
            List<Integer> freeNodes = new ArrayList<>();
            List<Integer> freeBlocks = new ArrayList<>();

            for (int i = 0; i < MAXBLOCKS; i++) {
                if (fnodes[i].isFree()) freeNodes.add(i);
                if (freeBlockList[i]) freeBlocks.add(i);
            }

            if (freeNodes.size() < blocksNeeded || freeBlocks.size() < blocksNeeded) {
                throw new Exception("ERROR: file too large");
            }

            FEntry entry = null;
            for (FEntry e : inodeTable) {
                if (!e.isFree() && e.getFilename().equals(fileName)) {
                    entry = e;
                    break;
                }
            }
            if (entry == null) throw new Exception("ERROR: file " + fileName + " does not exist");

            // Delete old data
            deleteFile(fileName);

            // Write new data
            int firstNode = freeNodes.get(0);
            entry.setFirstBlock((short) firstNode);
            entry.setFilesize((short) contents.length);

            int contentIndex = 0;
            for (int i = 0; i < blocksNeeded; i++) {
                int nodeIndex = freeNodes.get(i);
                int blockIndex = freeBlocks.get(i);

                fnodes[nodeIndex].setBlockIndex(blockIndex);
                freeBlockList[blockIndex] = false;

                int length = Math.min(BLOCK_SIZE, contents.length - contentIndex);
                System.arraycopy(contents, 0, blocks[blockIndex], 0, length);
                contentIndex += length;

                if (i < blocksNeeded - 1) {
                    fnodes[nodeIndex].setNext(freeNodes.get(i + 1));
                } else {
                    fnodes[nodeIndex].setNext(-1);
                }
            }
            saveToDisk();
        } finally {
            globalLock.writeLock().unlock();
        }
    }

    // READ operation
    public byte[] readFile(String fileName) throws Exception {
        globalLock.readLock().lock();
        try {
            for (FEntry entry : inodeTable) {
                if (!entry.isFree() && entry.getFilename().equals(fileName)) {
                    byte[] result = new byte[entry.getFilesize()];
                    int current = entry.getFirstBlock();
                    int offset = 0;

                    while (current != -1) {
                        FNode node = fnodes[current];
                        int blockIndex = node.getBlockIndex();
                        int length = Math.min(BLOCK_SIZE, result.length - offset);
                        System.arraycopy(blocks[blockIndex], 0, result, offset, length);
                        offset += length;
                        current = node.getNext();
                    }
                    return result;
                }
            }
            throw new Exception("ERROR: file " + fileName + " does not exist");
        } finally {
            globalLock.readLock().unlock();
        }
    }

    // LIST operation
    public String[] listFiles() {
        globalLock.readLock().lock();
        try {
            List<String> files = new ArrayList<>();
            for (FEntry entry : inodeTable) {
                if (!entry.isFree()) {
                    files.add(entry.getFilename());
                }
            }
            return files.toArray(new String[0]);
        } finally {
            globalLock.readLock().unlock();
        }
    }

    public void saveToDisk() throws Exception {
        globalLock.writeLock().lock();
        try {
            disk.seek(0);

            disk.writeInt(inodeTable.length);
            for (FEntry entry : inodeTable) {
                disk.writeUTF(entry.getFilename());
                disk.writeShort(entry.getFilesize());
                disk.writeShort(entry.getFirstBlock());
            }

            disk.writeInt(fnodes.length);
            for (FNode node : fnodes) {
                disk.writeInt(node.getBlockIndex());
                disk.writeInt(node.getNext());
            }

            disk.writeInt(freeBlockList.length);
            for (boolean freeBlock : freeBlockList) {
                disk.writeBoolean(freeBlock);
            }

        } finally {
            System.out.println("System data saved successfully");
            globalLock.writeLock().unlock();
        }
    }

    public void loadFromDisk() throws Exception {
        disk.seek(0);
        
        int inodeCount = disk.readInt();
        for (int i = 0; i < inodeCount; i++) {
            String filename = disk.readUTF();
            short filesize = disk.readShort();
            short firstBlock = disk.readShort();
            inodeTable[i] = new FEntry(filename, filesize, firstBlock);
        }

        int fnodeCount = disk.readInt();
        for (int i = 0; i < fnodeCount; i++) {
            int blockIndex = disk.readInt();
            fnodes[i].setBlockIndex(blockIndex);
            int nextBlock = disk.readInt();
            fnodes[i].setNext(nextBlock);
        }

        int freeBlockCount = disk.readInt();
        for (int i = 0; i < freeBlockCount; i++) {
            freeBlockList[i] = disk.readBoolean();
        }



        System.out.println("System data loaded successfully");
    }
}