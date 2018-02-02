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
package io.gravitee.management.service;

import io.gravitee.management.model.*;
import io.gravitee.management.model.permissions.ApiPermission;
import io.gravitee.management.model.permissions.RolePermissionAction;
import io.gravitee.management.service.impl.MembershipServiceImpl;
import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.model.Membership;
import io.gravitee.repository.management.model.MembershipReferenceType;
import io.gravitee.repository.management.model.RoleScope;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.*;

import static java.util.Optional.empty;
import static java.util.Optional.of;
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
    private static final String GROUP_ID2 = "GROUP_ID2";
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
    private RoleService roleService;

    @Test
    public void shouldGetNoPermissionsIfNotMemberWithoutGroups() throws Exception {
        ApiEntity api = mock(ApiEntity.class);
        doReturn(API_ID).when(api).getId();
        doReturn(empty()).when(membershipRepository).findById(USERNAME, MembershipReferenceType.API, API_ID);

        Map<String, char[]> permissions = membershipService.getMemberPermissions(api, USERNAME);

        assertNotNull(permissions);
        assertTrue("permissions are empty", permissions.isEmpty());
        verify(membershipRepository, times(1)).findById(USERNAME, MembershipReferenceType.API, API_ID);
        verify(membershipRepository, never()).findById(eq(USERNAME), eq(MembershipReferenceType.GROUP), anyString());
    }

    @Test
    public void shouldGetNoPermissionsIfNotMemberWithGroups() throws Exception {
        ApiEntity api = mock(ApiEntity.class);
        doReturn(API_ID).when(api).getId();
        when(api.getGroups()).thenReturn(Collections.singleton(GROUP_ID1));
        doReturn(empty()).when(membershipRepository).findById(USERNAME, MembershipReferenceType.API, API_ID);
        doReturn(empty()).when(membershipRepository).findById(USERNAME, MembershipReferenceType.GROUP, GROUP_ID1);

        Map<String, char[]> permissions = membershipService.getMemberPermissions(api, USERNAME);

        assertNotNull(permissions);
        assertTrue("permissions are empty", permissions.isEmpty());
        verify(membershipRepository, times(1)).findById(USERNAME, MembershipReferenceType.API, API_ID);
        verify(membershipRepository, times(1)).findById(USERNAME, MembershipReferenceType.GROUP, GROUP_ID1);
    }

    @Test
    public void shouldGetPermissionsIfMemberOfApi() throws Exception {
        ApiEntity api = mock(ApiEntity.class);
        doReturn(API_ID).when(api).getId();
        Membership membership = mock(Membership.class);
        doReturn(Collections.singletonMap(RoleScope.API.getId(), ROLENAME)).when(membership).getRoles();
        doReturn(MembershipReferenceType.API).when(membership).getReferenceType();
        doReturn(API_ID).when(membership).getReferenceId();
        doReturn(USERNAME).when(membership).getUserId();
        GroupEntity group = mock(GroupEntity.class);
        doReturn(GROUP_ID1).when(group).getId();
        doReturn(Collections.singleton(group)).when(api).getGroups();
        doReturn(of(membership)).when(membershipRepository).findById(USERNAME, MembershipReferenceType.API, API_ID);
        UserEntity userEntity = mock(UserEntity.class);
        doReturn(userEntity).when(userService).findById(USERNAME);
        RoleEntity roleEntity = mock(RoleEntity.class);
        Map<String, char[]> rolePerms = new HashMap<>();
        rolePerms.put(ApiPermission.DOCUMENTATION.getName(), new char[]{RolePermissionAction.UPDATE.getId(), RolePermissionAction.CREATE.getId()});
        doReturn(rolePerms).when(roleEntity).getPermissions();
        doReturn(roleEntity).when(roleService).findById(RoleScope.API, ROLENAME);

        Map<String, char[]> permissions = membershipService.getMemberPermissions(api, USERNAME);

        assertNotNull(permissions);
        assertPermissions(rolePerms, permissions);
        verify(membershipRepository, times(2)).findById(USERNAME, MembershipReferenceType.API, API_ID);
        verify(membershipRepository, never()).findById(eq(USERNAME), eq(MembershipReferenceType.GROUP), anyString());
        verify(userService, times(1)).findById(USERNAME);
    }

    @Test
    public void shouldGetPermissionsIfMemberOfApiGroup() throws Exception {
        ApiEntity api = mock(ApiEntity.class);
        doReturn(API_ID).when(api).getId();

        when(api.getGroups()).thenReturn(Collections.singleton(GROUP_ID1));

        Membership membership = mock(Membership.class);
        doReturn(Collections.singletonMap(RoleScope.API.getId(), ROLENAME)).when(membership).getRoles();
        doReturn(MembershipReferenceType.GROUP).when(membership).getReferenceType();
        doReturn(GROUP_ID1).when(membership).getReferenceId();
        doReturn(USERNAME).when(membership).getUserId();
        doReturn(empty()).when(membershipRepository).findById(USERNAME, MembershipReferenceType.API, API_ID);
        doReturn(of(membership)).when(membershipRepository).findById(USERNAME, MembershipReferenceType.GROUP, GROUP_ID1);

        UserEntity userEntity = mock(UserEntity.class);
        doReturn(userEntity).when(userService).findById(USERNAME);

        RoleEntity roleEntity = mock(RoleEntity.class);
        Map<String, char[]> rolePerms = new HashMap<>();
        rolePerms.put(ApiPermission.DOCUMENTATION.getName(), new char[]{RolePermissionAction.UPDATE.getId(), RolePermissionAction.CREATE.getId()});
        doReturn(rolePerms).when(roleEntity).getPermissions();
        doReturn(roleEntity).when(roleService).findById(RoleScope.API, ROLENAME);

        Map<String, char[]> permissions = membershipService.getMemberPermissions(api, USERNAME);

        assertNotNull(permissions);
        assertPermissions(rolePerms, permissions);
        verify(membershipRepository, times(1)).findById(USERNAME, MembershipReferenceType.API, API_ID);
        verify(membershipRepository, times(2)).findById(eq(USERNAME), eq(MembershipReferenceType.GROUP), anyString());
        verify(userService, times(1)).findById(USERNAME);
    }

    @Test
    public void shouldGetMergedPermissionsIfMemberOfMultipleApiGroups() throws Exception {
        ApiEntity api = mock(ApiEntity.class);
        doReturn(API_ID).when(api).getId();

        when(api.getGroups()).thenReturn(new HashSet<>(Arrays.asList(GROUP_ID1, GROUP_ID2)));

        Membership membership1 = mock(Membership.class);
        doReturn(Collections.singletonMap(RoleScope.API.getId(), ROLENAME)).when(membership1).getRoles();
        doReturn(MembershipReferenceType.GROUP).when(membership1).getReferenceType();
        doReturn(GROUP_ID1).when(membership1).getReferenceId();
        doReturn(USERNAME).when(membership1).getUserId();
        doReturn(empty()).when(membershipRepository).findById(USERNAME, MembershipReferenceType.API, API_ID);
        doReturn(of(membership1)).when(membershipRepository).findById(USERNAME, MembershipReferenceType.GROUP, GROUP_ID1);

        Membership membership2 = mock(Membership.class);
        doReturn(Collections.singletonMap(RoleScope.API.getId(), ROLENAME2)).when(membership2).getRoles();
        doReturn(MembershipReferenceType.GROUP).when(membership2).getReferenceType();
        doReturn(GROUP_ID2).when(membership2).getReferenceId();
        doReturn(USERNAME).when(membership2).getUserId();
        doReturn(empty()).when(membershipRepository).findById(USERNAME, MembershipReferenceType.API, API_ID);
        doReturn(of(membership2)).when(membershipRepository).findById(USERNAME, MembershipReferenceType.GROUP, GROUP_ID2);

        UserEntity userEntity = mock(UserEntity.class);
        doReturn(userEntity).when(userService).findById(USERNAME);

        RoleEntity roleEntity = mock(RoleEntity.class);
        Map<String, char[]> rolePerms = new HashMap<>();
        rolePerms.put(ApiPermission.DOCUMENTATION.getName(), new char[]{RolePermissionAction.UPDATE.getId(), RolePermissionAction.CREATE.getId()});
        doReturn(rolePerms).when(roleEntity).getPermissions();
        doReturn(roleEntity).when(roleService).findById(RoleScope.API, ROLENAME);

        RoleEntity roleEntity2 = mock(RoleEntity.class);
        Map<String, char[]> rolePerms2 = new HashMap<>();
        rolePerms2.put(ApiPermission.DOCUMENTATION.getName(), new char[]{RolePermissionAction.READ.getId(), RolePermissionAction.DELETE.getId()});
        rolePerms2.put(ApiPermission.PLAN.getName(), new char[]{RolePermissionAction.READ.getId()});
        doReturn(rolePerms2).when(roleEntity2).getPermissions();
        doReturn(roleEntity2).when(roleService).findById(RoleScope.API, ROLENAME2);

        Map<String, char[]> permissions = membershipService.getMemberPermissions(api, USERNAME);

        assertNotNull(permissions);
        Map<String, char[]> expectedPermissions = new HashMap<>();
        expectedPermissions.put(ApiPermission.DOCUMENTATION.getName(), new char[]{
                RolePermissionAction.CREATE.getId(),
                RolePermissionAction.READ.getId(),
                RolePermissionAction.UPDATE.getId(),
                RolePermissionAction.DELETE.getId()});
        expectedPermissions.put(ApiPermission.PLAN.getName(), new char[]{RolePermissionAction.READ.getId()});
        assertPermissions(expectedPermissions, permissions);
        verify(membershipRepository, times(1)).findById(USERNAME, MembershipReferenceType.API, API_ID);
        verify(membershipRepository, times(4)).findById(eq(USERNAME), eq(MembershipReferenceType.GROUP), anyString());
        verify(userService, times(2)).findById(USERNAME);
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
