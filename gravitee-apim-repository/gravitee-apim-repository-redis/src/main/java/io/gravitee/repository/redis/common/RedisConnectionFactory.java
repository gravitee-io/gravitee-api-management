/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.repository.redis.common;

import io.gravitee.node.vertx.client.redis.VertxRedisClientFactory;
import io.gravitee.plugin.configurations.redis.HostAndPort;
import io.gravitee.plugin.configurations.redis.RedisClientOptions;
import io.gravitee.plugin.configurations.redis.RedisSentinelOptions;
import io.gravitee.plugin.configurations.ssl.KeyStore;
import io.gravitee.plugin.configurations.ssl.SslOptions;
import io.gravitee.plugin.configurations.ssl.TrustStore;
import io.gravitee.plugin.configurations.ssl.jks.JKSKeyStore;
import io.gravitee.plugin.configurations.ssl.jks.JKSTrustStore;
import io.gravitee.plugin.configurations.ssl.none.NoneKeyStore;
import io.gravitee.plugin.configurations.ssl.none.NoneTrustStore;
import io.gravitee.plugin.configurations.ssl.pem.PEMKeyStore;
import io.gravitee.plugin.configurations.ssl.pem.PEMTrustStore;
import io.gravitee.plugin.configurations.ssl.pkcs12.PKCS12KeyStore;
import io.gravitee.plugin.configurations.ssl.pkcs12.PKCS12TrustStore;
import io.gravitee.repository.redis.vertx.RedisClient;
import io.vertx.core.Vertx;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.CustomLog;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
public class RedisConnectionFactory {

    private final Environment environment;
    private final Vertx vertx;
    private final String propertyPrefix;
    private final Map<String, String> scripts;

    private static final String SENTINEL_PARAMETER_PREFIX = "sentinel.";
    private static final String PASSWORD_PARAMETER = "password";

    private static final String STORE_FORMAT_JKS = "JKS";
    private static final String STORE_FORMAT_PEM = "PEM";
    private static final String STORE_FORMAT_PKCS12 = "PKCS12";

    public RedisConnectionFactory(
        final Environment environment,
        final Vertx vertx,
        final String propertyPrefix,
        final Map<String, String> scripts
    ) {
        this.environment = environment;
        this.vertx = vertx;
        this.propertyPrefix = propertyPrefix + ".redis.";
        this.scripts = scripts;
    }

    public RedisClient createRedisClient() {
        RedisClientOptions options = buildRedisClientOptions();
        VertxRedisClientFactory factory = new VertxRedisClientFactory(vertx);
        return new RedisClient(vertx, factory, options, scripts);
    }

    protected RedisClientOptions buildRedisClientOptions() {
        RedisClientOptions.RedisClientOptionsBuilder builder = RedisClientOptions.builder();

        boolean ssl = readPropertyValue(propertyPrefix + "ssl", boolean.class, false);
        String username = readPropertyValue(propertyPrefix + "username", String.class, null);
        String password = readPropertyValue(propertyPrefix + PASSWORD_PARAMETER, String.class);

        builder.host(readPropertyValue(propertyPrefix + "host", String.class, "localhost"));
        builder.port(readPropertyValue(propertyPrefix + "port", int.class, 6379));
        builder.username(username);
        builder.password(password);
        builder.useSsl(ssl);

        if (isSentinelEnabled()) {
            log.debug("Redis repository configured to use Sentinel connection");

            String redisMaster = readPropertyValue(propertyPrefix + SENTINEL_PARAMETER_PREFIX + "master", String.class);
            if (!StringUtils.hasText(redisMaster)) {
                throw new IllegalStateException(
                    "Incorrect Sentinel configuration : parameter '" + propertyPrefix + SENTINEL_PARAMETER_PREFIX + "master' is mandatory!"
                );
            }

            String sentinelPassword = readPropertyValue(
                propertyPrefix + SENTINEL_PARAMETER_PREFIX + PASSWORD_PARAMETER,
                String.class,
                null
            );

            builder.sentinel(
                RedisSentinelOptions.builder().masterId(redisMaster).password(sentinelPassword).nodes(getSentinelNodes()).build()
            );
        } else {
            log.debug("Redis repository configured to use standalone connection");
        }

        if (ssl) {
            log.debug("Redis repository configured with ssl enabled");
            builder.ssl(buildSslOptions());
        }

        builder.connectTimeout(readPropertyValue(propertyPrefix + "tcp.connectTimeout", int.class, 5000));
        builder.idleTimeout(readPropertyValue(propertyPrefix + "tcp.idleTimeout", int.class, 0));

        return builder.build();
    }

    private SslOptions buildSslOptions() {
        SslOptions sslOptions = new SslOptions();
        boolean trustAll = readPropertyValue(propertyPrefix + "trustAll", boolean.class, true);
        sslOptions.setTrustAll(trustAll);

        String hostnameVerificationAlgorithm = readPropertyValue(propertyPrefix + "hostnameVerificationAlgorithm", String.class, "NONE");
        sslOptions.setHostnameVerificationAlgorithm(hostnameVerificationAlgorithm);
        sslOptions.setHostnameVerifier(!"NONE".equalsIgnoreCase(hostnameVerificationAlgorithm));

        String tlsProtocols = readPropertyValue(propertyPrefix + "tlsProtocols", String.class, "");
        if (StringUtils.hasText(tlsProtocols)) {
            sslOptions.setTlsProtocols(StringUtils.commaDelimitedListToSet(StringUtils.trimAllWhitespace(tlsProtocols)));
        }

        String tlsCiphers = readPropertyValue(propertyPrefix + "tlsCiphers", String.class, "");
        if (StringUtils.hasText(tlsCiphers)) {
            sslOptions.setTlsCiphers(new java.util.ArrayList<>(StringUtils.commaDelimitedListToSet(tlsCiphers)));
        }

        sslOptions.setAlpn(readPropertyValue(propertyPrefix + "alpn", boolean.class, false));
        sslOptions.setOpenSsl(readPropertyValue(propertyPrefix + "openssl", boolean.class, false));

        if (!trustAll) {
            sslOptions.setTrustStore(buildTrustStore());
        }

        sslOptions.setKeyStore(buildKeyStore());

        return sslOptions;
    }

    private TrustStore buildTrustStore() {
        String truststorePropertyPrefix = propertyPrefix + "truststore.";
        String truststorePath = readPropertyValue(truststorePropertyPrefix + "path", String.class);
        String truststoreType = readPropertyValue(truststorePropertyPrefix + "type", String.class);
        String truststorePassword = readPropertyValue(truststorePropertyPrefix + "password", String.class);
        String truststoreAlias = readPropertyValue(truststorePropertyPrefix + "alias", String.class);

        if (!StringUtils.hasText(truststoreType)) {
            return new NoneTrustStore();
        }

        return switch (truststoreType.toUpperCase()) {
            case STORE_FORMAT_PEM -> {
                if (!StringUtils.hasText(truststorePath)) {
                    throw new IllegalArgumentException("Missing PEM truststore value");
                }
                PEMTrustStore pemTrustStore = new PEMTrustStore();
                pemTrustStore.setPath(truststorePath);
                yield pemTrustStore;
            }
            case STORE_FORMAT_PKCS12 -> {
                if (!StringUtils.hasText(truststorePath)) {
                    throw new IllegalArgumentException("Missing PKCS12 truststore value");
                }
                PKCS12TrustStore pkcs12TrustStore = new PKCS12TrustStore();
                pkcs12TrustStore.setPath(truststorePath);
                pkcs12TrustStore.setAlias(truststoreAlias);
                pkcs12TrustStore.setPassword(truststorePassword);
                yield pkcs12TrustStore;
            }
            case STORE_FORMAT_JKS -> {
                if (!StringUtils.hasText(truststorePath)) {
                    throw new IllegalArgumentException("Missing JKS truststore value");
                }
                JKSTrustStore jksTrustStore = new JKSTrustStore();
                jksTrustStore.setPath(truststorePath);
                jksTrustStore.setAlias(truststoreAlias);
                jksTrustStore.setPassword(truststorePassword);
                yield jksTrustStore;
            }
            default -> {
                log.error("Unknown type of Truststore provided {}", truststoreType);
                yield new NoneTrustStore();
            }
        };
    }

    private KeyStore buildKeyStore() {
        String keystorePropertyPrefix = propertyPrefix + "keystore.";
        String keystorePath = readPropertyValue(keystorePropertyPrefix + "path", String.class);
        String keystorePassword = readPropertyValue(keystorePropertyPrefix + "password", String.class);
        String keyPassword = readPropertyValue(keystorePropertyPrefix + "keyPassword", String.class);
        String keystoreType = readPropertyValue(keystorePropertyPrefix + "type", String.class);
        String keystoreAlias = readPropertyValue(keystorePropertyPrefix + "alias", String.class);

        if (!StringUtils.hasText(keystoreType)) {
            return new NoneKeyStore();
        }

        return switch (keystoreType.toUpperCase()) {
            case STORE_FORMAT_PEM -> {
                List<String> certPaths = new ArrayList<>();
                List<String> keyPaths = new ArrayList<>();
                for (
                    int idx = 0;
                    StringUtils.hasText(readPropertyValue(keystorePropertyPrefix + "certificates[" + idx + "].cert", String.class));
                    idx++
                ) {
                    certPaths.add(readPropertyValue(keystorePropertyPrefix + "certificates[" + idx + "].cert", String.class));
                    keyPaths.add(readPropertyValue(keystorePropertyPrefix + "certificates[" + idx + "].key", String.class));
                }
                if (certPaths.isEmpty()) {
                    throw new IllegalArgumentException("Missing PEM keystore value");
                }
                PEMKeyStore pemKeyStore = new PEMKeyStore();
                if (certPaths.size() == 1) {
                    pemKeyStore.setCertPath(certPaths.get(0));
                    pemKeyStore.setKeyPath(keyPaths.get(0));
                } else {
                    pemKeyStore.setCertPaths(certPaths);
                    pemKeyStore.setKeyPaths(keyPaths);
                }
                yield pemKeyStore;
            }
            case STORE_FORMAT_PKCS12 -> {
                if (!StringUtils.hasText(keystorePath)) {
                    throw new IllegalArgumentException("Missing PKCS12 keystore value");
                }
                PKCS12KeyStore pkcs12KeyStore = new PKCS12KeyStore();
                pkcs12KeyStore.setPath(keystorePath);
                pkcs12KeyStore.setAlias(keystoreAlias);
                pkcs12KeyStore.setKeyPassword(keyPassword);
                pkcs12KeyStore.setPassword(keystorePassword);
                yield pkcs12KeyStore;
            }
            case STORE_FORMAT_JKS -> {
                if (!StringUtils.hasText(keystorePath)) {
                    throw new IllegalArgumentException("Missing JKS keystore value");
                }
                JKSKeyStore jksKeyStore = new JKSKeyStore();
                jksKeyStore.setPath(keystorePath);
                jksKeyStore.setAlias(keystoreAlias);
                jksKeyStore.setKeyPassword(keyPassword);
                jksKeyStore.setPassword(keystorePassword);
                yield jksKeyStore;
            }
            default -> {
                log.error("Unknown type of Keystore provided {}", keystoreType);
                yield new NoneKeyStore();
            }
        };
    }

    private <T> T readPropertyValue(String propertyName, Class<T> propertyType) {
        return readPropertyValue(propertyName, propertyType, null);
    }

    private <T> T readPropertyValue(String propertyName, Class<T> propertyType, T defaultValue) {
        T value = environment.getProperty(propertyName, propertyType, defaultValue);
        log.debug("Read property {}: {}", propertyName, value);
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
            nodes.add(HostAndPort.builder().host(host).port(port).build());
        }
        return nodes;
    }
}
