public class Point {
    int x;
    int y;

    public static void main(String[] args) {
        Point p = new Point(); // Creates object on Heap
        p.x = 5;               // Sets field
        p.y = 10;              // Sets field
        System.out.println(p.x + p.y); // Reads fields and prints 15
    }
}