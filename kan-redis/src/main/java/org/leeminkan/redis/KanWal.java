package org.leeminkan.redis;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class KanWal {
    private final FileChannel logChannel;

    public KanWal(String filePath) throws IOException {
        this.logChannel = FileChannel.open(Paths.get(filePath),
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.READ); // Read needed for recovery
    }

    /**
     * Appends a SET command to the log.
     * Format: [CMD=2][KeyLen][Key][ValLen][Value]
     */
    public void writeSet(String key, byte[] value) {
        try {
            byte[] keyBytes = key.getBytes();
            int totalSize = 1 + 4 + keyBytes.length + 4 + value.length;

            ByteBuffer buffer = ByteBuffer.allocate(totalSize);
            buffer.put((byte) 2); // SET Command
            buffer.putInt(keyBytes.length);
            buffer.put(keyBytes);
            buffer.putInt(value.length);
            buffer.put(value);

            buffer.flip();

            // Synchronized to ensure sequential writes if we had multiple threads
            synchronized (this) {
                logChannel.write(buffer);
                // force(false) flushes to OS cache. force(true) flushes to disk hardware.
                logChannel.force(false);
            }
        } catch (IOException e) {
            throw new RuntimeException("WAL Write Failed", e);
        }
    }

    // For CAS, we technically only need to log the *Resulting* SET if it succeeded.
    // In a real DB, we might log the logic, but here we just log the final state.

    /**
     * Reads the log file and populates the store.
     */
    public void replay(KanStore store) throws IOException {
        System.out.println("Replaying WAL...");

        // Move to start of file
        logChannel.position(0);

        ByteBuffer header = ByteBuffer.allocate(5); // 1 byte type + 4 byte len

        while (logChannel.read(header) > 0) {
            header.flip();
            if (header.remaining() < 5) break; // End of file or partial write

            byte type = header.get();
            if (type != 2) {
                // For this MVP, we only implemented logging for SET.
                // Skip or handle other types if you add them.
                System.out.println("Skipping unknown log entry type: " + type);
                continue;
            }

            int keyLen = header.getInt();
            header.clear();

            // Read Key
            ByteBuffer keyBuf = ByteBuffer.allocate(keyLen);
            logChannel.read(keyBuf);
            String key = new String(keyBuf.array());

            // Read Value Len
            ByteBuffer valLenBuf = ByteBuffer.allocate(4);
            logChannel.read(valLenBuf);
            valLenBuf.flip();
            int valLen = valLenBuf.getInt();

            // Read Value
            ByteBuffer valBuf = ByteBuffer.allocate(valLen);
            logChannel.read(valBuf);
            byte[] value = valBuf.array();

            // Restore to memory (WITHOUT writing to WAL again!)
            store.restore(key, value);
        }

        // Move position back to end for appending new writes
        logChannel.position(logChannel.size());
        System.out.println("Replay Complete.");
    }

    public void close() throws IOException {
        logChannel.close();
    }
}