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
package io.gravitee.gateway.services.sync.process.repository.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.cluster.KafkaClusterReactableCluster;
import io.gravitee.definition.model.cluster.ReactableCluster;
import io.gravitee.repository.management.model.Event;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ClusterMapperTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ClusterMapper cut;

    @BeforeEach
    void setUp() {
        cut = new ClusterMapper(objectMapper);
    }

    @Test
    void should_extract_cluster_id_from_event() {
        Event event = new Event();
        event.setId("event-id");
        event.setProperties(Map.of(io.gravitee.repository.management.model.Event.EventProperties.CLUSTER_ID.getValue(), "my-cluster"));

        cut.toId(event).test().assertValue("my-cluster");
    }

    @Test
    void should_return_null_when_no_cluster_id_property() {
        Event event = new Event();
        event.setId("event-id");
        event.setProperties(Map.of());

        cut.toId(event).test().assertComplete().assertNoValues();
    }

    @Test
    void should_map_kafka_cluster_event_to_reactable() {
        Event event = new Event();
        event.setId("event-id");
        event.setPayload(
            """
            {
                "id": "cluster-id",
                "crossId": "my-kafka-cluster",
                "name": "My Kafka Cluster",
                "type": "KAFKA_CLUSTER",
                "environmentId": "env-1",
                "organizationId": "org-1",
                "deployedAt": 1698000,
                "configuration": {
                    "connections": [
                        {
                            "crossId": "primary",
                            "name": "Primary",
                            "bootstrapServers": "kafka:9092",
                            "security": { "protocol": "PLAINTEXT" }
                        }
                    ]
                }
            }
            """
        );

        ReactableCluster result = cut.to(event).blockingGet();

        assertThat(result).isInstanceOf(KafkaClusterReactableCluster.class);
        assertThat(result.getCrossId()).isEqualTo("my-kafka-cluster");
        assertThat(result.getName()).isEqualTo("My Kafka Cluster");
        assertThat(result.getEnvironmentId()).isEqualTo("env-1");

        KafkaClusterReactableCluster kafkaCluster = (KafkaClusterReactableCluster) result;
        assertThat(kafkaCluster.getConnections()).hasSize(1);
        assertThat(kafkaCluster.getConnections().get(0).getCrossId()).isEqualTo("primary");
        assertThat(kafkaCluster.getConnections().get(0).getBootstrapServers()).isEqualTo("kafka:9092");
    }

    @Test
    void should_return_null_for_kafka_connection_type() {
        Event event = new Event();
        event.setId("event-id");
        event.setPayload(
            """
            {
                "id": "conn-id",
                "crossId": "my-connection",
                "name": "My Connection",
                "type": "KAFKA_CLUSTER_CONNECTION",
                "environmentId": "env-1",
                "configuration": {}
            }
            """
        );

        cut.to(event).test().assertComplete().assertNoValues();
    }

    @Test
    void should_return_null_on_invalid_payload() {
        Event event = new Event();
        event.setId("event-id");
        event.setPayload("invalid json");

        cut.to(event).test().assertComplete().assertNoValues();
    }
}
