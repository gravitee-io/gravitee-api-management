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
package io.gravitee.apim.infra.membership;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.membership.exception.ApiPrimaryOwnerNotFoundException;
import io.gravitee.apim.core.membership.exception.ApplicationPrimaryOwnerNotFoundException;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity.Type;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.apim.infra.domain_service.membership.PrimaryOwnerDomainServiceImpl;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.GroupRepository;
import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.api.RoleRepository;
import io.gravitee.repository.management.model.Group;
import io.gravitee.repository.management.model.Membership;
import io.gravitee.repository.management.model.MembershipMemberType;
import io.gravitee.repository.management.model.MembershipReferenceType;
import io.gravitee.repository.management.model.Role;
import io.gravitee.repository.management.model.RoleReferenceType;
import io.gravitee.repository.management.model.RoleScope;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PrimaryOwnerDomainServiceImplTest {

    public static final String ORGANIZATION_ID = "DEFAULT";
    public static final String API_PRIMARY_OWNER_ROLE_ID = "api-po-role-id";
    public static final String APPLICATION_PRIMARY_OWNER_ROLE_ID = "app-po-role-id";
    UserCrudServiceInMemory userRepository;

    @Mock
    RoleRepository roleRepository;

    @Mock
    GroupRepository groupRepository;

    @Mock
    MembershipRepository membershipRepository;

    PrimaryOwnerDomainServiceImpl service;

    @BeforeEach
    void setUp() throws TechnicalException {
        userRepository = new UserCrudServiceInMemory();
        service = new PrimaryOwnerDomainServiceImpl(roleRepository, membershipRepository, userRepository, groupRepository);

        givenPrimaryRoleForOrganizationId(
            List.of(
                Role
                    .builder()
                    .id(API_PRIMARY_OWNER_ROLE_ID)
                    .referenceId(ORGANIZATION_ID)
                    .referenceType(RoleReferenceType.ORGANIZATION)
                    .scope(RoleScope.API)
                    .build(),
                Role
                    .builder()
                    .id(APPLICATION_PRIMARY_OWNER_ROLE_ID)
                    .referenceId(ORGANIZATION_ID)
                    .referenceType(RoleReferenceType.ORGANIZATION)
                    .scope(RoleScope.APPLICATION)
                    .build()
            )
        );
    }

    @AfterEach
    void tearDown() {
        Stream.of(userRepository).forEach(UserCrudServiceInMemory::reset);
        GraviteeContext.cleanContext();
    }

    @Nested
    class GetApiPrimaryOwner {

        @Test
        @SneakyThrows
        public void should_return_a_user_primary_owner_of_an_api() {
            givenExistingUsers(
                List.of(BaseUserEntity.builder().id("user-id").firstname("Jane").lastname("Doe").email("jane.doe@gravitee.io").build())
            );
            givenExistingMemberships(
                List.of(
                    Membership
                        .builder()
                        .referenceType(MembershipReferenceType.API)
                        .referenceId("api-id")
                        .memberType(MembershipMemberType.USER)
                        .memberId("user-id")
                        .roleId(API_PRIMARY_OWNER_ROLE_ID)
                        .build()
                )
            );

            var result = service.getApiPrimaryOwner(ORGANIZATION_ID, "api-id");

            Assertions
                .assertThat(result)
                .isEqualTo(
                    PrimaryOwnerEntity.builder().id("user-id").displayName("Jane Doe").email("jane.doe@gravitee.io").type(Type.USER).build()
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
                        .referenceType(MembershipReferenceType.API)
                        .referenceId("api-id")
                        .memberType(MembershipMemberType.GROUP)
                        .memberId("group-id")
                        .roleId(API_PRIMARY_OWNER_ROLE_ID)
                        .build(),
                    Membership
                        .builder()
                        .referenceType(MembershipReferenceType.GROUP)
                        .referenceId("group-id")
                        .memberType(MembershipMemberType.USER)
                        .memberId("user-id")
                        .roleId(API_PRIMARY_OWNER_ROLE_ID)
                        .build()
                )
            );

            var result = service.getApiPrimaryOwner(ORGANIZATION_ID, "api-id");

            Assertions
                .assertThat(result)
                .isEqualTo(
                    PrimaryOwnerEntity
                        .builder()
                        .id("group-id")
                        .displayName("Group name")
                        .email("jane.doe@gravitee.io")
                        .type(Type.GROUP)
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
                        .referenceType(MembershipReferenceType.API)
                        .referenceId("api-id")
                        .memberType(MembershipMemberType.GROUP)
                        .memberId("group-id")
                        .roleId(API_PRIMARY_OWNER_ROLE_ID)
                        .build()
                )
            );

            var result = service.getApiPrimaryOwner(ORGANIZATION_ID, "api-id");

            Assertions
                .assertThat(result)
                .isEqualTo(PrimaryOwnerEntity.builder().id("group-id").displayName("Group name").type(Type.GROUP).build());
        }

        @Test
        public void should_return_no_user_primary_owner_found() {
            givenExistingUsers(List.of());
            givenExistingMemberships(
                List.of(
                    Membership
                        .builder()
                        .referenceType(MembershipReferenceType.API)
                        .referenceId("api-id")
                        .memberType(MembershipMemberType.USER)
                        .memberId("user-id")
                        .roleId(API_PRIMARY_OWNER_ROLE_ID)
                        .build()
                )
            );

            Throwable throwable = catchThrowable(() -> service.getApiPrimaryOwner(ORGANIZATION_ID, "api-id"));

            assertThat(throwable).isInstanceOf(ApiPrimaryOwnerNotFoundException.class);
        }

        @Test
        public void should_throw_when_fail_to_get_primary_owner_role() {
            // Given
            givenRoleFailToBeFetched("technical exception");

            // When
            Throwable throwable = catchThrowable(() -> service.getApiPrimaryOwner(ORGANIZATION_ID, "api-id"));

            // Then
            assertThat(throwable).isInstanceOf(TechnicalManagementException.class).hasMessageContaining("technical exception");
        }

        @Test
        public void should_throw_when_fail_to_fetch_api_memberships() {
            givenApiPrimaryOwnerMembershipFailToBeFetched("technical exception");

            Throwable throwable = catchThrowable(() -> service.getApiPrimaryOwner(ORGANIZATION_ID, "api-id"));

            assertThat(throwable).isInstanceOf(TechnicalManagementException.class).hasMessageContaining("technical exception");
        }

        @Test
        public void should_throw_when_fail_to_fetch_group() {
            givenExistingMemberships(
                List.of(
                    Membership
                        .builder()
                        .referenceType(MembershipReferenceType.API)
                        .referenceId("api-id")
                        .memberType(MembershipMemberType.GROUP)
                        .memberId("group-id")
                        .roleId(API_PRIMARY_OWNER_ROLE_ID)
                        .build()
                )
            );
            givenGroupFailToBeFetched("technical exception");

            Throwable throwable = catchThrowable(() -> service.getApiPrimaryOwner(ORGANIZATION_ID, "api-id"));

            assertThat(throwable).isInstanceOf(TechnicalManagementException.class).hasMessageContaining("technical exception");
        }

        @Test
        public void should_throw_when_fail_to_fetch_group_members() {
            givenExistingMemberships(
                List.of(
                    Membership
                        .builder()
                        .referenceType(MembershipReferenceType.API)
                        .referenceId("api-id")
                        .memberType(MembershipMemberType.GROUP)
                        .memberId("group-id")
                        .roleId(API_PRIMARY_OWNER_ROLE_ID)
                        .build()
                )
            );
            givenGroupMembershipFailToBeFetched("technical exception");

            Throwable throwable = catchThrowable(() -> service.getApiPrimaryOwner(ORGANIZATION_ID, "api-id"));

            assertThat(throwable).isInstanceOf(TechnicalManagementException.class).hasMessageContaining("technical exception");
        }
    }

    @Nested
    class GetApplicationPrimaryOwner {

        @Test
        @SneakyThrows
        public void should_return_a_user_primary_owner_of_an_api() {
            givenExistingUsers(
                List.of(BaseUserEntity.builder().id("user-id").firstname("Jane").lastname("Doe").email("jane.doe@gravitee.io").build())
            );
            givenExistingMemberships(
                List.of(
                    Membership
                        .builder()
                        .referenceType(MembershipReferenceType.APPLICATION)
                        .referenceId("application-id")
                        .memberType(MembershipMemberType.USER)
                        .memberId("user-id")
                        .roleId(APPLICATION_PRIMARY_OWNER_ROLE_ID)
                        .build()
                )
            );

            var result = service.getApplicationPrimaryOwner(ORGANIZATION_ID, "application-id");

            Assertions
                .assertThat(result)
                .isEqualTo(
                    PrimaryOwnerEntity.builder().id("user-id").displayName("Jane Doe").email("jane.doe@gravitee.io").type(Type.USER).build()
                );
        }

        @Test
        @SneakyThrows
        public void should_return_a_group_primary_owner_of_an_application() {
            givenExistingUsers(
                List.of(BaseUserEntity.builder().id("user-id").firstname("Jane").lastname("Doe").email("jane.doe@gravitee.io").build())
            );
            givenExistingGroup(List.of(Group.builder().id("group-id").name("Group name").build()));
            givenExistingMemberships(
                List.of(
                    Membership
                        .builder()
                        .referenceType(MembershipReferenceType.APPLICATION)
                        .referenceId("application-id")
                        .memberType(MembershipMemberType.GROUP)
                        .memberId("group-id")
                        .roleId(APPLICATION_PRIMARY_OWNER_ROLE_ID)
                        .build(),
                    Membership
                        .builder()
                        .referenceType(MembershipReferenceType.GROUP)
                        .referenceId("group-id")
                        .memberType(MembershipMemberType.USER)
                        .memberId("user-id")
                        .roleId(APPLICATION_PRIMARY_OWNER_ROLE_ID)
                        .build()
                )
            );

            var result = service.getApplicationPrimaryOwner(ORGANIZATION_ID, "application-id");

            Assertions
                .assertThat(result)
                .isEqualTo(
                    PrimaryOwnerEntity
                        .builder()
                        .id("group-id")
                        .displayName("Group name")
                        .email("jane.doe@gravitee.io")
                        .type(Type.GROUP)
                        .build()
                );
        }

        @Test
        public void should_throw_when_no_user_primary_owner_found() {
            givenExistingUsers(List.of());
            givenExistingMemberships(
                List.of(
                    Membership
                        .builder()
                        .referenceType(MembershipReferenceType.APPLICATION)
                        .referenceId("application-id")
                        .memberType(MembershipMemberType.USER)
                        .memberId("user-id")
                        .roleId(APPLICATION_PRIMARY_OWNER_ROLE_ID)
                        .build()
                )
            );

            Throwable throwable = catchThrowable(() -> service.getApplicationPrimaryOwner(ORGANIZATION_ID, "application-id"));

            assertThat(throwable).isInstanceOf(ApplicationPrimaryOwnerNotFoundException.class);
        }

        @Test
        public void should_throw_when_fail_to_get_primary_owner_role() {
            // Given
            givenRoleFailToBeFetched("technical exception");

            // When
            Throwable throwable = catchThrowable(() -> service.getApplicationPrimaryOwner(ORGANIZATION_ID, "application-id"));

            // Then
            assertThat(throwable).isInstanceOf(TechnicalManagementException.class).hasMessageContaining("technical exception");
        }

        @Test
        public void should_throw_when_fail_to_fetch_api_memberships() {
            givenApiPrimaryOwnerMembershipFailToBeFetched("technical exception");

            Throwable throwable = catchThrowable(() -> service.getApplicationPrimaryOwner(ORGANIZATION_ID, "application-id"));

            assertThat(throwable).isInstanceOf(TechnicalManagementException.class).hasMessageContaining("technical exception");
        }

        @Test
        public void should_throw_when_fail_to_fetch_group() {
            givenExistingMemberships(
                List.of(
                    Membership
                        .builder()
                        .referenceType(MembershipReferenceType.APPLICATION)
                        .referenceId("application-id")
                        .memberType(MembershipMemberType.GROUP)
                        .memberId("group-id")
                        .roleId(APPLICATION_PRIMARY_OWNER_ROLE_ID)
                        .build()
                )
            );
            givenGroupFailToBeFetched("technical exception");

            Throwable throwable = catchThrowable(() -> service.getApplicationPrimaryOwner(ORGANIZATION_ID, "application-id"));

            assertThat(throwable).isInstanceOf(TechnicalManagementException.class).hasMessageContaining("technical exception");
        }

        @Test
        public void should_throw_when_fail_to_fetch_group_members() {
            givenExistingMemberships(
                List.of(
                    Membership
                        .builder()
                        .referenceType(MembershipReferenceType.APPLICATION)
                        .referenceId("application-id")
                        .memberType(MembershipMemberType.GROUP)
                        .memberId("group-id")
                        .roleId(APPLICATION_PRIMARY_OWNER_ROLE_ID)
                        .build()
                )
            );
            givenGroupMembershipFailToBeFetched("technical exception");

            Throwable throwable = catchThrowable(() -> service.getApplicationPrimaryOwner(ORGANIZATION_ID, "application-id"));

            assertThat(throwable).isInstanceOf(TechnicalManagementException.class).hasMessageContaining("technical exception");
        }
    }

    private void givenPrimaryRoleForOrganizationId(List<Role> roles) throws TechnicalException {
        for (Role role : roles) {
            lenient()
                .when(
                    roleRepository.findByScopeAndNameAndReferenceIdAndReferenceType(
                        role.getScope(),
                        SystemRole.PRIMARY_OWNER.name(),
                        role.getReferenceId(),
                        role.getReferenceType()
                    )
                )
                .thenReturn(Optional.of(role));
        }
    }

    @SneakyThrows
    private void givenRoleFailToBeFetched(String message) {
        when(roleRepository.findByScopeAndNameAndReferenceIdAndReferenceType(any(), any(), any(), any()))
            .thenThrow(new TechnicalException(message));
    }

    @SneakyThrows
    private void givenApiPrimaryOwnerMembershipFailToBeFetched(String message) {
        lenient()
            .when(membershipRepository.findByReferenceAndRoleId(eq(MembershipReferenceType.API), any(), any()))
            .thenThrow(new TechnicalException(message));
        lenient()
            .when(membershipRepository.findByReferenceAndRoleId(eq(MembershipReferenceType.APPLICATION), any(), any()))
            .thenThrow(new TechnicalException(message));
    }

    @SneakyThrows
    private void givenGroupMembershipFailToBeFetched(String message) {
        when(membershipRepository.findByReferenceAndRoleId(eq(MembershipReferenceType.GROUP), any(), any()))
            .thenThrow(new TechnicalException(message));
    }

    private void givenExistingUsers(List<BaseUserEntity> users) {
        userRepository.initWith(users);
    }

    @SneakyThrows
    private void givenExistingGroup(List<Group> groups) {
        when(groupRepository.findById(any())).thenReturn(Optional.empty());

        for (Group group : groups) {
            when(groupRepository.findById(eq(group.getId())))
                .thenAnswer(invocation -> Optional.of(Group.builder().id(invocation.getArgument(0)).name("Group name").build()));
        }
    }

    @SneakyThrows
    private void givenGroupFailToBeFetched(String message) {
        when(groupRepository.findById(any())).thenThrow(new TechnicalException(message));
    }

    @SneakyThrows
    private void givenExistingMemberships(List<Membership> memberships) {
        when(membershipRepository.findByReferenceAndRoleId(any(), any(), any())).thenReturn(Set.of());

        for (Membership m : memberships) {
            when(membershipRepository.findByReferenceAndRoleId(m.getReferenceType(), m.getReferenceId(), m.getRoleId()))
                .thenReturn(Set.of(m));
        }
    }
}
