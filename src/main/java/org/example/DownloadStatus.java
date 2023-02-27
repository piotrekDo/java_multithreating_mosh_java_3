package org.example;


public class DownloadStatus {
    private int bytes;
    private volatile boolean isDone;

    public int getBytes() {
        return bytes;
    }

    public void done() {
        this.isDone = true;
    }

    public synchronized void increment() {
        bytes++;
    }

    public boolean isDone() {
        return isDone;
    }
}
