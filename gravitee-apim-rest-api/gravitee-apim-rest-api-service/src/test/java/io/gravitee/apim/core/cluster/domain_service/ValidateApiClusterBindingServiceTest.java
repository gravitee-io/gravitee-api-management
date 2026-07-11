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
import io.gravitee.apim.core.cluster.model.ClusterLifecycleState;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.cluster.ClusterType;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.nativeapi.NativeApi;
import io.gravitee.definition.model.v4.nativeapi.NativeEndpoint;
import io.gravitee.definition.model.v4.nativeapi.NativeEndpointGroup;
import io.gravitee.rest.api.service.exceptions.ApiNotDeployableException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ValidateApiClusterBindingServiceTest {

    private static final String ENV_ID = "env-id";
    private static final String VC_CROSS_ID = "mesh";

    private final ClusterQueryServiceInMemory clusterQueryService = new ClusterQueryServiceInMemory();
    private final ObjectMapper objectMapper = new GraviteeMapper();

    private ValidateApiClusterBindingService service;

    @BeforeEach
    void setUp() {
        service = new ValidateApiClusterBindingService(clusterQueryService, objectMapper);
        clusterQueryService.reset();
    }

    @Test
    void should_throw_when_bound_virtual_cluster_is_missing() {
        assertThatThrownBy(() -> service.validateDeployable("api-1", ENV_ID, ApiType.NATIVE, boundDefinition(VC_CROSS_ID)))
            .isInstanceOf(ApiNotDeployableException.class)
            .hasMessageContaining(VC_CROSS_ID);
    }

    @Test
    void should_throw_when_bound_virtual_cluster_is_not_deployed() {
        clusterQueryService.initWith(List.of(virtualCluster(ClusterLifecycleState.UNDEPLOYED)));

        assertThatThrownBy(() -> service.validateDeployable("api-1", ENV_ID, ApiType.NATIVE, boundDefinition(VC_CROSS_ID))).isInstanceOf(
            ApiNotDeployableException.class
        );
    }

    @Test
    void should_pass_when_bound_virtual_cluster_is_deployed() {
        clusterQueryService.initWith(List.of(virtualCluster(ClusterLifecycleState.DEPLOYED)));

        assertThatCode(() ->
            service.validateDeployable("api-1", ENV_ID, ApiType.NATIVE, boundDefinition(VC_CROSS_ID))
        ).doesNotThrowAnyException();
    }

    @Test
    void should_be_noop_for_non_native_api() {
        assertThatCode(() ->
            service.validateDeployable("api-1", ENV_ID, ApiType.PROXY, boundDefinition(VC_CROSS_ID))
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

    private Cluster virtualCluster(ClusterLifecycleState lifecycleState) {
        return Cluster.builder()
            .id("cluster-id")
            .crossId(VC_CROSS_ID)
            .type(ClusterType.KAFKA_VIRTUAL_CLUSTER)
            .name("Mesh")
            .environmentId(ENV_ID)
            .lifecycleState(lifecycleState)
            .build();
    }

    private String boundDefinition(String vcCrossId) {
        try {
            return objectMapper.writeValueAsString(
                NativeApi.builder()
                    .endpointGroups(
                        List.of(
                            NativeEndpointGroup.builder()
                                .endpoints(
                                    List.of(
                                        NativeEndpoint.builder()
                                            .type("native-kafka-virtual-cluster")
                                            .configuration("{\"virtualClusterCrossId\":\"" + vcCrossId + "\"}")
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
