package org.leeminkan;

public class YieldExample implements Runnable {
    @Override
    public void run() {
        for (int i = 0; i < 5; i++) {
            System.out.println(Thread.currentThread().getName() + " chạy: " + i);
            if (i == 2) {
                System.out.println(Thread.currentThread().getName() + " nhường CPU...");
//                Thread.yield(); // Gợi ý nhường CPU
            }
        }
    }

    public static void main(String[] args) {
        Thread t1 = new Thread(new YieldExample(), "Luồng A");
        Thread t2 = new Thread(new YieldExample(), "Luồng B");

        t1.start();
        t2.start();

        // Output có thể thay đổi, nhưng bạn sẽ thấy Luồng A gọi yield() rồi Luồng B có cơ hội chạy (hoặc ngược lại)
    }
}