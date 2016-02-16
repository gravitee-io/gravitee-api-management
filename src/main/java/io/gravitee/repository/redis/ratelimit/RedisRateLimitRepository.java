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
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 * @author GraviteeSource Team
 */
public class RedisRateLimitRepository implements RateLimitRepository<String> {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private final static String KEY_PREFIX = "ratelimit:";
    private final static String FIELD_LAST_REQUEST = "last_request";
    private final static String FIELD_COUNTER = "counter";
    private final static String FIELD_RESET_TIME = "reset_time";

    @Override
    public RateLimit get(final RateLimit rateLimit) {
        boolean rateLimitExists = redisTemplate.execute((RedisConnection redisConnection) -> {
            return ((StringRedisConnection)redisConnection).exists(KEY_PREFIX + rateLimit.getKey());
        });

        if (rateLimitExists) {
            redisTemplate.execute((RedisConnection redisConnection) -> {
                List<String> fields = ((StringRedisConnection) redisConnection)
                        .hMGet(KEY_PREFIX + rateLimit.getKey(), FIELD_LAST_REQUEST, FIELD_RESET_TIME, FIELD_COUNTER);

                if (fields != null) {
                    Iterator<String> ite = fields.iterator();
                    rateLimit.setLastRequest(Long.parseLong(ite.next()));
                    rateLimit.setResetTime(Long.parseLong(ite.next()));
                    rateLimit.setCounter(Long.parseLong(ite.next()));
                }

                return rateLimit;
            });
        }

        return rateLimit;
    }

    @Override
    public void save(RateLimit rateLimit) {
        redisTemplate.executePipelined((RedisConnection redisConnection) -> {
            Map<String, String> fields = new HashMap<>(3);
            fields.put(FIELD_LAST_REQUEST, Long.toString(rateLimit.getLastRequest()));
            fields.put(FIELD_RESET_TIME, Long.toString(rateLimit.getResetTime()));
            fields.put(FIELD_COUNTER, Long.toString(rateLimit.getCounter()));
            ((StringRedisConnection) redisConnection).hMSet(KEY_PREFIX + rateLimit.getKey(), fields);
            ((StringRedisConnection) redisConnection).expireAt(KEY_PREFIX + rateLimit.getKey(), rateLimit.getResetTime());
            return null;
        });
    }
}
