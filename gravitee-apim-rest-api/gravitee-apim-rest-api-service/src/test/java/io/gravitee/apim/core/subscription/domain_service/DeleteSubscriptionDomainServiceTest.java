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
package io.gravitee.apim.core.subscription.domain_service;

import static org.assertj.core.api.Assertions.assertThat;

import fixtures.core.model.AuditInfoFixtures;
import fixtures.core.model.SubscriptionFixtures;
import inmemory.AuditCrudServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.SubscriptionCrudServiceInMemory;
import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditEntity;
import io.gravitee.apim.core.audit.model.event.SubscriptionAuditEvent;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.service.common.UuidString;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class DeleteSubscriptionDomainServiceTest {

    private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");
    private static final String ORGANIZATION_ID = "organization-id";
    private static final String USER_ID = "user-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String MY_API = "api-id";

    SubscriptionCrudServiceInMemory subscriptionCrudServiceInMemory = new SubscriptionCrudServiceInMemory();
    UserCrudServiceInMemory userCrudServiceInMemory = new UserCrudServiceInMemory();
    AuditCrudServiceInMemory auditCrudServiceInMemory = new AuditCrudServiceInMemory();

    DeleteSubscriptionDomainService service;

    @BeforeEach
    void setUp() {
        var auditDomainService = new AuditDomainService(auditCrudServiceInMemory, userCrudServiceInMemory, new JacksonJsonDiffProcessor());

        service = new DeleteSubscriptionDomainService(subscriptionCrudServiceInMemory, auditDomainService);
    }

    @AfterEach
    void tearDown() {
        Stream.of(subscriptionCrudServiceInMemory, userCrudServiceInMemory, auditCrudServiceInMemory).forEach(InMemoryAlternative::reset);
    }

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

    @ParameterizedTest
    @EnumSource(value = SubscriptionEntity.Status.class)
    void should_delete_subscription(SubscriptionEntity.Status status) {
        var subscription = SubscriptionFixtures.aSubscription().toBuilder().planId("federated").status(status).build();
        subscriptionCrudServiceInMemory.initWith(List.of(subscription));
        var auditInfo = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);

        service.delete(subscription, auditInfo);

        assertThat(subscriptionCrudServiceInMemory.storage()).isNotNull().isEmpty();
    }

    @Test
    void should_create_audit_log() {
        var subscription = SubscriptionFixtures.aSubscription().toBuilder().planId("federated").build();
        subscriptionCrudServiceInMemory.initWith(List.of(subscription));
        var auditInfo = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);

        service.delete(subscription, auditInfo);

        assertThat(auditCrudServiceInMemory.storage())
            .usingRecursiveFieldByFieldElementComparatorIgnoringFields("patch")
            .containsExactly(
                AuditEntity.builder()
                    .id("generated-id")
                    .organizationId(ORGANIZATION_ID)
                    .environmentId(ENVIRONMENT_ID)
                    .referenceType(AuditEntity.AuditReferenceType.API)
                    .referenceId(MY_API)
                    .user(USER_ID)
                    .event(SubscriptionAuditEvent.SUBSCRIPTION_DELETED.name())
                    .properties(Map.of())
                    .createdAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                    .build()
            );
    }
}
