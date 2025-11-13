package ca.concordia.filesystem;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

// Lock manager class to impose order of lock acquisition
public class LockManager {
    private final ReentrantLock globalLock = new ReentrantLock();
    private final ConcurrentHashMap<Integer, ReentrantLock> fileLocks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, ReentrantLock> blockLocks = new ConcurrentHashMap<>();

    public ReentrantLock getGlobalLock() {
        return globalLock;
    }

    public ReentrantLock fileLock(int inodeIndex) {
        return fileLocks.computeIfAbsent(inodeIndex, k -> new ReentrantLock());
    }

    public ReentrantLock blockLock(int blockNumber) {
        return blockLocks.computeIfAbsent(blockNumber, k -> new ReentrantLock());
    }

    // Ordering the lock based on rank, instance key and lock
   public static final class LockHandle {
        public final int rank;
        public final long instanceKey;
        public final ReentrantLock lock;
        public LockHandle(int rank, long instanceKey, ReentrantLock lock) {
            this.rank = rank;
            this.instanceKey = instanceKey;
            this.lock = lock;
        }
   }

   // Acquiring the lowest ranking locks first
   public void acquireOrdered(List<LockHandle> handles) {
        handles.sort(Comparator.comparingInt((LockHandle handle) -> handle.rank).thenComparingLong(handle -> handle.instanceKey));

        for (LockHandle handle : handles) {
            handle.lock.lock();
        }
   }

   // Releasing the highest ranking locks first
   public void releaseOrdered(List<LockHandle> handles) {
        ListIterator<LockHandle> iterator = handles.listIterator(handles.size());
        while (iterator.hasPrevious()) {
            iterator.previous().lock.unlock();
        }
   }
}
