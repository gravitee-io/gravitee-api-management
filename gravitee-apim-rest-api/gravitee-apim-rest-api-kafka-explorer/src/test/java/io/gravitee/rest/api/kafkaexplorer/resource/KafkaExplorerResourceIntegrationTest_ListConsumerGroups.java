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
import io.gravitee.rest.api.kafkaexplorer.rest.model.ListConsumerGroupsRequest;
import io.gravitee.rest.api.kafkaexplorer.rest.model.ListConsumerGroupsResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class KafkaExplorerResourceIntegrationTest_ListConsumerGroups extends AbstractKafkaExplorerResourceIntegrationTest {

    private static final String CONSUMER_GROUP_ID = "list-cg-integration-test-group";

    @BeforeAll
    static void createConsumerGroup() {
        var consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, plaintextBootstrapServers());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, CONSUMER_GROUP_ID);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        try (var consumer = new KafkaConsumer<String, String>(consumerProps)) {
            consumer.subscribe(List.of("__consumer_offsets"));
            consumer.poll(Duration.ofSeconds(5));
            consumer.commitSync();
        }
    }

    @Test
    void should_return_200_with_consumer_groups() {
        givenClusterWithConfig(Map.of("bootstrapServers", plaintextBootstrapServers(), "security", Map.of("protocol", "PLAINTEXT")));

        var request = new ListConsumerGroupsRequest().clusterId(CLUSTER_ID);

        var response = resource.listConsumerGroups(request, 1, 10);

        assertThat(response.getStatus()).isEqualTo(200);
        var body = (ListConsumerGroupsResponse) response.getEntity();
        assertThat(body.getData()).isNotNull();
        assertThat(body.getData()).isNotEmpty();
        assertThat(body.getPagination()).isNotNull();
        assertThat(body.getPagination().getPage()).isEqualTo(1);
        assertThat(body.getPagination().getPerPage()).isEqualTo(10);
        assertThat(body.getPagination().getTotalCount()).isGreaterThan(0);
    }

    @Test
    void should_filter_consumer_groups_by_name() {
        givenClusterWithConfig(Map.of("bootstrapServers", plaintextBootstrapServers(), "security", Map.of("protocol", "PLAINTEXT")));

        var request = new ListConsumerGroupsRequest().clusterId(CLUSTER_ID).nameFilter(CONSUMER_GROUP_ID);

        var response = resource.listConsumerGroups(request, 1, 10);

        assertThat(response.getStatus()).isEqualTo(200);
        var body = (ListConsumerGroupsResponse) response.getEntity();
        assertThat(body.getData()).isNotEmpty();
        assertThat(body.getData()).allMatch(g -> g.getGroupId().contains(CONSUMER_GROUP_ID));
    }

    @Test
    void should_return_400_when_request_is_null() {
        var response = resource.listConsumerGroups(null, 1, 10);

        assertThat(response.getStatus()).isEqualTo(400);
        var error = (KafkaExplorerError) response.getEntity();
        assertThat(error.getTechnicalCode()).isEqualTo("INVALID_PARAMETERS");
    }

    @Test
    void should_return_400_when_page_is_less_than_1() {
        givenClusterWithConfig(Map.of("bootstrapServers", plaintextBootstrapServers(), "security", Map.of("protocol", "PLAINTEXT")));

        var request = new ListConsumerGroupsRequest().clusterId(CLUSTER_ID);

        var response = resource.listConsumerGroups(request, 0, 10);

        assertThat(response.getStatus()).isEqualTo(400);
        var error = (KafkaExplorerError) response.getEntity();
        assertThat(error.getTechnicalCode()).isEqualTo("INVALID_PARAMETERS");
    }

    @Test
    void should_return_502_when_broker_unreachable() {
        givenClusterWithConfig(Map.of("bootstrapServers", "localhost:19092", "security", Map.of("protocol", "PLAINTEXT")));

        var request = new ListConsumerGroupsRequest().clusterId(CLUSTER_ID);

        var response = resource.listConsumerGroups(request, 1, 10);

        assertThat(response.getStatus()).isEqualTo(502);
        var error = (KafkaExplorerError) response.getEntity();
        assertThat(error.getTechnicalCode()).isEqualTo("CONNECTION_FAILED");
    }
}
