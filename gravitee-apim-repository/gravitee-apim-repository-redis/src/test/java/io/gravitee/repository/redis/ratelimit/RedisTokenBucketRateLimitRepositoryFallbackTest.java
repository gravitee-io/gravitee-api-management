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

import io.gravitee.repository.ratelimit.api.TokenBucketConsumeResult;
import io.gravitee.repository.ratelimit.model.TokenBucket;
import io.gravitee.repository.redis.vertx.RedisClient;
import io.vertx.core.Future;
import io.vertx.redis.client.RedisAPI;
import io.vertx.redis.client.Response;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * Mock-based coverage of the token-bucket Redis repository's {@code NOSCRIPT -> EVAL} Redis-Cluster
 * fallback and the {@code isNoScript} cause-chain matcher — neither is exercised by the container test
 * (which only hits the EVALSHA happy path).
 *
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class RedisTokenBucketRateLimitRepositoryFallbackTest {

    @Test
    void falls_back_to_eval_when_cluster_node_returns_noscript() {
        RedisClient redisClient = mock(RedisClient.class);
        RedisAPI redisAPI = mock(RedisAPI.class);

        when(redisClient.isConnected()).thenReturn(true);
        when(redisClient.scriptSha1("token-bucket")).thenReturn("the-sha");
        when(redisClient.scriptSource("token-bucket")).thenReturn("the-script-source");
        when(redisClient.redisApi()).thenReturn(Future.succeededFuture(redisAPI));
        // EVALSHA fails: the slot's master never received the SCRIPT LOAD (NOSCRIPT = script did not run).
        when(redisAPI.evalsha(anyList())).thenReturn(Future.failedFuture(new RuntimeException("NOSCRIPT No matching script")));
        // EVAL with the source succeeds; the Lua returns { allowed = 1, tokens = 5 }.
        // Build the response mock first — calling a stubbing method inside when(...) breaks Mockito.
        Response evalResponse = consumeResponse(1L, 5L);
        when(redisAPI.eval(anyList())).thenReturn(Future.succeededFuture(evalResponse));

        var repository = new RedisTokenBucketRateLimitRepository(redisClient, 2000);
        TokenBucketConsumeResult result = repository
            .refillAndTryConsume("my-key", 1, 10, 1_000L, 20, 1_000L, () -> new TokenBucket("my-key"))
            .blockingGet();

        // Result is mapped from the fallback EVAL response, not the supplier default.
        assertThat(result.allowed()).isTrue();
        assertThat(result.remainingTokens()).isEqualTo(5L);
        // The fallback EVAL must carry the script SOURCE (first ARGV), not the SHA.
        verify(redisAPI).eval(argThatStartsWith("the-script-source"));
    }

    @Test
    void propagates_non_noscript_errors_without_falling_back_to_eval() {
        RedisClient redisClient = mock(RedisClient.class);
        RedisAPI redisAPI = mock(RedisAPI.class);

        when(redisClient.isConnected()).thenReturn(true);
        when(redisClient.scriptSha1("token-bucket")).thenReturn("the-sha");
        when(redisClient.redisApi()).thenReturn(Future.succeededFuture(redisAPI));
        // A non-NOSCRIPT error (e.g. LOADING) must NOT trigger an EVAL replay — the script may have run.
        when(redisAPI.evalsha(anyList())).thenReturn(Future.failedFuture(new RuntimeException("LOADING Redis is loading the dataset")));

        var repository = new RedisTokenBucketRateLimitRepository(redisClient, 2000);

        assertThatThrownBy(() ->
            repository.refillAndTryConsume("my-key", 1, 10, 1_000L, 20, 1_000L, () -> new TokenBucket("my-key")).blockingGet()
        ).hasMessageContaining("LOADING");
        verify(redisAPI, never()).eval(anyList());
    }

    @Test
    void isNoScript_matches_through_the_cause_chain_only() {
        assertThat(RedisTokenBucketRateLimitRepository.isNoScript(new RuntimeException("NOSCRIPT No matching script"))).isTrue();
        assertThat(
            RedisTokenBucketRateLimitRepository.isNoScript(new RuntimeException("wrapper", new IllegalStateException("NOSCRIPT x")))
        ).isTrue();
        assertThat(RedisTokenBucketRateLimitRepository.isNoScript(new RuntimeException("LOADING dataset"))).isFalse();
        assertThat(RedisTokenBucketRateLimitRepository.isNoScript(new RuntimeException((String) null))).isFalse();
    }

    private static Response consumeResponse(long allowed, long tokens) {
        Response allowedField = longResponse(allowed);
        Response tokensField = longResponse(tokens);
        Response r = mock(Response.class);
        when(r.get(0)).thenReturn(allowedField);
        when(r.get(1)).thenReturn(tokensField);
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
