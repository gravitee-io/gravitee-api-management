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
package io.gravitee.apim.core.plan.use_case;

import static assertions.CoreAssertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import fakes.FakePolicyValidationDomainService;
import fixtures.core.model.ApiFixtures;
import fixtures.core.model.PlanFixtures;
import fixtures.definition.FlowFixtures;
import inmemory.ApiCrudServiceInMemory;
import inmemory.AuditCrudServiceInMemory;
import inmemory.EntrypointPluginQueryServiceInMemory;
import inmemory.FlowCrudServiceInMemory;
import inmemory.PageCrudServiceInMemory;
import inmemory.ParametersQueryServiceInMemory;
import inmemory.PlanCrudServiceInMemory;
import inmemory.PlanQueryServiceInMemory;
import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.flow.domain_service.FlowValidationDomainService;
import io.gravitee.apim.core.json.JsonDiffProcessor;
import io.gravitee.apim.core.plan.domain_service.PlanSynchronizationService;
import io.gravitee.apim.core.plan.domain_service.PlanValidatorDomainService;
import io.gravitee.apim.core.plan.domain_service.ReorderPlanDomainService;
import io.gravitee.apim.core.plan.domain_service.UpdatePlanDomainService;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.plan.model.PlanUpdates;
import io.gravitee.apim.core.plan.model.PlanWithFlows;
import io.gravitee.apim.infra.domain_service.plan.PlanSynchronizationLegacyWrapper;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.repository.management.model.Parameter;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import io.gravitee.rest.api.service.processor.SynchronizationService;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class UpdatePlanUseCaseTest {

    public static final String API_ID = "api-id";
    public static final String PLAN_ID = "plan-id";
    public static final String DEPRECATED_PLAN_ID = "deprecated-plan-id";
    public static final String DEPRECATED_PLAN_API_ID = "deprecated-plan-api-id";
    public static final String STAGING_PLAN_ID = "staging-plan-id";
    public static final String STAGING_PLAN_API_ID = "staging-plan-api-id";
    public static final String NATIVE_PLAN_ID = "native-plan-id";
    public static final String NATIVE_API_ID = "native-api-id";
    public static final String NEW_TEST_PLAN_ID = "newtest-plan-id";
    public static final String NEW_TEST_PLAN_API_ID = "newtest-plan-api-id";
    private final ObjectMapper objectMapper = new ObjectMapper();
    PlanCrudServiceInMemory planCrudService = new PlanCrudServiceInMemory();
    PlanQueryServiceInMemory planQueryService = new PlanQueryServiceInMemory();
    FlowCrudServiceInMemory flowCrudService = new FlowCrudServiceInMemory();
    FakePolicyValidationDomainService policyValidationDomainService = mock(FakePolicyValidationDomainService.class);
    EntrypointPluginQueryServiceInMemory entrypointConnectorPluginService = new EntrypointPluginQueryServiceInMemory();
    FlowValidationDomainService flowValidationDomainService = new FlowValidationDomainService(
        policyValidationDomainService,
        entrypointConnectorPluginService
    );
    ParametersQueryServiceInMemory parametersQueryService = new ParametersQueryServiceInMemory();
    PageCrudServiceInMemory pageCrudService = new PageCrudServiceInMemory();
    PlanValidatorDomainService planValidatorDomainService = new PlanValidatorDomainService(
        parametersQueryService,
        policyValidationDomainService,
        pageCrudService
    );
    SynchronizationService synchronizationService = new SynchronizationService(objectMapper);
    PlanSynchronizationService planSynchronizationService = new PlanSynchronizationLegacyWrapper(synchronizationService);
    AuditCrudServiceInMemory auditCrudService = new AuditCrudServiceInMemory();
    UserCrudServiceInMemory userCrudService = new UserCrudServiceInMemory();
    JsonDiffProcessor jsonDiffProcessor = new JacksonJsonDiffProcessor();
    AuditDomainService auditDomainService = new AuditDomainService(auditCrudService, userCrudService, jsonDiffProcessor);
    ReorderPlanDomainService reorderPlanDomainService = new ReorderPlanDomainService(planQueryService, planCrudService);
    UpdatePlanDomainService updatePlanDomainService = new UpdatePlanDomainService(
        planQueryService,
        planCrudService,
        planValidatorDomainService,
        flowValidationDomainService,
        flowCrudService,
        auditDomainService,
        planSynchronizationService,
        reorderPlanDomainService
    );
    ApiCrudServiceInMemory apiCrudService = new ApiCrudServiceInMemory();

    UpdatePlanUseCase updatePlanUseCase = new UpdatePlanUseCase(updatePlanDomainService, planCrudService, apiCrudService);

    @BeforeEach
    void setUp() {
        var plan = PlanFixtures.aPlanHttpV4()
            .toBuilder()
            .id(PLAN_ID)
            .apiId(API_ID)
            .referenceId(API_ID)
            .referenceType(GenericPlanEntity.ReferenceType.API)
            .order(1)
            .build();
        var anotherPlan = PlanFixtures.aPlanHttpV4()
            .toBuilder()
            .id("another-plan-id")
            .apiId(API_ID)
            .referenceId(API_ID)
            .referenceType(GenericPlanEntity.ReferenceType.API)
            .order(2)
            .build();
        var deprecatedPlan = PlanFixtures.aPlanHttpV4()
            .toBuilder()
            .id(DEPRECATED_PLAN_ID)
            .apiId(DEPRECATED_PLAN_API_ID)
            .referenceId(DEPRECATED_PLAN_API_ID)
            .referenceType(GenericPlanEntity.ReferenceType.API)
            .order(1)
            .build();
        deprecatedPlan.getPlanDefinitionV4().setStatus(PlanStatus.DEPRECATED);
        var stagingPlan = PlanFixtures.aPlanHttpV4()
            .toBuilder()
            .id(STAGING_PLAN_ID)
            .apiId(STAGING_PLAN_API_ID)
            .referenceId(STAGING_PLAN_API_ID)
            .referenceType(GenericPlanEntity.ReferenceType.API)
            .order(1)
            .build();
        stagingPlan.getPlanDefinitionV4().setStatus(PlanStatus.STAGING);
        var nativePlan = PlanFixtures.aPlanNativeV4()
            .toBuilder()
            .id(NATIVE_PLAN_ID)
            .apiId(NATIVE_API_ID)
            .referenceId(NATIVE_API_ID)
            .referenceType(GenericPlanEntity.ReferenceType.API)
            .order(3)
            .build();
        var newTestPlan = PlanFixtures.aPlanHttpV4()
            .toBuilder()
            .id(NEW_TEST_PLAN_ID)
            .apiId(NEW_TEST_PLAN_API_ID)
            .referenceId(NEW_TEST_PLAN_API_ID)
            .referenceType(GenericPlanEntity.ReferenceType.API)
            .order(2)
            .build();
        newTestPlan.getPlanDefinitionV4().setSecurity(null);
        newTestPlan.setPlanMode(PlanMode.PUSH);
        List<Plan> allPlans = List.of(plan, anotherPlan, deprecatedPlan, stagingPlan, nativePlan, newTestPlan);
        planCrudService.initWith(allPlans);
        planQueryService.initWith(allPlans);
        apiCrudService.initWith(
            List.of(
                ApiFixtures.aProxyApiV4().toBuilder().id(API_ID).build(),
                ApiFixtures.aProxyApiV4().toBuilder().id(DEPRECATED_PLAN_API_ID).build(),
                ApiFixtures.aProxyApiV4().toBuilder().id(STAGING_PLAN_API_ID).build(),
                ApiFixtures.aNativeApi().toBuilder().id(NATIVE_API_ID).build(),
                ApiFixtures.aProxyApiV4().toBuilder().id(NEW_TEST_PLAN_API_ID).build()
            )
        );

        parametersQueryService.initWith(
            List.of(
                Parameter.builder().key(Key.PLAN_SECURITY_KEYLESS_ENABLED.key()).value("true").build(),
                Parameter.builder().key(Key.PLAN_SECURITY_APIKEY_ENABLED.key()).value("true").build()
            )
        );
    }

    @ParameterizedTest
    @MethodSource("minMaxPlans")
    void should_update_plan(PlanUpdates updatePlan) {
        // Given
        var input = new UpdatePlanUseCase.Input(
            updatePlan,
            _api -> Collections.singletonList(FlowFixtures.aProxyFlowV4()),
            API_ID,
            new AuditInfo("user-id", "user-name", AuditActor.builder().build())
        );

        // When
        var output = updatePlanUseCase.execute(input);

        // Then
        assertThat(output)
            .isNotNull()
            .extracting(UpdatePlanUseCase.Output::updated)
            .extracting(
                PlanWithFlows::getId,
                PlanWithFlows::getCrossId,
                PlanWithFlows::getName,
                PlanWithFlows::getDescription,
                PlanWithFlows::getOrder,
                planWithFlows -> planWithFlows.getPlanSecurity().getConfiguration(),
                PlanWithFlows::getCharacteristics,
                PlanWithFlows::getExcludedGroups,
                PlanWithFlows::isCommentRequired,
                PlanWithFlows::getCommentMessage,
                PlanWithFlows::getGeneralConditions,
                PlanWithFlows::getPlanTags,
                PlanWithFlows::getSelectionRule
            )
            .containsExactly(
                PLAN_ID,
                "my-plan-crossId",
                updatePlan.getName(),
                updatePlan.getDescription(),
                updatePlan.getOrder(),
                updatePlan.getSecurityConfiguration(),
                updatePlan.getCharacteristics(),
                updatePlan.getExcludedGroups(),
                updatePlan.isCommentRequired(),
                updatePlan.getCommentMessage(),
                updatePlan.getGeneralConditions(),
                updatePlan.getTags(),
                updatePlan.getSelectionRule()
            );
    }

    @Test
    void should_update_plan_when_security_is_null() {
        // Given
        PlanUpdates updatePlan = planMinimal().toBuilder().id(NEW_TEST_PLAN_ID).order(1).build();
        var input = new UpdatePlanUseCase.Input(
            updatePlan,
            _api -> Collections.singletonList(FlowFixtures.aProxyFlowV4()),
            NEW_TEST_PLAN_API_ID,
            new AuditInfo("user-id", "user-name", AuditActor.builder().build())
        );

        // When
        var output = updatePlanUseCase.execute(input);

        // Then
        assertThat(output)
            .isNotNull()
            .extracting(UpdatePlanUseCase.Output::updated)
            .extracting(
                PlanWithFlows::getId,
                PlanWithFlows::getCrossId,
                PlanWithFlows::getName,
                PlanWithFlows::getDescription,
                PlanWithFlows::getOrder,
                PlanWithFlows::getCharacteristics,
                PlanWithFlows::getExcludedGroups,
                PlanWithFlows::isCommentRequired,
                PlanWithFlows::getCommentMessage,
                PlanWithFlows::getGeneralConditions,
                PlanWithFlows::getPlanTags,
                PlanWithFlows::getSelectionRule
            )
            .containsExactly(
                NEW_TEST_PLAN_ID,
                "my-plan-crossId",
                updatePlan.getName(),
                updatePlan.getDescription(),
                updatePlan.getOrder(),
                updatePlan.getCharacteristics(),
                updatePlan.getExcludedGroups(),
                updatePlan.isCommentRequired(),
                updatePlan.getCommentMessage(),
                updatePlan.getGeneralConditions(),
                updatePlan.getTags(),
                updatePlan.getSelectionRule()
            );
    }

    @Test
    void should_update_native_plan() {
        // Given
        PlanUpdates updatePlan = fullPlan().toBuilder().id(NATIVE_PLAN_ID).order(1).build();
        var input = new UpdatePlanUseCase.Input(
            updatePlan,
            _api -> Collections.singletonList(FlowFixtures.aNativeFlowV4()),
            NATIVE_API_ID,
            new AuditInfo("user-id", "user-name", AuditActor.builder().build())
        );

        // When
        var output = updatePlanUseCase.execute(input);

        // Then
        assertThat(output)
            .isNotNull()
            .extracting(UpdatePlanUseCase.Output::updated)
            .extracting(
                PlanWithFlows::getId,
                PlanWithFlows::getCrossId,
                PlanWithFlows::getName,
                PlanWithFlows::getDescription,
                PlanWithFlows::getOrder,
                planWithFlows -> planWithFlows.getPlanSecurity().getConfiguration(),
                PlanWithFlows::getCharacteristics,
                PlanWithFlows::getExcludedGroups,
                PlanWithFlows::isCommentRequired,
                PlanWithFlows::getCommentMessage,
                PlanWithFlows::getGeneralConditions,
                PlanWithFlows::getPlanTags,
                PlanWithFlows::getSelectionRule
            )
            .containsExactly(
                NATIVE_PLAN_ID,
                "my-plan-crossId",
                updatePlan.getName(),
                updatePlan.getDescription(),
                updatePlan.getOrder(),
                updatePlan.getSecurityConfiguration(),
                updatePlan.getCharacteristics(),
                updatePlan.getExcludedGroups(),
                updatePlan.isCommentRequired(),
                updatePlan.getCommentMessage(),
                updatePlan.getGeneralConditions(),
                updatePlan.getTags(),
                updatePlan.getSelectionRule()
            );
    }

    @Test
    void should_reject_when_tag_mismatch() {
        // Given
        var input = new UpdatePlanUseCase.Input(
            planMinimal().toBuilder().tags(Set.of("tag2", "tag3")).build(),
            _api -> Collections.singletonList(FlowFixtures.aProxyFlowV4()),
            API_ID,
            new AuditInfo("user-id", "user-name", AuditActor.builder().build())
        );

        // When
        var exception = org.junit.jupiter.api.Assertions.assertThrows(ValidationDomainException.class, () ->
            updatePlanUseCase.execute(input)
        );

        // Then
        assertThat(exception).hasMessage("Plan tags mismatch the tags defined by the API");
    }

    @Test
    void should_reject_with_invalid_security() {
        // Given
        when(policyValidationDomainService.validateAndSanitizeConfiguration(any(), any())).thenThrow(
            new InvalidDataException("Invalid configuration for policy " + "api-key")
        );
        var input = new UpdatePlanUseCase.Input(
            planMinimal().toBuilder().securityConfiguration("anything").build(),
            _api -> Collections.singletonList(FlowFixtures.aProxyFlowV4()),
            API_ID,
            new AuditInfo("user-id", "user-name", AuditActor.builder().build())
        );

        // When
        var exception = org.junit.jupiter.api.Assertions.assertThrows(InvalidDataException.class, () -> updatePlanUseCase.execute(input));

        // Then
        assertThat(exception).hasMessage("Invalid configuration for policy api-key");
    }

    @Nested
    class GeneralConditionsPage {

        @Test
        void should_update_with_published_page() {
            // Given
            var page = Page.builder().id("page-id").published(true).build();
            pageCrudService.initWith(List.of(page));

            var input = new UpdatePlanUseCase.Input(
                planMinimal().toBuilder().generalConditions("page-id").build(),
                _api -> Collections.singletonList(FlowFixtures.aProxyFlowV4()),
                API_ID,
                new AuditInfo("user-id", "user-name", AuditActor.builder().build())
            );

            // When
            var output = updatePlanUseCase.execute(input);

            // Then
            assertThat(output)
                .isNotNull()
                .extracting(UpdatePlanUseCase.Output::updated)
                .extracting(PlanWithFlows::getId, PlanWithFlows::getGeneralConditions)
                .containsExactly(PLAN_ID, "page-id");
        }

        @Test
        void should_update_staging_plan_with_unpublished_page() {
            // Given
            var page = Page.builder().id("page-id").published(false).build();
            pageCrudService.initWith(List.of(page));

            var input = new UpdatePlanUseCase.Input(
                planMinimal().toBuilder().id(STAGING_PLAN_ID).generalConditions("page-id").build(),
                _api -> Collections.singletonList(FlowFixtures.aProxyFlowV4()),
                STAGING_PLAN_API_ID,
                new AuditInfo("user-id", "user-name", AuditActor.builder().build())
            );

            // When
            var output = updatePlanUseCase.execute(input);

            // Then
            assertThat(output)
                .isNotNull()
                .extracting(UpdatePlanUseCase.Output::updated)
                .extracting(PlanWithFlows::getId, PlanWithFlows::getGeneralConditions)
                .containsExactly(STAGING_PLAN_ID, "page-id");
        }

        @Test
        void should_reject_with_unpublished_page_and_published_plan() {
            // Given
            var page = Page.builder().id("page-id").published(false).build();
            pageCrudService.initWith(List.of(page));

            var input = new UpdatePlanUseCase.Input(
                planMinimal().toBuilder().generalConditions("page-id").build(),
                _api -> Collections.singletonList(FlowFixtures.aProxyFlowV4()),
                API_ID,
                new AuditInfo("user-id", "user-name", AuditActor.builder().build())
            );

            // When
            var exception = org.junit.jupiter.api.Assertions.assertThrows(ValidationDomainException.class, () ->
                updatePlanUseCase.execute(input)
            );

            // Then
            assertThat(exception).hasMessage("Plan references a non published page as general conditions");
        }

        @Test
        void should_reject_with_unpublished_page_and_deprecated_plan() {
            // Given
            var page = Page.builder().id("page-id").published(false).build();
            pageCrudService.initWith(List.of(page));

            var input = new UpdatePlanUseCase.Input(
                planMinimal().toBuilder().id(DEPRECATED_PLAN_ID).generalConditions("page-id").build(),
                _api -> Collections.singletonList(FlowFixtures.aProxyFlowV4()),
                DEPRECATED_PLAN_API_ID,
                new AuditInfo("user-id", "user-name", AuditActor.builder().build())
            );

            // When
            var exception = org.junit.jupiter.api.Assertions.assertThrows(ValidationDomainException.class, () ->
                updatePlanUseCase.execute(input)
            );

            // Then
            assertThat(exception).hasMessage("Plan references a non published page as general conditions");
        }
    }

    @Nested
    class Flows {

        @Test
        void should_update_with_flow() {
            // Given
            when(policyValidationDomainService.validateAndSanitizeConfiguration(any(), any())).thenAnswer(invocation ->
                invocation.getArgument(1)
            );
            var input = new UpdatePlanUseCase.Input(
                planMinimal(),
                _api -> Collections.singletonList(FlowFixtures.aProxyFlowV4()),
                API_ID,
                new AuditInfo("user-id", "user-name", AuditActor.builder().build())
            );

            // When
            var output = updatePlanUseCase.execute(input);

            // Then
            assertThat(output)
                .isNotNull()
                .extracting(UpdatePlanUseCase.Output::updated)
                .extracting(PlanWithFlows::getId, PlanWithFlows::getFlows)
                .containsExactly(PLAN_ID, List.of(FlowFixtures.aProxyFlowV4()));
        }

        @Test
        void should_update_without_flows() {
            // Given
            var input = new UpdatePlanUseCase.Input(
                planMinimal(),
                _api -> Collections.emptyList(),
                API_ID,
                new AuditInfo("user-id", "user-name", AuditActor.builder().build())
            );

            // When
            var output = updatePlanUseCase.execute(input);

            // Then
            assertThat(output)
                .isNotNull()
                .extracting(UpdatePlanUseCase.Output::updated)
                .extracting(PlanWithFlows::getId, PlanWithFlows::getFlows)
                .containsExactly(PLAN_ID, Collections.emptyList());
        }
    }

    static Stream<Arguments> minMaxPlans() {
        var minPlan = planMinimal();

        var maxPlan = fullPlan();

        return Stream.of(Arguments.of(minPlan), Arguments.of(maxPlan));
    }

    private static PlanUpdates fullPlan() {
        return planMinimal()
            .toBuilder()
            .characteristics(List.of("characteristic1", "characteristic2"))
            .excludedGroups(List.of("group1", "group2"))
            .securityConfiguration(null)
            .commentRequired(true)
            .commentMessage("comment-message")
            .generalConditions("general-conditions")
            .tags(Set.of("tag1", "tag2"))
            .selectionRule("selection-rule")
            .build();
    }

    private static @NotNull PlanUpdates planMinimal() {
        return PlanUpdates.builder()
            .id(PLAN_ID)
            .crossId("my-plan-crossId")
            .name("plan-name-changed")
            .description("plan-description-changed")
            .order(2)
            .build();
    }

    @Test
    void should_use_existing_validation_when_null_is_passed() {
        var existingPlan = PlanFixtures.aPlanHttpV4()
            .toBuilder()
            .id(PLAN_ID)
            .apiId(API_ID)
            .referenceId(API_ID)
            .referenceType(GenericPlanEntity.ReferenceType.API)
            .validation(Plan.PlanValidationType.AUTO)
            .build();
        planCrudService.initWith(List.of(existingPlan));

        var updatePlan = planMinimal()
            .toBuilder()
            .validation(null) // <- null validation in update request
            .build();

        var input = new UpdatePlanUseCase.Input(
            updatePlan,
            _api -> Collections.singletonList(FlowFixtures.aProxyFlowV4()),
            API_ID,
            new AuditInfo("user-id", "user-name", AuditActor.builder().build())
        );
        var output = updatePlanUseCase.execute(input);
        assertThat(output)
            .isNotNull()
            .extracting(UpdatePlanUseCase.Output::updated)
            .extracting(PlanWithFlows::getValidation)
            .isEqualTo(Plan.PlanValidationType.AUTO);
    }
}
