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

import static fixtures.core.model.RoleFixtures.apiProductPrimaryOwnerRoleId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import fixtures.core.model.AuditInfoFixtures;
import inmemory.AuditCrudServiceInMemory;
import inmemory.GroupQueryServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.MembershipCrudServiceInMemory;
import inmemory.MembershipQueryServiceInMemory;
import inmemory.RoleQueryServiceInMemory;
import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditEntity;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.event.MembershipAuditEvent;
import io.gravitee.apim.core.group.model.Group;
import io.gravitee.apim.core.membership.exception.ApiProductPrimaryOwnerNotFoundException;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.service.common.UuidString;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ApiProductPrimaryOwnerDomainServiceTest {

    private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");
    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String MEMBER_ID = "member-id";
    private static final String GROUP_ID = "group-id";
    private static final String USER_ID = "user-id";
    private static final String API_PRODUCT_ID = "c45b8e66-4d2a-47ad-9b8e-664d2a97ad88";
    private static final AuditInfo AUDIT_INFO = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);

    AuditCrudServiceInMemory auditCrudService = new AuditCrudServiceInMemory();
    GroupQueryServiceInMemory groupQueryService = new GroupQueryServiceInMemory();
    MembershipCrudServiceInMemory membershipCrudService = new MembershipCrudServiceInMemory();
    MembershipQueryServiceInMemory membershipQueryService = new MembershipQueryServiceInMemory(membershipCrudService);
    RoleQueryServiceInMemory roleQueryService = new RoleQueryServiceInMemory();
    UserCrudServiceInMemory userCrudService = new UserCrudServiceInMemory();

    ApiProductPrimaryOwnerDomainService service;

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
        service = new ApiProductPrimaryOwnerDomainService(
            new AuditDomainService(auditCrudService, userCrudService, new JacksonJsonDiffProcessor()),
            groupQueryService,
            membershipCrudService,
            membershipQueryService,
            roleQueryService,
            userCrudService
        );

        roleQueryService.resetSystemRoles(ORGANIZATION_ID);
    }

    @AfterEach
    void tearDown() {
        Stream.of(auditCrudService, membershipCrudService, roleQueryService, userCrudService).forEach(InMemoryAlternative::reset);
    }

    @Nested
    class GetApiProductPrimaryOwner {

        @Test
        @SneakyThrows
        public void should_return_a_user_primary_owner_of_an_api_product() {
            givenExistingUsers(
                List.of(BaseUserEntity.builder().id(MEMBER_ID).firstname("Jane").lastname("Doe").email("jane.doe@gravitee.io").build())
            );
            givenExistingMemberships(
                List.of(
                    Membership.builder()
                        .referenceType(Membership.ReferenceType.API_PRODUCT)
                        .referenceId(API_PRODUCT_ID)
                        .memberType(Membership.Type.USER)
                        .memberId(MEMBER_ID)
                        .roleId(apiProductPrimaryOwnerRoleId(ORGANIZATION_ID))
                        .build()
                )
            );

            var result = service.getApiProductPrimaryOwner(ORGANIZATION_ID, API_PRODUCT_ID);

            Assertions.assertThat(result).isEqualTo(
                PrimaryOwnerEntity.builder()
                    .id(MEMBER_ID)
                    .email("jane.doe@gravitee.io")
                    .displayName("Jane Doe")
                    .type(PrimaryOwnerEntity.Type.USER)
                    .build()
            );
        }

        @Test
        @SneakyThrows
        public void should_return_a_group_primary_owner_of_an_api_product() {
            givenExistingUsers(
                List.of(BaseUserEntity.builder().id(MEMBER_ID).firstname("Jane").lastname("Doe").email("jane.doe@gravitee.io").build())
            );
            givenExistingGroup(List.of(Group.builder().id(GROUP_ID).name("Group name").build()));
            givenExistingMemberships(
                List.of(
                    Membership.builder()
                        .referenceType(Membership.ReferenceType.API_PRODUCT)
                        .referenceId(API_PRODUCT_ID)
                        .memberType(Membership.Type.GROUP)
                        .memberId(GROUP_ID)
                        .roleId(apiProductPrimaryOwnerRoleId(ORGANIZATION_ID))
                        .build(),
                    Membership.builder()
                        .referenceType(Membership.ReferenceType.GROUP)
                        .referenceId(GROUP_ID)
                        .memberType(Membership.Type.USER)
                        .memberId(MEMBER_ID)
                        .roleId(apiProductPrimaryOwnerRoleId(ORGANIZATION_ID))
                        .build()
                )
            );

            var result = service.getApiProductPrimaryOwner(ORGANIZATION_ID, API_PRODUCT_ID);

            Assertions.assertThat(result).isEqualTo(
                PrimaryOwnerEntity.builder()
                    .id(GROUP_ID)
                    .email("jane.doe@gravitee.io")
                    .displayName("Group name")
                    .type(PrimaryOwnerEntity.Type.GROUP)
                    .build()
            );
        }

        @Test
        public void should_throw_exception_when_no_primary_owner_found() {
            givenExistingUsers(List.of());
            givenExistingMemberships(List.of());

            Throwable throwable = catchThrowable(() -> service.getApiProductPrimaryOwner(ORGANIZATION_ID, API_PRODUCT_ID));

            assertThat(throwable)
                .isInstanceOf(ApiProductPrimaryOwnerNotFoundException.class)
                .hasMessageContaining("Primary owner not found for API Product");
        }
    }

    @Nested
    class CreateApiProductPrimaryOwnerMembership {

        @Nested
        class UserMode {

            @Test
            void should_create_an_user_api_product_primary_owner_membership() {
                service.createApiProductPrimaryOwnerMembership(
                    API_PRODUCT_ID,
                    PrimaryOwnerEntity.builder().id(MEMBER_ID).type(PrimaryOwnerEntity.Type.USER).build(),
                    AUDIT_INFO
                );

                assertThat(membershipCrudService.storage()).containsExactly(
                    Membership.builder()
                        .id("generated-id")
                        .roleId(apiProductPrimaryOwnerRoleId(ORGANIZATION_ID))
                        .memberId(MEMBER_ID)
                        .memberType(Membership.Type.USER)
                        .referenceId(API_PRODUCT_ID)
                        .referenceType(Membership.ReferenceType.API_PRODUCT)
                        .createdAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                        .updatedAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                        .build()
                );
            }

            @Test
            void should_create_an_audit() {
                service.createApiProductPrimaryOwnerMembership(
                    API_PRODUCT_ID,
                    PrimaryOwnerEntity.builder().id(MEMBER_ID).type(PrimaryOwnerEntity.Type.USER).build(),
                    AUDIT_INFO
                );

                assertThat(auditCrudService.storage())
                    .usingRecursiveFieldByFieldElementComparatorIgnoringFields("patch")
                    .containsExactly(
                        AuditEntity.builder()
                            .id("generated-id")
                            .organizationId(ORGANIZATION_ID)
                            .environmentId(ENVIRONMENT_ID)
                            .referenceType(AuditEntity.AuditReferenceType.API_PRODUCT)
                            .referenceId(API_PRODUCT_ID)
                            .user(USER_ID)
                            .properties(Map.of("USER", MEMBER_ID))
                            .event(MembershipAuditEvent.MEMBERSHIP_CREATED.name())
                            .createdAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                            .build()
                    );
            }
        }

        @Nested
        class GroupMode {

            @Test
            void should_create_a_group_api_product_primary_owner_membership() {
                service.createApiProductPrimaryOwnerMembership(
                    API_PRODUCT_ID,
                    PrimaryOwnerEntity.builder().id(GROUP_ID).type(PrimaryOwnerEntity.Type.GROUP).build(),
                    AUDIT_INFO
                );

                assertThat(membershipCrudService.storage()).containsExactly(
                    Membership.builder()
                        .id("generated-id")
                        .roleId(apiProductPrimaryOwnerRoleId(ORGANIZATION_ID))
                        .memberId(GROUP_ID)
                        .memberType(Membership.Type.GROUP)
                        .referenceId(API_PRODUCT_ID)
                        .referenceType(Membership.ReferenceType.API_PRODUCT)
                        .createdAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                        .updatedAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                        .build()
                );
            }

            @Test
            void should_create_an_audit() {
                service.createApiProductPrimaryOwnerMembership(
                    API_PRODUCT_ID,
                    PrimaryOwnerEntity.builder().id(GROUP_ID).type(PrimaryOwnerEntity.Type.GROUP).build(),
                    AUDIT_INFO
                );

                assertThat(auditCrudService.storage())
                    .usingRecursiveFieldByFieldElementComparatorIgnoringFields("patch")
                    .containsExactly(
                        AuditEntity.builder()
                            .id("generated-id")
                            .organizationId(ORGANIZATION_ID)
                            .environmentId(ENVIRONMENT_ID)
                            .referenceType(AuditEntity.AuditReferenceType.API_PRODUCT)
                            .referenceId(API_PRODUCT_ID)
                            .user(USER_ID)
                            .properties(Map.of("GROUP", GROUP_ID))
                            .event(MembershipAuditEvent.MEMBERSHIP_CREATED.name())
                            .createdAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                            .build()
                    );
            }
        }
    }

    @Nested
    class AssignPrimaryOwner {

        @Test
        void should_assign_user_as_primary_owner() {
            service.assignPrimaryOwner(
                API_PRODUCT_ID,
                PrimaryOwnerEntity.builder().id(MEMBER_ID).type(PrimaryOwnerEntity.Type.USER).build(),
                AUDIT_INFO
            );

            assertThat(membershipCrudService.storage()).hasSize(1);
            assertThat(membershipCrudService.storage().get(0)).satisfies(membership -> {
                assertThat(membership.getReferenceId()).isEqualTo(API_PRODUCT_ID);
                assertThat(membership.getReferenceType()).isEqualTo(Membership.ReferenceType.API_PRODUCT);
                assertThat(membership.getMemberId()).isEqualTo(MEMBER_ID);
                assertThat(membership.getMemberType()).isEqualTo(Membership.Type.USER);
            });
        }

        @Test
        void should_assign_group_as_primary_owner() {
            service.assignPrimaryOwner(
                API_PRODUCT_ID,
                PrimaryOwnerEntity.builder().id(GROUP_ID).type(PrimaryOwnerEntity.Type.GROUP).build(),
                AUDIT_INFO
            );

            assertThat(membershipCrudService.storage()).hasSize(1);
            assertThat(membershipCrudService.storage().get(0)).satisfies(membership -> {
                assertThat(membership.getReferenceId()).isEqualTo(API_PRODUCT_ID);
                assertThat(membership.getReferenceType()).isEqualTo(Membership.ReferenceType.API_PRODUCT);
                assertThat(membership.getMemberId()).isEqualTo(GROUP_ID);
                assertThat(membership.getMemberType()).isEqualTo(Membership.Type.GROUP);
            });
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
