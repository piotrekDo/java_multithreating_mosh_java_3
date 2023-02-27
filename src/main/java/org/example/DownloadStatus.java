package org.example;


import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

public class DownloadStatus {
    private LongAdder bytes = new LongAdder();

    public int getBytes() {
        return bytes.intValue();
    }

    public void increment(){
        bytes.increment();
    }
}
