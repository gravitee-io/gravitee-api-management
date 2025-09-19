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
package io.gravitee.apim.core.membership.domain_service;

import static org.assertj.core.api.Assertions.assertThat;

import fixtures.core.model.AuditInfoFixtures;
import fixtures.core.model.MembershipFixtures;
import inmemory.AuditCrudServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.MembershipCrudServiceInMemory;
import inmemory.MembershipQueryServiceInMemory;
import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditEntity;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.event.MembershipAuditEvent;
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

class DeleteMembershipDomainServiceTest {

    private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");
    private static final String API_ID = "my-api";
    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String USER_ID = "user-id";
    private static final AuditInfo AUDIT_INFO = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);

    MembershipCrudServiceInMemory membershipCrudServiceInMemory = new MembershipCrudServiceInMemory();
    MembershipQueryServiceInMemory membershipQueryServiceInMemory = new MembershipQueryServiceInMemory(membershipCrudServiceInMemory);
    AuditCrudServiceInMemory auditCrudServiceInMemory = new AuditCrudServiceInMemory();
    UserCrudServiceInMemory userCrudServiceInMemory = new UserCrudServiceInMemory();

    DeleteMembershipDomainService service;

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
        var auditDomainService = new AuditDomainService(auditCrudServiceInMemory, userCrudServiceInMemory, new JacksonJsonDiffProcessor());

        service = new DeleteMembershipDomainService(membershipQueryServiceInMemory, membershipCrudServiceInMemory, auditDomainService);

        membershipCrudServiceInMemory.initWith(List.of(MembershipFixtures.anApiMembership(API_ID)));
    }

    @AfterEach
    void tearDown() {
        Stream.of(membershipCrudServiceInMemory, membershipQueryServiceInMemory, auditCrudServiceInMemory, userCrudServiceInMemory).forEach(
            InMemoryAlternative::reset
        );
    }

    @Test
    void should_delete_ApiMemberships_membership() {
        service.deleteApiMemberships(API_ID, AUDIT_INFO);

        assertThat(membershipCrudServiceInMemory.storage()).isNotNull().isEmpty();
    }

    @Test
    void should_create_audit_log() {
        service.deleteApiMemberships(API_ID, AUDIT_INFO);

        assertThat(auditCrudServiceInMemory.storage())
            .usingRecursiveFieldByFieldElementComparatorIgnoringFields("patch")
            .contains(
                AuditEntity.builder()
                    .id("generated-id")
                    .organizationId(ORGANIZATION_ID)
                    .environmentId(ENVIRONMENT_ID)
                    .referenceType(AuditEntity.AuditReferenceType.API)
                    .referenceId(API_ID)
                    .user(USER_ID)
                    .properties(Map.of())
                    .event(MembershipAuditEvent.MEMBERSHIP_DELETED.name())
                    .createdAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                    .build()
            );
    }
}
