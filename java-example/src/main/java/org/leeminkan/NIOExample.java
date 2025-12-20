package org.leeminkan;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class NIOExample {
    public static void main(String[] args) {
        Path path = Paths.get("./src/sample.txt");

        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            int bytesRead;

            System.out.println("Reading " + path + " ...");

            while ((bytesRead = channel.read(buffer)) != -1) {
                if (bytesRead > 0) {
                    buffer.flip();
                    while (buffer.hasRemaining()) {
                        // Process the byte data
                         System.out.print((char) buffer.get()); // Uncomment to print content
                    }
                    buffer.clear();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
