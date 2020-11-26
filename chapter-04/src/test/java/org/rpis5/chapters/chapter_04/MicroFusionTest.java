package org.rpis5.chapters.chapter_04;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.TimeUnit;

@Slf4j
class MicroFusionTest {
    
    @Test
    void concatMap() throws Exception {
        Scheduler pub1 = Schedulers.newParallel("pub1");
        Scheduler pub2 = Schedulers.newParallel("pub2");
    
        Flux.just(1, 2, 3)
                .publishOn(pub1)
                .concatMap(i -> Flux.range(0, i)
                        .publishOn(pub2))
                .subscribe(
                        e -> log.info("onNext : {}", e),
                        e -> log.error("onError", e),
                        () -> log.info("onComplete"),
                        subscription -> subscription.request(4)
                );
    
        TimeUnit.SECONDS.sleep(1);
    }
}
