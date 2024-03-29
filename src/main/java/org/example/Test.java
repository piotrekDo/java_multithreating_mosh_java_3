package org.example;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Test {

    public static void main(String[] args) {
        AtomicInteger counter = new AtomicInteger();

        List<Integer> integers = new LinkedList<>(Arrays.asList(1, 2, 3, 4, 5, 6));
        integers.forEach(x -> counter.getAndIncrement());

        System.out.println(counter.get());

    }

}
