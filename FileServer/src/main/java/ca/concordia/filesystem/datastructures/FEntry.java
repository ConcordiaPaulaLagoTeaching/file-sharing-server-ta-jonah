package ca.concordia.filesystem.datastructures;

public class FEntry {

    private String filename;
    private short filesize;
    private short firstBlock; // Pointers to data blocks

    public FEntry(String filename, short filesize, short firstblock) throws IllegalArgumentException {
        if (filename.length() > 11) {
            throw new IllegalArgumentException("Filename cannot be longer than 11 characters.");
        }
        this.filename = filename;
        this.filesize = filesize;
        this.firstBlock = firstblock;
    }

    // Check if entry is free (unused)
    public boolean isFree() {
        return filename == null || filename.isEmpty();
    }

    // Getters and Setters
    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        if (filename.length() > 11) {
            throw new IllegalArgumentException("Filename cannot be longer than 11 characters.");
        }
        this.filename = filename;
    }

    public short getFilesize() {
        return filesize;
    }

    public void setFilesize(short filesize) {
        if (filesize < 0) {
            throw new IllegalArgumentException("Filesize cannot be negative.");
        }
        this.filesize = filesize;
    }

    public short getFirstBlock() {
        return firstBlock;
    }

    public void setFirstBlock(short blockIndex) {
        this.firstBlock = blockIndex;
    }
}
