package org.example;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

public class ExerciseBestPriceFinder {

    public static void main(String[] args) {
        RandomLatencyGenerator randomLatencyGenerator = new RandomLatencyGenerator();
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        executorService.submit(() -> null);

        LocalTime start = LocalTime.now();
        CompletableFuture<Integer> first = CompletableFuture.supplyAsync(() -> {
            randomLatencyGenerator.delay();
            return 200;
        }).thenApplyAsync(result -> {
            LocalTime end = LocalTime.now();
            Duration timePassed = Duration.between(start, end);
            System.out.println("First " + result + " in: " + timePassed.toMillis());
            return result;
        });
        CompletableFuture<Integer> second = CompletableFuture.supplyAsync(() -> {
            randomLatencyGenerator.delay();
            return 3432;
        }).thenApplyAsync(result -> {
            LocalTime end = LocalTime.now();
            Duration timePassed = Duration.between(start, end);
            System.out.println("Second " + result + " in: " + timePassed.toMillis());
            return result;
        });
        CompletableFuture<Integer> third = CompletableFuture.supplyAsync(() -> {
            randomLatencyGenerator.delay();
            return 442;
        }).thenApplyAsync(result -> {
            LocalTime end = LocalTime.now();
            Duration timePassed = Duration.between(start, end);
            System.out.println("Third " + result + " in: " + timePassed.toMillis());
            return result;
        });
        CompletableFuture<Integer> fourth = CompletableFuture.supplyAsync(() -> {
            randomLatencyGenerator.delay();
            return 15;
        }).thenApplyAsync(result -> {
            LocalTime end = LocalTime.now();
            Duration timePassed = Duration.between(start, end);
            System.out.println("Fourth " + result + " in: " + timePassed.toMillis());
            return result;
        });

        CompletableFuture<Void> allTogether = CompletableFuture.allOf(first, second, third, fourth);
        allTogether.thenRun(() -> {
            try {
                Integer firstValue = first.exceptionally(ex -> Integer.MAX_VALUE).get();
                Integer secondValue = second.exceptionally(ex -> Integer.MAX_VALUE).get();
                Integer thirdValue = third.exceptionally(ex -> Integer.MAX_VALUE).get();
                Integer forthValue = fourth.exceptionally(ex -> Integer.MAX_VALUE).get();

                Integer minPrice = List.of(firstValue, secondValue, thirdValue, forthValue).stream()
                        .filter(value -> value < Integer.MAX_VALUE)
                        .min(Integer::compareTo)
                        .orElseThrow(() -> new IllegalStateException("No values"));

                System.out.println("Min price: " + minPrice);
                LocalTime end = LocalTime.now();
                Duration timePassed = Duration.between(start, end);
                System.out.println("Total time: " + timePassed.toMillis());
                executorService.shutdown();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        });

    }
}
