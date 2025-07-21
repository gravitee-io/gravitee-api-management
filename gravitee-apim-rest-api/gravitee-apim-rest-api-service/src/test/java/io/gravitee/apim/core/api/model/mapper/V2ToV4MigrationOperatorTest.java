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
package io.gravitee.apim.core.api.model.mapper;

import static io.gravitee.definition.model.v4.endpointgroup.loadbalancer.LoadBalancerType.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import fixtures.core.model.ApiFixtures;
import fixtures.core.model.PlanFixtures;
import io.gravitee.apim.core.api.model.utils.MigrationResult;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.Endpoint;
import io.gravitee.definition.model.EndpointGroup;
import io.gravitee.definition.model.LoadBalancer;
import io.gravitee.definition.model.LoadBalancerType;
import io.gravitee.definition.model.Proxy;
import io.gravitee.definition.model.VirtualHost;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.listener.ListenerType;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class V2ToV4MigrationOperatorTest {

    private V2toV4MigrationOperator mapper;

    @BeforeEach
    void setUp() {
        mapper = new V2toV4MigrationOperator();
    }

    @Nested
    class ApiMigratingTest {

        @Test
        void should_preserve_api_metadata_when_mapping() {
            // Given
            var originalApi = ApiFixtures.aProxyApiV2();

            // When
            var result = get(mapper.mapApi(originalApi));

            // Then
            assertThat(result.getId()).isEqualTo(originalApi.getId());
            assertThat(result.getName()).isEqualTo(originalApi.getName());
            assertThat(result.getDescription()).isEqualTo(originalApi.getDescription());
            assertThat(result.getVersion()).isEqualTo(originalApi.getVersion());
            assertThat(result.getEnvironmentId()).isEqualTo(originalApi.getEnvironmentId());
            assertThat(result.getCrossId()).isEqualTo(originalApi.getCrossId());
            assertThat(result.getHrid()).isEqualTo(originalApi.getHrid());
            assertThat(result.getOriginContext()).isEqualTo(originalApi.getOriginContext());
            assertThat(result.getCreatedAt()).isEqualTo(originalApi.getCreatedAt());
            assertThat(result.getUpdatedAt()).isEqualTo(originalApi.getUpdatedAt());
            assertThat(result.getDeployedAt()).isEqualTo(originalApi.getDeployedAt());
            assertThat(result.getVisibility()).isEqualTo(originalApi.getVisibility());
            assertThat(result.getLifecycleState()).isEqualTo(originalApi.getLifecycleState());
            assertThat(result.getPicture()).isEqualTo(originalApi.getPicture());
            assertThat(result.getGroups()).isEqualTo(originalApi.getGroups());
            assertThat(result.getCategories()).isEqualTo(originalApi.getCategories());
            assertThat(result.getLabels()).isEqualTo(originalApi.getLabels());
            assertThat(result.isDisableMembershipNotifications()).isEqualTo(originalApi.isDisableMembershipNotifications());
            assertThat(result.getApiLifecycleState()).isEqualTo(originalApi.getApiLifecycleState());
            assertThat(result.getBackground()).isEqualTo(originalApi.getBackground());
        }

        @Test
        void should_upgrade_definition_version_to_v4() {
            var v2Api = ApiFixtures.aProxyApiV2().toBuilder().definitionVersion(DefinitionVersion.V2).build();

            var result = get(mapper.mapApi(v2Api));

            assertThat(result.getDefinitionVersion()).isEqualTo(DefinitionVersion.V4);
            assertThat(result.getType()).isEqualTo(ApiType.PROXY);
        }
    }

    @Nested
    class ListenerMigratingTest {

        @ParameterizedTest
        @ValueSource(strings = { "/api", "/v1/users", "/health/check", "/", "/api/", "/test/" })
        void path_should_end_with_slash(String inputPath) {
            // Given
            var virtualHost = new VirtualHost();
            virtualHost.setPath(inputPath);

            var proxy = new Proxy();
            proxy.setVirtualHosts(List.of(virtualHost));

            var apiDef = new io.gravitee.definition.model.Api();
            apiDef.setId("test-api");
            apiDef.setName("Test API");
            apiDef.setVersion("1.0");
            apiDef.setProxy(proxy);

            var api = ApiFixtures.aProxyApiV2().toBuilder().apiDefinition(apiDef).build();

            // When
            var result = get(mapper.mapApi(api));

            // Then
            if (result.getApiDefinitionHttpV4().getListeners().getFirst() instanceof HttpListener httpListener) {
                assertThat(httpListener.getPaths().getFirst().getPath()).endsWith("/");
            } else {
                fail("Listener is not an HttpListener");
            }
        }

        @Test
        void should_preserve_virtual_host_entrypoint_property() {
            var virtualHost = new VirtualHost();
            virtualHost.setPath("/api");
            virtualHost.setHost("api.example.com");

            var proxy = new Proxy();
            proxy.setVirtualHosts(List.of(virtualHost));

            var apiDef = new io.gravitee.definition.model.Api();
            apiDef.setId("test-api");
            apiDef.setName("Test API");
            apiDef.setVersion("1.0");
            apiDef.setProxy(proxy);

            var api = ApiFixtures.aProxyApiV2().toBuilder().apiDefinition(apiDef).build();

            var result = get(mapper.mapApi(api));

            var httpListener = (HttpListener) result.getApiDefinitionHttpV4().getListeners().getFirst();
            var path = httpListener.getPaths().getFirst();

            assertThat(path.getHost()).isEqualTo("api.example.com");
        }

        @Test
        void should_create_http_listener_with_correct_properties() {
            var virtualHost1 = new VirtualHost();
            virtualHost1.setPath("/api/v1");
            virtualHost1.setHost("api.example.com");

            var virtualHost2 = new VirtualHost();
            virtualHost2.setPath("/api/v2");
            virtualHost2.setHost("api2.example.com");

            var proxy = new Proxy();
            proxy.setVirtualHosts(List.of(virtualHost1, virtualHost2));
            proxy.setServers(List.of("server1", "server2"));

            var apiDef = new io.gravitee.definition.model.Api();
            apiDef.setId("test-api");
            apiDef.setName("Test API");
            apiDef.setVersion("1.0");
            apiDef.setProxy(proxy);

            var api = ApiFixtures.aProxyApiV2().toBuilder().apiDefinition(apiDef).build();

            var result = get(mapper.mapApi(api));

            assertThat(result.getApiDefinitionHttpV4().getListeners()).hasSize(1);
            var listener = result.getApiDefinitionHttpV4().getListeners().getFirst();
            assertThat(listener.getType()).isEqualTo(ListenerType.HTTP);

            var httpListener = (HttpListener) listener;
            assertThat(httpListener.getPaths()).hasSize(2);
            assertThat(httpListener.getServers()).containsExactly("server1", "server2");
            assertThat(httpListener.getEntrypoints()).hasSize(1);
            assertThat(httpListener.getEntrypoints().getFirst().getType()).isEqualTo("http-proxy");
        }
    }

    @Nested
    class ApiDefinitionMigratingTest {

        @ParameterizedTest
        @MethodSource("loadBalancerTypeProvider")
        void should_map_load_balancer_types_correctly(
            LoadBalancerType v2Type,
            io.gravitee.definition.model.v4.endpointgroup.loadbalancer.LoadBalancerType expectedV4Type
        ) {
            var endpoint = new Endpoint();
            endpoint.setName("test-endpoint");
            endpoint.setTarget("http://example.com");

            var loadBalancer = new LoadBalancer();
            loadBalancer.setType(v2Type);

            var endpointGroup = new EndpointGroup();
            endpointGroup.setName("default-group");
            endpointGroup.setEndpoints(Set.of(endpoint));
            endpointGroup.setLoadBalancer(loadBalancer);

            var proxy = new Proxy();
            proxy.setVirtualHosts(List.of(new VirtualHost()));
            proxy.setGroups(Set.of(endpointGroup));

            var apiDef = new io.gravitee.definition.model.Api();
            apiDef.setId("test-api");
            apiDef.setName("Test API");
            apiDef.setVersion("1.0");
            apiDef.setProxy(proxy);

            var api = ApiFixtures.aProxyApiV2().toBuilder().apiDefinition(apiDef).build();

            var result = get(mapper.mapApi(api));

            var mappedEndpointGroup = result.getApiDefinitionHttpV4().getEndpointGroups().getFirst();
            if (expectedV4Type != null) {
                assertThat(mappedEndpointGroup.getLoadBalancer().getType()).isEqualTo(expectedV4Type);
            } else {
                assertThat(mappedEndpointGroup.getLoadBalancer()).isNull();
            }
        }

        private static Stream<Arguments> loadBalancerTypeProvider() {
            return Stream.of(
                arguments(LoadBalancerType.RANDOM, RANDOM),
                arguments(LoadBalancerType.ROUND_ROBIN, ROUND_ROBIN),
                arguments(LoadBalancerType.WEIGHTED_RANDOM, WEIGHTED_RANDOM),
                arguments(LoadBalancerType.WEIGHTED_ROUND_ROBIN, WEIGHTED_ROUND_ROBIN),
                arguments(null, null)
            );
        }

        @Test
        void should_set_api_properties_correctly() {
            var apiDef = new io.gravitee.definition.model.Api();
            apiDef.setId("test-api-id");
            apiDef.setName("Test API Name");
            apiDef.setVersion("2.0.1");
            apiDef.setTags(Set.of("tag1", "tag2", "tag3"));
            apiDef.setProxy(new Proxy());

            var api = ApiFixtures.aProxyApiV2().toBuilder().apiDefinition(apiDef).build();

            var result = get(mapper.mapApi(api));
            var v4Definition = result.getApiDefinitionHttpV4();

            assertThat(v4Definition.getId()).isEqualTo("test-api-id");
            assertThat(v4Definition.getName()).isEqualTo("Test API Name");
            assertThat(v4Definition.getApiVersion()).isEqualTo("2.0.1");
            assertThat(v4Definition.getTags()).containsExactlyInAnyOrder("tag1", "tag2", "tag3");
            assertThat(v4Definition.getType()).isEqualTo(ApiType.PROXY);
            assertThat(v4Definition.getProperties()).isEmpty();
            assertThat(v4Definition.getResources()).isEmpty();
        }
    }

    @Nested
    class EndpointMigratingTest {

        @ParameterizedTest
        @ValueSource(booleans = { true, false })
        void should_preserve_endpoint_backup_as_secondary_property(boolean isBackup) {
            var endpoint = new Endpoint();
            endpoint.setName("test-endpoint");
            endpoint.setTarget("http://example.com");
            endpoint.setBackup(isBackup);
            endpoint.setWeight(10);
            endpoint.setTenants(List.of("tenant1", "tenant2"));

            var endpointGroup = new EndpointGroup();
            endpointGroup.setName("default-group");
            endpointGroup.setEndpoints(Set.of(endpoint));

            var proxy = new Proxy();
            proxy.setVirtualHosts(List.of(new VirtualHost()));
            proxy.setGroups(Set.of(endpointGroup));

            var apiDef = new io.gravitee.definition.model.Api();
            apiDef.setId("test-api");
            apiDef.setName("Test API");
            apiDef.setVersion("1.0");
            apiDef.setProxy(proxy);

            var api = ApiFixtures.aProxyApiV2().toBuilder().apiDefinition(apiDef).build();

            var result = get(mapper.mapApi(api));

            var mappedEndpoint = result.getApiDefinitionHttpV4().getEndpointGroups().getFirst().getEndpoints().getFirst();
            assertThat(mappedEndpoint.isSecondary()).isEqualTo(isBackup);
            assertThat(mappedEndpoint.getWeight()).isEqualTo(10);
            assertThat(mappedEndpoint.getTenants()).containsExactlyInAnyOrder("tenant1", "tenant2");
        }

        @Test
        void should_preserve_endpoint_configuration() {
            var endpoint = new Endpoint();
            endpoint.setName("test-endpoint");
            endpoint.setTarget("http://example.com");
            endpoint.setType("http");
            endpoint.setConfiguration("{\"target\": \"http://example.com\", \"ssl\": {\"trustStore\": true}}");

            var endpointGroup = new EndpointGroup();
            endpointGroup.setName("default-group");
            endpointGroup.setEndpoints(Set.of(endpoint));

            var proxy = new Proxy();
            proxy.setVirtualHosts(List.of(new VirtualHost()));
            proxy.setGroups(Set.of(endpointGroup));

            var apiDef = new io.gravitee.definition.model.Api();
            apiDef.setId("test-api");
            apiDef.setName("Test API");
            apiDef.setVersion("1.0");
            apiDef.setProxy(proxy);

            var api = ApiFixtures.aProxyApiV2().toBuilder().apiDefinition(apiDef).build();

            var result = get(mapper.mapApi(api));

            var mappedEndpoint = result.getApiDefinitionHttpV4().getEndpointGroups().getFirst().getEndpoints().getFirst();
            assertThat(mappedEndpoint.getName()).isEqualTo("test-endpoint");
            assertThat(mappedEndpoint.getType()).isEqualTo("http");
            assertThat(mappedEndpoint.getConfiguration()).contains("\"target\":\"http://example.com\"");
        }
    }

    @Nested
    class PlanMigratingTest {

        @ParameterizedTest
        @MethodSource("planStatusProvider")
        void should_map_plan_status_correctly(String v2Status, PlanStatus expectedV4Status) {
            var planDef = new io.gravitee.definition.model.Plan();
            planDef.setId("plan-id");
            planDef.setName("Test Plan");
            planDef.setStatus(v2Status);
            planDef.setSecurity("api-key");

            var plan = PlanFixtures.aPlanV2().toBuilder().id("plan-id").name("Test Plan").planDefinitionV2(planDef).build();

            var result = get(mapper.mapPlan(plan));

            assertThat(result.getPlanDefinitionHttpV4().getStatus()).isEqualTo(expectedV4Status);
            assertThat(result.getPlanDefinitionHttpV4().getMode()).isEqualTo(PlanMode.STANDARD);
            assertThat(result.getPlanDefinitionHttpV4().getSecurity().getType()).isEqualTo("api-key");
            assertThat(result.getDefinitionVersion()).isEqualTo(DefinitionVersion.V4);
            assertThat(result.getApiType()).isEqualTo(ApiType.PROXY);
        }

        private static Stream<Arguments> planStatusProvider() {
            return Stream.of(
                Arguments.of("STAGING", PlanStatus.STAGING),
                Arguments.of("PUBLISHED", PlanStatus.PUBLISHED),
                Arguments.of("CLOSED", PlanStatus.CLOSED),
                Arguments.of("DEPRECATED", PlanStatus.DEPRECATED)
            );
        }

        @ParameterizedTest
        @CsvSource({ "api-key, api-key", "oauth2, oauth2", "keyless, keyless", "jwt, jwt" })
        void should_preserve_plan_security_type(String v2SecurityType, String expectedV4SecurityType) {
            var planDef = new io.gravitee.definition.model.Plan();
            planDef.setId("plan-id");
            planDef.setName("Test Plan");
            planDef.setStatus("PUBLISHED");
            planDef.setSecurity(v2SecurityType);
            planDef.setSecurityDefinition("{\"config\": \"value\"}");

            var plan = PlanFixtures.aPlanV2().toBuilder().planDefinitionV2(planDef).build();

            var result = get(mapper.mapPlan(plan));

            assertThat(result.getPlanDefinitionHttpV4().getSecurity().getType()).isEqualTo(expectedV4SecurityType);
            assertThat(result.getPlanDefinitionHttpV4().getSecurity().getConfiguration()).isEqualTo("{\"config\": \"value\"}");
        }

        @Test
        void should_preserve_plan_properties() {
            var planDef = new io.gravitee.definition.model.Plan();
            planDef.setId("plan-id");
            planDef.setName("Test Plan");
            planDef.setStatus("PUBLISHED");
            planDef.setSecurity("api-key");
            planDef.setSelectionRule("rule");
            planDef.setTags(Set.of("tag1", "tag2"));

            var now = TimeProvider.now();
            var plan = PlanFixtures
                .aPlanV2()
                .toBuilder()
                .id("plan-id")
                .name("Test Plan")
                .description("Plan Description")
                .createdAt(now)
                .updatedAt(now)
                .publishedAt(now)
                .closedAt(now)
                .order(5)
                .characteristics(List.of("char1", "char2"))
                .excludedGroups(List.of("group1"))
                .planDefinitionV2(planDef)
                .validation(Plan.PlanValidationType.MANUAL)
                .build();

            var result = get(mapper.mapPlan(plan));

            assertThat(result.getId()).isEqualTo("plan-id");
            assertThat(result.getName()).isEqualTo("Test Plan");
            assertThat(result.getDescription()).isEqualTo("Plan Description");
            assertThat(result.getCreatedAt()).isEqualTo(now);
            assertThat(result.getUpdatedAt()).isEqualTo(now);
            assertThat(result.getPublishedAt()).isEqualTo(now);
            assertThat(result.getClosedAt()).isEqualTo(now);
            assertThat(result.getOrder()).isEqualTo(5);
            assertThat(result.getCharacteristics()).containsExactly("char1", "char2");
            assertThat(result.getExcludedGroups()).containsExactly("group1");
            assertThat(result.getValidation()).isEqualTo(Plan.PlanValidationType.MANUAL);
            assertThat(result.getPlanDefinitionHttpV4().getSelectionRule()).isEqualTo("rule");
            assertThat(result.getPlanDefinitionHttpV4().getTags()).containsExactlyInAnyOrder("tag1", "tag2");
            assertThat(result.isCommentRequired()).isFalse();
        }

        @ParameterizedTest
        @MethodSource("planValidationTypeProvider")
        void should_map_plan_validation_type_correctly(Plan.PlanValidationType v2Validation, Plan.PlanValidationType expectedValidation) {
            var plan = PlanFixtures.aPlanV2().toBuilder().validation(v2Validation).build();

            var result = get(mapper.mapPlan(plan));

            assertThat(result.getValidation()).isEqualTo(expectedValidation);
        }

        private static Stream<Arguments> planValidationTypeProvider() {
            return Stream.of(
                Arguments.of(Plan.PlanValidationType.AUTO, Plan.PlanValidationType.AUTO),
                Arguments.of(Plan.PlanValidationType.MANUAL, Plan.PlanValidationType.MANUAL)
            );
        }

        @Test
        void should_fail_when_plan_has_flows() {
            var flow = new io.gravitee.definition.model.flow.Flow();
            flow.setName("test-flow");

            var planDef = new io.gravitee.definition.model.Plan();
            planDef.setId("plan-id");
            planDef.setName("Test Plan");
            planDef.setStatus("PUBLISHED");
            planDef.setSecurity("api-key");
            planDef.setFlows(List.of(flow));

            Plan plan = Plan.builder().planDefinitionV2(planDef).build();

            MigrationResult<Plan> planResult = mapper.mapPlan(plan);

            assertThat(planResult.state()).isEqualTo(MigrationResult.State.IMPOSSIBLE);
        }

        @Test
        void should_set_needRedeployAt_for_plan_mapping() {
            // Given
            var now = TimeProvider.now();
            var plan = PlanFixtures.aPlanV2();

            // When
            var result = get(mapper.mapPlan(plan));

            // Then
            assertThat(result.getNeedRedeployAt()).isNotNull();
            assertThat(result.getNeedRedeployAt().toInstant()).isAfter(now.toInstant().minusSeconds(1));
            assertThat(result.getNeedRedeployAt().toInstant()).isBefore(now.toInstant().plusSeconds(10));
        }
    }

    private <T> T get(MigrationResult<T> result) {
        return Objects.requireNonNull(result.value());
    }
}
