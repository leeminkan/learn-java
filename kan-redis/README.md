# Kan-Redis 🚀

A high-performance, non-blocking **Key-Value Store** built from scratch in **Java 21**.

This project demonstrates deep JVM expertise by implementing a database without external frameworks (Spring/Netty). It focuses on **Zero-GC architectures**, **Low-Level Networking**, and **Data Safety**.

## 🌟 Senior-Level Features
* **Custom NIO Engine:** Selector-based event loop handling concurrent connections on a single thread.
* **Off-Heap Storage:** Uses **Java 21 Foreign Memory API** to store data outside the Java Heap, eliminating GC pauses.
* **Banking-Grade Safety:** Implements **Optimistic Locking (CAS)** and **Write-Ahead Logging (WAL)** for consistency and durability.
* **Observability:** Built-in **JMX Metrics** for production monitoring.

## 📚 Documentation
* [Architecture Decisions](docs/ARCHITECTURE.md) (NIO, Off-Heap, WAL)
* [Binary Protocol Spec](docs/PROTOCOL.md)
* [Setup & Monitoring](docs/SETUP.md)
* [Roadmap](docs/ROADMAP.md)

## 🛠 Tech Stack
* **Language:** Java 21 (LTS)
* **Core:** Java NIO, Foreign Function & Memory API (Project Panama)
* **Build:** Maven
* **Dependencies:** None (Core Java only)