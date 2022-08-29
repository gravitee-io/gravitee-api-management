/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.jupiter.http.vertx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import io.gravitee.common.http.IdGenerator;
import io.reactivex.Flowable;
import io.vertx.core.http.HttpMethod;
import io.vertx.reactivex.core.http.HttpHeaders;
import io.vertx.reactivex.core.http.HttpServerRequest;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class VertxMessageServerRequestTest {

    @Mock
    private HttpServerRequest httpServerRequest;

    @Mock
    private IdGenerator idGenerator;

    private AtomicInteger subscriptionCount;
    private VertxMessageServerRequest cut;

    @BeforeEach
    void init() {
        subscriptionCount = new AtomicInteger(0);
        Flowable<io.vertx.reactivex.core.buffer.Buffer> chunks = Flowable
            .just(
                io.vertx.reactivex.core.buffer.Buffer.buffer("chunk1"),
                io.vertx.reactivex.core.buffer.Buffer.buffer("chunk2"),
                io.vertx.reactivex.core.buffer.Buffer.buffer("chunk3")
            )
            .doOnSubscribe(subscription -> subscriptionCount.incrementAndGet());

        when(httpServerRequest.method()).thenReturn(HttpMethod.POST);
        when(httpServerRequest.headers()).thenReturn(HttpHeaders.headers());
        when(httpServerRequest.toFlowable()).thenReturn(chunks);
        cut = new VertxMessageServerRequest(httpServerRequest, idGenerator);
    }

    @Test
    void shouldSubscribeOnceWhenIgnoringAndReplacingExistingChunks() {
        long initRequestLength = cut.metrics().getRequestContentLength();
        cut.body().test().assertValue(buffer -> buffer.toString().equals("chunk1chunk2chunk3"));

        assertEquals(initRequestLength + 18, cut.metrics.getRequestContentLength());
        assertEquals(1, subscriptionCount.get());
    }
}
