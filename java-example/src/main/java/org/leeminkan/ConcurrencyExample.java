package org.leeminkan;

import java.util.concurrent.*;

public class ConcurrencyExample {

    // 1. Runnable Implementation
    static class SimpleRunnable implements Runnable {
        private String taskName;

        public SimpleRunnable(String taskName) {
            this.taskName = taskName;
        }

        @Override
        public void run() {
            System.out.println("Runnable Task: " + taskName + " is running. No return value.");
        }
    }

    // 2. Callable Implementation (returns an Integer)
    static class SumCallable implements Callable<Integer> {
        private int endValue;

        public SumCallable(int endValue) {
            this.endValue = endValue;
        }

        @Override
        public Integer call() throws Exception {
            int sum = 0;
            for (int i = 1; i <= endValue; i++) {
                sum += i;
            }
            // Callable can return a result
            return sum;
        }
    }

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        // Create a fixed-size thread pool
        ExecutorService executor = Executors.newFixedThreadPool(2);

        // --- Execute Runnable Task (fire-and-forget) ---
        SimpleRunnable runnableTask = new SimpleRunnable("Logger");
        executor.execute(runnableTask); // Use execute() for Runnable

        // --- Submit Callable Task (returns Future) ---
        SumCallable callableTask = new SumCallable(10);

        // Use submit() for Callable, which returns a Future object
        Future<Integer> futureResult = executor.submit(callableTask);

        System.out.println("Main thread waiting for Callable result...");

        // Blocks and waits for the result from the Callable task
        Integer result = futureResult.get();

        System.out.println("Callable Result: The sum of 1 to 10 is " + result);

        executor.shutdown();
    }
}
