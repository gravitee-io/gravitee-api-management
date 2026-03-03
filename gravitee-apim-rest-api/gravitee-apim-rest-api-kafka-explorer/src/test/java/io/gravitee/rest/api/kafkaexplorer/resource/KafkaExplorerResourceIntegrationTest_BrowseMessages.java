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

import io.gravitee.rest.api.kafkaexplorer.rest.model.BrowseMessagesRequest;
import io.gravitee.rest.api.kafkaexplorer.rest.model.BrowseMessagesResponse;
import io.gravitee.rest.api.kafkaexplorer.rest.model.KafkaExplorerError;
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
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class KafkaExplorerResourceIntegrationTest_BrowseMessages extends AbstractKafkaExplorerResourceIntegrationTest {

    private static final String BROWSE_TOPIC = "browse-messages-test";

    @BeforeAll
    static void createTestTopicAndProduceMessages() throws Exception {
        var adminProps = new Properties();
        adminProps.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, plaintextBootstrapServers());
        try (var admin = AdminClient.create(adminProps)) {
            admin.createTopics(List.of(new NewTopic(BROWSE_TOPIC, 2, (short) 1))).all().get(10, TimeUnit.SECONDS);
        }

        var producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, plaintextBootstrapServers());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        try (var producer = new KafkaProducer<String, String>(producerProps)) {
            for (int i = 0; i < 5; i++) {
                var record = new ProducerRecord<>(BROWSE_TOPIC, i % 2, "key-" + i, "{\"index\":" + i + "}");
                record.headers().add(new RecordHeader("test-header", ("value-" + i).getBytes()));
                producer.send(record).get(5, TimeUnit.SECONDS);
            }
        }
    }

    @Test
    void should_return_200_with_messages_using_newest_mode() {
        givenClusterWithConfig(Map.of("bootstrapServers", plaintextBootstrapServers(), "security", Map.of("protocol", "PLAINTEXT")));

        var request = new BrowseMessagesRequest().clusterId(CLUSTER_ID).topicName(BROWSE_TOPIC);

        var response = resource.browseMessages(request, 50);

        assertThat(response.getStatus()).isEqualTo(200);
        var body = (BrowseMessagesResponse) response.getEntity();
        assertThat(body.getData()).isNotEmpty();
        assertThat(body.getTotalFetched()).isGreaterThanOrEqualTo(body.getData().size());
        body
            .getData()
            .forEach(msg -> {
                assertThat(msg.getPartition()).isGreaterThanOrEqualTo(0);
                assertThat(msg.getOffset()).isGreaterThanOrEqualTo(0);
                assertThat(msg.getTimestamp()).isGreaterThan(0);
                assertThat(msg.getKey()).startsWith("key-");
                assertThat(msg.getValue()).contains("index");
                assertThat(msg.getHeaders()).isNotEmpty();
            });
    }

    @Test
    void should_return_200_with_messages_using_oldest_mode() {
        givenClusterWithConfig(Map.of("bootstrapServers", plaintextBootstrapServers(), "security", Map.of("protocol", "PLAINTEXT")));

        var request = new BrowseMessagesRequest()
            .clusterId(CLUSTER_ID)
            .topicName(BROWSE_TOPIC)
            .offsetMode(BrowseMessagesRequest.OffsetModeEnum.OLDEST);

        var response = resource.browseMessages(request, 50);

        assertThat(response.getStatus()).isEqualTo(200);
        var body = (BrowseMessagesResponse) response.getEntity();
        assertThat(body.getData()).hasSize(5);
    }

    @Test
    void should_return_200_with_messages_filtered_by_partition() {
        givenClusterWithConfig(Map.of("bootstrapServers", plaintextBootstrapServers(), "security", Map.of("protocol", "PLAINTEXT")));

        var request = new BrowseMessagesRequest()
            .clusterId(CLUSTER_ID)
            .topicName(BROWSE_TOPIC)
            .partition(0)
            .offsetMode(BrowseMessagesRequest.OffsetModeEnum.OLDEST);

        var response = resource.browseMessages(request, 50);

        assertThat(response.getStatus()).isEqualTo(200);
        var body = (BrowseMessagesResponse) response.getEntity();
        assertThat(body.getData()).isNotEmpty();
        body.getData().forEach(msg -> assertThat(msg.getPartition()).isEqualTo(0));
    }

    @Test
    void should_return_200_with_messages_filtered_by_key() {
        givenClusterWithConfig(Map.of("bootstrapServers", plaintextBootstrapServers(), "security", Map.of("protocol", "PLAINTEXT")));

        var request = new BrowseMessagesRequest()
            .clusterId(CLUSTER_ID)
            .topicName(BROWSE_TOPIC)
            .keyFilter("key-0")
            .offsetMode(BrowseMessagesRequest.OffsetModeEnum.OLDEST);

        var response = resource.browseMessages(request, 50);

        assertThat(response.getStatus()).isEqualTo(200);
        var body = (BrowseMessagesResponse) response.getEntity();
        assertThat(body.getData()).isNotEmpty();
        body.getData().forEach(msg -> assertThat(msg.getKey()).containsIgnoringCase("key-0"));
    }

    @Test
    void should_return_200_with_messages_filtered_by_value() {
        givenClusterWithConfig(Map.of("bootstrapServers", plaintextBootstrapServers(), "security", Map.of("protocol", "PLAINTEXT")));

        var request = new BrowseMessagesRequest()
            .clusterId(CLUSTER_ID)
            .topicName(BROWSE_TOPIC)
            .valueFilter("index\":0")
            .offsetMode(BrowseMessagesRequest.OffsetModeEnum.EARLIEST);

        var response = resource.browseMessages(request, 50);

        assertThat(response.getStatus()).isEqualTo(200);
        var body = (BrowseMessagesResponse) response.getEntity();
        assertThat(body.getData()).isNotEmpty();
        body.getData().forEach(msg -> assertThat(msg.getValue()).containsIgnoringCase("index\":0"));
    }

    @Test
    void should_return_200_with_limited_messages() {
        givenClusterWithConfig(Map.of("bootstrapServers", plaintextBootstrapServers(), "security", Map.of("protocol", "PLAINTEXT")));

        var request = new BrowseMessagesRequest()
            .clusterId(CLUSTER_ID)
            .topicName(BROWSE_TOPIC)
            .offsetMode(BrowseMessagesRequest.OffsetModeEnum.OLDEST);

        var response = resource.browseMessages(request, 2);

        assertThat(response.getStatus()).isEqualTo(200);
        var body = (BrowseMessagesResponse) response.getEntity();
        assertThat(body.getData()).hasSizeLessThanOrEqualTo(2);
    }

    @Test
    void should_return_400_when_cluster_id_missing() {
        var request = new BrowseMessagesRequest().topicName(BROWSE_TOPIC);

        var response = resource.browseMessages(request, 50);

        assertThat(response.getStatus()).isEqualTo(400);
        var error = (KafkaExplorerError) response.getEntity();
        assertThat(error.getTechnicalCode()).isEqualTo("INVALID_PARAMETERS");
    }

    @Test
    void should_return_400_when_topic_name_missing() {
        var request = new BrowseMessagesRequest().clusterId(CLUSTER_ID);

        var response = resource.browseMessages(request, 50);

        assertThat(response.getStatus()).isEqualTo(400);
        var error = (KafkaExplorerError) response.getEntity();
        assertThat(error.getTechnicalCode()).isEqualTo("INVALID_PARAMETERS");
    }

    @Test
    void should_return_400_when_limit_out_of_range() {
        var request = new BrowseMessagesRequest().clusterId(CLUSTER_ID).topicName(BROWSE_TOPIC);

        var response = resource.browseMessages(request, 0);

        assertThat(response.getStatus()).isEqualTo(400);
        var error = (KafkaExplorerError) response.getEntity();
        assertThat(error.getMessage()).contains("limit");
    }

    @Test
    void should_return_502_when_broker_unreachable() {
        givenClusterWithConfig(Map.of("bootstrapServers", "localhost:19092", "security", Map.of("protocol", "PLAINTEXT")));

        var request = new BrowseMessagesRequest().clusterId(CLUSTER_ID).topicName(BROWSE_TOPIC);

        var response = resource.browseMessages(request, 50);

        assertThat(response.getStatus()).isEqualTo(502);
        var error = (KafkaExplorerError) response.getEntity();
        assertThat(error.getTechnicalCode()).isEqualTo("CONNECTION_FAILED");
    }
}
