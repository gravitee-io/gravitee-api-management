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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;

import assertions.CoreAssertions;
import fixtures.core.model.AuditInfoFixtures;
import fixtures.core.model.PlanFixtures;
import inmemory.AuditCrudServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.PageCrudServiceInMemory;
import inmemory.ParametersQueryServiceInMemory;
import inmemory.PlanCrudServiceInMemory;
import inmemory.PlanQueryServiceInMemory;
import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditEntity;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.event.PlanAuditEvent;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.plan.domain_service.PlanValidatorDomainService;
import io.gravitee.apim.core.plan.domain_service.ReorderPlanDomainService;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.policy.domain_service.PolicyValidationDomainService;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.rest.api.service.common.UuidString;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UpdateFederatedPlanUseCaseTest {

    private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");
    private static final String API_ID = "my-api";
    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String USER_ID = "user-id";

    private static final AuditInfo AUDIT_INFO = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);

    AuditCrudServiceInMemory auditCrudService = new AuditCrudServiceInMemory();
    PolicyValidationDomainService policyValidationDomainService = mock(PolicyValidationDomainService.class);
    PageCrudServiceInMemory pageCrudService = new PageCrudServiceInMemory();
    ParametersQueryServiceInMemory parametersQueryService = new ParametersQueryServiceInMemory();
    PlanCrudServiceInMemory planCrudService = new PlanCrudServiceInMemory();
    PlanQueryServiceInMemory planQueryService = new PlanQueryServiceInMemory(planCrudService);

    UpdateFederatedPlanUseCase useCase;

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
        var planValidatorService = new PlanValidatorDomainService(parametersQueryService, policyValidationDomainService, pageCrudService);
        var reorderPlanDomainService = new ReorderPlanDomainService(planQueryService, planCrudService);
        useCase = new UpdateFederatedPlanUseCase(planCrudService, planValidatorService, reorderPlanDomainService, auditDomainService);
    }

    @AfterEach
    public void tearDown() {
        Stream.of(auditCrudService, pageCrudService, parametersQueryService, planCrudService).forEach(InMemoryAlternative::reset);
    }

    @Test
    void should_update_federated_plan_attributes() {
        // Given
        var plan = givenExistingPlan(PlanFixtures.aFederatedPlan());

        // When
        var result = useCase.execute(
            new UpdateFederatedPlanUseCase.Input(
                plan
                    .toBuilder()
                    .name("new name")
                    .description("new description")
                    .validation(Plan.PlanValidationType.AUTO)
                    .characteristics(List.of("new characteristic"))
                    .commentMessage("new comment message")
                    .commentRequired(true)
                    .build(),
                AUDIT_INFO
            )
        );

        // Then
        CoreAssertions
            .assertThat(planCrudService.findById(plan.getId()))
            .isEqualTo(result.updated())
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
    void should_create_an_audit() {
        // Given
        var plan = givenExistingPlan(PlanFixtures.aFederatedPlan());

        // When
        useCase.execute(new UpdateFederatedPlanUseCase.Input(plan.toBuilder().name("new name").build(), AUDIT_INFO));

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
            PlanFixtures.aFederatedPlan().toBuilder().id("plan1").order(1).build(),
            PlanFixtures.aFederatedPlan().toBuilder().id("plan2").order(2).build(),
            PlanFixtures.aFederatedPlan().toBuilder().id("plan3").order(3).build()
        );

        // When
        var toUpdate = plans.get(0).toBuilder().order(2).build();
        useCase.execute(new UpdateFederatedPlanUseCase.Input(toUpdate, AUDIT_INFO));

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

        // When
        var throwable = Assertions.catchThrowable(() ->
            useCase.execute(new UpdateFederatedPlanUseCase.Input(plan.toBuilder().generalConditions("page-id").build(), AUDIT_INFO))
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
            useCase.execute(new UpdateFederatedPlanUseCase.Input(plan.copy().setPlanStatus(PlanStatus.DEPRECATED), AUDIT_INFO))
        );

        // Then
        Assertions.assertThat(throwable).isInstanceOf(ValidationDomainException.class);
    }

    private Plan givenExistingPlan(Plan plan) {
        planCrudService.initWith(List.of(plan));
        return plan;
    }

    List<Plan> givenExistingPlans(Plan... plans) {
        var list = Arrays.asList(plans);
        planQueryService.initWith(list);
        return list;
    }
}
