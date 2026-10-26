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
package io.gravitee.apim.core.cluster.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import inmemory.ClusterQueryServiceInMemory;
import inmemory.EventLatestQueryServiceInMemory;
import io.gravitee.apim.core.cluster.domain_service.ClusterBindingPermissionDomainService;
import io.gravitee.apim.core.cluster.domain_service.DeployedClustersDomainService;
import io.gravitee.apim.core.cluster.model.DeployedCluster;
import io.gravitee.apim.core.event.model.Event;
import io.gravitee.apim.core.permission.domain_service.PermissionDomainService;
import io.gravitee.definition.model.cluster.ClusterType;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import java.time.ZonedDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetDeployedClustersUseCaseTest {

    private static final String ENV_ID = "env-id";
    private static final String ORG_ID = "org-id";
    private static final String USER_ID = "user-id";

    private final EventLatestQueryServiceInMemory eventLatestQueryService = new EventLatestQueryServiceInMemory();
    private final PermissionDomainService permissionDomainService = mock(PermissionDomainService.class);
    private GetDeployedClustersUseCase useCase;

    @BeforeEach
    void setUp() {
        eventLatestQueryService.reset();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        useCase = new GetDeployedClustersUseCase(
            new DeployedClustersDomainService(eventLatestQueryService, objectMapper),
            new ClusterBindingPermissionDomainService(new ClusterQueryServiceInMemory(), permissionDomainService, new ObjectMapper()),
            objectMapper
        );
    }

    @Test
    void should_return_empty_list_when_no_deployed_clusters() {
        var output = useCase.execute(new GetDeployedClustersUseCase.Input(ORG_ID, ENV_ID, USER_ID, null));
        assertThat(output.deployedClusters()).isEmpty();
    }

    @Test
    void should_return_deployed_cluster_with_connections() {
        String payload = """
            {
                "id": "cluster-id",
                "crossId": "my-kafka-cluster",
                "name": "My Kafka Cluster",
                "description": "A test cluster",
                "type": "KAFKA_CLUSTER",
                "deployedAt": 1698000000,
                "version": 2,
                "configuration": {
                    "connections": [
                        { "crossId": "primary", "name": "Primary Connection", "bootstrapServers": "kafka:9092", "security": { "protocol": "PLAINTEXT" } },
                        { "crossId": "secondary", "name": "Secondary Connection", "bootstrapServers": "kafka2:9092", "security": { "protocol": "SSL" } },
                        { "crossId": "sasl", "name": "SASL Connection", "bootstrapServers": "kafka3:9093", "security": { "protocol": "SASL_SSL", "sasl": { "mechanism": { "type": "SCRAM-SHA-512", "username": "a", "password": "b" } } } },
                        { "crossId": "oauth", "name": "OAuth Connection", "bootstrapServers": "kafka3:9093", "security": { "protocol": "SASL_SSL", "sasl": { "mechanism": { "type": "OAUTHBEARER_TOKEN" } } } }
                    ]
                }
            }
            """;

        var properties = new EnumMap<Event.EventProperties, String>(Event.EventProperties.class);
        properties.put(Event.EventProperties.CLUSTER_ID, "my-kafka-cluster");

        eventLatestQueryService.initWith(
            List.of(
                Event.builder()
                    .id("event-1")
                    .type(EventType.DEPLOY_CLUSTER)
                    .payload(payload)
                    .environments(Set.of(ENV_ID))
                    .properties(properties)
                    .createdAt(ZonedDateTime.now())
                    .build()
            )
        );

        var output = useCase.execute(new GetDeployedClustersUseCase.Input(ORG_ID, ENV_ID, USER_ID, null));

        assertThat(output.deployedClusters()).hasSize(1);
        DeployedCluster cluster = output.deployedClusters().get(0);
        assertThat(cluster.getCrossId()).isEqualTo("my-kafka-cluster");
        assertThat(cluster.getName()).isEqualTo("My Kafka Cluster");
        assertThat(cluster.getDescription()).isEqualTo("A test cluster");
        assertThat(cluster.getVersion()).isEqualTo(2);
        assertThat(cluster.getConnections()).hasSize(4);
        assertThat(cluster.getConnections().get(0).getCrossId()).isEqualTo("primary");
        assertThat(cluster.getConnections().get(0).getName()).isEqualTo("Primary Connection");
        assertThat(cluster.getConnections().get(1).getCrossId()).isEqualTo("secondary");
        assertThat(cluster.getConnections())
            .extracting(DeployedCluster.DeployedClusterConnection::getMessageApiUnsupportedReason)
            .containsExactly(null, null, null, "Message APIs do not support the SASL mechanism OAUTHBEARER_TOKEN yet.");
    }

    @Test
    void should_not_return_clusters_from_other_environments() {
        String payload = """
            {
                "id": "cluster-id",
                "crossId": "other-cluster",
                "name": "Other Cluster",
                "type": "KAFKA_CLUSTER",
                "configuration": { "connections": [] }
            }
            """;

        var properties = new EnumMap<Event.EventProperties, String>(Event.EventProperties.class);
        properties.put(Event.EventProperties.CLUSTER_ID, "other-cluster");

        eventLatestQueryService.initWith(
            List.of(
                Event.builder()
                    .id("event-1")
                    .type(EventType.DEPLOY_CLUSTER)
                    .payload(payload)
                    .environments(Set.of("other-env"))
                    .properties(properties)
                    .createdAt(ZonedDateTime.now())
                    .build()
            )
        );

        var output = useCase.execute(new GetDeployedClustersUseCase.Input(ORG_ID, ENV_ID, USER_ID, null));
        assertThat(output.deployedClusters()).isEmpty();
    }

    @Test
    void should_filter_deployed_clusters_by_type() {
        String kafkaClusterPayload = """
            {
                "id": "cluster-1",
                "crossId": "kafka-cluster",
                "name": "Kafka Cluster",
                "type": "KAFKA_CLUSTER",
                "configuration": { "connections": [] }
            }
            """;

        String virtualClusterPayload = """
            {
                "id": "cluster-2",
                "crossId": "virtual-cluster",
                "name": "Virtual Cluster",
                "type": "KAFKA_VIRTUAL_CLUSTER",
                "configuration": { "backends": [] }
            }
            """;

        var props1 = new EnumMap<Event.EventProperties, String>(Event.EventProperties.class);
        props1.put(Event.EventProperties.CLUSTER_ID, "kafka-cluster");
        var props2 = new EnumMap<Event.EventProperties, String>(Event.EventProperties.class);
        props2.put(Event.EventProperties.CLUSTER_ID, "virtual-cluster");

        eventLatestQueryService.initWith(
            List.of(
                Event.builder()
                    .id("event-1")
                    .type(EventType.DEPLOY_CLUSTER)
                    .payload(kafkaClusterPayload)
                    .environments(Set.of(ENV_ID))
                    .properties(props1)
                    .createdAt(ZonedDateTime.now())
                    .build(),
                Event.builder()
                    .id("event-2")
                    .type(EventType.DEPLOY_CLUSTER)
                    .payload(virtualClusterPayload)
                    .environments(Set.of(ENV_ID))
                    .properties(props2)
                    .createdAt(ZonedDateTime.now())
                    .build()
            )
        );

        var output = useCase.execute(new GetDeployedClustersUseCase.Input(ORG_ID, ENV_ID, USER_ID, ClusterType.KAFKA_VIRTUAL_CLUSTER));

        assertThat(output.deployedClusters()).hasSize(1);
        assertThat(output.deployedClusters().get(0).getCrossId()).isEqualTo("virtual-cluster");
        assertThat(output.deployedClusters().get(0).getType()).isEqualTo(ClusterType.KAFKA_VIRTUAL_CLUSTER);
    }

    @Test
    void should_tell_which_clusters_the_caller_may_bind() {
        givenDeployed(
            deployEvent("member-id", "member", "KAFKA_CLUSTER"),
            deployEvent("other-id", "other", "KAFKA_CLUSTER"),
            deployEvent("mesh-id", "mesh", "KAFKA_VIRTUAL_CLUSTER")
        );
        when(
            permissionDomainService.hasPermission(
                ORG_ID,
                USER_ID,
                RolePermission.CLUSTER_DEFINITION,
                "member-id",
                RolePermissionAction.READ
            )
        ).thenReturn(true);

        var output = useCase.execute(new GetDeployedClustersUseCase.Input(ORG_ID, ENV_ID, USER_ID, null));

        assertThat(output.deployedClusters())
            .extracting(DeployedCluster::getCrossId, DeployedCluster::isBindable)
            .containsExactlyInAnyOrder(tuple("member", true), tuple("other", false), tuple("mesh", true));
    }

    private void givenDeployed(Event... events) {
        eventLatestQueryService.initWith(List.of(events));
    }

    private static Event deployEvent(String id, String crossId, String type) {
        var properties = new EnumMap<Event.EventProperties, String>(Event.EventProperties.class);
        properties.put(Event.EventProperties.CLUSTER_ID, crossId);
        return Event.builder()
            .id("event-" + id)
            .type(EventType.DEPLOY_CLUSTER)
            .payload(
                """
                { "id": "%s", "crossId": "%s", "name": "%s", "type": "%s", "configuration": {} }
                """.formatted(id, crossId, crossId, type)
            )
            .environments(Set.of(ENV_ID))
            .properties(properties)
            .createdAt(ZonedDateTime.now())
            .build();
    }
}
