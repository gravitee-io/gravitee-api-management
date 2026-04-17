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
package io.gravitee.rest.api.management.v2.rest.mapper;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import fixtures.ApiFixtures;
import fixtures.FlowFixtures;
import fixtures.ListenerFixtures;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.Plugin;
import io.gravitee.definition.model.v4.failover.Failover;
import io.gravitee.definition.model.v4.listener.ListenerType;
import io.gravitee.definition.model.v4.service.ApiServices;
import io.gravitee.rest.api.management.v2.rest.model.Analytics;
import io.gravitee.rest.api.management.v2.rest.model.ApiType;
import io.gravitee.rest.api.management.v2.rest.model.BaseOriginContext;
import io.gravitee.rest.api.management.v2.rest.model.CreateApiV4;
import io.gravitee.rest.api.management.v2.rest.model.FailoverV4;
import io.gravitee.rest.api.management.v2.rest.model.IntegrationOriginContext;
import io.gravitee.rest.api.management.v2.rest.model.KubernetesOriginContext;
import io.gravitee.rest.api.management.v2.rest.model.Listener;
import io.gravitee.rest.api.management.v2.rest.model.ManagementOriginContext;
import io.gravitee.rest.api.management.v2.rest.model.Visibility;
import io.gravitee.rest.api.model.context.OriginContext;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mapstruct.factory.Mappers;
import org.mockito.Mockito;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class ApiMapperTest {

    private final ApiMapper apiMapper = Mappers.getMapper(ApiMapper.class);
    private static final String API_ID = "api-id";

    @Test
    void should_map_to_update_api_entity_v4() {
        var updateApi = ApiFixtures.anUpdateApiV4();
        updateApi.failover(
            new FailoverV4().enabled(true).perSubscription(false).maxFailures(3).openStateDuration(11000L).slowCallDuration(500L)
        );

        var updateApiEntity = apiMapper.map(updateApi, "api-id");
        assertThat(updateApiEntity).isNotNull();
        assertThat(updateApiEntity.getId()).isEqualTo("api-id");
        assertThat(updateApiEntity.getCrossId()).isNull();
        assertThat(updateApiEntity.getName()).isEqualTo(updateApi.getName());
        assertThat(updateApiEntity.getApiVersion()).isEqualTo(updateApi.getApiVersion());
        assertThat(updateApiEntity.getDefinitionVersion().name()).isEqualTo(updateApi.getDefinitionVersion().name());
        assertThat(updateApiEntity.getType().name()).isEqualTo(updateApi.getType().name());
        assertThat(updateApiEntity.getDescription()).isEqualTo(updateApi.getDescription());
        assertThat(new ArrayList<>(updateApiEntity.getTags())).isEqualTo(updateApi.getTags());
        assertThat(updateApiEntity.getListeners()).isNotNull(); // Tested in ListenerMapperTest
        assertThat(updateApiEntity.getEndpointGroups()).isNotNull(); // Tested in EndpointMapperTest
        assertThat(updateApiEntity.getAnalytics()).isNotNull();
        assertThat(updateApiEntity.getProperties()).isNotNull(); // Tested in PropertiesMapperTest
        assertThat(updateApiEntity.getResources()).isNotNull(); // Tested in ResourceMapperTest
        assertThat(updateApiEntity.getPlans().size()).isEqualTo(0);
        assertThat(updateApiEntity.getFlowExecution()).usingRecursiveAssertion().isEqualTo(updateApi.getFlowExecution());
        assertThat(updateApiEntity.getFlows()).isNotNull(); // Tested in FlowMapperTest
        assertThat(updateApiEntity.getResponseTemplates()).isNotNull();
        assertThat(updateApiEntity.getServices()).isNotNull(); // Tested in ServiceMapperTest
        assertThat(new ArrayList<>(updateApiEntity.getGroups())).isEqualTo(updateApi.getGroups());
        assertThat(updateApiEntity.getVisibility().name()).isEqualTo(updateApi.getVisibility().name());
        assertThat(new ArrayList<>(updateApiEntity.getCategories())).isEqualTo(updateApi.getCategories());
        assertThat(updateApiEntity.getLabels()).isEqualTo(updateApi.getLabels());
        assertThat(updateApiEntity.getMetadata()).isNull();
        assertThat(updateApiEntity.getLifecycleState().name()).isEqualTo(updateApi.getLifecycleState().name());
        assertThat(updateApiEntity.isDisableMembershipNotifications()).isEqualTo(updateApi.getDisableMembershipNotifications());
        assertThat(updateApiEntity.getFailover()).isEqualTo(
            Failover.builder().enabled(true).perSubscription(false).maxFailures(3).openStateDuration(11000).slowCallDuration(500).build()
        );
    }

    @Test
    void should_map_to_update_api_entity_v2() {
        var updateApi = ApiFixtures.anUpdateApiV2();

        var updateApiEntity = apiMapper.map(updateApi);
        assertThat(updateApiEntity).isNotNull();
        assertThat(updateApiEntity.getCrossId()).isNull();
        assertThat(updateApiEntity.getName()).isEqualTo(updateApi.getName());
        assertThat(updateApiEntity.getVersion()).isEqualTo(updateApi.getApiVersion());
        assertThat(updateApiEntity.getGraviteeDefinitionVersion()).isEqualTo(DefinitionVersion.V2.getLabel());
        assertThat(updateApiEntity.getDescription()).isEqualTo(updateApi.getDescription());
        assertThat(new ArrayList<>(updateApiEntity.getTags())).isEqualTo(updateApi.getTags());
        assertThat(updateApiEntity.getProxy()).isNotNull(); // To be tested in ProxyMapperTest ?
        assertThat(updateApiEntity.getProperties()).isNotNull(); // Tested in PropertiesMapperTest
        assertThat(updateApiEntity.getResources()).isNotNull(); // Tested in ResourceMapperTest
        assertThat(updateApiEntity.getPlans().size()).isEqualTo(0);
        assertThat(updateApiEntity.getFlowMode().name()).isEqualTo(updateApi.getFlowMode().getValue());
        assertThat(updateApiEntity.getFlows()).isNotNull(); // Tested in FlowMapperTest
        assertThat(updateApiEntity.getResponseTemplates()).isNotNull();
        assertThat(updateApiEntity.getServices()).isNotNull(); // Tested in ServiceMapperTest
        assertThat(new ArrayList<>(updateApiEntity.getGroups())).isEqualTo(updateApi.getGroups());
        assertThat(updateApiEntity.getVisibility().name()).isEqualTo(updateApi.getVisibility().name());
        assertThat(new ArrayList<>(updateApiEntity.getCategories())).isEqualTo(updateApi.getCategories());
        assertThat(updateApiEntity.getLabels()).isEqualTo(updateApi.getLabels());
        assertThat(updateApiEntity.getMetadata()).isNull();
        assertThat(updateApiEntity.getLifecycleState().name()).isEqualTo(updateApi.getLifecycleState().name());
        assertThat(updateApiEntity.isDisableMembershipNotifications()).isEqualTo(updateApi.getDisableMembershipNotifications());
        assertThat(updateApiEntity.getExecutionMode().name()).isEqualTo(updateApi.getExecutionMode().getValue());
    }

    @ParameterizedTest
    @EnumSource(Visibility.class)
    void should_map_create_api_v4_visibility(Visibility visibility) {
        CreateApiV4 newApi = new CreateApiV4();
        newApi.setName("Test API");
        newApi.setApiVersion("1.0.0");
        newApi.setDefinitionVersion(io.gravitee.rest.api.management.v2.rest.model.DefinitionVersion.V4);
        newApi.setType(io.gravitee.rest.api.management.v2.rest.model.ApiType.PROXY);
        newApi.setVisibility(visibility);
        newApi.setTags(Set.of("tag1"));
        var mapped = apiMapper.mapToNewHttpApi(newApi);
        assertThat(mapped).isNotNull();
        assertThat(mapped.getVisibility()).isEqualTo(Api.Visibility.valueOf(visibility.name()));
    }

    @ParameterizedTest
    @EnumSource(Visibility.class)
    void should_map_create_native_api_v4_visibility(Visibility visibility) {
        CreateApiV4 newApi = new CreateApiV4();
        newApi.setName("Test API");
        newApi.setApiVersion("1.0.0");
        newApi.setDefinitionVersion(io.gravitee.rest.api.management.v2.rest.model.DefinitionVersion.V4);
        newApi.setType(ApiType.NATIVE);
        newApi.setVisibility(visibility);
        newApi.setTags(Set.of("tag1"));

        var mapped = apiMapper.mapToNewNativeApi(newApi);
        assertThat(mapped).isNotNull();
        assertThat(mapped.getVisibility()).isEqualTo(Api.Visibility.valueOf(visibility.name()));
    }

    @Test
    void map_to_http_v4_preserves_nested_definition_fields() {
        var uriInfo = Mockito.mock(UriInfo.class);
        Mockito.when(uriInfo.getBaseUriBuilder()).thenReturn(UriBuilder.fromUri("http://localhost/"));

        var baseApi = fixtures.core.model.ApiFixtures.aProxyApiV4();
        var api = baseApi
            .toBuilder()
            .apiDefinitionValue(
                baseApi
                    .getApiDefinitionHttpV4()
                    .toBuilder()
                    .failover(Failover.builder().enabled(true).maxFailures(3).build())
                    .services(new ApiServices())
                    .allowedInApiProducts(true)
                    .build()
            )
            .build();

        var apiV4 = apiMapper.mapToHttpV4(api, uriInfo, null);

        assertThat(apiV4.getFailover()).isNotNull();
        assertThat(apiV4.getFailover().getEnabled()).isTrue();
        assertThat(apiV4.getServices()).isNotNull();
        assertThat(apiV4.getAllowedInApiProducts()).isTrue();
    }

    @Test
    void map_to_http_v4_propagates_null_response_templates() {
        var uriInfo = Mockito.mock(UriInfo.class);
        Mockito.when(uriInfo.getBaseUriBuilder()).thenReturn(UriBuilder.fromUri("http://localhost/"));

        var baseApi = fixtures.core.model.ApiFixtures.aProxyApiV4();
        var api = baseApi
            .toBuilder()
            .apiDefinitionValue(baseApi.getApiDefinitionHttpV4().toBuilder().responseTemplates(null).build())
            .build();

        var apiV4 = apiMapper.mapToHttpV4(api, uriInfo, null);

        assertThat(apiV4.getResponseTemplates()).isNull();
    }

    @Test
    void map_to_http_v4_propagates_non_null_response_templates() {
        var uriInfo = Mockito.mock(UriInfo.class);
        Mockito.when(uriInfo.getBaseUriBuilder()).thenReturn(UriBuilder.fromUri("http://localhost/"));

        var template = new io.gravitee.definition.model.ResponseTemplate();
        template.setStatusCode(400);
        var templates = java.util.Map.of("FOO", java.util.Map.of("application/json", template));

        var baseApi = fixtures.core.model.ApiFixtures.aProxyApiV4();
        var api = baseApi
            .toBuilder()
            .apiDefinitionValue(baseApi.getApiDefinitionHttpV4().toBuilder().responseTemplates(templates).build())
            .build();

        var apiV4 = apiMapper.mapToHttpV4(api, uriInfo, null);

        assertThat(apiV4.getResponseTemplates()).isNotNull();
        assertThat(apiV4.getResponseTemplates().containsKey("FOO")).isTrue();
    }

    @Nested
    class MapToV4AllowedInApiProducts {

        private UriInfo uriInfo;

        @org.junit.jupiter.api.BeforeEach
        void setUp() {
            uriInfo = Mockito.mock(UriInfo.class);
            Mockito.when(uriInfo.getBaseUriBuilder()).thenReturn(UriBuilder.fromUri("http://localhost/"));
        }

        @Test
        void coalesces_null_to_false() {
            var base = fixtures.core.model.ApiFixtures.aProxyApiV4();
            var api = base
                .toBuilder()
                .apiDefinitionValue(base.getApiDefinitionHttpV4().toBuilder().allowedInApiProducts(null).build())
                .build();

            var result = apiMapper.mapToV4(api, uriInfo, null);

            assertThat(result.getAllowedInApiProducts()).isFalse();
        }

        @Test
        void preserves_true() {
            var base = fixtures.core.model.ApiFixtures.aProxyApiV4();
            var api = base
                .toBuilder()
                .apiDefinitionValue(base.getApiDefinitionHttpV4().toBuilder().allowedInApiProducts(true).build())
                .build();

            var result = apiMapper.mapToV4(api, uriInfo, null);

            assertThat(result.getAllowedInApiProducts()).isTrue();
        }

        @Test
        void preserves_false() {
            var base = fixtures.core.model.ApiFixtures.aProxyApiV4();
            var api = base
                .toBuilder()
                .apiDefinitionValue(base.getApiDefinitionHttpV4().toBuilder().allowedInApiProducts(false).build())
                .build();

            var result = apiMapper.mapToV4(api, uriInfo, null);

            assertThat(result.getAllowedInApiProducts()).isFalse();
        }
    }

    @Nested
    class ComputeOriginContext {

        static Stream<Arguments> computeOriginContext() {
            return Stream.of(
                arguments(new OriginContext.Management(), new ManagementOriginContext().origin(BaseOriginContext.OriginEnum.MANAGEMENT)),
                arguments(
                    new OriginContext.Kubernetes(OriginContext.Kubernetes.Mode.FULLY_MANAGED),
                    new KubernetesOriginContext()
                        .mode(KubernetesOriginContext.ModeEnum.FULLY_MANAGED)
                        .syncFrom(KubernetesOriginContext.SyncFromEnum.KUBERNETES)
                        .origin(BaseOriginContext.OriginEnum.KUBERNETES)
                ),
                arguments(
                    new OriginContext.Kubernetes(OriginContext.Kubernetes.Mode.FULLY_MANAGED, "MANAGEMENT"),
                    new KubernetesOriginContext()
                        .mode(KubernetesOriginContext.ModeEnum.FULLY_MANAGED)
                        .syncFrom(KubernetesOriginContext.SyncFromEnum.MANAGEMENT)
                        .origin(BaseOriginContext.OriginEnum.KUBERNETES)
                ),
                arguments(
                    new OriginContext.Kubernetes(OriginContext.Kubernetes.Mode.FULLY_MANAGED, "KUBERNETES"),
                    new KubernetesOriginContext()
                        .mode(KubernetesOriginContext.ModeEnum.FULLY_MANAGED)
                        .syncFrom(KubernetesOriginContext.SyncFromEnum.KUBERNETES)
                        .origin(BaseOriginContext.OriginEnum.KUBERNETES)
                ),
                arguments(
                    new OriginContext.Integration("integID"),
                    new IntegrationOriginContext().integrationId("integID").origin(BaseOriginContext.OriginEnum.INTEGRATION)
                ),
                arguments(
                    new OriginContext.Integration("integID", "inte name", "provider"),
                    new IntegrationOriginContext()
                        .integrationId("integID")
                        .provider("provider")
                        .integrationName("inte name")
                        .origin(BaseOriginContext.OriginEnum.INTEGRATION)
                )
            );
        }

        @ParameterizedTest
        @MethodSource("computeOriginContext")
        void compute_origin_context(OriginContext input, BaseOriginContext expected) {
            // Given
            GenericApiEntity api = new ApiEntity().withOriginContext(input);

            // When
            var context = ApiMapper.INSTANCE.computeOriginContext(api);

            // Then
            assertThat(context).isEqualTo(expected);
        }
    }

    @Nested
    class ToApiExport {

        @Test
        void should_map_to_native_api() {
            var apiV4 = ApiFixtures.anApiV4();
            apiV4.setId(API_ID);
            apiV4.setListeners(List.of(new Listener(ListenerFixtures.aKafkaListener())));
            apiV4.setFlows(List.of(FlowFixtures.aFlowNativeV4()));
            apiV4.setType(ApiType.NATIVE);
            Analytics analytics = new Analytics();
            analytics.setEnabled(true);
            apiV4.setAnalytics(analytics);

            var result = ApiMapper.INSTANCE.toApiExport(apiV4);
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result).isNotNull();
                softly.assertThat(result.getId()).isEqualTo(API_ID);
                softly.assertThat(result.getType()).isEqualTo(io.gravitee.definition.model.v4.ApiType.NATIVE);

                var firstListener = result.getListeners().getFirst();
                softly.assertThat(firstListener).isNotNull();
                softly.assertThat(firstListener.getType()).isEqualTo(ListenerType.KAFKA);
                softly.assertThat(firstListener.getPlugins()).isEqualTo(List.of(new Plugin("entrypoint-connector", "Entrypoint type")));

                var firstEntrypoint = firstListener.getEntrypoints().getFirst();
                softly.assertThat(firstEntrypoint).isNotNull();
                softly.assertThat(firstEntrypoint.getType()).isEqualTo("Entrypoint type");
                softly.assertThat(firstEntrypoint.getConfiguration()).isEqualTo("{\n  \"nice\" : \"configuration\"\n}");
                softly.assertThat(firstEntrypoint.getPlugins()).isEqualTo(List.of(new Plugin("entrypoint-connector", "Entrypoint type")));

                var expectedFlow = FlowFixtures.aModelFlowNativeV4().toBuilder().tags(Set.of("tag1", "tag2")).build();
                softly.assertThat(result.getFlows()).isNotNull().first().isEqualTo(expectedFlow);
                io.gravitee.definition.model.v4.analytics.Analytics mappedAnalytics = result.getAnalytics();
                softly.assertThat(mappedAnalytics.isEnabled()).isTrue();
            });
        }

        @Test
        void should_map_to_proxy_api() {
            var apiV4 = ApiFixtures.anApiV4();
            apiV4.setId(API_ID);
            apiV4.setListeners(List.of(new Listener(ListenerFixtures.aHttpListener())));
            apiV4.setFlows(List.of(FlowFixtures.aFlowHttpV4()));
            apiV4.setType(ApiType.PROXY);

            var result = ApiMapper.INSTANCE.toApiExport(apiV4);
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result).isNotNull();
                softly.assertThat(result.getId()).isEqualTo(API_ID);
                softly.assertThat(result.getType()).isEqualTo(io.gravitee.definition.model.v4.ApiType.PROXY);

                var firstListener = result.getListeners().getFirst();
                softly.assertThat(firstListener).isNotNull();
                softly.assertThat(firstListener.getType()).isEqualTo(ListenerType.HTTP);
                softly.assertThat(firstListener.getPlugins()).isEqualTo(List.of(new Plugin("entrypoint-connector", "Entrypoint type")));

                var firstEntrypoint = firstListener.getEntrypoints().getFirst();
                softly.assertThat(firstEntrypoint).isNotNull();
                softly.assertThat(firstEntrypoint.getType()).isEqualTo("Entrypoint type");
                softly.assertThat(firstEntrypoint.getConfiguration()).isEqualTo("{\n  \"nice\" : \"configuration\"\n}");
                softly.assertThat(firstEntrypoint.getPlugins()).isEqualTo(List.of(new Plugin("entrypoint-connector", "Entrypoint type")));

                var expectedFlow = FlowFixtures.aModelFlowHttpV4().toBuilder().tags(Set.of("tag1", "tag2")).build();
                softly.assertThat(result.getFlows()).isNotNull().first().isEqualTo(expectedFlow);
            });
        }
    }
}
