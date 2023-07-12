/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.jupiter.reactor.v4.subscription.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gateway.api.buffer.Buffer;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
class SubscriptionResponseTest {

    public static final String NEW_CHUNK = "NEW_CHUNK";
    private SubscriptionResponse cut;

    @BeforeEach
    void setUp() {
        cut = new SubscriptionResponse();
    }

    @Test
    void checkSubscriptionResponse() {
        assertThat(cut.status()).isEqualTo(HttpStatusCode.OK_200);
        assertThat(cut.reason()).isNull();
        assertThat(cut.trailers()).isNull();
        assertThat(cut.ended()).isFalse();
        assertThat(cut.messages()).isNotNull();
        assertThat(cut.chunks()).isNotNull();
    }

    @Test
    void shouldSubscribeOnceWhenIgnoringAndReplacingExistingChunks() {
        cut.chunks(cut.chunks().ignoreElements().andThen(Flowable.just(Buffer.buffer(NEW_CHUNK))));
        cut.end().test().assertComplete();
        cut.body().test().assertComplete().assertValue(b -> NEW_CHUNK.equals(b.toString()));
    }

    @Test
    void shouldEndProperlyWithoutHavingSetChunks() {
        cut.end().test().assertComplete();
        cut.body().test().assertComplete().assertNoValues();
    }

    @Test
    void shouldSubscribeOnceWhenReplacingExistingChunksWithBody() {
        cut
            .chunks()
            .ignoreElements()
            .andThen(Completable.fromRunnable(() -> cut.body(Buffer.buffer(NEW_CHUNK))))
            .andThen(Completable.defer(() -> cut.end()))
            .test()
            .assertComplete();

        cut.body().test().assertComplete().assertValue(b -> NEW_CHUNK.equals(b.toString()));
        cut.chunks().test().assertComplete().assertValue(b -> NEW_CHUNK.equals(b.toString()));
    }
}
