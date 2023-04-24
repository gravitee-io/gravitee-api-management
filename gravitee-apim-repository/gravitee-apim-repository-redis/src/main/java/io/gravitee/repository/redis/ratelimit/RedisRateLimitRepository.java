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
package io.gravitee.repository.redis.ratelimit;

import static io.gravitee.repository.redis.ratelimit.RateLimitRepositoryConfiguration.SCRIPT_RATELIMIT_KEY;

import io.gravitee.repository.ratelimit.api.RateLimitRepository;
import io.gravitee.repository.ratelimit.model.RateLimit;
import io.gravitee.repository.redis.vertx.RedisClient;
import io.reactivex.rxjava3.core.Single;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.redis.client.Response;
import io.vertx.rxjava3.SingleHelper;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class RedisRateLimitRepository implements RateLimitRepository<RateLimit> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisRateLimitRepository.class);
    private static final String REDIS_KEY_PREFIX = "ratelimit:";

    private final RedisClient redisClient;

    public RedisRateLimitRepository(final RedisClient redisClient) {
        this.redisClient = redisClient;
    }

    @Override
    public Single<RateLimit> incrementAndGet(String key, long weight, Supplier<RateLimit> supplier) {
        final RateLimit newRate = supplier.get();

        return SingleHelper
            .toSingle(
                (Consumer<Handler<AsyncResult<Response>>>) asyncResultHandler ->
                    redisClient
                        .redisApi()
                        .flatMap(redisAPI ->
                            redisAPI.evalsha(
                                convertToList(this.redisClient.scriptSha1(SCRIPT_RATELIMIT_KEY), REDIS_KEY_PREFIX + key, weight, newRate)
                            )
                        )
                        .onFailure(t -> LOGGER.error("Failed to run rate-limit script on Redis {}", t.getMessage()))
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
            .onErrorReturn(throwable -> newRate);
    }

    private List<String> convertToList(String scriptSha1, String key, long weight, RateLimit rate) {
        return Arrays.asList(
            scriptSha1,
            "2", // Number of keys
            key,
            Long.toString(weight),
            Long.toString(rate.getCounter()),
            Long.toString(rate.getLimit()),
            Long.toString(rate.getResetTime()),
            rate.getSubscription()
        );
    }
}
