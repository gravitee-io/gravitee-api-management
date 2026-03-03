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

import io.gravitee.rest.api.kafkaexplorer.rest.model.KafkaExplorerError;
import io.gravitee.rest.api.kafkaexplorer.rest.model.ListTopicsRequest;
import io.gravitee.rest.api.kafkaexplorer.rest.model.ListTopicsResponse;
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
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class KafkaExplorerResourceIntegrationTest_ListTopics extends AbstractKafkaExplorerResourceIntegrationTest {

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
