package org.example;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        CompletableFuture<String> price = CompletableFuture.supplyAsync(() -> "20USD");
        CompletableFuture<Integer> items = CompletableFuture.supplyAsync(() -> {
            LongTask.simulate(); // symulacja oczekiwania na dane kilka sekund
            return 19;
        });
        CompletableFuture<Double> exRate = CompletableFuture.supplyAsync(() -> 0.9);

        CompletableFuture.allOf(price, items, exRate)
                .thenRun(() -> {
                    try {
                        String priceString = price.get();
                        Integer itemsTotal = items.get();
                        Double exchangeRate = exRate.get();

                        int pricePerItem = Integer.parseInt(priceString.replace("USD", ""));
                        double result = pricePerItem * itemsTotal * exchangeRate;
                        System.out.println(result);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    } catch (ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                });

        Thread.sleep(5000);
    }
}