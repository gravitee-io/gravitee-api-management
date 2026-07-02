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

import static io.gravitee.repository.redis.ratelimit.RateLimitRepositoryConfiguration.SCRIPT_TOKEN_BUCKET_KEY;

import io.gravitee.repository.exception.RedisNotConnectedException;
import io.gravitee.repository.exception.RedisOperationTimeoutException;
import io.gravitee.repository.ratelimit.api.TokenBucketCalculator;
import io.gravitee.repository.ratelimit.api.TokenBucketConsumeResult;
import io.gravitee.repository.ratelimit.api.TokenBucketRateLimitRepository;
import io.gravitee.repository.ratelimit.model.TokenBucket;
import io.gravitee.repository.redis.vertx.RedisClient;
import io.reactivex.rxjava3.core.Single;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.redis.client.Response;
import io.vertx.rxjava3.SingleHelper;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Supplier;
import lombok.CustomLog;

/**
 * Redis-backed {@link TokenBucketRateLimitRepository}. Refill-and-consume is a single atomic Lua
 * script ({@code token-bucket.lua}) executed via {@code EVALSHA}, with the same {@code NOSCRIPT} ->
 * {@code EVAL} fallback as the rate-limit repository so it is correct on Redis Cluster.
 */
@CustomLog
public class RedisTokenBucketRateLimitRepository implements TokenBucketRateLimitRepository<TokenBucket> {

    private static final String REDIS_KEY_PREFIX = "tokenbucket:";

    private static final String NOSCRIPT_PREFIX = "NOSCRIPT";

    private final RedisClient redisClient;
    private final int operationTimeout;
    private final AtomicLong operationFailureCounter;

    public RedisTokenBucketRateLimitRepository(final RedisClient redisClient, int operationTimeout) {
        this.redisClient = redisClient;
        this.operationTimeout = operationTimeout;
        this.operationFailureCounter = new AtomicLong(0);
    }

    @Override
    public Single<TokenBucketConsumeResult> refillAndTryConsume(
        String key,
        long tokensRequested,
        long refillRate,
        long refillPeriodMillis,
        long capacity,
        long nowMillis,
        Supplier<TokenBucket> supplier
    ) {
        TokenBucketCalculator.requireValidArgs(tokensRequested, refillPeriodMillis, capacity);
        if (!redisClient.isConnected()) {
            return Single.error(new RedisNotConnectedException());
        }

        String subscription = supplier.get().getSubscription();
        long expireAt = System.currentTimeMillis() + TokenBucketCalculator.ttlMillis(refillRate, refillPeriodMillis, capacity);
        String redisKey = REDIS_KEY_PREFIX + key;

        return SingleHelper.toSingle(
            (Consumer<Handler<AsyncResult<Response>>>) asyncResultHandler ->
                redisClient
                    .redisApi()
                    .flatMap(redisAPI ->
                        redisAPI
                            .evalsha(
                                args(
                                    redisClient.scriptSha1(SCRIPT_TOKEN_BUCKET_KEY),
                                    redisKey,
                                    tokensRequested,
                                    refillRate,
                                    refillPeriodMillis,
                                    capacity,
                                    nowMillis,
                                    subscription,
                                    expireAt
                                )
                            )
                            .recover(t -> {
                                if (!isNoScript(t)) {
                                    return Future.failedFuture(t);
                                }
                                final String source = redisClient.scriptSource(SCRIPT_TOKEN_BUCKET_KEY);
                                if (source == null) {
                                    return Future.failedFuture(
                                        new IllegalStateException(
                                            "Cannot recover from NOSCRIPT: token-bucket script source unavailable (script was never loaded)"
                                        )
                                    );
                                }
                                // On Redis Cluster, SCRIPT LOAD only reaches the contacted node; an EVALSHA routed by
                                // hash slot to another master returns NOSCRIPT. NOSCRIPT means the script did not run,
                                // so replaying via EVAL (which caches it on that node) cannot double-consume.
                                log.debug(
                                    "EVALSHA returned NOSCRIPT; falling back to EVAL to load the token-bucket script on the target node"
                                );
                                return redisAPI
                                    .eval(
                                        args(
                                            source,
                                            redisKey,
                                            tokensRequested,
                                            refillRate,
                                            refillPeriodMillis,
                                            capacity,
                                            nowMillis,
                                            subscription,
                                            expireAt
                                        )
                                    )
                                    .recover(evalError -> {
                                        evalError.addSuppressed(t);
                                        return Future.failedFuture(evalError);
                                    });
                            })
                    )
                    .onFailure(this::logOperationFailure)
                    .onComplete(asyncResultHandler)
        )
            .map(response -> {
                boolean allowed = response.get(0).toLong() == 1L;
                long newTokens = response.get(1).toLong();
                return new TokenBucketConsumeResult(
                    allowed,
                    newTokens,
                    TokenBucketCalculator.nextAvailableAtMillis(newTokens, refillRate, refillPeriodMillis, nowMillis)
                );
            })
            .timeout(operationTimeout, TimeUnit.MILLISECONDS, Single.error(new RedisOperationTimeoutException(operationTimeout)));
    }

    // numkeys is "1": only the bucket key is a KEY, so all keys touched share one hash slot.
    // scriptRef is a SHA on the EVALSHA path and the script source on the EVAL fallback path.
    static List<String> args(
        String scriptRef,
        String key,
        long tokensRequested,
        long refillRate,
        long refillPeriodMillis,
        long capacity,
        long now,
        String subscription,
        long expireAt
    ) {
        return Arrays.asList(
            scriptRef,
            "1", // numkeys
            key,
            Long.toString(tokensRequested),
            Long.toString(refillRate),
            Long.toString(refillPeriodMillis),
            Long.toString(capacity),
            Long.toString(now),
            subscription == null ? "" : subscription,
            Long.toString(expireAt)
        );
    }

    // package-private for direct unit testing of the cause-chain matcher
    static boolean isNoScript(Throwable t) {
        int depth = 0;
        for (Throwable cause = t; cause != null && depth < 20; cause = cause.getCause(), depth++) {
            String message = cause.getMessage();
            if (message != null && message.stripLeading().regionMatches(true, 0, NOSCRIPT_PREFIX, 0, NOSCRIPT_PREFIX.length())) {
                return true;
            }
        }
        return false;
    }

    private void logOperationFailure(Throwable t) {
        long failureCount = operationFailureCounter.getAndIncrement();
        if (failureCount < 10) {
            log.warn("Failed to run token-bucket script on Redis {}", t.getMessage());
        } else if (failureCount % 10000 == 0) {
            log.warn("Failed to run token-bucket script on Redis {} ({} times)", t.getMessage(), failureCount);
        }
    }
}
