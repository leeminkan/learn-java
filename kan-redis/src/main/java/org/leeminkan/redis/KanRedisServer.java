package org.leeminkan.redis;

import org.leeminkan.redis.jmx.KanMonitor;

import javax.management.ObjectName;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * KanRedisServer
 * A non-blocking, event-driven server using Java NIO.
 * Designed to handle C10K (10,000 concurrent connections) on a single thread.
 */
public class KanRedisServer {

    // Defined in ADR-001: Protection against OOM attacks
    private static final int MAX_FRAME_SIZE = 10 * 1024 * 1024; // 10 MB

    private final int port;
    private Selector selector;
    private ServerSocketChannel serverChannel;
    private boolean isRunning = true;

    // Dependencies
    private KanStore store;
    private KanProtocol protocol;

    public static final AtomicInteger connectedClients = new AtomicInteger(0);

    // A map to store data associated with a connection (buffers, state)
    // In a real Netty implementation, this would be the 'ChannelContext'
    private final ConcurrentHashMap<SocketChannel, ByteBuffer> clientBuffers = new ConcurrentHashMap<>();

    public KanRedisServer(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        // Initialize dependencies (WAL, Store, Protocol)
        // 1. Initialize WAL
        KanWal wal = new KanWal("kan-data.log");

        // 2. Initialize Store with WAL
        store = new KanStore(wal);

        // 3. Replay Old Data
        wal.replay(store);

        // 4. Initialize Protocol
        protocol = new KanProtocol(store);

        // JMX Registration
        try {
            KanMonitor monitor = new KanMonitor(store);
            ObjectName name = new ObjectName("org.leeminkan.redis:type=KanMonitor");
            ManagementFactory.getPlatformMBeanServer().registerMBean(monitor, name);
            System.out.println("JMX Monitor registered. Connect using JConsole.");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Network Setup
        selector = Selector.open();
        serverChannel = ServerSocketChannel.open();
        serverChannel.bind(new InetSocketAddress(port));
        // CRITICAL: Must be non-blocking to work with Selector
        serverChannel.configureBlocking(false);
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        System.out.println("Kan-Redis listening on port " + port + "...");
        runEventLoop();
    }

    private void runEventLoop() {
        while (isRunning) {
            try {
                // Blocks until at least one event occurs
                selector.select();

                // Get the set of keys (events) that are ready
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iter = selectedKeys.iterator();

                while (iter.hasNext()) {
                    SelectionKey key = iter.next();

                    if (key.isAcceptable()) {
                        handleAccept(key);
                    } else if (key.isReadable()) {
                        handleRead(key);
                    }

                    // Crucial: Remove the key from the iterator to prevent processing it twice
                    iter.remove();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleAccept(SelectionKey key) throws IOException {
        ServerSocketChannel server = (ServerSocketChannel) key.channel();
        SocketChannel client = server.accept();
        client.configureBlocking(false);

        // Register this new client for READ events (we want to know when they send data)
        client.register(selector, SelectionKey.OP_READ);

        // Allocate a buffer for this specific client (4KB)
        clientBuffers.put(client, ByteBuffer.allocate(4096));

        System.out.println("New Connection: " + client.getRemoteAddress());
        connectedClients.incrementAndGet();
    }

    private void handleRead(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        ByteBuffer buffer = clientBuffers.get(client);

        int bytesRead = client.read(buffer);
        if (bytesRead == -1) {
            closeConnection(client);
            return;
        }

        if (bytesRead > 0) {
            buffer.flip(); // Switch to READ mode

            // Temp buffer for responses
            ByteBuffer responseBuffer = ByteBuffer.allocate(4096);
            boolean processedAny = false; // Tracks if we made progress

            // Loop to process all complete commands in the buffer
            while (protocol.process(buffer, responseBuffer)) {
                processedAny = true;

                // If a command was processed, we might have a response to send
                if (responseBuffer.position() > 0) {
                    responseBuffer.flip();
                    while (responseBuffer.hasRemaining()) {
                        client.write(responseBuffer);
                    }
                    responseBuffer.clear(); // Clear for next command's response
                }
            }

            // Buffer Management Strategy (ADR-001)
            // If we processed NO commands, and the buffer is completely full,
            // it means the current command is larger than the buffer capacity.
            // Note: buffer.remaining() == capacity() implies the 'mark' is at 0 and 'limit' is at capacity.
            if (!processedAny && buffer.remaining() == buffer.capacity()) {

                // Check Safety Limit
                if (buffer.capacity() * 2 > MAX_FRAME_SIZE) {
                    System.err.println("Error: Client " + client.getRemoteAddress() + " exceeded max frame size.");
                    closeConnection(client);
                    return;
                }

                // Resize: Double the capacity
                ByteBuffer newBuffer = ByteBuffer.allocate(buffer.capacity() * 2);

                // Copy the partial data from the old buffer to the new one
                newBuffer.put(buffer);

                // Update map
                clientBuffers.put(client, newBuffer);

                System.out.println("ADR-001: Resized buffer for " + client.getRemoteAddress() +
                        " to " + newBuffer.capacity() + " bytes");
            } else {
                // Standard case: We made progress OR we have space left.
                // Move partial bytes to the start for the next read.
                buffer.compact();
            }
        }
    }

    private void closeConnection(SocketChannel client) throws IOException {
        System.out.println("Connection Closed: " + client.getRemoteAddress());
        clientBuffers.remove(client);
        connectedClients.decrementAndGet();
        client.close();

    }

    public static void main(String[] args) throws IOException {
        new KanRedisServer(6379).start();
    }
}