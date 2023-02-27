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
Pauzować możemy metodą ``Thread.sleep`` wywołaną wewnątrz odpowiedniego wątku- w metodzie main czy w metodzie implementującej
run. 

#### Łaączenie wątków

metoda ``wątek.join()`` metoda wywoływana na wątku działającym w innym. Dla przykładu jeżeli wewnątrz metody main utworzymy
osobny wątek i na tym wątku wywołamy metodę ``join`` w metodzie main, reszta kodu z main zaczeka na ten wątek.

#### Przerywanie wątku
Aby przerwać wątek należy na nim wywołać metodę ``interrupt``. Jednak jest ona tylko żądaniem zakończenia wątku i nie spowoduje
natychmiastowego przerwania jego pracy. Można to rozwiązać stosując if sprawdzający czy obecny wątek ``isInterrupted``
```
public class DownloadFileTask implements Runnable{
    @Override
    public void run() {
        for (int i = 0; i < Integer.MAX_VALUE; i++) {
            if (Thread.currentThread().isInterrupted()) break;
            System.out.println("Downloaded");
        }
    }
}


public class Main {
    public static void main(String[] args) throws InterruptedException {
        Thread thread = new Thread(new DownloadFileTask());
        thread.start();

        Thread.sleep(1500);
        thread.interrupt();
    }
}
```
Powyżej w metodzie main wywołujemy nowy wątek i dalej metodę ``sleep``, następnie po 1,5s. metoda main wyśle żądanie 
przerwania wątku. Bez if wątek może się nie przerwać. 

## Problemy z wieloma wątkami
Czasami, gdy kilka wątków stara się zmodyfikować dane możemy natrafić na nieoczekiwane zachowanie programu lub błąd.

### Race conditions
Sytuacja w której dwa lub więcej wątków starają się dostać do tego samego zasobu, w rezultacie możemy utracić część danych.
Poniżej przykład, gdzie tworzymy 10 wątków, a każdy z nich powinien 10tys. razy zinkrementować obiket status. Niestety
wynik sięga około 30tys.
```
public class DownloadStatus {
    private int bytes;

    public int getBytes() {
        return bytes;
    }

    public void increment(){
        bytes++;
    }
}

public class DownloadFileTask implements Runnable{
    private DownloadStatus status;

    public DownloadFileTask(DownloadStatus status) {
        this.status = status;
    }

    @Override
    public void run() {
        for (int i = 0; i < 10_000; i++) {
            if (Thread.currentThread().isInterrupted()) break;
            status.increment();
        }
    }
}

public class Main {
    public static void main(String[] args) throws InterruptedException {
        DownloadStatus status = new DownloadStatus();

        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Thread thread = new Thread(new DownloadFileTask(status));
            thread.start();
            threads.add(thread);
        }
        threads.forEach(thread -> {
            try {
                thread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        System.out.println(status.getBytes());
    }
}
```

Powyższej sytuacji można zapobiec nieudostępniając tego samego obiktu dla każdego wątku. Można wykorzystać różne obiekty
a ich wyniki zsumować po zakończeniu pracy wszystkich wątków.  
  

Innym rozwiązaniem jest synchronizacja. Wprowadza to wymóg sekwencyjnego wykonania, kolejkując działania wątków. Może to 
doprowadzić do powstania *deadlock* czyli sytuacji, gdzie dwa wątki będą czekały w nieskończoność aż drugi się wykona.  
W celu wprowadzenia synchrnizacji stosujemy obiekty Lock, wywoływane wewnątrz zainteresowanej metody. Metodę ``unlock`` należy 
zapisywać w ramach bloku finally aby uniknąć sytuacji, gdzie obiekt zostanie zablokowany na stałe. 
```
public class DownloadStatus {
    private int bytes;
    private Lock lock = new ReentrantLock();

    public int getBytes() {
        return bytes;
    }

    public void increment(){
        lock.lock();
        try{
            bytes++;
        }catch (Exception e) {
            //logic
        }finally {
            lock.unlock();
        }
    }
}
```
Synchronizację możemy osiągnąc również blokiem ``synchronized``. Blok ten wymaga przekazania *monitor object*. Monitor object
odpowiada za synchronizację i pilnowanie kolejności. Złą praktyką jest ``this`` co może doprowadzić do problemów jezeli w 
takiej klasie jest więcej metod oznaczonych jako ``synchronized``. Wówczas gdy jeden wątek korzysta z jednej metody, żaden
inny nie może skorzystać z drugiej metody. Powinniśmy wykorzystywać osobne obiekty dla Monitor Object.
```
public class DownloadStatus {
    private int bytes;
    private Object lock = new Object();

    public int getBytes() {
        return bytes;
    }

    public void increment(){
       synchronized(lock) {
         bytes++;
       }
    }
}
```
Można również oznaczyć metodę jako ``synchronized``- ``public synchronized void increment()``, jednak wówczas jako Monitor
Object zostanie wykorzystane ``this``.

Kolejnym rozwiązaniem są **obiekty atomiczne** jak ``AtomicInteger``. Obiekty te są bezpieczne wielowątkowo, 
ponieważ operacje na nich wykonowane są atomiczne- wykonywane w całości jako jedna, nie osobno.