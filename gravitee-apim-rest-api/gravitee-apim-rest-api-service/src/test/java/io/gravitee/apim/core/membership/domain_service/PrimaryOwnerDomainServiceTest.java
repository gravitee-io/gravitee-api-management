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
import inmemory.GroupQueryServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.MembershipCrudServiceInMemory;
import inmemory.MembershipQueryServiceInMemory;
import inmemory.RoleQueryServiceInMemory;
import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.group.model.Group;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.service.common.UuidString;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class PrimaryOwnerDomainServiceTest {

    private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");
    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String MEMBER_ID = "member-id";
    private static final String GROUP_ID = "group-id";
    private static final String USER_ID = "user-id";
    private static final String INTEGRATION_ID = "my-integration";
    private static final String APPLICATION_PRIMARY_OWNER_ROLE_ID = "app-po-id-" + ORGANIZATION_ID;
    private static final AuditInfo AUDIT_INFO = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);

    MembershipCrudServiceInMemory membershipCrudService = new MembershipCrudServiceInMemory();
    RoleQueryServiceInMemory roleQueryService = new RoleQueryServiceInMemory();
    MembershipQueryServiceInMemory membershipQueryService = new MembershipQueryServiceInMemory();
    GroupQueryServiceInMemory groupQueryService = new GroupQueryServiceInMemory();
    UserCrudServiceInMemory userCrudService = new UserCrudServiceInMemory();

    PrimaryOwnerDomainService service;

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
            new PrimaryOwnerDomainService(
                membershipCrudService,
                roleQueryService,
                membershipQueryService,
                groupQueryService,
                userCrudService
            );

        roleQueryService.resetSystemRoles(ORGANIZATION_ID);
    }

    @AfterEach
    void tearDown() {
        Stream
            .of(membershipCrudService, roleQueryService, membershipQueryService, groupQueryService, userCrudService)
            .forEach(InMemoryAlternative::reset);
    }

    @Nested
    class CreateIntegrationPrimaryOwnerMembership {

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

    @Nested
    class GetApplicationPrimaryOwner {

        @Test
        @SneakyThrows
        public void should_return_a_user_primary_owner_of_an_application() {
            givenExistingUsers(
                List.of(BaseUserEntity.builder().id("user-id").firstname("Jane").lastname("Doe").email("jane.doe@gravitee.io").build())
            );
            givenExistingMemberships(
                List.of(
                    Membership
                        .builder()
                        .referenceType(Membership.ReferenceType.APPLICATION)
                        .referenceId("application-id")
                        .memberType(Membership.Type.USER)
                        .memberId("user-id")
                        .roleId(APPLICATION_PRIMARY_OWNER_ROLE_ID)
                        .build()
                )
            );

            var result = service.getApplicationPrimaryOwner(ORGANIZATION_ID, "application-id").blockingGet();

            Assertions
                .assertThat(result)
                .isEqualTo(
                    PrimaryOwnerEntity
                        .builder()
                        .id("user-id")
                        .displayName("Jane Doe")
                        .email("jane.doe@gravitee.io")
                        .type(PrimaryOwnerEntity.Type.USER)
                        .build()
                );
        }

        @Test
        @SneakyThrows
        public void should_return_a_group_primary_owner_of_an_api() {
            givenExistingUsers(
                List.of(BaseUserEntity.builder().id("user-id").firstname("Jane").lastname("Doe").email("jane.doe@gravitee.io").build())
            );
            givenExistingGroup(List.of(Group.builder().id("group-id").name("Group name").build()));
            givenExistingMemberships(
                List.of(
                    Membership
                        .builder()
                        .referenceType(Membership.ReferenceType.APPLICATION)
                        .referenceId("application-id")
                        .memberType(Membership.Type.GROUP)
                        .memberId("group-id")
                        .roleId(APPLICATION_PRIMARY_OWNER_ROLE_ID)
                        .build(),
                    Membership
                        .builder()
                        .referenceType(Membership.ReferenceType.GROUP)
                        .referenceId("group-id")
                        .memberType(Membership.Type.USER)
                        .memberId("user-id")
                        .roleId(APPLICATION_PRIMARY_OWNER_ROLE_ID)
                        .build()
                )
            );

            var result = service.getApplicationPrimaryOwner(ORGANIZATION_ID, "application-id").blockingGet();

            Assertions
                .assertThat(result)
                .isEqualTo(
                    PrimaryOwnerEntity
                        .builder()
                        .id("group-id")
                        .displayName("Group name")
                        .email("jane.doe@gravitee.io")
                        .type(PrimaryOwnerEntity.Type.GROUP)
                        .build()
                );
        }

        @Test
        @SneakyThrows
        public void should_return_a_group_primary_owner_with_no_email_when_group_has_no_users() {
            givenExistingGroup(List.of(Group.builder().id("group-id").name("Group name").build()));
            givenExistingMemberships(
                List.of(
                    Membership
                        .builder()
                        .referenceType(Membership.ReferenceType.APPLICATION)
                        .referenceId("application-id")
                        .memberType(Membership.Type.GROUP)
                        .memberId("group-id")
                        .roleId(APPLICATION_PRIMARY_OWNER_ROLE_ID)
                        .build()
                )
            );

            var result = service.getApplicationPrimaryOwner(ORGANIZATION_ID, "application-id").blockingGet();

            Assertions
                .assertThat(result)
                .isEqualTo(
                    PrimaryOwnerEntity.builder().id("group-id").displayName("Group name").type(PrimaryOwnerEntity.Type.GROUP).build()
                );
        }

        @Test
        public void should_return_no_user_primary_owner_found() {
            givenExistingUsers(List.of());
            givenExistingMemberships(
                List.of(
                    Membership
                        .builder()
                        .referenceType(Membership.ReferenceType.APPLICATION)
                        .referenceId("application-id")
                        .memberType(Membership.Type.USER)
                        .memberId("user-id")
                        .roleId(APPLICATION_PRIMARY_OWNER_ROLE_ID)
                        .build()
                )
            );

            var primaryOwner = service.getApplicationPrimaryOwner(ORGANIZATION_ID, "application-id").blockingGet();

            assertThat(primaryOwner).isNull();
        }
    }

    private void givenExistingUsers(List<BaseUserEntity> users) {
        userCrudService.initWith(users);
    }

    private void givenExistingMemberships(List<Membership> memberships) {
        membershipQueryService.initWith(memberships);
    }

    @SneakyThrows
    private void givenExistingGroup(List<Group> groups) {
        groupQueryService.initWith(groups);
    }
}
