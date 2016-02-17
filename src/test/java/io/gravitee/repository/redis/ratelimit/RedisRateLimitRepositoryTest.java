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
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 * @author GraviteeSource Team
 */
public class RedisRateLimitRepositoryTest extends AbstractRedisTest {

    @Autowired
    private RateLimitRepository rateLimitRepository;
    
    @Test
    public void test() {
        RateLimit rateLimit = new RateLimit("mykey");
        rateLimit.setResetTime(new Date().getTime() + 120000);

        RateLimit rateLimit2 = new RateLimit("otherkey");
        rateLimit2.setResetTime(new Date().getTime() + 120000);

        System.out.println(rateLimitRepository.get(rateLimit));

        rateLimitRepository.save(rateLimit);

        System.out.println(rateLimitRepository.get(rateLimit));

        System.out.println(rateLimitRepository.get(rateLimit2));
    }
}
