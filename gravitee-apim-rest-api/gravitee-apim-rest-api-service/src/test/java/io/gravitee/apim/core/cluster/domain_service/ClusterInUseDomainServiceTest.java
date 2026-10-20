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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import com.fasterxml.jackson.databind.ObjectMapper;
import fixtures.core.model.ApiFixtures;
import inmemory.ApiQueryServiceInMemory;
import inmemory.EventLatestQueryServiceInMemory;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.cluster.model.Cluster;
import io.gravitee.apim.core.cluster.model.ClusterLifecycleState;
import io.gravitee.apim.core.cluster.model.KafkaClusterConfiguration;
import io.gravitee.apim.core.cluster.model.KafkaClusterConnection;
import io.gravitee.apim.core.event.model.Event;
import io.gravitee.definition.model.cluster.ClusterType;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.service.exceptions.ClusterInUseException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ClusterInUseDomainServiceTest {

    private static final String ENV_ID = "env-id";
    private static final String CLUSTER_CROSS_ID = "prod";
    private static final String VC_CROSS_ID = "mesh";

    private final ApiQueryServiceInMemory apiQueryService = new ApiQueryServiceInMemory();
    private final EventLatestQueryServiceInMemory eventLatestQueryService = new EventLatestQueryServiceInMemory();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<Event> deployedClusters = new ArrayList<>();

    private ClusterInUseDomainService service;

    @BeforeEach
    void setUp() {
        apiQueryService.reset();
        eventLatestQueryService.reset();
        deployedClusters.clear();
        service = new ClusterInUseDomainService(
            new ClusterBoundApisQueryService(apiQueryService, objectMapper),
            new DeployedClustersDomainService(eventLatestQueryService, objectMapper),
            objectMapper
        );
    }

    @Nested
    class CheckNotUsed {

        @Test
        void should_refuse_with_the_started_apis_using_the_cluster() {
            apiQueryService.initWith(
                List.of(
                    ApiFixtures.aNativeApiBoundToCluster("orders", ENV_ID, Api.LifecycleState.STARTED, CLUSTER_CROSS_ID, "sasl"),
                    ApiFixtures.aMessageApiBoundToCluster("payments", ENV_ID, Api.LifecycleState.STARTED, CLUSTER_CROSS_ID, "plain"),
                    ApiFixtures.aMessageApiBoundToCluster("audit", ENV_ID, Api.LifecycleState.STOPPED, CLUSTER_CROSS_ID, "plain"),
                    ApiFixtures.aMessageApiBoundToCluster("other", ENV_ID, Api.LifecycleState.STARTED, "other-cluster", "plain")
                )
            );

            var error = catchThrowableOfType(ClusterInUseException.class, () ->
                service.checkNotUsed(registeredCluster(connection("sasl"), connection("plain")), ENV_ID, "undeploy")
            );

            assertThat(error.getHttpStatusCode()).isEqualTo(409);
            assertThat(error.getTechnicalCode()).isEqualTo("cluster.inUse");
            assertThat(error.getParameters()).containsExactly(Map.entry("orders", "orders"), Map.entry("payments", "payments"));
            assertThat(error.getMessage()).isEqualTo(
                "Cannot undeploy cluster 'Production': 2 started APIs (orders, payments) use it. Stop them first."
            );
        }

        @Test
        void should_name_three_apis_in_the_message_and_list_them_all_in_the_parameters() {
            apiQueryService.initWith(
                List.of("e", "d", "c", "b", "a")
                    .stream()
                    .map(id -> ApiFixtures.aMessageApiBoundToCluster(id, ENV_ID, Api.LifecycleState.STARTED, CLUSTER_CROSS_ID, "plain"))
                    .toList()
            );

            var error = catchThrowableOfType(ClusterInUseException.class, () ->
                service.checkNotUsed(registeredCluster(connection("plain")), ENV_ID, "undeploy")
            );

            assertThat(error.getMessage()).isEqualTo(
                "Cannot undeploy cluster 'Production': 5 started APIs (a, b, c and 2 more) use it. Stop them first."
            );
            assertThat(error.getParameters()).containsOnlyKeys("a", "b", "c", "d", "e");
        }

        @Test
        void should_accept_when_only_stopped_apis_use_the_cluster() {
            apiQueryService.initWith(
                List.of(ApiFixtures.aMessageApiBoundToCluster("audit", ENV_ID, Api.LifecycleState.STOPPED, CLUSTER_CROSS_ID, "plain"))
            );

            assertThatCode(() ->
                service.checkNotUsed(registeredCluster(connection("plain")), ENV_ID, "undeploy")
            ).doesNotThrowAnyException();
        }

        @Test
        void should_refuse_a_virtual_cluster_used_by_a_started_api() {
            apiQueryService.initWith(
                List.of(ApiFixtures.aNativeApiBoundToVirtualCluster("orders", ENV_ID, Api.LifecycleState.STARTED, VC_CROSS_ID))
            );

            var error = catchThrowableOfType(ClusterInUseException.class, () -> service.checkNotUsed(virtualCluster(), ENV_ID, "delete"));

            assertThat(error.getParameters()).containsOnlyKeys("orders");
            assertThat(error.getMessage()).isEqualTo("Cannot delete cluster 'Mesh': 1 started API (orders) uses it. Stop it first.");
        }

        @Test
        void should_refuse_a_registered_cluster_a_deployed_virtual_cluster_with_started_apis_uses() {
            deploy(virtualCluster(new Backend(CLUSTER_CROSS_ID, "sasl")));
            apiQueryService.initWith(
                List.of(ApiFixtures.aNativeApiBoundToVirtualCluster("orders", ENV_ID, Api.LifecycleState.STARTED, VC_CROSS_ID))
            );

            var error = catchThrowableOfType(ClusterInUseException.class, () ->
                service.checkNotUsed(registeredCluster(connection("sasl")), ENV_ID, "undeploy")
            );

            assertThat(error.getParameters()).containsOnlyKeys("orders");
        }

        @Test
        void should_accept_a_registered_cluster_a_virtual_cluster_uses_when_its_apis_are_stopped() {
            deploy(virtualCluster(new Backend(CLUSTER_CROSS_ID, "sasl")));
            apiQueryService.initWith(
                List.of(ApiFixtures.aNativeApiBoundToVirtualCluster("orders", ENV_ID, Api.LifecycleState.STOPPED, VC_CROSS_ID))
            );

            assertThatCode(() ->
                service.checkNotUsed(registeredCluster(connection("sasl")), ENV_ID, "undeploy")
            ).doesNotThrowAnyException();
        }

        @Test
        void should_accept_a_registered_cluster_no_deployed_virtual_cluster_uses() {
            deploy(virtualCluster(new Backend("other-cluster", "sasl")));
            apiQueryService.initWith(
                List.of(ApiFixtures.aNativeApiBoundToVirtualCluster("orders", ENV_ID, Api.LifecycleState.STARTED, VC_CROSS_ID))
            );

            assertThatCode(() ->
                service.checkNotUsed(registeredCluster(connection("sasl")), ENV_ID, "undeploy")
            ).doesNotThrowAnyException();
        }
    }

    @Nested
    class CheckRemovedConnectionsNotUsed {

        @Test
        void should_refuse_removing_a_connection_a_started_api_uses() {
            apiQueryService.initWith(
                List.of(
                    ApiFixtures.aMessageApiBoundToCluster("payments", ENV_ID, Api.LifecycleState.STARTED, CLUSTER_CROSS_ID, "sasl"),
                    ApiFixtures.aMessageApiBoundToCluster("orders", ENV_ID, Api.LifecycleState.STARTED, CLUSTER_CROSS_ID, "plain")
                )
            );

            var error = catchThrowableOfType(ClusterInUseException.class, () ->
                service.checkRemovedConnectionsNotUsed(
                    registeredCluster(connection("sasl"), connection("plain")),
                    configuration(connection("sasl"), connection("plain")),
                    configuration(connection("plain")),
                    ENV_ID,
                    "save"
                )
            );

            assertThat(error.getParameters()).containsOnlyKeys("payments");
            assertThat(error.getMessage()).isEqualTo(
                "Cannot save cluster 'Production': it removes connection 'SASL sasl', and 1 started API (payments) uses it. Stop it first."
            );
        }

        @Test
        void should_accept_removing_a_connection_no_started_api_uses() {
            apiQueryService.initWith(
                List.of(
                    ApiFixtures.aMessageApiBoundToCluster("payments", ENV_ID, Api.LifecycleState.STOPPED, CLUSTER_CROSS_ID, "sasl"),
                    ApiFixtures.aMessageApiBoundToCluster("orders", ENV_ID, Api.LifecycleState.STARTED, CLUSTER_CROSS_ID, "plain")
                )
            );

            assertThatCode(() ->
                service.checkRemovedConnectionsNotUsed(
                    registeredCluster(connection("sasl"), connection("plain")),
                    configuration(connection("sasl"), connection("plain")),
                    configuration(connection("plain")),
                    ENV_ID,
                    "save"
                )
            ).doesNotThrowAnyException();
        }

        @Test
        void should_accept_removing_a_connection_when_the_started_api_names_no_connection() {
            // The gateway refuses a cluster endpoint without a connection, so such an API uses none of them.
            apiQueryService.initWith(
                List.of(ApiFixtures.aNativeApiBoundToCluster("payments", ENV_ID, Api.LifecycleState.STARTED, CLUSTER_CROSS_ID, null))
            );

            assertThatCode(() ->
                service.checkRemovedConnectionsNotUsed(
                    registeredCluster(connection("sasl"), connection("plain")),
                    configuration(connection("sasl"), connection("plain")),
                    configuration(connection("plain")),
                    ENV_ID,
                    "save"
                )
            ).doesNotThrowAnyException();
        }

        @Test
        void should_refuse_removing_a_connection_a_deployed_virtual_cluster_with_started_apis_uses() {
            deploy(virtualCluster(new Backend(CLUSTER_CROSS_ID, "sasl")));
            apiQueryService.initWith(
                List.of(ApiFixtures.aNativeApiBoundToVirtualCluster("orders", ENV_ID, Api.LifecycleState.STARTED, VC_CROSS_ID))
            );

            var error = catchThrowableOfType(ClusterInUseException.class, () ->
                service.checkRemovedConnectionsNotUsed(
                    registeredCluster(connection("sasl"), connection("plain")),
                    configuration(connection("sasl"), connection("plain")),
                    configuration(connection("plain")),
                    ENV_ID,
                    "deploy"
                )
            );

            assertThat(error.getParameters()).containsOnlyKeys("orders");
        }

        @Test
        void should_accept_removing_a_connection_the_virtual_cluster_does_not_use() {
            deploy(virtualCluster(new Backend(CLUSTER_CROSS_ID, "plain")));
            apiQueryService.initWith(
                List.of(ApiFixtures.aNativeApiBoundToVirtualCluster("orders", ENV_ID, Api.LifecycleState.STARTED, VC_CROSS_ID))
            );

            assertThatCode(() ->
                service.checkRemovedConnectionsNotUsed(
                    registeredCluster(connection("sasl"), connection("plain")),
                    configuration(connection("sasl"), connection("plain")),
                    configuration(connection("plain")),
                    ENV_ID,
                    "deploy"
                )
            ).doesNotThrowAnyException();
        }

        @Test
        void should_name_every_removed_connection() {
            apiQueryService.initWith(
                List.of(ApiFixtures.aMessageApiBoundToCluster("payments", ENV_ID, Api.LifecycleState.STARTED, CLUSTER_CROSS_ID, "sasl"))
            );

            var error = catchThrowableOfType(ClusterInUseException.class, () ->
                service.checkRemovedConnectionsNotUsed(
                    registeredCluster(connection("sasl"), connection("plain")),
                    configuration(connection("sasl"), connection("plain")),
                    configuration(),
                    ENV_ID,
                    "save"
                )
            );

            assertThat(error.getMessage()).contains(
                "it removes connections 'SASL sasl', 'SASL plain', and 1 started API (payments) uses them."
            );
        }
    }

    private record Backend(String clusterCrossId, String connectionCrossId) {}

    private static KafkaClusterConnection connection(String crossId) {
        return new KafkaClusterConnection(crossId, "SASL " + crossId, "kafka:9092", null);
    }

    private static KafkaClusterConfiguration configuration(KafkaClusterConnection... connections) {
        return new KafkaClusterConfiguration(List.of(connections));
    }

    private Cluster registeredCluster(KafkaClusterConnection... connections) {
        return Cluster.builder()
            .id("cluster-id")
            .crossId(CLUSTER_CROSS_ID)
            .type(ClusterType.KAFKA_CLUSTER)
            .name("Production")
            .environmentId(ENV_ID)
            .lifecycleState(ClusterLifecycleState.DEPLOYED)
            .configuration(objectMapper.convertValue(configuration(connections), Object.class))
            .build();
    }

    private Cluster virtualCluster(Backend... backends) {
        return Cluster.builder()
            .id("vc-id")
            .crossId(VC_CROSS_ID)
            .type(ClusterType.KAFKA_VIRTUAL_CLUSTER)
            .name("Mesh")
            .environmentId(ENV_ID)
            .lifecycleState(ClusterLifecycleState.DEPLOYED)
            .configuration(Map.of("backends", List.of(backends)))
            .build();
    }

    private void deploy(Cluster cluster) {
        var properties = new EnumMap<Event.EventProperties, String>(Event.EventProperties.class);
        properties.put(Event.EventProperties.CLUSTER_ID, cluster.getCrossId());
        try {
            deployedClusters.add(
                Event.builder()
                    .id("event-" + cluster.getCrossId())
                    .type(EventType.DEPLOY_CLUSTER)
                    .payload(objectMapper.writeValueAsString(cluster))
                    .environments(Set.of(cluster.getEnvironmentId()))
                    .properties(properties)
                    .createdAt(ZonedDateTime.now())
                    .build()
            );
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        eventLatestQueryService.initWith(deployedClusters);
    }
}
