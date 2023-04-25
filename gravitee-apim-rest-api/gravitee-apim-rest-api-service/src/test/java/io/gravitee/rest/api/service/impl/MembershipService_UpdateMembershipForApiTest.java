/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.model.Membership;
import io.gravitee.rest.api.model.MemberEntity;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.permissions.OrganizationPermission;
import io.gravitee.rest.api.model.permissions.Permission;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MembershipService_UpdateMembershipForApiTest {

    private static final String API_ID = "api-id-1";

    @InjectMocks
    private MembershipService membershipService = new MembershipServiceImpl();

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

    @Before
    public void setUp() throws Exception {
        reset(membershipRepository, apiSearchService, userService, auditService, roleService);
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

        MemberEntity updatedMember = membershipService.updateMembershipForApi(
            GraviteeContext.getExecutionContext(),
            API_ID,
            userId,
            "API_OWNER"
        );

        assertNotNull(updatedMember);
        assertEquals("API_OWNER", updatedMember.getRoles().iterator().next().getId());
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
        assertNull(updatedMember);
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
        when(roleService.findByScopeAndName(RoleScope.API, roleId, GraviteeContext.getCurrentOrganization())).thenReturn(Optional.of(role));
        when(roleService.findById(roleId)).thenReturn(role);
    }

    private void mockApi() {
        ApiEntity apiEntity = new ApiEntity();
        apiEntity.setId(API_ID);
        apiEntity.setGroups(Collections.emptySet());
        when(apiSearchService.findGenericById(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(apiEntity);
    }
}
