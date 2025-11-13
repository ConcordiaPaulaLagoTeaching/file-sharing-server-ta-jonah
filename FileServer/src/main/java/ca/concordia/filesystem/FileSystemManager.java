package ca.concordia.filesystem;

import ca.concordia.filesystem.datastructures.FEntry;

import java.io.RandomAccessFile;
import java.util.concurrent.locks.ReentrantLock;
import ca.concordia.filesystem.datastructures.FNode;
import java.io.IOException;
import java.io.File;

public class FileSystemManager {

    private int MAXFILES = 5;
    private int MAXBLOCKS = 10;
    private static FileSystemManager instance = null;
    private RandomAccessFile disk = null;
    private final ReentrantLock globalLock = new ReentrantLock();

    private static final int BLOCK_SIZE = 128; // Example block size

    private FEntry[] inodeTable; // Array of inodes
    private FNode[] fnodes;
    private boolean[] freeBlockList; // Bitmap for free blocks

    public FileSystemManager(String filename, int totalSize) throws Exception {
        // totalSize = metadataSize + (MAXBLOCKS × BLOCKSIZE)

        // Prevent multiple initializations
        if (instance != null) {
            throw new IllegalStateException("FileSystemManager is already initialized.");
        }

        // Initialize the virtual disk file
        File file = new File(filename);
        this.disk = new RandomAccessFile(file, "rw");

        if (this.disk.length() < totalSize) {
            this.disk.setLength(totalSize);
        }

        // Initialize constants
        this.MAXFILES = 5;     // you can change later if you want
        this.MAXBLOCKS = 10;   // same here

        // Initialize metadata tables
        this.inodeTable = new FEntry[MAXFILES];
        this.fnodes = new FNode[MAXBLOCKS];

        // Fill with empty structures
        for (int i = 0; i < MAXFILES; i++) {
            inodeTable[i] = new FEntry("", (short) 0, (short) -1);
        }
        for (int i = 0; i < MAXBLOCKS; i++) {
            fnodes[i] = new FNode(-1);
        }

        instance = this; // Set instance

        System.out.println("FileSystemManager initialized successfully.");
    }


    // createFile Implementation
    public void createFile(String filename) throws Exception {
        globalLock.lock();
        try {
            // Validate the filename
            if (filename == null || filename.isEmpty()) {
                throw new Exception("ERROR: filename is null or empty");
            }
            if (filename.length() > 11) {
                throw new Exception("ERROR: filename too large");
            }

            // Check if the file already exists
            for (int i = 0; i < MAXFILES; i++) {
                FEntry entry = inodeTable[i];
                if (entry != null && filename.equals(entry.getFilename())) {
                    throw new Exception("ERROR: file " + filename + " already exists");
                }
            }

            // Find a free FEntry slot
            int freeIndex = -1;
            for (int i = 0; i < MAXFILES; i++) {
                FEntry entry = inodeTable[i];
                if (entry == null || entry.getFilename() == null || entry.getFilename().isEmpty()) {
                    freeIndex = i;
                    break;
                }
            }

            if (freeIndex == -1) {
                throw new Exception("ERROR: no free file entries available");
            }

            // Create the new entry (size = 0, firstBlock = -1)
            FEntry newFile = new FEntry(filename, (short)0, (short)-1);
            inodeTable[freeIndex] = newFile;

            // Persist metadata to disk
            persistMetadata();

            System.out.println("SUCCESS: file created -> " + filename);
        } finally {
            globalLock.unlock();
        }
    }

    // deleteFile Implementation
    public void deleteFile(String filename) throws Exception {
        globalLock.lock();
        try {
            // Validate filename
            if (filename == null || filename.isEmpty()) {
                throw new Exception("ERROR: filename is null or empty");
            }

            // Locate the file in the FEntry table
            int fileIndex = -1;
            FEntry target = null;
            for (int i = 0; i < MAXFILES; i++) {
                FEntry entry = inodeTable[i];
                if (entry != null && filename.equals(entry.getFilename())) {
                    fileIndex = i;
                    target = entry;
                    break;
                }
            }

            if (fileIndex == -1 || target == null) {
                throw new Exception("ERROR: file " + filename + " does not exist");
            }

            // Free all blocks in its FNode chain
            int fnodeIndex = target.getFirstBlock();
            while (fnodeIndex != -1 && fnodeIndex < MAXBLOCKS) {
                FNode node = fnodes[fnodeIndex];
                if (node == null) break;

                int blockIdx = node.getBlockIndex();
                if (blockIdx >= 0) {
                    // Overwrite data with zeroes for security
                    long dataOffset = calculateDataOffset(blockIdx);
                    disk.seek(dataOffset);
                    byte[] zeroes = new byte[BLOCK_SIZE];
                    disk.write(zeroes);
                }

                // Mark node as unused
                node.setBlockIndex(-1);
                int next = node.getNext();
                node.setNext(-1);
                fnodeIndex = next;
            }

            // Clear the file entry
            inodeTable[fileIndex] = new FEntry("", (short) 0, (short) -1);

            // Persist metadata to disk
            persistMetadata();

            System.out.println("SUCCESS: file deleted -> " + filename);
        } finally {
            globalLock.unlock();
        }
    }

    // writeFile Implementation
    public void writeFile(String filename, byte[] contents) throws Exception {
        globalLock.lock();
        try {
            // Validate filename
            if (filename == null || filename.isEmpty()) {
                throw new Exception("ERROR: filename is null or empty");
            }

            // Find the file entry
            FEntry target = null;
            int fileIndex = -1;
            for (int i = 0; i < MAXFILES; i++) {
                FEntry entry = inodeTable[i];
                if (entry != null && filename.equals(entry.getFilename())) {
                    target = entry;
                    fileIndex = i;
                    break;
                }
            }

            if (target == null) {
                throw new Exception("ERROR: file " + filename + " does not exist");
            }

            // Free existing blocks
            int currentNodeIndex = target.getFirstBlock();
            while (currentNodeIndex != -1 && currentNodeIndex < MAXBLOCKS) {
                FNode node = fnodes[currentNodeIndex];
                if (node == null) break;
                node.setBlockIndex(-1);
                int next = node.getNext();
                node.setNext(-1);
                currentNodeIndex = next;
            }

            // Calculate number of blocks needed
            int numBlocks = (int) Math.ceil((double) contents.length / BLOCK_SIZE);
            if (numBlocks > MAXBLOCKS) {
                throw new Exception("ERROR: file too large");
            }

            // Find free FNodes for allocation
            java.util.List<Integer> allocatedNodes = new java.util.ArrayList<>();
            for (int i = 0; i < MAXBLOCKS && allocatedNodes.size() < numBlocks; i++) {
                if (fnodes[i].getBlockIndex() < 0) { // free node
                    allocatedNodes.add(i);
                }
            }

            if (allocatedNodes.size() < numBlocks) {
                throw new Exception("ERROR: not enough free blocks available");
            }

            // Write file data block by block
            for (int i = 0; i < allocatedNodes.size(); i++) {
                int nodeIndex = allocatedNodes.get(i);
                int start = i * BLOCK_SIZE;
                int end = Math.min(start + BLOCK_SIZE, contents.length);
                byte[] chunk = java.util.Arrays.copyOfRange(contents, start, end);

                // Write chunk to correct disk position
                long dataOffset = calculateDataOffset(nodeIndex);
                disk.seek(dataOffset);
                disk.write(chunk);

                // Fill rest of block with zeros if needed
                if (chunk.length < BLOCK_SIZE) {
                    byte[] padding = new byte[BLOCK_SIZE - chunk.length];
                    disk.write(padding);
                }

                // Update FNode
                fnodes[nodeIndex].setBlockIndex(nodeIndex);
                if (i < allocatedNodes.size() - 1) {
                    fnodes[nodeIndex].setNext(allocatedNodes.get(i + 1));
                } else {
                    fnodes[nodeIndex].setNext(-1);
                }
            }

            // Update FEntry metadata
            target.setFilesize((short) contents.length);
            target.setFirstBlock((short) (allocatedNodes.isEmpty() ? -1 : allocatedNodes.get(0)));

            // Persist metadata
            persistMetadata();

            System.out.println("SUCCESS: file written -> " + filename + " (" + contents.length + " bytes)");

        } finally {
            globalLock.unlock();
        }
    }

    // readFile Implementation
    public byte[] readFile(String filename) throws Exception {
        globalLock.lock();
        try {
            // Validate filename
            if (filename == null || filename.isEmpty()) {
                throw new Exception("ERROR: filename is null or empty");
            }

            // Find the file entry
            FEntry target = null;
            for (FEntry entry : inodeTable) {
                if (entry != null && filename.equals(entry.getFilename())) {
                    target = entry;
                    break;
                }
            }

            if (target == null) {
                throw new Exception("ERROR: file " + filename + " does not exist");
            }

            short firstBlock = target.getFirstBlock();
            if (firstBlock < 0) {
                return new byte[0]; // Empty file
            }

            short size = target.getFilesize();
            byte[] data = new byte[size];
            int bytesRead = 0;

            // Go over FNode chain
            int currentNodeIndex = firstBlock;

            while (currentNodeIndex != -1 && bytesRead < size) {
                FNode node = fnodes[currentNodeIndex];
                if (node == null || node.getBlockIndex() < 0) {
                    throw new Exception("ERROR: corrupted fnode chain for " + filename);
                }

                int blockPos = (MAXFILES * (11 + 2 + 2)) + (MAXBLOCKS * 4) + (node.getBlockIndex() * BLOCK_SIZE);
                disk.seek(blockPos);

                // Determine how many bytes to read from this block
                int bytesToRead = Math.min(BLOCK_SIZE, size - bytesRead);
                byte[] blockData = new byte[bytesToRead];
                disk.read(blockData);
                System.arraycopy(blockData, 0, data, bytesRead, bytesToRead);

                bytesRead += bytesToRead;
                currentNodeIndex = node.getNext();
            }

            System.out.println("SUCCESS: file read -> " + filename + " (" + bytesRead + " bytes)");
            return data;
        } finally {
            globalLock.unlock();
        }
    }

    // listFiles Implementation
    public String[] listFiles() {
        java.util.List<String> names = new java.util.ArrayList<>(); // Create a temporary list to store filenames

        // Iterate over all file entries in the inode table
        for (FEntry entry : inodeTable) {
            // Check if this entry is valid
            if (entry != null && entry.getFilename() != null && !entry.getFilename().isEmpty()) {
                names.add(entry.getFilename()); // Add filename to the list
            }
        }

        return names.toArray(new String[0]); // Convert the list to a String array and return it
    }

    // Saves all filesystem metadata to the start of the disk file. (FEntries + FNodes)
    private void persistMetadata() throws IOException {
        // Move to beginning of the simulated disk file
        disk.seek(0);

        // Write all FEntries
        for (int i = 0; i < MAXFILES; i++) {
            FEntry entry = inodeTable[i];
            byte[] nameBytes = new byte[11]; // fixed-size filename field

            if (entry != null && entry.getFilename() != null) {
                byte[] src = entry.getFilename().getBytes("UTF-8");
                int copyLen = Math.min(src.length, 11);
                System.arraycopy(src, 0, nameBytes, 0, copyLen);
            }

            disk.write(nameBytes); // 11 bytes
            short size = (entry != null) ? entry.getFilesize() : 0;
            short firstBlock = (entry != null) ? entry.getFirstBlock() : -1;

            disk.writeShort(size);
            disk.writeShort(firstBlock);
        }

        // Write all FNodes
        for (int i = 0; i < MAXBLOCKS; i++) {
            FNode node = fnodes[i];
            short blockIndex = (short) ((node != null) ? node.getBlockIndex() : -1);
            short next = (short) ((node != null) ? node.getNext() : -1);
            disk.writeShort(blockIndex);
            disk.writeShort(next);
        }

        // Force all writes to disk
        disk.getChannel().force(true);
    }

    //Calculates where a given data block starts on the disk file.
    private long calculateDataOffset(int blockIndex) {
        // Metadata section size = (15 * MAXFILES) + (4 * MAXBLOCKS)
        int metadataSize = (15 * MAXFILES) + (4 * MAXBLOCKS);
        return metadataSize + ((long) blockIndex * BLOCK_SIZE);
    }

}
