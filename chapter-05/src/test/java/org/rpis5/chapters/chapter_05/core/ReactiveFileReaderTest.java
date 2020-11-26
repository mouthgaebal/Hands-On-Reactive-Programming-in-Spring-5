package org.rpis5.chapters.chapter_05.core;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class ReactiveFileReaderTest {

    final ReactiveFileReader reader = new ReactiveFileReader();

    @Test
    public void readShakespeareWithBackpressureTest() {
        StepVerifier.create(reader.backpressuredShakespeare(), 1)
                    .assertNext(db -> assertThat(db.capacity()).isEqualTo(1024))
                    .expectNoEvent(Duration.ofMillis(2000))
                    .thenRequest(1)
                    .assertNext(db -> assertThat(db.capacity()).isEqualTo(1024))
                    .thenCancel()
                    .verify();
    }
    
    @Test
    public void read() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        
        reader.backpressuredShakespeare()
                .doOnTerminate(latch::countDown)
                .subscribe(dataBuffer -> log.info("data : {}", dataBuffer.toString(StandardCharsets.UTF_8)));
        
        latch.await();
    }
}