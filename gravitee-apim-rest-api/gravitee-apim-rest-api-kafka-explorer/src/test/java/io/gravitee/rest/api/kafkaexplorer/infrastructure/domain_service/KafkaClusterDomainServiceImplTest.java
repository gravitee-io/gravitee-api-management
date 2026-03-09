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
package io.gravitee.rest.api.kafkaexplorer.infrastructure.domain_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.cluster.model.KafkaClusterConfiguration;
import io.gravitee.apim.core.cluster.model.SaslConfig;
import io.gravitee.apim.core.cluster.model.SaslMechanism;
import io.gravitee.apim.core.cluster.model.SecurityConfig;
import io.gravitee.apim.core.cluster.model.SecurityProtocol;
import io.gravitee.plugin.configurations.ssl.SslOptions;
import io.gravitee.plugin.configurations.ssl.jks.JKSKeyStore;
import io.gravitee.plugin.configurations.ssl.jks.JKSTrustStore;
import io.gravitee.plugin.configurations.ssl.pem.PEMKeyStore;
import io.gravitee.plugin.configurations.ssl.pem.PEMTrustStore;
import io.gravitee.plugin.configurations.ssl.pkcs12.PKCS12KeyStore;
import io.gravitee.plugin.configurations.ssl.pkcs12.PKCS12TrustStore;
import io.gravitee.rest.api.kafkaexplorer.domain.exception.KafkaExplorerException;
import io.gravitee.rest.api.kafkaexplorer.domain.exception.TechnicalCode;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.errors.AuthenticationException;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class KafkaClusterDomainServiceImplTest {

    private static final KafkaClusterConfiguration CONFIG = new KafkaClusterConfiguration(
        "localhost:9092",
        new SecurityConfig(SecurityProtocol.PLAINTEXT, null, null)
    );

    @Test
    void should_throw_connection_failed_when_admin_client_creation_fails() {
        var service = serviceWithAdminClient(() -> {
            throw new RuntimeException("Cannot create admin client");
        });

        assertThatThrownBy(() -> service.describeCluster(CONFIG))
            .isInstanceOf(KafkaExplorerException.class)
            .hasMessageContaining("Failed to connect to Kafka cluster")
            .satisfies(e -> assertThat(((KafkaExplorerException) e).getTechnicalCode()).isEqualTo(TechnicalCode.CONNECTION_FAILED));
    }

    @Test
    void should_throw_timeout_when_cluster_response_times_out() throws Exception {
        var service = serviceWithFailingFuture(new TimeoutException("Request timed out"));

        assertThatThrownBy(() -> service.describeCluster(CONFIG))
            .isInstanceOf(KafkaExplorerException.class)
            .hasMessageContaining("timed out")
            .satisfies(e -> assertThat(((KafkaExplorerException) e).getTechnicalCode()).isEqualTo(TechnicalCode.TIMEOUT));
    }

    @Test
    void should_throw_authentication_failed_when_auth_error() throws Exception {
        var service = serviceWithFailingFuture(new ExecutionException(new AuthenticationException("Bad credentials")));

        assertThatThrownBy(() -> service.describeCluster(CONFIG))
            .isInstanceOf(KafkaExplorerException.class)
            .hasMessageContaining("Authentication failed")
            .satisfies(e -> assertThat(((KafkaExplorerException) e).getTechnicalCode()).isEqualTo(TechnicalCode.AUTHENTICATION_FAILED));
    }

    @Test
    void should_throw_interrupted_when_thread_is_interrupted() throws Exception {
        var service = serviceWithFailingFuture(new InterruptedException("Thread interrupted"));

        assertThatThrownBy(() -> service.describeCluster(CONFIG))
            .isInstanceOf(KafkaExplorerException.class)
            .hasMessageContaining("interrupted")
            .satisfies(e -> assertThat(((KafkaExplorerException) e).getTechnicalCode()).isEqualTo(TechnicalCode.INTERRUPTED));

        // Reset interrupted flag
        Thread.interrupted();
    }

    @Test
    void should_throw_connection_failed_when_execution_fails_with_unknown_cause() throws Exception {
        var service = serviceWithFailingFuture(new ExecutionException(new RuntimeException("Something went wrong")));

        assertThatThrownBy(() -> service.describeCluster(CONFIG))
            .isInstanceOf(KafkaExplorerException.class)
            .hasMessageContaining("Failed to connect to Kafka cluster")
            .satisfies(e -> assertThat(((KafkaExplorerException) e).getTechnicalCode()).isEqualTo(TechnicalCode.CONNECTION_FAILED));
    }

    @Nested
    class SslConfiguration {

        @Test
        void should_configure_ssl_protocol() {
            var ssl = SslOptions.builder().build();
            var config = configWith(SecurityProtocol.SSL, null, ssl);

            var properties = captureProperties(config);

            assertThat(properties.get("security.protocol")).isEqualTo("SSL");
        }

        @Test
        void should_disable_endpoint_identification_when_trust_all() {
            var ssl = SslOptions.builder().trustAll(true).build();
            var config = configWith(SecurityProtocol.SSL, null, ssl);

            var properties = captureProperties(config);

            assertThat(properties.get(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG)).isEqualTo("");
        }

        @Test
        void should_disable_endpoint_identification_when_hostname_verifier_false() {
            var ssl = SslOptions.builder().hostnameVerifier(false).build();
            var config = configWith(SecurityProtocol.SSL, null, ssl);

            var properties = captureProperties(config);

            assertThat(properties.get(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG)).isEqualTo("");
        }

        @Test
        void should_configure_jks_truststore_with_path() {
            var trustStore = JKSTrustStore.builder().path("/tmp/truststore.jks").password("changeit").build();
            var ssl = SslOptions.builder().trustStore(trustStore).build();
            var config = configWith(SecurityProtocol.SSL, null, ssl);

            var properties = captureProperties(config);

            assertThat(properties.get(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG)).isEqualTo("JKS");
            assertThat(properties.get(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG)).isEqualTo("/tmp/truststore.jks");
            assertThat(properties.get(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG)).isEqualTo("changeit");
        }

        @Test
        void should_configure_jks_truststore_with_base64_content() {
            var base64Content = Base64.getEncoder().encodeToString("fake-jks-content".getBytes());
            var trustStore = JKSTrustStore.builder().content(base64Content).password("changeit").build();
            var ssl = SslOptions.builder().trustStore(trustStore).build();
            var config = configWith(SecurityProtocol.SSL, null, ssl);

            var properties = captureProperties(config);

            assertThat(properties.get(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG)).isEqualTo("JKS");
            var location = (String) properties.get(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG);
            assertThat(location).isNotNull().contains("kafka-ssl-").endsWith(".store");
            assertThat(properties.get(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG)).isEqualTo("changeit");
        }

        @Test
        void should_configure_pkcs12_truststore_with_path() {
            var trustStore = PKCS12TrustStore.builder().path("/tmp/truststore.p12").password("changeit").build();
            var ssl = SslOptions.builder().trustStore(trustStore).build();
            var config = configWith(SecurityProtocol.SSL, null, ssl);

            var properties = captureProperties(config);

            assertThat(properties.get(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG)).isEqualTo("PKCS12");
            assertThat(properties.get(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG)).isEqualTo("/tmp/truststore.p12");
            assertThat(properties.get(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG)).isEqualTo("changeit");
        }

        @Test
        void should_configure_pem_truststore_with_content() {
            var pemContent = "-----BEGIN CERTIFICATE-----\nfake\n-----END CERTIFICATE-----";
            var trustStore = PEMTrustStore.builder().content(pemContent).build();
            var ssl = SslOptions.builder().trustStore(trustStore).build();
            var config = configWith(SecurityProtocol.SSL, null, ssl);

            var properties = captureProperties(config);

            assertThat(properties.get(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG)).isEqualTo("PEM");
            assertThat(properties.get(SslConfigs.SSL_TRUSTSTORE_CERTIFICATES_CONFIG)).isEqualTo(pemContent);
        }

        @Test
        void should_configure_pem_truststore_with_path() throws IOException {
            var certPath = Path.of("src/test/resources/ssl/test-cert.pem").toAbsolutePath().toString();
            var trustStore = PEMTrustStore.builder().path(certPath).build();
            var ssl = SslOptions.builder().trustStore(trustStore).build();
            var config = configWith(SecurityProtocol.SSL, null, ssl);

            var properties = captureProperties(config);

            assertThat(properties.get(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG)).isEqualTo("PEM");
            var certificates = (String) properties.get(SslConfigs.SSL_TRUSTSTORE_CERTIFICATES_CONFIG);
            assertThat(certificates).contains("-----BEGIN CERTIFICATE-----");
        }

        @Test
        void should_configure_jks_keystore_with_path() {
            var keyStore = JKSKeyStore.builder().path("/tmp/keystore.jks").password("changeit").keyPassword("keypass").build();
            var ssl = SslOptions.builder().keyStore(keyStore).build();
            var config = configWith(SecurityProtocol.SSL, null, ssl);

            var properties = captureProperties(config);

            assertThat(properties.get(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG)).isEqualTo("JKS");
            assertThat(properties.get(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG)).isEqualTo("/tmp/keystore.jks");
            assertThat(properties.get(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG)).isEqualTo("changeit");
            assertThat(properties.get(SslConfigs.SSL_KEY_PASSWORD_CONFIG)).isEqualTo("keypass");
        }

        @Test
        void should_configure_pkcs12_keystore_with_path() {
            var keyStore = PKCS12KeyStore.builder().path("/tmp/keystore.p12").password("changeit").build();
            var ssl = SslOptions.builder().keyStore(keyStore).build();
            var config = configWith(SecurityProtocol.SSL, null, ssl);

            var properties = captureProperties(config);

            assertThat(properties.get(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG)).isEqualTo("PKCS12");
            assertThat(properties.get(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG)).isEqualTo("/tmp/keystore.p12");
            assertThat(properties.get(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG)).isEqualTo("changeit");
        }

        @Test
        void should_configure_pem_keystore_with_content() {
            var certContent = "-----BEGIN CERTIFICATE-----\nfake-cert\n-----END CERTIFICATE-----";
            var keyContent = "-----BEGIN PRIVATE KEY-----\nfake-key\n-----END PRIVATE KEY-----";
            var keyStore = PEMKeyStore.builder().certContent(certContent).keyContent(keyContent).build();
            var ssl = SslOptions.builder().keyStore(keyStore).build();
            var config = configWith(SecurityProtocol.SSL, null, ssl);

            var properties = captureProperties(config);

            assertThat(properties.get(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG)).isEqualTo("PEM");
            assertThat(properties.get(SslConfigs.SSL_KEYSTORE_CERTIFICATE_CHAIN_CONFIG)).isEqualTo(certContent);
            assertThat(properties.get(SslConfigs.SSL_KEYSTORE_KEY_CONFIG)).isEqualTo(keyContent);
        }

        @Test
        void should_configure_sasl_ssl() {
            var sasl = new SaslConfig(new SaslMechanism("PLAIN", "user", "pass"));
            var ssl = SslOptions.builder().trustAll(true).build();
            var config = configWith(SecurityProtocol.SASL_SSL, sasl, ssl);

            var properties = captureProperties(config);

            assertThat(properties.get("security.protocol")).isEqualTo("SASL_SSL");
            assertThat(properties.get("sasl.mechanism")).isEqualTo("PLAIN");
            assertThat(properties.get(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG)).isEqualTo("");
        }

        @Test
        void should_not_set_ssl_properties_for_plaintext() {
            var config = configWith(SecurityProtocol.PLAINTEXT, null, null);

            var properties = captureProperties(config);

            assertThat(properties.get("security.protocol")).isEqualTo("PLAINTEXT");
            assertThat(
                properties
                    .keySet()
                    .stream()
                    .map(Object::toString)
                    .filter(k -> k.startsWith("ssl."))
            ).isEmpty();
        }

        @Test
        void should_cleanup_temp_files_after_execution() {
            var base64Content = Base64.getEncoder().encodeToString("fake-jks-content".getBytes());
            var trustStore = JKSTrustStore.builder().content(base64Content).build();
            var ssl = SslOptions.builder().trustStore(trustStore).build();
            var config = configWith(SecurityProtocol.SSL, null, ssl);

            // Capture the temp file path, then verify it's cleaned up
            var tempFilePath = new AtomicReference<String>();
            var service = new KafkaClusterDomainServiceImpl() {
                @Override
                protected AdminClient createAdminClient(Properties properties) {
                    tempFilePath.set((String) properties.get(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG));
                    throw new RuntimeException("capture");
                }
            };
            try {
                service.describeCluster(config);
            } catch (Exception ignored) {}

            assertThat(tempFilePath.get()).isNotNull();
            assertThat(Path.of(tempFilePath.get())).doesNotExist();
        }

        private KafkaClusterConfiguration configWith(SecurityProtocol protocol, SaslConfig sasl, SslOptions ssl) {
            return new KafkaClusterConfiguration("localhost:9092", new SecurityConfig(protocol, sasl, ssl));
        }

        private Properties captureProperties(KafkaClusterConfiguration config) {
            var captured = new AtomicReference<Properties>();
            var service = new KafkaClusterDomainServiceImpl() {
                @Override
                protected AdminClient createAdminClient(Properties properties) {
                    captured.set(new Properties());
                    captured.get().putAll(properties);
                    throw new RuntimeException("capture");
                }
            };
            try {
                service.describeCluster(config);
            } catch (Exception ignored) {}
            return captured.get();
        }
    }

    @SuppressWarnings("unchecked")
    private KafkaClusterDomainServiceImpl serviceWithFailingFuture(Exception exception) throws Exception {
        KafkaFuture<String> failingFuture = mock(KafkaFuture.class);
        when(failingFuture.get(anyLong(), any())).thenThrow(exception);

        DescribeClusterResult describeResult = mock(DescribeClusterResult.class);
        when(describeResult.clusterId()).thenReturn(failingFuture);

        AdminClient adminClient = mock(AdminClient.class);
        when(adminClient.describeCluster()).thenReturn(describeResult);

        return serviceWithAdminClient(() -> adminClient);
    }

    private KafkaClusterDomainServiceImpl serviceWithAdminClient(AdminClientFactory factory) {
        return new KafkaClusterDomainServiceImpl() {
            @Override
            protected AdminClient createAdminClient(Properties properties) {
                return factory.create();
            }
        };
    }

    @FunctionalInterface
    private interface AdminClientFactory {
        AdminClient create();
    }
}
