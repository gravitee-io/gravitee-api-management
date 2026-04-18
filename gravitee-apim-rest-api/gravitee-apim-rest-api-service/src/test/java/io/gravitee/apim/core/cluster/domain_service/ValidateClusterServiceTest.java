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
package io.gravitee.apim.core.cluster.domain_service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import inmemory.ClusterQueryServiceInMemory;
import io.gravitee.apim.core.cluster.model.Cluster;
import io.gravitee.apim.core.json.JsonSchemaChecker;
import io.gravitee.apim.infra.json.JsonSchemaCheckerImpl;
import io.gravitee.definition.model.cluster.ClusterType;
import io.gravitee.json.validation.JsonSchemaValidatorImpl;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import io.gravitee.rest.api.service.impl.JsonSchemaServiceImpl;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ValidateClusterServiceTest {

    private ValidateClusterService validateClusterService;
    private final ClusterQueryServiceInMemory clusterQueryService = new ClusterQueryServiceInMemory();

    @BeforeEach
    void setUp() {
        JsonSchemaChecker jsonSchemaChecker = new JsonSchemaCheckerImpl(new JsonSchemaServiceImpl(new JsonSchemaValidatorImpl()));
        ClusterConfigurationSchemaService clusterConfigurationSchemaService = new ClusterConfigurationSchemaService();
        validateClusterService = new ValidateClusterService(
            jsonSchemaChecker,
            clusterConfigurationSchemaService,
            new ObjectMapper(),
            clusterQueryService
        );
        clusterQueryService.reset();
    }

    private static Map<String, Object> validConfig() {
        return Map.of("bootstrapServers", "localhost:9092", "security", Map.of("protocol", "PLAINTEXT"));
    }

    @Test
    void should_throw_when_name_is_empty() {
        Cluster cluster = Cluster.builder().type(ClusterType.KAFKA_CLUSTER_CONNECTION).name("").configuration(validConfig()).build();

        assertThatThrownBy(() -> validateClusterService.validate(cluster))
            .isInstanceOf(InvalidDataException.class)
            .hasMessage("Name is required.");
    }

    @Test
    void should_throw_when_name_is_null() {
        Cluster cluster = Cluster.builder().type(ClusterType.KAFKA_CLUSTER_CONNECTION).name(null).configuration(validConfig()).build();

        assertThatThrownBy(() -> validateClusterService.validate(cluster))
            .isInstanceOf(InvalidDataException.class)
            .hasMessage("Name is required.");
    }

    @Test
    void should_throw_when_type_is_null() {
        Cluster cluster = Cluster.builder().name("my-cluster").configuration(validConfig()).build();

        assertThatThrownBy(() -> validateClusterService.validate(cluster))
            .isInstanceOf(InvalidDataException.class)
            .hasMessage("Type is required.");
    }

    @Test
    void should_throw_when_configuration_is_null() {
        Cluster cluster = Cluster.builder().type(ClusterType.KAFKA_CLUSTER_CONNECTION).name("my-cluster").configuration(null).build();

        assertThatThrownBy(() -> validateClusterService.validate(cluster))
            .isInstanceOf(InvalidDataException.class)
            .hasMessage("Configuration is required.");
    }

    @Test
    void should_pass_with_valid_plaintext_configuration() {
        Cluster cluster = Cluster.builder()
            .type(ClusterType.KAFKA_CLUSTER_CONNECTION)
            .name("my-cluster")
            .configuration(validConfig())
            .build();

        assertThatCode(() -> validateClusterService.validate(cluster)).doesNotThrowAnyException();
    }

    @Test
    void should_throw_when_protocol_has_invalid_value() {
        Cluster cluster = Cluster.builder()
            .type(ClusterType.KAFKA_CLUSTER_CONNECTION)
            .name("my-cluster")
            .configuration(Map.of("bootstrapServers", "localhost:9092", "security", Map.of("protocol", "INVALID_PROTOCOL")))
            .build();

        assertThatThrownBy(() -> validateClusterService.validate(cluster)).isInstanceOf(InvalidDataException.class);
    }

    @Test
    void should_throw_when_bootstrap_servers_is_missing() {
        Cluster cluster = Cluster.builder()
            .type(ClusterType.KAFKA_CLUSTER_CONNECTION)
            .name("my-cluster")
            .configuration(Map.of("security", Map.of("protocol", "PLAINTEXT")))
            .build();

        assertThatThrownBy(() -> validateClusterService.validate(cluster)).isInstanceOf(InvalidDataException.class);
    }

    @Test
    void should_pass_with_valid_kafka_cluster_configuration() {
        Cluster cluster = Cluster.builder()
            .type(ClusterType.KAFKA_CLUSTER)
            .name("my-kafka-cluster")
            .configuration(
                Map.of(
                    "connections",
                    List.of(Map.of("name", "conn-1", "bootstrapServers", "kafka1:9092", "security", Map.of("protocol", "PLAINTEXT")))
                )
            )
            .build();

        assertThatCode(() -> validateClusterService.validate(cluster)).doesNotThrowAnyException();
    }

    @Test
    void should_pass_when_kafka_cluster_has_empty_connections() {
        Cluster cluster = Cluster.builder()
            .type(ClusterType.KAFKA_CLUSTER)
            .name("my-kafka-cluster")
            .configuration(Map.of("connections", List.of()))
            .build();

        assertThatCode(() -> validateClusterService.validate(cluster)).doesNotThrowAnyException();
    }

    @Test
    void should_throw_when_kafka_cluster_connection_missing_bootstrap_servers() {
        Cluster cluster = Cluster.builder()
            .type(ClusterType.KAFKA_CLUSTER)
            .name("my-kafka-cluster")
            .configuration(Map.of("connections", List.of(Map.of("name", "conn-1", "security", Map.of("protocol", "PLAINTEXT")))))
            .build();

        assertThatThrownBy(() -> validateClusterService.validate(cluster)).isInstanceOf(InvalidDataException.class);
    }

    @Test
    void should_throw_when_kafka_cluster_connection_missing_name() {
        Cluster cluster = Cluster.builder()
            .type(ClusterType.KAFKA_CLUSTER)
            .name("my-kafka-cluster")
            .configuration(
                Map.of("connections", List.of(Map.of("bootstrapServers", "kafka1:9092", "security", Map.of("protocol", "PLAINTEXT"))))
            )
            .build();

        assertThatThrownBy(() -> validateClusterService.validate(cluster)).isInstanceOf(InvalidDataException.class);
    }

    @Test
    void should_throw_when_kafka_cluster_has_duplicate_connection_names() {
        Cluster cluster = Cluster.builder()
            .type(ClusterType.KAFKA_CLUSTER)
            .name("my-kafka-cluster")
            .configuration(
                Map.of(
                    "connections",
                    List.of(
                        Map.of("name", "same-name", "bootstrapServers", "kafka1:9092", "security", Map.of("protocol", "PLAINTEXT")),
                        Map.of("name", "same-name", "bootstrapServers", "kafka2:9092", "security", Map.of("protocol", "PLAINTEXT"))
                    )
                )
            )
            .build();

        assertThatThrownBy(() -> validateClusterService.validate(cluster))
            .isInstanceOf(InvalidDataException.class)
            .hasMessageContaining("Connection names must be unique");
    }

    @Test
    void should_throw_when_crossId_is_not_unique_in_environment() {
        Cluster existingCluster = Cluster.builder()
            .id("existing-id")
            .crossId("my-cluster")
            .type(ClusterType.KAFKA_CLUSTER_CONNECTION)
            .name("My Cluster")
            .environmentId("env-1")
            .configuration(validConfig())
            .build();
        clusterQueryService.initWith(List.of(existingCluster));

        Cluster newCluster = Cluster.builder()
            .id("new-id")
            .crossId("my-cluster")
            .type(ClusterType.KAFKA_CLUSTER_CONNECTION)
            .name("My Cluster 2")
            .environmentId("env-1")
            .configuration(validConfig())
            .build();

        assertThatThrownBy(() -> validateClusterService.validateForCreate(newCluster))
            .isInstanceOf(InvalidDataException.class)
            .hasMessageContaining("A cluster with crossId 'my-cluster' already exists");
    }

    @Test
    void should_pass_when_crossId_is_unique_in_environment() {
        Cluster existingCluster = Cluster.builder()
            .id("existing-id")
            .crossId("my-cluster")
            .type(ClusterType.KAFKA_CLUSTER_CONNECTION)
            .name("My Cluster")
            .environmentId("env-1")
            .configuration(validConfig())
            .build();
        clusterQueryService.initWith(List.of(existingCluster));

        Cluster newCluster = Cluster.builder()
            .id("new-id")
            .crossId("my-other-cluster")
            .type(ClusterType.KAFKA_CLUSTER_CONNECTION)
            .name("My Other Cluster")
            .environmentId("env-1")
            .configuration(validConfig())
            .build();

        assertThatCode(() -> validateClusterService.validateForCreate(newCluster)).doesNotThrowAnyException();
    }

    @Test
    void should_pass_when_same_crossId_in_different_environment() {
        Cluster existingCluster = Cluster.builder()
            .id("existing-id")
            .crossId("my-cluster")
            .type(ClusterType.KAFKA_CLUSTER_CONNECTION)
            .name("My Cluster")
            .environmentId("env-1")
            .configuration(validConfig())
            .build();
        clusterQueryService.initWith(List.of(existingCluster));

        Cluster newCluster = Cluster.builder()
            .id("new-id")
            .crossId("my-cluster")
            .type(ClusterType.KAFKA_CLUSTER_CONNECTION)
            .name("My Cluster")
            .environmentId("env-2")
            .configuration(validConfig())
            .build();

        assertThatCode(() -> validateClusterService.validateForCreate(newCluster)).doesNotThrowAnyException();
    }

    @Test
    void should_throw_when_crossId_is_empty() {
        Cluster cluster = Cluster.builder()
            .id("new-id")
            .crossId("")
            .type(ClusterType.KAFKA_CLUSTER_CONNECTION)
            .name("My Cluster")
            .environmentId("env-1")
            .configuration(validConfig())
            .build();

        assertThatThrownBy(() -> validateClusterService.validateForCreate(cluster))
            .isInstanceOf(InvalidDataException.class)
            .hasMessageContaining("CrossId is required.");
    }

    @Test
    void should_throw_when_kafka_cluster_has_duplicate_connection_crossIds() {
        Cluster cluster = Cluster.builder()
            .type(ClusterType.KAFKA_CLUSTER)
            .name("my-kafka-cluster")
            .configuration(
                Map.of(
                    "connections",
                    List.of(
                        Map.of(
                            "crossId",
                            "same-cross-id",
                            "name",
                            "conn-1",
                            "bootstrapServers",
                            "kafka1:9092",
                            "security",
                            Map.of("protocol", "PLAINTEXT")
                        ),
                        Map.of(
                            "crossId",
                            "same-cross-id",
                            "name",
                            "conn-2",
                            "bootstrapServers",
                            "kafka2:9092",
                            "security",
                            Map.of("protocol", "PLAINTEXT")
                        )
                    )
                )
            )
            .build();

        assertThatThrownBy(() -> validateClusterService.validate(cluster))
            .isInstanceOf(InvalidDataException.class)
            .hasMessageContaining("Connection crossIds must be unique within a cluster");
    }

    @Test
    void should_throw_when_crossId_is_changed_on_update() {
        Cluster existingCluster = Cluster.builder()
            .id("cluster-id")
            .crossId("original-cross-id")
            .type(ClusterType.KAFKA_CLUSTER_CONNECTION)
            .name("My Cluster")
            .environmentId("env-1")
            .configuration(validConfig())
            .build();

        Cluster updatedCluster = Cluster.builder()
            .id("cluster-id")
            .crossId("modified-cross-id")
            .type(ClusterType.KAFKA_CLUSTER_CONNECTION)
            .name("My Cluster")
            .environmentId("env-1")
            .configuration(validConfig())
            .build();

        assertThatThrownBy(() -> validateClusterService.validateForUpdate(existingCluster, updatedCluster))
            .isInstanceOf(InvalidDataException.class)
            .hasMessageContaining("CrossId is immutable");
    }

    @Test
    void should_pass_when_crossId_is_unchanged_on_update() {
        Cluster existingCluster = Cluster.builder()
            .id("cluster-id")
            .crossId("my-cross-id")
            .type(ClusterType.KAFKA_CLUSTER_CONNECTION)
            .name("My Cluster")
            .environmentId("env-1")
            .configuration(validConfig())
            .build();

        Cluster updatedCluster = Cluster.builder()
            .id("cluster-id")
            .crossId("my-cross-id")
            .type(ClusterType.KAFKA_CLUSTER_CONNECTION)
            .name("Updated Name")
            .environmentId("env-1")
            .configuration(validConfig())
            .build();

        assertThatCode(() -> validateClusterService.validateForUpdate(existingCluster, updatedCluster)).doesNotThrowAnyException();
    }
}
