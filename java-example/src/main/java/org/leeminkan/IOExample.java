package org.leeminkan;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class IOExample {
    public static void main(String[] args)  {
        System.out.println("Hello World!");
        System.out.println("Java is currently looking for the file in: " +
                System.getProperty("user.dir"));

        try (InputStream input = new FileInputStream("./src/sample.txt")) {

            // Read one byte at a time (inefficient, for demonstration)
            int data = input.read();
            System.out.println("Reading with java.io...");
            while (data != -1) {
                // Process the byte data
                System.out.print((char) data); // Uncomment to print content
                data = input.read();
            }
            System.out.println("\nFinished reading with java.io.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
