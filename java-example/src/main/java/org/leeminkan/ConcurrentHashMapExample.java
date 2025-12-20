package org.leeminkan;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ConcurrentHashMapExample {
    public static void main(String[] args) throws InterruptedException {
        // Sử dụng lock phân đoạn, cho phép đồng thời cao
        ConcurrentMap<String, Integer> concurrentMap = new ConcurrentHashMap<>();

        Runnable task = () -> {
            for (int i = 0; i < 1000; i++) {
                concurrentMap.put(Thread.currentThread().getName() + i, i);
            }
        };

        Thread t1 = new Thread(task, "T1");
        Thread t2 = new Thread(task, "T2");

        t1.start();
        t2.start();

        t1.join();
        t2.join();

        System.out.println("Kích thước cuối cùng: " + concurrentMap.size()); // 2000 (An toàn)
        // Các luồng hoạt động song song mà không cần chờ toàn bộ Map
    }
}