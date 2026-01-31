public class ArrayTest {
    public static void main(String[] args) {
        // 1. Create array
        int[] nums = new int[3];

        // 2. Store values (IASTORE)
        nums[0] = 10;
        nums[1] = 20;
        nums[2] = 30;

        int sum = 0;

        // 3. Loop and read values (ARRAYLENGTH, IALOAD)
        for (int i = 0; i < nums.length; i++) {
            sum = sum + nums[i];
        }

        System.out.println(sum); // Expect: 60
    }
}
