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
package io.gravitee.repository.redis.vertx;

import static org.assertj.core.api.Assertions.assertThat;

import io.vertx.core.Vertx;
import io.vertx.core.impl.ContextInternal;
import io.vertx.core.impl.VertxInternal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/**
 * The Redis connection is opened per event loop: every context running on the same event loop
 * (including per-request duplicated contexts) must resolve to the same connection key, while
 * contexts on different event loops must not share one.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class RedisClientLoopKeyTest {

    private final Vertx vertx = Vertx.vertx();

    @AfterAll
    void tear_down() {
        vertx.close();
    }

    @Test
    void should_use_a_distinct_key_per_event_loop() throws Exception {
        ContextInternal context = eventLoopContext();
        ContextInternal otherLoopContext = eventLoopContext();
        while (otherLoopContext.nettyEventLoop() == context.nettyEventLoop()) {
            otherLoopContext = eventLoopContext();
        }

        assertThat(keyOn(context)).isNotEqualTo(keyOn(otherLoopContext));
    }

    @Test
    void should_share_the_key_between_contexts_of_the_same_event_loop() throws Exception {
        ContextInternal context = eventLoopContext();
        ContextInternal sameLoopContext = eventLoopContext();
        while (sameLoopContext.nettyEventLoop() != context.nettyEventLoop()) {
            sameLoopContext = eventLoopContext();
        }

        assertThat(keyOn(context)).isEqualTo(keyOn(sameLoopContext));
    }

    @Test
    void should_share_the_key_with_duplicated_contexts() throws Exception {
        ContextInternal context = eventLoopContext();

        assertThat(keyOn(context.duplicate())).isEqualTo(keyOn(context));
    }

    private ContextInternal eventLoopContext() {
        return ((VertxInternal) vertx).createEventLoopContext();
    }

    private int keyOn(final ContextInternal context) throws Exception {
        CompletableFuture<Integer> key = new CompletableFuture<>();
        context.runOnContext(v -> key.complete(RedisClient.currentLoopKey()));
        return key.get(10, TimeUnit.SECONDS);
    }
}
