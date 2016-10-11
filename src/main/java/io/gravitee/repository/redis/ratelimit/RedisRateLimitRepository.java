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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class RedisRateLimitRepository implements RateLimitRepository {

    @Autowired
    @Qualifier("rateLimitRedisTemplate")
    private StringRedisTemplate redisTemplate;

    @Autowired
    @Qualifier("rateLimitAsyncScript")
    private RedisScript<List> rateLimitAsyncScript;

    private final static String REDIS_KEY_PREFIX = "ratelimit:";
    private final static String REDIS_ASYNC_SUFFIX = ":async";

    @Override
    public RateLimit get(String rateLimitKey) {
        RateLimit rateLimit = new RateLimit(rateLimitKey);

        List<String> members = redisTemplate.opsForList().range(REDIS_KEY_PREFIX + rateLimit.getKey(), 0, 5);

        if (! members.isEmpty()) {
            Iterator<String> ite = members.iterator();

            rateLimit.setAsync(Boolean.parseBoolean(ite.next()));
            rateLimit.setUpdatedAt(Long.parseLong(ite.next()));
            rateLimit.setCreatedAt(Long.parseLong(ite.next()));
            rateLimit.setCounter(Long.parseLong(ite.next()));
            rateLimit.setResetTime(Long.parseLong(ite.next()));
            rateLimit.setLastRequest(Long.parseLong(ite.next()));
        }

        return rateLimit;
    }

    @Override
    public void save(RateLimit rateLimit) {
        redisTemplate.executePipelined((RedisConnection redisConnection) -> {
            ((StringRedisConnection) redisConnection).lTrim(REDIS_KEY_PREFIX + rateLimit.getKey(), 1, 0);
            ((StringRedisConnection) redisConnection).lPush(REDIS_KEY_PREFIX + rateLimit.getKey(),
                    Long.toString(rateLimit.getLastRequest()),
                    Long.toString(rateLimit.getResetTime()),
                    Long.toString(rateLimit.getCounter()),
                    Long.toString(rateLimit.getCreatedAt()),
                    Long.toString(rateLimit.getUpdatedAt()),
                    Boolean.toString(rateLimit.isAsync()));
            ((StringRedisConnection) redisConnection).pExpireAt(REDIS_KEY_PREFIX + rateLimit.getKey(),
                    rateLimit.getResetTime());

            if (rateLimit.isAsync()) {
                ((StringRedisConnection) redisConnection).lTrim(
                        REDIS_KEY_PREFIX + rateLimit.getKey() + REDIS_ASYNC_SUFFIX, 1, 0);
                ((StringRedisConnection) redisConnection).lPush(
                        REDIS_KEY_PREFIX + rateLimit.getKey() + REDIS_ASYNC_SUFFIX,
                        REDIS_KEY_PREFIX + rateLimit.getKey(),
                        Long.toString(rateLimit.getUpdatedAt()));
                ((StringRedisConnection) redisConnection).pExpireAt(REDIS_KEY_PREFIX + rateLimit.getKey() + REDIS_ASYNC_SUFFIX,
                        rateLimit.getResetTime());
            }
            return null;
        });
    }

    @Override
    public Iterator<RateLimit> findAsyncAfter(long timestamp) {
        final List<List<String>> rateLimits = redisTemplate.execute(
                rateLimitAsyncScript, Collections.singletonList("after"), Long.toString(timestamp));
        final Iterator<List<String>> iterator = rateLimits.iterator();

        return new Iterator<RateLimit>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public RateLimit next() {
                List<String> lstRateLimit = iterator.next();
                Iterator<String> innerIterator = lstRateLimit.iterator();
                RateLimit rateLimit = new RateLimit(innerIterator.next());
                rateLimit.setAsync(Boolean.parseBoolean(innerIterator.next()));
                rateLimit.setUpdatedAt(Long.parseLong(innerIterator.next()));
                rateLimit.setCreatedAt(Long.parseLong(innerIterator.next()));
                rateLimit.setCounter(Long.parseLong(innerIterator.next()));
                rateLimit.setResetTime(Long.parseLong(innerIterator.next()));
                rateLimit.setLastRequest(Long.parseLong(innerIterator.next()));

                return rateLimit;
            }
        };
    }
}
