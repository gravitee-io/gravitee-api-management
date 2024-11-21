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

import static org.assertj.core.api.Assertions.assertThat;

import fixtures.core.model.AuditInfoFixtures;
import fixtures.core.model.PlanFixtures;
import inmemory.*;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditEntity;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.event.PlanAuditEvent;
import io.gravitee.apim.core.plan.exception.InvalidPlanStatusForDeprecationException;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.rest.api.service.common.UuidString;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.jupiter.api.*;

class DeprecatePlanDomainServiceTest {

    private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");
    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String USER_ID = "user-id";

    private static final AuditInfo AUDIT_INFO = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);

    PlanCrudServiceInMemory planCrudService = new PlanCrudServiceInMemory();
    AuditCrudServiceInMemory auditCrudService = new AuditCrudServiceInMemory();

    DeprecatePlanDomainService service;

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
            new DeprecatePlanDomainService(
                planCrudService,
                new AuditDomainService(auditCrudService, new UserCrudServiceInMemory(), new JacksonJsonDiffProcessor())
            );
    }

    @AfterEach
    void tearDown() {
        Stream.of(auditCrudService, planCrudService).forEach(InMemoryAlternative::reset);
    }

    @Test
    void should_deprecate_a_plan() {
        // Given
        var plan = givenExistingPlan(PlanFixtures.HttpV4.anApiKey());

        // When
        service.deprecate(plan.getId(), AUDIT_INFO, false);

        // Then
        assertThat(planCrudService.storage().get(0))
            .usingRecursiveComparison(RecursiveComparisonConfiguration.builder().build())
            .isEqualTo(
                PlanFixtures.HttpV4
                    .anApiKey()
                    .toBuilder()
                    .planDefinitionHttpV4(
                        PlanFixtures.HttpV4.anApiKey().getPlanDefinitionHttpV4().toBuilder().status(PlanStatus.DEPRECATED).build()
                    )
                    .updatedAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                    .build()
            );
    }

    @Test
    void should_create_an_audit_when_deprecating_plan_successfully() {
        // Given
        var plan = givenExistingPlan(PlanFixtures.HttpV4.anApiKey());

        // When
        service.deprecate(plan.getId(), AUDIT_INFO, true);

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
                    .referenceId(plan.getApiId())
                    .user(USER_ID)
                    .properties(Map.of("PLAN", plan.getId()))
                    .event(PlanAuditEvent.PLAN_DEPRECATED.name())
                    .createdAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                    .build()
            );
    }

    @Test
    void should_throw_when_plan_already_closed() {
        // Given
        var plan = givenExistingPlan(
            PlanFixtures.HttpV4
                .anApiKey()
                .toBuilder()
                .planDefinitionHttpV4(
                    PlanFixtures.HttpV4
                        .anApiKey()
                        .getPlanDefinitionHttpV4()
                        .toBuilder()
                        .status(io.gravitee.definition.model.v4.plan.PlanStatus.CLOSED)
                        .build()
                )
                .build()
        );

        // When
        var throwable = org.assertj.core.api.Assertions.catchThrowable(() -> service.deprecate(plan.getId(), AUDIT_INFO, true));

        // Then
        assertThat(throwable)
            .isInstanceOf(InvalidPlanStatusForDeprecationException.class)
            .hasMessage("Cannot deprecate plan [ apikey ] with current status [ closed ]");
    }

    @Test
    void should_throw_when_plan_already_deprecated() {
        // Given
        var plan = givenExistingPlan(
            PlanFixtures.HttpV4
                .anApiKey()
                .toBuilder()
                .planDefinitionHttpV4(
                    PlanFixtures.HttpV4.anApiKey().getPlanDefinitionHttpV4().toBuilder().status(PlanStatus.DEPRECATED).build()
                )
                .build()
        );

        // When
        var throwable = org.assertj.core.api.Assertions.catchThrowable(() -> service.deprecate(plan.getId(), AUDIT_INFO, true));

        // Then
        assertThat(throwable)
            .isInstanceOf(InvalidPlanStatusForDeprecationException.class)
            .hasMessage("Cannot deprecate plan [ apikey ] with current status [ deprecated ]");
    }

    @Test
    void should_throw_when_plan_in_staging_and_staging_not_allowed() {
        // Given
        var plan = givenExistingPlan(
            PlanFixtures.HttpV4
                .anApiKey()
                .toBuilder()
                .planDefinitionHttpV4(
                    PlanFixtures.HttpV4.anApiKey().getPlanDefinitionHttpV4().toBuilder().status(PlanStatus.STAGING).build()
                )
                .build()
        );

        // When
        var throwable = org.assertj.core.api.Assertions.catchThrowable(() -> service.deprecate(plan.getId(), AUDIT_INFO, false));

        // Then
        assertThat(throwable)
            .isInstanceOf(InvalidPlanStatusForDeprecationException.class)
            .hasMessage("Cannot deprecate plan [ apikey ] with current status [ staging ]");
    }

    @Test
    void should_deprecate_a_plan_in_staging_with_staging_allowed() {
        // Given
        var plan = givenExistingPlan(
            PlanFixtures.HttpV4
                .anApiKey()
                .toBuilder()
                .planDefinitionHttpV4(
                    PlanFixtures.HttpV4.anApiKey().getPlanDefinitionHttpV4().toBuilder().status(PlanStatus.STAGING).build()
                )
                .build()
        );

        // When
        service.deprecate(plan.getId(), AUDIT_INFO, true);

        // Then
        assertThat(planCrudService.storage().get(0))
            .usingRecursiveComparison(RecursiveComparisonConfiguration.builder().build())
            .isEqualTo(
                PlanFixtures.HttpV4
                    .anApiKey()
                    .toBuilder()
                    .planDefinitionHttpV4(
                        PlanFixtures.HttpV4.anApiKey().getPlanDefinitionHttpV4().toBuilder().status(PlanStatus.DEPRECATED).build()
                    )
                    .updatedAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                    .build()
            );
    }

    Plan givenExistingPlan(Plan plan) {
        planCrudService.initWith(List.of(plan));
        return plan;
    }
}
