package org.rpis5.chapters.chapter_06.sse;

import reactor.core.publisher.Flux;

import java.time.Duration;

public class StockServiceImpl implements StocksService {
    @Override
    public Flux<StockItem> stream() {
        return Flux.interval(Duration.ofMillis(500))
                .map(i -> new StockItem(String.valueOf(i), "type : " + i));
    }
}
