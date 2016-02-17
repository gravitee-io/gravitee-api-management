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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.repository.Scope;
import io.gravitee.repository.redis.common.AbstractRepositoryConfiguration;
import io.gravitee.repository.redis.management.transaction.NoTransactionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 * @author GraviteeSource Team
 */
@Configuration
@ComponentScan
public class ManagementRepositoryConfiguration extends AbstractRepositoryConfiguration {

    @Bean(name = "managementRedisTemplate")
    public RedisTemplate redisTemplate(org.springframework.data.redis.connection.RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate redisTemplate = new RedisTemplate();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.setValueSerializer(new Jackson2JsonRedisSerializer<>(Object.class));
        redisTemplate.setDefaultSerializer(new Jackson2JsonRedisSerializer<>(Object.class));
        return redisTemplate;
    }

    @Override
    protected Scope getScope() {
        return Scope.MANAGEMENT;
    }

    @Bean
    public ObjectMapper mapper() {
        return new ObjectMapper();
    }

    @Bean
    public AbstractPlatformTransactionManager graviteeTransactionManager() {
        return new NoTransactionManager();
    }
}
