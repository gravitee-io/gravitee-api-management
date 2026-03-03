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

import io.gravitee.rest.api.kafkaexplorer.rest.model.DescribeTopicRequest;
import io.gravitee.rest.api.kafkaexplorer.rest.model.DescribeTopicResponse;
import io.gravitee.rest.api.kafkaexplorer.rest.model.KafkaExplorerError;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class KafkaExplorerResourceIntegrationTest_DescribeTopic extends AbstractKafkaExplorerResourceIntegrationTest {

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
        assertThat(body.getPartitions().getFirst().getId()).isGreaterThanOrEqualTo(0);
        assertThat(body.getPartitions().getFirst().getLeader()).isNotNull();
        assertThat(body.getPartitions().getFirst().getReplicas()).isNotEmpty();
        assertThat(body.getPartitions().getFirst().getIsr()).isNotEmpty();
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
