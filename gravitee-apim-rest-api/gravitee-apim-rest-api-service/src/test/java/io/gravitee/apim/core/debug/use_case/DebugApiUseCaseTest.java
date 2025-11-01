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
package io.gravitee.apim.core.debug.use_case;

import static io.gravitee.apim.core.gateway.model.Instance.DEBUG_PLUGIN_ID;
import static io.gravitee.definition.model.ExecutionMode.V4_EMULATION_ENGINE;
import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fakes.FakePolicyValidationDomainService;
import fixtures.core.model.ApiFixtures;
import fixtures.core.model.AuditInfoFixtures;
import fixtures.definition.ApiDefinitionFixtures;
import fixtures.definition.FlowFixtures;
import fixtures.definition.PlanFixtures;
import inmemory.ApiCrudServiceInMemory;
import inmemory.EventCrudInMemory;
import inmemory.FlowCrudServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.InstanceQueryServiceInMemory;
import inmemory.PlanQueryServiceInMemory;
import io.gravitee.apim.core.api.domain_service.ApiPolicyValidatorDomainService;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.debug.exceptions.DebugApiInvalidDefinitionVersionException;
import io.gravitee.apim.core.debug.exceptions.DebugApiNoCompatibleInstanceException;
import io.gravitee.apim.core.debug.exceptions.DebugApiNoValidPlanException;
import io.gravitee.apim.core.event.model.Event;
import io.gravitee.apim.core.gateway.model.Instance;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.infra.adapter.GraviteeJacksonMapper;
import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.ApiDefinition;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.HttpRequest;
import io.gravitee.definition.model.Logging;
import io.gravitee.definition.model.LoggingContent;
import io.gravitee.definition.model.LoggingMode;
import io.gravitee.definition.model.LoggingScope;
import io.gravitee.definition.model.Proxy;
import io.gravitee.definition.model.debug.DebugApiV2;
import io.gravitee.definition.model.debug.DebugApiV4;
import io.gravitee.definition.model.services.Services;
import io.gravitee.definition.model.services.healthcheck.HealthCheckService;
import io.gravitee.definition.model.v4.analytics.Analytics;
import io.gravitee.definition.model.v4.analytics.logging.LoggingPhase;
import io.gravitee.definition.model.v4.endpointgroup.Endpoint;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.definition.model.v4.endpointgroup.service.EndpointGroupServices;
import io.gravitee.definition.model.v4.endpointgroup.service.EndpointServices;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.definition.model.v4.service.Service;
import io.gravitee.repository.management.model.ApiDebugStatus;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.model.PlanStatus;
import io.gravitee.rest.api.model.PluginEntity;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.data.Index;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DebugApiUseCaseTest {

    public static final String API_ID = ApiFixtures.MY_API;
    private static final String USER_ID = "user-id";
    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    public static final String GATEWAY_ID = "gateway-id";
    public static final String PLAN_ID = "plan-id";
    private static final AuditInfo AUDIT_INFO = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);
    private DebugApiUseCase cut;
    private final ApiPolicyValidatorDomainService apiPolicyValidatorDomainService = new ApiPolicyValidatorDomainService(
        new FakePolicyValidationDomainService()
    );

    private final ApiCrudServiceInMemory apiCrudServiceInMemory = new ApiCrudServiceInMemory();
    private final PlanQueryServiceInMemory planQueryService = new PlanQueryServiceInMemory();
    private final FlowCrudServiceInMemory flowCrudService = new FlowCrudServiceInMemory();
    private final InstanceQueryServiceInMemory instanceQueryService = new InstanceQueryServiceInMemory();
    private final EventCrudInMemory eventCrudService = new EventCrudInMemory();

    @BeforeEach
    void setUp() {
        cut = new DebugApiUseCase(
            apiPolicyValidatorDomainService,
            apiCrudServiceInMemory,
            instanceQueryService,
            planQueryService,
            flowCrudService,
            eventCrudService
        );
    }

    @AfterEach
    void tearDown() {
        Stream.of(apiCrudServiceInMemory, eventCrudService, flowCrudService, instanceQueryService, planQueryService).forEach(
            InMemoryAlternative::reset
        );
    }

    @Test
    void should_throw_when_debugging_non_existing_api() {
        assertThatThrownBy(() -> cut.execute(new DebugApiUseCase.Input(API_ID, aDebugApiV2(ApiDefinitionFixtures.anApiV2()), AUDIT_INFO)))
            .isInstanceOf(ApiNotFoundException.class)
            .hasMessage("Api not found.");

        assertThatThrownBy(() -> cut.execute(new DebugApiUseCase.Input(API_ID, new HttpRequest("/path", "GET"), AUDIT_INFO)))
            .isInstanceOf(ApiNotFoundException.class)
            .hasMessage("Api not found.");
    }

    @Nested
    class ApiV2WithEmbeddedDefinition {

        @ParameterizedTest
        @MethodSource("io.gravitee.apim.core.debug.use_case.DebugApiUseCaseTest#provideNonEligibleGateways")
        @SneakyThrows
        void should_throw_when_no_gateway_available(List<Instance> instances) {
            instanceQueryService.initWith(instances);
            io.gravitee.apim.core.api.model.Api api1 = ApiFixtures.aProxyApiV2().setTags(Set.of("valid-tag"));
            var api = givenExistingApiDefinition(api1, Api.class);

            assertThatThrownBy(() -> cut.execute(new DebugApiUseCase.Input(API_ID, aDebugApiV2(api), AUDIT_INFO)))
                .isInstanceOf(DebugApiNoCompatibleInstanceException.class)
                .hasMessage("There is no compatible gateway instance to debug this API [my-api].");
        }

        @ParameterizedTest
        @EnumSource(value = PlanStatus.class, names = { "STAGING", "PUBLISHED" }, mode = EnumSource.Mode.EXCLUDE)
        @SneakyThrows
        void should_throw_when_no_active_plan(PlanStatus planStatus) {
            instanceQueryService.initWith(List.of(validInstance()));
            io.gravitee.apim.core.api.model.Api api1 = ApiFixtures.aProxyApiV2().setTags(Set.of("valid-tag"));
            var api = givenExistingApiDefinition(api1, Api.class);
            api.setPlans(List.of(PlanFixtures.aKeylessV2().toBuilder().status(planStatus.name()).build()));

            assertThatThrownBy(() -> cut.execute(new DebugApiUseCase.Input(API_ID, aDebugApiV2(api), AUDIT_INFO)))
                .isInstanceOf(DebugApiNoValidPlanException.class)
                .hasMessage("There is no staging or published plan for this API [my-api].");
        }

        @Test
        @SneakyThrows
        void should_create_debug_event() {
            instanceQueryService.initWith(List.of(validInstance()));
            io.gravitee.apim.core.api.model.Api api1 = ApiFixtures.aProxyApiV2().setTags(Set.of("valid-tag"));
            var api = givenExistingApiDefinition(api1, Api.class);
            api.setPlans(List.of(PlanFixtures.aKeylessV2()));
            api.setExecutionMode(V4_EMULATION_ENGINE);
            api.getProxy().setLogging(new Logging(LoggingMode.PROXY, LoggingScope.REQUEST, LoggingContent.HEADERS, null));
            api.setServices(healthcheckService(true));

            final DebugApiUseCase.Output output = cut.execute(
                new DebugApiUseCase.Input(API_ID, aDebugApiV2(api, new HttpRequest("/", "GET")), AUDIT_INFO)
            );

            assertThat(eventCrudService.storage()).hasSize(1).first().isEqualTo(output.debugApiEvent());
            assertThat(output.debugApiEvent())
                .extracting(Event::getType, Event::getEnvironments, Event::getProperties)
                .contains(EventType.DEBUG_API, Index.atIndex(0))
                .contains(Set.of(ENVIRONMENT_ID), Index.atIndex(1))
                .contains(
                    Map.ofEntries(
                        entry(Event.EventProperties.API_ID, API_ID),
                        entry(Event.EventProperties.USER, USER_ID),
                        entry(Event.EventProperties.API_DEBUG_STATUS, ApiDebugStatus.TO_DEBUG.name()),
                        entry(Event.EventProperties.GATEWAY_ID, GATEWAY_ID)
                    ),
                    Index.atIndex(2)
                );
            final DebugApiV2 debugApiFromEvent = GraviteeJacksonMapper.getInstance().readValue(
                output.debugApiEvent().getPayload(),
                DebugApiV2.class
            );
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(debugApiFromEvent.getExecutionMode()).isEqualTo(V4_EMULATION_ENGINE);
                softly
                    .assertThat(debugApiFromEvent.getProxy())
                    .extracting(Proxy::getLogging)
                    .isEqualTo(new Logging(LoggingMode.NONE, LoggingScope.NONE, LoggingContent.NONE, null));
                softly.assertThat(debugApiFromEvent.getServices()).usingRecursiveComparison().isEqualTo(healthcheckService(false));
            });
        }
    }

    @Nested
    class ApiV2 {

        @ParameterizedTest
        @MethodSource("io.gravitee.apim.core.debug.use_case.DebugApiUseCaseTest#provideNonEligibleGateways")
        @SneakyThrows
        void should_throw_when_no_gateway_available(List<Instance> instances) {
            instanceQueryService.initWith(instances);
            givenExistingPlan(fixtures.core.model.PlanFixtures.aPlanV2().setApiId(API_ID));
            givenExistingApi(ApiFixtures.aProxyApiV2().setTags(Set.of("valid-tag")));

            assertThatThrownBy(() -> cut.execute(new DebugApiUseCase.Input(API_ID, new HttpRequest("/path", "GET"), AUDIT_INFO)))
                .isInstanceOf(DebugApiNoCompatibleInstanceException.class)
                .hasMessage("There is no compatible gateway instance to debug this API [my-api].");
        }

        @ParameterizedTest
        @EnumSource(
            value = io.gravitee.definition.model.v4.plan.PlanStatus.class,
            names = { "STAGING", "PUBLISHED" },
            mode = EnumSource.Mode.EXCLUDE
        )
        @SneakyThrows
        void should_throw_when_no_active_plan(io.gravitee.definition.model.v4.plan.PlanStatus planStatus) {
            instanceQueryService.initWith(List.of(validInstance()));
            givenExistingPlan(fixtures.core.model.PlanFixtures.aPlanV2().setApiId(API_ID).setPlanStatus(planStatus));
            givenExistingApi(ApiFixtures.aProxyApiV2().setTags(Set.of("valid-tag")));

            assertThatThrownBy(() -> cut.execute(new DebugApiUseCase.Input(API_ID, new HttpRequest("/path", "GET"), AUDIT_INFO)))
                .isInstanceOf(DebugApiNoValidPlanException.class)
                .hasMessage("There is no staging or published plan for this API [my-api].");
        }

        @Test
        @SneakyThrows
        void should_throw_plan_flows_invalid() {
            instanceQueryService.initWith(List.of(validInstance()));
            var plan = givenExistingPlan(fixtures.core.model.PlanFixtures.aPlanV2().setApiId(API_ID));
            givenExistingPlanV2Flows(
                plan.getId(),
                FlowFixtures.aFlowV2()
                    .toBuilder()
                    .pre(
                        List.of(
                            io.gravitee.definition.model.flow.Step.builder()
                                .name("my-step-name-1")
                                .policy("a-policy_throw_invalid_data_exception")
                                .configuration("{}")
                                .build()
                        )
                    )
                    .build()
            );
            givenExistingApi(ApiFixtures.aProxyApiV2().setTags(Set.of("valid-tag")));

            assertThatThrownBy(() -> cut.execute(new DebugApiUseCase.Input(API_ID, new HttpRequest("/path", "GET"), AUDIT_INFO)))
                .isInstanceOf(InvalidDataException.class)
                .hasMessage("Invalid configuration for policy a-policy_throw_invalid_data_exception");
        }

        @Test
        @SneakyThrows
        void should_create_debug_event() {
            instanceQueryService.initWith(List.of(validInstance()));
            givenExistingPlan(fixtures.core.model.PlanFixtures.aPlanV2().setApiId(API_ID));
            io.gravitee.apim.core.api.model.Api api1 = ApiFixtures.aProxyApiV2().setTags(Set.of("valid-tag"));
            var api = givenExistingApiDefinition(api1, Api.class);
            api.setPlans(List.of(PlanFixtures.aKeylessV2()));
            api.setExecutionMode(V4_EMULATION_ENGINE);
            api.getProxy().setLogging(new Logging(LoggingMode.PROXY, LoggingScope.REQUEST, LoggingContent.HEADERS, null));
            api.setServices(healthcheckService(true));

            final DebugApiUseCase.Output output = cut.execute(new DebugApiUseCase.Input(API_ID, new HttpRequest("/", "GET"), AUDIT_INFO));

            assertThat(eventCrudService.storage()).hasSize(1).first().isEqualTo(output.debugApiEvent());
            assertThat(output.debugApiEvent())
                .extracting(Event::getType, Event::getEnvironments, Event::getProperties)
                .contains(EventType.DEBUG_API, Index.atIndex(0))
                .contains(Set.of(ENVIRONMENT_ID), Index.atIndex(1))
                .contains(
                    Map.ofEntries(
                        entry(Event.EventProperties.API_ID, API_ID),
                        entry(Event.EventProperties.USER, USER_ID),
                        entry(Event.EventProperties.API_DEBUG_STATUS, ApiDebugStatus.TO_DEBUG.name()),
                        entry(Event.EventProperties.GATEWAY_ID, GATEWAY_ID)
                    ),
                    Index.atIndex(2)
                );
            final DebugApiV2 debugApiFromEvent = GraviteeJacksonMapper.getInstance().readValue(
                output.debugApiEvent().getPayload(),
                DebugApiV2.class
            );
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(debugApiFromEvent.getExecutionMode()).isEqualTo(V4_EMULATION_ENGINE);
                softly
                    .assertThat(debugApiFromEvent.getProxy())
                    .extracting(Proxy::getLogging)
                    .isEqualTo(new Logging(LoggingMode.NONE, LoggingScope.NONE, LoggingContent.NONE, null));
                softly.assertThat(debugApiFromEvent.getServices()).usingRecursiveComparison().isEqualTo(healthcheckService(false));
            });
        }
    }

    @Nested
    class ProxyV4 {

        @Test
        void should_throw_when_debugging_v4_message_api() {
            givenExistingApi(ApiFixtures.aMessageApiV4().setId(API_ID));

            assertThatThrownBy(() -> cut.execute(new DebugApiUseCase.Input(API_ID, new HttpRequest("/path", "GET"), AUDIT_INFO)))
                .isInstanceOf(DebugApiInvalidDefinitionVersionException.class)
                .hasMessage("Only API V2 or V4 PROXY can be debugged.")
                .extracting("parameters")
                .isEqualTo(Map.of("apiId", API_ID));
        }

        @ParameterizedTest
        @MethodSource("io.gravitee.apim.core.debug.use_case.DebugApiUseCaseTest#provideNonEligibleGateways")
        @SneakyThrows
        void should_throw_when_no_gateway_available(List<Instance> instances) {
            instanceQueryService.initWith(instances);
            givenExistingPlan(fixtures.core.model.PlanFixtures.aPlanHttpV4().setApiId(API_ID));
            givenExistingApi(ApiFixtures.aProxyApiV4().setTags(Set.of("valid-tag")));

            assertThatThrownBy(() -> cut.execute(new DebugApiUseCase.Input(API_ID, new HttpRequest("/path", "GET"), AUDIT_INFO)))
                .isInstanceOf(DebugApiNoCompatibleInstanceException.class)
                .hasMessage("There is no compatible gateway instance to debug this API [my-api].");
        }

        @ParameterizedTest
        @EnumSource(
            value = io.gravitee.definition.model.v4.plan.PlanStatus.class,
            names = { "STAGING", "PUBLISHED" },
            mode = EnumSource.Mode.EXCLUDE
        )
        @SneakyThrows
        void should_throw_when_no_active_plan(io.gravitee.definition.model.v4.plan.PlanStatus planStatus) {
            instanceQueryService.initWith(List.of(validInstance()));
            givenExistingPlan(fixtures.core.model.PlanFixtures.aPlanHttpV4().setApiId(API_ID).setPlanStatus(planStatus));
            givenExistingApi(ApiFixtures.aProxyApiV4().setTags(Set.of("valid-tag")));

            assertThatThrownBy(() -> cut.execute(new DebugApiUseCase.Input(API_ID, new HttpRequest("/path", "GET"), AUDIT_INFO)))
                .isInstanceOf(DebugApiNoValidPlanException.class)
                .hasMessage("There is no staging or published plan for this API [my-api].");
        }

        @Test
        @SneakyThrows
        void should_throw_plan_flows_invalid() {
            instanceQueryService.initWith(List.of(validInstance()));
            var plan = givenExistingPlan(fixtures.core.model.PlanFixtures.aPlanHttpV4().setApiId(API_ID));
            givenExistingPlanV4Flows(
                plan.getId(),
                FlowFixtures.aProxyFlowV4()
                    .toBuilder()
                    .request(
                        List.of(
                            Step.builder()
                                .name("my-step-name-1")
                                .policy("a-policy_throw_invalid_data_exception")
                                .configuration("{}")
                                .build()
                        )
                    )
                    .build()
            );
            givenExistingApi(ApiFixtures.aProxyApiV4().setTags(Set.of("valid-tag")));

            assertThatThrownBy(() -> cut.execute(new DebugApiUseCase.Input(API_ID, new HttpRequest("/path", "GET"), AUDIT_INFO)))
                .isInstanceOf(InvalidDataException.class)
                .hasMessage("Invalid configuration for policy a-policy_throw_invalid_data_exception");
        }

        @Test
        @SneakyThrows
        void should_throw_api_flows_invalid() {
            instanceQueryService.initWith(List.of(validInstance()));
            givenExistingPlan(fixtures.core.model.PlanFixtures.aPlanHttpV4().setApiId(API_ID));
            var api = givenExistingApi(ApiFixtures.aProxyApiV4().setTags(Set.of("valid-tag")));
            givenExistingApiV4Flows(
                api.getId(),
                FlowFixtures.aProxyFlowV4()
                    .toBuilder()
                    .request(
                        List.of(
                            Step.builder()
                                .name("my-step-name-1")
                                .policy("a-policy_throw_invalid_data_exception")
                                .configuration("{}")
                                .build()
                        )
                    )
                    .build()
            );

            assertThatThrownBy(() -> cut.execute(new DebugApiUseCase.Input(API_ID, new HttpRequest("/path", "GET"), AUDIT_INFO)))
                .isInstanceOf(InvalidDataException.class)
                .hasMessage("Invalid configuration for policy a-policy_throw_invalid_data_exception");
        }

        @Test
        @SneakyThrows
        void should_create_debug_event() {
            instanceQueryService.initWith(List.of(validInstance()));
            givenExistingPlan(fixtures.core.model.PlanFixtures.aPlanHttpV4().setApiId(API_ID));
            io.gravitee.apim.core.api.model.Api api1 = ApiFixtures.aProxyApiV4().setTags(Set.of("valid-tag"));
            var api = givenExistingApiDefinition(api1, io.gravitee.definition.model.v4.Api.class);
            api
                .getAnalytics()
                .setLogging(
                    new io.gravitee.definition.model.v4.analytics.logging.Logging(
                        new io.gravitee.definition.model.v4.analytics.logging.LoggingMode(true, true),
                        new LoggingPhase(true, true),
                        new io.gravitee.definition.model.v4.analytics.logging.LoggingContent(true, false, false, false, false),
                        null,
                        null,
                        null
                    )
                );
            api
                .getEndpointGroups()
                .forEach(endpointGroup -> {
                    endpointGroup.setServices(new EndpointGroupServices(null, new Service(false, true, "healthcheck", null)));
                    endpointGroup
                        .getEndpoints()
                        .forEach(endpoint -> endpoint.setServices(new EndpointServices(new Service(false, true, "healthcheck", null))));
                });

            final DebugApiUseCase.Output output = cut.execute(
                new DebugApiUseCase.Input(API_ID, new HttpRequest("/path", "GET"), AUDIT_INFO)
            );

            assertThat(eventCrudService.storage()).hasSize(1).first().isEqualTo(output.debugApiEvent());

            assertThat(output.debugApiEvent())
                .extracting(Event::getType, Event::getEnvironments, Event::getProperties)
                .contains(EventType.DEBUG_API, Index.atIndex(0))
                .contains(Set.of(ENVIRONMENT_ID), Index.atIndex(1))
                .contains(
                    Map.ofEntries(
                        entry(Event.EventProperties.API_ID, API_ID),
                        entry(Event.EventProperties.USER, USER_ID),
                        entry(Event.EventProperties.API_DEBUG_STATUS, ApiDebugStatus.TO_DEBUG.name()),
                        entry(Event.EventProperties.GATEWAY_ID, GATEWAY_ID),
                        entry(Event.EventProperties.API_DEFINITION_VERSION, DefinitionVersion.V4.name())
                    ),
                    Index.atIndex(2)
                );
            var debugApiFromEvent = GraviteeJacksonMapper.getInstance().readValue(output.debugApiEvent().getPayload(), DebugApiV4.class);
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(debugApiFromEvent.getApiDefinition().getAnalytics()).extracting(Analytics::getLogging).isNull();

                softly
                    .assertThat(debugApiFromEvent.getApiDefinition().getEndpointGroups())
                    .extracting(EndpointGroup::getServices)
                    .containsExactly(new EndpointGroupServices(null, null));
                softly
                    .assertThat(
                        debugApiFromEvent
                            .getApiDefinition()
                            .getEndpointGroups()
                            .stream()
                            .map(EndpointGroup::getEndpoints)
                            .flatMap(List::stream)
                    )
                    .extracting(Endpoint::getServices)
                    .containsExactly(new EndpointServices(null));
            });
        }
    }

    private static Instance validInstance() {
        return Instance.builder()
            .id(GATEWAY_ID)
            .startedAt(new Date())
            .clusterPrimaryNode(true)
            .environments(Set.of(ENVIRONMENT_ID))
            .plugins(Set.of(PluginEntity.builder().id(DEBUG_PLUGIN_ID).build()))
            .build();
    }

    private static Stream<Arguments> provideNonEligibleGateways() {
        return Stream.of(
            Arguments.of(List.of()),
            Arguments.of(List.of(Instance.builder().build())),
            Arguments.of(List.of(Instance.builder().environments(Set.of(ENVIRONMENT_ID)).build())),
            Arguments.of(List.of(Instance.builder().startedAt(new Date()).build())),
            Arguments.of(List.of(Instance.builder().environments(Set.of(ENVIRONMENT_ID)).startedAt(new Date()).build())),
            Arguments.of(List.of(Instance.builder().startedAt(new Date()).clusterPrimaryNode(false).build())),
            Arguments.of(
                List.of(Instance.builder().environments(Set.of(ENVIRONMENT_ID)).startedAt(new Date()).clusterPrimaryNode(false).build())
            ),
            Arguments.of(List.of(Instance.builder().environments(Set.of()).startedAt(new Date()).clusterPrimaryNode(true).build())),
            Arguments.of(
                List.of(Instance.builder().environments(Set.of(ENVIRONMENT_ID)).startedAt(new Date()).clusterPrimaryNode(true).build())
            ),
            Arguments.of(
                List.of(
                    Instance.builder()
                        .startedAt(new Date())
                        .clusterPrimaryNode(true)
                        .environments(Set.of(ENVIRONMENT_ID))
                        .plugins(Set.of())
                        .build()
                )
            ),
            Arguments.of(
                List.of(
                    Instance.builder()
                        .startedAt(new Date())
                        .clusterPrimaryNode(true)
                        .environments(Set.of(ENVIRONMENT_ID))
                        .plugins(Set.of(PluginEntity.builder().build()))
                        .build()
                )
            ),
            Arguments.of(
                List.of(
                    Instance.builder()
                        .startedAt(new Date())
                        .clusterPrimaryNode(true)
                        .environments(Set.of(ENVIRONMENT_ID))
                        .plugins(Set.of(PluginEntity.builder().id(DEBUG_PLUGIN_ID).build()))
                        .tags(List.of("invalid-tag"))
                        .build()
                )
            )
        );
    }

    @NotNull
    private static DebugApiV2 aDebugApiV2(Api api) {
        return new DebugApiV2(api, new HttpRequest("/", "GET"));
    }

    @NotNull
    private static DebugApiV2 aDebugApiV2(Api api, HttpRequest httpRequest) {
        return new DebugApiV2(api, httpRequest);
    }

    private Services healthcheckService(boolean enabled) {
        final Services services = new Services();
        services.setHealthCheckService(new HealthCheckService());
        services.getHealthCheckService().setEnabled(enabled);
        return services;
    }

    public io.gravitee.apim.core.api.model.Api givenExistingApi(io.gravitee.apim.core.api.model.Api api) {
        apiCrudServiceInMemory.initWith(List.of(api));
        return api;
    }

    public <T extends ApiDefinition> T givenExistingApiDefinition(io.gravitee.apim.core.api.model.Api api, Class<T> clazz) {
        apiCrudServiceInMemory.initWith(List.of(api));
        return clazz.cast(api.getApiDefinitionValue());
    }

    public Plan givenExistingPlan(Plan plan) {
        planQueryService.initWith(List.of(plan));
        return plan;
    }

    public void givenExistingPlanV4Flows(String planId, Flow... flows) {
        flowCrudService.savePlanFlows(planId, Arrays.asList(flows));
    }

    public void givenExistingPlanV2Flows(String planId, io.gravitee.definition.model.flow.Flow... flows) {
        flowCrudService.savePlanFlowsV2(planId, Arrays.asList(flows));
    }

    public void givenExistingApiV4Flows(String apiId, Flow... flows) {
        flowCrudService.saveApiFlows(apiId, Arrays.asList(flows));
    }
}
