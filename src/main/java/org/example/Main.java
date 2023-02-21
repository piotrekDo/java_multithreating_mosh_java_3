package org.example;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        Thread thread = new Thread(new DownloadFileTask());
        thread.start();

        Thread.sleep(1500);
        thread.interrupt();
    }
}