package org.rpis5.chapters.chapter_06.functional.password.verification.client;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static org.springframework.http.HttpStatus.EXPECTATION_FAILED;

public class DefaultPasswordVerificationService
        implements PasswordVerificationService {

    final WebClient webClient;

    public DefaultPasswordVerificationService(WebClient.Builder webClientBuilder) {
        webClient = webClientBuilder
            .baseUrl("http://localhost:8080")
            .build();
    }

    @Override
    public Mono<Void> check(String raw, String encoded) {
        return webClient
            .post()
            .uri("/password")
            .body(BodyInserters.fromPublisher(
                Mono.just(new PasswordDTO(raw, encoded)),
                PasswordDTO.class
            ))
            .exchange()
            .flatMap(response -> {
                if (response.statusCode().is2xxSuccessful()) {
                    return Mono.empty();
                }
                else if (response.statusCode() == EXPECTATION_FAILED) {
                    return Mono.error(new BadCredentialsException("Invalid credentials"));
                }
                return Mono.error(new IllegalStateException());
            });
    }
    
    public Mono<Void> check2(String raw, String encoded) {
        return webClient
                .post()
                .uri("/password")
                .body(Mono.just(new PasswordDTO(raw, encoded)), PasswordDTO.class)
                .exchange()
                .flatMap(response -> {
                    if (response.statusCode().is2xxSuccessful()) {
                        return Mono.empty();
                    }
                    else if (response.statusCode() == EXPECTATION_FAILED) {
                        return Mono.error(new BadCredentialsException("Invalid credentials"));
                    }
                    return Mono.error(new IllegalStateException());
                });
    }
    
    public Mono<Void> check3(String raw, String encoded) {
        return webClient
                .post()
                .uri("/password")
                .body(Mono.just(new PasswordDTO(raw, encoded)), PasswordDTO.class)
                .retrieve()
                .onStatus(httpStatus -> !httpStatus.is2xxSuccessful(),
                        (ClientResponse response) -> response.statusCode() == EXPECTATION_FAILED ?
                                Mono.error(new BadCredentialsException("Invalid credentials")) :
                                Mono.error(new IllegalStateException())
                ).bodyToMono(Void.class);
    }
    
}
