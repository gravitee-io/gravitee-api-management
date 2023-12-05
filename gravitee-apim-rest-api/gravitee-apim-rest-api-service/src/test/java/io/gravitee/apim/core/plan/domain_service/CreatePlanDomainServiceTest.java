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
package io.gravitee.apim.core.plan.domain_service;

import static fixtures.core.model.ApiFixtures.aMessageApiV4;
import static fixtures.core.model.ApiFixtures.aProxyApiV4;
import static fixtures.core.model.PlanFixtures.aPushPlan;
import static fixtures.core.model.PlanFixtures.anApiKeyV4;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import fixtures.core.model.AuditInfoFixtures;
import inmemory.AuditCrudServiceInMemory;
import inmemory.EntrypointPluginQueryServiceInMemory;
import inmemory.FlowCrudServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.PageCrudServiceInMemory;
import inmemory.ParametersQueryServiceInMemory;
import inmemory.PlanCrudServiceInMemory;
import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.api.exception.ApiDeprecatedException;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditEntity;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.event.PlanAuditEvent;
import io.gravitee.apim.core.datetime.TimeProvider;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.flow.domain_service.FlowValidationDomainService;
import io.gravitee.apim.core.plan.exception.PlanInvalidException;
import io.gravitee.apim.core.plan.exception.UnauthorizedPlanSecurityTypeException;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.plugin.domain_service.PolicyValidationDomainService;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.definition.model.flow.Operator;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.selector.ChannelSelector;
import io.gravitee.definition.model.v4.flow.selector.HttpSelector;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.repository.management.model.Parameter;
import io.gravitee.repository.management.model.ParameterReferenceType;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class CreatePlanDomainServiceTest {

    private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");
    private static final String API_ID = "my-api";
    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String USER_ID = "user-id";
    private static final String TAG = "tag1";

    private static final Api API_PROXY_V4 = aProxyApiV4().toBuilder().id(API_ID).build();
    private static final Api API_MESSAGE_V4 = aMessageApiV4().toBuilder().id(API_ID).build();
    private static final AuditInfo AUDIT_INFO = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);

    ParametersQueryServiceInMemory parametersQueryService = new ParametersQueryServiceInMemory();
    PolicyValidationDomainService policyValidationDomainService = mock(PolicyValidationDomainService.class);
    PlanCrudServiceInMemory planCrudService = new PlanCrudServiceInMemory();
    FlowCrudServiceInMemory flowCrudService = new FlowCrudServiceInMemory();
    AuditCrudServiceInMemory auditCrudService = new AuditCrudServiceInMemory();
    PageCrudServiceInMemory pageCrudService = new PageCrudServiceInMemory();
    CreatePlanDomainServiceImpl service;

    @BeforeAll
    static void beforeAll() {
        UuidString.overrideGenerator(() -> "generated-id");
        TimeProvider.overrideClock(Clock.fixed(INSTANT_NOW, ZoneId.systemDefault()));
    }

    @AfterAll
    static void afterAll() {
        UuidString.reset();
        TimeProvider.overrideClock(Clock.systemDefaultZone());
    }

    @BeforeEach
    void setUp() {
        service =
            new CreatePlanDomainServiceImpl(
                new PlanValidatorDomainService(parametersQueryService, policyValidationDomainService, pageCrudService),
                new FlowValidationDomainService(policyValidationDomainService, new EntrypointPluginQueryServiceInMemory()),
                planCrudService,
                flowCrudService,
                new AuditDomainService(auditCrudService, new UserCrudServiceInMemory(), new JacksonJsonDiffProcessor())
            );

        parametersQueryService.initWith(
            List.of(new Parameter(Key.PLAN_SECURITY_APIKEY_ENABLED.key(), ENVIRONMENT_ID, ParameterReferenceType.ENVIRONMENT, "true"))
        );
        when(policyValidationDomainService.validateAndSanitizeConfiguration(any(), any()))
            .thenAnswer(invocation -> invocation.getArgument(1));
    }

    @AfterEach
    void tearDown() {
        Stream
            .of(auditCrudService, flowCrudService, pageCrudService, parametersQueryService, planCrudService)
            .forEach(InMemoryAlternative::reset);
        reset(policyValidationDomainService);
    }

    @Nested
    class StandardPlan {

        @Test
        void should_throw_when_invalid_status_change_detected() {
            // Given
            var plan = anApiKeyV4().toBuilder().build();
            parametersQueryService.initWith(
                List.of(new Parameter(Key.PLAN_SECURITY_APIKEY_ENABLED.key(), ENVIRONMENT_ID, ParameterReferenceType.ENVIRONMENT, "false"))
            );

            // When
            var throwable = Assertions.catchThrowable(() -> service.create(plan, List.of(), API_PROXY_V4, AUDIT_INFO));

            // Then
            assertThat(throwable).isInstanceOf(UnauthorizedPlanSecurityTypeException.class);
        }

        @Test
        void should_throw_when_security_configuration_is_invalid() {
            // Given
            var plan = anApiKeyV4().toBuilder().build();
            when(policyValidationDomainService.validateAndSanitizeConfiguration(any(), any()))
                .thenThrow(new InvalidDataException("invalid"));

            // When
            var throwable = Assertions.catchThrowable(() -> service.create(plan, List.of(), API_PROXY_V4, AUDIT_INFO));

            // Then
            assertThat(throwable).isInstanceOf(InvalidDataException.class);
        }
    }

    @Nested
    class PushPlan {

        @Test
        void should_throw_when_security_configuration_is_invalid() {
            // Given
            var plan = aPushPlan().toBuilder().security(PlanSecurity.builder().build()).build();

            // When
            var throwable = Assertions.catchThrowable(() -> service.create(plan, List.of(), API_PROXY_V4, AUDIT_INFO));

            // Then
            assertThat(throwable)
                .isInstanceOf(PlanInvalidException.class)
                .hasMessage("Security type is forbidden for plan with 'Push' mode");
        }
    }

    @Nested
    class Common {

        @ParameterizedTest
        @MethodSource("plans")
        void should_throw_when_api_is_deprecated(Api api, Plan plan, List<Flow> flows) {
            // Given
            var deprecatedApi = api.toBuilder().apiLifecycleState(Api.ApiLifecycleState.DEPRECATED).build();

            // When
            var throwable = Assertions.catchThrowable(() -> service.create(plan, flows, deprecatedApi, AUDIT_INFO));

            // Then
            assertThat(throwable).isInstanceOf(ApiDeprecatedException.class);
        }

        @ParameterizedTest
        @MethodSource("plans")
        void should_throw_when_plan_tags_mismatch_with_tags_defined_in_api(Api api, Plan plan, List<Flow> flows) {
            // Given
            api.getApiDefinitionV4().setTags(Set.of());

            // When
            var throwable = Assertions.catchThrowable(() -> service.create(plan, flows, api, AUDIT_INFO));

            // Then
            assertThat(throwable)
                .isInstanceOf(ValidationDomainException.class)
                .hasMessage("Plan tags mismatch the tags defined by the API");
        }

        @ParameterizedTest
        @MethodSource("plans")
        void should_throw_when_flows_are_invalid(Api api, Plan plan) {
            // Given
            var invalidFlows = List.of(
                Flow.builder().name("invalid").selectors(List.of(new HttpSelector(), new ChannelSelector())).build()
            );

            // When
            var throwable = Assertions.catchThrowable(() -> service.create(plan, invalidFlows, api, AUDIT_INFO));

            // Then
            assertThat(throwable)
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("The flow [invalid] contains selectors that couldn't apply");
        }

        @ParameterizedTest
        @MethodSource("plans")
        void should_throw_when_general_conditions_page_is_not_published_while_updating_a_published_plan(
            Api api,
            Plan plan,
            List<Flow> flows
        ) {
            // When
            var throwable = Assertions.catchThrowable(() ->
                service.create(plan.toBuilder().status(PlanStatus.PUBLISHED).generalConditions("page-id").build(), flows, api, AUDIT_INFO)
            );

            // Then
            assertThat(throwable)
                .isInstanceOf(ValidationDomainException.class)
                .hasMessage("Plan references a non published page as general conditions");
        }

        @ParameterizedTest
        @MethodSource("plans")
        void should_throw_when_flows_contains_overlapped_path_parameters(Api api, Plan plan) {
            // Given
            var selector1 = api.getType() == ApiType.PROXY
                ? HttpSelector.builder().path("/products/:productId/items/:itemId").pathOperator(Operator.STARTS_WITH).build()
                : ChannelSelector.builder().channel("/products/:productId/items/:itemId").channelOperator(Operator.STARTS_WITH).build();
            var selector2 = api.getType() == ApiType.PROXY
                ? HttpSelector.builder().path("/:productId").pathOperator(Operator.STARTS_WITH).build()
                : ChannelSelector.builder().channel("/:productId").channelOperator(Operator.STARTS_WITH).build();
            var invalidFlows = List.of(
                Flow.builder().name("flow1").selectors(List.of(selector1)).build(),
                Flow.builder().name("flow2").selectors(List.of(selector2)).build()
            );

            // When
            var throwable = Assertions.catchThrowable(() -> service.create(plan, invalidFlows, api, AUDIT_INFO));

            // Then
            assertThat(throwable)
                .isInstanceOf(ValidationDomainException.class)
                .hasMessage("Some path parameters are used at different position across different flows.");
        }

        @ParameterizedTest
        @MethodSource("plans")
        void should_create_plan(Api api, Plan plan, List<Flow> flows) {
            // When
            var result = service.create(plan, flows, api, AUDIT_INFO);

            // Then
            assertThat(result).isNotNull();
            assertThat(planCrudService.storage()).containsExactly(result);

            assertThat(result)
                .usingRecursiveComparison()
                .isEqualTo(
                    plan
                        .toBuilder()
                        .createdAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                        .updatedAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                        .needRedeployAt(Date.from(INSTANT_NOW))
                        .apiId(API_ID)
                        .build()
                );
        }

        @ParameterizedTest
        @MethodSource("plans")
        void should_create_plan_with_generated_id_when_no_id_provided(Api api, Plan plan, List<Flow> flows) {
            // Given
            var idLessPlan = plan.toBuilder().id(null).build();

            // When
            var result = service.create(idLessPlan, flows, api, AUDIT_INFO);

            // Then
            assertThat(result).extracting(Plan::getId).isEqualTo("generated-id");
        }

        @ParameterizedTest
        @MethodSource("plans")
        void should_create_plan_with_publishedAt_filled_when_creating_a_published_plan(Api api, Plan plan, List<Flow> flows) {
            // Given
            var publishedPlan = plan.toBuilder().status(PlanStatus.PUBLISHED).generalConditions(null).build();

            // When
            var result = service.create(publishedPlan, flows, api, AUDIT_INFO);

            // Then
            assertThat(result).extracting(Plan::getPublishedAt).isEqualTo(INSTANT_NOW.atZone(ZoneId.systemDefault()));
        }

        @ParameterizedTest
        @MethodSource("plans")
        void should_save_all_flows(Api api, Plan plan, List<Flow> flows) {
            // Given

            // When
            service.create(plan, flows, api, AUDIT_INFO);

            // Then
            assertThat(flowCrudService.storage()).containsExactlyElementsOf(flows);
        }

        @ParameterizedTest
        @MethodSource("plans")
        void should_create_an_audit(Api api, Plan plan, List<Flow> flows) {
            // Given

            // When
            service.create(plan, flows, api, AUDIT_INFO);

            // Then
            assertThat(auditCrudService.storage())
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("patch")
                .containsExactly(
                    AuditEntity
                        .builder()
                        .id("generated-id")
                        .organizationId(ORGANIZATION_ID)
                        .environmentId(ENVIRONMENT_ID)
                        .referenceType(AuditEntity.AuditReferenceType.API)
                        .referenceId(API_ID)
                        .user(USER_ID)
                        .properties(Map.of("PLAN", plan.getId()))
                        .event(PlanAuditEvent.PLAN_CREATED.name())
                        .createdAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                        .build()
                );
        }

        static Stream<Arguments> plans() {
            return Stream.of(
                Arguments.of(
                    API_PROXY_V4,
                    anApiKeyV4().toBuilder().apiId(API_ID).tags(Set.of(TAG)).status(PlanStatus.STAGING).build(),
                    List.of(Flow.builder().name("flow").selectors(List.of(new HttpSelector())).build())
                ),
                Arguments.of(
                    API_MESSAGE_V4,
                    aPushPlan().toBuilder().apiId(API_ID).tags(Set.of(TAG)).status(PlanStatus.STAGING).build(),
                    List.of(Flow.builder().name("flow").selectors(List.of(new ChannelSelector())).build())
                )
            );
        }
    }
}
