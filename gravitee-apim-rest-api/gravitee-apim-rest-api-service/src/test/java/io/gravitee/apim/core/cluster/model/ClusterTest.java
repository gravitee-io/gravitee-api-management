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
package io.gravitee.apim.core.cluster.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class ClusterTest {

    @Test
    public void update() {
        String id = "cluster-1";
        Instant createdAt = Instant.parse("2025-08-04T10:15:30.00Z");
        String name = "Cluster 1";
        String description = "Cluster 1 description";
        String orgId = "org-1";
        String envId = "env-1";
        Object configuration = Map.of("bootstrapServers", "localhost:9092", "security", "security-data");
        Cluster cluster = Cluster.builder()
            .id(id)
            .createdAt(createdAt)
            .name(name)
            .description(description)
            .organizationId(orgId)
            .environmentId(envId)
            .configuration(configuration)
            .build();

        assertAll(
            () -> assertThat(cluster.getId()).isEqualTo(id),
            () -> assertThat(cluster.getCreatedAt()).isEqualTo(createdAt),
            () -> assertThat(cluster.getUpdatedAt()).isNull(),
            () -> assertThat(cluster.getName()).isEqualTo(name),
            () -> assertThat(cluster.getDescription()).isEqualTo(description),
            () -> assertThat(cluster.getOrganizationId()).isEqualTo(orgId),
            () -> assertThat(cluster.getEnvironmentId()).isEqualTo(envId),
            () -> assertThat(cluster.getConfiguration()).isEqualTo(configuration)
        );

        Object newConfiguration = Map.of("bootstrapServers", "localhost:9093", "security", "security-data-2");
        UpdateCluster updateCluster = UpdateCluster.builder().configuration(newConfiguration).build();
        cluster.update(updateCluster);

        assertAll(
            () -> assertThat(cluster.getId()).isEqualTo(id),
            () -> assertThat(cluster.getCreatedAt()).isEqualTo(createdAt),
            () -> assertThat(cluster.getUpdatedAt()).isAfter(Instant.now().minus(30, ChronoUnit.SECONDS)),
            () -> assertThat(cluster.getName()).isEqualTo(name),
            () -> assertThat(cluster.getDescription()).isEqualTo(description),
            () -> assertThat(cluster.getOrganizationId()).isEqualTo(orgId),
            () -> assertThat(cluster.getEnvironmentId()).isEqualTo(envId),
            () -> assertThat(cluster.getConfiguration()).isEqualTo(newConfiguration)
        );
    }

    @Test
    public void should_deserialize_kafka_cluster_configuration() {
        ObjectMapper objectMapper = new ObjectMapper();
        Object configuration = Map.of(
            "connections",
            List.of(
                Map.of("name", "primary", "bootstrapServers", "kafka1:9092", "security", Map.of("protocol", "PLAINTEXT")),
                Map.of("name", "secondary", "bootstrapServers", "kafka2:9092", "security", Map.of("protocol", "SSL"))
            )
        );
        Cluster cluster = Cluster.builder().type(ClusterType.KAFKA_CLUSTER).configuration(configuration).build();

        KafkaClusterConfiguration kafkaConfig = cluster.getKafkaClusterConfiguration(objectMapper);

        assertThat(kafkaConfig.connections()).hasSize(2);
        assertThat(kafkaConfig.connections().get(0).name()).isEqualTo("primary");
        assertThat(kafkaConfig.connections().get(0).bootstrapServers()).isEqualTo("kafka1:9092");
        assertThat(kafkaConfig.connections().get(1).name()).isEqualTo("secondary");
        assertThat(kafkaConfig.connections().get(1).bootstrapServers()).isEqualTo("kafka2:9092");
    }

    @Test
    public void should_deserialize_kafka_cluster_connection_configuration() {
        ObjectMapper objectMapper = new ObjectMapper();
        Object configuration = Map.of("bootstrapServers", "localhost:9092", "security", Map.of("protocol", "PLAINTEXT"));
        Cluster cluster = Cluster.builder().type(ClusterType.KAFKA_CLUSTER_CONNECTION).configuration(configuration).build();

        KafkaClusterConnectionConfiguration connectionConfig = cluster.getKafkaClusterConnectionConfiguration(objectMapper);

        assertThat(connectionConfig.bootstrapServers()).isEqualTo("localhost:9092");
    }
}
