package org.leeminkan;

public class ThreadSafetyComparison {

    // Tạo hai đối tượng, một an toàn và một không an toàn
    private static final StringBuffer safeBuffer = new StringBuffer();
    private static final StringBuilder unsafeBuilder = new StringBuilder();

    // Số lần mỗi luồng thêm vào chuỗi
    private static final int APPEND_COUNT = 5000;

    // Tổng số ký tự mong đợi: 2 luồng * 50 lần/luồng = 100 ký tự

    public static void main(String[] args) throws InterruptedException {

        // --- VÍ DỤ 1: StringBuffer (Safe) ---
        runThreads(safeBuffer, "StringBuffer");
        // Kết quả luôn là 100 (hoặc 100 ký tự 'A'), vì các thao tác được đồng bộ hóa
        System.out.println("StringBuffer - Độ dài cuối cùng: " + safeBuffer.length());

        // --- VÍ DỤ 2: StringBuilder (Unsafe) ---
        // Cần reset và chạy lại các luồng để so sánh
        runThreads(unsafeBuilder, "StringBuilder");
        // Kết quả: Độ dài có thể nhỏ hơn 100 và chuỗi bên trong có thể bị lỗi (corrupted)
        System.out.println("StringBuilder - Độ dài cuối cùng: " + unsafeBuilder.length());
    }

    private static void runThreads(Appendable target, String type) throws InterruptedException {

        Runnable task = () -> {
            for (int i = 0; i < APPEND_COUNT; i++) {
                if (target instanceof StringBuilder) {
                    ((StringBuilder) target).append("A");
                } else if (target instanceof StringBuffer) {
                    ((StringBuffer) target).append("A");
                }
            }
        };

        Thread t1 = new Thread(task);
        Thread t2 = new Thread(task);

        t1.start();
        t2.start();

        t1.join(); // Chờ luồng 1 kết thúc
        t2.join(); // Chờ luồng 2 kết thúc
    }
}