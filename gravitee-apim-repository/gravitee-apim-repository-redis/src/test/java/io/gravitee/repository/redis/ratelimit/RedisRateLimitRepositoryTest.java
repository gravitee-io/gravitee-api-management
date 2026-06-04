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
package io.gravitee.repository.redis.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.repository.ratelimit.model.RateLimit;
import io.gravitee.repository.redis.vertx.RedisClient;
import io.vertx.core.Future;
import io.vertx.redis.client.RedisAPI;
import io.vertx.redis.client.Response;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class RedisRateLimitRepositoryTest {

    @Test
    void script_is_invoked_with_a_single_redis_key_for_cluster_slot_safety() {
        var rate = new RateLimit("api-1");
        rate.setCounter(0);
        rate.setLimit(100);
        rate.setResetTime(123L);
        rate.setSubscription("sub-1");

        List<String> command = RedisRateLimitRepository.convertToList("sha1", "ratelimit:api-1", 5, rate);

        // EVALSHA <sha> <numkeys> <key...> <arg...>
        // Exactly one Redis key must be declared so all touched keys share a hash slot
        // on Redis Cluster (otherwise CROSSSLOT). The weight travels as an ARGV value.
        assertThat(command.get(1)).isEqualTo("1");
        assertThat(command.get(2)).isEqualTo("ratelimit:api-1");
        assertThat(command.get(3)).isEqualTo("5"); // weight, now an ARGV, not a KEY
    }

    @Test
    void falls_back_to_eval_when_cluster_node_returns_noscript() {
        RedisClient redisClient = mock(RedisClient.class);
        RedisAPI redisAPI = mock(RedisAPI.class);

        when(redisClient.isConnected()).thenReturn(true);
        when(redisClient.scriptSha1("ratelimit")).thenReturn("the-sha");
        when(redisClient.scriptSource("ratelimit")).thenReturn("the-script-source");
        when(redisClient.redisApi()).thenReturn(Future.succeededFuture(redisAPI));
        // EVALSHA fails because the slot's master never received the SCRIPT LOAD.
        when(redisAPI.evalsha(anyList())).thenReturn(Future.failedFuture(new RuntimeException("NOSCRIPT No matching script")));
        // EVAL (with the source) succeeds — Redis caches it on that node and returns the rate.
        Response evalResponse = rateResponse(7L, 5L, 123L, "sub-1");
        when(redisAPI.eval(anyList())).thenReturn(Future.succeededFuture(evalResponse));

        var repository = new RedisRateLimitRepository(redisClient, 2000);
        RateLimit result = repository.incrementAndGet("my-key", 1, () -> new RateLimit("my-key")).blockingGet();

        // The result must be mapped from the fallback EVAL response, not the supplier default.
        assertThat(result.getCounter()).isEqualTo(7L);
        assertThat(result.getLimit()).isEqualTo(5L);
        assertThat(result.getResetTime()).isEqualTo(123L);
        assertThat(result.getSubscription()).isEqualTo("sub-1");
        // The fallback EVAL must carry the script SOURCE, not the SHA.
        verify(redisAPI).eval(argThatStartsWith("the-script-source"));
    }

    @Test
    void propagates_non_noscript_errors_without_falling_back_to_eval() {
        RedisClient redisClient = mock(RedisClient.class);
        RedisAPI redisAPI = mock(RedisAPI.class);

        when(redisClient.isConnected()).thenReturn(true);
        when(redisClient.scriptSha1("ratelimit")).thenReturn("the-sha");
        when(redisClient.redisApi()).thenReturn(Future.succeededFuture(redisAPI));
        // A non-NOSCRIPT error (e.g. LOADING) must NOT trigger an EVAL replay — the script may have run.
        when(redisAPI.evalsha(anyList())).thenReturn(Future.failedFuture(new RuntimeException("LOADING Redis is loading the dataset")));

        var repository = new RedisRateLimitRepository(redisClient, 2000);

        assertThatThrownBy(() -> repository.incrementAndGet("my-key", 1, () -> new RateLimit("my-key")).blockingGet()).hasMessageContaining(
            "LOADING"
        );
        verify(redisAPI, never()).eval(anyList());
    }

    private static Response rateResponse(long counter, long limit, long reset, String subscription) {
        // Build the field mocks first — calling a stubbing method inside when(...) breaks Mockito.
        Response counterField = longResponse(counter);
        Response limitField = longResponse(limit);
        Response resetField = longResponse(reset);
        Response subscriptionField = mock(Response.class);
        when(subscriptionField.toString()).thenReturn(subscription);

        Response r = mock(Response.class);
        when(r.size()).thenReturn(4);
        when(r.get(0)).thenReturn(counterField);
        when(r.get(1)).thenReturn(limitField);
        when(r.get(2)).thenReturn(resetField);
        when(r.get(3)).thenReturn(subscriptionField);
        return r;
    }

    private static Response longResponse(long value) {
        Response r = mock(Response.class);
        when(r.toLong()).thenReturn(value);
        return r;
    }

    private static List<String> argThatStartsWith(String first) {
        return org.mockito.ArgumentMatchers.argThat(list -> list != null && !list.isEmpty() && first.equals(list.get(0)));
    }
}
