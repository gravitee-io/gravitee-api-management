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
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
import io.gravitee.rest.api.model.providers.User;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.IdentityService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.MembershipAlreadyExistsException;
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
public class MembershipService_CreateNewMembershipForApiTest {

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

    @Mock
    private IdentityService identityService;

    @Mock
    private ApiRepository apiRepository;

    @Before
    public void setUp() throws Exception {
        reset(membershipRepository, apiSearchService, userService, auditService, roleService, identityService);
        mockRole();
        mockApi();
    }

    @Test
    public void should_create_membership_for_existing_user_without_existing_membership() throws Exception {
        String existingUserId = "existing-user-id";
        mockExistingUser(existingUserId);

        Membership newMembership = new Membership();
        newMembership.setReferenceType(io.gravitee.repository.management.model.MembershipReferenceType.API);
        newMembership.setRoleId("API_OWNER");
        newMembership.setReferenceId(API_ID);
        newMembership.setMemberId(existingUserId);
        newMembership.setMemberType(io.gravitee.repository.management.model.MembershipMemberType.USER);
        when(
            membershipRepository.findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId(
                existingUserId,
                io.gravitee.repository.management.model.MembershipMemberType.USER,
                io.gravitee.repository.management.model.MembershipReferenceType.API,
                API_ID
            )
        )
            .thenReturn(Collections.emptySet(), Set.of(newMembership));

        MemberEntity createdMember = membershipService.createNewMembershipForApi(
            GraviteeContext.getExecutionContext(),
            API_ID,
            existingUserId,
            null,
            "OWNER"
        );

        assertNotNull(createdMember);
    }

    @Test(expected = MembershipAlreadyExistsException.class)
    public void should_not_create_membership_for_existing_user_with_existing_membership() throws Exception {
        String existingUserId = "existing-user-id";
        mockExistingUser(existingUserId);

        Membership existingMembership = new Membership();
        existingMembership.setReferenceType(io.gravitee.repository.management.model.MembershipReferenceType.API);
        existingMembership.setRoleId("API_OWNER");
        existingMembership.setReferenceId(API_ID);
        existingMembership.setMemberId(existingUserId);
        existingMembership.setMemberType(io.gravitee.repository.management.model.MembershipMemberType.USER);
        when(
            membershipRepository.findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId(
                existingUserId,
                io.gravitee.repository.management.model.MembershipMemberType.USER,
                io.gravitee.repository.management.model.MembershipReferenceType.API,
                API_ID
            )
        )
            .thenReturn(Set.of(existingMembership));

        membershipService.createNewMembershipForApi(GraviteeContext.getExecutionContext(), API_ID, existingUserId, null, "OWNER");
    }

    @Test
    public void should_create_membership_for_external_reference() throws Exception {
        String externalReference = "external-reference";
        mockExternalUser(externalReference);

        Membership newMembership = new Membership();
        newMembership.setReferenceType(io.gravitee.repository.management.model.MembershipReferenceType.API);
        newMembership.setRoleId("API_OWNER");
        newMembership.setReferenceId(API_ID);
        newMembership.setMemberId("external-user-id");
        newMembership.setMemberType(io.gravitee.repository.management.model.MembershipMemberType.USER);
        when(
            membershipRepository.findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId(
                "external-user-id",
                io.gravitee.repository.management.model.MembershipMemberType.USER,
                io.gravitee.repository.management.model.MembershipReferenceType.API,
                API_ID
            )
        )
            .thenReturn(Set.of(newMembership));

        MemberEntity createdMember = membershipService.createNewMembershipForApi(
            GraviteeContext.getExecutionContext(),
            API_ID,
            null,
            externalReference,
            "OWNER"
        );

        assertNotNull(createdMember);
    }

    private void mockExistingUser(String existingUserId) {
        UserEntity userEntity = new UserEntity();
        userEntity.setId(existingUserId);
        when(userService.findById(GraviteeContext.getExecutionContext(), existingUserId)).thenReturn(userEntity);
    }

    private void mockRole() {
        Map<String, char[]> perms = new HashMap<>();
        for (Permission perm : OrganizationPermission.values()) {
            perms.put(perm.getName(), new char[] { CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId() });
        }
        RoleEntity role = new RoleEntity();
        role.setScope(RoleScope.API);
        role.setName("OWNER");
        role.setId("API_OWNER");
        role.setPermissions(perms);
        when(roleService.findByScopeAndName(RoleScope.API, "OWNER", GraviteeContext.getCurrentOrganization()))
            .thenReturn(Optional.of(role));
        when(roleService.findById("API_OWNER")).thenReturn(role);
    }

    private void mockApi() throws TechnicalException {
        Api api = new Api();
        api.setId(API_ID);
        api.setGroups(Collections.emptySet());
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));
    }

    private void mockExternalUser(String externalReference) {
        User externalUser = new User();
        externalUser.setSource("source");
        externalUser.setSourceId(externalReference);
        externalUser.setId("external-user-id");
        when(identityService.findByReference(externalReference)).thenReturn(Optional.of(externalUser));

        UserEntity userEntity = new UserEntity();
        userEntity.setId("external-user-id");
        userEntity.setSource("source");
        userEntity.setSourceId(externalReference);
        when(userService.findBySource(any(ExecutionContext.class), anyString(), anyString(), eq(false))).thenReturn(userEntity);

        mockExistingUser("external-user-id");
    }
}
