# Wielowątkowość w Java

Wielowątkowość pozwala wykonywać zadania jednocześnie, przyśpieszając działanie aplikacji w przypadku czasochłonnych zadań.

## Proces vs wątek
Proces to obraz aplikacji, pojedyńcza instancja. Procesem może być InteliJ i przeglądarka. Wątek jest ciągiem instrukcji
do wykoanania. Każdy proces musi zawierać jeden, główny wątek, ale można tworzyć więcej wątków w ramach tego samego
procesu. Dla przykłądu możemy uruchomić serwer z którego korzystałoby wielu użytkowników i każdy otrzymałby osobny wątek
w ramach tego samego procesu serwera. Innym przykłądem może być przeglądarka pobierająca jednocześnie dwa pliki. 
Wywołując polecenia:
```
System.out.println(Thread.activeCount());
System.out.println(Runtime.getRuntime().availableProcessors());
```
otrzymamy aktualnie używane wątki i dostępne, w zależności od budowy procesora. Na początku programu będą 2- jeden głowny
i drugi dla [Garbage Collector](https://www.baeldung.com/jvm-garbage-collectors). Druga liczba to przemnożona wartość zajętych wątków 
przez ilość rdzeni procesora. 

## Tworzenie wątków w Java

### Interfejs Runnable
Możemy roszszerzyc interfejs i zaimplementować metodę ``run`` która ma wykonać nasze zaadanie. Następnie tworzymy obiekt
Thread i wkonstruktorze umieszczamy naszą klasę z zadaniem. Uruchamiamy je metodą ``start``.
```
public class DownloadFileTask implements Runnable{
    @Override
    public void run() {
        System.out.println("Downloading");
    }
}

public class Main {
    public static void main(String[] args) {
        Thread thread = new Thread(new DownloadFileTask());
        thread.start();
    }
}
```
Wątki można również tworzyć w pętli
```
public static void main(String[] args) {
    for (int i = 0; i < 10; i++) {
        Thread thread = new Thread(new DownloadFileTask());
        thread.start();
    }
}
```
#### Pauzowanie wątku
