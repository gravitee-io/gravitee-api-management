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
package io.gravitee.apim.core.api_product.use_case;

import static assertions.CoreAssertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import fakes.FakePolicyValidationDomainService;
import fixtures.PlanModelFixtures;
import fixtures.core.model.ApiFixtures;
import fixtures.core.model.AuditInfoFixtures;
import fixtures.core.model.PlanFixtures;
import fixtures.definition.FlowFixtures;
import inmemory.ApiCrudServiceInMemory;
import inmemory.ApiProductCrudServiceInMemory;
import inmemory.ApiProductQueryServiceInMemory;
import inmemory.AuditCrudServiceInMemory;
import inmemory.EntrypointPluginQueryServiceInMemory;
import inmemory.FlowCrudServiceInMemory;
import inmemory.PageCrudServiceInMemory;
import inmemory.ParametersQueryServiceInMemory;
import inmemory.PlanCrudServiceInMemory;
import inmemory.PlanQueryServiceInMemory;
import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.flow.crud_service.FlowCrudService;
import io.gravitee.apim.core.flow.domain_service.FlowValidationDomainService;
import io.gravitee.apim.core.json.JsonDiffProcessor;
import io.gravitee.apim.core.plan.crud_service.PlanCrudService;
import io.gravitee.apim.core.plan.domain_service.CreatePlanDomainService;
import io.gravitee.apim.core.plan.domain_service.PlanSynchronizationService;
import io.gravitee.apim.core.plan.domain_service.PlanValidatorDomainService;
import io.gravitee.apim.core.plan.domain_service.ReorderPlanDomainService;
import io.gravitee.apim.core.plan.domain_service.UpdatePlanDomainService;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.plan.model.PlanUpdates;
import io.gravitee.apim.core.plan.model.PlanWithFlows;
import io.gravitee.apim.core.plan.query_service.PlanQueryService;
import io.gravitee.apim.core.plan.use_case.api_product.UpdateApiProductPlanUseCase;
import io.gravitee.apim.core.policy.domain_service.PolicyValidationDomainService;
import io.gravitee.apim.infra.domain_service.plan.PlanSynchronizationLegacyWrapper;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.repository.management.model.Parameter;
import io.gravitee.repository.management.model.PlanReferenceType;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import io.gravitee.rest.api.service.processor.SynchronizationService;
import io.gravitee.rest.api.service.v4.PlanSearchService;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class UpdateApiProductPlanUseCaseTest {

    public static final String API_PRODUCT_ID = "api-product-id";
    public static final String PLAN_ID = "plan-id";
    public static final String DEPRECATED_PLAN_ID = "deprecated-plan-id";
    public static final String DEPRECATED_PLAN_API_ID = "deprecated-plan-api-id";
    public static final String STAGING_PLAN_ID = "staging-plan-id";
    public static final String STAGING_PLAN_API_ID = "staging-plan-api-id";
    public static final String NATIVE_PLAN_ID = "native-plan-id";
    public static final String NATIVE_API_ID = "native-api-id";
    public static final String NEW_TEST_PLAN_ID = "newtest-plan-id";
    public static final String NEW_TEST_PLAN_API_ID = "newtest-plan-api-id";
    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String USER_ID = "user-id";
    private static final AuditInfo AUDIT_INFO = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);
    private static final String GENERATED_ID = "generated-id";

    PolicyValidationDomainService policyValidationDomainService = mock(PolicyValidationDomainService.class);
    PlanCrudServiceInMemory planCrudService = new PlanCrudServiceInMemory();
    FlowCrudServiceInMemory flowCrudService = new FlowCrudServiceInMemory();
    AuditCrudServiceInMemory auditCrudService = new AuditCrudServiceInMemory();
    ParametersQueryServiceInMemory parametersQueryService = new ParametersQueryServiceInMemory();
    ApiProductCrudServiceInMemory apiProductCrudServiceInMemory = new ApiProductCrudServiceInMemory();
    PlanSearchService planSearchService = mock(PlanSearchService.class);
    AuditDomainService auditDomainService = new AuditDomainService(
        auditCrudService,
        new UserCrudServiceInMemory(),
        new JacksonJsonDiffProcessor()
    );
    PageCrudServiceInMemory pageCrudService = new PageCrudServiceInMemory();
    PlanValidatorDomainService planValidatorService = new PlanValidatorDomainService(
        parametersQueryService,
        policyValidationDomainService,
        pageCrudService
    );
    FlowValidationDomainService flowValidationDomainService = new FlowValidationDomainService(
        policyValidationDomainService,
        new EntrypointPluginQueryServiceInMemory()
    );
    PlanQueryServiceInMemory planQueryService = new PlanQueryServiceInMemory();
    PlanValidatorDomainService planValidatorDomainService = new PlanValidatorDomainService(
        parametersQueryService,
        policyValidationDomainService,
        pageCrudService
    );
    private final ObjectMapper objectMapper = new ObjectMapper();
    SynchronizationService synchronizationService = new SynchronizationService(objectMapper);

    PlanSynchronizationService planSynchronizationService = new PlanSynchronizationLegacyWrapper(synchronizationService);
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
    private final ApiProductQueryServiceInMemory apiProductQueryService = new ApiProductQueryServiceInMemory();

    UpdateApiProductPlanUseCase updatePlanUseCase = new UpdateApiProductPlanUseCase(
        updatePlanDomainService,
        planCrudService,
        apiProductCrudServiceInMemory,
        planSearchService
    );

    @BeforeEach
    void setUp() {
        var plan = PlanFixtures.aPlanHttpV4()
            .toBuilder()
            .id(PLAN_ID)
            .apiId(API_PRODUCT_ID)
            .referenceId(API_PRODUCT_ID)
            .referenceType(GenericPlanEntity.ReferenceType.API_PRODUCT)
            .order(1)
            .build();
        var anotherPlan = PlanFixtures.aPlanHttpV4()
            .toBuilder()
            .id("another-plan-id")
            .apiId(API_PRODUCT_ID)
            .referenceId(API_PRODUCT_ID)
            .referenceType(GenericPlanEntity.ReferenceType.API_PRODUCT)
            .order(2)
            .build();
        var deprecatedPlan = PlanFixtures.aPlanHttpV4()
            .toBuilder()
            .id(DEPRECATED_PLAN_ID)
            .apiId(DEPRECATED_PLAN_API_ID)
            .referenceId(API_PRODUCT_ID)
            .referenceType(GenericPlanEntity.ReferenceType.API_PRODUCT)
            .order(1)
            .build();
        deprecatedPlan.getPlanDefinitionV4().setStatus(PlanStatus.DEPRECATED);
        var stagingPlan = PlanFixtures.aPlanHttpV4()
            .toBuilder()
            .id(STAGING_PLAN_ID)
            .apiId(STAGING_PLAN_API_ID)
            .referenceId(API_PRODUCT_ID)
            .referenceType(GenericPlanEntity.ReferenceType.API_PRODUCT)
            .order(1)
            .build();
        stagingPlan.getPlanDefinitionV4().setStatus(PlanStatus.STAGING);
        var nativePlan = PlanFixtures.aPlanNativeV4()
            .toBuilder()
            .id(NATIVE_PLAN_ID)
            .apiId(NATIVE_API_ID)
            .referenceId(API_PRODUCT_ID)
            .referenceType(GenericPlanEntity.ReferenceType.API_PRODUCT)
            .order(3)
            .build();
        var newTestPlan = PlanFixtures.aPlanHttpV4()
            .toBuilder()
            .id(NEW_TEST_PLAN_ID)
            .apiId(NEW_TEST_PLAN_API_ID)
            .referenceId(API_PRODUCT_ID)
            .referenceType(GenericPlanEntity.ReferenceType.API_PRODUCT)
            .order(2)
            .build();
        newTestPlan.getPlanDefinitionV4().setSecurity(null);
        newTestPlan.setPlanMode(PlanMode.PUSH);
        List<Plan> allPlans = List.of(plan, anotherPlan, deprecatedPlan, stagingPlan, nativePlan, newTestPlan);
        planCrudService.initWith(allPlans);
        planQueryService.initWith(allPlans);
        apiProductCrudServiceInMemory.initWith(
            List.of(
                BASE(API_PRODUCT_ID).id(API_PRODUCT_ID).build(),
                BASE(DEPRECATED_PLAN_API_ID).id(DEPRECATED_PLAN_API_ID).build(),
                BASE(STAGING_PLAN_API_ID).id(STAGING_PLAN_API_ID).build(),
                BASE(NATIVE_API_ID).id(NATIVE_API_ID).build(),
                BASE(NEW_TEST_PLAN_API_ID).id(NEW_TEST_PLAN_API_ID).build()
            )
        );

        parametersQueryService.initWith(
            List.of(
                Parameter.builder().key(Key.PLAN_SECURITY_KEYLESS_ENABLED.key()).value("true").build(),
                Parameter.builder().key(Key.PLAN_SECURITY_APIKEY_ENABLED.key()).value("true").build()
            )
        );

        GraviteeContext.fromExecutionContext(new io.gravitee.rest.api.service.common.ExecutionContext(ORGANIZATION_ID, ENVIRONMENT_ID));
        when(planSearchService.findByPlanIdIdForApiProduct(any(), any(), eq(API_PRODUCT_ID))).thenReturn(
            PlanModelFixtures.aPlanEntityV4()
                .toBuilder()
                .referenceId(API_PRODUCT_ID)
                .referenceType(GenericPlanEntity.ReferenceType.API_PRODUCT)
                .build()
        );
    }

    @AfterEach
    void tearDown() {
        GraviteeContext.cleanContext();
    }

    @ParameterizedTest
    @MethodSource("minMaxPlans")
    void should_update_plan(PlanUpdates updatePlan) {
        // Given
        var input = new UpdateApiProductPlanUseCase.Input(
            updatePlan,
            API_PRODUCT_ID,
            new AuditInfo("user-id", "user-name", AuditActor.builder().build())
        );

        // When
        var output = updatePlanUseCase.execute(input);

        // Then
        assertThat(output)
            .isNotNull()
            .extracting(UpdateApiProductPlanUseCase.Output::updated)
            .extracting(
                Plan::getId,
                Plan::getCrossId,
                Plan::getName,
                Plan::getDescription,
                Plan::getOrder,
                plan -> plan.getPlanSecurity().getConfiguration(),
                Plan::getCharacteristics,
                Plan::getExcludedGroups,
                Plan::isCommentRequired,
                Plan::getCommentMessage,
                Plan::getGeneralConditions,
                Plan::getPlanTags,
                Plan::getSelectionRule
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
        updatePlan.setReferenceId(API_PRODUCT_ID);
        updatePlan.setReferenceType(GenericPlanEntity.ReferenceType.API_PRODUCT);
        when(planSearchService.findByPlanIdIdForApiProduct(any(), eq(NEW_TEST_PLAN_ID), eq(API_PRODUCT_ID))).thenReturn(
            PlanModelFixtures.aPlanEntityV4()
                .toBuilder()
                .id(NEW_TEST_PLAN_ID)
                .referenceId(API_PRODUCT_ID)
                .apiId(null)
                .referenceType(GenericPlanEntity.ReferenceType.API_PRODUCT)
                .build()
        );
        var input = new UpdateApiProductPlanUseCase.Input(
            updatePlan,
            API_PRODUCT_ID,
            new AuditInfo("user-id", "user-name", AuditActor.builder().build())
        );

        // When
        var output = updatePlanUseCase.execute(input);

        // Then
        assertThat(output)
            .isNotNull()
            .extracting(UpdateApiProductPlanUseCase.Output::updated)
            .extracting(
                Plan::getId,
                Plan::getCrossId,
                Plan::getName,
                Plan::getDescription,
                Plan::getOrder,
                Plan::getCharacteristics,
                Plan::getExcludedGroups,
                Plan::isCommentRequired,
                Plan::getCommentMessage,
                Plan::getGeneralConditions,
                Plan::getPlanTags,
                Plan::getSelectionRule
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
    void should_reject_with_invalid_security() {
        // Given
        when(policyValidationDomainService.validateAndSanitizeConfiguration(any(), any())).thenThrow(
            new InvalidDataException("Invalid configuration for policy " + "api-key")
        );
        var input = new UpdateApiProductPlanUseCase.Input(
            planMinimal().toBuilder().securityConfiguration("anything").build(),
            API_PRODUCT_ID,
            new AuditInfo("user-id", "user-name", AuditActor.builder().build())
        );

        // When
        var exception = org.junit.jupiter.api.Assertions.assertThrows(InvalidDataException.class, () -> updatePlanUseCase.execute(input));

        // Then
        assertThat(exception).hasMessage("Invalid configuration for policy api-key");
    }

    @Test
    void should_use_existing_validation_when_null_is_passed() {
        var existingPlan = PlanFixtures.aPlanHttpV4()
            .toBuilder()
            .id(PLAN_ID)
            .apiId(API_PRODUCT_ID)
            .referenceType(GenericPlanEntity.ReferenceType.API_PRODUCT)
            .referenceId(API_PRODUCT_ID)
            .validation(Plan.PlanValidationType.AUTO)
            .build();
        planCrudService.initWith(List.of(existingPlan));
        planQueryService.initWith(List.of(existingPlan));

        when(planSearchService.findByPlanIdIdForApiProduct(any(), eq(PLAN_ID), eq(API_PRODUCT_ID))).thenReturn(
            PlanModelFixtures.aPlanEntityV4()
                .toBuilder()
                .id(PLAN_ID)
                .referenceId(API_PRODUCT_ID)
                .referenceType(GenericPlanEntity.ReferenceType.API_PRODUCT)
                .build()
        );

        var updatePlan = planMinimal()
            .toBuilder()
            .validation(null) // <- null validation in update request
            .build();

        var input = new UpdateApiProductPlanUseCase.Input(
            updatePlan,
            API_PRODUCT_ID,
            new AuditInfo("user-id", "user-name", AuditActor.builder().build())
        );
        var output = updatePlanUseCase.execute(input);
        assertThat(output)
            .isNotNull()
            .extracting(UpdateApiProductPlanUseCase.Output::updated)
            .extracting(Plan::getValidation)
            .isEqualTo(Plan.PlanValidationType.AUTO);
    }

    private io.gravitee.apim.core.api_product.model.ApiProduct.ApiProductBuilder BASE(String id) {
        return io.gravitee.apim.core.api_product.model.ApiProduct.builder()
            .id(id)
            .name(id)
            .environmentId("environment-id")
            .description("api-product-description")
            .version("1.0.0")
            .createdAt(Instant.parse("2020-02-01T20:22:02.00Z").atZone(ZoneId.systemDefault()))
            .updatedAt(Instant.parse("2020-02-02T20:22:02.00Z").atZone(ZoneId.systemDefault()));
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
            .referenceId(API_PRODUCT_ID)
            .referenceType(GenericPlanEntity.ReferenceType.API_PRODUCT)
            .build();
    }

    private static @NotNull PlanUpdates planMinimal() {
        return PlanUpdates.builder()
            .id(PLAN_ID)
            .crossId("my-plan-crossId")
            .name("plan-name-changed")
            .description("plan-description-changed")
            .referenceId(API_PRODUCT_ID)
            .referenceType(GenericPlanEntity.ReferenceType.API_PRODUCT)
            .order(2)
            .build();
    }
}
