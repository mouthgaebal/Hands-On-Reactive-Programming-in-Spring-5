/**
 * Copyright (C) Zoomdata, Inc. 2012-2018. All rights reserved.
 */
package org.rpis5.chapters.chapter_04;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.Assert.fail;

@Slf4j
public class ThreadLocalProblemShowcaseTest {

    @Test
    public void shouldFailDueToDifferentThread() {
        ThreadLocal<Map<Object, Object>> threadLocal = new ThreadLocal<>();
        threadLocal.set(new HashMap<>());
        
        try {
            Flux.range(0, 10)
                    .doOnNext(k -> threadLocal.get().put(k, new Random(k).nextGaussian()))
                    .publishOn(Schedulers.parallel())
                    .map(k -> threadLocal.get().get(k))
                    .blockLast();
    
            fail("NullPointerException 안났다!!!");
        } catch (NullPointerException e) {
            log.error("NullPointerException 났다!!", e);
        }
    }
    
    @Test
    public void useContext() throws Exception {
        final String key = "randoms";
    
        List<Double> result = Flux.range(0, 10)
                .flatMap(k ->
                        Mono.subscriberContext()
                                .doOnNext(context -> {
                                    Map<Integer, Double> map = context.get(key);
                                    map.put(k, new Random(k).nextGaussian());
                                })
                                .thenReturn(k)
                )
                .publishOn(Schedulers.parallel())
                .flatMap(k ->
                        Mono.subscriberContext()
                                .map(context -> {
                                    Map<Integer, Double> map = context.get(key);
                                    return map.get(k);
                                })
                )
                .subscriberContext(context -> context.put(key, new HashMap<Integer, Double>()))
                .collectList()
                .block();
    
        System.out.println(result);
    }
}
