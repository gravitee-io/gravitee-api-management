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

import static io.gravitee.repository.redis.ratelimit.RateLimitRepositoryConfiguration.SCRIPT_RATELIMIT_KEY;

import io.gravitee.repository.exception.RedisNotConnectedException;
import io.gravitee.repository.exception.RedisOperationTimeoutException;
import io.gravitee.repository.ratelimit.api.RateLimitRepository;
import io.gravitee.repository.ratelimit.model.RateLimit;
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
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
public class RedisRateLimitRepository implements RateLimitRepository<RateLimit> {

    private static final String REDIS_KEY_PREFIX = "ratelimit:";

    private final RedisClient redisClient;
    private final int operationTimeout;
    private final AtomicLong operationFailureCounter;

    public RedisRateLimitRepository(final RedisClient redisClient, int operationTimeout) {
        this.redisClient = redisClient;
        this.operationTimeout = operationTimeout;
        this.operationFailureCounter = new AtomicLong(0);
    }

    @Override
    public Single<RateLimit> incrementAndGet(String key, long weight, Supplier<RateLimit> supplier) {
        if (!redisClient.isConnected()) {
            // Fail fast in case the connection to Redis is not available.
            return Single.error(new RedisNotConnectedException());
        }

        final RateLimit newRate = supplier.get();

        return SingleHelper.toSingle(
            (Consumer<Handler<AsyncResult<Response>>>) asyncResultHandler ->
                redisClient
                    .redisApi()
                    .flatMap(redisAPI ->
                        redisAPI
                            .evalsha(
                                convertToList(this.redisClient.scriptSha1(SCRIPT_RATELIMIT_KEY), REDIS_KEY_PREFIX + key, weight, newRate)
                            )
                            .recover(t -> {
                                if (!isNoScript(t)) {
                                    return Future.failedFuture(t);
                                }
                                final String source = this.redisClient.scriptSource(SCRIPT_RATELIMIT_KEY);
                                if (source == null) {
                                    return Future.failedFuture(
                                        new IllegalStateException(
                                            "Cannot recover from NOSCRIPT: rate-limit script source unavailable (script was never loaded)"
                                        )
                                    );
                                }
                                // On Redis Cluster, SCRIPT LOAD only reaches the contacted node, so an
                                // EVALSHA routed by hash slot to another master returns NOSCRIPT. Fall back
                                // to EVAL with the script source, which caches it on that node for next time.
                                // NOSCRIPT means the script did not execute, so replaying via EVAL cannot double-count.
                                log.debug(
                                    "EVALSHA returned NOSCRIPT; falling back to EVAL to load the rate-limit script on the target node"
                                );
                                return redisAPI
                                    .eval(convertToList(source, REDIS_KEY_PREFIX + key, weight, newRate))
                                    .recover(evalError -> {
                                        // Preserve the original NOSCRIPT cause for diagnostics under a fallback storm.
                                        evalError.addSuppressed(t);
                                        return Future.failedFuture(evalError);
                                    });
                            })
                    )
                    .onFailure(this::logOperationFailure)
                    .onComplete(asyncResultHandler)
        )
            .map(response -> {
                // It may happen when the rate has been expired while running the script
                // expired values return a list of 'null'
                if (response.size() > 0 && response.get(0) != null) {
                    RateLimit rateLimit = new RateLimit(key);
                    rateLimit.setCounter(response.get(0).toLong());
                    rateLimit.setLimit(response.get(1).toLong());
                    rateLimit.setResetTime(response.get(2).toLong());
                    rateLimit.setSubscription(response.get(3).toString());

                    return rateLimit;
                }

                return newRate;
            })
            .timeout(operationTimeout, TimeUnit.MILLISECONDS, Single.error(new RedisOperationTimeoutException(operationTimeout)));
    }

    private static final String NOSCRIPT_PREFIX = "NOSCRIPT";

    /**
     * Returns true only when the error (or one of its causes) is a Redis {@code NOSCRIPT} reply.
     * Matches the error-code prefix (Redis error replies start with the uppercase code), not a
     * substring, so unrelated messages can't trigger a (non-idempotent) EVAL replay; walks the
     * cause chain and is case-insensitive to survive wrapping.
     */
    private static boolean isNoScript(Throwable t) {
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
            log.warn("Failed to run rate-limit script on Redis {}", t.getMessage());
        } else if (failureCount % 10000 == 0) {
            log.warn("Failed to run rate-limit script on Redis {} ({} times)", t.getMessage(), failureCount);
        }
    }

    // numkeys is "1": only the rate-limit key is a KEY, so all keys touched by the command share
    // one Redis Cluster hash slot (avoids CROSSSLOT). The weight is passed as an ARGV value, not a key.
    // scriptRef is a SHA on the EVALSHA path and the script source on the EVAL fallback path.
    static List<String> convertToList(String scriptRef, String key, long weight, RateLimit rate) {
        return Arrays.asList(
            scriptRef,
            "1", // numkeys
            key,
            Long.toString(weight),
            Long.toString(rate.getCounter()),
            Long.toString(rate.getLimit()),
            Long.toString(rate.getResetTime()),
            rate.getSubscription()
        );
    }
}
