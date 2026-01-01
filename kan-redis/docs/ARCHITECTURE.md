# Kan-Redis Architecture

## Overview
Kan-Redis is a simplified, high-performance Key-Value store built on **Java 21**. It abandons traditional blocking I/O models in favor of a single-threaded, event-driven architecture, similar to Redis or Node.js.

## Key Engineering Decisions

### 1. Non-Blocking I/O (NIO)
* **Constraint:** Traditional `Thread-Per-Request` models fail at scale (C10K problem) due to context switching and stack memory overhead.
* **Solution:** Kan-Redis uses a single `Selector` event loop.
* **Mechanism:**
    * The server handles all connections on a single thread.
    * It multiplexes `OP_ACCEPT` and `OP_READ` events.
    * **Buffer Management:** Implements `compact()` logic to handle TCP fragmentation (partial packets) and coalescing (multiple commands in one packet).

### 2. Off-Heap Memory (Project Panama)
* **Constraint:** Storing millions of objects on the Java Heap causes massive Garbage Collection (GC) pauses ("Stop-the-World").
* **Solution:** Java 21 Foreign Function & Memory API.
* **Mechanism:**
    * Values are stored in **Native Memory** (off-heap) using `Arena` and `MemorySegment`.
    * The Java Heap only stores a lightweight reference (Pointer) to the data.
    * **Result:** The GC does not scan the dataset, allowing for predictable low-latency performance regardless of DB size.

### 3. Durability (Write-Ahead Log)
* **Constraint:** In-memory stores lose data on crash/restart.
* **Solution:** Append-Only Log (WAL).
* **Mechanism:**
    * All mutating operations (`SET`, successful `CAS`) are appended to `kan-data.log` via `FileChannel`.
    * Uses `force(false)` to flush to OS cache (balancing durability with performance).
    * On startup, the server performs a sequential read of the log to reconstruct the memory state.

### 4. Concurrency Control (Banking Grade)
* **Constraint:** In distributed systems (e.g., banking), "Lost Updates" are a critical failure mode.
* **Solution:** Optimistic Locking (CAS - Compare And Swap).
* **Mechanism:**
    * Clients provide `(Key, ExpectedValue, NewValue)`.
    * The server atomically verifies the current state matches `ExpectedValue` before updating.
    * This enables safe concurrent transactions without heavy pessimistic locks.