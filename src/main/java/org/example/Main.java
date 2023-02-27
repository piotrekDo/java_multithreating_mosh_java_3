package org.example;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        Collection<Integer> collection = Collections.synchronizedCollection(new ArrayList<>());

        Thread thread1 = new Thread(() -> collection.addAll(Arrays.asList(1, 2, 3)));
        Thread thread2 = new Thread(() -> collection.addAll(Arrays.asList(4, 5, 6)));

        thread1.start();
        thread2.start();

        thread1.join();
        thread2.join();

        System.out.println(collection);

        Map<Integer, String> map = new ConcurrentHashMap<>();
    }
}