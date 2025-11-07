package ca.concordia.filesystem.datastructures;

import java.util.LinkedList;

public class FEntry { //

    private String filename;
    private short filesize;
    private short firstBlock; // Pointers to data blocks

    public FEntry(String filename, short filesize, short firstblock) throws IllegalArgumentException{
        //Check filename is max 11 bytes long
        if (filename.length() > 11) {
            throw new IllegalArgumentException("Filename cannot be longer than 11 characters.");
        }
        this.filename = filename;
        this.filesize = filesize;
        this.firstBlock = firstblock;
    }

    //default constructorbloading frm disk
    public FEntry(){

        this.filename =" ";
        this.filesize = 0;
        this.firstBlock = -1;
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
    public void setFirstBlock (short firstBlock){
        this.firstBlock =firstBlock;
    }
    // Helper method to check if this FEntry is in use

    public boolean isInUse() {
        return filename != null && !filename.isEmpty();
    }
    
    // Method to mark as free/unused
    public void markFree() {
        this.filename = "";
        this.filesize = 0;
        this.firstBlock = -1;
    }
    
    // Calculate the size in bytes that this FEntry will occupy on disk
    public static int getDiskSize() {
        return 15; // 11 bytes (filename) + 2 bytes (size) + 2 bytes (firstBlock) = 15 bytes
    }




}
