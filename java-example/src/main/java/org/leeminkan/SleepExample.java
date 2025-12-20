package org.leeminkan;

public class SleepExample {
    public static void main(String[] args) {
        System.out.println("Luồng chính (Main) bắt đầu.");
        try {
            // Tạm dừng luồng chính trong 10 giây (10000 milliseconds)
            System.out.println("Luồng Main tạm dừng 10 giây...");
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println("Luồng Main tiếp tục chạy sau 10 giây.");
    }
}
