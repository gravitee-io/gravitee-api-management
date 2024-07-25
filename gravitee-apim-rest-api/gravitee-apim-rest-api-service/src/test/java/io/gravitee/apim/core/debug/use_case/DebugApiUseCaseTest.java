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
import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.JsonProcessingException;
import fixtures.core.model.ApiFixtures;
import fixtures.core.model.AuditInfoFixtures;
import inmemory.ApiCrudServiceInMemory;
import inmemory.EventCrudInMemory;
import inmemory.InMemoryAlternative;
import inmemory.InstanceQueryServiceInMemory;
import fakes.FakePolicyValidationDomainService;
import io.gravitee.apim.core.api.domain_service.ApiPolicyValidatorDomainService;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.debug.exceptions.DebugApiInvalidDefinitionVersionException;
import io.gravitee.apim.core.debug.exceptions.DebugApiNoCompatibleInstanceException;
import io.gravitee.apim.core.debug.exceptions.DebugApiNoValidPlanException;
import io.gravitee.apim.core.event.model.Event;
import io.gravitee.apim.core.gateway.model.Instance;
import io.gravitee.apim.infra.adapter.GraviteeJacksonMapper;
import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.ExecutionMode;
import io.gravitee.definition.model.HttpRequest;
import io.gravitee.definition.model.Logging;
import io.gravitee.definition.model.LoggingContent;
import io.gravitee.definition.model.LoggingMode;
import io.gravitee.definition.model.LoggingScope;
import io.gravitee.definition.model.Plan;
import io.gravitee.definition.model.Proxy;
import io.gravitee.definition.model.VirtualHost;
import io.gravitee.definition.model.debug.DebugApi;
import io.gravitee.definition.model.services.Services;
import io.gravitee.definition.model.services.healthcheck.HealthCheckService;
import io.gravitee.repository.management.model.ApiDebugStatus;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.model.PlanSecurityType;
import io.gravitee.rest.api.model.PlanStatus;
import io.gravitee.rest.api.model.PluginEntity;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.assertj.core.data.Index;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
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
    private final InstanceQueryServiceInMemory instanceQueryService = new InstanceQueryServiceInMemory();
    private final EventCrudInMemory eventCrudService = new EventCrudInMemory();

    @BeforeEach
    void setUp() {
        cut = new DebugApiUseCase(apiPolicyValidatorDomainService, apiCrudServiceInMemory, instanceQueryService, eventCrudService);
    }

    @AfterEach
    void tearDown() {
        Stream.of(apiCrudServiceInMemory, instanceQueryService, eventCrudService).forEach(InMemoryAlternative::reset);
    }

    @ParameterizedTest
    @EnumSource(value = DefinitionVersion.class, names = "V2", mode = EnumSource.Mode.EXCLUDE)
    void should_throw_when_debugging_non_v2_api(DefinitionVersion definitionVersion) {
        final DebugApi debugApi = new DebugApi();
        debugApi.setDefinitionVersion(definitionVersion);
        assertThatThrownBy(() -> cut.execute(DebugApiUseCase.Input.builder().apiId(API_ID).debugApi(debugApi).auditInfo(AUDIT_INFO).build())
            )
            .isInstanceOf(DebugApiInvalidDefinitionVersionException.class)
            .hasMessage("Only API with V2 definition can be debugged.");
    }

    @Test
    void should_throw_when_debugging_non_existing_api() {
        final DebugApi debugApi = debugApiDefinition();
        assertThatThrownBy(() -> cut.execute(DebugApiUseCase.Input.builder().apiId(API_ID).debugApi(debugApi).auditInfo(AUDIT_INFO).build())
            )
            .isInstanceOf(ApiNotFoundException.class)
            .hasMessage("Api not found.");
    }

    @ParameterizedTest
    @MethodSource("provideNonEligibleGateways")
    @SneakyThrows
    void should_throw_when_no_gateway_available(List<Instance> instances) {
        instanceQueryService.initWith(instances);
        final DebugApi debugApi = debugApiDefinition();

        apiCrudServiceInMemory.initWith(List.of(originalApi(debugApi)));
        debugApi.setDefinitionVersion(DefinitionVersion.V2);
        debugApi.setTags(Set.of("valid-tag"));
        assertThatThrownBy(() -> cut.execute(DebugApiUseCase.Input.builder().apiId(API_ID).debugApi(debugApi).auditInfo(AUDIT_INFO).build())
            )
            .isInstanceOf(DebugApiNoCompatibleInstanceException.class)
            .hasMessage("There is no compatible gateway instance to debug this API [my-api].");
    }

    @ParameterizedTest
    @EnumSource(value = PlanStatus.class, names = { "STAGING", "PUBLISHED" }, mode = EnumSource.Mode.EXCLUDE)
    @SneakyThrows
    void should_throw_when_no_active_plan(PlanStatus planStatus) {
        instanceQueryService.initWith(List.of(validInstance()));
        apiCrudServiceInMemory.initWith(List.of(originalApi(debugApiDefinition())));
        final DebugApi debugApi = debugApiDefinition();
        debugApi.getPlan(PLAN_ID).setStatus(planStatus.name());
        debugApi.setTags(Set.of("valid-tag"));
        assertThatThrownBy(() -> cut.execute(DebugApiUseCase.Input.builder().apiId(API_ID).debugApi(debugApi).auditInfo(AUDIT_INFO).build())
            )
            .isInstanceOf(DebugApiNoValidPlanException.class)
            .hasMessage("There is no staging or published plan for this API [my-api].");
    }

    @Test
    @SneakyThrows
    void should_create_debug_event() {
        instanceQueryService.initWith(List.of(validInstance()));
        final DebugApi debugApi = debugApiDefinition();
        apiCrudServiceInMemory.initWith(List.of(originalApi(debugApi)));
        debugApi.setExecutionMode(ExecutionMode.V4_EMULATION_ENGINE);
        debugApi.setPlans(List.of(keylessPlan()));
        debugApi.setProxy(proxyWithLogging(true));
        debugApi.setServices(healthcheckService(true));
        debugApi.setRequest(new HttpRequest());
        final DebugApiUseCase.Output output = cut.execute(
            DebugApiUseCase.Input.builder().apiId(API_ID).debugApi(debugApi).auditInfo(AUDIT_INFO).build()
        );
        assertThat(output.debugApiEvent()).isNotNull();
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
        final DebugApi debugApiFromEvent = GraviteeJacksonMapper
            .getInstance()
            .readValue(output.debugApiEvent().getPayload(), DebugApi.class);
        assertThat(debugApiFromEvent)
            .extracting(DebugApi::getExecutionMode, DebugApi::getProxy, DebugApi::getServices)
            // Compare field by field because instance of Proxy and Services have changed due to ser/deser
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactly(ExecutionMode.V4_EMULATION_ENGINE, proxyWithLogging(false), healthcheckService(false));
    }

    private static Plan keylessPlan() {
        return Plan.builder().id(PLAN_ID).security(PlanSecurityType.KEY_LESS.name()).status(PlanStatus.PUBLISHED.name()).build();
    }

    private Services healthcheckService(boolean enabled) {
        final Services services = new Services();
        services.setHealthCheckService(new HealthCheckService());
        services.getHealthCheckService().setEnabled(enabled);
        return services;
    }

    private Proxy proxyWithLogging(boolean enabled) {
        final Proxy proxy = new Proxy();
        final VirtualHost virtualHost = new VirtualHost();
        virtualHost.setPath("/path");
        proxy.setVirtualHosts(List.of(virtualHost));
        proxy.setLogging(new Logging());
        if (enabled) {
            proxy.getLogging().setMode(LoggingMode.PROXY);
            proxy.getLogging().setContent(LoggingContent.HEADERS);
            proxy.getLogging().setScope(LoggingScope.REQUEST);
        } else {
            proxy.getLogging().setMode(LoggingMode.NONE);
            proxy.getLogging().setContent(LoggingContent.NONE);
            proxy.getLogging().setScope(LoggingScope.NONE);
        }
        return proxy;
    }

    private static io.gravitee.apim.core.api.model.Api originalApi(Api apiDefinition) throws JsonProcessingException {
        return ApiFixtures
            .aProxyApiV2()
            .toBuilder()
            .definitionVersion(DefinitionVersion.V2)
            .apiDefinition(apiDefinition)
            .environmentId(ENVIRONMENT_ID)
            .build();
    }

    private static Instance validInstance() {
        return Instance
            .builder()
            .id(GATEWAY_ID)
            .startedAt(new Date())
            .clusterPrimaryNode(true)
            .environments(Set.of(ENVIRONMENT_ID))
            .plugins(Set.of(PluginEntity.builder().id(DEBUG_PLUGIN_ID).build()))
            .build();
    }

    @NotNull
    private static DebugApi debugApiDefinition() {
        final DebugApi apiDefinition = new DebugApi();
        apiDefinition.setId(API_ID);
        apiDefinition.setDefinitionVersion(DefinitionVersion.V2);
        final Proxy proxy = new Proxy();
        proxy.setVirtualHosts(List.of(new VirtualHost()));
        apiDefinition.setProxy(proxy);
        apiDefinition.setPlans(List.of(keylessPlan()));
        return apiDefinition;
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
                    Instance
                        .builder()
                        .startedAt(new Date())
                        .clusterPrimaryNode(true)
                        .environments(Set.of(ENVIRONMENT_ID))
                        .plugins(Set.of())
                        .build()
                )
            ),
            Arguments.of(
                List.of(
                    Instance
                        .builder()
                        .startedAt(new Date())
                        .clusterPrimaryNode(true)
                        .environments(Set.of(ENVIRONMENT_ID))
                        .plugins(Set.of(PluginEntity.builder().build()))
                        .build()
                )
            ),
            Arguments.of(
                List.of(
                    Instance
                        .builder()
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
}
