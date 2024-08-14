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

import static fixtures.core.model.RoleFixtures.apiPrimaryOwnerRoleId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import inmemory.GroupQueryServiceInMemory;
import inmemory.MembershipQueryServiceInMemory;
import inmemory.ParametersQueryServiceInMemory;
import inmemory.RoleQueryServiceInMemory;
import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.group.model.Group;
import io.gravitee.apim.core.membership.exception.NoPrimaryOwnerGroupForUserException;
import io.gravitee.apim.core.membership.exception.RoleNotFoundException;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.repository.management.model.Parameter;
import io.gravitee.repository.management.model.ParameterReferenceType;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.settings.ApiPrimaryOwnerMode;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class IntegrationPrimaryOwnerFactoryTest {

    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String MEMBER_ID = "member-id";
    private static final String USER_ID = "user-id";
    private static final String GROUP_ID = "group-id";

    GroupQueryServiceInMemory groupQueryService = new GroupQueryServiceInMemory();
    MembershipQueryServiceInMemory membershipQueryService = new MembershipQueryServiceInMemory();
    ParametersQueryServiceInMemory parametersQueryService = new ParametersQueryServiceInMemory();
    RoleQueryServiceInMemory roleQueryService = new RoleQueryServiceInMemory();
    UserCrudServiceInMemory userCrudService = new UserCrudServiceInMemory();

    IntegrationPrimaryOwnerFactory factory;

    @BeforeEach
    void setUp() {
        factory =
            new IntegrationPrimaryOwnerFactory(
                membershipQueryService,
                parametersQueryService,
                roleQueryService,
                userCrudService,
                groupQueryService
            );
    }

    @Nested
    class UserMode {

        @BeforeEach
        void setUp() {
            parametersQueryService.initWith(
                List.of(
                    new Parameter(
                        Key.API_PRIMARY_OWNER_MODE.key(),
                        ENVIRONMENT_ID,
                        ParameterReferenceType.ENVIRONMENT,
                        ApiPrimaryOwnerMode.USER.name()
                    )
                )
            );
        }

        @Test
        void should_build_an_user_primary_owner() {
            givenExistingUsers(
                List.of(BaseUserEntity.builder().id(USER_ID).firstname("Jane").lastname("Doe").email("jane.doe@gravitee.io").build())
            );

            var primaryOwner = factory.createForNewIntegration(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);

            assertThat(primaryOwner)
                .isEqualTo(new PrimaryOwnerEntity(USER_ID, "jane.doe@gravitee.io", "Jane Doe", PrimaryOwnerEntity.Type.USER));
        }
    }

    @Nested
    class GroupMode {

        @BeforeEach
        void setUp() {
            parametersQueryService.initWith(
                List.of(
                    new Parameter(
                        Key.API_PRIMARY_OWNER_MODE.key(),
                        ENVIRONMENT_ID,
                        ParameterReferenceType.ENVIRONMENT,
                        ApiPrimaryOwnerMode.GROUP.name()
                    )
                )
            );
            roleQueryService.resetSystemRoles(ORGANIZATION_ID);
            givenExistingUsers(
                List.of(
                    BaseUserEntity.builder().id(USER_ID).firstname("Jane").lastname("Doe").email("jane.doe@gravitee.io").build(),
                    BaseUserEntity.builder().id(MEMBER_ID).firstname("John").lastname("Doe").email("john.doe@gravitee.io").build()
                )
            );
        }

        @Test
        void should_build_a_group_primary_owner_with_the_1st_group_having_primary_owner() {
            // Given
            givenExistingGroup(List.of(Group.builder().id(GROUP_ID).name("My Group").apiPrimaryOwner(MEMBER_ID).build()));
            givenExistingMemberships(
                List.of(
                    // PO role for member-id
                    Membership
                        .builder()
                        .referenceType(Membership.ReferenceType.GROUP)
                        .referenceId(GROUP_ID)
                        .memberType(Membership.Type.USER)
                        .memberId(MEMBER_ID)
                        .roleId(apiPrimaryOwnerRoleId(ORGANIZATION_ID))
                        .build(),
                    // Regular role for user-id
                    Membership
                        .builder()
                        .referenceType(Membership.ReferenceType.GROUP)
                        .referenceId(GROUP_ID)
                        .memberType(Membership.Type.USER)
                        .memberId(USER_ID)
                        .roleId("role-id")
                        .build()
                )
            );

            // When
            var primaryOwner = factory.createForNewIntegration(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);

            // Then
            assertThat(primaryOwner)
                .isEqualTo(new PrimaryOwnerEntity(GROUP_ID, "john.doe@gravitee.io", "My Group", PrimaryOwnerEntity.Type.GROUP));
        }

        @Test
        void should_throw_when_user_does_not_belong_to_a_group_having_primary_owner() {
            // Given
            givenExistingGroup(List.of(Group.builder().id(GROUP_ID).name("My Group").build()));
            givenExistingMemberships(
                List.of(
                    Membership
                        .builder()
                        .referenceType(Membership.ReferenceType.GROUP)
                        .referenceId(GROUP_ID)
                        .memberType(Membership.Type.USER)
                        .memberId(USER_ID)
                        .roleId("role-id")
                        .build()
                )
            );

            // When
            Throwable throwable = catchThrowable(() -> factory.createForNewIntegration(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID));

            // Then
            assertThat(throwable).isInstanceOf(NoPrimaryOwnerGroupForUserException.class);
        }

        @Test
        void should_throw_when_primary_owner_role_does_not_exist_for_organization() {
            // Given
            roleQueryService.reset();
            givenExistingGroup(List.of(Group.builder().id(GROUP_ID).name("My Group").apiPrimaryOwner(MEMBER_ID).build()));
            givenExistingMemberships(
                List.of(
                    // PO role for member-id
                    Membership
                        .builder()
                        .referenceType(Membership.ReferenceType.GROUP)
                        .referenceId(GROUP_ID)
                        .memberType(Membership.Type.USER)
                        .memberId(MEMBER_ID)
                        .roleId(apiPrimaryOwnerRoleId(ORGANIZATION_ID))
                        .build(),
                    // Regular role for user-id
                    Membership
                        .builder()
                        .referenceType(Membership.ReferenceType.GROUP)
                        .referenceId(GROUP_ID)
                        .memberType(Membership.Type.USER)
                        .memberId(USER_ID)
                        .roleId("role-id")
                        .build()
                )
            );

            // When
            Throwable throwable = catchThrowable(() -> factory.createForNewIntegration(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID));

            // Then
            assertThat(throwable).isInstanceOf(RoleNotFoundException.class);
        }
    }

    @Nested
    class HybridMode {

        @BeforeEach
        void setUp() {
            parametersQueryService.initWith(
                List.of(
                    new Parameter(
                        Key.API_PRIMARY_OWNER_MODE.key(),
                        ENVIRONMENT_ID,
                        ParameterReferenceType.ENVIRONMENT,
                        ApiPrimaryOwnerMode.HYBRID.name()
                    )
                )
            );
        }

        @Test
        void should_build_an_user_primary_owner() {
            givenExistingUsers(
                List.of(BaseUserEntity.builder().id(USER_ID).firstname("Jane").lastname("Doe").email("jane.doe@gravitee.io").build())
            );

            var primaryOwner = factory.createForNewIntegration(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);

            assertThat(primaryOwner)
                .isEqualTo(new PrimaryOwnerEntity(USER_ID, "jane.doe@gravitee.io", "Jane Doe", PrimaryOwnerEntity.Type.USER));
        }
    }

    private void givenExistingUsers(List<BaseUserEntity> users) {
        userCrudService.initWith(users);
    }

    private void givenExistingMemberships(List<Membership> memberships) {
        membershipQueryService.initWith(memberships);
    }

    private void givenExistingGroup(List<Group> groups) {
        groupQueryService.initWith(groups);
    }
}
