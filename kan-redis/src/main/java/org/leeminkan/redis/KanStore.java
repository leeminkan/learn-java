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

    public MemorySegment get(String key) {
        return store.get(key);
    }

    /**
     * Allocates off-heap memory for the value and stores the pointer.
     */
    public void set(String key, byte[] valueBytes) {
        // 1. Allocate native memory (malloc)
        MemorySegment nativeMem = offHeapArena.allocate(valueBytes.length);

        // 2. Copy the Java byte array INTO the native memory
        // Heap -> Off-Heap copy
        MemorySegment.copy(valueBytes, 0, nativeMem, ValueLayout.JAVA_BYTE, 0, valueBytes.length);

        System.out.println("DEBUG: Allocated " + valueBytes.length + " bytes at Off-Heap Address: " + nativeMem.address());
        // 3. Store the pointer
        store.put(key, nativeMem);
    }

    public boolean cas(String key, String expectedValue, byte[] newValueBytes) {
        MemorySegment currentSeg = store.get(key);

        // If key doesn't exist, we can't Compare (unless expected is null)
        if (currentSeg == null) {
            if (expectedValue == null) {
                set(key, newValueBytes);
                return true;
            }
            return false;
        }

        // READ current value from Off-Heap to compare
        // Note: This copy is the cost of validation.
        byte[] currentBytes = currentSeg.toArray(ValueLayout.JAVA_BYTE);
        String currentStr = new String(currentBytes, StandardCharsets.UTF_8);

        if (currentStr.equals(expectedValue)) {
            set(key, newValueBytes); // This allocates NEW memory and updates the map
            return true;
        }

        return false;
    }
}