package org.example;

public class RandomLatencyGenerator {
    private final float min = 0.5f;
    private final float max = 4.5f;

    void delay() {
        double random = Math.random() * (max - min) + min;
        try {
            Thread.sleep((int) random * 1000L);
        } catch (InterruptedException e) {
            System.err.println("niestety");
        }
    }
}
