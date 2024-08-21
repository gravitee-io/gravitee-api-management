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
package io.gravitee.rest.api.service.impl;

import static io.gravitee.rest.api.model.permissions.RolePermissionAction.CREATE;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.DELETE;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.READ;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.UPDATE;
import static io.gravitee.rest.api.model.permissions.SystemRole.PRIMARY_OWNER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.model.Membership;
import io.gravitee.repository.management.model.MembershipMemberType;
import io.gravitee.repository.management.model.MembershipReferenceType;
import io.gravitee.rest.api.model.MemberEntity;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.permissions.OrganizationPermission;
import io.gravitee.rest.api.model.permissions.Permission;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.model.providers.User;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.IdentityService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.MembershipAlreadyExistsException;
import io.gravitee.rest.api.service.exceptions.PrimaryOwnerRemovalException;
import io.gravitee.rest.api.service.v4.ApiSearchService;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class MembershipService_IntegrationMembership {

    private static final String INTEGRATION_ID = "integration-id";
    private static final String INTEGRATION_OWNER = "OWNER";
    private static final String INTEGRATION_USER = "USER";
    private static final String INTEGRATION_PRIMARY_OWNER = "PRIMARY_OWNER";
    private static final String EXISTING_USER_ID = "existing-user-id";
    private static final String EXTERNAL_USER_ID = "external-user-id";

    private MembershipServiceImpl membershipService;

    @Mock
    private MembershipRepository membershipRepository;

    @Mock
    private ApiSearchService apiSearchService;

    @Mock
    private UserService userService;

    @Mock
    private AuditService auditService;

    @Mock
    private RoleService roleService;

    @Mock
    private IdentityService identityService;

    @Mock
    private ApiRepository apiRepository;

    @BeforeEach
    public void setUp() throws Exception {
        reset(membershipRepository, apiSearchService, userService, auditService, roleService, identityService, apiRepository);

        membershipService =
            new MembershipServiceImpl(
                identityService,
                userService,
                null,
                null,
                null,
                null,
                membershipRepository,
                roleService,
                null,
                null,
                apiSearchService,
                null,
                apiRepository,
                null,
                auditService,
                null
            );
    }

    @Nested
    class CreateMembership {

        @BeforeEach
        void setUp() {
            mockRole(INTEGRATION_OWNER);
        }

        @Test
        public void should_create_membership_for_existing_user_without_existing_membership() throws Exception {
            String existingUserId = EXISTING_USER_ID;
            mockExistingUser(existingUserId);

            Membership newMembership = new Membership();
            newMembership.setReferenceType(MembershipReferenceType.INTEGRATION);
            newMembership.setRoleId(INTEGRATION_OWNER);
            newMembership.setReferenceId(INTEGRATION_ID);
            newMembership.setMemberId(existingUserId);
            newMembership.setMemberType(MembershipMemberType.USER);

            when(
                membershipRepository.findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId(
                    existingUserId,
                    MembershipMemberType.USER,
                    MembershipReferenceType.INTEGRATION,
                    INTEGRATION_ID
                )
            )
                .thenReturn(Set.of())
                .thenReturn(Set.of(newMembership));

            MemberEntity createdMember = membershipService.createNewMembershipForIntegration(
                GraviteeContext.getExecutionContext(),
                INTEGRATION_ID,
                existingUserId,
                null,
                INTEGRATION_OWNER
            );

            assertThat(createdMember).isNotNull();
        }

        @Test
        public void should_not_create_membership_for_existing_user_with_existing_membership() throws Exception {
            String existingUserId = EXISTING_USER_ID;
            mockExistingUser(existingUserId);

            Membership existingMembership = new Membership();
            existingMembership.setReferenceType(MembershipReferenceType.API);
            existingMembership.setRoleId(INTEGRATION_OWNER);
            existingMembership.setReferenceId(INTEGRATION_ID);
            existingMembership.setMemberId(existingUserId);
            existingMembership.setMemberType(MembershipMemberType.USER);
            when(
                membershipRepository.findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId(
                    existingUserId,
                    MembershipMemberType.USER,
                    MembershipReferenceType.INTEGRATION,
                    INTEGRATION_ID
                )
            )
                .thenReturn(Set.of(existingMembership));

            assertThatThrownBy(() ->
                    membershipService.createNewMembershipForIntegration(
                        GraviteeContext.getExecutionContext(),
                        INTEGRATION_ID,
                        existingUserId,
                        null,
                        "OWNER"
                    )
                )
                .isInstanceOf(MembershipAlreadyExistsException.class);
        }

        @Test
        public void should_create_membership_for_external_reference() throws Exception {
            String externalReference = "external-reference";
            mockExternalUser(externalReference);

            Membership newMembership = new Membership();
            newMembership.setReferenceType(MembershipReferenceType.INTEGRATION);
            newMembership.setRoleId(INTEGRATION_OWNER);
            newMembership.setReferenceId(INTEGRATION_ID);
            newMembership.setMemberId(EXTERNAL_USER_ID);
            newMembership.setMemberType(MembershipMemberType.USER);
            when(
                membershipRepository.findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId(
                    EXTERNAL_USER_ID,
                    MembershipMemberType.USER,
                    MembershipReferenceType.INTEGRATION,
                    INTEGRATION_ID
                )
            )
                .thenReturn(Set.of(newMembership));

            MemberEntity createdMember = membershipService.createNewMembershipForIntegration(
                GraviteeContext.getExecutionContext(),
                INTEGRATION_ID,
                null,
                externalReference,
                INTEGRATION_OWNER
            );

            assertThat(createdMember).isNotNull();
        }
    }

    @Nested
    class UpdateMembership {

        @Test
        public void should_update_membership() throws Exception {
            String userId = "user-id";
            mockRole(INTEGRATION_USER);
            mockRole(INTEGRATION_OWNER);
            mockExistingUser(userId);
            Membership existingMembership = new Membership();
            existingMembership.setId("existing-membership-id");
            existingMembership.setReferenceType(MembershipReferenceType.INTEGRATION);
            existingMembership.setRoleId(INTEGRATION_USER);
            existingMembership.setReferenceId(INTEGRATION_ID);
            existingMembership.setMemberId(userId);
            existingMembership.setMemberType(MembershipMemberType.USER);

            Membership updatedMembership = new Membership();
            updatedMembership.setId("existing-membership-id");
            updatedMembership.setReferenceType(MembershipReferenceType.INTEGRATION);
            updatedMembership.setRoleId(INTEGRATION_OWNER);
            updatedMembership.setReferenceId(INTEGRATION_ID);
            updatedMembership.setMemberId(userId);
            updatedMembership.setMemberType(MembershipMemberType.USER);
            when(
                membershipRepository.findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId(
                    userId,
                    MembershipMemberType.USER,
                    MembershipReferenceType.INTEGRATION,
                    INTEGRATION_ID
                )
            )
                .thenReturn(Set.of(existingMembership))
                .thenReturn(Set.of(existingMembership))
                .thenReturn(Set.of(updatedMembership));
            when(membershipRepository.findById("existing-membership-id")).thenReturn(Optional.of(existingMembership));
            when(roleService.findByScopeAndName(RoleScope.API, PRIMARY_OWNER.name(), GraviteeContext.getCurrentOrganization()))
                .thenReturn(Optional.of(new RoleEntity()));
            when(roleService.findByScopeAndName(RoleScope.INTEGRATION, PRIMARY_OWNER.name(), GraviteeContext.getCurrentOrganization()))
                .thenReturn(Optional.of(new RoleEntity()));

            MemberEntity updatedMember = membershipService.updateMembershipForIntegration(
                GraviteeContext.getExecutionContext(),
                INTEGRATION_ID,
                userId,
                INTEGRATION_OWNER
            );

            assertThat(updatedMember).isNotNull();
            assertThat(updatedMember.getRoles()).flatExtracting(RoleEntity::getId).contains(INTEGRATION_OWNER);
        }

        @Test
        public void should_not_update_membership_for_user_without_existing_membership() throws Exception {
            String userId = "user-id";
            when(
                membershipRepository.findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId(
                    userId,
                    MembershipMemberType.USER,
                    MembershipReferenceType.INTEGRATION,
                    INTEGRATION_ID
                )
            )
                .thenReturn(Collections.emptySet());

            MemberEntity updatedMember = membershipService.updateMembershipForIntegration(
                GraviteeContext.getExecutionContext(),
                INTEGRATION_ID,
                userId,
                INTEGRATION_OWNER
            );
            assertThat(updatedMember).isNull();
        }
    }

    @Nested
    class DeleteMembership {

        @Test
        public void shouldNotRemoveApiPrimaryOwner() throws TechnicalException {
            Membership membership = new Membership();
            membership.setId("membership-id");
            membership.setRoleId(INTEGRATION_PRIMARY_OWNER);
            membership.setReferenceType(MembershipReferenceType.INTEGRATION);

            when(roleService.findByScopeAndName(RoleScope.API, PRIMARY_OWNER.name(), GraviteeContext.getCurrentOrganization()))
                .thenReturn(Optional.of(new RoleEntity()));
            when(roleService.findByScopeAndName(RoleScope.INTEGRATION, PRIMARY_OWNER.name(), GraviteeContext.getCurrentOrganization()))
                .thenReturn(Optional.of(role(INTEGRATION_PRIMARY_OWNER)));
            when(roleService.findByScopeAndName(RoleScope.APPLICATION, PRIMARY_OWNER.name(), GraviteeContext.getCurrentOrganization()))
                .thenReturn(Optional.of(new RoleEntity()));
            when(
                membershipRepository.findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId(
                    EXISTING_USER_ID,
                    MembershipMemberType.USER,
                    MembershipReferenceType.INTEGRATION,
                    INTEGRATION_ID
                )
            )
                .thenReturn(Set.of(membership));

            assertThatThrownBy(() ->
                    membershipService.deleteMemberForIntegration(GraviteeContext.getExecutionContext(), INTEGRATION_ID, EXISTING_USER_ID)
                )
                .isInstanceOf(PrimaryOwnerRemovalException.class);
        }

        @Test
        public void shouldRemoveIntegrationPrimaryOwner() throws TechnicalException {
            ArgumentCaptor<String> deletedUserId = ArgumentCaptor.forClass(String.class);

            Membership membership = new Membership();
            membership.setId("membership-id");
            membership.setRoleId(INTEGRATION_PRIMARY_OWNER);
            membership.setReferenceType(MembershipReferenceType.INTEGRATION);
            membership.setReferenceId(INTEGRATION_ID);

            when(roleService.findByScopeAndName(RoleScope.API, PRIMARY_OWNER.name(), GraviteeContext.getCurrentOrganization()))
                .thenReturn(Optional.of(new RoleEntity()));
            when(roleService.findByScopeAndName(RoleScope.INTEGRATION, PRIMARY_OWNER.name(), GraviteeContext.getCurrentOrganization()))
                .thenReturn(Optional.of(new RoleEntity()));
            when(roleService.findByScopeAndName(RoleScope.APPLICATION, PRIMARY_OWNER.name(), GraviteeContext.getCurrentOrganization()))
                .thenReturn(Optional.of(new RoleEntity()));
            when(
                membershipRepository.findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId(
                    EXISTING_USER_ID,
                    MembershipMemberType.USER,
                    MembershipReferenceType.INTEGRATION,
                    INTEGRATION_ID
                )
            )
                .thenReturn(Set.of(membership));

            membershipService.deleteMemberForIntegration(GraviteeContext.getExecutionContext(), INTEGRATION_ID, EXISTING_USER_ID);

            verify(membershipRepository)
                .findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId(
                    EXISTING_USER_ID,
                    MembershipMemberType.USER,
                    MembershipReferenceType.INTEGRATION,
                    INTEGRATION_ID
                );
            verify(membershipRepository).delete(deletedUserId.capture());
            assertThat(deletedUserId.getValue()).contains(membership.getId());
        }
    }

    private void mockExistingUser(String existingUserId) {
        UserEntity userEntity = new UserEntity();
        userEntity.setId(existingUserId);
        when(userService.findById(GraviteeContext.getExecutionContext(), existingUserId)).thenReturn(userEntity);
    }

    private void mockRole(String roleId) {
        var role = role(roleId);
        lenient()
            .when(roleService.findByScopeAndName(RoleScope.INTEGRATION, roleId, GraviteeContext.getCurrentOrganization()))
            .thenReturn(Optional.of(role));
        when(roleService.findById(roleId)).thenReturn(role);
    }

    private static RoleEntity role(String roleId) {
        Map<String, char[]> perms = new HashMap<>();
        for (Permission perm : OrganizationPermission.values()) {
            perms.put(perm.getName(), new char[] { CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId() });
        }
        RoleEntity role = new RoleEntity();
        role.setScope(RoleScope.INTEGRATION);
        role.setName(roleId);
        role.setId(roleId);
        role.setPermissions(perms);
        return role;
    }

    private void mockExternalUser(String externalReference) {
        User externalUser = new User();
        externalUser.setSource("source");
        externalUser.setSourceId(externalReference);
        externalUser.setId(EXTERNAL_USER_ID);
        when(identityService.findByReference(externalReference)).thenReturn(Optional.of(externalUser));

        UserEntity userEntity = new UserEntity();
        userEntity.setId(EXTERNAL_USER_ID);
        userEntity.setSource("source");
        userEntity.setSourceId(externalReference);
        when(userService.findBySource(anyString(), anyString(), anyString(), eq(false))).thenReturn(userEntity);

        mockExistingUser(EXTERNAL_USER_ID);
    }
}
