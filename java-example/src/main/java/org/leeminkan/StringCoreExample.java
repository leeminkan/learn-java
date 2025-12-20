package org.leeminkan;

import java.util.Arrays;

public class StringCoreExample {

    public static void main(String[] args) {

        // --- 1. STRING POOL VÀ TÍNH BẤT BIẾN (IMMUTABILITY) ---
        System.out.println("=== 1. String Pool & Immutability ===");

        // S1 và S2 là String Literals: JVM kiểm tra String Pool.
        // Vì "Hello" chưa có, nó được tạo 1 lần trong Pool.
        String s1 = "Hello";
        String s2 = "Hello";

        // S3 được tạo bằng 'new': Lực lượng tạo đối tượng mới trong Heap,
        // ngay cả khi "Hello" đã có trong Pool.
        String s3 = new String("Hello");

        System.out.println("s1 == s2: " + (s1 == s2)); // true (Cùng tham chiếu đến đối tượng trong Pool)
        System.out.println("s1 == s3: " + (s1 == s3)); // false (s3 trỏ đến đối tượng trong Heap)

        // Phương thức equals() chỉ so sánh nội dung (Content)
        System.out.println("s1.equals(s3): " + s1.equals(s3)); // true (Nội dung giống nhau)

        // Phương thức intern(): Đảm bảo String nằm trong Pool
        String s4 = s3.intern();
        System.out.println("s1 == s4 (intern): " + (s1 == s4)); // true (s4 giờ trỏ đến đối tượng trong Pool)

        // Minh họa Tính Bất Biến (Immutability):
        // Khi bạn gán s1 = s1 + " World", s1 KHÔNG thay đổi.
        // Thay vào đó, một đối tượng String mới ("Hello World") được tạo ra
        // trong Heap/Pool, và s1 trỏ đến đối tượng mới đó.
        String s5 = "Hello";
        String s6 = s5;
        s5 = s5 + " World";

        System.out.println("\ns5 (sau khi thay đổi): " + s5); // "Hello World"
        System.out.println("s6 (vẫn giữ giá trị cũ): " + s6); // "Hello"
        System.out.println("s5 == s6: " + (s5 == s6)); // false (Tham chiếu đã khác)


        // --- 2. CÁC THAO TÁC CƠ BẢN VÀ SO SÁNH ---
        System.out.println("\n=== 2. Các Thao tác Cơ bản ===");
        String text = " Java Programming is Fun ";

        // a. Độ dài và truy cập ký tự
        System.out.println("Độ dài: " + text.length()); // 25
        System.out.println("Ký tự tại index 1: " + text.charAt(1)); // 'J' (ký tự đầu tiên)

        // b. Xóa khoảng trắng
        String trimmedText = text.trim();
        System.out.println("Sau trim(): '" + trimmedText + "'"); // "Java Programming is Fun"

        // c. Thay thế
        String replacedText = trimmedText.replace("Fun", "Awesome");
        System.out.println("Sau replace(): " + replacedText);

        // d. Phân tách (Splitting)
        String[] words = replacedText.split(" ");
        System.out.println("Sau split(): " + Arrays.toString(words)); // [Java, Programming, is, Awesome]

        // e. So sánh:
        String strUpper = "JAVA";
        String strLower = "java";

        // So sánh có phân biệt chữ hoa/thường
        System.out.println("strUpper.equals(strLower): " + strUpper.equals(strLower)); // false
        // So sánh KHÔNG phân biệt chữ hoa/thường
        System.out.println("strUpper.equalsIgnoreCase(strLower): " + strUpper.equalsIgnoreCase(strLower)); // true


        // --- 3. HIỆU SUẤT: StringBuilder & StringBuffer ---
        System.out.println("\n=== 3. Hiệu suất: StringBuilder vs String ===");

        long startTime = System.currentTimeMillis();
        String result = "";
        for (int i = 0; i < 10000; i++) {
            // Việc nối chuỗi liên tục tạo ra các đối tượng String mới (tốn kém)
            result += "x";
        }
        long endTime = System.currentTimeMillis();
        System.out.println("Result " + result);
        System.out.println("Thời gian nối chuỗi String (10000 lần): " + (endTime - startTime) + "ms");


        startTime = System.currentTimeMillis();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            // StringBuilder thay đổi trên cùng một đối tượng (hiệu quả hơn)
            sb.append("x");
        }
        endTime = System.currentTimeMillis();
        System.out.println("Result " + sb.toString());
        System.out.println("Thời gian nối chuỗi StringBuilder (10000 lần): " + (endTime - startTime) + "ms");

        // Lưu ý: StringBuffer tương tự StringBuilder nhưng là Thread-Safe (chậm hơn một chút)
    }
}