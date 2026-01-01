package org.leeminkan.redis;

import java.lang.foreign.MemorySegment;
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

    private void handleSet(ByteBuffer in, ByteBuffer out) {
        String key = readString(in);

        // Read value as raw bytes, don't turn into String yet!
        byte[] valueBytes = readByteArray(in);

        store.set(key, valueBytes);

        out.putInt(1);
        out.put((byte) 1);
    }

    private void handleGet(ByteBuffer in, ByteBuffer out) {
        String key = readString(in);
        MemorySegment valueSeg = store.get(key);

        if (valueSeg == null) {
            out.putInt(0);
        } else {
            long size = valueSeg.byteSize();
            out.putInt((int) size);

            // OPTIMIZED: Copy from Off-Heap (Segment) directly to On-Heap (ByteBuffer)
            // MemorySegment.asByteBuffer() creates a view we can put()
            ByteBuffer view = valueSeg.asByteBuffer();
            out.put(view);
        }
    }

    private void handleCas(ByteBuffer in, ByteBuffer out) {
        String key = readString(in);
        String expected = readString(in); // We still need String for comparison logic
        byte[] newValueBytes = readByteArray(in); // New value stays raw bytes

        boolean success = store.cas(key, expected, newValueBytes);

        out.putInt(1);
        out.put((byte) (success ? 1 : 0));
    }

    // NEW HELPER: Reads bytes without converting to String
    private byte[] readByteArray(ByteBuffer buffer) {
        int len = buffer.getInt();
        if (len == 0) return new byte[0];
        if (buffer.remaining() < len) throw new java.nio.BufferUnderflowException();

        byte[] bytes = new byte[len];
        buffer.get(bytes);
        return bytes;
    }

    // readString stays the same (it just wraps readByteArray usually, but keeping it distinct is fine)
    private String readString(ByteBuffer buffer) {
        byte[] bytes = readByteArray(buffer);
        if (bytes.length == 0) return null;
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private void writeError(ByteBuffer out, String msg) {
        out.putInt(-1);
    }
}