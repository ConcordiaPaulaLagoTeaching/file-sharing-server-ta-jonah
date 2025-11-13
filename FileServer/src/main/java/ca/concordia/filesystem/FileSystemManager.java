package ca.concordia.filesystem;

import ca.concordia.filesystem.datastructures.FEntry;
import ca.concordia.filesystem.datastructures.FNode;
import java.nio.charset.StandardCharsets;

import java.io.RandomAccessFile;
import java.util.concurrent.locks.ReentrantLock;
import java.io.IOException;

public class FileSystemManager {

    private final int MAXFILES = 5;
    private final int MAXBLOCKS = 10;
    private static final int BLOCK_SIZE = 128; // Example block size
    private final long DATA_START = 115;
    private static final int FENTRY_SIZE = 15; // 11 (filename) + 2 (size) + 2 (firstBlock)

    private final RandomAccessFile disk;
    private final ReentrantLock globalLock = new ReentrantLock();

    // offset in the disk file where data block starts
    private static FileSystemManager instance;

    private FEntry[] inodeTable = new FEntry[MAXFILES]; // Array of inodesd
    private FNode[] blockTable = new FNode[MAXBLOCKS] ;
    private boolean[] freeBlockList = new boolean[MAXBLOCKS]; // Bitmap for free blocks

    public FileSystemManager(String filename, int totalSize) throws IOException {
        // Initialize the file system manager with a file
        disk = new RandomAccessFile(filename, "rw");
        disk.setLength(totalSize);
        //Initialize blocks and free list
        for (int i = 0; i < MAXBLOCKS; i++) {
            blockTable[i] = new FNode(i);
            freeBlockList[i] = true;
        }
        System.out.println("File system initialized:" + totalSize + "bytes");
    }

    public static synchronized FileSystemManager getInstance(String filename, int totalSize) throws IOException {
        if (instance == null) {
            instance = new FileSystemManager(filename, totalSize);
        }
        return instance;
    }

    //Find an existing file entry by name
    private FEntry findEntry(String name) {
        for (FEntry e : inodeTable) {
            if (e != null && e.getFilename().equals(name)) {
                return e;
            }
        }
        return null;
    }

    // Find index of a free inode (FEntry) slot
    private int findFreeInodeIndex() {
        for (int i = 0; i < MAXFILES; i++) {
            if (inodeTable[i] == null) {
                return i;
            }
        }
        return -1; // none free
    }

    //Find first free data block
    private int findFreeBlock() {
        for (int i = 0; i < MAXBLOCKS; i++) {
            if (freeBlockList[i]) {    // true means free
                return i;
            }
        }
        return -1; // no free block
    }

    //convert block index for byte offset
    private long dataOffset (int blockIndex) {
        return DATA_START + (long)blockIndex*BLOCK_SIZE;
    }

//    //for delete or overwrite
//    private void clearBlocks(int firstBlock) {
//        int current = firstBlock;
//        while(current!= -1) {
//            freeBlockList[current]=true; // note the block is free
//            int next = blockTable[current].getNext();
//            blockTable[current].setNext(-1); //unlink
//            current=next;
//        }
//    }



//        public static synchronized FileSystemManager getInstance (String filename, int totalSize) throws IOException {
//            if(instance == null) {
//                //TODO Initialize the file system
//                instance = new FileSystemManager(filename,totalSize);
//            } else {
//                throw new IllegalStateException("FileSystemManager is already initialized.");
//            }
//            return instance;
//
//        }

    // Serialize the FEntry at inodeTable[index] into the filesystem file.
// Layout per entry: 11 bytes filename (ASCII, padded with 0) + 2 bytes size + 2 bytes firstBlock.
    private void writeFEntryToDisk(int index) throws IOException {
        FEntry entry = inodeTable[index];

        long offset = (long) index * FENTRY_SIZE;   // FEntry region starts at beginning of the file
        disk.seek(offset);

        if (entry == null) {
            // Clear this slot: write 15 zero bytes
            byte[] empty = new byte[FENTRY_SIZE];
            disk.write(empty);
            return;
        }

        // 1) filename → 11 bytes ASCII, padded with 0
        byte[] nameBytes = entry.getFilename().getBytes(StandardCharsets.US_ASCII);
        byte[] nameBuf = new byte[11];
        int len = Math.min(nameBytes.length, 11);
        System.arraycopy(nameBytes, 0, nameBuf, 0, len);
        disk.write(nameBuf);

        // 2) filesize (short) + firstBlock (short)
        disk.writeShort(entry.getFilesize());    // adjust if your getter name differs
        disk.writeShort(entry.getFirstBlock());  // idem
    }

    public void createFile(String fileName) throws Exception {
        globalLock.lock();
        try {
            if(fileName == null || fileName.isEmpty()) {
                throw new Exception("ERROR: filename cannot be empty");
            }

            if(fileName.length() > 11) {
                throw new Exception("ERROR: filename is too long");
            }

            if (findEntry(fileName) != null) {
                throw new Exception("ERROR: file " + fileName + " already exists");
            }

            // 3) find a free inode slot
            int freeIndex = findFreeInodeIndex();
            if (freeIndex == -1) {
                throw new Exception("ERROR: maximum number of files reached");
            }

            FEntry entry = new FEntry(fileName, (short) 0, (short) -1);
            inodeTable[freeIndex] = entry;

            // NEW: write this FEntry into the filesystem file, like your friend does with seek+write
            writeFEntryToDisk(freeIndex);

            System.out.println("Created file: " + fileName + " at inode index " + freeIndex);

        }
        finally{
            globalLock.unlock();
        }
    }


        // TODO: Add readFile, writeFile and other required methods,
    }