package org.leeminkan.backpressure;

import java.io.*;
import java.net.*;

public class SlowServer {
    public static void main(String[] args) throws Exception {
        ServerSocket serverSocket = new ServerSocket(9999);
        // Giới hạn bộ đệm nhận nhỏ để thấy Backpressure nhanh hơn
        serverSocket.setOption(StandardSocketOptions.SO_RCVBUF, 16384); // 16KB

        System.out.println("Server sẵn sàng, đang đợi Client...");
        Socket socket = serverSocket.accept();
        InputStream in = socket.getInputStream();

        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = in.read(buffer)) != -1) {
            String data = new String(buffer, 0, bytesRead);
            System.out.println("Server: Đã nhận 1KB dữ liệu.");

            // MÔ PHỎNG XỬ LÝ CHẬM: Nghỉ 2 giây cho mỗi 1KB
            Thread.sleep(2000);
        }
    }
}