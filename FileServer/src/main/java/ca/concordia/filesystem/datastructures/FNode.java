package ca.concordia.filesystem.datastructures;

public class FNode { //fnode

    private int blockIndex;
    private int next;

    public FNode(int blockIndex) {
        this.blockIndex = blockIndex;
        this.next = -1;
    }
}
