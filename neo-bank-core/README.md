# 🏦 Neo-Bank Core (Java Microservices)

A "cloud-native" banking ledger system built with **Java 23, Spring Boot 3, and Apache Kafka**.
This project demonstrates an **Event-Driven Architecture** for handling high-volume financial transactions with eventual consistency and fault tolerance.

## 🚀 Architecture Overview

The system is composed of two primary microservices and a distributed infrastructure:

* **Account Service:** The "Source of Truth" for user balances. Uses Optimistic Locking to prevent double-spending.
* **Transaction Service:** Handles money movement requests. Uses the **Saga Pattern (Choreography)** to ensure data consistency across services without distributed transactions (2PC).
* **Infrastructure:** * **Kafka:** Asynchronous messaging backbone.
    * **PostgreSQL:** Database-per-service pattern.
    * **Zipkin:** Distributed Tracing for observability.

## 🛠 Tech Stack

* **Language:** Java 23 (Eclipse Temurin)
* **Framework:** Spring Boot 3.3.0
* **Messaging:** Apache Kafka (Producer/Consumer)
* **Database:** PostgreSQL 15
* **Resilience:** Resilience4j (Circuit Breaker)
* **Observability:** Micrometer Tracing + Zipkin
* **Build Tool:** Maven (Multi-Module Monorepo)
* **Containerization:** Docker & Docker Compose

## ✨ Key Features Implemented

1.  **Event-Driven Communication:** Decoupled services communicating via `transaction-events` and `transaction-results` topics.
2.  **Saga Pattern:** Implemented Choreography-based Sagas to handle distributed transactions (Pending -> Success/Fail).
3.  **Optimistic Locking:** Used `@Version` in JPA to handle concurrent balance updates safely.
4.  **Resilience:** Implemented a **Circuit Breaker** on the Account Validation HTTP call to prevent cascading failures.
5.  **Distributed Tracing:** Full request visibility across services using Trace IDs injected into Kafka headers.

## 📦 How to Run

### Prerequisites
* Docker & Docker Compose
* Java 23 (Optional, only for local dev)

### 1. Start the Infrastructure & Services
Run the entire stack (Postgres, Kafka, Zookeeper, Zipkin, and Java Apps) with one command:

```bash
docker-compose up -d --build