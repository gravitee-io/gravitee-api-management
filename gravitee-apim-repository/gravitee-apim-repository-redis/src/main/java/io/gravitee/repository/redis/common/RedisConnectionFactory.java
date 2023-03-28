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

import io.gravitee.repository.redis.vertx.RedisClient;
import io.vertx.core.*;
import io.vertx.redis.client.RedisClientType;
import io.vertx.redis.client.RedisOptions;
import io.vertx.redis.client.RedisRole;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class RedisConnectionFactory implements FactoryBean<RedisClient> {

    private final Logger logger = LoggerFactory.getLogger(RedisConnectionFactory.class);

    private Environment environment;

    private final String propertyPrefix;

    private final Vertx vertx;

    private static final String SENTINEL_PARAMETER_PREFIX = "sentinel.";

    private static final String PASSWORD_PARAMETER = "password";

    public RedisConnectionFactory(Environment environment, Vertx vertx, String propertyPrefix) {
        this.environment = environment;
        this.vertx = vertx;
        this.propertyPrefix = propertyPrefix + ".redis.";
    }

    @Override
    public RedisClient getObject() {
        return new RedisClient(vertx, buildRedisOptions());
    }

    protected RedisOptions buildRedisOptions() {
        final RedisOptions options = new RedisOptions();

        boolean ssl = readPropertyValue(propertyPrefix + "ssl", boolean.class, false);

        if (isSentinelEnabled()) {
            // Sentinels + Redis master / replicas
            logger.debug("Redis repository configured to use Sentinel connection");

            options.setType(RedisClientType.SENTINEL);
            List<HostAndPort> sentinelNodes = getSentinelNodes();

            // Redis Password
            String password = readPropertyValue(propertyPrefix + PASSWORD_PARAMETER, String.class);
            sentinelNodes.forEach(hostAndPort ->
                options.addConnectionString(hostAndPort.withPassword(password).withSsl(ssl).toConnectionString())
            );

            String redisMaster = readPropertyValue(propertyPrefix + SENTINEL_PARAMETER_PREFIX + "master", String.class);
            if (!StringUtils.hasText(redisMaster)) {
                throw new IllegalStateException(
                    "Incorrect Sentinel configuration : parameter '" + propertyPrefix + SENTINEL_PARAMETER_PREFIX + "master' is mandatory!"
                );
            }
            options.setMasterName(redisMaster).setRole(RedisRole.MASTER);

            // Sentinel Password
            String sentinelPassword = readPropertyValue(
                propertyPrefix + SENTINEL_PARAMETER_PREFIX + PASSWORD_PARAMETER,
                String.class,
                null
            );
            options.setPassword(sentinelPassword);
        } else {
            // Standalone Redis
            logger.debug("Redis repository configured to use standalone connection");

            options.setType(RedisClientType.STANDALONE);

            HostAndPort hostAndPort = HostAndPort
                .of(
                    readPropertyValue(propertyPrefix + "host", String.class, "localhost"),
                    readPropertyValue(propertyPrefix + "port", int.class, 6379)
                )
                .withPassword(readPropertyValue(propertyPrefix + PASSWORD_PARAMETER, String.class))
                .withSsl(ssl);

            options.setConnectionString(hostAndPort.toConnectionString());
        }

        // SSL
        if (ssl) {
            options.getNetClientOptions().setSsl(true).setTrustAll(true);
        }

        // Set max waiting handlers high enough to manage high throughput since we are not using the pooled mode
        options.setMaxWaitingHandlers(1024);

        return options;
    }

    @Override
    public Class<?> getObjectType() {
        return RedisClient.class;
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
        return StringUtils.hasLength(readPropertyValue(propertyPrefix + SENTINEL_PARAMETER_PREFIX + "nodes[0].host", String.class));
    }

    private List<HostAndPort> getSentinelNodes() {
        final List<HostAndPort> nodes = new ArrayList<>();
        for (
            int idx = 0;
            StringUtils.hasText(readPropertyValue(propertyPrefix + SENTINEL_PARAMETER_PREFIX + "nodes[" + idx + "].host", String.class));
            idx++
        ) {
            String host = readPropertyValue(propertyPrefix + SENTINEL_PARAMETER_PREFIX + "nodes[" + idx + "].host", String.class);
            int port = readPropertyValue(propertyPrefix + SENTINEL_PARAMETER_PREFIX + "nodes[" + idx + "].port", int.class);
            nodes.add(HostAndPort.of(host, port));
        }
        return nodes;
    }

    private static class HostAndPort {

        private final String host;
        private final int port;
        private String password;
        private boolean useSsl;

        private HostAndPort(String host, int port) {
            this.host = host;
            this.port = port;
        }

        static HostAndPort of(String host, int port) {
            return new HostAndPort(host, port);
        }

        public HostAndPort withPassword(String password) {
            this.password = password;

            return this;
        }

        public HostAndPort withSsl(boolean useSsl) {
            this.useSsl = useSsl;

            return this;
        }

        public String toConnectionString() {
            String connectionType = "redis";

            if (useSsl) {
                connectionType = "rediss";
            }

            if (StringUtils.hasText(password)) {
                return connectionType + "://:" + password + '@' + host + ':' + port;
            }

            return connectionType + "://" + host + ':' + port;
        }
    }
}
