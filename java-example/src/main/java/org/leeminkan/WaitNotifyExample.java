package org.leeminkan;

public class WaitNotifyExample {
    private static final Object lock = new Object();
    private static boolean dataReady = false;

    public static void main(String[] args) {
        // Luồng Người tiêu thụ (Consumer)
        Thread consumer = new Thread(() -> {
            synchronized (lock) {
                while (!dataReady) {
                    try {
                        System.out.println("Consumer: Dữ liệu chưa sẵn sàng. Đang chờ (nhả lock)...");
                        lock.wait(); // Nhả lock và chờ
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                System.out.println("Consumer: Dữ liệu đã sẵn sàng! Bắt đầu xử lý.");
            }
        });

        // Luồng Nhà sản xuất (Producer)
        Thread producer = new Thread(() -> {
            try {
                // Giả lập thời gian tạo dữ liệu
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            synchronized (lock) {

                System.out.println("Producer: Dữ liệu đã tạo xong. Sẵn sàng thông báo.");
                dataReady = true;
                lock.notify(); // Thông báo cho luồng đang chờ (Consumer)
                System.out.println("Producer: Đã gọi notify(). Lock vẫn còn giữ cho đến khi khối synchronized kết thúc.");
            }
        });

        consumer.start();
        producer.start();
    }
}
