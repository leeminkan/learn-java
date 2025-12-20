package org.leeminkan;

import java.util.HashMap;

public class UnsafeHashMapExample {
    public static void main(String[] args) throws InterruptedException {
        // HashMap không được đồng bộ hóa
        HashMap<String, Integer> unsafeMap = new HashMap<>();

        Runnable task = () -> {
            for (int i = 0; i < 1000; i++) {
                // Hai luồng cố gắng put() đồng thời
                unsafeMap.put(Thread.currentThread().getName() + i, i);
            }
        };

        Thread t1 = new Thread(task, "T1");
        Thread t2 = new Thread(task, "T2");

        t1.start();
        t2.start();

        t1.join();
        t2.join();

        // Kích thước mong đợi là 2000, nhưng có thể gặp lỗi hoặc kích thước không chính xác
        System.out.println("Kích thước cuối cùng: " + unsafeMap.size());
        // Trong môi trường phức tạp hơn, nó có thể dẫn đến vòng lặp vô hạn (infinite loop)
    }
}