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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Membership;
import io.gravitee.rest.api.model.MemberEntity;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.permissions.OrganizationPermission;
import io.gravitee.rest.api.model.permissions.Permission;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.v4.ApiSearchService;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class MembershipService_UpdateMembershipForApiTest {

    private static final String API_ID = "api-id-1";

    private MembershipService membershipService;

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
    private ApiRepository apiRepository;

    @BeforeEach
    public void setUp() throws Exception {
        reset(membershipRepository, apiSearchService, userService, auditService, roleService);

        membershipService =
            new MembershipServiceImpl(
                null,
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
        mockApi();
    }

    @Test
    public void should_update_membership() throws Exception {
        String userId = "user-id";
        mockRole("API_USER");
        mockRole("API_OWNER");
        mockExistingUser(userId);
        Membership existingMembership = new Membership();
        existingMembership.setId("existing-membership-id");
        existingMembership.setReferenceType(io.gravitee.repository.management.model.MembershipReferenceType.API);
        existingMembership.setRoleId("API_USER");
        existingMembership.setReferenceId(API_ID);
        existingMembership.setMemberId(userId);
        existingMembership.setMemberType(io.gravitee.repository.management.model.MembershipMemberType.USER);

        Membership updatedMembership = new Membership();
        updatedMembership.setId("existing-membership-id");
        updatedMembership.setReferenceType(io.gravitee.repository.management.model.MembershipReferenceType.API);
        updatedMembership.setRoleId("API_OWNER");
        updatedMembership.setReferenceId(API_ID);
        updatedMembership.setMemberId(userId);
        updatedMembership.setMemberType(io.gravitee.repository.management.model.MembershipMemberType.USER);
        when(
            membershipRepository.findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId(
                userId,
                io.gravitee.repository.management.model.MembershipMemberType.USER,
                io.gravitee.repository.management.model.MembershipReferenceType.API,
                API_ID
            )
        )
            .thenReturn(Set.of(existingMembership), Set.of(existingMembership), Set.of(updatedMembership));
        when(membershipRepository.findById("existing-membership-id")).thenReturn(Optional.of(existingMembership));
        when(roleService.findByScopeAndName(RoleScope.API, PRIMARY_OWNER.name(), GraviteeContext.getCurrentOrganization()))
            .thenReturn(Optional.of(new RoleEntity()));
        when(roleService.findByScopeAndName(RoleScope.INTEGRATION, PRIMARY_OWNER.name(), GraviteeContext.getCurrentOrganization()))
            .thenReturn(Optional.of(new RoleEntity()));

        MemberEntity updatedMember = membershipService.updateMembershipForApi(
            GraviteeContext.getExecutionContext(),
            API_ID,
            userId,
            "API_OWNER"
        );

        assertThat(updatedMember).isNotNull();
        assertThat(updatedMember.getRoles()).flatExtracting(RoleEntity::getId).contains("API_OWNER");
    }

    @Test
    public void should_not_update_membership_for_user_without_existing_membership() throws Exception {
        String userId = "user-id";
        when(
            membershipRepository.findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId(
                userId,
                io.gravitee.repository.management.model.MembershipMemberType.USER,
                io.gravitee.repository.management.model.MembershipReferenceType.API,
                API_ID
            )
        )
            .thenReturn(Collections.emptySet());

        MemberEntity updatedMember = membershipService.updateMembershipForApi(
            GraviteeContext.getExecutionContext(),
            API_ID,
            userId,
            "OWNER"
        );
        assertThat(updatedMember).isNull();
    }

    private void mockExistingUser(String existingUserId) {
        UserEntity userEntity = new UserEntity();
        userEntity.setId(existingUserId);
        when(userService.findById(GraviteeContext.getExecutionContext(), existingUserId)).thenReturn(userEntity);
    }

    private void mockRole(String roleId) {
        Map<String, char[]> perms = new HashMap<>();
        for (Permission perm : OrganizationPermission.values()) {
            perms.put(perm.getName(), new char[] { CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId() });
        }
        RoleEntity role = new RoleEntity();
        role.setScope(RoleScope.API);
        role.setName(roleId);
        role.setId(roleId);
        role.setPermissions(perms);
        lenient()
            .when(roleService.findByScopeAndName(RoleScope.API, roleId, GraviteeContext.getCurrentOrganization()))
            .thenReturn(Optional.of(role));
        when(roleService.findById(roleId)).thenReturn(role);
    }

    private void mockApi() throws TechnicalException {
        Api api = new Api();
        api.setId(API_ID);
        api.setGroups(Collections.emptySet());
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));
    }
}
