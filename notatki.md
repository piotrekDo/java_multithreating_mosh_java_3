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

## The Executive Framework
Dostarcza wygodne narzędzia do pracy z wątkami takie jak 
- pule wątków,
- obiekty Executors,
- interfejsy Collable oraz Future,
- programowanie asynchroniczne,
- obiekty Completable Futures

## Thread Pools- pula wątków.
Zwyczajowo korzystanie z wielu wątków jest kosztowe dla pamięci i wydajności. Dla każdego zadania należy utworzyć osobny
obiekt wątku co nie jest wydajne. W ramach *Executive Framework* możemy skorzystać z puli wątków a więc wykorzystywać
ten sam wątek wielokrotnie zamiast niszczyć go i tworzyć kolejny. 

### Executors
W ramach pakietu ``java.util.concurrent`` otrzymujemy interfejs ``ExecutorService`` oraz kilka jego implementacji 
pozwalających na zarządzanie wątkami. Są to:
- ThreadPoolExecutor,
- ScheduledThreadPoolExecutor,
- ForkJoinPool,
- AbstractExecutorService

Tworzenie tych obiektów jest kłopotliwe i ich konstruktory wymagają wielu argumentów. Możemy wykorzystać metody pochodzące
z klasy ``Executors`` pozwalające wygodnie tworzyć pule wątków. Dla przykładu wywołanie metody 
``ExecutorService executorService = Executors.newFixedThreadPool(2);`` utworzy nam implementację ``ThreadPoolExecutor``
z dwoma wątkami w puli. Teraz z pomocą metody ``submit()`` przekazujemy obiekt ``Runnable`` lub ``Callable`` reprezentujący
zadanie do wykonania w oddzielnym wątku.  
``executorService.submit(()-> System.out.println(Thread.currentThread().getName()));``  
uruchomienie ``Executor'a`` pozostawi działanie programu w zawieszeniu oczekując na kolejne zadania, które mogą nadejść
w przyszłośći. Chcąc zamknąć program po wykonaniu całości zleconych zadań wywołujemy metodę ``shutdown()``, istnieje 
również metoda ``shutdownNow()`` różniąca się tym, że przerwya wykonywane zadanie, podczas gdy ``shutdown()`` czeka na jego
wykonanie przed zamknięciem. 
**Dobrą praktyką jest wykonywanie zadań w ramach ExecutoService w ramach bloku try/catch. ExecutorService implementuje
interfejs AutoCloseable**  

## Interfejsy Collable oraz Future
Interfejs ``Collable`` podobnie jak ``Runnable`` pozwala na wykonywanie zadań wielowątkowo, z tą różnicą że w wyniku 
swojego działania zwraca obiekt generyczny ``Future``.
```
Future<Integer> future = executorService.submit(() -> {
    System.out.println(Thread.currentThread().getName());
    return 1;
});
```

Poniższy przykład prezentuje proste wywołanie obiektu ``Future`` opóźnionego o 3 sekundy. Wywołanie metody ``get`` zamrozi 
bieżący wątek do czasu wykonania zadania. Metoda ``get`` przyjmuje też argument w postaci ``Timeout``, gdzie możemy określić
maksymalny czas przewidziany na wykoananie zadania. 
```
ExecutorService executorService = Executors.newFixedThreadPool(2);

Future<Integer> future = executorService.submit(() -> {
    try {
        Thread.sleep(3000);
    } catch (InterruptedException e) {
        e.printStackTrace();
    }
    return 1;
});

Integer integer = future.get();
```
Po wywołaniu metody ``get`` otrzymujemy już oczekiwany obiekt, zamiast wrappera ``Future``. Metoda ``get`` poza ``InterruptedException``
wyrzuca również wyjątek ``ExecutionException`` oznaczający problem powstały w wyniku pozyskiwania obiektu. 

## Programowanie asynchroniczne- CompletableFuture<T>
Przykład prezentujący działanie ``ExecutorService`` niesie ze sobą pewien problem- blokujemy aplikację na czas wykonania
osobnego wątku co mija się z założeniami wielowątkowości. Obiekty te powołujemy z pomocą metod zawartych w klasie 
``CompletableFuture``, np. ``CompletableFuture.runAsync()`` przyjmuje jako argument implementację interfejsu ``Runnable``
i wykonuje metodę ``void``. Jako drugi argument możemy przekazać obiekt ``ExecutorService`` z pulą wątków. Jeżeli tego nie 
zrobimy zostanie wykorzystana tzw. *common pool*. Jest to pula powoływana przez ``CompletableFuture`` w ramach metody 
``ForkJoinPool.commonPool``. Można ją skonsfigurować, tak by otrzymać oczekiwaną liczbę wyjątków. Domyślna liczba jest
powiązana z ilością rdzeni procesora.  
Inną metodą w ramach klasy ``CompletableFuture`` jest ``supplyAsync`` tym razem oczekującej implementacji interfejsu ``supplier``
i opcjonalnie puli wątków. 
```
Supplier<Integer> supplier = () -> 1;
CompletableFuture<Integer> integerCompletableFuture = CompletableFuture.supplyAsync(supplier);
```
Na obiekcie ``CompletableFuture`` możemy wywołać metodę ``get`` jednak blokuje ona działanie programu i nie działa 
asynchronicznie. Jej wywołanie nie różni się od metody ``get`` z obiektu ``Future``.  

### Wykorzystanie obiektu CompletableFuture w sposób asynchroniczny
W poniższym przykładzie symulujemy wysyłanie wiadomości e-mail, co zwykle jest czasochłonnym zajęciem i powinno być 
wykonywane asynchronicznie. Synchroniczne wywołanie metody możemy owrapować wewnątrz ``CompletableFuture``
```
public class FakeMailService {
    void send() {
        LongTask.simulate(); // symuluje 3 sekundy opóźnienia.
        System.out.println("Mail was sent.");
    }

    CompletableFuture<Void> sendAsync() {
        return CompletableFuture.runAsync(this::send);
    }
}
```
Konwencją nazewnictwa jest końcówka *Async* sugerująca, że dana metoda jest asynchroniczna właśnie. 
```
FakeMailService fakeMailService = new FakeMailService();
fakeMailService.sendAsync();
System.out.println("Hello world");
```
Dzięki takiemu działaniu najpierw zobaczymy tekst *Hello world* a następnie, po 3 sekundach naszą wiadomość pochodzącą 
z medoty ``sendAsync``
**W przypadku aplikacji konsolowych możemy nie doczekać wyświetlnia tekstu, ponieważ aplikacje te wykonują się imperatywnie,
linia po linii. Chcąc zobaczyć efekt musimy "zawiesić" działanie programu, np. poprzez metodę ``sleep`` albo przez utworzenie
puli wątków co utrzyma działanie programu.**

### Reagowanie na wykonanie obiektu CompletableFuture
Na obiekcie CompletableFuture możemy wywołać jedną z metod pochodzących z interfejsu [CompletionStage](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletionStage.html)
pozwalających zareagować na ukończenie zadania. Jedną z takich metod może byc ``thenRun`` przyjmującą jako argument 
obiekt ``Runnable`` pozwalający wykonać jakiś fragment kodu, lub np. ``thenRunAsync`` pozwalającą wykonać kolejne, 
asynchroniczne zadanie. Poniżej przykład metody przyjmującej interfejs funkcyjny ``Consumer`` pozwalający skonsumować
otrzymany obiekt
```
CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> 1);
future.thenAccept(result -> System.out.println(result));
```

### Obsługa wyjątków w pracy z obiektami CompletableFuture
Czasami nasze zadanie nie może zostać wykonane, wówczas musimy obsłużyć takie zdarzenie stosując metodę ``get``.
```
public static void main(String[] args)  {
    CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
        System.out.println("Getting data...");
        throw new IllegalStateException("Error connecting");
    });

    try {
        future.get();
    } catch (InterruptedException e) {
        e.printStackTrace();
    } catch (ExecutionException e) {
        e.getCause();
        e.printStackTrace();
    }
}
```

Opcjonalnie można wykorzystać metodę ``exceptionally`` zwracającą kolejny ``CompletableFuture``. Wówczas wywołujemy
na nim metodę ``get``
```
CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
    System.out.println("Getting data...");
    throw new IllegalStateException("Error connecting");
});

try {
    Integer integer = future.exceptionally(throwable -> -1).get();
    System.out.println(integer);
} catch (InterruptedException e) {
    e.printStackTrace();
} catch (ExecutionException e) {
    e.getCause();
    e.printStackTrace();
}
```

**Metodę ``get`` można wywołać w ramach innej metody asynchronicznej, np ``thenRun``. Wówczas metoda ``get`` nie zmrozi
działania programu**

### Łączenie kilku asynchronicznych zadań
Z pomocą metody ``thenCombine`` wywołanej na jednym ``CompletableFuture`` możemy połączyć kilka zadań.
Metoda ``thenCombine`` przyjmuje dwa argumenty- ``CompletionStage`` reprezntujący drugą wartość oraz interfejs ``BiFunction``
gdzie wykonamy kalkulację:
```
CompletableFuture<Integer> first = CompletableFuture.supplyAsync(() -> 20);
CompletableFuture<Double> second = CompletableFuture.supplyAsync(() -> 0.9);

first.thenCombine(second, (price, exchangeRate) -> price * exchangeRate);
```

Wynikiem działania metody będzie kolejny ``CompletableFuture``. Możemy więc wywołać metodę ``.thenAccept(Sytem.out::print)``.  
W ciekawszych przypadkach jedna z metod pobierających dane mogłaby zwracać ``String``. W takiej sytuacji trzeba go najpierw zmappować.
```
CompletableFuture<Integer> first = CompletableFuture.supplyAsync(() -> "20USD")
        .thenApply(str -> {
            String priceString = str.replace("USD", "");
            return Integer.parseInt(priceString);
        });
CompletableFuture<Double> second = CompletableFuture.supplyAsync(() -> 0.9);

first.thenCombine(second, (price, exchangeRate) -> price * exchangeRate)
        .thenAccept(System.out::println);
```
  
  
Poniżej przykład, gdzie pobieramy dane z trzech źródeł niezależnie. Wykorzystujemy metodę ``CompletableFuture.allOf`` do
wyczekania na wszystkie rezultaty. Wewnątrz metody ``thenRun`` wywołujemy ``get`` na rezultatach, ale to nie zmrozi całości
aplikacji. Metoda ``Thread.sleep(5000)`` jest konieczna z uwagi na konsolowe środowisko aplikacji. 

```
CompletableFuture<String> price = CompletableFuture.supplyAsync(() -> "20USD");
CompletableFuture<Integer> items = CompletableFuture.supplyAsync(() -> {
    LongTask.simulate(); // symulacja oczekiwania na dane kilka sekund
    return 19;
});
CompletableFuture<Double> exRate = CompletableFuture.supplyAsync(() -> 0.9);

CompletableFuture.allOf(price, items, exRate)
        .thenRun(() -> {
            try {
                String priceString = price.get();
                Integer itemsTotal = items.get();
                Double exchangeRate = exRate.get();

                int pricePerItem = Integer.parseInt(priceString.replace("USD", ""));
                double result = pricePerItem * itemsTotal * exchangeRate;
                System.out.println(result);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        });

Thread.sleep(5000);
```

#### Metoda anyOf
Metoda o działaniu zbliżonym do ``allOf``, przy czym ``anyOf`` wykona się, gdy tylko jedna z wartości się spełni, podczas gdy
``allOf`` czeka na ukończenie wszyskich zadań. Również zwraca ``CompletableFuture``