# Product Roadmap

This document outlines the future development plan for Kan-Redis, moving from a single-node persistent store to a distributed, secure caching cluster.

## ✅ Completed (v1.0 - The MVP)
- [x] **NIO Networking:** Non-blocking accept/read loop.
- [x] **Binary Protocol:** Custom TLV format for efficient parsing.
- [x] **Off-Heap Storage:** Zero-GC memory management via FFM API.
- [x] **Persistence:** Append-only Write-Ahead Log (WAL).
- [x] **Concurrency:** Optimistic Locking (CAS) implementation.
- [x] **Observability:** JMX Metrics export.

---

## 🚧 Short Term (v1.1 - The "Cache" Features)
*Focus: making Kan-Redis a viable alternative to Memcached.*

### 1. Key Expiration (TTL)
* **Goal:** Allow keys to auto-delete after $N$ seconds.
* **Strategy:** Use a `DelayQueue` or a "Passive Expire" approach (check TTL on read) + "Active Expire" (random sampling loop), similar to Redis.

### 2. Eviction Policies (LRU)
* **Goal:** Prevent Out-Of-Memory (OOM) crashes when the Arena is full.
* **Strategy:** Implement Least Recently Used (LRU) eviction to remove old keys when memory usage crosses a threshold (e.g., 80%).

---

## 🔮 Medium Term (v1.2 - The "Banking" Features)
*Focus: Security and High Availability (HA), critical for Fintech.*

### 3. Master-Replica Replication
* **Goal:** Data redundancy.
* **Strategy:** Implement a `PSYNC` command where a Replica connects to Master, receives the initial RDB snapshot, and then streams the WAL updates.

### 4. TLS/SSL Support
* **Goal:** Encrypt data in transit.
* **Strategy:** Wrap the `SocketChannel` with an `SSLEngine` to secure the wire protocol against packet sniffing.

### 5. Authentication (ACLs)
* **Goal:** Restrict access.
* **Strategy:** Implement an `AUTH user password` command and restrict `FLUSHALL` or `KEYS` commands to admin users only.

---

## 🚀 Long Term (v2.0 - Performance at Scale)

### 6. io_uring Integration
* **Goal:** Linux-native asynchronous I/O.
* **Strategy:** Replace Java NIO `Selector` with `io_uring` via the Foreign Function API for even lower latency and higher throughput on Linux kernels.

### 7. Sharding
* **Goal:** Horizontal scaling.
* **Strategy:** Implement client-side partitioning (Consistent Hashing) to distribute keys across multiple Kan-Redis instances.