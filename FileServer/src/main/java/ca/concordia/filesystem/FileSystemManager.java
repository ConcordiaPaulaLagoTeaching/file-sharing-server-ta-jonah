package ca.concordia.filesystem;

import ca.concordia.filesystem.datastructures.FEntry;

import java.io.RandomAccessFile;
import java.util.concurrent.locks.ReentrantLock;

public class FileSystemManager {

    private final int MAXFILES = 5;
    private final int MAXBLOCKS = 10;
    private static final int BLOCK_SIZE = 128; // Example block size

    private final RandomAccessFile disk;
    private final ReentrantLock globalLock = new ReentrantLock();

    private final static FileSystemManager instance;

    private FEntry[] inodeTable = new FEntry[MAXFILES]; // Array of inodesd
    private FNode[] blockTable = new FNode[MAXBLOCKS] ;
    private boolean[] freeBlockList = new boolean[MAXBLOCKS]; // Bitmap for free blocks

    private final long DATA_START = 115;

    public FileSystemManager(String filename, int totalSize) throws IOException {
        // Initialize the file system manager with a file
        disk = new RandomAccessFile(filename,"rw");
        disk.setLength(totalSize);
        //Initialize blocks and free list
        for(int i=0;i<MAXBLOCKS;i++){
            blockTable[i]=new FNode(i);
            freeBlockList[i]=true;
        }
        System.out.println("File system initialized:" + totalSize+"bytes");

        //Find an existing file entry by name
        private FEntry findEntry(String name) {
            for (FEntry e: inodeTable)
                if(e!= null && e.getFilenam().equals(name) ) {
                    return e;
                }
            return null;
        }

        //Find first free data block
        private int findFreeBlock(){
            for (int i=0;i<MAXBLOCKS;i++) {
                if (!freeBlockList[i]) {
                    return i;
                }
            }
            return -1; //no free block
        }

        //for delete or overwrite
        private void clearBlocks(int firstBlock) {
            int current = firstBlock;
            while(current!= -1) {
                freeBlockList[current]=true; // note the block is free
                int next = fnodes[current].getNext();
                fnodes[current].setNext(-1); //unlink
                current=next;
            }
        }

        //convert block index for byte offset
        private long dataOffset (int blockIndex) {
            return DATA_START + (long)blockIndex*BLOCK_SIZE;
        }

        public static synchronized FileSystemManager getInstance (String filename, int totalSize) throws IOException {
            if(instance == null) {
                //TODO Initialize the file system
                instance = new FileSystemManager(filename,totalSize);
            } else {
                throw new IllegalStateException("FileSystemManager is already initialized.");
            }
            return instance;

        }

        public void createFile(String fileName) throws Exception {
            // TODO
            throw new UnsupportedOperationException("Method not implemented yet.");
        }


        // TODO: Add readFile, writeFile and other required methods,
    }
