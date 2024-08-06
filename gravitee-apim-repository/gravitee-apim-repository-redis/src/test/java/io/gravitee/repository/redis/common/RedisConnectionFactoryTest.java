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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import io.vertx.core.net.JksOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.core.net.PfxOptions;
import io.vertx.redis.client.RedisOptions;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.env.MockEnvironment;

/**
 * @author Remi Baptiste (remi.baptiste at graviteesource.com)
 * @author GraviteeSource Team
 */
public class RedisConnectionFactoryTest {

    private static final String PROPERTY_PREFIX = "ratelimit";

    private RedisConnectionFactory redisConnectionFactory;
    private MockEnvironment environment;

    @BeforeEach
    void setUp() {
        environment = new MockEnvironment();
        redisConnectionFactory = new RedisConnectionFactory(environment, null, PROPERTY_PREFIX, Map.of());
    }

    @Test
    void shouldReturnRedisOptionsWithSecuredEndpoint() {
        environment.setProperty(PROPERTY_PREFIX + ".redis.host", "redis");
        environment.setProperty(PROPERTY_PREFIX + ".redis.port", "6379");
        environment.setProperty(PROPERTY_PREFIX + ".redis.ssl", "true");

        RedisOptions options = redisConnectionFactory.buildRedisOptions();

        assertThat(options).isNotNull();
        assertThat(options.getEndpoint()).isEqualTo("rediss://redis:6379");
    }

    @Test
    void shouldReturnRedisOptionsWithoutSecuredEndpoint() {
        environment.setProperty(PROPERTY_PREFIX + ".redis.host", "redis");
        environment.setProperty(PROPERTY_PREFIX + ".redis.port", "6379");

        RedisOptions options = redisConnectionFactory.buildRedisOptions();

        assertThat(options).isNotNull();
        assertThat(options.getEndpoint()).isEqualTo("redis://redis:6379");
    }

    @Test
    public void shouldReturnRedisOptionsWithSentinelsEndpoints() {
        environment.setProperty(PROPERTY_PREFIX + ".redis.sentinel.master", "redis-master");
        environment.setProperty(PROPERTY_PREFIX + ".redis.sentinel.nodes[0].host", "sent1");
        environment.setProperty(PROPERTY_PREFIX + ".redis.sentinel.nodes[0].port", "26379");
        environment.setProperty(PROPERTY_PREFIX + ".redis.sentinel.nodes[1].host", "sent2");
        environment.setProperty(PROPERTY_PREFIX + ".redis.sentinel.nodes[1].port", "26379");

        RedisOptions options = redisConnectionFactory.buildRedisOptions();

        List<String> sentinelEndpoints = new ArrayList<>();
        sentinelEndpoints.add("redis://sent1:26379");
        sentinelEndpoints.add("redis://sent2:26379");

        assertThat(options).isNotNull();
        assertThat(options.getEndpoints()).containsAll(sentinelEndpoints);
    }

    @Test
    void shouldReturnRedisOptionsWithSentinelsSecuredEndpoints() {
        environment.setProperty(PROPERTY_PREFIX + ".redis.ssl", "true");
        environment.setProperty(PROPERTY_PREFIX + ".redis.sentinel.master", "redis-master");
        environment.setProperty(PROPERTY_PREFIX + ".redis.sentinel.nodes[0].host", "sent1");
        environment.setProperty(PROPERTY_PREFIX + ".redis.sentinel.nodes[0].port", "26379");
        environment.setProperty(PROPERTY_PREFIX + ".redis.sentinel.nodes[1].host", "sent2");
        environment.setProperty(PROPERTY_PREFIX + ".redis.sentinel.nodes[1].port", "26379");

        RedisOptions options = redisConnectionFactory.buildRedisOptions();

        List<String> sentinelEndpoints = new ArrayList<>();
        sentinelEndpoints.add("rediss://sent1:26379");
        sentinelEndpoints.add("rediss://sent2:26379");

        assertThat(options).isNotNull();
        assertThat(options.getNetClientOptions().getHostnameVerificationAlgorithm()).isEqualTo("");
        assertThat(options.getEndpoints()).containsAll(sentinelEndpoints);
    }

    @Test
    void shouldThrowAnExceptionWhenMissingSentinelMaster() {
        environment.setProperty(PROPERTY_PREFIX + ".redis.sentinel.nodes[0].host", "sent1");
        environment.setProperty(PROPERTY_PREFIX + ".redis.sentinel.nodes[0].port", "26379");
        environment.setProperty(PROPERTY_PREFIX + ".redis.sentinel.nodes[1].host", "sent2");
        environment.setProperty(PROPERTY_PREFIX + ".redis.sentinel.nodes[1].port", "26379");

        assertThatIllegalStateException()
            .isThrownBy(() -> redisConnectionFactory.buildRedisOptions())
            .withMessageContaining("Incorrect Sentinel configuration");
    }

    @Test
    void shouldReturnRedisOptionsWithPemTruststoreAndKeystore() {
        String keystoreCertPath = "/path/to/client.crt";
        String keystoreKeyPath = "/path/to/client.key";
        String truststorePath = "/path/to/ca.crt";
        environment.setProperty(PROPERTY_PREFIX + ".redis.host", "redis");
        environment.setProperty(PROPERTY_PREFIX + ".redis.port", "6379");
        environment.setProperty(PROPERTY_PREFIX + ".redis.ssl", "true");
        environment.setProperty(PROPERTY_PREFIX + ".redis.hostnameVerificationAlgorithm", "HTTPS");
        environment.setProperty(PROPERTY_PREFIX + ".redis.trustAll", "false");
        environment.setProperty(PROPERTY_PREFIX + ".redis.keystore.type", "pem");
        environment.setProperty(PROPERTY_PREFIX + ".redis.keystore.certificates[0].cert", keystoreCertPath);
        environment.setProperty(PROPERTY_PREFIX + ".redis.keystore.certificates[0].key", keystoreKeyPath);

        environment.setProperty(PROPERTY_PREFIX + ".redis.truststore.type", "pem");
        environment.setProperty(PROPERTY_PREFIX + ".redis.truststore.path", truststorePath);

        RedisOptions options = redisConnectionFactory.buildRedisOptions();

        PemKeyCertOptions pemKeyCertOptions = new PemKeyCertOptions();
        pemKeyCertOptions.addCertPath(keystoreCertPath);
        pemKeyCertOptions.addKeyPath(keystoreKeyPath);

        PemTrustOptions pemTrustOptions = new PemTrustOptions();
        pemTrustOptions.addCertPath(truststorePath);

        assertThat(options).isNotNull();
        assertThat(options.getNetClientOptions().getHostnameVerificationAlgorithm()).isEqualTo("HTTPS");
        assertThat(options.getNetClientOptions().getPemKeyCertOptions()).usingRecursiveComparison().isEqualTo(pemKeyCertOptions);
        assertThat(options.getNetClientOptions().getPemTrustOptions()).usingRecursiveComparison().isEqualTo(pemTrustOptions);
    }

    @Test
    void shouldReturnRedisOptionsWithPKCS12TruststoreAndKeystore() {
        String keystorePath = "/path/to/client.pkcs12";
        String truststorePath = "/path/to/ca.pkcs12";
        environment.setProperty(PROPERTY_PREFIX + ".redis.host", "redis");
        environment.setProperty(PROPERTY_PREFIX + ".redis.port", "6379");
        environment.setProperty(PROPERTY_PREFIX + ".redis.ssl", "true");
        environment.setProperty(PROPERTY_PREFIX + ".redis.trustAll", "false");
        environment.setProperty(PROPERTY_PREFIX + ".redis.keystore.type", "pkcs12");
        environment.setProperty(PROPERTY_PREFIX + ".redis.keystore.path", keystorePath);

        environment.setProperty(PROPERTY_PREFIX + ".redis.truststore.type", "pkcs12");
        environment.setProperty(PROPERTY_PREFIX + ".redis.truststore.path", truststorePath);

        RedisOptions options = redisConnectionFactory.buildRedisOptions();

        PfxOptions keystorePfxOptions = new PfxOptions();
        keystorePfxOptions.setPath(keystorePath);

        PfxOptions truststorePfxOptions = new PfxOptions();
        truststorePfxOptions.setPath(truststorePath);

        assertThat(options).isNotNull();
        assertThat(options.getNetClientOptions().getPfxKeyCertOptions()).usingRecursiveComparison().isEqualTo(keystorePfxOptions);
        assertThat(options.getNetClientOptions().getPfxTrustOptions()).usingRecursiveComparison().isEqualTo(truststorePfxOptions);
    }

    @Test
    void shouldReturnRedisOptionsWithJKSTruststoreAndKeystore() {
        String keystorePath = "/path/to/client.jks";
        String truststorePath = "/path/to/ca.jks";
        environment.setProperty(PROPERTY_PREFIX + ".redis.host", "redis");
        environment.setProperty(PROPERTY_PREFIX + ".redis.port", "6379");
        environment.setProperty(PROPERTY_PREFIX + ".redis.ssl", "true");
        environment.setProperty(PROPERTY_PREFIX + ".redis.trustAll", "false");
        environment.setProperty(PROPERTY_PREFIX + ".redis.keystore.type", "jks");
        environment.setProperty(PROPERTY_PREFIX + ".redis.keystore.path", keystorePath);

        environment.setProperty(PROPERTY_PREFIX + ".redis.truststore.type", "jks");
        environment.setProperty(PROPERTY_PREFIX + ".redis.truststore.path", truststorePath);

        RedisOptions options = redisConnectionFactory.buildRedisOptions();

        JksOptions keystoreJksOptions = new JksOptions();
        keystoreJksOptions.setPath(keystorePath);

        JksOptions truststoreJksOptions = new JksOptions();
        truststoreJksOptions.setPath(truststorePath);

        assertThat(options).isNotNull();
        assertThat(options.getNetClientOptions().getKeyStoreOptions()).usingRecursiveComparison().isEqualTo(keystoreJksOptions);
        assertThat(options.getNetClientOptions().getTrustStoreOptions()).usingRecursiveComparison().isEqualTo(truststoreJksOptions);
    }

    @ParameterizedTest
    @ValueSource(strings = { "JKS", "PEM", "PKCS12" })
    void shouldThrowAnExceptionWhenSslEnabledAndMissingKeystoreCertPath(String keystoreType) {
        environment.setProperty(PROPERTY_PREFIX + ".redis.host", "redis");
        environment.setProperty(PROPERTY_PREFIX + ".redis.port", "6379");
        environment.setProperty(PROPERTY_PREFIX + ".redis.ssl", "true");
        environment.setProperty(PROPERTY_PREFIX + ".redis.trustAll", "false");
        environment.setProperty(PROPERTY_PREFIX + ".redis.keystore.type", keystoreType);
        environment.setProperty(PROPERTY_PREFIX + ".redis.truststore.type", "jks");
        environment.setProperty(PROPERTY_PREFIX + ".redis.truststore.path", "truststorePath");

        assertThatIllegalArgumentException()
            .isThrownBy(() -> redisConnectionFactory.buildRedisOptions())
            .withMessageContaining("Missing " + keystoreType + " keystore value");
    }

    @ParameterizedTest
    @ValueSource(strings = { "JKS", "PEM", "PKCS12" })
    void shouldThrowAnExceptionWhenSslEnabledAndMissingTruststoreCertPath(String truststoreType) {
        environment.setProperty(PROPERTY_PREFIX + ".redis.host", "redis");
        environment.setProperty(PROPERTY_PREFIX + ".redis.port", "6379");
        environment.setProperty(PROPERTY_PREFIX + ".redis.ssl", "true");
        environment.setProperty(PROPERTY_PREFIX + ".redis.trustAll", "false");
        environment.setProperty(PROPERTY_PREFIX + ".redis.keystore.type", "jks");
        environment.setProperty(PROPERTY_PREFIX + ".redis.keystore.path", "keystorePath");
        environment.setProperty(PROPERTY_PREFIX + ".redis.truststore.type", truststoreType);

        assertThatIllegalArgumentException()
            .isThrownBy(() -> redisConnectionFactory.buildRedisOptions())
            .withMessageContaining("Missing " + truststoreType + " truststore value");
    }

    @Test
    void shouldReturnRedisOptionsWithTrustAllEnabledToAvoidBreakingChangeWhenOnlySslConfigured() {
        environment.setProperty(PROPERTY_PREFIX + ".redis.host", "redis");
        environment.setProperty(PROPERTY_PREFIX + ".redis.port", "6379");
        environment.setProperty(PROPERTY_PREFIX + ".redis.ssl", "true");

        RedisOptions options = redisConnectionFactory.buildRedisOptions();

        assertThat(options.getNetClientOptions()).isNotNull();
        assertThat(options.getNetClientOptions().isTrustAll()).isTrue();
    }

    @Test
    void shouldReturnRedisOptionsWithTCPTimeouts() {
        environment.setProperty(PROPERTY_PREFIX + ".redis.tcp.connectTimeout", "1234");
        environment.setProperty(PROPERTY_PREFIX + ".redis.tcp.idleTimeout", "5678");

        RedisOptions options = redisConnectionFactory.buildRedisOptions();

        assertThat(options.getNetClientOptions()).isNotNull();
        assertThat(options.getNetClientOptions().getConnectTimeout()).isEqualTo(1234);
        assertThat(options.getNetClientOptions().getIdleTimeout()).isEqualTo(5678);
    }
}
