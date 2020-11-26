package org.rpis5.chapters.chapter_05.core;

import io.reactivex.Flowable;
import io.reactivex.Maybe;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.ReactiveTypeDescriptor;
import reactor.core.publisher.Mono;

class ReactiveAdapterRegistryTest {
    
    @BeforeAll
    static void beforeAll() {
        ReactiveAdapterRegistry
                .getSharedInstance()
                .registerReactiveType(
                        ReactiveTypeDescriptor.singleOptionalValue(Maybe.class, Maybe::empty),
                        rawMaybe -> ((Maybe<?>)rawMaybe).toFlowable(),
                        publisher -> Flowable.fromPublisher(publisher).singleElement()
                );
    }
    
    @Test
    void convertFromMaybeToPublisherTest() {
        ReactiveAdapter maybeAdapter = ReactiveAdapterRegistry.getSharedInstance()
                .getAdapter(Maybe.class);
        Assertions.assertThat(maybeAdapter.toPublisher(Maybe.just(1)))
                .isInstanceOf(Publisher.class);
    }
    
    @Test
    void convertFromPublisherToMaybeTest() {
        ReactiveAdapter maybeAdapter = ReactiveAdapterRegistry.getSharedInstance()
                .getAdapter(Maybe.class);
        Assertions.assertThat(maybeAdapter.fromPublisher(Mono.just(1)))
                .isInstanceOf(Maybe.class);
    }
}
