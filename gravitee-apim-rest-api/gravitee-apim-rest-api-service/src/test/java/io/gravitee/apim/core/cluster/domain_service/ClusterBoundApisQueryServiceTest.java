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

import com.fasterxml.jackson.databind.ObjectMapper;
import fixtures.core.model.ApiFixtures;
import inmemory.ApiQueryServiceInMemory;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.ApiFieldFilter;
import io.gravitee.apim.core.api.model.ApiSearchCriteria;
import io.gravitee.apim.core.api.model.Sortable;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.nativeapi.NativeApi;
import io.gravitee.definition.model.v4.nativeapi.NativeEndpoint;
import io.gravitee.definition.model.v4.nativeapi.NativeEndpointGroup;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ClusterBoundApisQueryServiceTest {

    private static final String ENV_ID = "env-id";
    private static final String OTHER_ENV_ID = "other-env-id";
    private static final String VC_CROSS_ID = "mesh";
    private static final String VC_ENDPOINT_TYPE = "native-kafka-virtual-cluster";
    private static final String CLUSTER_ENDPOINT_TYPE = "native-kafka-cluster";

    private final ApiQueryServiceInMemory apiQueryService = new ApiQueryServiceInMemory();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private ClusterBoundApisQueryService service;

    @BeforeEach
    void setUp() {
        service = new ClusterBoundApisQueryService(apiQueryService, objectMapper);
    }

    @Test
    void should_return_started_native_apis_bound_to_the_virtual_cluster() {
        apiQueryService.initWith(
            List.of(
                boundNativeApi("started-bound", ENV_ID, Api.LifecycleState.STARTED, VC_ENDPOINT_TYPE, vcConfig(VC_CROSS_ID)),
                boundNativeApi("stopped-bound", ENV_ID, Api.LifecycleState.STOPPED, VC_ENDPOINT_TYPE, vcConfig(VC_CROSS_ID)),
                boundNativeApi("started-other-vc", ENV_ID, Api.LifecycleState.STARTED, VC_ENDPOINT_TYPE, vcConfig("other-vc")),
                boundNativeApi("started-other-env", OTHER_ENV_ID, Api.LifecycleState.STARTED, VC_ENDPOINT_TYPE, vcConfig(VC_CROSS_ID)),
                startedProxyApi("started-non-native")
            )
        );

        var result = service.findStartedApisUsing(ENV_ID, use -> VC_CROSS_ID.equals(use.clusterCrossId()));

        assertThat(result).extracting(Api::getId).containsExactly("started-bound");
    }

    @Test
    void should_return_started_native_apis_bound_to_a_connection_of_a_registered_cluster() {
        apiQueryService.initWith(
            List.of(
                boundNativeApi("started-sasl", ENV_ID, Api.LifecycleState.STARTED, CLUSTER_ENDPOINT_TYPE, clusterConfig("prod", "sasl")),
                boundNativeApi("started-plain", ENV_ID, Api.LifecycleState.STARTED, CLUSTER_ENDPOINT_TYPE, clusterConfig("prod", "plain"))
            )
        );

        var result = service.findStartedApisUsing(ENV_ID, new ClusterBoundApisQueryService.ClusterUse("prod", "sasl")::equals);

        assertThat(result).extracting(Api::getId).containsExactly("started-sasl");
    }

    @Test
    void should_return_started_message_apis_bound_to_a_connection_of_a_registered_cluster() {
        apiQueryService.initWith(
            List.of(
                ApiFixtures.aMessageApiBoundToCluster("started-message", ENV_ID, Api.LifecycleState.STARTED, "prod", "sasl"),
                ApiFixtures.aMessageApiBoundToCluster("stopped-message", ENV_ID, Api.LifecycleState.STOPPED, "prod", "sasl"),
                ApiFixtures.aMessageApiV4()
                    .toBuilder()
                    .id("started-direct")
                    .environmentId(ENV_ID)
                    .lifecycleState(Api.LifecycleState.STARTED)
                    .build()
            )
        );

        var result = service.findStartedApisUsing(ENV_ID, new ClusterBoundApisQueryService.ClusterUse("prod", "sasl")::equals);

        assertThat(result).extracting(Api::getId).containsExactly("started-message");
    }

    @Test
    void should_only_ask_the_store_for_the_started_api_types_that_can_use_a_cluster() {
        var criteria = new ArrayList<ApiSearchCriteria>();
        var capturingQueryService = new ApiQueryServiceInMemory() {
            @Override
            public Stream<Api> search(ApiSearchCriteria apiCriteria, Sortable sortable, ApiFieldFilter apiFieldFilter) {
                criteria.add(apiCriteria);
                return super.search(apiCriteria, sortable, apiFieldFilter);
            }
        };
        capturingQueryService.initWith(List.of(startedProxyApi("started-proxy")));

        var result = new ClusterBoundApisQueryService(capturingQueryService, objectMapper).findStartedApisUsing(ENV_ID, use -> true);

        assertThat(result).isEmpty();
        assertThat(criteria)
            .singleElement()
            .satisfies(c -> {
                assertThat(c.getEnvironmentId()).isEqualTo(ENV_ID);
                assertThat(c.getState()).isEqualTo(Api.LifecycleState.STARTED);
                assertThat(c.getApiTypes()).containsExactlyInAnyOrder(ApiType.NATIVE, ApiType.MESSAGE);
            });
    }

    @Test
    void should_sort_the_apis_by_name() {
        apiQueryService.initWith(
            List.of(
                ApiFixtures.aMessageApiBoundToCluster("payments", ENV_ID, Api.LifecycleState.STARTED, "prod", "sasl"),
                ApiFixtures.aMessageApiBoundToCluster("Audit", ENV_ID, Api.LifecycleState.STARTED, "prod", "sasl"),
                ApiFixtures.aNativeApiBoundToCluster("orders", ENV_ID, Api.LifecycleState.STARTED, "prod", "sasl")
            )
        );

        var result = service.findStartedApisUsing(ENV_ID, use -> true);

        assertThat(result).extracting(Api::getName).containsExactly("Audit", "orders", "payments");
    }

    @Test
    void should_return_empty_when_no_started_api_is_bound() {
        apiQueryService.initWith(
            List.of(boundNativeApi("stopped-bound", ENV_ID, Api.LifecycleState.STOPPED, VC_ENDPOINT_TYPE, vcConfig(VC_CROSS_ID)))
        );

        assertThat(service.findStartedApisUsing(ENV_ID, use -> true)).isEmpty();
    }

    @Test
    void should_tolerate_malformed_endpoint_configuration_without_throwing() {
        apiQueryService.initWith(
            List.of(boundNativeApi("started-malformed", ENV_ID, Api.LifecycleState.STARTED, VC_ENDPOINT_TYPE, "{ not-json"))
        );

        assertThatCode(() -> assertThat(service.findStartedApisUsing(ENV_ID, use -> true)).isEmpty()).doesNotThrowAnyException();
    }

    private static String clusterConfig(String clusterCrossId, String connectionCrossId) {
        return "{\"clusterCrossId\":\"" + clusterCrossId + "\",\"connectionCrossId\":\"" + connectionCrossId + "\"}";
    }

    private static String vcConfig(String crossId) {
        return "{\"virtualClusterCrossId\":\"" + crossId + "\"}";
    }

    private static Api boundNativeApi(
        String id,
        String environmentId,
        Api.LifecycleState state,
        String endpointType,
        String endpointConfiguration
    ) {
        return ApiFixtures.aNativeApi()
            .toBuilder()
            .id(id)
            .name(id)
            .environmentId(environmentId)
            .lifecycleState(state)
            .apiDefinitionValue(
                NativeApi.builder()
                    .id(id)
                    .name(id)
                    .type(ApiType.NATIVE)
                    .endpointGroups(
                        List.of(
                            NativeEndpointGroup.builder()
                                .name("default-group")
                                .type("native-kafka")
                                .endpoints(
                                    List.of(
                                        NativeEndpoint.builder()
                                            .name("default-endpoint")
                                            .type(endpointType)
                                            .configuration(endpointConfiguration)
                                            .build()
                                    )
                                )
                                .build()
                        )
                    )
                    .build()
            )
            .build();
    }

    private static Api startedProxyApi(String id) {
        return ApiFixtures.aProxyApiV4().toBuilder().id(id).environmentId(ENV_ID).lifecycleState(Api.LifecycleState.STARTED).build();
    }
}
