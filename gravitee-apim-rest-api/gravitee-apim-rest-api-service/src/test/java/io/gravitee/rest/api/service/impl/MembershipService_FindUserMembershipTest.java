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

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.google.common.collect.Sets;
import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.management.api.ApiFieldInclusionFilter;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.ApplicationCriteria;
import io.gravitee.repository.management.model.*;
import io.gravitee.rest.api.model.GroupEntity;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.UserMembership;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class MembershipService_FindUserMembershipTest {

    private static final String USER_ID = "john-doe";

    @InjectMocks
    private MembershipService membershipService = new MembershipServiceImpl();

    @Mock
    private MembershipRepository mockMembershipRepository;

    @Mock
    private ApiRepository mockApiRepository;

    @Mock
    private ApplicationRepository mockApplicationRepository;

    @Mock
    private GroupService mockGroupService;

    @Mock
    private RoleService mockRoleService;

    @Test
    public void shouldGetEmptyResultForEnvironmentType() {
        List<UserMembership> references = membershipService.findUserMembership(
            GraviteeContext.getExecutionContext(),
            io.gravitee.rest.api.model.MembershipReferenceType.ENVIRONMENT,
            USER_ID
        );

        assertTrue(references.isEmpty());
    }

    @Test
    public void shouldGetEmptyResultIfNoApiNorGroups() throws Exception {
        when(mockRoleService.findByScope(any(), any())).thenReturn(Collections.emptyList());
        when(
            mockMembershipRepository.findByMemberIdAndMemberTypeAndReferenceType(
                eq(USER_ID),
                eq(MembershipMemberType.USER),
                eq(MembershipReferenceType.API)
            )
        )
            .thenReturn(Collections.emptySet());

        doReturn(Collections.emptySet()).when(mockGroupService).findByUser(USER_ID);

        List<UserMembership> references = membershipService.findUserMembership(
            GraviteeContext.getExecutionContext(),
            io.gravitee.rest.api.model.MembershipReferenceType.API,
            USER_ID
        );

        assertTrue(references.isEmpty());
    }

    @Test
    public void shouldGetApiWithoutGroups() throws Exception {
        RoleEntity roleEntity = new RoleEntity();
        roleEntity.setId("role");
        roleEntity.setName("PO");
        roleEntity.setScope(RoleScope.API);
        when(mockRoleService.findByScope(any(), any())).thenReturn(Collections.singletonList(roleEntity));
        Membership mApi = mock(Membership.class);
        when(mApi.getReferenceId()).thenReturn("api-id1");
        when(mApi.getRoleId()).thenReturn("role");

        when(
            mockMembershipRepository.findByMemberIdAndMemberTypeAndReferenceType(
                eq(USER_ID),
                eq(MembershipMemberType.USER),
                eq(MembershipReferenceType.API)
            )
        )
            .thenReturn(Collections.singleton(mApi));
        doReturn(Collections.emptySet()).when(mockGroupService).findByUser(USER_ID);

        List<UserMembership> references = membershipService.findUserMembership(
            GraviteeContext.getExecutionContext(),
            io.gravitee.rest.api.model.MembershipReferenceType.API,
            USER_ID
        );

        assertFalse(references.isEmpty());
        assertEquals(1, references.size());
        assertEquals("api-id1", references.get(0).getReference());
        assertEquals("API", references.get(0).getType());
    }

    @Test
    public void shouldGetApiWithOnlyGroups() throws Exception {
        when(mockRoleService.findByScope(any(), any())).thenReturn(Collections.emptyList());

        when(
            mockMembershipRepository.findByMemberIdAndMemberTypeAndReferenceType(
                eq(USER_ID),
                eq(MembershipMemberType.USER),
                eq(MembershipReferenceType.API)
            )
        )
            .thenReturn(Collections.emptySet());

        GroupEntity group1 = new GroupEntity();
        group1.setId("Group1Id");
        when(mockGroupService.findByUser(eq(USER_ID))).thenReturn(new HashSet<>(asList(group1)));

        Api api = new Api();
        api.setId("apiGroup1Id");
        when(mockApiRepository.search(eq(new ApiCriteria.Builder().groups("Group1Id").build()), (ApiFieldInclusionFilter) any()))
            .thenReturn(new HashSet<>(asList(api)));

        List<UserMembership> references = membershipService.findUserMembership(
            GraviteeContext.getExecutionContext(),
            io.gravitee.rest.api.model.MembershipReferenceType.API,
            USER_ID
        );

        assertFalse(references.isEmpty());
        assertEquals(1, references.size());
        assertEquals("apiGroup1Id", references.get(0).getReference());
        assertEquals("API", references.get(0).getType());
    }

    @Test
    public void shouldGetApiWithApiAndGroups() throws Exception {
        RoleEntity roleEntity = new RoleEntity();
        roleEntity.setId("role");
        roleEntity.setName("PO");
        roleEntity.setScope(RoleScope.API);
        when(mockRoleService.findByScope(any(), any())).thenReturn(Collections.singletonList(roleEntity));
        Membership mApi = mock(Membership.class);
        when(mApi.getReferenceId()).thenReturn("api-id1");
        when(mApi.getRoleId()).thenReturn("role");

        when(
            mockMembershipRepository.findByMemberIdAndMemberTypeAndReferenceType(
                eq(USER_ID),
                eq(MembershipMemberType.USER),
                eq(MembershipReferenceType.API)
            )
        )
            .thenReturn(Collections.singleton(mApi));

        GroupEntity group1 = new GroupEntity();
        group1.setId("Group1Id");
        when(mockGroupService.findByUser(eq(USER_ID))).thenReturn(new HashSet<>(asList(group1)));

        Api api = new Api();
        api.setId("apiGroup1Id");
        when(mockApiRepository.search(eq(new ApiCriteria.Builder().groups("Group1Id").build()), (ApiFieldInclusionFilter) any()))
            .thenReturn(new HashSet<>(asList(api)));

        List<UserMembership> references = membershipService.findUserMembership(
            GraviteeContext.getExecutionContext(),
            io.gravitee.rest.api.model.MembershipReferenceType.API,
            USER_ID
        );

        assertFalse(references.isEmpty());
        assertEquals(2, references.size());
        assertNotEquals(references.get(0).getReference(), references.get(1).getReference());
        assertEquals(references.get(0).getReference(), "api-id1");
        assertEquals(references.get(1).getReference(), "apiGroup1Id");
        assertEquals("API", references.get(0).getType());
    }

    @Test
    public void shouldGetMembershipForGivenSource() throws Exception {
        RoleEntity roleApi = new RoleEntity();
        roleApi.setId("roleApi");
        roleApi.setName("PO");
        roleApi.setScope(RoleScope.API);
        RoleEntity roleApp = new RoleEntity();
        roleApp.setId("roleApp");
        roleApp.setName("PO");
        roleApp.setScope(RoleScope.API);
        when(mockRoleService.findAllByOrganization(any())).thenReturn(asList(roleApi, roleApp));

        Membership mApi = mock(Membership.class);
        when(mApi.getReferenceId()).thenReturn("api-id1");
        when(mApi.getRoleId()).thenReturn("roleApi");
        when(mApi.getSource()).thenReturn("oauth2");

        Membership mApp = mock(Membership.class);
        when(mApp.getReferenceId()).thenReturn("app-id1");
        when(mApp.getRoleId()).thenReturn("roleApp");
        when(mApp.getSource()).thenReturn("oauth2");

        when(
            mockMembershipRepository.findByMemberIdAndMemberTypeAndReferenceTypeAndSource(
                eq(USER_ID),
                eq(MembershipMemberType.USER),
                eq(MembershipReferenceType.GROUP),
                eq("oauth2")
            )
        )
            .thenReturn(Sets.newHashSet(mApi, mApp));

        List<UserMembership> references = membershipService.findUserMembershipBySource(
            GraviteeContext.getExecutionContext(),
            io.gravitee.rest.api.model.MembershipReferenceType.GROUP,
            USER_ID,
            "oauth2"
        );

        assertFalse(references.isEmpty());
        assertEquals(2, references.size());
        assertNotEquals(references.get(0).getReference(), references.get(1).getReference());
        assertTrue(references.get(0).getReference().equals("api-id1") || references.get(0).getReference().equals("app-id1"));
        assertTrue(references.get(1).getReference().equals("api-id1") || references.get(1).getReference().equals("app-id1"));
    }

    @Test
    public void shouldGetApplicationWithOnlyGroups() throws Exception {
        when(mockRoleService.findByScope(any(), any())).thenReturn(Collections.emptyList());

        when(
            mockMembershipRepository.findByMemberIdAndMemberTypeAndReferenceType(
                eq(USER_ID),
                eq(MembershipMemberType.USER),
                eq(MembershipReferenceType.APPLICATION)
            )
        )
            .thenReturn(Collections.emptySet());

        GroupEntity group1 = new GroupEntity();
        group1.setId("Group1Id");
        when(mockGroupService.findByUser(eq(USER_ID))).thenReturn(new HashSet<>(asList(group1)));

        Application application = new Application();
        application.setId("applicationGroup1Id");
        when(mockApplicationRepository.findByGroups(eq(List.of("Group1Id")))).thenReturn(new HashSet<>(asList(application)));

        List<UserMembership> references = membershipService.findUserMembership(
            GraviteeContext.getExecutionContext(),
            io.gravitee.rest.api.model.MembershipReferenceType.APPLICATION,
            USER_ID
        );

        assertFalse(references.isEmpty());
        assertEquals(1, references.size());
        assertEquals("applicationGroup1Id", references.get(0).getReference());
        assertEquals("APPLICATION", references.get(0).getType());
    }
}
