Here is a complete, minimal **TCP Echo Server** using a `Selector`. You can run this on your Mac, and then we will use a specific macOS tool to "peek" inside and see it working with the kernel.

### 1. The Java Code

Copy this into a file named `NioServer.java`:

```java
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

```

---

### 2. How to run it

1. **Compile:** `javac NioServer.java`
2. **Run:** `java NioServer`

---

### 3. How to "See" it on macOS (The Proof)

Open a **second terminal** window while the server is running. We will use `lsof` to prove that Java has created a `KQUEUE` (the macOS high-performance I/O engine).

#### Check the Engine

Run this command:

```bash
lsof -p $(pgrep -f NioServer) | grep -i "KQUEUE"

```

**What you'll see:** You should see a line with the type `KQUEUE`. This confirms that the `Selector.open()` successfully talked to the macOS kernel to create a dedicated event queue.

#### Check the Wakeup Pipe

Run this:

```bash
lsof -p $(pgrep -f NioServer) | grep "PIPE"

```

**What you'll see:** You will see two pipes. This is the "hidden pipe" Java uses to wake up the thread if `selector.wakeup()` is called.

#### Test the Server

Open a **third terminal** and connect:

```bash
nc localhost 9999

```

Type something (e.g., "Hello Mac!"). The server will echo it back instantly.

---

### 4. Summary of what just happened "Under the Hood"

1. **`Selector.open()`**: Java called the macOS system call `kqueue()`. The OS returned a file descriptor for a new event queue.
2. **`register()`**: Java called `kevent()` to add your server socket to that queue.
3. **`select()`**: Java called `kevent()` again, but this time told the kernel: **"Stop my thread until one of these sockets is ready."**
4. **Hardware Interrupt**: When you typed "Hello" in the other terminal, your Network Card triggered an interrupt. The macOS kernel saw the data, identified your socket, and added it to the `kqueue` **Ready List**.
5. **Return**: The `kevent()` call returned, your Java thread woke up, and the `selectedKeys()` set was populated with your client's key.

---

