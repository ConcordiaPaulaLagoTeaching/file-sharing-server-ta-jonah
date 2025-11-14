package ca.concordia.filesystem;

// Enum to determine client lock levels
public enum LockLevel { GLOBAL(0), FILE(1), BLOCK(2);
    private final int rank;
    LockLevel(int rank) {
        this.rank = rank;
    }
    public int getRank() {
        return rank;
    }
}