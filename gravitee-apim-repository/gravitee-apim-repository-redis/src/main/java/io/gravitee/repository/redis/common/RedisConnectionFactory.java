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

import io.lettuce.core.internal.HostAndPort;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration.LettucePoolingClientConfigurationBuilder;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class RedisConnectionFactory implements FactoryBean<org.springframework.data.redis.connection.RedisConnectionFactory> {

    private final Logger logger = LoggerFactory.getLogger(RedisConnectionFactory.class);

    @Autowired
    private Environment environment;

    private final String propertyPrefix;

    private static final String SENTINEL_PARAMETER_PREFIX = "sentinel.";
    private static final int DEFAULT_COMMAND_TIMEOUT = 1_000;

    public RedisConnectionFactory(String propertyPrefix) {
        this.propertyPrefix = propertyPrefix + ".redis.";
    }

    @Override
    public org.springframework.data.redis.connection.RedisConnectionFactory getObject() throws Exception {
        final LettuceConnectionFactory lettuceConnectionFactory;

        if (isSentinelEnabled()) {
            // Sentinels + Redis master / replicas
            logger.debug("Redis repository configured to use Sentinel connection");
            List<HostAndPort> sentinelNodes = getSentinelNodes();
            String redisMaster = readPropertyValue(propertyPrefix + SENTINEL_PARAMETER_PREFIX + "master", String.class);
            if (StringUtils.isBlank(redisMaster)) {
                throw new IllegalStateException(
                    "Incorrect Sentinel configuration : parameter '" + SENTINEL_PARAMETER_PREFIX + "master' is mandatory !"
                );
            }

            RedisSentinelConfiguration sentinelConfiguration = new RedisSentinelConfiguration();
            sentinelConfiguration.master(redisMaster);
            // Parsing and registering nodes
            sentinelNodes.forEach(hostAndPort -> sentinelConfiguration.sentinel(hostAndPort.getHostText(), hostAndPort.getPort()));
            // Sentinel Password
            sentinelConfiguration.setSentinelPassword(
                readPropertyValue(propertyPrefix + SENTINEL_PARAMETER_PREFIX + "password", String.class)
            );
            // Redis Password
            sentinelConfiguration.setPassword(readPropertyValue(propertyPrefix + "password", String.class));

            lettuceConnectionFactory = new LettuceConnectionFactory(sentinelConfiguration, buildLettuceClientConfiguration());
        } else {
            // Standalone Redis
            logger.debug("Redis repository configured to use standalone connection");
            RedisStandaloneConfiguration standaloneConfiguration = new RedisStandaloneConfiguration();
            standaloneConfiguration.setHostName(readPropertyValue(propertyPrefix + "host", String.class, "localhost"));
            standaloneConfiguration.setPort(readPropertyValue(propertyPrefix + "port", int.class, 6379));
            standaloneConfiguration.setPassword(readPropertyValue(propertyPrefix + "password", String.class));

            lettuceConnectionFactory = new LettuceConnectionFactory(standaloneConfiguration, buildLettuceClientConfiguration());
        }
        lettuceConnectionFactory.afterPropertiesSet();

        return lettuceConnectionFactory;
    }

    @Override
    public Class<?> getObjectType() {
        return org.springframework.data.redis.connection.RedisConnectionFactory.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    private <T> T readPropertyValue(String propertyName, Class<T> propertyType) {
        return readPropertyValue(propertyName, propertyType, null);
    }

    private <T> T readPropertyValue(String propertyName, Class<T> propertyType, T defaultValue) {
        T value = environment.getProperty(propertyName, propertyType, defaultValue);
        logger.debug("Read property {}: {}", propertyName, value);
        return value;
    }

    private boolean isSentinelEnabled() {
        return StringUtils.isNotBlank(readPropertyValue(propertyPrefix + SENTINEL_PARAMETER_PREFIX + "nodes[0].host", String.class));
    }

    private List<HostAndPort> getSentinelNodes() {
        final List<HostAndPort> nodes = new ArrayList<>();
        for (
            int idx = 0;
            StringUtils.isNotBlank(readPropertyValue(propertyPrefix + SENTINEL_PARAMETER_PREFIX + "nodes[" + idx + "].host", String.class));
            idx++
        ) {
            String host = readPropertyValue(propertyPrefix + SENTINEL_PARAMETER_PREFIX + "nodes[" + idx + "].host", String.class);
            int port = readPropertyValue(propertyPrefix + SENTINEL_PARAMETER_PREFIX + "nodes[" + idx + "].port", int.class);
            nodes.add(HostAndPort.of(host, port));
        }
        return nodes;
    }

    private LettucePoolingClientConfiguration buildLettuceClientConfiguration() {
        final LettucePoolingClientConfigurationBuilder builder = LettucePoolingClientConfiguration.builder();
        int timeout = readPropertyValue(propertyPrefix + "timeout", int.class, DEFAULT_COMMAND_TIMEOUT);
        // For backward compatibility (negative timeout is no longer accepted)
        if (timeout < 0) {
            timeout = DEFAULT_COMMAND_TIMEOUT;
        }
        builder.commandTimeout(Duration.ofMillis(timeout));
        if (readPropertyValue(propertyPrefix + "ssl", boolean.class, false)) {
            builder.useSsl();
        }
        int poolMax = readPropertyValue(propertyPrefix + "pool.max", int.class, 256);

        GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
        poolConfig.setMaxTotal(poolMax);
        poolConfig.setBlockWhenExhausted(false);

        builder.poolConfig(poolConfig);
        return builder.build();
    }
}
