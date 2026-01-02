package org.leeminkan.selector;

import java.io.*;
import java.net.*;
import java.util.concurrent.Executors;

public class VirtualServer {
    public static void main(String[] args) throws IOException {
        // Create an Executor that starts a new Virtual Thread for every task
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            try (ServerSocket serverSocket = new ServerSocket(9999)) {
                System.out.println("Virtual Thread Server started on port 9999...");

                while (true) {
                    // Wait for a connection (the thread 'yields' here)
                    Socket clientSocket = serverSocket.accept();

                    // Submit a new task to the executor
                    executor.submit(() -> handleClient(clientSocket));
                }
            }
        }
    }

    private static void handleClient(Socket socket) {
        try (socket;
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            String inputLine;
            // This 'read' looks blocking, but under the hood, 
            // the JVM is using kqueue/Selector to yield the CPU!
            while ((inputLine = in.readLine()) != null) {
                System.out.println("Received: " + inputLine);
                out.println("Echo: " + inputLine);
            }
        } catch (IOException e) {
            System.out.println("Client disconnected.");
        }
    }
}