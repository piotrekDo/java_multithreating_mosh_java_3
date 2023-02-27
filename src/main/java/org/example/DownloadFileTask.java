package org.example;

public class DownloadFileTask implements Runnable {
    private DownloadStatus status;

    public DownloadFileTask(DownloadStatus status) {
        this.status = status;
    }

    @Override
    public void run() {
        for (int i = 0; i < 1_000_000; i++) {
            if (Thread.currentThread().isInterrupted()) break;
            status.increment();
        }
        status.done();
        synchronized (status) {
            status.notify();
        }
    }
}
