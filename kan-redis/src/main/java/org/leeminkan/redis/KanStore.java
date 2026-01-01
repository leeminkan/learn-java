package org.leeminkan.redis;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

public class KanStore {

    // Global allocator for off-heap memory.
    // "ofShared" allows multiple threads to read/write these segments safely.
    private final Arena offHeapArena = Arena.ofShared();

    // Store the POINTER (MemorySegment) not the data itself.
    private final ConcurrentHashMap<String, MemorySegment> store = new ConcurrentHashMap<>();

    private final KanWal wal;

    public KanStore(KanWal wal) {
        this.wal = wal;
    }

    public MemorySegment get(String key) {
        return store.get(key);
    }

    // Helper to avoid duplicating the allocation logic
    private void putInMemory(String key, byte[] valueBytes) {
        // 1. Allocate native memory (malloc)
        MemorySegment nativeMem = offHeapArena.allocate(valueBytes.length);

        // 2. Copy the Java byte array INTO the native memory
        // Heap -> Off-Heap copy
        MemorySegment.copy(valueBytes, 0, nativeMem, ValueLayout.JAVA_BYTE, 0, valueBytes.length);

        System.out.println("DEBUG: Allocated " + valueBytes.length + " bytes at Off-Heap Address: " + nativeMem.address());
        // 3. Store the pointer
        store.put(key, nativeMem);
    }

    /**
     * Allocates off-heap memory for the value and stores the pointer.
     */
    public void set(String key, byte[] valueBytes) {
        // 1. Write to Disk FIRST (Durability)
        if (wal != null) {
            wal.writeSet(key, valueBytes);
        }

        // 2. Update Memory
        putInMemory(key, valueBytes);
    }

    // Restore (Called by WAL Replay)
    public void restore(String key, byte[] valueBytes) {
        putInMemory(key, valueBytes); // Skip WAL write
    }

    public boolean cas(String key, String expectedValue, byte[] newValueBytes) {
        MemorySegment currentSeg = store.get(key);
        if (currentSeg == null) {
            if (expectedValue == null) {
                set(key, newValueBytes); // set() already handles WAL
                return true;
            }
            return false;
        }

        byte[] currentBytes = currentSeg.toArray(ValueLayout.JAVA_BYTE);
        String currentStr = new String(currentBytes, StandardCharsets.UTF_8);

        if (currentStr.equals(expectedValue)) {
            // Log the NEW value as a standard SET operation
            wal.writeSet(key, newValueBytes);
            putInMemory(key, newValueBytes);
            return true;
        }
        return false;
    }
}