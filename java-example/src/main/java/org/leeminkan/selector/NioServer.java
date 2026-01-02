package org.leeminkan.selector;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;

public class NioServer {
    public static void main(String[] args) throws IOException {
        // 1. Open the Selector (This creates the 'kqueue' in macOS)
        Selector selector = Selector.open();

        // 2. Open Server Socket Channel
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.bind(new InetSocketAddress("localhost", 9999));
        serverChannel.configureBlocking(false);

        // 3. Register for 'Accept' events (Step A: Interest List)
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        System.out.println("Echo Server started on port 9999...");

        while (true) {
            // 4. The 'select' call (Step B: The Nap / kevent system call)
            selector.select();

            // 5. Wakeup (Step C: Ready List)
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> iter = selectedKeys.iterator();

            while (iter.hasNext()) {
                SelectionKey key = iter.next();

                if (key.isAcceptable()) {
                    registerClient(selector, serverChannel);
                }
                if (key.isReadable()) {
                    answerClient(key);
                }
                // CRITICAL: Remove the key so we don't process it again
                iter.remove();
            }
        }
    }

    private static void registerClient(Selector selector, ServerSocketChannel serverChannel) throws IOException {
        SocketChannel client = serverChannel.accept();
        client.configureBlocking(false);
        // Add new client to the OS 'Interest List'
        client.register(selector, SelectionKey.OP_READ);
        System.out.println("New connection accepted.");
    }

    private static void answerClient(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(256);
        int bytesRead = client.read(buffer);

        if (bytesRead == -1) {
            client.close();
            System.out.println("Connection closed.");
        } else {
            buffer.flip(); // Prepare to read from buffer
            client.write(buffer); // Echo data back
            System.out.println("Echoed: " + new String(buffer.array()).trim());
        }
    }
}