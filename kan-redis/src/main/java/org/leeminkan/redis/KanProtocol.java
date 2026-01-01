package org.leeminkan.redis;

import java.nio.ByteBuffer;
import java.nio.BufferUnderflowException;
import java.nio.charset.StandardCharsets;

public class KanProtocol {

    private final KanStore store;

    public KanProtocol(KanStore store) {
        this.store = store;
    }

    /**
     * Tries to process a command from the buffer.
     * @return true if a command was successfully processed.
     * false if there wasn't enough data (fragmentation).
     */
    public boolean process(ByteBuffer buffer, ByteBuffer responseBuffer) {
        if (buffer.remaining() < 1) return false; // Not even a command byte

        // Mark the current position. If we run out of data mid-command,
        // we rewind to this mark so we can try again later when more data arrives.
        buffer.mark();

        try {
            byte cmdByte = buffer.get();
            // System.out.println("DEBUG: Attempting Command: " + cmdByte);

            CommandType type = CommandType.fromByte(cmdByte);
            if (type == null) {
                // Determine if it's garbage data or just not enough data?
                // For simplicity, we assume garbage creates an error response.
                writeError(responseBuffer, "Unknown Command: " + cmdByte);
                return true; // We handled it (by erroring), so return true
            }

            switch (type) {
                case GET -> handleGet(buffer, responseBuffer);
                case SET -> handleSet(buffer, responseBuffer);
                case CAS -> handleCas(buffer, responseBuffer);
            }
            return true; // Success!

        } catch (BufferUnderflowException e) {
            // CRITICAL: We ran out of bytes in the middle of a command.
            // Reset position to where we started (buffer.mark())
            buffer.reset();
            // System.out.println("DEBUG: Partial command received. Waiting for more data...");
            return false;
        }
    }

    // ... (Keep handleGet, handleSet, handleCas, writeError exactly as they were) ...
    // ... (Keep readString exactly as it was) ...

    // RE-PASTE methods just in case:
    private void handleSet(ByteBuffer in, ByteBuffer out) {
        String key = readString(in);
        String value = readString(in);
        store.set(key, value);
        out.putInt(1);
        out.put((byte) 1);
    }

    private void handleGet(ByteBuffer in, ByteBuffer out) {
        String key = readString(in);
        String value = store.get(key);
        if (value == null) {
            out.putInt(0);
        } else {
            byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
            out.putInt(bytes.length);
            out.put(bytes);
        }
    }

    private void handleCas(ByteBuffer in, ByteBuffer out) {
        String key = readString(in);
        String expected = readString(in);
        String newValue = readString(in);
        boolean success = store.cas(key, expected, newValue);
        out.putInt(1);
        out.put((byte) (success ? 1 : 0));
    }

    private String readString(ByteBuffer buffer) {
        int len = buffer.getInt(); // Can throw BufferUnderflow
        if (len == 0) return null;
        if (buffer.remaining() < len) throw new BufferUnderflowException(); // Ensure full string is there
        byte[] bytes = new byte[len];
        buffer.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private void writeError(ByteBuffer out, String msg) {
        out.putInt(-1);
    }
}