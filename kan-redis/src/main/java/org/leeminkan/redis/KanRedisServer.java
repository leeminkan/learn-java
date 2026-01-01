package org.leeminkan.redis;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * KanRedisServer
 * A non-blocking, event-driven server using Java NIO.
 * Designed to handle C10K (10,000 concurrent connections) on a single thread.
 */
public class KanRedisServer {

    private final int port;
    private Selector selector;
    private ServerSocketChannel serverChannel;
    private boolean isRunning = true;
    private KanStore store;
    private KanProtocol protocol;

    // A map to store data associated with a connection (buffers, state)
    // In a real Netty implementation, this would be the 'ChannelContext'
    private final ConcurrentHashMap<SocketChannel, ByteBuffer> clientBuffers = new ConcurrentHashMap<>();

    public KanRedisServer(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        // 1. Initialize WAL
        KanWal wal = new KanWal("kan-data.log");

        // 2. Initialize Store with WAL
        store = new KanStore(wal);

        // 3. Replay Old Data
        wal.replay(store);

        // 4. Initialize Protocol
        protocol = new KanProtocol(store);

        // 1. Open the Selector (The "Event Loop" Monitor)
        selector = Selector.open();

        // 2. Open Server Channel (The "Door")
        serverChannel = ServerSocketChannel.open();
        serverChannel.bind(new InetSocketAddress(port));

        // CRITICAL: Must be non-blocking to work with Selector
        serverChannel.configureBlocking(false);

        // 3. Register the "Accept" event. We want to know when a new client knocks.
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

            // Loop to process all complete commands in the buffer
            while (protocol.process(buffer, responseBuffer)) {
                // If a command was processed, we might have a response to send
                if (responseBuffer.position() > 0) {
                    responseBuffer.flip();
                    while (responseBuffer.hasRemaining()) {
                        client.write(responseBuffer);
                    }
                    responseBuffer.clear(); // Clear for next command's response
                }
            }

            // CRITICAL: Move remaining (partial) bytes to the start
            // and switch back to WRITE mode for the next socket read.
            buffer.compact();
        }
    }

    private void closeConnection(SocketChannel client) throws IOException {
        System.out.println("Connection Closed: " + client.getRemoteAddress());
        clientBuffers.remove(client);
        client.close();
    }

    public static void main(String[] args) throws IOException {
        new KanRedisServer(6379).start();
    }
}