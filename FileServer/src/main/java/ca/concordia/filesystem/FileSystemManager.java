package ca.concordia.filesystem;

import ca.concordia.filesystem.datastructures.FEntry;
import ca.concordia.filesystem.datastructures.FNode;
import java.nio.charset.StandardCharsets;

import java.io.RandomAccessFile;
import java.util.concurrent.locks.ReentrantLock;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.locks.ReentrantReadWriteLock;



public class FileSystemManager {

    private final int MAXFILES = 5;
    private final int MAXBLOCKS = 10;
    private static final int BLOCK_SIZE = 128;
    private final long DATA_START = 115;
    // 11 (filename) + 2 (size) + 2 (firstBlock)
    private static final int FENTRY_SIZE = 15;

    private final RandomAccessFile disk;
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock(true);

    // offset in the disk file where data block starts
    private static FileSystemManager instance;

    private FEntry[] inodeTable = new FEntry[MAXFILES]; // Array of inodesd
    private FNode[] blockTable = new FNode[MAXBLOCKS] ;
    private boolean[] freeBlockList = new boolean[MAXBLOCKS]; // Bitmap for free blocks

    public FileSystemManager(String filename, int totalSize) throws IOException {
        // Initialize the file system manager with a file
        disk = new RandomAccessFile(filename, "rw");
        if (disk.length() == 0) {
            disk.setLength(totalSize);
        }


        //Initialize blocks and free list
        for (int i = 0; i < MAXBLOCKS; i++) {
            blockTable[i] = new FNode(i);
        }

        loadInodeTable();
        loadFNodeTable();
        rebuildFreeBlockList();

        System.out.println("File system initialized:" + totalSize + "bytes");
    }

    public static synchronized FileSystemManager getInstance(String filename, int totalSize) throws IOException {
        if (instance == null) {
            instance = new FileSystemManager(filename, totalSize);
        }
        return instance;
    }

    // ------------------------- LOAD METADATA -------------------------------

    private void loadInodeTable() throws IOException {
        for (int i = 0; i < MAXFILES; i++) {
            long offset = i * FENTRY_SIZE;
            disk.seek(offset);

            byte[] nameBuf = new byte[11];
            disk.read(nameBuf);
            String name = new String(nameBuf, StandardCharsets.US_ASCII).trim();

            short size = disk.readShort();
            short firstBlock = disk.readShort();

            if (!name.isEmpty()) {
                inodeTable[i] = new FEntry(name, size, firstBlock);
            }
        }
    }

    private void loadFNodeTable() throws IOException {
        for (int i = 0; i < MAXBLOCKS; i++) {
            long offset = DATA_START + (long) i * BLOCK_SIZE;
            blockTable[i].setNext(-1); // default until WRITE updates chain
        }
    }

    private void rebuildFreeBlockList() {
        Arrays.fill(freeBlockList, true);

        for (FEntry e : inodeTable) {
            if (e != null && e.getFirstBlock() != -1) {

                int blk = e.getFirstBlock();

                while (blk != -1) {
                    freeBlockList[blk] = false;
                    blk = blockTable[blk].getNext();
                }
            }
        }
    }

    //-----Helpers-----------


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

    //Count how many block are free
    private int countFreeBlocks() {
        int count = 0;
        for (boolean b : freeBlockList) {
            if (b) {
                count++;
            }
        }
        return count;
    }

    //Find the index of existing Fentry
    private int findEntryIndex(String name) {
        for (int i = 0; i < MAXFILES; i++) {
            if (inodeTable[i] != null && inodeTable[i].getFilename().equals(name)) {
                return i;
            }
        }
        return -1;
    }

    //Clear block for overwrite
    private void clearBlocks(short firstBlock) throws IOException {
        int current = firstBlock;
        while (current != -1) {
            freeBlockList[current] = true;
            int next = blockTable[current].getNext();
            blockTable[current].setNext(-1);

            // optional: blank the data on disk
            disk.seek(dataOffset(current));
            disk.write(new byte[BLOCK_SIZE]);

            current = next;
        }
    }


    //Fentry serialization


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
        rwLock.writeLock().lock();
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

            int freeIndex = findFreeInodeIndex();
            if (freeIndex == -1) {
                throw new Exception("ERROR: maximum number of files reached");
            }

            FEntry entry = new FEntry(fileName, (short) 0, (short) -1);
            inodeTable[freeIndex] = entry;

            writeFEntryToDisk(freeIndex);

            System.out.println("Created file: " + fileName + " at inode index " + freeIndex);

        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public void writeFile(String filename, String content) throws Exception {
        boolean locked = rwLock.writeLock().tryLock();

        if (!locked) {
            //Another writer exists
            throw new Exception("BUSY_WRITING");
        }
        try {
            System.out.println("Writer started for: " + filename);

            // *** TESTING DELAY to simulate long write ***
            //Thread.sleep(10000);   // force writer to hold lock for 3 seconds

            FEntry file = findEntry(filename);
            if (file == null)
                throw new Exception("ERROR: file " + filename + " does not exist");

            //clear old blocks (if any)
            if (file.getFirstBlock() != -1) {
                clearBlocks(file.getFirstBlock());
            }

            //calculate blocks needed
            byte[] data = content.getBytes(StandardCharsets.US_ASCII);
            int totalBytes = data.length;
            int blocksNeeded = (int) Math.ceil((double) totalBytes / BLOCK_SIZE);

            if (blocksNeeded > countFreeBlocks())
                throw new Exception("ERROR: file too large (not enough blocks)");

            //allocate new blocks
            int firstBlock = -1;
            int prevBlock = -1;
            int offset = 0;

            for (int i = 0; i < blocksNeeded; i++) {

                int blk = findFreeBlock();
                if (blk == -1) throw new Exception("ERROR: no free blocks (should not happen)");

                freeBlockList[blk] = false;

                long diskOffset = dataOffset(blk);
                disk.seek(diskOffset);

                int chunk = Math.min(BLOCK_SIZE, totalBytes - offset);
                disk.write(data, offset, chunk);
                offset += chunk;

                // link FNODE chain in memory
                if (prevBlock != -1)
                    blockTable[prevBlock].setNext(blk);

                if (i == 0)
                    firstBlock = blk;

                prevBlock = blk;
            }

            // mark last block end
            if (prevBlock != -1) {
                blockTable[prevBlock].setNext(-1);
            }

            // update FEntry metadata in RAM
            file.setFilesize((short) totalBytes);
            file.setFirstBlock((short) firstBlock);

            // write FEntry back to disk
            int idx = findEntryIndex(filename);
            if (idx != -1) {
                writeFEntryToDisk(idx);
            }

       } finally {
            rwLock.writeLock().unlock();
            //System.out.println("Content bytes = " + " / Free blocks = " + countFreeBlocks());

       }
        //System.out.println("Content bytes = " + totalBytes + " / Free blocks = " + countFreeBlocks());
    }

    //ReadFile
    public String readFile(String filename) throws Exception {
        rwLock.readLock().lock();
        try {
            FEntry file = findEntry(filename);
            if (file == null)
                throw new Exception("ERROR: file " + filename + " does not exist");

            if (file.getFirstBlock() == -1)
                return "";   // empty file

            int current = file.getFirstBlock();
            int remaining = file.getFilesize();
            byte[] buffer = new byte[remaining];
            int offset = 0;

            while (current != -1 && remaining > 0) {

                long diskOffset = dataOffset(current);
                disk.seek(diskOffset);

                int chunk = Math.min(remaining, BLOCK_SIZE);

                disk.readFully(buffer, offset, chunk);

                offset += chunk;
                remaining -= chunk;

                current = blockTable[current].getNext();
            }

            return new String(buffer, StandardCharsets.US_ASCII);

        } finally {
            rwLock.readLock().unlock();
        }
    }

    public void deleteFile(String filename) throws Exception {
        rwLock.writeLock().lock();
        try {
            int idx = findEntryIndex(filename);
            if (idx == -1){
                throw new Exception("ERROR: file " + filename + " does not exist");
            }

            FEntry file = inodeTable[idx];

            // Clear its block if any exist
            if (file.getFirstBlock() != -1){
                clearBlocks(file.getFirstBlock());
            }

            // Remove from inode table (in memory)
            inodeTable[idx] = null;
            writeFEntryToDisk(idx);

            System.out.println("Deleted file: " + filename + " at inode index " + idx);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public String listFiles(){
        rwLock.readLock().lock();
        try {
            StringBuilder sb = new StringBuilder();
            for(FEntry e : inodeTable){
                if(e != null){
                    sb.append(e.getFilename()).append("\n");
                }
            }
            return sb.toString().trim();
        } finally{
            rwLock.readLock().unlock();
        }
    }

}

