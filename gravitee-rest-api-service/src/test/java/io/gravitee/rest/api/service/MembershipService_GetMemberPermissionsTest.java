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
package io.gravitee.rest.api.service;

import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.model.Membership;
import io.gravitee.repository.management.model.MembershipMemberType;
import io.gravitee.repository.management.model.MembershipReferenceType;
import io.gravitee.rest.api.model.GroupEntity;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.permissions.ApiPermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.impl.MembershipServiceImpl;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.*;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class MembershipService_GetMemberPermissionsTest {

    private static final String API_ID = "api-id-1";
    private static final String GROUP_ID1 = "GROUP_ID1";
    private static final String USERNAME = "johndoe";
    private static final String ROLENAME = "ROLE";
    private static final String ROLENAME2 = "ROLE2";

    @InjectMocks
    private MembershipService membershipService = new MembershipServiceImpl();

    @Mock
    private MembershipRepository membershipRepository;

    @Mock
    private UserService userService;

    @Mock
    private ApiService apiService;

    @Mock
    private RoleService roleService;

    @Test
    public void shouldGetNoPermissionsIfNotMemberAndWithNoGroup() throws Exception {
        ApiEntity api = mock(ApiEntity.class);
        doReturn(API_ID).when(api).getId();
        doReturn(Collections.emptySet()).when(api).getGroups();
        doReturn(api).when(apiService).findById(API_ID);

        doReturn(Collections.emptySet()).when(membershipRepository).findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId(USERNAME, MembershipMemberType.USER, MembershipReferenceType.API, API_ID);

        Map<String, char[]> permissions = membershipService.getUserMemberPermissions(api, USERNAME);

        assertNull(permissions);
        verify(membershipRepository, times(1)).findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId(USERNAME, MembershipMemberType.USER, MembershipReferenceType.API, API_ID);
        verify(apiService, times(1)).findById(API_ID);
    }

    @Test
    public void shouldGetPermissionsIfMemberOfApi() throws Exception {
        ApiEntity api = mock(ApiEntity.class);
        doReturn(API_ID).when(api).getId();
        doReturn(Collections.emptySet()).when(api).getGroups();
        doReturn(api).when(apiService).findById(API_ID);

        Membership membership = mock(Membership.class);
        doReturn("API_"+ROLENAME).when(membership).getRoleId();
        doReturn(new HashSet<>(asList(membership))).when(membershipRepository).findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId(USERNAME, MembershipMemberType.USER, MembershipReferenceType.API, API_ID);

        UserEntity userEntity = mock(UserEntity.class);
        doReturn(userEntity).when(userService).findById(USERNAME);
        
        RoleEntity roleEntity = mock(RoleEntity.class);
        Map<String, char[]> rolePerms = new HashMap<>();
        rolePerms.put(ApiPermission.DOCUMENTATION.getName(), new char[]{RolePermissionAction.UPDATE.getId(), RolePermissionAction.CREATE.getId()});
        doReturn(rolePerms).when(roleEntity).getPermissions();
        doReturn(roleEntity).when(roleService).findById("API_"+ROLENAME);

        Map<String, char[]> permissions = membershipService.getUserMemberPermissions(api, USERNAME);

        assertNotNull(permissions);
        assertPermissions(rolePerms, permissions);
        verify(membershipRepository, times(1)).findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId(USERNAME, MembershipMemberType.USER, MembershipReferenceType.API, API_ID);
        verify(membershipRepository, never()).findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId(USERNAME, MembershipMemberType.USER, MembershipReferenceType.GROUP, GROUP_ID1);
        verify(apiService, times(1)).findById(API_ID);
        verify(userService, times(1)).findById(USERNAME);
        verify(roleService, times(1)).findById("API_" + ROLENAME);
    }

    @Test
    public void shouldGetPermissionsIfMemberOfApiGroup() throws Exception {
        ApiEntity api = mock(ApiEntity.class);
        doReturn(API_ID).when(api).getId();
        doReturn(Collections.singleton(GROUP_ID1)).when(api).getGroups();
        doReturn(api).when(apiService).findById(API_ID);

        doReturn(Collections.emptySet()).when(membershipRepository).findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId(USERNAME, MembershipMemberType.USER, MembershipReferenceType.API, API_ID);

        Membership membership = mock(Membership.class);
        doReturn("API_"+ROLENAME).when(membership).getRoleId();
        doReturn(new HashSet<>(asList(membership))).when(membershipRepository).findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId(USERNAME, MembershipMemberType.USER, MembershipReferenceType.GROUP, GROUP_ID1);

        UserEntity userEntity = mock(UserEntity.class);
        doReturn(userEntity).when(userService).findById(USERNAME);
        
        RoleEntity roleEntity = mock(RoleEntity.class);
        Map<String, char[]> rolePerms = new HashMap<>();
        rolePerms.put(ApiPermission.DOCUMENTATION.getName(), new char[]{RolePermissionAction.UPDATE.getId(), RolePermissionAction.CREATE.getId()});
        doReturn(rolePerms).when(roleEntity).getPermissions();
        doReturn(RoleScope.API).when(roleEntity).getScope();
        doReturn(roleEntity).when(roleService).findById("API_"+ROLENAME);

        Map<String, char[]> permissions = membershipService.getUserMemberPermissions(api, USERNAME);

        assertNotNull(permissions);
        assertPermissions(rolePerms, permissions);
        verify(membershipRepository, times(1)).findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId(USERNAME, MembershipMemberType.USER, MembershipReferenceType.API, API_ID);
        verify(membershipRepository, times(1)).findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId(USERNAME, MembershipMemberType.USER, MembershipReferenceType.GROUP, GROUP_ID1);
        verify(apiService, times(1)).findById(API_ID);
        verify(userService, times(1)).findById(USERNAME);
        verify(roleService, times(1)).findById("API_" + ROLENAME);
    }

    @Test
    public void shouldGetMergedPermissionsIfMemberOfApiAndApiGroup() throws Exception {
        ApiEntity api = mock(ApiEntity.class);
        doReturn(API_ID).when(api).getId();
        doReturn(Collections.singleton(GROUP_ID1)).when(api).getGroups();
        doReturn(api).when(apiService).findById(API_ID);

        Membership membershipUser = mock(Membership.class);
        doReturn("API_"+ROLENAME).when(membershipUser).getRoleId();
        doReturn(new HashSet<>(asList(membershipUser))).when(membershipRepository).findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId(USERNAME, MembershipMemberType.USER, MembershipReferenceType.API, API_ID);

        Membership membershipGroup = mock(Membership.class);
        doReturn("API_"+ROLENAME2).when(membershipGroup).getRoleId();
        doReturn(new HashSet<>(asList(membershipGroup))).when(membershipRepository).findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId(USERNAME, MembershipMemberType.USER, MembershipReferenceType.GROUP, GROUP_ID1);

        UserEntity userEntity = mock(UserEntity.class);
        doReturn(userEntity).when(userService).findById(USERNAME);

        RoleEntity roleEntity = mock(RoleEntity.class);
        Map<String, char[]> rolePerms = new HashMap<>();
        rolePerms.put(ApiPermission.DOCUMENTATION.getName(), new char[]{RolePermissionAction.UPDATE.getId(), RolePermissionAction.CREATE.getId()});
        doReturn(rolePerms).when(roleEntity).getPermissions();
        doReturn(roleEntity).when(roleService).findById("API_" + ROLENAME);

        RoleEntity roleEntity2 = mock(RoleEntity.class);
        Map<String, char[]> rolePerms2 = new HashMap<>();
        rolePerms2.put(ApiPermission.DOCUMENTATION.getName(), new char[]{RolePermissionAction.READ.getId(), RolePermissionAction.DELETE.getId()});
        rolePerms2.put(ApiPermission.PLAN.getName(), new char[]{RolePermissionAction.READ.getId()});
        doReturn(rolePerms2).when(roleEntity2).getPermissions();
        doReturn(RoleScope.API).when(roleEntity2).getScope();
        doReturn(roleEntity2).when(roleService).findById("API_" + ROLENAME2);

        Map<String, char[]> permissions = membershipService.getUserMemberPermissions(api, USERNAME);

        assertNotNull(permissions);
        Map<String, char[]> expectedPermissions = new HashMap<>();
        expectedPermissions.put(ApiPermission.DOCUMENTATION.getName(), new char[]{
                RolePermissionAction.CREATE.getId(),
                RolePermissionAction.READ.getId(),
                RolePermissionAction.UPDATE.getId(),
                RolePermissionAction.DELETE.getId()});
        expectedPermissions.put(ApiPermission.PLAN.getName(), new char[]{RolePermissionAction.READ.getId()});
        assertPermissions(expectedPermissions, permissions);
        verify(membershipRepository, times(1)).findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId(USERNAME, MembershipMemberType.USER, MembershipReferenceType.API, API_ID);
        verify(membershipRepository, times(1)).findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId(USERNAME, MembershipMemberType.USER, MembershipReferenceType.GROUP, GROUP_ID1);
        verify(apiService, times(1)).findById(API_ID);
        verify(userService, times(1)).findById(USERNAME);
        verify(roleService, times(1)).findById("API_" + ROLENAME);
        verify(roleService, times(1)).findById("API_" + ROLENAME2);
    }

    private void assertPermissions(Map<String, char[]> expected, Map<String, char[]> actual) {
        assertEquals("there must be " + expected.size() + " permission", expected.size(), actual.size());
        for (Map.Entry<String, char[]> expectedEntry : expected.entrySet()) {
            assertTrue("must contains perm:" + expectedEntry.getKey(), actual.containsKey(expectedEntry.getKey()));
            Arrays.sort(expectedEntry.getValue());
            String expectedCRUD = new String(expectedEntry.getValue());
            Arrays.sort(actual.get(expectedEntry.getKey()));
            String actualCRUD = new String(actual.get(expectedEntry.getKey()));
            assertEquals("CRUD is OK", expectedCRUD, actualCRUD);
        }
    }
}
