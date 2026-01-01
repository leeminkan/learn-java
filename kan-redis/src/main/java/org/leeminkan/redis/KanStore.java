package org.leeminkan.redis;

import java.util.concurrent.ConcurrentHashMap;

public class KanStore {

    // In Phase 3, we will change <String, String> to <MemorySegment, MemorySegment>
    private final ConcurrentHashMap<String, String> store = new ConcurrentHashMap<>();

    public String get(String key) {
        return store.get(key);
    }

    public void set(String key, String value) {
        store.put(key, value);
    }

    /**
     * Compare-And-Swap (CAS) - The "Banking" Operation.
     * Only updates the key if the current value matches the expected value.
     * @return true if update succeeded, false if data changed in background.
     */
    public boolean cas(String key, String expectedValue, String newValue) {
        // If key doesn't exist, replace logic usually fails.
        // For this MVP, we treat "null" expectedValue as "putIfAbsent"
        if (expectedValue == null) {
            return store.putIfAbsent(key, newValue) == null;
        }
        return store.replace(key, expectedValue, newValue);
    }
}