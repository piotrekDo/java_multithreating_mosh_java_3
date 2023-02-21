package org.example;

public class DownloadFileTask implements Runnable{
    @Override
    public void run() {
        for (int i = 0; i < Integer.MAX_VALUE; i++) {
            if (Thread.currentThread().isInterrupted()) break;
            System.out.println("Downloaded");
        }
    }
}
