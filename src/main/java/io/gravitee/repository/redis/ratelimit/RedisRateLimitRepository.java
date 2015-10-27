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
import io.gravitee.repository.ratelimit.model.RateLimitResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class RedisRateLimitRepository implements RateLimitRepository<String> {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private final static String KEY_PREFIX = "ratelimit:";
    private final static String FIELD_LAST_REQUEST = "last_request";
    private final static String FIELD_COUNTER = "counter";
    private final static String FIELD_RESET_TIME = "reset_time";

    @Override
    public RateLimitResult acquire(String key, int pound, long limit, long periodTime, TimeUnit periodTimeUnit) {
        boolean rateLimitExists = redisTemplate.execute((RedisConnection redisConnection) -> {
            return ((StringRedisConnection)redisConnection).exists(KEY_PREFIX + key);
        });

        RateLimit rateLimit;
        if (rateLimitExists) {
            rateLimit = redisTemplate.execute((RedisConnection redisConnection) -> {
                return convert(key, ((StringRedisConnection) redisConnection)
                        .hMGet(KEY_PREFIX + key, FIELD_LAST_REQUEST, FIELD_RESET_TIME, FIELD_COUNTER));
            });
        } else {
            rateLimit = new RateLimit();
        }

        final RateLimit rateLimiting = rateLimit;
        RateLimitResult rateLimitResult = new RateLimitResult();

        // We prefer currentTimeMillis in place of nanoTime() because nanoTime is relatively
        // expensive call and depends on the underlying architecture.

        long now = System.currentTimeMillis();
        long endOfWindow = rateLimit.getEndOfWindow(periodTime, periodTimeUnit);

        if (now >= endOfWindow) {
            rateLimit.setCounter(0);
        }

        if (rateLimit.getCounter() >= limit) {
            rateLimitResult.setExceeded(true);
        } else {
            // Update rate limiter
            rateLimitResult.setExceeded(false);
            rateLimit.setCounter(rateLimit.getCounter() + pound);
            rateLimit.setLastRequest(now);
        }

        // Set the time at which the current rate limit window resets in UTC epoch seconds.
        long resetTimeMillis = rateLimit.getEndOfPeriod(now, periodTime, periodTimeUnit);
        rateLimitResult.setResetTime(resetTimeMillis / 1000L);
        rateLimit.setResetTime(resetTimeMillis);
        rateLimitResult.setRemains(limit - rateLimit.getCounter());

        redisTemplate.executePipelined((RedisConnection redisConnection) -> {
            Map<String, String> fields = new HashMap<>(3);
            fields.put(FIELD_LAST_REQUEST, Long.toString(rateLimiting.getLastRequest()));
            fields.put(FIELD_RESET_TIME, Long.toString(rateLimiting.getResetTime()));
            fields.put(FIELD_COUNTER, Long.toString(rateLimiting.getCounter()));
            ((StringRedisConnection) redisConnection).hMSet(KEY_PREFIX + key, fields);
            ((StringRedisConnection) redisConnection).expireAt(KEY_PREFIX + key, resetTimeMillis);
            return null;
        });

        return rateLimitResult;
    }

    private static RateLimit convert(String key, List<String> fields) {
        RateLimit rateLimit = new RateLimit();

        if (fields != null) {
            Iterator<String> ite = fields.iterator();
            rateLimit.setLastRequest(Long.parseLong(ite.next()));
            rateLimit.setResetTime(Long.parseLong(ite.next()));
            rateLimit.setCounter(Long.parseLong(ite.next()));

            rateLimit.setKey(key);
        }

        return rateLimit;
    }
}
