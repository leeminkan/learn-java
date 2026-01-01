# Setup & Usage

## Prerequisites
* Java 21+
* Maven

## Build
```bash
mvn clean package

```

## Run Server

The server starts on port **6379** by default.

```bash
# Run directly from source
mvn exec:java -Dexec.mainClass="org.leeminkan.redis.KanRedisServer"

```

## Run Test Client

A simple client to demonstrate SET, GET, and CAS operations.

```bash
mvn exec:java -Dexec.mainClass="org.leeminkan.redis.SimpleClientTest"

```

## Monitoring (JMX)

Kan-Redis is production-ready with JMX hooks.

1. Start the server.
2. Open `jconsole` or `jvisualvm`.
3. Connect to the `KanRedisServer` process.
4. Navigate to **MBeans** -> `org.leeminkan.redis`.
5. Metrics available:
* `ConnectedClients`
* `TotalCommandsProcessed`
* `OffHeapMemoryUsage` (Bytes)
