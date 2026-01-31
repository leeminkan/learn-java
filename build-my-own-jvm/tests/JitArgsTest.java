public class JitArgsTest {
    // This maps to: iload_0, iload_1, iadd, ireturn
    public static int add(int a, int b) {
        return a + b;
    }

    public static void main(String[] args) {
        int sum = 0;
        // Loop to trigger JIT (Threshold 5)
        for (int i = 0; i < 10; i++) {
            sum = sum + add(10, 20);
        }
        System.out.println(sum); // Expect: 300
    }
}
