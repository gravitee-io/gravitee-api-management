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
package io.gravitee.repository.redis.management;

import io.gravitee.repository.config.TestRepositoryInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * @author David BRASSELY (david at graviteesource.com)
 * @author GraviteeSource Team
 */
public class RedisTestRepositoryInitializer implements TestRepositoryInitializer {

    private final Logger logger = LoggerFactory.getLogger(RedisTestRepositoryInitializer.class);

    @Autowired
    @Qualifier("managementRedisTemplate")
    protected RedisTemplate<String, Object> redisTemplate;

    public void setUp() {
    }

    public void tearDown() {
        logger.info("Flush all data from Redis");
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }
}
