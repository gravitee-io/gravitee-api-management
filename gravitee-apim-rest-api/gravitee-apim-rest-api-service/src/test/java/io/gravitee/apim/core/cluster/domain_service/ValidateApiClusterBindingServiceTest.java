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
import inmemory.EventLatestQueryServiceInMemory;
import io.gravitee.apim.core.cluster.model.Cluster;
import io.gravitee.apim.core.cluster.model.ClusterLifecycleState;
import io.gravitee.apim.core.event.model.Event;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.cluster.ClusterType;
import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.endpointgroup.Endpoint;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.definition.model.v4.nativeapi.NativeApi;
import io.gravitee.definition.model.v4.nativeapi.NativeEndpoint;
import io.gravitee.definition.model.v4.nativeapi.NativeEndpointGroup;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.service.exceptions.ApiNotDeployableException;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ValidateApiClusterBindingServiceTest {

    private static final String ENV_ID = "env-id";
    private static final String VC_CROSS_ID = "mesh";

    private final ClusterQueryServiceInMemory clusterQueryService = new ClusterQueryServiceInMemory();
    private final EventLatestQueryServiceInMemory eventLatestQueryService = new EventLatestQueryServiceInMemory();
    private final ObjectMapper objectMapper = new GraviteeMapper();

    private ValidateApiClusterBindingService service;

    @BeforeEach
    void setUp() {
        clusterQueryService.reset();
        eventLatestQueryService.reset();
        var deployedClustersDomainService = new DeployedClustersDomainService(eventLatestQueryService, objectMapper);
        service = new ValidateApiClusterBindingService(
            new ClusterResolutionDomainService(clusterQueryService, deployedClustersDomainService, objectMapper),
            new ValidateKafkaClusterBindingDomainService(deployedClustersDomainService, objectMapper),
            objectMapper
        );
    }

    @Test
    void should_throw_when_bound_virtual_cluster_is_missing() {
        assertThatThrownBy(() -> service.validateDeployable("api-1", ENV_ID, ApiType.NATIVE, virtualClusterBoundDefinition()))
            .isInstanceOf(ApiNotDeployableException.class)
            .hasMessage("The API {api-1} cannot be deployed or started because its cluster {mesh} is not deployed");
    }

    @Test
    void should_throw_when_bound_virtual_cluster_is_not_deployed() {
        givenStored(virtualCluster(ClusterLifecycleState.UNDEPLOYED), registeredCluster(ClusterLifecycleState.DEPLOYED));
        givenDeployed(virtualCluster(ClusterLifecycleState.DEPLOYED), registeredCluster(ClusterLifecycleState.DEPLOYED));

        assertThatThrownBy(() -> service.validateDeployable("api-1", ENV_ID, ApiType.NATIVE, virtualClusterBoundDefinition()))
            .isInstanceOf(ApiNotDeployableException.class)
            .hasMessage("The API {api-1} cannot be deployed or started because its cluster {mesh} is not deployed");
    }

    @Test
    void should_pass_when_bound_virtual_cluster_and_its_backends_are_deployed() {
        givenDeployedAndStored(virtualCluster(ClusterLifecycleState.DEPLOYED), registeredCluster(ClusterLifecycleState.DEPLOYED));

        assertThatCode(() ->
            service.validateDeployable("api-1", ENV_ID, ApiType.NATIVE, virtualClusterBoundDefinition())
        ).doesNotThrowAnyException();
    }

    @Test
    void should_pass_when_bound_virtual_cluster_is_pending() {
        // PENDING = deployed at the gateway with not-yet-redeployed edits, so still a valid target.
        givenDeployedAndStored(virtualCluster(ClusterLifecycleState.PENDING), registeredCluster(ClusterLifecycleState.DEPLOYED));

        assertThatCode(() ->
            service.validateDeployable("api-1", ENV_ID, ApiType.NATIVE, virtualClusterBoundDefinition())
        ).doesNotThrowAnyException();
    }

    @Test
    void should_throw_when_a_backend_cluster_of_the_bound_virtual_cluster_is_not_deployed() {
        givenStored(virtualCluster(ClusterLifecycleState.DEPLOYED), registeredCluster(ClusterLifecycleState.UNDEPLOYED));
        givenDeployed(virtualCluster(ClusterLifecycleState.DEPLOYED));

        assertThatThrownBy(() -> service.validateDeployable("api-1", ENV_ID, ApiType.NATIVE, virtualClusterBoundDefinition()))
            .isInstanceOf(ApiNotDeployableException.class)
            .hasMessage(
                "The API {api-1} cannot be deployed or started because its virtual cluster {mesh} has a backend on cluster {prod}, which is not deployed"
            );
    }

    @Test
    void should_throw_when_a_backend_connection_of_the_bound_virtual_cluster_is_not_in_the_deployed_cluster() {
        givenDeployedAndStored(virtualCluster(ClusterLifecycleState.DEPLOYED, "plain"), registeredCluster(ClusterLifecycleState.DEPLOYED));

        assertThatThrownBy(() -> service.validateDeployable("api-1", ENV_ID, ApiType.NATIVE, virtualClusterBoundDefinition()))
            .isInstanceOf(ApiNotDeployableException.class)
            .hasMessage(
                "The API {api-1} cannot be deployed or started because its virtual cluster {mesh} has a backend on connection {plain}, which does not exist in the deployed cluster {prod}"
            );
    }

    @Test
    void should_throw_when_native_api_registered_cluster_is_not_deployed() {
        givenStored(registeredCluster(ClusterLifecycleState.UNDEPLOYED));

        assertThatThrownBy(() -> service.validateDeployable("api-1", ENV_ID, ApiType.NATIVE, registeredClusterBoundDefinition("sasl")))
            .isInstanceOf(ApiNotDeployableException.class)
            .hasMessage("The API {api-1} cannot be deployed or started because its cluster {prod} is not deployed");
    }

    @Test
    void should_pass_when_native_api_registered_cluster_is_deployed() {
        givenDeployedAndStored(registeredCluster(ClusterLifecycleState.DEPLOYED));

        assertThatCode(() ->
            service.validateDeployable("api-1", ENV_ID, ApiType.NATIVE, registeredClusterBoundDefinition("sasl"))
        ).doesNotThrowAnyException();
    }

    @Test
    void should_throw_when_native_api_connection_is_not_in_the_deployed_cluster() {
        givenDeployedAndStored(registeredCluster(ClusterLifecycleState.DEPLOYED));

        assertThatThrownBy(() -> service.validateDeployable("api-1", ENV_ID, ApiType.NATIVE, registeredClusterBoundDefinition("plain")))
            .isInstanceOf(ApiNotDeployableException.class)
            .hasMessage(
                "The API {api-1} cannot be deployed or started because its connection {plain} does not exist in the deployed cluster {prod}"
            );
    }

    @Test
    void should_throw_when_native_api_connection_is_only_in_the_edits_not_yet_redeployed() {
        givenStored(registeredCluster(ClusterLifecycleState.PENDING, "sasl", "plain"));
        givenDeployed(registeredCluster(ClusterLifecycleState.DEPLOYED, "sasl"));

        assertThatThrownBy(() -> service.validateDeployable("api-1", ENV_ID, ApiType.NATIVE, registeredClusterBoundDefinition("plain")))
            .isInstanceOf(ApiNotDeployableException.class)
            .hasMessage(
                "The API {api-1} cannot be deployed or started because its connection {plain} does not exist in the deployed cluster {prod}"
            );
    }

    @Test
    void should_resolve_a_pending_virtual_cluster_from_its_deployed_backends_not_its_edits() {
        Cluster deployed = virtualCluster(ClusterLifecycleState.DEPLOYED);
        Cluster edited = virtualCluster(ClusterLifecycleState.PENDING, "plain");
        givenStored(edited, registeredCluster(ClusterLifecycleState.DEPLOYED));
        givenDeployed(deployed, registeredCluster(ClusterLifecycleState.DEPLOYED));

        assertThatCode(() ->
            service.validateDeployable("api-1", ENV_ID, ApiType.NATIVE, virtualClusterBoundDefinition())
        ).doesNotThrowAnyException();
    }

    @Test
    void should_throw_when_a_deployed_cluster_has_no_deployed_version() {
        givenStored(registeredCluster(ClusterLifecycleState.DEPLOYED));

        assertThatThrownBy(() -> service.validateDeployable("api-1", ENV_ID, ApiType.NATIVE, registeredClusterBoundDefinition("sasl")))
            .isInstanceOf(ApiNotDeployableException.class)
            .hasMessage("The API {api-1} cannot be deployed or started because its cluster {prod} has no deployed version: redeploy it");
    }

    @Test
    void should_throw_when_a_cluster_endpoint_names_a_virtual_cluster() {
        givenDeployedAndStored(virtualCluster(ClusterLifecycleState.DEPLOYED), registeredCluster(ClusterLifecycleState.DEPLOYED));

        assertThatThrownBy(() ->
            service.validateDeployable(
                "api-1",
                ENV_ID,
                ApiType.NATIVE,
                nativeDefinition("native-kafka-cluster", "{\"clusterCrossId\":\"mesh\",\"connectionCrossId\":\"sasl\"}")
            )
        )
            .isInstanceOf(ApiNotDeployableException.class)
            .hasMessage("The API {api-1} cannot be deployed or started because its cluster {mesh} is not a registered Kafka cluster");
    }

    @Test
    void should_throw_when_a_virtual_cluster_endpoint_names_a_registered_cluster() {
        givenDeployedAndStored(registeredCluster(ClusterLifecycleState.DEPLOYED));

        assertThatThrownBy(() ->
            service.validateDeployable(
                "api-1",
                ENV_ID,
                ApiType.NATIVE,
                nativeDefinition("native-kafka-virtual-cluster", "{\"virtualClusterCrossId\":\"prod\"}")
            )
        )
            .isInstanceOf(ApiNotDeployableException.class)
            .hasMessage("The API {api-1} cannot be deployed or started because its cluster {prod} is not a virtual cluster");
    }

    @Test
    void should_throw_when_the_deployed_virtual_cluster_has_no_backend() {
        Cluster noBackend = virtualCluster(ClusterLifecycleState.DEPLOYED).toBuilder().configuration(Map.of("backends", List.of())).build();
        givenDeployedAndStored(noBackend);

        assertThatThrownBy(() -> service.validateDeployable("api-1", ENV_ID, ApiType.NATIVE, virtualClusterBoundDefinition()))
            .isInstanceOf(ApiNotDeployableException.class)
            .hasMessage("The API {api-1} cannot be deployed or started because its virtual cluster {mesh} has no backend");
    }

    @Test
    void should_check_only_the_cluster_of_an_endpoint_naming_no_connection() {
        givenDeployedAndStored(registeredCluster(ClusterLifecycleState.DEPLOYED));

        assertThatCode(() ->
            service.validateDeployable(
                "api-1",
                ENV_ID,
                ApiType.NATIVE,
                nativeDefinition("native-kafka-cluster", "{\"clusterCrossId\":\"prod\"}")
            )
        ).doesNotThrowAnyException();
    }

    @Test
    void should_be_noop_for_non_native_api() {
        assertThatCode(() ->
            service.validateDeployable("api-1", ENV_ID, ApiType.PROXY, virtualClusterBoundDefinition())
        ).doesNotThrowAnyException();
    }

    @Test
    void should_be_noop_when_api_is_not_bound_to_a_virtual_cluster() throws Exception {
        String definition = objectMapper.writeValueAsString(
            NativeApi.builder()
                .endpointGroups(
                    List.of(
                        NativeEndpointGroup.builder()
                            .endpoints(List.of(NativeEndpoint.builder().type("mock").configuration("{}").build()))
                            .build()
                    )
                )
                .build()
        );

        assertThatCode(() -> service.validateDeployable("api-1", ENV_ID, ApiType.NATIVE, definition)).doesNotThrowAnyException();
    }

    @Test
    void should_be_noop_for_null_and_malformed_definition() {
        assertThatCode(() -> service.validateDeployable("api-1", ENV_ID, ApiType.NATIVE, null)).doesNotThrowAnyException();
        assertThatCode(() -> service.validateDeployable("api-1", ENV_ID, ApiType.NATIVE, "{ not-json")).doesNotThrowAnyException();
    }

    @Test
    void should_throw_when_message_api_kafka_cluster_is_not_deployed() {
        givenStored(registeredCluster(ClusterLifecycleState.UNDEPLOYED));

        assertThatThrownBy(() -> service.validateDeployable("api-1", ENV_ID, ApiType.MESSAGE, messageDefinition("sasl")))
            .isInstanceOf(ApiNotDeployableException.class)
            .hasMessage("The API {api-1} cannot be deployed or started because its cluster {prod} is not deployed");
    }

    @Test
    void should_throw_when_message_api_kafka_cluster_is_missing() {
        assertThatThrownBy(() -> service.validateDeployable("api-1", ENV_ID, ApiType.MESSAGE, messageDefinition("sasl"))).isInstanceOf(
            ApiNotDeployableException.class
        );
    }

    @Test
    void should_throw_when_message_api_connection_is_not_in_the_deployed_cluster() {
        givenDeployedAndStored(registeredCluster(ClusterLifecycleState.DEPLOYED));

        assertThatThrownBy(() -> service.validateDeployable("api-1", ENV_ID, ApiType.MESSAGE, messageDefinition("plain")))
            .isInstanceOf(ApiNotDeployableException.class)
            .hasMessage(
                "The API {api-1} cannot be deployed or started because its connection {plain} does not exist in the deployed cluster {prod}"
            );
    }

    @Test
    void should_pass_when_message_api_kafka_cluster_is_deployed_or_pending() {
        givenStored(registeredCluster(ClusterLifecycleState.PENDING));
        givenDeployed(registeredCluster(ClusterLifecycleState.DEPLOYED));

        assertThatCode(() ->
            service.validateDeployable("api-1", ENV_ID, ApiType.MESSAGE, messageDefinition("sasl"))
        ).doesNotThrowAnyException();
    }

    @Test
    void should_throw_when_a_message_api_cluster_group_names_no_cluster() {
        assertThatThrownBy(() ->
            service.validateDeployable(
                "api-1",
                ENV_ID,
                ApiType.MESSAGE,
                messageDefinition("{\"mode\":\"CLUSTER\"}", "{\"connectionCrossId\":\"sasl\"}")
            )
        )
            .isInstanceOf(ApiNotDeployableException.class)
            .hasMessage("The API {api-1} cannot be deployed or started because an endpoint names no cluster");
    }

    @Test
    void should_throw_when_a_message_api_connection_has_a_security_message_apis_cannot_use() {
        Cluster trustAll = registeredCluster(ClusterLifecycleState.DEPLOYED)
            .toBuilder()
            .configuration(
                Map.of(
                    "connections",
                    List.of(
                        Map.of(
                            "crossId",
                            "sasl",
                            "name",
                            "Trust all",
                            "bootstrapServers",
                            "kafka:9094",
                            "security",
                            Map.of("protocol", "SSL", "ssl", Map.of("trustAll", true))
                        )
                    )
                )
            )
            .build();
        givenDeployedAndStored(trustAll);

        assertThatThrownBy(() -> service.validateDeployable("api-1", ENV_ID, ApiType.MESSAGE, messageDefinition("sasl")))
            .isInstanceOf(ApiNotDeployableException.class)
            .hasMessageStartingWith("The API {api-1} cannot be deployed or started: ")
            .hasMessageContaining("Trust all is not supported: give a truststore.");
    }

    @Test
    void should_be_noop_for_message_api_in_direct_mode() {
        assertThatCode(() ->
            service.validateDeployable("api-1", ENV_ID, ApiType.MESSAGE, directMessageDefinition())
        ).doesNotThrowAnyException();
    }

    @Nested
    class Startable {

        @Test
        void should_throw_when_a_native_api_connection_is_not_in_the_deployed_cluster() {
            givenDeployedAndStored(registeredCluster(ClusterLifecycleState.DEPLOYED));

            assertThatThrownBy(() ->
                service.validateStartable(
                    "api-1",
                    ENV_ID,
                    ApiType.NATIVE,
                    nativeEndpointGroups("native-kafka-cluster", connection("plain"))
                )
            )
                .isInstanceOf(ApiNotDeployableException.class)
                .hasMessageContaining("its connection {plain} does not exist in the deployed cluster {prod}");
        }

        @Test
        void should_pass_when_a_native_api_connection_is_in_the_deployed_cluster() {
            givenDeployedAndStored(registeredCluster(ClusterLifecycleState.DEPLOYED));

            assertThatCode(() ->
                service.validateStartable("api-1", ENV_ID, ApiType.NATIVE, nativeEndpointGroups("native-kafka-cluster", connection("sasl")))
            ).doesNotThrowAnyException();
        }
    }

    private void givenStored(Cluster... clusters) {
        clusterQueryService.initWith(List.of(clusters));
    }

    private void givenDeployed(Cluster... clusters) {
        eventLatestQueryService.initWith(Arrays.stream(clusters).map(this::deployEvent).toList());
    }

    private void givenDeployedAndStored(Cluster... clusters) {
        givenStored(clusters);
        givenDeployed(clusters);
    }

    private Event deployEvent(Cluster cluster) {
        var properties = new EnumMap<Event.EventProperties, String>(Event.EventProperties.class);
        properties.put(Event.EventProperties.CLUSTER_ID, cluster.getCrossId());
        try {
            return Event.builder()
                .id("deploy-" + cluster.getCrossId())
                .type(EventType.DEPLOY_CLUSTER)
                .payload(objectMapper.writeValueAsString(cluster))
                .environments(Set.of(ENV_ID))
                .properties(properties)
                .createdAt(ZonedDateTime.now())
                .build();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static Cluster virtualCluster(ClusterLifecycleState lifecycleState) {
        return virtualCluster(lifecycleState, "sasl");
    }

    private static Cluster virtualCluster(ClusterLifecycleState lifecycleState, String backendConnectionCrossId) {
        return Cluster.builder()
            .id("cluster-id")
            .crossId(VC_CROSS_ID)
            .type(ClusterType.KAFKA_VIRTUAL_CLUSTER)
            .name("Mesh")
            .environmentId(ENV_ID)
            .lifecycleState(lifecycleState)
            .configuration(Map.of("backends", List.of(Map.of("clusterCrossId", "prod", "connectionCrossId", backendConnectionCrossId))))
            .build();
    }

    private static Cluster registeredCluster(ClusterLifecycleState lifecycleState) {
        return registeredCluster(lifecycleState, "sasl");
    }

    private static Cluster registeredCluster(ClusterLifecycleState lifecycleState, String... connectionCrossIds) {
        return Cluster.builder()
            .id("registered-cluster-id")
            .crossId("prod")
            .type(ClusterType.KAFKA_CLUSTER)
            .name("Production")
            .environmentId(ENV_ID)
            .lifecycleState(lifecycleState)
            .configuration(
                Map.of(
                    "connections",
                    Arrays.stream(connectionCrossIds)
                        .map(crossId -> Map.of("crossId", crossId, "name", crossId, "bootstrapServers", "kafka:9092"))
                        .toList()
                )
            )
            .build();
    }

    private static String connection(String connectionCrossId) {
        return "{\"clusterCrossId\":\"prod\",\"connectionCrossId\":\"" + connectionCrossId + "\"}";
    }

    private String registeredClusterBoundDefinition(String connectionCrossId) {
        return nativeDefinition("native-kafka-cluster", connection(connectionCrossId));
    }

    private String virtualClusterBoundDefinition() {
        return nativeDefinition("native-kafka-virtual-cluster", "{\"virtualClusterCrossId\":\"" + VC_CROSS_ID + "\"}");
    }

    private static List<NativeEndpointGroup> nativeEndpointGroups(String endpointType, String endpointConfiguration) {
        return List.of(
            NativeEndpointGroup.builder()
                .endpoints(List.of(NativeEndpoint.builder().type(endpointType).configuration(endpointConfiguration).build()))
                .build()
        );
    }

    private String nativeDefinition(String endpointType, String endpointConfiguration) {
        try {
            return objectMapper.writeValueAsString(
                NativeApi.builder().endpointGroups(nativeEndpointGroups(endpointType, endpointConfiguration)).build()
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String messageDefinition(String connectionCrossId) {
        return messageDefinition(
            "{\"mode\":\"CLUSTER\",\"clusterCrossId\":\"prod\"}",
            "{\"connectionCrossId\":\"" + connectionCrossId + "\"}"
        );
    }

    private String directMessageDefinition() {
        return messageDefinition("{\"security\":{\"protocol\":\"PLAINTEXT\"}}", "{\"bootstrapServers\":\"kafka:9092\"}");
    }

    private String messageDefinition(String sharedConfiguration, String endpointConfiguration) {
        try {
            return objectMapper.writeValueAsString(
                Api.builder()
                    .type(ApiType.MESSAGE)
                    .endpointGroups(
                        List.of(
                            EndpointGroup.builder()
                                .name("default-group")
                                .type("kafka")
                                .sharedConfiguration(sharedConfiguration)
                                .endpoints(
                                    List.of(
                                        Endpoint.builder()
                                            .name("default")
                                            .type("kafka")
                                            .inheritConfiguration(true)
                                            .configuration(endpointConfiguration)
                                            .build()
                                    )
                                )
                                .build()
                        )
                    )
                    .build()
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
