package org.rpis5.chapters.chapter_06.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
public class EchoWebSocketHandler implements WebSocketHandler {        // (1)

    @Override                                                          // (2)
    public Mono<Void> handle(WebSocketSession session) {               //
        return session                                                 // (3)
            .receive()                                                 // (4)
            .map((WebSocketMessage webSocketMessage) -> webSocketMessage.getPayloadAsText())                   // (5)
            .map((String tm) -> "Echo: " + tm)                                  // (6)
            .doOnNext(e -> log.info("server : {}", e))
            .map((String payload) -> session.textMessage(payload))                                 // (7)
            .as((Flux<WebSocketMessage> messages) -> session.send(messages));                                        // (8)
    }                                                                  //
}
