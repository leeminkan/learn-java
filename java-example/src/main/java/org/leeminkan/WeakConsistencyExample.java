package org.leeminkan;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class WeakConsistencyExample {

    public static void main(String[] args) throws InterruptedException {
        // 1. Khởi tạo ConcurrentHashMap
        ConcurrentHashMap<Integer, String> concurrentMap = new ConcurrentHashMap<>();
        concurrentMap.put(1, "StartA");
        concurrentMap.put(2, "StartB");

        System.out.println("--- 1. Trạng thái Map ban đầu: " + concurrentMap);
        System.out.println("--- 2. Kích thước ban đầu (size()): " + concurrentMap.size() + "\n");

        // --- LUỒNG DUYỆT (ITERATOR THREAD) ---
        Thread iteratorThread = new Thread(() -> {
            System.out.println("--- [Iterator] Bắt đầu duyệt Map ---");

            // Lấy Iterator trước khi luồng khác bắt đầu sửa đổi
            Iterator<Integer> iterator = concurrentMap.keySet().iterator();

            int count = 0;
            while (iterator.hasNext()) {
                Integer key = iterator.next();
                String value = concurrentMap.get(key);
                count++;

                System.out.println("[Iterator] Đang duyệt: Key=" + key + ", Value=" + value);

                // Giả lập thời gian xử lý chậm
                if (count == 2) {
                    try {
                        // Tạo thời gian cho Luồng Sửa đổi thêm phần tử
                        TimeUnit.MILLISECONDS.sleep(500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
            System.out.println("--- [Iterator] Kết thúc duyệt. Tổng cộng: " + count + " phần tử ---");
        }, "Iterator-Thread");

        // --- LUỒNG SỬA ĐỔI (MODIFIER THREAD) ---
        Thread modifierThread = new Thread(() -> {
            try {
                // Đảm bảo IteratorThread đã bắt đầu duyệt
                TimeUnit.MILLISECONDS.sleep(100);

                System.out.println("\n*** [Modifier] Bắt đầu sửa đổi Map ***");

                // Thêm 2 phần tử MỚI sau khi Iterator đã được tạo
                concurrentMap.put(100, "NewX");
                concurrentMap.put(101, "NewY");

                System.out.println("*** [Modifier] Đã thêm 2 phần tử mới (100, 101) ***");
                System.out.println("*** [Modifier] Kích thước Map hiện tại (size()): " + concurrentMap.size() + " ***\n");

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Modifier-Thread");

        iteratorThread.start();
        modifierThread.start();

        iteratorThread.join();
        modifierThread.join();

        System.out.println("\n--- 3. Trạng thái Map cuối cùng: " + concurrentMap);
        System.out.println("--- 4. Kích thước Map cuối cùng: " + concurrentMap.size());
    }
}