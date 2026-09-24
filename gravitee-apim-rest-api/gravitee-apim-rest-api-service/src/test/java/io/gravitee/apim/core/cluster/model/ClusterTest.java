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
import io.gravitee.definition.model.cluster.ClusterType;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
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
        Cluster cluster = Cluster.builder().type(ClusterType.KAFKA_CLUSTER_STANDALONE).configuration(configuration).build();

        KafkaClusterStandaloneConfiguration connectionConfig = cluster.getKafkaClusterStandaloneConfiguration(objectMapper);

        assertThat(connectionConfig.bootstrapServers()).isEqualTo("localhost:9092");
    }

    @Test
    public void deploy_should_set_deployed_state_and_increment_version() {
        Cluster cluster = Cluster.builder().id("cluster-1").lifecycleState(ClusterLifecycleState.UNDEPLOYED).build();

        cluster.deploy();

        assertAll(
            () -> assertThat(cluster.getLifecycleState()).isEqualTo(ClusterLifecycleState.DEPLOYED),
            () -> assertThat(cluster.getVersion()).isEqualTo(1),
            () -> assertThat(cluster.getDeployedAt()).isNotNull(),
            () -> assertThat(cluster.getUpdatedAt()).isNotNull()
        );
    }

    @Test
    public void deploy_should_increment_existing_version() {
        Cluster cluster = Cluster.builder().id("cluster-1").lifecycleState(ClusterLifecycleState.PENDING).version(2).build();

        cluster.deploy();

        assertThat(cluster.getVersion()).isEqualTo(3);
        assertThat(cluster.getLifecycleState()).isEqualTo(ClusterLifecycleState.DEPLOYED);
    }

    @Test
    public void undeploy_should_set_undeployed_state() {
        Cluster cluster = Cluster.builder().id("cluster-1").lifecycleState(ClusterLifecycleState.DEPLOYED).version(1).build();

        cluster.undeploy();

        assertAll(
            () -> assertThat(cluster.getLifecycleState()).isEqualTo(ClusterLifecycleState.UNDEPLOYED),
            () -> assertThat(cluster.getDeployedAt()).isNotNull(),
            () -> assertThat(cluster.getUpdatedAt()).isNotNull(),
            () -> assertThat(cluster.getVersion()).isEqualTo(1)
        );
    }

    @Test
    public void update_should_transition_deployed_to_pending() {
        Cluster cluster = Cluster.builder().id("cluster-1").name("Original").lifecycleState(ClusterLifecycleState.DEPLOYED).build();

        cluster.update(UpdateCluster.builder().name("Updated").build());

        assertThat(cluster.getLifecycleState()).isEqualTo(ClusterLifecycleState.PENDING);
        assertThat(cluster.getName()).isEqualTo("Updated");
    }

    @Test
    public void update_should_keep_undeployed_state() {
        Cluster cluster = Cluster.builder().id("cluster-1").name("Original").lifecycleState(ClusterLifecycleState.UNDEPLOYED).build();

        cluster.update(UpdateCluster.builder().name("Updated").build());

        assertThat(cluster.getLifecycleState()).isEqualTo(ClusterLifecycleState.UNDEPLOYED);
    }

    @Nested
    class WithoutCredentials {

        private static Map<String, Object> securedConnection(String crossId) {
            return Map.of(
                "crossId",
                crossId,
                "name",
                "conn " + crossId,
                "bootstrapServers",
                "broker:9093",
                "security",
                Map.of(
                    "protocol",
                    "SASL_SSL",
                    "sasl",
                    Map.of("mechanism", Map.of("type", "PLAIN", "username", "u", "password", "secret")),
                    "ssl",
                    Map.of("keyStore", Map.of("type", "PEM", "key", "private-key"))
                )
            );
        }

        @Test
        void should_remove_sasl_and_ssl_from_every_connection() {
            var cluster = Cluster.builder()
                .type(ClusterType.KAFKA_CLUSTER)
                .name("c")
                .configuration(Map.of("connections", List.of(securedConnection("a"), securedConnection("b")), "extra", "kept"))
                .build();

            var redacted = cluster.withoutCredentials();

            assertThat(redacted.getConfiguration()).isEqualTo(
                Map.of(
                    "connections",
                    List.of(
                        Map.of(
                            "crossId",
                            "a",
                            "name",
                            "conn a",
                            "bootstrapServers",
                            "broker:9093",
                            "security",
                            Map.of("protocol", "SASL_SSL")
                        ),
                        Map.of(
                            "crossId",
                            "b",
                            "name",
                            "conn b",
                            "bootstrapServers",
                            "broker:9093",
                            "security",
                            Map.of("protocol", "SASL_SSL")
                        )
                    ),
                    "extra",
                    "kept"
                )
            );
            assertThat(redacted.getName()).isEqualTo("c");
        }

        @Test
        void should_remove_sasl_and_ssl_from_standalone_configuration() {
            var cluster = Cluster.builder()
                .type(ClusterType.KAFKA_CLUSTER_STANDALONE)
                .configuration(
                    Map.of(
                        "bootstrapServers",
                        "broker:9093",
                        "security",
                        Map.of("protocol", "SSL", "ssl", Map.of("trustStore", Map.of("password", "secret")))
                    )
                )
                .build();

            assertThat(cluster.withoutCredentials().getConfiguration()).isEqualTo(
                Map.of("bootstrapServers", "broker:9093", "security", Map.of("protocol", "SSL"))
            );
        }

        @Test
        void should_return_null_configuration_when_configuration_is_null() {
            var cluster = Cluster.builder().type(ClusterType.KAFKA_CLUSTER).name("c").configuration(null).build();

            var redacted = cluster.withoutCredentials();

            assertThat(redacted.getConfiguration()).isNull();
        }

        @Test
        void should_leave_security_unchanged_when_it_only_has_protocol() {
            var configuration = Map.of("bootstrapServers", "broker:9092", "security", Map.of("protocol", "PLAINTEXT"));
            var cluster = Cluster.builder().type(ClusterType.KAFKA_CLUSTER_STANDALONE).name("c").configuration(configuration).build();

            var redacted = cluster.withoutCredentials();

            assertThat(redacted.getConfiguration()).isEqualTo(configuration);
        }

        @Test
        void should_not_mutate_shared_configuration_map() {
            var security = new HashMap<String, Object>();
            security.put("protocol", "SASL_SSL");
            security.put("sasl", Map.of("username", "u"));
            var connection = new HashMap<String, Object>();
            connection.put("security", security);
            var configuration = new HashMap<String, Object>();
            configuration.put("connections", List.of(connection));

            var cluster = Cluster.builder().type(ClusterType.KAFKA_CLUSTER).name("c").configuration(configuration).build();

            var redacted = cluster.withoutCredentials();

            assertThat(security).containsKey("sasl");
            assertThat(cluster.getConfiguration()).isEqualTo(configuration);
            assertThat(redacted.getConfiguration()).isNotSameAs(configuration);
        }

        @Test
        void should_leave_non_map_security_value_untouched() {
            var configuration = Map.of("bootstrapServers", "broker:9092", "security", "opaque-string");
            var cluster = Cluster.builder().type(ClusterType.KAFKA_CLUSTER_STANDALONE).name("c").configuration(configuration).build();

            var redacted = cluster.withoutCredentials();

            assertThat(redacted.getConfiguration()).isEqualTo(configuration);
        }

        @Test
        void should_leave_null_security_value_untouched() {
            var configuration = new HashMap<String, Object>();
            configuration.put("bootstrapServers", "broker:9092");
            configuration.put("security", null);
            var cluster = Cluster.builder().type(ClusterType.KAFKA_CLUSTER_STANDALONE).name("c").configuration(configuration).build();

            var redacted = cluster.withoutCredentials();

            assertThat(redacted.getConfiguration()).isEqualTo(configuration);
        }

        @Test
        void should_keep_virtual_cluster_configuration_unchanged() {
            var configuration = Map.of("backends", List.of(Map.of("clusterCrossId", "c", "connectionCrossId", "k")));
            var cluster = Cluster.builder().type(ClusterType.KAFKA_VIRTUAL_CLUSTER).name("c").configuration(configuration).build();

            var redacted = cluster.withoutCredentials();

            assertThat(redacted.getConfiguration()).isEqualTo(configuration);
        }
    }
}
