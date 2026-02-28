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

import io.gravitee.rest.api.kafkaexplorer.rest.model.DescribeConsumerGroupRequest;
import io.gravitee.rest.api.kafkaexplorer.rest.model.DescribeConsumerGroupResponse;
import io.gravitee.rest.api.kafkaexplorer.rest.model.KafkaExplorerError;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class KafkaExplorerResourceIntegrationTest_DescribeConsumerGroup extends AbstractKafkaExplorerResourceIntegrationTest {

    private static final String CONSUMER_GROUP_ID = "describe-cg-integration-test-group";
    private static final String TEST_TOPIC = "describe-cg-integration-test-topic";

    @BeforeAll
    static void createTopicAndConsumerGroup() throws Exception {
        var adminProps = new Properties();
        adminProps.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, plaintextBootstrapServers());
        try (var admin = AdminClient.create(adminProps)) {
            admin.createTopics(List.of(new NewTopic(TEST_TOPIC, 2, (short) 1))).all().get(10, TimeUnit.SECONDS);
        }

        var producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, plaintextBootstrapServers());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        try (var producer = new KafkaProducer<String, String>(producerProps)) {
            for (int i = 0; i < 10; i++) {
                producer.send(new ProducerRecord<>(TEST_TOPIC, "key-" + i, "value-" + i)).get();
            }
        }

        var consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, plaintextBootstrapServers());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, CONSUMER_GROUP_ID);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        try (var consumer = new KafkaConsumer<String, String>(consumerProps)) {
            consumer.subscribe(List.of(TEST_TOPIC));
            consumer.poll(Duration.ofSeconds(5));
            consumer.commitSync();
        }
    }

    @Test
    void should_return_200_with_consumer_group_detail() {
        givenClusterWithConfig(Map.of("bootstrapServers", plaintextBootstrapServers(), "security", Map.of("protocol", "PLAINTEXT")));

        var request = new DescribeConsumerGroupRequest().clusterId(CLUSTER_ID).groupId(CONSUMER_GROUP_ID);

        var response = resource.describeConsumerGroup(request);

        assertThat(response.getStatus()).isEqualTo(200);
        var body = (DescribeConsumerGroupResponse) response.getEntity();
        assertThat(body.getGroupId()).isEqualTo(CONSUMER_GROUP_ID);
        assertThat(body.getState()).isNotBlank();
        assertThat(body.getCoordinator()).isNotNull();
        assertThat(body.getOffsets()).isNotEmpty();
        assertThat(body.getOffsets()).allMatch(o -> TEST_TOPIC.equals(o.getTopic()));
    }

    @Test
    void should_return_400_when_request_is_null() {
        var response = resource.describeConsumerGroup(null);

        assertThat(response.getStatus()).isEqualTo(400);
        var error = (KafkaExplorerError) response.getEntity();
        assertThat(error.getTechnicalCode()).isEqualTo("INVALID_PARAMETERS");
    }

    @Test
    void should_return_400_when_group_id_missing() {
        givenClusterWithConfig(Map.of("bootstrapServers", plaintextBootstrapServers(), "security", Map.of("protocol", "PLAINTEXT")));

        var request = new DescribeConsumerGroupRequest().clusterId(CLUSTER_ID);

        var response = resource.describeConsumerGroup(request);

        assertThat(response.getStatus()).isEqualTo(400);
        var error = (KafkaExplorerError) response.getEntity();
        assertThat(error.getTechnicalCode()).isEqualTo("INVALID_PARAMETERS");
    }

    @Test
    void should_return_502_when_broker_unreachable() {
        givenClusterWithConfig(Map.of("bootstrapServers", "localhost:19092", "security", Map.of("protocol", "PLAINTEXT")));

        var request = new DescribeConsumerGroupRequest().clusterId(CLUSTER_ID).groupId(CONSUMER_GROUP_ID);

        var response = resource.describeConsumerGroup(request);

        assertThat(response.getStatus()).isEqualTo(502);
        var error = (KafkaExplorerError) response.getEntity();
        assertThat(error.getTechnicalCode()).isEqualTo("CONNECTION_FAILED");
    }
}
