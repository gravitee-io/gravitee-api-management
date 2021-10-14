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

import io.gravitee.repository.redis.vertx.RedisAPI;
import io.vertx.core.*;
import io.vertx.redis.client.RedisClientType;
import io.vertx.redis.client.RedisOptions;
import io.vertx.redis.client.RedisRole;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class RedisConnectionFactory implements FactoryBean<RedisAPI> {

    private final Logger logger = LoggerFactory.getLogger(RedisConnectionFactory.class);

    @Autowired
    private Environment environment;

    private final String propertyPrefix;

    private final Vertx vertx;

    private static final String SENTINEL_PARAMETER_PREFIX = "sentinel.";

    public RedisConnectionFactory(Vertx vertx, String propertyPrefix) {
        this.vertx = vertx;
        this.propertyPrefix = propertyPrefix + ".redis.";
    }

    @Override
    public RedisAPI getObject() {
        final RedisOptions options = new RedisOptions();

        if (isSentinelEnabled()) {
            options.setType(RedisClientType.SENTINEL).setRole(RedisRole.MASTER);

            // Sentinels + Redis master / replicas
            logger.debug("Redis repository configured to use Sentinel connection");

            String master = readPropertyValue(propertyPrefix + SENTINEL_PARAMETER_PREFIX + "master", String.class);
            if (master == null || master.trim().isEmpty()) {
                throw new IllegalStateException(
                    "Incorrect Sentinel configuration : parameter '" + SENTINEL_PARAMETER_PREFIX + "master' is mandatory !"
                );
            }
            options.setMasterName(master);

            List<HostAndPort> sentinelNodes = getSentinelNodes();

            // Redis Password
            String password = readPropertyValue(propertyPrefix + "password", String.class);

            sentinelNodes.forEach(
                new Consumer<HostAndPort>() {
                    @Override
                    public void accept(HostAndPort hostAndPort) {
                        options.addConnectionString(hostAndPort.withPassword(password).toConnectionString());
                    }
                }
            );
        } else {
            // Standalone Redis
            logger.debug("Redis repository configured to use standalone connection");

            options.setType(RedisClientType.STANDALONE);

            HostAndPort hostAndPort = HostAndPort
                .of(
                    readPropertyValue(propertyPrefix + "host", String.class, "localhost"),
                    readPropertyValue(propertyPrefix + "port", int.class, 6379)
                )
                .withPassword(readPropertyValue(propertyPrefix + "password", String.class));

            options.setConnectionString(hostAndPort.toConnectionString());
        }

        // SSL
        boolean ssl = readPropertyValue(propertyPrefix + "ssl", boolean.class, false);
        if (ssl) {
            options.getNetClientOptions().setSsl(true).setTrustAll(true);
        }

        // Connection Pool
        options.setMaxPoolSize(readPropertyValue(propertyPrefix + "pool.max", int.class, 6));
        options.setMaxWaitingHandlers(32);
        options.setPoolCleanerInterval(10000);

        return new RedisAPI(vertx, options);
    }

    @Override
    public Class<?> getObjectType() {
        return RedisAPI.class;
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
        String value = readPropertyValue(propertyPrefix + SENTINEL_PARAMETER_PREFIX + "nodes[0].host", String.class);
        return value != null && !value.trim().isEmpty();
    }

    private List<HostAndPort> getSentinelNodes() {
        final List<HostAndPort> nodes = new ArrayList<>();
        boolean found = true;
        int idx = 0;

        while (found) {
            String host = readPropertyValue(propertyPrefix + SENTINEL_PARAMETER_PREFIX + "nodes[" + idx + "].host", String.class);
            if (host == null || host.trim().isEmpty()) {
                found = false;
            } else {
                int port = readPropertyValue(propertyPrefix + SENTINEL_PARAMETER_PREFIX + "nodes[" + idx + "].port", int.class);
                nodes.add(HostAndPort.of(host, port));
            }
            idx++;
        }

        return nodes;
    }

    private static class HostAndPort {

        private final String host;
        private final int port;
        private String password;

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

        public String toConnectionString() {
            if (password != null && !password.trim().isEmpty()) {
                return "redis://:" + password + '@' + host + ':' + port;
            }

            return "redis://" + host + ':' + port;
        }
    }
}
