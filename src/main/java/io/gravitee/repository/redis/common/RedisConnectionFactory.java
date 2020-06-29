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
package io.gravitee.repository.redis.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import redis.clients.jedis.JedisPoolConfig;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class RedisConnectionFactory implements FactoryBean<org.springframework.data.redis.connection.RedisConnectionFactory> {

    private final Logger logger = LoggerFactory.getLogger(RedisConnectionFactory.class);

    @Autowired
    private Environment environment;

    private final String propertyPrefix;

    public RedisConnectionFactory(String propertyPrefix) {
        this.propertyPrefix = propertyPrefix + ".redis.";
    }

    @Override
    public org.springframework.data.redis.connection.RedisConnectionFactory getObject() throws Exception {
        JedisConnectionFactory jedisConnectionFactory = new JedisConnectionFactory();
        jedisConnectionFactory.setHostName(readPropertyValue(propertyPrefix + "host", String.class, "localhost"));
        jedisConnectionFactory.setPort(readPropertyValue(propertyPrefix + "port", int.class, 6379));
        jedisConnectionFactory.setPassword(readPropertyValue(propertyPrefix + "password", String.class, null));
        jedisConnectionFactory.setTimeout(readPropertyValue(propertyPrefix + "timeout", int.class, -1));
        jedisConnectionFactory.setUseSsl(readPropertyValue(propertyPrefix + "ssl", boolean.class, false));

        int poolMax = readPropertyValue(propertyPrefix + "pool.max", int.class, 256);

        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(poolMax);
        poolConfig.setBlockWhenExhausted(false);
        jedisConnectionFactory.setPoolConfig(poolConfig);

        jedisConnectionFactory.afterPropertiesSet();

        return jedisConnectionFactory;
    }

    @Override
    public Class<?> getObjectType() {
        return org.springframework.data.redis.connection.RedisConnectionFactory.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    private String readPropertyValue(String propertyName) {
        return readPropertyValue(propertyName, String.class, null);
    }

    private <T> T readPropertyValue(String propertyName, Class<T> propertyType) {
        return readPropertyValue(propertyName, propertyType, null);
    }

    private <T> T readPropertyValue(String propertyName, Class<T> propertyType, T defaultValue) {
        T value = environment.getProperty(propertyName, propertyType, defaultValue);
        logger.debug("Read property {}: {}", propertyName, value);
        return value;
    }
}
