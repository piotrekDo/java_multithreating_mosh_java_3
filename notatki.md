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

#### Łączenie wątków

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
wynik sięga około 30tys. zamiast oczekiwanych 100tys.
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

#### Strategie zapobiegania *Race Conditions*

**Confinement** - *uwięzienie*. Powyższej sytuacji można zapobiec nieudostępniając tego samego obiktu dla każdego wątku.
Można wykorzystać różne obiekty a ich wyniki zsumować po zakończeniu pracy wszystkich wątków.  
  
**Synchronization** - Innym rozwiązaniem jest synchronizacja. Wprowadza to wymóg sekwencyjnego wykonania, kolejkując działania wątków. Może to 
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

#### Słowo kluczowe volatile
Poza *deadlock* istnieje drugi problem rozwiązywany przez słowo kluczowe ``volatile``. Wykorzystanie go zapewnia, że zmiany
wprowadzone przez jeden wątek będą widoczne przez drugi. Przykładem może być wykorzystanie przez procesor pamięci cache. 
Oznacza to że wątek z innego rdzenia nie ma możliwości odczytu tych danych. Mamy poniższy przykład z obiektem ``DownloadStatus``
zawierającym pole ``isDone``, oznaczone słowem ``volatile``.

```
public class DownloadStatus {
    private int bytes;
    private volatile boolean isDone;

    public int getBytes() {
        return bytes;
    }

    public void done() {
        this.isDone = true;
    }

    public boolean isDone() {
        return isDone;
    }

    public synchronized void increment() {
        bytes++;
    }
}
```

Oraz naszą implementację interfejsu ``Runnable``
```
public class DownloadFileTask implements Runnable{
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
    }
}
```

Wywołujemy całość w metodzie main razem z drugim wątkiem sprawdzającym ciągle czy pole ``isDone`` uległo zmianie.
```
public class Main {
    public static void main(String[] args) throws InterruptedException {
        DownloadStatus status = new DownloadStatus();
        Thread thread1 = new Thread(new DownloadFileTask(status));
        Thread thread2 = new Thread(() -> {
            while (!status.isDone()) {};
            System.out.println(status.getBytes());
        });

        thread1.start();
        thread2.start();
    }
}
```

Bez zastosowania słowa ``volatile`` program się nie zakończy a drugi wątek będzie w nieskończoność odpytywał obiket 
``DownloadStatus`` ponieważ obydwa rdzenie przechowują to pole *lokalnie* w pamięci cache. **Zastosowanie słowa ``volatile``
wymusza przechowywanie pola w pamięci ram**.   
  
#### wait() oraz notify()
Powyższy przykład zawiera pewnien haczyk- implementacja drugiego wątku nieustannie sprawdza pole ``isDone`` co będzie 
ciąle obciążało procesor. Taka metoda będzie wywoływana miliardy razy.
```
Thread thread2 = new Thread(() -> {
    while (!status.isDone()) {};
    System.out.println(status.getBytes());
});
```
Można wykorzystać metodę ``wait()``, dzięki czemu warunek nie będzie ciągle sprawdzany. Metoda rzuca wyjątek i JVM
wymaga jej wykonania w ramach bloku ``synchronized``.
```
Thread thread2 = new Thread(() -> {
    while (!status.isDone()) {
        synchronized (status) {
            try {
                status.wait();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    };
    System.out.println("bytes: " + String.format("%,d", status.getBytes()));
});
```
Teraz, po drugiej stronie- w miejscu gdzie pole ``isDone`` jest zmieniane, należy wywołać metodę ``notify()`` lub ``notifyAll()``
w przypadku, gdyby zainteresowanych wątków było więcej. Ponownie należy to wykonać w ramach bluku ``synchronized``, w przeciwnym 
przypadku JVM zaprotestuje wyrzuceniem wyjątku ``RuntimeException``.
```
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
```
**WAŻNE!**- obydwie metody, ``notify()`` oraz ``wait()`` wymagają w ramach bloku ``synchronized`` tego samego obiektu. 
W tym przypadku jest to ``DownloadStatus``.


Wracając do strategii zapobiegania *Race Conditions* możemy jeszcze skorzystać z  
**Atomic objects**/  **obiekty atomiczne** jak ``AtomicInteger``. Obiekty te są bezpieczne wielowątkowo,
ponieważ operacje na nich wykonowane są atomiczne- wykonywane są w całości jako jedna, podobnie jak transakcje SQL.
Wracając do problemu z *race conditions*, możemy go rozwiązać zastępując pole ``private int bytes;`` na 
``private AtomicInteger bytes = new AtomicInteger();``
```
public class DownloadStatus {
    private AtomicInteger bytes = new AtomicInteger();

    public int getBytes() {
        return bytes.get();
    }

    public void increment(){
        bytes.getAndIncrement(); // odpowiednik bytes++, metoda incrementAndGet zadziała jak ++bytes
    }
}
```
Ta mała zmiana pozwala rozwiązać problem. Obiekty ``Atimic`` są wydajnym rozwiązaniem oferującym metody na których możemy
operować. Są domyślnym rozwiązaniem dla problemu *race conditions*.  
  
**LongAdder & DoubleAdder** to inne rozwiązanie, podobne do obiektów ``Atomic``, działają jeszcze wydajniej składając się 
z tablicy, do której dostęp może mieć więcej wątków a wywołanie metody ```...value``` spowoduje zsumowanie wartości.
```
public class DownloadStatus {
    private LongAdder bytes = new LongAdder();

    public int getBytes() {
        return bytes.intValue();
    }

    public void increment(){
        bytes.increment();
    }
}
```

### Synchronized Collections && Race conditions
Problem z *race conditions* występi również w przypadku kolekcji. W przykładzie poniżej w wydruku zobaczymy nieoczekiwany
wynik w postaci wyniku działania tylko jednego bądź drugiego wątku, różna może być kolejność w sytuacji gdy uda się 
wykonać całość. 
```
public class Main {
    public static void main(String[] args) throws InterruptedException {
        Collection<Integer> collection = new ArrayList<>();

        Thread thread1 = new Thread(() -> collection.addAll(Arrays.asList(1, 2, 3)));
        Thread thread2 = new Thread(() -> collection.addAll(Arrays.asList(4, 5, 6)));

        thread1.start();
        thread2.start();
        
        thread1.join();
        thread2.join();

        System.out.println(collection);
    }
}
```
możemy skorzystać z metod statycznych dostępnych w ramach klasy ``Collections`` np. ``Collections.synchronizedCollection()``
metoda oczekuje argumenu w postaci kolekcji, którą chcemy synchronizować.  
``Collection<Integer> collection = Collections.synchronizedCollection(new ArrayList<>());``  
zastosowanie synchronizowanej kolekcji rozwiązuje problem z *race condition*, trzeba pamiętać, że różna będzie kolejność
dodanych elementów w zależności od szybkości wywołania wszystkich wątków.  
  
**Concurrent collections** stanowią rozszerzenie kolekcji synchronizowanych, segmentując kolekcję w taki sposób, że kilka 
wątków jest w stanie działać jednocześnie na tej samej kolekcji, zakładając, że medyfikują różne jej segmenty. Wówczas
kolejkowane są jedynie wątki ubiegające się o dane w tym samym segmencie. Obiekty te tworzymy w podobny sposób co *zwyczajne* 
kolekcje, jednak z przedrostkiem *Concurrent*, np.  
``Map<Integer, String> map = new ConcurrentHashMap<>();``  
Tak utworzona mapa jest bezpieczna wielowątkowo. 