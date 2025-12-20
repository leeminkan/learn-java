package org.leeminkan;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SynchronizedMapExample {
    public static void main(String[] args) throws InterruptedException {
        // Sử dụng lock duy nhất để đồng bộ hóa TẤT CẢ các hoạt động
        Map<String, Integer> safeMap = Collections.synchronizedMap(new HashMap<>());

        Runnable task = () -> {
            for (int i = 0; i < 1000; i++) {
                safeMap.put(Thread.currentThread().getName() + i, i);
            }
        };

        Thread t1 = new Thread(task, "T1");
        Thread t2 = new Thread(task, "T2");

        t1.start();
        t2.start();

        t1.join();
        t2.join();

        System.out.println("Kích thước cuối cùng: " + safeMap.size()); // 2000 (An toàn)
        // Các luồng sẽ phải chờ nhau hoàn thành put()
    }
}