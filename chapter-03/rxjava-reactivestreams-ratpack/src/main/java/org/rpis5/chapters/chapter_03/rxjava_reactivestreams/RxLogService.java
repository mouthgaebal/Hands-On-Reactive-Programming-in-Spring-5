package org.rpis5.chapters.chapter_03.rxjava_reactivestreams;

import io.netty.buffer.ByteBuf;
import io.reactivex.netty.protocol.http.client.HttpClient;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import io.reactivex.netty.protocol.http.sse.ServerSentEvent;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Service;
import rx.Observable;
import rx.RxReactiveStreams;

import java.net.InetSocketAddress;

@Service
public class RxLogService implements LogService {

    final HttpClient<ByteBuf, ByteBuf> rxClient =
            HttpClient.newClient(new InetSocketAddress(8080));

    @Override
    public Publisher<String> stream() {
        Observable<String> rxStream = rxClient.createGet("/logs")
                                              .flatMap(HttpClientResponse::getContentAsServerSentEvents)
                                              .map(ServerSentEvent::contentAsString);

        return RxReactiveStreams.toPublisher(rxStream);
    }
}
