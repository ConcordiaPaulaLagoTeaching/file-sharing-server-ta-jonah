//Represents each block of data belonging to a file (like FAT filesystem)
package ca.concordia.filesystem.datastructures;

public class FNode {

    private int blockIndex;
    private int next;

    public FNode(int blockIndex) {
        this.blockIndex = blockIndex;
        this.next = -1;
    }
    
    // Getters 

    //getNext for traversing the linked list of blocks
    public int getNext() {
        return next;
    }

    //getBlock to know where on disk to read from 
    public int getBlockIndex() {
        return blockIndex;
    }

    // Setters

    //setBlockIndex to track which blocks are available (free or used)
    public void setBlockIndex(int blockIndex) {
        this.blockIndex = blockIndex;
    }

    // setNext to link blocks together
    public void setNext(int next) {
        this.next = next;
    }
}