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

import com.fasterxml.jackson.databind.ObjectMapper;
import inmemory.ClusterCrudServiceInMemory;
import io.gravitee.apim.core.cluster.model.Cluster;
import io.gravitee.rest.api.kafkaexplorer.domain.use_case.DescribeKafkaClusterUseCase;
import io.gravitee.rest.api.kafkaexplorer.domain.use_case.ListTopicsUseCase;
import io.gravitee.rest.api.kafkaexplorer.infrastructure.domain_service.KafkaClusterDomainServiceImpl;
import io.gravitee.rest.api.kafkaexplorer.rest.model.DescribeClusterRequest;
import io.gravitee.rest.api.kafkaexplorer.rest.model.DescribeClusterResponse;
import io.gravitee.rest.api.kafkaexplorer.rest.model.KafkaExplorerError;
import io.gravitee.rest.api.kafkaexplorer.rest.model.ListTopicsRequest;
import io.gravitee.rest.api.kafkaexplorer.rest.model.ListTopicsResponse;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class KafkaExplorerResourceIntegrationTest {

    private static final String SASL_USERNAME = "admin";
    private static final String SASL_PASSWORD = "admin-secret";
    private static final String BROKER_SERVICE = "broker";
    private static final int PLAINTEXT_PORT = 9092;
    private static final int SSL_PORT = 9094;
    private static final int SASL_PLAINTEXT_PORT = 9095;
    private static final String CLUSTER_ID = "test-cluster";
    private static final String ENVIRONMENT_ID = "test-env";

    @Container
    static final DockerComposeContainer<?> kafka = new DockerComposeContainer<>(new File("src/test/resources/docker/docker-compose.yml"))
        .withExposedService(BROKER_SERVICE, PLAINTEXT_PORT, Wait.forHealthcheck().withStartupTimeout(Duration.ofSeconds(60)))
        .withExposedService(BROKER_SERVICE, SSL_PORT)
        .withExposedService(BROKER_SERVICE, SASL_PLAINTEXT_PORT);

    private static KafkaExplorerResource resource;
    private static ClusterCrudServiceInMemory clusterCrudService;

    @BeforeAll
    static void setUp() throws Exception {
        GraviteeContext.setCurrentEnvironment(ENVIRONMENT_ID);

        resource = new KafkaExplorerResource();
        clusterCrudService = new ClusterCrudServiceInMemory();
        var clusterService = new KafkaClusterDomainServiceImpl();
        var objectMapper = new ObjectMapper();
        var describeUseCase = new DescribeKafkaClusterUseCase(clusterCrudService, clusterService, objectMapper);
        var listTopicsUseCase = new ListTopicsUseCase(clusterCrudService, clusterService, objectMapper);
        injectField(resource, "describeKafkaClusterUseCase", describeUseCase);
        injectField(resource, "listTopicsUseCase", listTopicsUseCase);
    }

    @BeforeEach
    void resetClusterCrudService() {
        clusterCrudService.reset();
    }

    private static String plaintextBootstrapServers() {
        return kafka.getServiceHost(BROKER_SERVICE, PLAINTEXT_PORT) + ":" + kafka.getServicePort(BROKER_SERVICE, PLAINTEXT_PORT);
    }

    private static String sslBootstrapServers() {
        return kafka.getServiceHost(BROKER_SERVICE, SSL_PORT) + ":" + kafka.getServicePort(BROKER_SERVICE, SSL_PORT);
    }

    private static String saslBootstrapServers() {
        return kafka.getServiceHost(BROKER_SERVICE, SASL_PLAINTEXT_PORT) + ":" + kafka.getServicePort(BROKER_SERVICE, SASL_PLAINTEXT_PORT);
    }

    private static void givenClusterWithConfig(Map<String, Object> config) {
        clusterCrudService.create(Cluster.builder().id(CLUSTER_ID).environmentId(ENVIRONMENT_ID).configuration(config).build());
    }

    @Nested
    class Plaintext {

        @Test
        void should_return_200_with_cluster_info() {
            givenClusterWithConfig(Map.of("bootstrapServers", plaintextBootstrapServers(), "security", Map.of("protocol", "PLAINTEXT")));

            var request = new DescribeClusterRequest().clusterId(CLUSTER_ID);

            var response = resource.describeCluster(request);

            assertThat(response.getStatus()).isEqualTo(200);
            var body = (DescribeClusterResponse) response.getEntity();
            assertThat(body.getClusterId()).isNotBlank();
            assertThat(body.getController()).isNotNull();
            assertThat(body.getNodes()).isNotEmpty();
            assertThat(body.getTotalTopics()).isGreaterThanOrEqualTo(0);
            assertThat(body.getTotalPartitions()).isGreaterThanOrEqualTo(0);

            var firstNode = body.getNodes().get(0);
            assertThat(firstNode.getHost()).isNotBlank();
            assertThat(firstNode.getPort()).isGreaterThan(0);
            assertThat(firstNode.getLeaderPartitions()).isGreaterThanOrEqualTo(0);
            assertThat(firstNode.getReplicaPartitions()).isGreaterThanOrEqualTo(0);
        }
    }

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
            assertThat(body.getTotalTopics()).isGreaterThanOrEqualTo(0);
            assertThat(body.getTotalPartitions()).isGreaterThanOrEqualTo(0);
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

    @Nested
    class UnreachableBroker {

        @Test
        void should_return_502_with_connection_failed() {
            givenClusterWithConfig(Map.of("bootstrapServers", "localhost:19092", "security", Map.of("protocol", "PLAINTEXT")));

            var request = new DescribeClusterRequest().clusterId(CLUSTER_ID);

            var response = resource.describeCluster(request);

            assertThat(response.getStatus()).isEqualTo(502);
            var error = (KafkaExplorerError) response.getEntity();
            assertThat(error.getTechnicalCode()).isEqualTo("CONNECTION_FAILED");
        }
    }

    @Nested
    class InvalidRequest {

        @Test
        void should_return_400_when_request_is_null() {
            var response = resource.describeCluster(null);

            assertThat(response.getStatus()).isEqualTo(400);
            var error = (KafkaExplorerError) response.getEntity();
            assertThat(error.getTechnicalCode()).isEqualTo("INVALID_PARAMETERS");
        }

        @Test
        void should_return_400_when_cluster_id_is_empty() {
            var request = new DescribeClusterRequest().clusterId("");

            var response = resource.describeCluster(request);

            assertThat(response.getStatus()).isEqualTo(400);
        }
    }

    @Nested
    class ListTopics {

        @Test
        void should_return_200_with_topics() {
            givenClusterWithConfig(Map.of("bootstrapServers", plaintextBootstrapServers(), "security", Map.of("protocol", "PLAINTEXT")));

            var request = new ListTopicsRequest().clusterId(CLUSTER_ID);

            var response = resource.listTopics(request, 1, 10);

            assertThat(response.getStatus()).isEqualTo(200);
            var body = (ListTopicsResponse) response.getEntity();
            assertThat(body.getData()).isNotNull();
            assertThat(body.getData()).isNotEmpty();
            assertThat(body.getData()).anyMatch(t -> t.getInternal() != null && t.getInternal());
            assertThat(body.getPagination()).isNotNull();
            assertThat(body.getPagination().getTotalCount()).isGreaterThan(0);
            assertThat(body.getPagination().getPage()).isEqualTo(1);
            assertThat(body.getPagination().getPerPage()).isEqualTo(10);
            assertThat(body.getPagination().getPageItemsCount()).isEqualTo(body.getData().size());
            assertThat(body.getPagination().getPageCount()).isGreaterThanOrEqualTo(1);
        }

        @Test
        void should_return_topics_with_size_and_message_count() {
            givenClusterWithConfig(Map.of("bootstrapServers", plaintextBootstrapServers(), "security", Map.of("protocol", "PLAINTEXT")));

            var request = new ListTopicsRequest().clusterId(CLUSTER_ID);

            var response = resource.listTopics(request, 1, 10);

            assertThat(response.getStatus()).isEqualTo(200);
            var body = (ListTopicsResponse) response.getEntity();
            assertThat(body.getData()).isNotEmpty();
            // Internal topics should have non-null size and messageCount
            var internalTopic = body
                .getData()
                .stream()
                .filter(t -> Boolean.TRUE.equals(t.getInternal()))
                .findFirst();
            assertThat(internalTopic).isPresent();
            assertThat(internalTopic.get().getSize()).isNotNull();
            assertThat(internalTopic.get().getMessageCount()).isNotNull();
        }

        @Test
        void should_return_400_when_request_is_null() {
            var response = resource.listTopics(null, 1, 10);

            assertThat(response.getStatus()).isEqualTo(400);
            var error = (KafkaExplorerError) response.getEntity();
            assertThat(error.getTechnicalCode()).isEqualTo("INVALID_PARAMETERS");
        }

        @Test
        void should_return_400_when_page_is_less_than_1() {
            givenClusterWithConfig(Map.of("bootstrapServers", plaintextBootstrapServers(), "security", Map.of("protocol", "PLAINTEXT")));

            var request = new ListTopicsRequest().clusterId(CLUSTER_ID);

            var response = resource.listTopics(request, 0, 10);

            assertThat(response.getStatus()).isEqualTo(400);
            var error = (KafkaExplorerError) response.getEntity();
            assertThat(error.getTechnicalCode()).isEqualTo("INVALID_PARAMETERS");
        }

        @Test
        void should_return_502_when_broker_unreachable() {
            givenClusterWithConfig(Map.of("bootstrapServers", "localhost:19092", "security", Map.of("protocol", "PLAINTEXT")));

            var request = new ListTopicsRequest().clusterId(CLUSTER_ID);

            var response = resource.listTopics(request, 1, 10);

            assertThat(response.getStatus()).isEqualTo(502);
            var error = (KafkaExplorerError) response.getEntity();
            assertThat(error.getTechnicalCode()).isEqualTo("CONNECTION_FAILED");
        }
    }

    private static void injectField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
