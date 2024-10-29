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
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import assertions.CoreAssertions;
import fixtures.core.model.AuditInfoFixtures;
import fixtures.core.model.PlanFixtures;
import inmemory.AuditCrudServiceInMemory;
import inmemory.EntrypointPluginQueryServiceInMemory;
import inmemory.FlowCrudServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.PageCrudServiceInMemory;
import inmemory.ParametersQueryServiceInMemory;
import inmemory.PlanCrudServiceInMemory;
import inmemory.PlanQueryServiceInMemory;
import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditEntity;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.event.PlanAuditEvent;
import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.flow.domain_service.FlowValidationDomainService;
import io.gravitee.apim.core.plan.exception.UnauthorizedPlanSecurityTypeException;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.policy.domain_service.PolicyValidationDomainService;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.flow.Operator;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.selector.ChannelSelector;
import io.gravitee.definition.model.v4.flow.selector.HttpSelector;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.repository.management.model.Parameter;
import io.gravitee.repository.management.model.ParameterReferenceType;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import java.sql.Date;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class UpdatePlanDomainServiceTest {

    private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");
    private static final String API_ID = "my-api";
    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String USER_ID = "user-id";
    private static final String TAG = "tag1";

    private static final Api API_PROXY_V4 = aProxyApiV4().toBuilder().id(API_ID).build();
    private static final Api API_MESSAGE_V4 = aMessageApiV4().toBuilder().id(API_ID).build();
    private static final AuditInfo AUDIT_INFO = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);

    PolicyValidationDomainService policyValidationDomainService = mock(PolicyValidationDomainService.class);
    PlanSynchronizationService planSynchronizationService = mock(PlanSynchronizationService.class);
    ParametersQueryServiceInMemory parametersQueryService = new ParametersQueryServiceInMemory();
    PlanCrudServiceInMemory planCrudService = new PlanCrudServiceInMemory();
    PlanQueryServiceInMemory planQueryService = new PlanQueryServiceInMemory(planCrudService);
    FlowCrudServiceInMemory flowCrudService = new FlowCrudServiceInMemory();
    AuditCrudServiceInMemory auditCrudService = new AuditCrudServiceInMemory();
    PageCrudServiceInMemory pageCrudService = new PageCrudServiceInMemory();

    UpdatePlanDomainService service;

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
        var auditDomainService = new AuditDomainService(auditCrudService, new UserCrudServiceInMemory(), new JacksonJsonDiffProcessor());

        service =
            new UpdatePlanDomainService(
                planQueryService,
                planCrudService,
                new PlanValidatorDomainService(parametersQueryService, policyValidationDomainService, pageCrudService),
                new FlowValidationDomainService(policyValidationDomainService, new EntrypointPluginQueryServiceInMemory()),
                flowCrudService,
                auditDomainService,
                planSynchronizationService,
                new ReorderPlanDomainService(planQueryService, planCrudService)
            );

        parametersQueryService.initWith(
            List.of(
                new Parameter(Key.PLAN_SECURITY_KEYLESS_ENABLED.key(), ENVIRONMENT_ID, ParameterReferenceType.ENVIRONMENT, "true"),
                new Parameter(Key.PLAN_SECURITY_APIKEY_ENABLED.key(), ENVIRONMENT_ID, ParameterReferenceType.ENVIRONMENT, "true")
            )
        );
        when(policyValidationDomainService.validateAndSanitizeConfiguration(any(), any()))
            .thenAnswer(invocation -> invocation.getArgument(1));
        when(planSynchronizationService.checkSynchronized(any(), any(), any(), any())).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        Stream
            .of(auditCrudService, flowCrudService, pageCrudService, parametersQueryService, planCrudService)
            .forEach(InMemoryAlternative::reset);
        reset(policyValidationDomainService, planSynchronizationService);
    }

    @Nested
    class StandardPlan {

        @Test
        void should_throw_when_invalid_status_change_detected() {
            // Given
            var plan = PlanFixtures.HttpV4.anApiKey().toBuilder().build();
            parametersQueryService.initWith(
                List.of(new Parameter(Key.PLAN_SECURITY_APIKEY_ENABLED.key(), ENVIRONMENT_ID, ParameterReferenceType.ENVIRONMENT, "false"))
            );

            // When
            var throwable = Assertions.catchThrowable(() -> service.update(plan, List.of(), Map.of(), API_PROXY_V4, AUDIT_INFO));

            // Then
            assertThat(throwable).isInstanceOf(UnauthorizedPlanSecurityTypeException.class);
        }

        @Test
        void should_throw_when_security_configuration_is_invalid() {
            // Given
            var plan = PlanFixtures.HttpV4.anApiKey().toBuilder().build();
            when(policyValidationDomainService.validateAndSanitizeConfiguration(any(), any()))
                .thenThrow(new InvalidDataException("invalid"));

            // When
            var throwable = Assertions.catchThrowable(() -> service.update(plan, List.of(), Map.of(), API_PROXY_V4, AUDIT_INFO));

            // Then
            assertThat(throwable).isInstanceOf(InvalidDataException.class);
        }
    }

    @Nested
    class FederatedPlan {

        @Test
        void should_update_federated_plan_attributes() {
            // Given
            var plan = givenExistingPlan(PlanFixtures.aFederatedPlan());

            // When
            var result = service.update(
                plan
                    .toBuilder()
                    .name("new name")
                    .description("new description")
                    .validation(Plan.PlanValidationType.AUTO)
                    .characteristics(List.of("new characteristic"))
                    .commentMessage("new comment message")
                    .commentRequired(true)
                    .build(),
                List.of(),
                Map.of(),
                null,
                AUDIT_INFO
            );

            // Then
            CoreAssertions
                .assertThat(planCrudService.getById(plan.getId()))
                .isEqualTo(result)
                .extracting(
                    Plan::getName,
                    Plan::getDescription,
                    Plan::getValidation,
                    Plan::getCharacteristics,
                    Plan::getCommentMessage,
                    Plan::isCommentRequired,
                    Plan::getUpdatedAt
                )
                .contains(
                    "new name",
                    "new description",
                    Plan.PlanValidationType.AUTO,
                    List.of("new characteristic"),
                    "new comment message",
                    true,
                    INSTANT_NOW.atZone(ZoneId.systemDefault())
                );
        }

        @Test
        void should_reorder_all_plans_when_order_is_updated() {
            // Given
            var plans = givenExistingPlans(
                PlanFixtures.aFederatedPlan().toBuilder().id("plan1").order(1).build(),
                PlanFixtures.aFederatedPlan().toBuilder().id("plan2").order(2).build(),
                PlanFixtures.aFederatedPlan().toBuilder().id("plan3").order(3).build()
            );

            // When
            var toUpdate = plans.get(0).toBuilder().order(2).build();

            service.update(toUpdate, List.of(), null, null, AUDIT_INFO);

            // Then
            Assertions
                .assertThat(planCrudService.storage())
                .extracting(Plan::getId, Plan::getOrder)
                .containsOnly(tuple("plan2", 1), tuple("plan1", 2), tuple("plan3", 3));
        }

        @Test
        void should_throw_when_general_conditions_page_is_not_published_while_updating_a_federated_plan() {
            // Given
            var plan = givenExistingPlan(PlanFixtures.aFederatedPlan());
            pageCrudService.initWith(List.of(Page.builder().id("page-id").published(false).build()));

            // When
            var throwable = Assertions.catchThrowable(() ->
                service.update(plan.toBuilder().generalConditions("page-id").build(), List.of(), null, null, AUDIT_INFO)
            );

            // Then
            assertThat(throwable)
                .isInstanceOf(ValidationDomainException.class)
                .hasMessage("Plan references a non published page as general conditions");
        }

        @Test
        void should_throw_when_invalid_status_change_detected() {
            // Given
            var plan = givenExistingPlan(PlanFixtures.aFederatedPlan().setPlanStatus(PlanStatus.CLOSED));

            // When
            var throwable = Assertions.catchThrowable(() ->
                service.update(plan.copy().setPlanStatus(PlanStatus.DEPRECATED), List.of(), null, null, AUDIT_INFO)
            );

            // Then
            Assertions.assertThat(throwable).isInstanceOf(ValidationDomainException.class);
        }
    }

    @Nested
    class V4Plan {

        @ParameterizedTest
        @MethodSource("io.gravitee.apim.core.plan.domain_service.UpdatePlanDomainServiceTest#v4testData")
        void should_update_existing_plans(Api api, Plan plan, List<Flow> flows) {
            // Given
            givenExistingPlan(plan);

            // When
            Plan toUpdate = plan
                .toBuilder()
                .definitionVersion(DefinitionVersion.V4)
                .name("updated name")
                .description("updated description")
                .commentRequired(true)
                .commentMessage("updated comment")
                .planDefinitionHttpV4(
                    plan
                        .getPlanDefinitionHttpV4()
                        .toBuilder()
                        .status(PlanStatus.PUBLISHED)
                        .tags(Set.of("tag1"))
                        .selectionRule("updated rule")
                        .build()
                )
                .excludedGroups(List.of("updated group"))
                .characteristics(List.of("updated characteristic"))
                .build();
            var result = service.update(toUpdate, flows, null, api, AUDIT_INFO);

            // Then
            Assertions.assertThat(planCrudService.storage()).hasSize(1).contains(result);
            assertThat(result)
                .usingRecursiveComparison(RecursiveComparisonConfiguration.builder().build())
                .isEqualTo(toUpdate.toBuilder().updatedAt(INSTANT_NOW.atZone(ZoneId.systemDefault())).build());
        }

        @ParameterizedTest
        @MethodSource("io.gravitee.apim.core.plan.domain_service.UpdatePlanDomainServiceTest#v4testData")
        void should_throw_when_a_plan_have_no_tag_matching_api_tags(Api api, Plan plan, List<Flow> flows) {
            // Given
            givenExistingPlan(plan);
            api.getApiDefinitionHttpV4().setTags(Set.of("tag1", "tag2"));

            // When
            var throwable = Assertions.catchThrowable(() -> service.update(plan.setPlanTags(Set.of("tag3")), flows, null, api, AUDIT_INFO));

            // Then
            Assertions.assertThat(throwable).isInstanceOf(ValidationDomainException.class);
        }

        @ParameterizedTest
        @MethodSource("io.gravitee.apim.core.plan.domain_service.UpdatePlanDomainServiceTest#v4testData")
        void should_throw_when_flows_are_invalid(Api api, Plan plan) {
            // Given
            givenExistingPlan(plan);
            List<Flow> invalidFlows = List.of(
                Flow.builder().name("invalid").selectors(List.of(new HttpSelector(), new ChannelSelector())).build()
            );

            // When
            var throwable = Assertions.catchThrowable(() -> service.update(plan, invalidFlows, null, api, AUDIT_INFO));

            // Then
            assertThat(throwable)
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("The flow [invalid] contains selectors that couldn't apply");
        }

        @ParameterizedTest
        @MethodSource("io.gravitee.apim.core.plan.domain_service.UpdatePlanDomainServiceTest#v4testData")
        void should_throw_when_flows_contains_overlapped_path_parameters(Api api, Plan plan) {
            // Given
            givenExistingPlan(plan);
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
            var throwable = Assertions.catchThrowable(() -> service.update(plan, invalidFlows, null, api, AUDIT_INFO));

            // Then
            assertThat(throwable)
                .isInstanceOf(ValidationDomainException.class)
                .hasMessage("Some path parameters are used at different position across different flows.");
        }

        @ParameterizedTest
        @MethodSource("io.gravitee.apim.core.plan.domain_service.UpdatePlanDomainServiceTest#v4testData")
        void should_throw_when_general_conditions_page_is_not_published_while_updating_a_published_plan(
            Api api,
            Plan plan,
            List<Flow> flows
        ) {
            // Given
            var publishedPlan = givenExistingPlan(plan.toBuilder().build().setPlanStatus(PlanStatus.PUBLISHED));
            pageCrudService.initWith(List.of(Page.builder().id("page-id").published(false).build()));

            // When
            var throwable = Assertions.catchThrowable(() ->
                service.update(publishedPlan.toBuilder().generalConditions("page-id").build(), flows, null, api, AUDIT_INFO)
            );

            // Then
            assertThat(throwable)
                .isInstanceOf(ValidationDomainException.class)
                .hasMessage("Plan references a non published page as general conditions");
        }

        @ParameterizedTest
        @MethodSource("io.gravitee.apim.core.plan.domain_service.UpdatePlanDomainServiceTest#v4testData")
        void should_set_needDeployAt_when_update_triggers_a_synchronization(Api api, Plan plan, List<Flow> flows) {
            // Given
            givenExistingPlan(plan);
            when(planSynchronizationService.checkSynchronized(any(), any(), any(), any())).thenReturn(false);

            // When
            var toUpdate = plan
                .toBuilder()
                .planDefinitionHttpV4(plan.getPlanDefinitionHttpV4().toBuilder().selectionRule("updated rule").build())
                .build();
            var result = service.update(toUpdate, flows, null, api, AUDIT_INFO);

            // Then
            assertThat(result.getNeedRedeployAt()).isEqualTo(Date.from(INSTANT_NOW));
        }
    }

    @Nested
    class Common {

        @ParameterizedTest
        @MethodSource("io.gravitee.apim.core.plan.domain_service.UpdatePlanDomainServiceTest#v4WithFederatedTestData")
        void should_throw_when_invalid_status_change_detected(Api api, Plan plan, List<Flow> flows) {
            // Given
            givenExistingPlan(plan.copy().setPlanStatus(PlanStatus.CLOSED));

            // When
            var throwable = Assertions.catchThrowable(() -> service.update(plan, flows, null, api, AUDIT_INFO));

            // Then
            Assertions.assertThat(throwable).isInstanceOf(ValidationDomainException.class);
        }

        @ParameterizedTest
        @MethodSource("io.gravitee.apim.core.plan.domain_service.UpdatePlanDomainServiceTest#v4WithFederatedTestData")
        void should_save_all_flows(Api api, Plan plan, List<Flow> flows) {
            // Given
            givenExistingPlan(plan);

            // When
            service.update(plan, flows, null, api, AUDIT_INFO);

            // Then
            assertThat(flowCrudService.storage()).containsExactlyElementsOf(flows);
        }

        @ParameterizedTest
        @MethodSource("io.gravitee.apim.core.plan.domain_service.UpdatePlanDomainServiceTest#v4WithFederatedTestData")
        void should_create_an_audit(Api api, Plan plan, List<Flow> flows) {
            // Given
            givenExistingPlan(plan);

            // When
            service.update(plan.toBuilder().name("updated name").build(), flows, null, api, AUDIT_INFO);

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
                        .event(PlanAuditEvent.PLAN_UPDATED.name())
                        .createdAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                        .build()
                );
        }

        @Test
        void should_reorder_all_plans_when_order_is_updated() {
            // Given
            var plans = givenExistingPlans(
                PlanFixtures.HttpV4.aKeyless().toBuilder().id("plan1").order(1).build(),
                PlanFixtures.HttpV4.aKeyless().toBuilder().id("plan2").order(2).build(),
                PlanFixtures.HttpV4.aKeyless().toBuilder().id("plan3").order(3).build()
            );

            // When
            var toUpdate = plans.get(0).toBuilder().order(2).build();
            service.update(toUpdate, List.of(), null, API_PROXY_V4, AUDIT_INFO);

            // Then
            Assertions
                .assertThat(planCrudService.storage())
                .extracting(Plan::getId, Plan::getOrder)
                .containsOnly(tuple("plan2", 1), tuple("plan1", 2), tuple("plan3", 3));
        }
    }

    static Stream<Arguments> v4testData() {
        return Stream.of(
            Arguments.of(
                API_PROXY_V4,
                PlanFixtures.HttpV4
                    .anApiKey()
                    .toBuilder()
                    .apiId(API_ID)
                    .planDefinitionHttpV4(
                        fixtures.definition.PlanFixtures.HttpV4Definition
                            .anApiKeyV4()
                            .toBuilder()
                            .tags(Set.of(TAG))
                            .status(PlanStatus.STAGING)
                            .build()
                    )
                    .build(),
                List.of(Flow.builder().name("flow").selectors(List.of(new HttpSelector())).build())
            ),
            Arguments.of(
                API_MESSAGE_V4,
                PlanFixtures.HttpV4
                    .aPushPlan()
                    .toBuilder()
                    .apiId(API_ID)
                    .planDefinitionHttpV4(
                        fixtures.definition.PlanFixtures.HttpV4Definition
                            .anApiKeyV4()
                            .toBuilder()
                            .tags(Set.of(TAG))
                            .status(PlanStatus.STAGING)
                            .build()
                    )
                    .build(),
                List.of(Flow.builder().name("flow").selectors(List.of(new ChannelSelector())).build())
            )
        );
    }

    static Stream<Arguments> v4WithFederatedTestData() {
        return Stream.concat(v4testData(), Stream.of(Arguments.of(null, PlanFixtures.aFederatedPlan(), List.of())));
    }

    Plan givenExistingPlan(Plan plan) {
        planQueryService.initWith(List.of(plan));
        return plan;
    }

    List<Plan> givenExistingPlans(Plan... plans) {
        var list = Arrays.asList(plans);
        planQueryService.initWith(list);
        return list;
    }
}
