# ADR-001: Adaptive Buffer Resizing Strategy

## Status
Accepted

## Context
The server currently initializes a fixed-size `ByteBuffer` (4KB) for every client connection.
If a client sends a command payload larger than 4KB (e.g., `SET huge_key [5KB_VALUE]`), the following occurs:
1. The buffer fills to capacity.
2. The protocol parser waits for more data.
3. The server attempts to read more data into the full buffer.
4. The server enters a busy loop or stops processing that client, effectively causing a Denial of Service (DoS) for that connection.

## Decision
We will implement **Adaptive Buffer Resizing** using a "Double-and-Copy" strategy, with a hard memory limit.

### Logic
1. If the buffer is full AND no commands were successfully processed in the current cycle (`processedAny == false`), we infer the buffer is too small for the pending command.
2. We allocate a new `ByteBuffer` with **double** the capacity of the current one.
3. We copy the existing partial data to the new buffer.
4. We replace the old buffer in the connection map.

### Constraints
* **Initial Size:** 4KB (Optimized for typical small KV workloads).
* **Growth Factor:** 2x (Exponential growth).
* **Hard Limit:** 10MB (`MAX_FRAME_SIZE`). If a command exceeds this, the connection is closed to prevent Heap Out-Of-Memory (OOM) attacks.

## Consequences
### Positive
* Supports large keys/values transparently to the client.
* Prevents server hangs on large payloads.
* "Hard Limit" protects the JVM heap from malicious allocation attacks.

### Negative
* **Memory Pressure:** A single client sending 9MB payloads will cause a 16MB buffer allocation (due to doubling power-of-2 sizing).
* **GC Impact:** Resizing involves allocating large arrays on the Heap, which may trigger Garbage Collection. (Mitigated: We only resize when absolutely necessary).

## Alternatives Considered
* **Streaming (Zero-Copy):** Directly copying bytes from `SocketChannel` to Off-Heap memory.
    * *Rejected for now:* Adds significant complexity to the `KanProtocol` state machine. Resizing is sufficient for the MVP limit of 10MB.