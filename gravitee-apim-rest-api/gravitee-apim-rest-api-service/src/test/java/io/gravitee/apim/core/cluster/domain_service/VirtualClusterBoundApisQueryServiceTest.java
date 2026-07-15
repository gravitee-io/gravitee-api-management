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
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.nativeapi.NativeApi;
import io.gravitee.definition.model.v4.nativeapi.NativeEndpoint;
import io.gravitee.definition.model.v4.nativeapi.NativeEndpointGroup;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VirtualClusterBoundApisQueryServiceTest {

    private static final String ENV_ID = "env-id";
    private static final String OTHER_ENV_ID = "other-env-id";
    private static final String VC_CROSS_ID = "mesh";
    private static final String VC_ENDPOINT_TYPE = "native-kafka-virtual-cluster";

    private final ApiQueryServiceInMemory apiQueryService = new ApiQueryServiceInMemory();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private VirtualClusterBoundApisQueryService service;

    @BeforeEach
    void setUp() {
        service = new VirtualClusterBoundApisQueryService(apiQueryService, objectMapper);
    }

    @Test
    void should_return_started_native_apis_bound_to_the_virtual_cluster() {
        apiQueryService.initWith(
            List.of(
                boundNativeApi("started-bound", ENV_ID, Api.LifecycleState.STARTED, VC_ENDPOINT_TYPE, vcConfig(VC_CROSS_ID)),
                boundNativeApi("stopped-bound", ENV_ID, Api.LifecycleState.STOPPED, VC_ENDPOINT_TYPE, vcConfig(VC_CROSS_ID)),
                boundNativeApi("started-other-vc", ENV_ID, Api.LifecycleState.STARTED, VC_ENDPOINT_TYPE, vcConfig("other-vc")),
                // managed-cluster binding must be ignored: endpoint type is not the virtual-cluster one
                boundNativeApi(
                    "started-managed-cluster",
                    ENV_ID,
                    Api.LifecycleState.STARTED,
                    "native-kafka-cluster",
                    "{\"clusterCrossId\":\"mesh\"}"
                ),
                boundNativeApi("started-other-env", OTHER_ENV_ID, Api.LifecycleState.STARTED, VC_ENDPOINT_TYPE, vcConfig(VC_CROSS_ID)),
                startedProxyApi("started-non-native")
            )
        );

        var result = service.findStartedBoundApis(ENV_ID, VC_CROSS_ID);

        assertThat(result).extracting(Api::getId).containsExactly("started-bound");
    }

    @Test
    void should_return_empty_when_no_started_api_is_bound() {
        apiQueryService.initWith(
            List.of(boundNativeApi("stopped-bound", ENV_ID, Api.LifecycleState.STOPPED, VC_ENDPOINT_TYPE, vcConfig(VC_CROSS_ID)))
        );

        assertThat(service.findStartedBoundApis(ENV_ID, VC_CROSS_ID)).isEmpty();
    }

    @Test
    void should_tolerate_malformed_endpoint_configuration_without_throwing() {
        apiQueryService.initWith(
            List.of(boundNativeApi("started-malformed", ENV_ID, Api.LifecycleState.STARTED, VC_ENDPOINT_TYPE, "{ not-json"))
        );

        assertThatCode(() -> assertThat(service.findStartedBoundApis(ENV_ID, VC_CROSS_ID)).isEmpty()).doesNotThrowAnyException();
    }

    @Test
    void should_return_empty_for_null_cross_id() {
        apiQueryService.initWith(
            List.of(boundNativeApi("started-bound", ENV_ID, Api.LifecycleState.STARTED, VC_ENDPOINT_TYPE, vcConfig(VC_CROSS_ID)))
        );

        assertThat(service.findStartedBoundApis(ENV_ID, null)).isEmpty();
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
