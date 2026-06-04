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

import io.gravitee.plugin.configurations.redis.RedisClientOptions;
import io.gravitee.plugin.configurations.ssl.jks.JKSKeyStore;
import io.gravitee.plugin.configurations.ssl.jks.JKSTrustStore;
import io.gravitee.plugin.configurations.ssl.pem.PEMKeyStore;
import io.gravitee.plugin.configurations.ssl.pem.PEMTrustStore;
import io.gravitee.plugin.configurations.ssl.pkcs12.PKCS12KeyStore;
import io.gravitee.plugin.configurations.ssl.pkcs12.PKCS12TrustStore;
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
    void shouldReturnRedisClientOptionsWithSslEnabled() {
        environment.setProperty(PROPERTY_PREFIX + ".redis.host", "redis");
        environment.setProperty(PROPERTY_PREFIX + ".redis.port", "6379");
        environment.setProperty(PROPERTY_PREFIX + ".redis.ssl", "true");

        RedisClientOptions options = redisConnectionFactory.buildRedisClientOptions();

        assertThat(options).isNotNull();
        assertThat(options.getHost()).isEqualTo("redis");
        assertThat(options.getPort()).isEqualTo(6379);
        assertThat(options.isUseSsl()).isTrue();
    }

    @Test
    void shouldReturnRedisClientOptionsWithoutSsl() {
        environment.setProperty(PROPERTY_PREFIX + ".redis.host", "redis");
        environment.setProperty(PROPERTY_PREFIX + ".redis.port", "6379");

        RedisClientOptions options = redisConnectionFactory.buildRedisClientOptions();

        assertThat(options).isNotNull();
        assertThat(options.getHost()).isEqualTo("redis");
        assertThat(options.getPort()).isEqualTo(6379);
        assertThat(options.isUseSsl()).isFalse();
    }

    @Test
    public void shouldReturnRedisClientOptionsWithSentinel() {
        environment.setProperty(PROPERTY_PREFIX + ".redis.sentinel.master", "redis-master");
        environment.setProperty(PROPERTY_PREFIX + ".redis.sentinel.nodes[0].host", "sent1");
        environment.setProperty(PROPERTY_PREFIX + ".redis.sentinel.nodes[0].port", "26379");
        environment.setProperty(PROPERTY_PREFIX + ".redis.sentinel.nodes[1].host", "sent2");
        environment.setProperty(PROPERTY_PREFIX + ".redis.sentinel.nodes[1].port", "26379");

        RedisClientOptions options = redisConnectionFactory.buildRedisClientOptions();

        assertThat(options).isNotNull();
        assertThat(options.getSentinel()).isNotNull();
        assertThat(options.getSentinel().getMasterId()).isEqualTo("redis-master");
        assertThat(options.getSentinel().getNodes()).hasSize(2);
        assertThat(options.getSentinel().getNodes().get(0).getHost()).isEqualTo("sent1");
        assertThat(options.getSentinel().getNodes().get(1).getHost()).isEqualTo("sent2");
    }

    @Test
    void shouldReturnRedisClientOptionsWithCluster() {
        environment.setProperty(PROPERTY_PREFIX + ".redis.cluster.nodes[0].host", "node1");
        environment.setProperty(PROPERTY_PREFIX + ".redis.cluster.nodes[0].port", "6379");
        environment.setProperty(PROPERTY_PREFIX + ".redis.cluster.nodes[1].host", "node2");
        environment.setProperty(PROPERTY_PREFIX + ".redis.cluster.nodes[1].port", "6380");

        RedisClientOptions options = redisConnectionFactory.buildRedisClientOptions();

        assertThat(options).isNotNull();
        assertThat(options.getCluster()).isNotNull();
        assertThat(options.getCluster().getNodes()).hasSize(2);
        assertThat(options.getCluster().getNodes().get(0).getHost()).isEqualTo("node1");
        assertThat(options.getCluster().getNodes().get(1).getPort()).isEqualTo(6380);
        // Rate limiting must read master-consistent counters — never from replicas.
        assertThat(options.getCluster().getUseReplicas()).isEqualTo("NEVER");
    }

    @Test
    void shouldReturnRedisClientOptionsWithSentinelAndSsl() {
        environment.setProperty(PROPERTY_PREFIX + ".redis.ssl", "true");
        environment.setProperty(PROPERTY_PREFIX + ".redis.sentinel.master", "redis-master");
        environment.setProperty(PROPERTY_PREFIX + ".redis.sentinel.nodes[0].host", "sent1");
        environment.setProperty(PROPERTY_PREFIX + ".redis.sentinel.nodes[0].port", "26379");
        environment.setProperty(PROPERTY_PREFIX + ".redis.sentinel.nodes[1].host", "sent2");
        environment.setProperty(PROPERTY_PREFIX + ".redis.sentinel.nodes[1].port", "26379");

        RedisClientOptions options = redisConnectionFactory.buildRedisClientOptions();

        assertThat(options).isNotNull();
        assertThat(options.isUseSsl()).isTrue();
        assertThat(options.getSsl()).isNotNull();
        assertThat(options.getSsl().isHostnameVerifier()).isFalse();
        assertThat(options.getSsl().getHostnameVerificationAlgorithm()).isEqualTo("NONE");
        assertThat(options.getSentinel()).isNotNull();
        assertThat(options.getSentinel().getMasterId()).isEqualTo("redis-master");
        assertThat(options.getSentinel().getNodes()).hasSize(2);
    }

    @Test
    void shouldPreserveLdapsHostnameVerificationAlgorithm() {
        environment.setProperty(PROPERTY_PREFIX + ".redis.host", "redis");
        environment.setProperty(PROPERTY_PREFIX + ".redis.port", "6379");
        environment.setProperty(PROPERTY_PREFIX + ".redis.ssl", "true");
        environment.setProperty(PROPERTY_PREFIX + ".redis.hostnameVerificationAlgorithm", "LDAPS");

        RedisClientOptions options = redisConnectionFactory.buildRedisClientOptions();

        assertThat(options.getSsl().getHostnameVerificationAlgorithm()).isEqualTo("LDAPS");
        assertThat(options.getSsl().isHostnameVerifier()).isTrue();
    }

    @Test
    void shouldReturnRedisClientOptionsWithMultiCertPemKeystore() {
        environment.setProperty(PROPERTY_PREFIX + ".redis.host", "redis");
        environment.setProperty(PROPERTY_PREFIX + ".redis.port", "6379");
        environment.setProperty(PROPERTY_PREFIX + ".redis.ssl", "true");
        environment.setProperty(PROPERTY_PREFIX + ".redis.trustAll", "true");
        environment.setProperty(PROPERTY_PREFIX + ".redis.keystore.type", "pem");
        environment.setProperty(PROPERTY_PREFIX + ".redis.keystore.certificates[0].cert", "/path/cert0.pem");
        environment.setProperty(PROPERTY_PREFIX + ".redis.keystore.certificates[0].key", "/path/key0.pem");
        environment.setProperty(PROPERTY_PREFIX + ".redis.keystore.certificates[1].cert", "/path/cert1.pem");
        environment.setProperty(PROPERTY_PREFIX + ".redis.keystore.certificates[1].key", "/path/key1.pem");

        RedisClientOptions options = redisConnectionFactory.buildRedisClientOptions();

        assertThat(options.getSsl().getKeyStore()).isInstanceOf(PEMKeyStore.class);
        PEMKeyStore pemKeyStore = (PEMKeyStore) options.getSsl().getKeyStore();
        assertThat(pemKeyStore.getCertPaths()).containsExactly("/path/cert0.pem", "/path/cert1.pem");
        assertThat(pemKeyStore.getKeyPaths()).containsExactly("/path/key0.pem", "/path/key1.pem");
    }

    @Test
    void shouldThrowAnExceptionWhenMissingSentinelMaster() {
        environment.setProperty(PROPERTY_PREFIX + ".redis.sentinel.nodes[0].host", "sent1");
        environment.setProperty(PROPERTY_PREFIX + ".redis.sentinel.nodes[0].port", "26379");
        environment.setProperty(PROPERTY_PREFIX + ".redis.sentinel.nodes[1].host", "sent2");
        environment.setProperty(PROPERTY_PREFIX + ".redis.sentinel.nodes[1].port", "26379");

        assertThatIllegalStateException()
            .isThrownBy(() -> redisConnectionFactory.buildRedisClientOptions())
            .withMessageContaining("Incorrect Sentinel configuration");
    }

    @Test
    void shouldReturnRedisClientOptionsWithPemTruststoreAndKeystore() {
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

        RedisClientOptions options = redisConnectionFactory.buildRedisClientOptions();

        assertThat(options).isNotNull();
        assertThat(options.getSsl()).isNotNull();
        assertThat(options.getSsl().isHostnameVerifier()).isTrue();

        assertThat(options.getSsl().getKeyStore()).isInstanceOf(PEMKeyStore.class);
        PEMKeyStore pemKeyStore = (PEMKeyStore) options.getSsl().getKeyStore();
        assertThat(pemKeyStore.getCertPath()).isEqualTo(keystoreCertPath);
        assertThat(pemKeyStore.getKeyPath()).isEqualTo(keystoreKeyPath);

        assertThat(options.getSsl().getTrustStore()).isInstanceOf(PEMTrustStore.class);
        PEMTrustStore pemTrustStore = (PEMTrustStore) options.getSsl().getTrustStore();
        assertThat(pemTrustStore.getPath()).isEqualTo(truststorePath);
    }

    @Test
    void shouldReturnRedisClientOptionsWithPKCS12TruststoreAndKeystore() {
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

        RedisClientOptions options = redisConnectionFactory.buildRedisClientOptions();

        assertThat(options).isNotNull();
        assertThat(options.getSsl().getKeyStore()).isInstanceOf(PKCS12KeyStore.class);
        assertThat(((PKCS12KeyStore) options.getSsl().getKeyStore()).getPath()).isEqualTo(keystorePath);
        assertThat(options.getSsl().getTrustStore()).isInstanceOf(PKCS12TrustStore.class);
        assertThat(((PKCS12TrustStore) options.getSsl().getTrustStore()).getPath()).isEqualTo(truststorePath);
    }

    @Test
    void shouldReturnRedisClientOptionsWithJKSTruststoreAndKeystore() {
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

        RedisClientOptions options = redisConnectionFactory.buildRedisClientOptions();

        assertThat(options).isNotNull();
        assertThat(options.getSsl().getKeyStore()).isInstanceOf(JKSKeyStore.class);
        assertThat(((JKSKeyStore) options.getSsl().getKeyStore()).getPath()).isEqualTo(keystorePath);
        assertThat(options.getSsl().getTrustStore()).isInstanceOf(JKSTrustStore.class);
        assertThat(((JKSTrustStore) options.getSsl().getTrustStore()).getPath()).isEqualTo(truststorePath);
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
            .isThrownBy(() -> redisConnectionFactory.buildRedisClientOptions())
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
            .isThrownBy(() -> redisConnectionFactory.buildRedisClientOptions())
            .withMessageContaining("Missing " + truststoreType + " truststore value");
    }

    @Test
    void shouldReturnRedisClientOptionsWithTrustAllEnabledByDefault() {
        environment.setProperty(PROPERTY_PREFIX + ".redis.host", "redis");
        environment.setProperty(PROPERTY_PREFIX + ".redis.port", "6379");
        environment.setProperty(PROPERTY_PREFIX + ".redis.ssl", "true");

        RedisClientOptions options = redisConnectionFactory.buildRedisClientOptions();

        assertThat(options.getSsl()).isNotNull();
        assertThat(options.getSsl().isTrustAll()).isTrue();
    }

    @Test
    void shouldReturnRedisClientOptionsWithTCPTimeouts() {
        environment.setProperty(PROPERTY_PREFIX + ".redis.tcp.connectTimeout", "1234");
        environment.setProperty(PROPERTY_PREFIX + ".redis.tcp.idleTimeout", "5678");

        RedisClientOptions options = redisConnectionFactory.buildRedisClientOptions();

        assertThat(options).isNotNull();
        assertThat(options.getConnectTimeout()).isEqualTo(1234);
        assertThat(options.getIdleTimeout()).isEqualTo(5678);
    }

    @Test
    void shouldReturnRedisClientOptionsWithUsernameAndPassword() {
        environment.setProperty(PROPERTY_PREFIX + ".redis.host", "redis");
        environment.setProperty(PROPERTY_PREFIX + ".redis.port", "6379");
        environment.setProperty(PROPERTY_PREFIX + ".redis.username", "testuser");
        environment.setProperty(PROPERTY_PREFIX + ".redis.password", "testpass");

        RedisClientOptions options = redisConnectionFactory.buildRedisClientOptions();

        assertThat(options).isNotNull();
        assertThat(options.getUsername()).isEqualTo("testuser");
        assertThat(options.getPassword()).isEqualTo("testpass");
    }

    @Test
    void shouldReturnRedisClientOptionsWithOnlyPassword() {
        environment.setProperty(PROPERTY_PREFIX + ".redis.host", "redis");
        environment.setProperty(PROPERTY_PREFIX + ".redis.port", "6379");
        environment.setProperty(PROPERTY_PREFIX + ".redis.password", "testpass");

        RedisClientOptions options = redisConnectionFactory.buildRedisClientOptions();

        assertThat(options).isNotNull();
        assertThat(options.getPassword()).isEqualTo("testpass");
        assertThat(options.getUsername()).isNull();
    }
}
