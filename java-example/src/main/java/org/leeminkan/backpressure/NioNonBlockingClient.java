package org.leeminkan.backpressure;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class NioNonBlockingClient {
    public static void main(String[] args) throws IOException, InterruptedException {
        // 1. Mở channel và cấu hình Non-blocking
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        socketChannel.connect(new InetSocketAddress("localhost", 9999));

        // Đợi cho đến khi kết nối xong (vì là non-blocking nên connect trả về ngay)
        while (!socketChannel.finishConnect()) {
            System.out.println("Đang thiết lập kết nối...");
        }

        ByteBuffer buffer = ByteBuffer.allocate(1024); // 1KB dữ liệu
        int totalBytesSent = 0;
        int attempt = 0;

        while (true) {
            buffer.clear();
            buffer.put(new byte[1024]); // Nạp dữ liệu vào buffer
            buffer.flip();

            // 2. Ghi dữ liệu vào channel
            int bytesWritten = socketChannel.write(buffer);

            if (bytesWritten > 0) {
                totalBytesSent += bytesWritten;
                System.out.println("Đã gửi: " + bytesWritten + " bytes. Tổng: " + totalBytesSent / 1024 + " KB");
            } else {
                // 3. ĐÂY LÀ ĐIỂM QUAN TRỌNG: Backpressure xảy ra!
                // TCP Send Buffer đã đầy, bytesWritten trả về 0 ngay lập tức.
                attempt++;
                System.err.println("==> CẢNH BÁO: Đường ống đầy! Không thể gửi thêm. Thử lại lần: " + attempt);

                // Thay vì bị treo (block), Client có thể làm việc khác ở đây
                doOtherTask();

                Thread.sleep(500); // Đợi một chút rồi thử lại
            }
        }
    }

    private static void doOtherTask() {
        System.out.println("Client: 'Đường ống đang kẹt, tôi tranh thủ đi nén ảnh hoặc xử lý logic khác...'");
    }
}