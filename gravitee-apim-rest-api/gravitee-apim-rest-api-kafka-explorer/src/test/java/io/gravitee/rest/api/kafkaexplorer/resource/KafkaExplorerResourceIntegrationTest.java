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

import io.gravitee.rest.api.kafkaexplorer.rest.model.DescribeBrokerRequest;
import io.gravitee.rest.api.kafkaexplorer.rest.model.DescribeBrokerResponse;
import io.gravitee.rest.api.kafkaexplorer.rest.model.DescribeClusterRequest;
import io.gravitee.rest.api.kafkaexplorer.rest.model.DescribeClusterResponse;
import io.gravitee.rest.api.kafkaexplorer.rest.model.DescribeTopicRequest;
import io.gravitee.rest.api.kafkaexplorer.rest.model.DescribeTopicResponse;
import io.gravitee.rest.api.kafkaexplorer.rest.model.KafkaExplorerError;
import io.gravitee.rest.api.kafkaexplorer.rest.model.ListTopicsRequest;
import io.gravitee.rest.api.kafkaexplorer.rest.model.ListTopicsResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class KafkaExplorerResourceIntegrationTest extends AbstractKafkaExplorerResourceIntegrationTest {

    private static final String SASL_USERNAME = "admin";
    private static final String SASL_PASSWORD = "admin-secret";

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

        private static final String TEST_TOPIC = "test-topic-for-integration";
        private static final int TEST_MESSAGES_COUNT = 5;

        @BeforeAll
        static void createTestTopic() throws Exception {
            var props = new Properties();
            props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, plaintextBootstrapServers());
            try (var admin = AdminClient.create(props)) {
                admin.createTopics(List.of(new NewTopic(TEST_TOPIC, 1, (short) 1))).all().get(10, TimeUnit.SECONDS);
            }

            var producerProps = new Properties();
            producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, plaintextBootstrapServers());
            producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
            producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
            try (var producer = new KafkaProducer<String, String>(producerProps)) {
                for (int i = 0; i < TEST_MESSAGES_COUNT; i++) {
                    producer.send(new ProducerRecord<>(TEST_TOPIC, "key-" + i, "value-" + i)).get();
                }
            }
        }

        @Test
        void should_return_200_with_topics() {
            givenClusterWithConfig(Map.of("bootstrapServers", plaintextBootstrapServers(), "security", Map.of("protocol", "PLAINTEXT")));

            var request = new ListTopicsRequest().clusterId(CLUSTER_ID);

            var response = resource.listTopics(request, 1, 10);

            assertThat(response.getStatus()).isEqualTo(200);
            var body = (ListTopicsResponse) response.getEntity();
            assertThat(body.getData()).isNotNull();
            assertThat(body.getData()).isNotEmpty();
            assertThat(body.getData()).anyMatch(t -> TEST_TOPIC.equals(t.getName()));
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

            var request = new ListTopicsRequest().clusterId(CLUSTER_ID).nameFilter(TEST_TOPIC);

            var response = resource.listTopics(request, 1, 10);

            assertThat(response.getStatus()).isEqualTo(200);
            var body = (ListTopicsResponse) response.getEntity();
            assertThat(body.getData()).isNotEmpty();
            var testTopic = body
                .getData()
                .stream()
                .filter(t -> TEST_TOPIC.equals(t.getName()))
                .findFirst();
            assertThat(testTopic).isPresent();
            assertThat(testTopic.get().getSize()).isNotNull().isGreaterThan(0);
            assertThat(testTopic.get().getMessageCount()).isNotNull().isEqualTo((long) TEST_MESSAGES_COUNT);
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

    @Nested
    class DescribeTopic {

        private static final String DESCRIBE_TOPIC = "describe-topic-test";

        @BeforeAll
        static void createTestTopic() throws Exception {
            var props = new Properties();
            props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, plaintextBootstrapServers());
            try (var admin = AdminClient.create(props)) {
                admin.createTopics(List.of(new NewTopic(DESCRIBE_TOPIC, 2, (short) 1))).all().get(10, TimeUnit.SECONDS);
            }
        }

        @Test
        void should_return_200_with_topic_detail() {
            givenClusterWithConfig(Map.of("bootstrapServers", plaintextBootstrapServers(), "security", Map.of("protocol", "PLAINTEXT")));

            var request = new DescribeTopicRequest().clusterId(CLUSTER_ID).topicName(DESCRIBE_TOPIC);

            var response = resource.describeTopic(request);

            assertThat(response.getStatus()).isEqualTo(200);
            var body = (DescribeTopicResponse) response.getEntity();
            assertThat(body.getName()).isEqualTo(DESCRIBE_TOPIC);
            assertThat(body.getInternal()).isFalse();
            assertThat(body.getPartitions()).hasSize(2);
            assertThat(body.getPartitions().get(0).getId()).isGreaterThanOrEqualTo(0);
            assertThat(body.getPartitions().get(0).getLeader()).isNotNull();
            assertThat(body.getPartitions().get(0).getReplicas()).isNotEmpty();
            assertThat(body.getPartitions().get(0).getIsr()).isNotEmpty();
            assertThat(body.getConfigs()).isNotEmpty();
            assertThat(body.getConfigs()).anyMatch(c -> c.getName() != null && !c.getName().isBlank());
        }

        @Test
        void should_return_400_when_topic_name_missing() {
            givenClusterWithConfig(Map.of("bootstrapServers", plaintextBootstrapServers(), "security", Map.of("protocol", "PLAINTEXT")));

            var request = new DescribeTopicRequest().clusterId(CLUSTER_ID);

            var response = resource.describeTopic(request);

            assertThat(response.getStatus()).isEqualTo(400);
            var error = (KafkaExplorerError) response.getEntity();
            assertThat(error.getTechnicalCode()).isEqualTo("INVALID_PARAMETERS");
        }

        @Test
        void should_return_502_when_broker_unreachable() {
            givenClusterWithConfig(Map.of("bootstrapServers", "localhost:19092", "security", Map.of("protocol", "PLAINTEXT")));

            var request = new DescribeTopicRequest().clusterId(CLUSTER_ID).topicName(DESCRIBE_TOPIC);

            var response = resource.describeTopic(request);

            assertThat(response.getStatus()).isEqualTo(502);
            var error = (KafkaExplorerError) response.getEntity();
            assertThat(error.getTechnicalCode()).isEqualTo("CONNECTION_FAILED");
        }
    }

    @Nested
    class DescribeBroker {

        @Test
        void should_return_200_with_broker_detail() {
            givenClusterWithConfig(Map.of("bootstrapServers", plaintextBootstrapServers(), "security", Map.of("protocol", "PLAINTEXT")));

            var request = new DescribeBrokerRequest().clusterId(CLUSTER_ID).brokerId(0);

            var response = resource.describeBroker(request);

            assertThat(response.getStatus()).isEqualTo(200);
            var body = (DescribeBrokerResponse) response.getEntity();
            assertThat(body.getId()).isEqualTo(0);
            assertThat(body.getHost()).isNotBlank();
            assertThat(body.getPort()).isGreaterThan(0);
            assertThat(body.getIsController()).isNotNull();
            assertThat(body.getLeaderPartitions()).isGreaterThanOrEqualTo(0);
            assertThat(body.getReplicaPartitions()).isGreaterThanOrEqualTo(0);
            assertThat(body.getLogDirEntries()).isNotNull();
            assertThat(body.getConfigs()).isNotNull().isNotEmpty();
        }

        @Test
        void should_return_400_when_broker_id_is_null() {
            givenClusterWithConfig(Map.of("bootstrapServers", plaintextBootstrapServers(), "security", Map.of("protocol", "PLAINTEXT")));

            var request = new DescribeBrokerRequest().clusterId(CLUSTER_ID);

            var response = resource.describeBroker(request);

            assertThat(response.getStatus()).isEqualTo(400);
            var error = (KafkaExplorerError) response.getEntity();
            assertThat(error.getTechnicalCode()).isEqualTo("INVALID_PARAMETERS");
        }

        @Test
        void should_return_400_when_broker_id_is_negative() {
            givenClusterWithConfig(Map.of("bootstrapServers", plaintextBootstrapServers(), "security", Map.of("protocol", "PLAINTEXT")));

            var request = new DescribeBrokerRequest().clusterId(CLUSTER_ID).brokerId(-1);

            var response = resource.describeBroker(request);

            assertThat(response.getStatus()).isEqualTo(400);
            var error = (KafkaExplorerError) response.getEntity();
            assertThat(error.getTechnicalCode()).isEqualTo("INVALID_PARAMETERS");
        }

        @Test
        void should_return_502_when_broker_unreachable() {
            givenClusterWithConfig(Map.of("bootstrapServers", "localhost:19092", "security", Map.of("protocol", "PLAINTEXT")));

            var request = new DescribeBrokerRequest().clusterId(CLUSTER_ID).brokerId(0);

            var response = resource.describeBroker(request);

            assertThat(response.getStatus()).isEqualTo(502);
            var error = (KafkaExplorerError) response.getEntity();
            assertThat(error.getTechnicalCode()).isEqualTo("CONNECTION_FAILED");
        }
    }
}
