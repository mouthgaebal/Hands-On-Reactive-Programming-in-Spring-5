package org.rpis5.chapters.chapter_06.sse;

import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.Map;

@RestController
public class ServerSentController {

    private Map<String, StocksService> stringStocksServiceMap = Map.of("Impl", new StockServiceImpl());

    @GetMapping("/sse/stocks")
    public Flux<ServerSentEvent<?>> streamStocks() {
        return Flux
            .fromIterable(stringStocksServiceMap.values())
            .flatMap(StocksService::stream)
            .<ServerSentEvent<?>>map(item ->
                ServerSentEvent
                    .builder(item)
                    .event("StockItem")
                    .id(item.getId())
                    .build()
            )
            .startWith(
                ServerSentEvent
                    .builder()
                    .event("Stocks")
                    .data(stringStocksServiceMap.keySet())
                    .build()
            );
    }
    
    @GetMapping(value = "/sse/stocks2", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<StockItem> streamStocks2() {
        return Flux
                .fromIterable(stringStocksServiceMap.values())
                .flatMap(StocksService::stream);
    }
}
