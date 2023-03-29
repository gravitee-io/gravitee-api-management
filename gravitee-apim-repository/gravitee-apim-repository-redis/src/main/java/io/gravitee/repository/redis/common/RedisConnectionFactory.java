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
import io.vertx.core.net.JksOptions;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.OpenSSLEngineOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.core.net.PfxOptions;
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

    private static final String STORE_FORMAT_JKS = "JKS";
    private static final String STORE_FORMAT_PEM = "PEM";
    private static final String STORE_FORMAT_PKCS12 = "PKCS12";

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
            logger.debug("Redis repository configured with ssl enabled");

            boolean trustAll = readPropertyValue(propertyPrefix + "trustAll", boolean.class, true);
            options.getNetClientOptions().setSsl(true);
            options.getNetClientOptions().setTrustAll(trustAll);

            // TLS Protocols
            options
                .getNetClientOptions()
                .setEnabledSecureTransportProtocols(
                    StringUtils.commaDelimitedListToSet(
                        StringUtils.trimAllWhitespace(readPropertyValue(propertyPrefix + "tlsProtocols", String.class, ""))
                    )
                );

            // TLS Ciphers
            StringUtils
                .commaDelimitedListToSet(readPropertyValue(propertyPrefix + "tlsCiphers", String.class, ""))
                .forEach(cipherSuite -> options.getNetClientOptions().addEnabledCipherSuite(cipherSuite.strip()));

            options.getNetClientOptions().setUseAlpn(readPropertyValue(propertyPrefix + "alpn", boolean.class, false));

            if (readPropertyValue(propertyPrefix + "openssl", boolean.class, false)) {
                options.getNetClientOptions().setSslEngineOptions(new OpenSSLEngineOptions());
            }

            if (!trustAll) {
                // Client truststore configuration (trust server certificate).
                configureTrustStore(options.getNetClientOptions());
            } else {
                logger.warn("Redis repository configured with ssl and trustAll which is not a good practice for security");
            }

            // Client keystore configuration (client certificate for mtls).
            configureKeyStore(options.getNetClientOptions());
        }

        // Set max waiting handlers high enough to manage high throughput since we are not using the pooled mode
        options.setMaxWaitingHandlers(1024);

        return options;
    }

    private void configureTrustStore(NetClientOptions netClientOptions) {
        String truststorePropertyPrefix = propertyPrefix + "truststore.";
        String truststorePath = readPropertyValue(truststorePropertyPrefix + "path", String.class);
        String truststoreType = readPropertyValue(truststorePropertyPrefix + "type", String.class);
        String truststorePassword = readPropertyValue(truststorePropertyPrefix + "password", String.class);
        String truststoreAlias = readPropertyValue(truststorePropertyPrefix + "alias", String.class);

        if (StringUtils.hasText(truststoreType)) {
            switch (truststoreType.toUpperCase()) {
                case STORE_FORMAT_PEM:
                    final PemTrustOptions pemTrustOptions = new PemTrustOptions();

                    if (StringUtils.hasText(truststorePath)) {
                        pemTrustOptions.addCertPath(truststorePath);
                    } else {
                        throw new IllegalArgumentException("Missing PEM truststore value");
                    }

                    netClientOptions.setPemTrustOptions(pemTrustOptions);
                    break;
                case STORE_FORMAT_PKCS12:
                    final PfxOptions pfxOptions = new PfxOptions();

                    if (StringUtils.hasText(truststorePath)) {
                        pfxOptions.setPath(truststorePath);
                    } else {
                        throw new IllegalArgumentException("Missing PKCS12 truststore value");
                    }

                    pfxOptions.setAlias(truststoreAlias);
                    pfxOptions.setPassword(truststorePassword);
                    netClientOptions.setPfxTrustOptions(pfxOptions);
                    break;
                case STORE_FORMAT_JKS:
                    final JksOptions jksOptions = new JksOptions();

                    if (StringUtils.hasText(truststorePath)) {
                        jksOptions.setPath(truststorePath);
                    } else {
                        throw new IllegalArgumentException("Missing JKS truststore value");
                    }

                    jksOptions.setAlias(truststoreAlias);
                    jksOptions.setPassword(truststorePassword);
                    netClientOptions.setTrustStoreOptions(jksOptions);
                    break;
                default:
                    logger.error("Unknown type of Truststore provided {}", truststoreType);
                    break;
            }
        }
    }

    private void configureKeyStore(NetClientOptions netClientOptions) {
        String keystorePropertyPrefix = propertyPrefix + "keystore.";
        String keystorePath = readPropertyValue(keystorePropertyPrefix + "path", String.class);
        String keystorePassword = readPropertyValue(keystorePropertyPrefix + "password", String.class);
        String keyPassword = readPropertyValue(keystorePropertyPrefix + "keyPassword", String.class);
        String keystoreType = readPropertyValue(keystorePropertyPrefix + "type", String.class);
        String keystoreAlias = readPropertyValue(keystorePropertyPrefix + "alias", String.class);

        if (StringUtils.hasText(keystoreType)) {
            switch (keystoreType.toUpperCase()) {
                case STORE_FORMAT_PEM:
                    final PemKeyCertOptions pemKeyCertOptions = new PemKeyCertOptions();

                    for (
                        int idx = 0;
                        StringUtils.hasText(readPropertyValue(keystorePropertyPrefix + "certificates[" + idx + "].cert", String.class));
                        idx++
                    ) {
                        pemKeyCertOptions.addCertPath(
                            readPropertyValue(keystorePropertyPrefix + "certificates[" + idx + "].cert", String.class)
                        );
                        pemKeyCertOptions.addKeyPath(
                            readPropertyValue(keystorePropertyPrefix + "certificates[" + idx + "].key", String.class)
                        );
                    }

                    if (pemKeyCertOptions.getCertPaths().isEmpty()) {
                        throw new IllegalArgumentException("Missing PEM keystore value");
                    }

                    netClientOptions.setPemKeyCertOptions(pemKeyCertOptions);
                    break;
                case STORE_FORMAT_PKCS12:
                    final PfxOptions pfxOptions = new PfxOptions();

                    if (StringUtils.hasText(keystorePath)) {
                        pfxOptions.setPath(keystorePath);
                    } else {
                        throw new IllegalArgumentException("Missing PKCS12 keystore value");
                    }

                    pfxOptions.setAlias(keystoreAlias);
                    pfxOptions.setAliasPassword(keyPassword);
                    pfxOptions.setPassword(keystorePassword);
                    netClientOptions.setPfxKeyCertOptions(pfxOptions);
                    break;
                case STORE_FORMAT_JKS:
                    final JksOptions jksOptions = new JksOptions();

                    if (StringUtils.hasText(keystorePath)) {
                        jksOptions.setPath(keystorePath);
                    } else {
                        throw new IllegalArgumentException("Missing JKS keystore value");
                    }

                    jksOptions.setAlias(keystoreAlias);
                    jksOptions.setAliasPassword(keyPassword);
                    jksOptions.setPassword(keystorePassword);
                    netClientOptions.setKeyStoreOptions(jksOptions);
                    break;
                default:
                    logger.error("Unknown type of Keystore provided {}", keystoreType);
                    break;
            }
        }
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
