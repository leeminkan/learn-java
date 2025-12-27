package org.leeminkan.backpressure;

import java.io.*;
import java.net.*;

public class FastClient {
    public static void main(String[] args) throws Exception {
        Socket socket = new Socket("localhost", 9999);
        OutputStream out = socket.getOutputStream();

        byte[] bigData = new byte[1024]; // 1KB junk data
        int count = 0;

        while (true) {
            out.write(bigData);
            count++;
            System.out.println("Client: Đã đẩy vào đường ống " + count + " KB");

            // Không sleep, gửi nhanh nhất có thể
        }
    }
}