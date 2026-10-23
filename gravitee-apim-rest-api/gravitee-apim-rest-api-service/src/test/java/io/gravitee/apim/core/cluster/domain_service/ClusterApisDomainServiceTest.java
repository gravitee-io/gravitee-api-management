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
import static org.assertj.core.api.Assertions.tuple;

import com.fasterxml.jackson.databind.ObjectMapper;
import fixtures.core.model.ApiFixtures;
import inmemory.ApiQueryServiceInMemory;
import inmemory.ClusterApiSearchQueryServiceInMemory;
import inmemory.EventLatestQueryServiceInMemory;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.cluster.model.Cluster;
import io.gravitee.apim.core.cluster.model.ClusterLifecycleState;
import io.gravitee.apim.core.cluster.model.KafkaClusterConfiguration;
import io.gravitee.apim.core.cluster.model.KafkaClusterConnection;
import io.gravitee.apim.core.event.model.Event;
import io.gravitee.definition.model.cluster.ClusterType;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.model.common.PageableImpl;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ClusterApisDomainServiceTest {

    private static final String ENV_ID = "env-id";
    private static final String CLUSTER_CROSS_ID = "prod";
    private static final String VC_CROSS_ID = "mesh";
    private static final PageableImpl FIRST_PAGE = new PageableImpl(1, 10);

    private final ApiQueryServiceInMemory apiQueryService = new ApiQueryServiceInMemory();
    private final ClusterApiSearchQueryServiceInMemory clusterApiSearchQueryService = new ClusterApiSearchQueryServiceInMemory();
    private final EventLatestQueryServiceInMemory eventLatestQueryService = new EventLatestQueryServiceInMemory();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<Event> deployedClusters = new ArrayList<>();

    private ClusterApisDomainService service;

    @BeforeEach
    void setUp() {
        apiQueryService.reset();
        clusterApiSearchQueryService.reset();
        eventLatestQueryService.reset();
        deployedClusters.clear();
        service = new ClusterApisDomainService(
            clusterApiSearchQueryService,
            apiQueryService,
            new ClusterInUseDomainService(
                new ClusterBoundApisQueryService(apiQueryService, objectMapper),
                new DeployedClustersDomainService(eventLatestQueryService, objectMapper),
                objectMapper
            ),
            objectMapper
        );
    }

    private void givenApis(List<Api> apis) {
        apiQueryService.initWith(apis);
        clusterApiSearchQueryService.initWith(apis);
    }

    @Test
    void should_list_every_api_using_the_registered_cluster_whatever_its_state_with_its_connections() {
        givenApis(
            List.of(
                ApiFixtures.aMessageApiBoundToCluster("payments", ENV_ID, Api.LifecycleState.STARTED, CLUSTER_CROSS_ID, "sasl"),
                ApiFixtures.aNativeApiBoundToCluster("orders", ENV_ID, Api.LifecycleState.STOPPED, CLUSTER_CROSS_ID, "plain"),
                ApiFixtures.aMessageApiBoundToCluster("audit", ENV_ID, Api.LifecycleState.STARTED, "other-cluster", "sasl")
            )
        );

        var apis = service.apisUsing(registeredCluster(connection("sasl"), connection("plain")), ENV_ID, null, FIRST_PAGE).getContent();

        assertThat(apis)
            .extracting(api -> api.api().getId(), ClusterApisDomainService.ClusterApi::connectionCrossIds)
            .containsExactly(tuple("orders", Set.of("plain")), tuple("payments", Set.of("sasl")));
        assertThat(apis).allSatisfy(api -> assertThat(api.virtualClusters()).isEmpty());
    }

    @Test
    void should_list_an_api_naming_no_connection_with_no_connection() {
        givenApis(List.of(ApiFixtures.aNativeApiBoundToCluster("orders", ENV_ID, Api.LifecycleState.STOPPED, CLUSTER_CROSS_ID, null)));

        var apis = service.apisUsing(registeredCluster(connection("sasl")), ENV_ID, null, FIRST_PAGE).getContent();

        assertThat(apis)
            .singleElement()
            .satisfies(api -> assertThat(api.connectionCrossIds()).isEmpty());
    }

    @Test
    void should_list_the_apis_of_a_deployed_virtual_cluster_with_a_backend_on_it_with_the_backend_connections() {
        deploy(virtualCluster(new Backend(CLUSTER_CROSS_ID, "sasl"), new Backend("other-cluster", "plain")));
        givenApis(List.of(ApiFixtures.aNativeApiBoundToVirtualCluster("orders", ENV_ID, Api.LifecycleState.STOPPED, VC_CROSS_ID)));

        var apis = service.apisUsing(registeredCluster(connection("sasl"), connection("plain")), ENV_ID, null, FIRST_PAGE).getContent();

        assertThat(apis)
            .singleElement()
            .satisfies(api -> {
                assertThat(api.api().getId()).isEqualTo("orders");
                assertThat(api.connectionCrossIds()).containsExactly("sasl");
                assertThat(api.virtualClusters()).extracting(Cluster::getName).containsExactly("Mesh");
            });
    }

    @Test
    void should_list_the_apis_of_a_virtual_cluster_whose_backend_names_no_connection_with_no_connection() {
        deploy(virtualCluster(new Backend(CLUSTER_CROSS_ID, null)));
        givenApis(List.of(ApiFixtures.aNativeApiBoundToVirtualCluster("orders", ENV_ID, Api.LifecycleState.STARTED, VC_CROSS_ID)));

        var apis = service.apisUsing(registeredCluster(connection("sasl")), ENV_ID, null, FIRST_PAGE).getContent();

        assertThat(apis)
            .singleElement()
            .satisfies(api -> {
                assertThat(api.connectionCrossIds()).isEmpty();
                assertThat(api.virtualClusters()).extracting(Cluster::getName).containsExactly("Mesh");
            });
    }

    @Test
    void should_not_list_the_apis_of_a_virtual_cluster_with_no_backend_on_it() {
        deploy(virtualCluster(new Backend("other-cluster", "sasl")));
        givenApis(List.of(ApiFixtures.aNativeApiBoundToVirtualCluster("orders", ENV_ID, Api.LifecycleState.STARTED, VC_CROSS_ID)));

        assertThat(service.apisUsing(registeredCluster(connection("sasl")), ENV_ID, null, FIRST_PAGE).getContent()).isEmpty();
    }

    @Test
    void should_list_the_apis_bound_to_a_virtual_cluster_with_no_connection() {
        givenApis(List.of(ApiFixtures.aNativeApiBoundToVirtualCluster("orders", ENV_ID, Api.LifecycleState.STOPPED, VC_CROSS_ID)));

        var apis = service.apisUsing(virtualCluster(new Backend(CLUSTER_CROSS_ID, "sasl")), ENV_ID, null, FIRST_PAGE).getContent();

        assertThat(apis)
            .singleElement()
            .satisfies(api -> {
                assertThat(api.api().getId()).isEqualTo("orders");
                assertThat(api.connectionCrossIds()).isEmpty();
                assertThat(api.virtualClusters()).isEmpty();
            });
    }

    @Test
    void should_page_the_apis_and_keep_the_total() {
        givenApis(
            List.of(
                ApiFixtures.aMessageApiBoundToCluster("payments", ENV_ID, Api.LifecycleState.STARTED, CLUSTER_CROSS_ID, "sasl"),
                ApiFixtures.aNativeApiBoundToCluster("orders", ENV_ID, Api.LifecycleState.STOPPED, CLUSTER_CROSS_ID, "sasl"),
                ApiFixtures.aNativeApiBoundToCluster("audit", ENV_ID, Api.LifecycleState.STOPPED, CLUSTER_CROSS_ID, "sasl")
            )
        );

        var page = service.apisUsing(registeredCluster(connection("sasl")), ENV_ID, null, new PageableImpl(2, 2));

        assertThat(page.getContent())
            .extracting(api -> api.api().getId())
            .containsExactly("payments");
        assertThat(page.getTotalElements()).isEqualTo(3);
    }

    @Test
    void should_narrow_the_apis_to_the_names_containing_the_query() {
        givenApis(
            List.of(
                ApiFixtures.aMessageApiBoundToCluster("payments", ENV_ID, Api.LifecycleState.STARTED, CLUSTER_CROSS_ID, "sasl"),
                ApiFixtures.aNativeApiBoundToCluster("orders", ENV_ID, Api.LifecycleState.STOPPED, CLUSTER_CROSS_ID, "sasl")
            )
        );

        var page = service.apisUsing(registeredCluster(connection("sasl")), ENV_ID, "ORD", FIRST_PAGE);

        assertThat(page.getContent())
            .extracting(api -> api.api().getId())
            .containsExactly("orders");
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    void should_leave_out_an_api_the_index_names_but_the_store_no_longer_has() {
        clusterApiSearchQueryService.initWith(
            List.of(ApiFixtures.aNativeApiBoundToCluster("deleted", ENV_ID, Api.LifecycleState.STOPPED, CLUSTER_CROSS_ID, "sasl"))
        );

        var page = service.apisUsing(registeredCluster(connection("sasl")), ENV_ID, null, FIRST_PAGE);

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    void should_leave_out_an_api_the_index_names_but_that_no_longer_uses_the_cluster() {
        // Indexed while bound to the cluster, then rebound to another one before the index caught up.
        clusterApiSearchQueryService.initWith(
            List.of(ApiFixtures.aNativeApiBoundToCluster("orders", ENV_ID, Api.LifecycleState.STOPPED, CLUSTER_CROSS_ID, "sasl"))
        );
        apiQueryService.initWith(
            List.of(ApiFixtures.aNativeApiBoundToCluster("orders", ENV_ID, Api.LifecycleState.STOPPED, "other-cluster", "sasl"))
        );

        var page = service.apisUsing(registeredCluster(connection("sasl")), ENV_ID, null, FIRST_PAGE);

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isEqualTo(1);
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
