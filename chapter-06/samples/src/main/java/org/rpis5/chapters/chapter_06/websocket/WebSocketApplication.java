package org.rpis5.chapters.chapter_06.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.time.Duration;

@Slf4j
@SpringBootApplication
public class WebSocketApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebSocketApplication.class, args);
    }

    @Bean
    public CommandLineRunner commandLineRunner() {
        return (args) -> {
            ReactorNettyWebSocketClient client = new ReactorNettyWebSocketClient();

            client.execute(
                    URI.create("http://localhost:8080/ws/echo"),
                    (WebSocketSession session) -> {
                        session.receive()
                                .map((WebSocketMessage webSocketMessage) -> webSocketMessage.getPayloadAsText())
                                .subscribe(e -> log.info("receive : {}", e));
                        
                        return Flux.interval(Duration.ofMillis(500))
                                .map(String::valueOf)
                                .map(session::textMessage)
                                .doOnNext(e -> log.info("send : {}", e.getPayloadAsText()))
                                .as(session::send);
                    }
            ).subscribe();
        };
    }
}
