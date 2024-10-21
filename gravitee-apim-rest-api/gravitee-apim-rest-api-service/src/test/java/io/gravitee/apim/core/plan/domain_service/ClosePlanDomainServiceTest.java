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
import fixtures.core.model.SubscriptionFixtures;
import inmemory.AuditCrudServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.PlanCrudServiceInMemory;
import inmemory.SubscriptionQueryServiceInMemory;
import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditEntity;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.event.PlanAuditEvent;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.service.common.UuidString;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ClosePlanDomainServiceTest {

    private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");
    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String USER_ID = "user-id";

    private static final AuditInfo AUDIT_INFO = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);

    PlanCrudServiceInMemory planCrudService = new PlanCrudServiceInMemory();
    SubscriptionQueryServiceInMemory subscriptionQueryService = new SubscriptionQueryServiceInMemory();
    AuditCrudServiceInMemory auditCrudService = new AuditCrudServiceInMemory();

    ClosePlanDomainService service;

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
            new ClosePlanDomainService(
                planCrudService,
                subscriptionQueryService,
                new AuditDomainService(auditCrudService, new UserCrudServiceInMemory(), new JacksonJsonDiffProcessor())
            );
    }

    @AfterEach
    void tearDown() {
        Stream.of(auditCrudService, planCrudService, subscriptionQueryService).forEach(InMemoryAlternative::reset);
    }

    @Test
    void should_close_a_plan() {
        // Given
        var plan = givenExistingPlan(PlanFixtures.anApiKeyV4());

        // When
        service.close(plan.getId(), AUDIT_INFO);

        // Then
        assertThat(planCrudService.storage().get(0))
            .usingRecursiveComparison(RecursiveComparisonConfiguration.builder().build())
            .isEqualTo(
                PlanFixtures
                    .anApiKeyV4()
                    .toBuilder()
                    .planDefinitionHttpV4(
                        PlanFixtures
                            .anApiKeyV4()
                            .getPlanDefinitionHttpV4()
                            .toBuilder()
                            .status(io.gravitee.definition.model.v4.plan.PlanStatus.CLOSED)
                            .build()
                    )
                    .closedAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                    .updatedAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                    .needRedeployAt(Date.from(INSTANT_NOW.atZone(ZoneId.systemDefault()).toInstant()))
                    .build()
            );
    }

    @Test
    void should_create_an_audit_when_closing_plan_successfully() {
        // Given
        var plan = givenExistingPlan(PlanFixtures.anApiKeyV4());

        // When
        service.close(plan.getId(), AUDIT_INFO);

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
                    .event(PlanAuditEvent.PLAN_CLOSED.name())
                    .createdAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                    .build()
            );
    }

    @ParameterizedTest
    @EnumSource(value = SubscriptionEntity.Status.class, mode = EnumSource.Mode.EXCLUDE, names = { "CLOSED", "REJECTED" })
    void should_throw_when_closing_plan_with_active_subscriptions(SubscriptionEntity.Status status) {
        // Given
        var plan = givenExistingPlan(PlanFixtures.anApiKeyV4());
        givenExistingSubscriptions(SubscriptionFixtures.aSubscription().toBuilder().planId(plan.getId()).status(status).build());

        // When
        var throwable = org.assertj.core.api.Assertions.catchThrowable(() -> service.close(plan.getId(), AUDIT_INFO));

        // Then
        assertThat(throwable)
            .isInstanceOf(ValidationDomainException.class)
            .hasMessage("Impossible to close a plan with active subscriptions");
    }

    @ParameterizedTest
    @EnumSource(value = SubscriptionEntity.Status.class, mode = EnumSource.Mode.INCLUDE, names = { "CLOSED", "REJECTED" })
    void should_close_plan_if_existing_subscriptions_are_inactive(SubscriptionEntity.Status status) {
        // Given
        var plan = givenExistingPlan(PlanFixtures.anApiKeyV4());
        givenExistingSubscriptions(SubscriptionFixtures.aSubscription().toBuilder().planId(plan.getId()).status(status).build());

        // When
        service.close(plan.getId(), AUDIT_INFO);

        // Then
        assertThat(planCrudService.storage().get(0).getPlanStatus()).isEqualTo(io.gravitee.definition.model.v4.plan.PlanStatus.CLOSED);
    }

    @Test
    void should_throw_when_plan_already_closed() {
        // Given
        var plan = givenExistingPlan(
            PlanFixtures
                .anApiKeyV4()
                .toBuilder()
                .planDefinitionHttpV4(
                    PlanFixtures
                        .anApiKeyV4()
                        .getPlanDefinitionHttpV4()
                        .toBuilder()
                        .status(io.gravitee.definition.model.v4.plan.PlanStatus.CLOSED)
                        .build()
                )
                .build()
        );

        // When
        var throwable = org.assertj.core.api.Assertions.catchThrowable(() -> service.close(plan.getId(), AUDIT_INFO));

        // Then
        assertThat(throwable).isInstanceOf(ValidationDomainException.class).hasMessage("Plan apikey is already closed");
    }

    Plan givenExistingPlan(Plan plan) {
        planCrudService.initWith(List.of(plan));
        return plan;
    }

    void givenExistingSubscriptions(SubscriptionEntity... subscriptions) {
        subscriptionQueryService.initWith(Arrays.asList(subscriptions));
    }
}
