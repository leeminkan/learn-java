# ADR 001: Synchronous Audit Strategy for Core Ledger

* **Status:** Accepted
* **Date:** 2025-12-24
* **Deciders:** [Your Name]
* **Technical Owner:** Account Service Team

## Context and Problem Statement

The `account-service` manages the core banking ledger. For regulatory compliance (SOX, GDPR) and customer support, every change to an account balance must be strictly traceable. We need to answer: *Who changed the balance, when did they change it, and what was the value before the change?*

The solution must guarantee that **no transaction occurs without an audit log** (Atomicity). If the audit log fails to write, the transaction must roll back.

## Decision Drivers

* **Data Integrity:** The audit trail must be strictly consistent with the operational data (ACID).
* **Complexity:** The team is currently small (1 developer), avoiding excessive infrastructure overhead.
* **Latency:** The current TPS (Transactions Per Second) requirement is moderate (< 1000 TPS).
* **Query Patterns:** Audit data is primarily accessed by ID for specific investigations, not for broad analytics.

## Considered Options

1.  **Hibernate Envers (Synchronous)** - Write shadow rows in the same transaction.
2.  **Kafka Event Sourcing (Asynchronous)** - Reconstruct state from `TransactionCreatedEvent`.
3.  **Change Data Capture / CDC (Debezium)** - Mine database transaction logs (WAL) to generate audit events.

## Decision Outcome

Chosen option: **Hibernate Envers**.

We have chosen to implement **Synchronous Auditing** using Hibernate Envers for the initial release of the Ledger.

### Positive Consequences
* **Simplicity:** Requires zero additional infrastructure (no Kafka Connect cluster to manage).
* **Guaranteed Consistency:** It uses the same database transaction. It is impossible to have a balance update without a corresponding audit record.
* **Development Speed:** Implemented via a single `@Audited` annotation.

### Negative Consequences
* **Write Amplification:** Every `UPDATE` results in two `INSERT` operations (one to the log, one to the shadow table), increasing DB I/O.
* **Coupling:** The application logic is tightly coupled to the audit schema.
* **Scalability Limit:** At very high scale (e.g., Tier-1 Bank volume), the single database writer becomes a bottleneck.

## Future Scale Strategy (Migration Path)

As the system scales to handle high-frequency trading or massive concurrent users (NAB Scale), we acknowledge that Envers will become a bottleneck due to Write Amplification.

**Target Architecture (Future State):**
We will migrate to **Log-Based Change Data Capture (CDC)** using **Debezium**.

### Why Debezium for Future State?
1.  **Zero Latency Impact:** Moves audit processing out of the critical path of the user request.
2.  **Decoupling:** The Operational DB (Postgres) does not know about the Audit DB (e.g., Elasticsearch/Snowflake).
3.  **Resilience:** If the Audit consumer is down, the operational ledger continues to function; the consumer catches up later.

### Visual Comparison

```mermaid
graph TD
    subgraph "Current State (Envers)"
        App[Java App]
        DB[(Postgres)]
        
        App -->|Tx: Update Balance| DB
        App -->|Tx: Insert Audit| DB
        style App stroke:#f66,stroke-width:2px
    end

    subgraph "Future State (CDC)"
        App2[Java App]
        DB2[(Postgres)]
        WAL[Write Ahead Log]
        Deb[Debezium Connector]
        Kafka[Kafka Audit Topic]
        
        App2 -->|Tx: Update Balance| DB2
        DB2 -.->|Stream| WAL
        WAL -->|Read| Deb
        Deb -->|Publish| Kafka
        style Deb stroke:#6f6,stroke-width:2px
    end