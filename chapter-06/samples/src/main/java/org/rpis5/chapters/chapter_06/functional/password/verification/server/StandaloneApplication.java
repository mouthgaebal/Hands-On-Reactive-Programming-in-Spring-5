package org.rpis5.chapters.chapter_06.functional.password.verification.server;

import org.rpis5.chapters.chapter_06.functional.password.verification.client.PasswordDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.reactive.function.server.*;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;


public class StandaloneApplication {
   
    private static final Logger logger = LoggerFactory.getLogger(StandaloneApplication.class);

    public static void main(String... args) {
        long start = System.currentTimeMillis();
        HttpHandler httpHandler = RouterFunctions.toHttpHandler(
                routes(new BCryptPasswordEncoder(18))
        );
        
        ReactorHttpHandlerAdapter reactorHttpHandler = new ReactorHttpHandlerAdapter(httpHandler);

        DisposableServer server = HttpServer.create()
                                            .host("localhost")
                                            .port(8080)
                                            .handle(reactorHttpHandler)
                                            .bindNow();

        logger.debug("Started in " + (System.currentTimeMillis() - start) + " ms");

        server.onDispose()
              .block();
    }

    public static RouterFunction<ServerResponse> routes(PasswordEncoder passwordEncoder) {
        return RouterFunctions.route(
                RequestPredicates.POST("/password"),
                (ServerRequest request) -> request
                        .bodyToMono(PasswordDTO.class)
                        .map(p -> passwordEncoder.matches(p.getRaw(), p.getSecured()))
                        .flatMap(isMatched -> isMatched
                                ? ServerResponse.ok().build()
                                : ServerResponse.status(HttpStatus.EXPECTATION_FAILED).build()
                        )
        ).and(RouterFunctions.route(
                RequestPredicates.GET("/hello"),
                request -> ServerResponse.ok().bodyValue("hello"))
        ).andRoute(RequestPredicates.GET("/world"),
                request -> ServerResponse.ok().bodyValue("world")
        ).and(RouterFunctions.route()
                .GET("/hello-world", request -> ServerResponse.ok().bodyValue("hello-world"))
                .build()
        );
    }
}
