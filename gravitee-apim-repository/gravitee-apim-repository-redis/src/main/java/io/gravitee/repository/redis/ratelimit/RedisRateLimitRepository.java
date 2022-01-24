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
import io.reactivex.Single;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class RedisRateLimitRepository implements RateLimitRepository<RateLimit> {

    @Autowired
    @Qualifier("rateLimitRedisTemplate")
    private StringRedisTemplate redisTemplate;

    @Autowired
    @Qualifier("rateLimitIncrScript")
    private RedisScript<List> rateLimitIncrScript;

    private static final String KEY_PREFIX = "ratelimit:";

    @Override
    public Single<RateLimit> incrementAndGet(String key, long weight, Supplier<RateLimit> supplier) {
        RateLimit newRate = supplier.get();

        //TODO: for now, we have to call the supplier for each call, we must find a better way to handle this case
        final List values = redisTemplate.execute(
            rateLimitIncrScript,
            Arrays.asList(KEY_PREFIX + key, Long.toString(weight)),
            convertToValuesArray(newRate)
        );

        // It may happen when the rate has been expired while running the script
        // expired values return a list of 'null'
        if (!values.isEmpty() && !values.stream().filter(Objects::nonNull).findFirst().isEmpty()) {
            RateLimit rateLimit = new RateLimit(key);
            rateLimit.setCounter(Long.parseLong((String) values.get(0)));
            rateLimit.setLimit(Long.parseLong((String) values.get(1)));
            rateLimit.setResetTime(Long.parseLong((String) values.get(2)));
            rateLimit.setSubscription((String) values.get(3));

            return Single.just(rateLimit);
        }

        return Single.just(newRate);
    }

    private Object[] convertToValuesArray(RateLimit rate) {
        return new Object[] {
            Long.toString(rate.getCounter()),
            Long.toString(rate.getLimit()),
            Long.toString(rate.getResetTime()),
            rate.getSubscription(),
        };
    }
}
