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

import io.gravitee.repository.ratelimit.api.RateLimitRepository;
import io.gravitee.repository.ratelimit.model.RateLimit;
import io.gravitee.repository.redis.vertx.RedisAPI;
import io.reactivex.Single;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Function;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.reactivex.impl.AsyncResultMaybe;
import io.vertx.redis.client.Response;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class RedisRateLimitRepository implements RateLimitRepository<RateLimit> {

    private static final String KEY_PREFIX = "rl:";

    private final RedisAPI redisAPI;

    public RedisRateLimitRepository(final RedisAPI redisAPI) {
        this.redisAPI = redisAPI;
    }

    @Override
    public Single<RateLimit> incrementAndGet(String key, long weight, Supplier<RateLimit> supplier) {
        final RateLimit newRate = supplier.get();

        return AsyncResultMaybe
            .toMaybe(
                (Consumer<Handler<AsyncResult<Response>>>) asyncResultHandler ->
                    redisAPI
                        .getNative()
                        .evalsha(
                            convertToList(redisAPI.getScriptSha1(), KEY_PREFIX + key, Long.toString(weight), newRate),
                            asyncResultHandler
                        )
            )
            .map(
                new Function<Response, RateLimit>() {
                    @Override
                    public RateLimit apply(@NonNull Response response) throws Exception {
                        // It may happen when the rate has been expired while running the script
                        if (response.size() > 0 && response.get(0) != null) {
                            RateLimit rateLimit = new RateLimit(key);
                            rateLimit.setCounter(response.get(0).toLong());
                            rateLimit.setLimit(newRate.getLimit());
                            rateLimit.setResetTime(response.get(1).toLong());
                            rateLimit.setSubscription(newRate.getSubscription());

                            return rateLimit;
                        }

                        return newRate;
                    }
                }
            )
            .onErrorReturn(throwable -> newRate)
            .toSingle();
    }

    private List<String> convertToList(String scriptSha1, String key, String weight, RateLimit rate) {
        return Arrays.asList(
            scriptSha1,
            "1", // Number of keys
            key,
            weight,
            Long.toString(rate.getResetTime())
        );
    }
}
