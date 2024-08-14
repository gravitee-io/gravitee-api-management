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

import static fixtures.core.model.RoleFixtures.integrationPrimaryOwnerRoleId;
import static org.assertj.core.api.Assertions.assertThat;

import fixtures.core.model.AuditInfoFixtures;
import inmemory.InMemoryAlternative;
import inmemory.MembershipCrudServiceInMemory;
import inmemory.RoleQueryServiceInMemory;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.service.common.UuidString;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class IntegrationPrimaryOwnerDomainServiceTest {

    private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");
    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String MEMBER_ID = "member-id";
    private static final String GROUP_ID = "group-id";
    private static final String USER_ID = "user-id";
    private static final String INTEGRATION_ID = "my-integration";
    private static final AuditInfo AUDIT_INFO = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);

    MembershipCrudServiceInMemory membershipCrudService = new MembershipCrudServiceInMemory();
    RoleQueryServiceInMemory roleQueryService = new RoleQueryServiceInMemory();

    IntegrationPrimaryOwnerDomainService service;

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
        service = new IntegrationPrimaryOwnerDomainService(membershipCrudService, roleQueryService);

        roleQueryService.resetSystemRoles(ORGANIZATION_ID);
    }

    @AfterEach
    void tearDown() {
        Stream.of(membershipCrudService, roleQueryService).forEach(InMemoryAlternative::reset);
    }

    @Nested
    class CreateApiPrimaryOwnerMembership {

        @Nested
        class UserMode {

            @Test
            void should_create_an_user_api_primary_owner_membership() {
                // When
                service.createIntegrationPrimaryOwnerMembership(
                    INTEGRATION_ID,
                    PrimaryOwnerEntity.builder().id(MEMBER_ID).type(PrimaryOwnerEntity.Type.USER).build(),
                    AUDIT_INFO
                );

                // Then
                assertThat(membershipCrudService.storage())
                    .containsExactly(
                        Membership
                            .builder()
                            .id("generated-id")
                            .roleId(integrationPrimaryOwnerRoleId(ORGANIZATION_ID))
                            .memberId(MEMBER_ID)
                            .memberType(Membership.Type.USER)
                            .referenceId(INTEGRATION_ID)
                            .referenceType(Membership.ReferenceType.INTEGRATION)
                            .source("system")
                            .createdAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                            .updatedAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                            .build()
                    );
            }
        }

        @Nested
        class GroupMode {

            @Test
            void should_create_a_group_api_primary_owner_membership() {
                // When
                service.createIntegrationPrimaryOwnerMembership(
                    INTEGRATION_ID,
                    PrimaryOwnerEntity.builder().id(GROUP_ID).type(PrimaryOwnerEntity.Type.GROUP).build(),
                    AUDIT_INFO
                );

                // Then
                assertThat(membershipCrudService.storage())
                    .containsExactly(
                        Membership
                            .builder()
                            .id("generated-id")
                            .roleId(integrationPrimaryOwnerRoleId(ORGANIZATION_ID))
                            .memberId(GROUP_ID)
                            .memberType(Membership.Type.GROUP)
                            .referenceId(INTEGRATION_ID)
                            .referenceType(Membership.ReferenceType.INTEGRATION)
                            .source("system")
                            .createdAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                            .updatedAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                            .build()
                    );
            }
        }
    }
}
