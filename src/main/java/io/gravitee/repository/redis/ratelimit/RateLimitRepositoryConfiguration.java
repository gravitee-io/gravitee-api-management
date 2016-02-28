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

import io.gravitee.repository.Scope;
import io.gravitee.repository.redis.common.AbstractRepositoryConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

import java.util.List;
import java.util.Set;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 * @author GraviteeSource Team
 */
@Configuration
@ComponentScan
public class RateLimitRepositoryConfiguration extends AbstractRepositoryConfiguration {

    @Bean(name = "rateLimitRedisTemplate")
    public StringRedisTemplate redisTemplate(org.springframework.data.redis.connection.RedisConnectionFactory redisConnectionFactory) {
        StringRedisTemplate redisTemplate = new StringRedisTemplate();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        return redisTemplate;
    }

    @Bean(name = "rateLimitAsyncScript")
    public RedisScript<List> script() {
        DefaultRedisScript<List> redisScript = new DefaultRedisScript<List>();
        redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("scripts/ratelimit-async.lua")));
        redisScript.setResultType(List.class);
        return  redisScript;
    }

    @Override
    protected Scope getScope() {
        return Scope.MANAGEMENT;
    }
}
