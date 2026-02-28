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
package io.gravitee.rest.api.kafkaexplorer.resource;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.rest.api.kafkaexplorer.rest.model.DescribeClusterRequest;
import io.gravitee.rest.api.kafkaexplorer.rest.model.DescribeClusterResponse;
import io.gravitee.rest.api.kafkaexplorer.rest.model.KafkaExplorerError;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class KafkaExplorerResourceIntegrationTest extends AbstractKafkaExplorerResourceIntegrationTest {

    private static final String SASL_USERNAME = "admin";
    private static final String SASL_PASSWORD = "admin-secret";

    @Nested
    class SaslPlaintext {

        @Test
        void should_return_200_with_valid_credentials() {
            givenClusterWithConfig(
                Map.of(
                    "bootstrapServers",
                    saslBootstrapServers(),
                    "security",
                    Map.of(
                        "protocol",
                        "SASL_PLAINTEXT",
                        "sasl",
                        Map.of("mechanism", Map.of("type", "PLAIN", "username", SASL_USERNAME, "password", SASL_PASSWORD))
                    )
                )
            );

            var request = new DescribeClusterRequest().clusterId(CLUSTER_ID);

            var response = resource.describeCluster(request);

            assertThat(response.getStatus()).isEqualTo(200);
            var body = (DescribeClusterResponse) response.getEntity();
            assertThat(body.getClusterId()).isNotBlank();
            assertThat(body.getNodes()).isNotEmpty();
        }

        @Test
        void should_return_502_with_wrong_credentials() {
            givenClusterWithConfig(
                Map.of(
                    "bootstrapServers",
                    saslBootstrapServers(),
                    "security",
                    Map.of(
                        "protocol",
                        "SASL_PLAINTEXT",
                        "sasl",
                        Map.of("mechanism", Map.of("type", "PLAIN", "username", SASL_USERNAME, "password", "wrong-password"))
                    )
                )
            );

            var request = new DescribeClusterRequest().clusterId(CLUSTER_ID);

            var response = resource.describeCluster(request);

            assertThat(response.getStatus()).isEqualTo(502);
            var error = (KafkaExplorerError) response.getEntity();
            assertThat(error.getTechnicalCode()).isEqualTo("AUTHENTICATION_FAILED");
        }
    }

    @Nested
    class Ssl {

        private static final String TRUSTSTORE_PATH = Path.of("src/test/resources/docker/ssl/broker.truststore.p12")
            .toAbsolutePath()
            .toString();
        private static final String TRUSTSTORE_PASSWORD = "test1234";

        @Test
        void should_return_200_with_ssl_truststore_path() {
            var sslConfig = new HashMap<String, Object>();
            sslConfig.put("trustAll", false);
            sslConfig.put("hostnameVerifier", false);
            sslConfig.put("trustStore", Map.of("type", "PKCS12", "path", TRUSTSTORE_PATH, "password", TRUSTSTORE_PASSWORD));

            givenClusterWithConfig(
                Map.of("bootstrapServers", sslBootstrapServers(), "security", Map.of("protocol", "SSL", "ssl", sslConfig))
            );

            var request = new DescribeClusterRequest().clusterId(CLUSTER_ID);

            var response = resource.describeCluster(request);

            assertThat(response.getStatus()).isEqualTo(200);
            var body = (DescribeClusterResponse) response.getEntity();
            assertThat(body.getClusterId()).isNotBlank();
            assertThat(body.getNodes()).isNotEmpty();
        }

        @Test
        void should_return_200_with_ssl_truststore_content() throws IOException {
            var truststoreBytes = Files.readAllBytes(Path.of(TRUSTSTORE_PATH));
            var base64Content = Base64.getEncoder().encodeToString(truststoreBytes);

            var sslConfig = new HashMap<String, Object>();
            sslConfig.put("trustAll", false);
            sslConfig.put("hostnameVerifier", false);
            sslConfig.put("trustStore", Map.of("type", "PKCS12", "content", base64Content, "password", TRUSTSTORE_PASSWORD));

            givenClusterWithConfig(
                Map.of("bootstrapServers", sslBootstrapServers(), "security", Map.of("protocol", "SSL", "ssl", sslConfig))
            );

            var request = new DescribeClusterRequest().clusterId(CLUSTER_ID);

            var response = resource.describeCluster(request);

            assertThat(response.getStatus()).isEqualTo(200);
            var body = (DescribeClusterResponse) response.getEntity();
            assertThat(body.getClusterId()).isNotBlank();
            assertThat(body.getNodes()).isNotEmpty();
        }
    }

    @Nested
    class WrongProtocol {

        @Test
        void should_return_502_when_using_sasl_on_plaintext_listener() {
            givenClusterWithConfig(
                Map.of(
                    "bootstrapServers",
                    plaintextBootstrapServers(),
                    "security",
                    Map.of(
                        "protocol",
                        "SASL_PLAINTEXT",
                        "sasl",
                        Map.of("mechanism", Map.of("type", "PLAIN", "username", "user", "password", "password"))
                    )
                )
            );

            var request = new DescribeClusterRequest().clusterId(CLUSTER_ID);

            var response = resource.describeCluster(request);

            assertThat(response.getStatus()).isEqualTo(502);
        }

        @Test
        void should_return_502_when_using_ssl_on_plaintext_listener() {
            givenClusterWithConfig(Map.of("bootstrapServers", plaintextBootstrapServers(), "security", Map.of("protocol", "SSL")));

            var request = new DescribeClusterRequest().clusterId(CLUSTER_ID);

            var response = resource.describeCluster(request);

            assertThat(response.getStatus()).isEqualTo(502);
        }
    }
}
