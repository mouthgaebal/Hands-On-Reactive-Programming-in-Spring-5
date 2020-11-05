package org.rpis5.chapters.chapter_04;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.Disposable;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.IntStream;


@Slf4j
public class ReactorEssentialsTest {

    private final Random random = new Random();

    @Test
    @Ignore
    public void endlessStream() {
        Flux.interval(Duration.ofMillis(1))
            .collectList()
            .block();
    }

    @Test
    @Ignore
    public void endlessStream2() {
        Flux.range(1, 5)
            .repeat()
            .doOnNext(e -> log.info("E: {}", e))
            .take(100)
            .blockLast();
    }

    @Test
    @Ignore
    public void endlessStreamAndCauseAnError() {
        Flux.range(1, 100)
            .repeat()
            .collectList()
            .block();
    }

    @Test
    public void createFlux() {
        Flux<String> stream1 = Flux.just("Hello", "world");
        Flux<Integer> stream2 = Flux.fromArray(new Integer[]{1, 2, 3});
        Flux<Integer> stream3 = Flux.range(1, 500);
        IntStream intStream = IntStream.range(1, 500);

        Flux<String> emptyStream = Flux.empty();
        Flux<String> streamWithError = Flux.error(new RuntimeException("Hi!"));
    }

    @Test
    public void createMono() {
        Mono<String> stream4 = Mono.just("One");
        Mono<String> stream5 = Mono.justOrEmpty(null);
        Mono<String> stream6 = Mono.justOrEmpty(Optional.empty());

        Mono<String> stream7 = Mono.fromCallable(() -> httpRequest());
        Mono<String> stream8 = Mono.fromCallable(this::httpRequest);

        StepVerifier.create(stream8)
            .expectErrorMessage("IO error")
            .verify();

//        stream8.subscribe(System.out::println);

        Mono<Void> noData = Mono.fromRunnable(() -> doLongAction());

        StepVerifier.create(noData)
            .expectSubscription()
            .expectNextCount(0)
            .expectComplete()
            .verify();
    }

    private String httpRequest() {
        log.info("Making HTTP request");
        throw new RuntimeException("IO error");
    }

    private void doLongAction() {
        log.info("Long action");
    }

    @Test
    public void emptyOrError() {
        Flux<String> empty = Flux.empty();
        Mono<String> error = Mono.error(new RuntimeException("Unknown id"));
    }

    @Test
    public void shouldCreateDefer() {
        Mono<User> userMono = requestUserData("null");
        userMono.subscribe(System.out::println);
        userMono.subscribe(System.out::println);
        userMono.subscribe(System.out::println);

//        Mono<User> userMono = requestUserData(null);
//        StepVerifier.create(userMono)
//                .expectNextCount(0)
//                .expectErrorMessage("Invalid user id")
//                .verify();
    }

    public Mono<User> requestUserData(String userId) {
        return Mono.defer(() ->
                isValid(userId)
                        ? Mono.fromCallable(() -> requestUser(userId))
                        : Mono.error(new IllegalArgumentException("Invalid user id")));
    }

    @Test
    public void shouldCreateDefer2() {
        Mono<User> userMono2 = requestUserData2("null");
        userMono2.subscribe(System.out::println);
        userMono2.subscribe(System.out::println);
        userMono2.subscribe(System.out::println);

//        Mono<User> userMono2 = requestUserData2(null);
//        StepVerifier.create(userMono2)
//                .expectNextCount(0)
//                .expectErrorMessage("Invalid user id")
//                .verify();
    }

    public Mono<User> requestUserData2(String userId) {
        return isValid(userId)
                ? Mono.fromCallable(() -> requestUser(userId))
                : Mono.error(new IllegalArgumentException("Invalid user id"));
    }

    private boolean isValid(String userId) {
        log.warn("isValid!!!");
        return userId != null;
    }

    private User requestUser(String id) {
        return new User();
    }

    @ToString
    static class User {
        public String id, name;
    }

    @Test
    public void justOrEmpty() {
        final Mono<Object> justOrEmpty = Mono.justOrEmpty(null);
        justOrEmpty.subscribe(System.out::println);

        final Mono<Object> justOrEmpty2 = Mono.justOrEmpty("justOrEmpty2");
        justOrEmpty2.subscribe(System.out::println);
    }

    @Test
    public void fromMethods() {
        final Mono<String> fromSupplier = Mono.fromSupplier(() -> "가나다");
        final Mono<String> fromFuture = Mono.fromFuture(CompletableFuture.completedFuture("가나다"));
        final Mono<Object> fromRunnable = Mono.fromRunnable(() -> {});

        final Flux<Integer> fromArray = Flux.fromArray(new Integer[]{1, 2, 3});
        final Flux<Integer> fromIterable = Flux.fromIterable(List.of(1, 2, 3));
    }

    @Test
    public void simpleSubscribe() {
        Flux.just("A", "B", "C")
                .subscribe(
                        data -> log.info("onNext : {}", data),
                        errorIgnored -> {},
                        () -> log.info("onComplete"));
    }

    @Test
    public void simpleSubscribe2() {
        Flux.range(1, 100)
                .subscribe(
                        data -> log.info("onNext : {}", data),
                        errorIgnored -> {},
                        () -> log.info("onComplete"),
                        subscription -> {
                            subscription.request(4);
                            subscription.cancel();
                        });
    }

    @Test
    public void managingSubscription() throws InterruptedException {
        Disposable disposable = Flux.interval(Duration.ofMillis(50))
            .doOnCancel(() -> log.info("Cancelled"))
            .subscribe(
                data -> log.info("onNext: {}", data)
            );
        Thread.sleep(200);
        log.warn("dispose() 호출");
        disposable.dispose();
    }

    @Test
    public void subscribingOnStream() throws Exception {
        Subscriber<String> subscriber = new Subscriber<String>() {
            volatile Subscription subscription;

            @Override
            public void onSubscribe(Subscription s) {
                subscription = s;
                log.info("initial request for 1 element");
                subscription.request(1);
            }

            public void onNext(String s) {
                log.info("onNext: {}", s);
                log.info("requesting 1 more element");
                subscription.request(1);
            }

            public void onComplete() {
                log.info("onComplete");
            }

            public void onError(Throwable t) {
            }
        };

        Flux<String> stream = Flux.just("Hello", "world", "!");
        stream.subscribe(subscriber);
    }

    @Test
    public void mySubscriber() {
        Flux.just("A", "B", "C")
            .subscribe(new MySubscriber<>());
    }

    public static class MySubscriber<T> extends BaseSubscriber<T> {

        public void hookOnSubscribe(Subscription subscription) {
            System.out.println("Subscribed");
            request(1);
        }

        public void hookOnNext(T value) {
            System.out.println(value);
            request(1);
        }
    }

    @Test
    public void simpleRange() {
        Flux.range(2010, 9)
            .subscribe(y -> System.out.print(y + ","));
    }

    @Test
    public void cast() {
        Object a = 1;
        Object b = 2;
        Object c = 3;

        Flux.just(a, b, c)
                .cast(Integer.class)
                .reduce(Integer::sum)
                .subscribe(System.out::println);
    }

    @Test
    public void indexElements() {
        Flux.range(2018, 5)
                .timestamp()
                .doOnNext(t2 -> log.info("timeStamp() : {}", t2))
                .index()
                .subscribe(e -> log.info("index: {}, ts: {}, value: {}",
                        e.getT1(),
                        Instant.ofEpochMilli(e.getT2().getT1()),
                        e.getT2().getT2()));
    }

    @Test
    public void startStopStreamProcessing() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        Mono<?> startCommand = Mono.delay(Duration.ofSeconds(1));
        Mono<?> stopCommand = Mono.delay(Duration.ofSeconds(3));
        Flux<Long> streamOfData = Flux.interval(Duration.ofMillis(100));

        streamOfData
            .skipUntilOther(startCommand) // 이때까지 건너뜀
            .takeUntilOther(stopCommand) // 이때까지만 구독
            .doOnComplete(latch::countDown)
            .subscribe(e -> log.info("{}", e));

        latch.await();
    }

    @Test
    public void collectSort() {
        Flux.just(1, 6, 2, 8, 3, 1, 5, 1)
            .collectSortedList(Comparator.reverseOrder())
            .subscribe(System.out::println);
    }

    @Test
    public void distinct() {
        Flux.just(1, 1, 1, 2, 2, 3, 2, 1, 1, 4)
                .distinct()
                .collectList()
                .subscribe(System.out::println);
    }

    @Test
    public void distinctUntilChanged() {
        Flux.just(1, 1, 1, 2, 2, 3, 2, 1, 1, 4)
                .distinctUntilChanged()
                .collectList()
                .subscribe(System.out::println);
    }

    @Test
    public void findingIfThereIsEvenElements() {
        Flux.just(3, 5, 7, 9, 11, 15, 16, 17)
            .any(e -> e % 2 == 0)
            .subscribe(hasEvens -> log.info("Has evens: {}", hasEvens));
    }

    @Test
    public void findingIfThereIsAllEvenElements() {
        Flux.just(3, 5, 7, 9, 11, 15, 16, 17)
                .all(e -> e % 2 == 0)
                .subscribe(allEvens -> log.info("all evens: {}", allEvens));
    }

    @Test
    public void reduceExample() {
        Flux.range(1, 5)
                .reduce(0, (acc, elem) -> acc + elem)
                .subscribe(result -> log.info("Result: {}", result));
    }

    @Test
    public void scanExample() {
        Flux.range(1, 5)
            .scan(0, (acc, elem) -> acc + elem)
            .subscribe(result -> log.info("Result: {}", result));
    }

    @Test
    public void runningAverageExample() {
        int bucketSize = 5;
        Flux.range(1, 500)
            .index()
            .scan(
                new int[bucketSize],
                (acc, elem) -> {
                    acc[(int) (elem.getT1() % bucketSize)] = elem.getT2();
                    return acc;
                })
            .doOnNext(e -> log.warn("before skip : {}", Arrays.toString(e)))
            .skip(bucketSize)
            .doOnNext(e -> log.warn("after skip : {}", Arrays.toString(e)))
            .map(array -> Arrays.stream(array).sum() * 1.0 / bucketSize)
            .subscribe(av -> log.info("Running average: {}", av));
    }

    @Test
    public void thenOperator() {
        Flux.just(1, 2, 3)
            .thenMany(Flux.just(5, 6))
            .subscribe(e -> log.info("onNext: {}", e));
    }

    @Test
    public void concatOperator() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Flux.concat(
                Flux.range(1, 3).delayElements(Duration.ofMillis(500L), Schedulers.newElastic("s-01")),
                Flux.range(4, 2).delayElements(Duration.ofMillis(300L), Schedulers.newElastic("s-02")),
                Flux.range(6, 5).delayElements(Duration.ofMillis(200L), Schedulers.newElastic("s-03"))
        ).doOnComplete(latch::countDown)
                .subscribe(value -> log.info("concat - onNext: {}", value));

        latch.await();
    }

    @Test
    public void mergeOperator() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Flux.merge(
                Flux.range(1, 3).delayElements(Duration.ofMillis(500L), Schedulers.newElastic("s-01")),
                Flux.range(4, 2).delayElements(Duration.ofMillis(300L), Schedulers.newElastic("s-02")),
                Flux.range(6, 5).delayElements(Duration.ofMillis(200L), Schedulers.newElastic("s-03"))
        ).doOnComplete(latch::countDown)
                .subscribe(value -> log.info("merge - onNext: {}", value));

        latch.await();
    }

    @Test
    public void zipOperator() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Flux.zip(
                Flux.range(1, 3).delayElements(Duration.ofMillis(500L), Schedulers.newElastic("s-01")),
                Flux.range(4, 2).delayElements(Duration.ofMillis(300L), Schedulers.newElastic("s-02")),
                Flux.range(6, 5).delayElements(Duration.ofMillis(200L), Schedulers.newElastic("s-03"))
        ).doOnComplete(latch::countDown)
                .subscribe(value -> log.info("zip - onNext: {}", value));

        latch.await();
    }

    @Test
    public void combineLatestOperator() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Flux.combineLatest(
                Flux.range(1, 3).delayElements(Duration.ofMillis(500L), Schedulers.newElastic("s-01")),
                Flux.range(4, 2).delayElements(Duration.ofMillis(300L), Schedulers.newElastic("s-02")),
                Flux.range(6, 5).delayElements(Duration.ofMillis(200L), Schedulers.newElastic("s-03")),
                Arrays::asList
        ).doOnComplete(latch::countDown)
                .subscribe(value -> log.info("combineLatest - onNext: {}", value));

        latch.await();
    }

    @Test
    public void bufferBySize() {
        Flux.range(1, 13)
            .buffer(4)
            .subscribe(e -> log.info("onNext: {}", e));
    }

    @Test
    public void windowByPredicate() {
        Flux<Flux<Integer>> fluxFlux = Flux.range(101, 20)
            .windowUntil(this::isPrime, true);
//            .windowUntil(this::isPrime, false);
//            .window(5);

        fluxFlux.subscribe(window -> window
            .collectList()
            .subscribe(e -> log.info("window: {}", e)));
    }

    @Test
    public void groupByExample() {
        Flux.range(1, 7)
            .groupBy(e -> e % 2 == 0 ? "Even" : "Odd")
            .subscribe(groupFlux -> {
                Flux<LinkedList<Integer>> scan = groupFlux.scan(
                        new LinkedList<>(),
                        (list, elem) -> {
                            if(list.size() > 1) {
                                list.remove(0);
                            }
                            list.add(elem);
                            return list;
                        });

                scan.filter(arr -> !arr.isEmpty())
                        .subscribe(data -> log.info("{}: {}", groupFlux.key(), data));
            });
    }

    @Test
    public void flatMapExample() throws InterruptedException {
        Flux.just("user-1", "user-2", "user-3")
            .flatMap(u -> requestBooks(u)
                .map(b -> u + "/" + b))
            .subscribe(r -> log.info("onNext: {}", r));

        Thread.sleep(1000);
    }

    private Flux<String> requestBooks(String user) {
        return Flux.range(1, getRandomCount(user))
                .delayElements(Duration.ofMillis(3))
                .map(i -> "book-" + i);
    }

    private int getRandomCount(String user) {
        int rnd = random.nextInt(3) + 1;
        log.warn("randomCount : {} :: {}", user, rnd);
        return rnd;
    }


    @Test
    public void sampleExample() throws InterruptedException {
        Flux.range(1, 100)
            .delayElements(Duration.ofMillis(1))
            .sample(Duration.ofMillis(50))
            .subscribe(e -> log.info("onNext: {}", e));

        Thread.sleep(1000);
    }

    @Test
    public void doOnExample() {
        Flux.just(1, 2, 3)
            .concatWith(Flux.error(new RuntimeException("Conn error")))
            .doOnEach(s -> log.info("signal: {}", s))
            .subscribe();
    }

    @Test
    public void signalProcessing() {
        Flux.range(1, 3)
            .doOnNext(e -> System.out.println("data  : " + e))
            .materialize()
            .doOnNext(e -> System.out.println("signal: " + e))
            .dematerialize()
            .collectList()
            .subscribe(r -> System.out.println("result: " + r));
    }

    @Test
    public void signalProcessingWithLog() {
        Flux.range(1, 3)
            .log("FluxEvents")
            .subscribe(e -> {}, e -> {}, () -> {}, s -> s.request(2));
    }

    @Test
    public void usingPushOperator() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Flux.push(emitter -> {
            emitter.onDispose(() -> log.info("Disposed"));
            IntStream.range(20, 30)
                    .parallel()
                    .forEach(t -> emitter.next(Thread.currentThread().getName() + " : " + t));
            emitter.complete();
        })
                .delayElements(Duration.ofMillis(1))
                .doOnComplete(latch::countDown)
                .subscribe(e -> log.info("onNext: {}", e));

        latch.await();
    }

    @Test
    public void usingCreateOperator() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Flux.create(emitter -> {
            emitter.onDispose(() -> log.info("Disposed"));

            IntStream.range(20, 30)
                    .parallel()
                    .forEach(t -> emitter.next(Thread.currentThread().getName() + " : " + t));
            emitter.complete();
        }).delayElements(Duration.ofMillis(1))
                .doOnComplete(latch::countDown)
                .subscribe(e -> log.info("onNext: {}", e));

        latch.await();
    }

    @Test
    public void usingGenerate() throws InterruptedException {
        Flux.generate(
                () -> Tuples.of(0L, 1L), // 초기값
                (state, sink) -> {
                    log.info("generated value: {}", state.getT2());
                    sink.next(state.getT2());
                    long newValue = state.getT1() + state.getT2();
                    return Tuples.of(state.getT2(), newValue);
                })
                .take(7)
                .subscribe(e -> log.info("onNext: {}", e));
    }

    @Test
    public void tryWithResources() {
        try (Connection conn = Connection.newConnection()) {
            conn.getData().forEach(
                data -> log.info("Received data: {}", data)
            );
        } catch (Exception e) {
            log.info("Error: {}", e.getMessage());
        }
    }
    @Test
    public void usingOperator() {
        Flux<String> ioRequestResults = Flux.using(
            Connection::newConnection,
            connection -> Flux.fromIterable(connection.getData()),
            Connection::close
        );

        ioRequestResults
            .subscribe(
                data -> log.info("Received data: {}", data),
                e -> log.info("Error: {}", e.getMessage()),
                () -> log.info("Stream finished"));
    }

    static class Connection implements AutoCloseable {
        private final Random rnd = new Random();

        static Connection newConnection() {
            log.info("IO Connection created");
            return new Connection();
        }

        public Iterable<String> getData() {
            if (rnd.nextInt(10) < 3) {
                throw new RuntimeException("Communication error");
            }
            return Arrays.asList("Some", "data");
        }

        @Override
        public void close() {
            log.info("IO Connection closed");
        }
    }

    @Test
    public void usingWhenExample() throws InterruptedException {
        Flux.usingWhen(
                Transaction.beginTransaction(),
                transaction -> transaction.insertRows(Flux.just("A", "B")),
                Transaction::commit,
                Transaction::rollback
        ).subscribe(
            d -> log.info("onNext: {}", d),
            e -> log.info("onError: {}", e.getMessage()),
            () -> log.info("onComplete")
        );

        Thread.sleep(1000);
    }

    @Test
    public void usingWhenExample1() throws InterruptedException {
        Flux.usingWhen(
                Transaction.beginTransaction(),
                transaction -> transaction.insertRows(Flux.just("A", "B")),
                transaction -> transaction.commit(), // complete
                (transaction, throwable) -> transaction.rollback(), // error
                transaction -> transaction.rollback() // cancel
        ).subscribe(
                d -> log.info("onNext: {}", d),
                e -> log.error("onError: {}", e.getMessage()),
                () -> log.info("onComplete")
        );

        Thread.sleep(1000);
    }

    static class Transaction {

        private static final Random random = new Random();

        private final int id;

        public Transaction(int id) {
            this.id = id;
            log.info("[T: {}] created", id);
        }

        public static Mono<Transaction> beginTransaction() {
            return Mono.defer(() ->
                    Mono.just(new Transaction(random.nextInt(1000))));
        }

        public Flux<String> insertRows(Publisher<String> rows) {
            return Flux.from(rows)
                    .delayElements(Duration.ofMillis(100))
                    .flatMap(row -> {
                        if (random.nextInt(10) < 2) {
                            return Mono.error(new RuntimeException("Error on: " + row));
                        } else {
                            return Mono.just(row);
                        }
                    });
        }

        public Mono<Void> commit() {
            return Mono.defer(() -> {
                log.info("[T: {}] commit", id);
                if (random.nextBoolean()) {
                    return Mono.empty();
                } else {
                    return Mono.error(new RuntimeException("Conflict"));
                }
            });
        }

        public Mono<Void> rollback() {
            return Mono.defer(() -> {
                log.info("[T: {}] rollback", id);
                if (random.nextBoolean()) {
                    return Mono.empty();
                } else {
                    return Mono.error(new RuntimeException("Conn error"));
                }
            });
        }

    } // Transaction

    @Test
    public void managingDemand() {
        Flux.range(1, 100)
            .subscribe(
                data -> log.info("onNext: {}", data),
                err -> { /* ignore */ },
                () -> log.info("onComplete"),
                subscription -> {
                    subscription.request(4);
                    subscription.cancel();
                }
            );
    }

    @Test
    public void handlingErrorsWithDelay() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        Flux.just("user-1")
                .flatMap(user ->
                        recommendedBooksWithDelay(user)
                                .retryWhen(Retry.backoff(5, Duration.ofMillis(100)))
                                .timeout(Duration.ofSeconds(1))
                                .onErrorResume(e -> {
                                    log.error("onErrorResume : {}", e.getMessage());
                                    return Flux.just("The Martian");
                                })
                )
                .doOnComplete(latch::countDown)
                .subscribe(
                        b -> log.info("onNext: {}", b),
                        e -> log.warn("onError: {}", e.getMessage()),
                        () -> log.info("onComplete")
                );

        latch.await();
    }

    public Flux<String> recommendedBooksWithDelay(String userId) {
        return Flux.defer(() ->
                Flux.just("Blue Mars", "The Expanse").delayElements(Duration.ofMillis(1100))
        ).doOnSubscribe(s -> log.info("Request for {}", userId));
    }

    @Test
    public void handlingErrorsWithRuntimeException() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        Flux.just("user-1")
            .flatMap(user ->
                recommendedBooksWithRuntimeException(user)
                    .retryWhen(Retry.backoff(5, Duration.ofMillis(100)))
                    .timeout(Duration.ofSeconds(1))
                        .onErrorResume(e -> {
                            log.error("onErrorResume : {}", e.getMessage());
                            return Flux.just("The Martian");
                        })
            )
            .doOnComplete(latch::countDown)
            .subscribe(
                b -> log.info("onNext: {}", b),
                e -> log.warn("onError: {}", e.getMessage()),
                () -> log.info("onComplete")
            );

        latch.await();
    }

    public Flux<String> recommendedBooksWithRuntimeException(String userId) {
        return Flux.defer(() -> Flux.<String>error(new RuntimeException("Conn error")))
                .doOnSubscribe(s -> log.info("Request for {}", userId));
    }

    @Test
    public void coldPublisher() {
        Flux<String> coldPublisher = Flux.defer(() -> {
            log.info("Generating new items");
            return Flux.just(UUID.randomUUID().toString());
        });

        log.info("No data was generated so far");
        coldPublisher.subscribe(e -> log.info("onNext: {}", e));
        coldPublisher.subscribe(e -> log.info("onNext: {}", e));
        log.info("Data was generated twice for two subscribers");
    }

    @Test
    public void connectExample() {
        Flux<Integer> source = Flux.range(0, 3)
            .doOnSubscribe(s ->
                log.info("new subscription for the cold publisher"));

        ConnectableFlux<Integer> conn = source.publish();

        conn.subscribe(e -> log.info("[Subscriber 1] onNext: {}", e));
        conn.subscribe(e -> log.info("[Subscriber 2] onNext: {}", e));

        log.info("all subscribers are ready, connecting");
        conn.connect();
    }

    @Test
    public void cachingExample() throws InterruptedException {
        Flux<Integer> source = Flux.range(0, 2)
            .doOnSubscribe(s ->
                log.info("new subscription for the cold publisher"));

        Flux<Integer> cachedSource = source.cache(Duration.ofSeconds(1));

        cachedSource.subscribe(e -> log.info("[S 1] onNext: {}", e));
        cachedSource.subscribe(e -> log.info("[S 2] onNext: {}", e));

        Thread.sleep(1200);

        cachedSource.subscribe(e -> log.info("[S 3] onNext: {}", e));
    }

    @Test
    public void replayExample() throws InterruptedException {
        Flux<Integer> source = Flux.range(0, 5)
            .delayElements(Duration.ofMillis(100))
            .doOnSubscribe(s ->
                log.info("new subscription for the cold publisher"));

        Flux<Integer> cachedSource = source.share();

        cachedSource.subscribe(e -> log.info("[S 1] onNext: {}", e));
        Thread.sleep(400);
        cachedSource.subscribe(e -> log.info("[S 2] onNext: {}", e));

        Thread.sleep(1000);
    }

    @Test
    public void elapsedExample() throws InterruptedException {
        Flux.range(0, 5)
            .delayElements(Duration.ofMillis(100))
            .elapsed()
            .subscribe(e -> log.info("Elapsed {} ms: {}", e.getT1(), e.getT2()));

        Thread.sleep(1000);
    }

    @Test
    public void transformExample() {
        Function<Flux<String>, Flux<String>> logUserInfo =
            stream -> stream
                .index()
                .doOnNext(tp ->
                    log.info("[{}] User: {}", tp.getT1(), tp.getT2()))
                .map(Tuple2::getT2);

        Flux.range(1000, 3)
            .map(i -> "user-" + i)
            .transform(logUserInfo)
            .subscribe(e -> log.info("onNext: {}", e));
    }

    @Test
    public void composeExample() {
        Function<Flux<String>, Flux<String>> logUserInfo = (stream) -> {
            if (random.nextBoolean()) {
                return stream
                    .doOnNext(e -> log.info("[path A] User: {}", e));
            } else {
                return stream
                    .doOnNext(e -> log.info("[path B] User: {}", e));
            }
        };

        Flux<String> publisher = Flux.just("1", "2")
            .compose(logUserInfo);

        publisher.subscribe();
        publisher.subscribe();
    }

    public boolean isPrime(int number) {
        return number > 2
            && IntStream.rangeClosed(2, (int) Math.sqrt(number))
            .noneMatch(n -> (number % n == 0));
    }

}
