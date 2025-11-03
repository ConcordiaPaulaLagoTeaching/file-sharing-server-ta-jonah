package ca.concordia.filesystem.datastructures;

import java.util.LinkedList;

public class FEntry {

    private String filename;          // name of the file
    private short filesize;           // size of file in bytes
    private short firstBlock;         // first block index
    private LinkedList<Short> blockChain; // all blocks belonging to this file

    public FEntry(String filename, short filesize, short firstBlock) {
        if (filename.length() > 11)
            throw new IllegalArgumentException("Filename cannot be longer than 11 characters.");

        this.filename = filename;
        this.filesize = filesize;
        this.firstBlock = firstBlock;
        this.blockChain = new LinkedList<>();

        if (firstBlock >= 0)
            this.blockChain.add(firstBlock);
    }

    // ==================== Getters ====================
    public String getFilename() { return filename; }
    public int getSize() { return filesize; }
    public short getFilesize() { return filesize; }
    public short getFirstBlock() { return firstBlock; }
    public LinkedList<Short> getBlockChain() { return blockChain; }

    // ==================== Setters ====================
    public void setFilename(String filename) {
        if (filename.length() > 11)
            throw new IllegalArgumentException("Filename cannot be longer than 11 characters.");
        this.filename = filename;
    }

    public void setFilesize(short filesize) {
        if (filesize < 0)
            throw new IllegalArgumentException("Filesize cannot be negative.");
        this.filesize = filesize;
    }

    public void setSize(int size) {
        if (size < 0 || size > Short.MAX_VALUE)
            throw new IllegalArgumentException("Invalid file size value.");
        this.filesize = (short) size;
    }

    public void setFirstBlock(int firstBlock) {
        if (firstBlock < -1)
            throw new IllegalArgumentException("Invalid block index.");
        this.firstBlock = (short) firstBlock;

        if (firstBlock >= 0) {
            if (blockChain.isEmpty())
                blockChain.add((short) firstBlock);
            else
                blockChain.set(0, (short) firstBlock);
        } else {
            blockChain.clear();
        }
    }

    // ==================== LinkedList Helpers ====================
    public void addBlock(short blockIndex) {
        if (!blockChain.contains(blockIndex))
            blockChain.add(blockIndex);
    }

    public void clearBlocks() {
        blockChain.clear();
        firstBlock = -1;
        filesize = 0;
    }

    public int getBlockCount() {
        return blockChain.size();
    }

    // ==================== Utility ====================
    @Override
    public String toString() {
        return "FEntry{" +
                "filename='" + filename + '\'' +
                ", filesize=" + filesize +
                ", firstBlock=" + firstBlock +
                ", blockChain=" + blockChain +
                '}';
    }
}