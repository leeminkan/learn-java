public class JitTest {
    // This method is simple enough for our JIT to handle
    // It maps to: bipush 42, ireturn
    public static int getNumber() {
        return 42;
    }

    public static void main(String[] args) {
        int sum = 0;
        // Loop 10 times to trigger JIT (Threshold is 5)
        for (int i = 0; i < 10; i++) {
            sum = sum + getNumber();
        }
        System.out.println(sum); // Should be 420
    }
}
