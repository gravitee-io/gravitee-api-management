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
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.Iterator;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 * @author GraviteeSource Team
 */
public class RedisRateLimitRepositoryOld extends AbstractRedisOld {

    @Autowired
    private RateLimitRepository rateLimitRepository;
    
    @Test
    public void saveRateLimit() {
        RateLimit rateLimit = new RateLimit("api-id:app-id:1234:0");
        rateLimit.setCreatedAt(System.currentTimeMillis());
        rateLimit.setUpdatedAt(rateLimit.getCreatedAt());
        rateLimit.setResetTime(rateLimit.getCreatedAt() + 240000);

        RateLimit rateLimit2 = new RateLimit("api2-id:app-id:1:0");
        rateLimit2.setCreatedAt(System.currentTimeMillis());
        rateLimit2.setUpdatedAt(rateLimit.getCreatedAt());
        rateLimit2.setResetTime(new Date().getTime() + 240000);

        rateLimitRepository.save(rateLimit);
        rateLimitRepository.save(rateLimit2);
    }

    @Test
    public void saveRateLimitAsync() {
        RateLimit rateLimit = new RateLimit("api-id:app-id:1234:0");
        rateLimit.setCreatedAt(System.currentTimeMillis());
        rateLimit.setUpdatedAt(rateLimit.getCreatedAt());
        rateLimit.setResetTime(rateLimit.getCreatedAt() + 240000);
        rateLimit.setAsync(true);

        rateLimitRepository.save(rateLimit);
    }

    @Test
    public void getRateLimit() {
        RateLimit rateLimit = rateLimitRepository.get("api-id:app-id:1234:0");
        Assert.assertNotNull(rateLimit);
    }

    @Test
    @Ignore
    public void findUpdatedRateLimit() {
        RateLimit rateLimit = new RateLimit("api-id:app-id:1234:0");
        rateLimit.setCreatedAt(System.currentTimeMillis());
        rateLimit.setUpdatedAt(rateLimit.getCreatedAt());
        rateLimit.setResetTime(rateLimit.getCreatedAt() + 240000);
        rateLimit.setAsync(true);

        rateLimitRepository.save(rateLimit);

        Iterator<RateLimit> ite = rateLimitRepository.findAsyncAfter(System.currentTimeMillis() - 120000L);
        Assert.assertTrue(ite.hasNext());

        RateLimit rl = ite.next();
        Assert.assertEquals(rateLimit.getKey(), rl.getKey());
    }
}
