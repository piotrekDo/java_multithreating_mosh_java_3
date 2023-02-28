package org.example;

import java.util.concurrent.CompletableFuture;

public class FakeMailService {
    int send() {
        LongTask.simulate(); // symuluje 3 sekundy opóźnienia.
        System.out.println("Mail was sent.");
        return 1;
    }

    CompletableFuture<Integer> sendAsync() {
        return CompletableFuture.supplyAsync(this::send);
    }
}
