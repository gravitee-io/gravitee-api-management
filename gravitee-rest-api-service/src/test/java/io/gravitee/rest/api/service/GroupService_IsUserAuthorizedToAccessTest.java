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

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.GroupRepository;
import io.gravitee.repository.management.model.Group;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.impl.GroupServiceImpl;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class GroupService_IsUserAuthorizedToAccessTest {

    @InjectMocks
    private GroupService groupService = new GroupServiceImpl();

    @Mock
    private MembershipService membershipService;

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private ApiEntity api;

    @Test
    public void shouldBeAuthorizedForAnonymousAndPublicApiWithoutRestrictions() throws TechnicalException {
        when(api.getVisibility()).thenReturn(Visibility.PUBLIC);

        boolean userAuthorizedToAccess = groupService.isUserAuthorizedToAccessApiData(api, Collections.emptyList(), null);

        assertTrue(userAuthorizedToAccess);
        verify(membershipService, never()).getRoles(any(), any(), any(), any());
        verify(groupRepository, never()).findAllByEnvironment("DEFAULT");
    }

    @Test
    public void shouldNotBeAuthorizedForAnonymousAndPrivateApiWithoutRestrictions() throws TechnicalException {
        when(api.getVisibility()).thenReturn(Visibility.PRIVATE);

        boolean userAuthorizedToAccess = groupService.isUserAuthorizedToAccessApiData(api, Collections.emptyList(), null);

        assertFalse(userAuthorizedToAccess);
        verify(membershipService, never()).getRoles(any(), any(), any(), any());
        verify(groupRepository, never()).findAllByEnvironment("DEFAULT");
    }

    @Test
    public void shouldBeAuthorizedForPublicApiWithoutRestriction() throws TechnicalException {
        boolean userAuthorizedToAccess = groupService.isUserAuthorizedToAccessApiData(api, Collections.emptyList(), "user");

        assertTrue(userAuthorizedToAccess);
        verify(membershipService, never()).getRoles(any(), any(), any(), any());
        verify(groupRepository, never()).findAllByEnvironment("DEFAULT");
    }

    @Test
    public void shouldBeAuthorizedForPrivateApiWithoutGroups() throws TechnicalException {
        when(api.getVisibility()).thenReturn(Visibility.PRIVATE);
        when(api.getGroups()).thenReturn(null);
        when(membershipService.getRoles(any(), any(), any(), any())).thenReturn(Collections.emptySet());
        boolean userAuthorizedToAccess = groupService.isUserAuthorizedToAccessApiData(api, Collections.singletonList("grp1"), "user");

        assertFalse(userAuthorizedToAccess);
        verify(membershipService, times(1)).getRoles(any(), any(), any(), any());
        verify(groupRepository, never()).findAllByEnvironment("DEFAULT");
    }

    @Test
    public void shouldBeAuthorizedForPrivateApiWithoutExcludedGroups() throws TechnicalException {
        boolean userAuthorizedToAccess = groupService.isUserAuthorizedToAccessApiData(api, null, "user");

        assertTrue(userAuthorizedToAccess);
        verify(membershipService, never()).getRoles(any(), any(), any(), any());
        verify(groupRepository, never()).findAllByEnvironment("DEFAULT");
    }

    @Test
    public void shouldBeAuthorizedForPrivateApiWithDirectMember() throws TechnicalException {
        when(api.getId()).thenReturn("apiId");
        when(membershipService.getRoles(
                MembershipReferenceType.API,
                api.getId(),
                MembershipMemberType.USER,
                "user")).
                thenReturn(new HashSet<>(Arrays.asList(new RoleEntity())));

        boolean userAuthorizedToAccess = groupService.isUserAuthorizedToAccessApiData(api, Collections.singletonList("grp1"), "user");

        assertTrue(userAuthorizedToAccess);
        verify(membershipService, times(1)).getRoles(any(), any(), any(), any());
        verify(api, never()).getGroups();
        verify(groupRepository, never()).findAllByEnvironment("DEFAULT");
    }

    @Test
    public void shouldBeAuthorizedForPrivateApiIfMemberOfAuthorizedGroups() throws TechnicalException {
        when(api.getVisibility()).thenReturn(Visibility.PRIVATE);
        when(api.getId()).thenReturn("apiId");
        when(api.getGroups()).thenReturn(new HashSet<>(Arrays.asList("grp1", "grp2")));
        when(membershipService.getRoles(
                MembershipReferenceType.API,
                api.getId(),
                MembershipMemberType.USER,
                "user")).
                thenReturn(Collections.emptySet());
        RoleEntity apiRoleEntity = new RoleEntity();
        apiRoleEntity.setScope(RoleScope.API);
        when(membershipService.getRoles(
                MembershipReferenceType.GROUP,
                "grp2",
                MembershipMemberType.USER,
                "user")).
                thenReturn(new HashSet<>(Arrays.asList(apiRoleEntity)));

        boolean userAuthorizedToAccess = groupService.isUserAuthorizedToAccessApiData(api, Collections.singletonList("grp1"), "user");

        assertTrue(userAuthorizedToAccess);
        verify(membershipService, times(2)).getRoles(any(), any(), any(), any());
        verify(membershipService, times(1)).
                getRoles(MembershipReferenceType.API,
                        api.getId(),
                        MembershipMemberType.USER,
                        "user");
        verify(membershipService, times(1)).
                getRoles(MembershipReferenceType.GROUP,
                        "grp2",
                        MembershipMemberType.USER,
                        "user");
        verify(api, atLeast(2)).getGroups();
        verify(groupRepository, never()).findAllByEnvironment("DEFAULT");
    }

    @Test
    public void shouldNotBeAuthorizedForPrivateApiIfMemberOfUnauthorizedGroups() throws TechnicalException {
        when(api.getVisibility()).thenReturn(Visibility.PRIVATE);
        when(api.getId()).thenReturn("apiId");
        when(api.getGroups()).thenReturn(new HashSet<>(Arrays.asList("grp1", "grp2")));
        when(membershipService.getRoles(
                MembershipReferenceType.API,
                api.getId(),
                MembershipMemberType.USER,
                "user")).
                thenReturn(Collections.emptySet());
        when(membershipService.getRoles(
                MembershipReferenceType.GROUP,
                "grp2",
                MembershipMemberType.USER,
                "user")).
                thenReturn(Collections.emptySet());

        boolean userAuthorizedToAccess = groupService.isUserAuthorizedToAccessApiData(api, Collections.singletonList("grp1"), "user");

        assertFalse(userAuthorizedToAccess);
        verify(membershipService, times(2)).getRoles(any(), any(), any(), any());
        verify(membershipService, times(1)).
                getRoles(MembershipReferenceType.API,
                        api.getId(),
                        MembershipMemberType.USER,
                        "user");
        verify(membershipService, times(1)).
                getRoles(MembershipReferenceType.GROUP,
                        "grp2",
                        MembershipMemberType.USER,
                        "user");
        verify(api, atLeast(2)).getGroups();
        verify(groupRepository, never()).findAllByEnvironment("DEFAULT");
    }

    @Test
    public void shouldNotBeAuthorizedForPublicApiIfMemberOfUnauthorizedGroups() throws TechnicalException {
        when(api.getVisibility()).thenReturn(Visibility.PUBLIC);
        when(api.getId()).thenReturn("apiId");
        Group grp1 = new Group();
        Group grp2 = new Group();
        grp1.setId("grp1");
        grp2.setId("grp2");
        when(groupRepository.findAllByEnvironment("DEFAULT")).thenReturn(new HashSet<>(Arrays.asList(grp1, grp2)));
        when(membershipService.getRoles(
                MembershipReferenceType.API,
                api.getId(),
                MembershipMemberType.USER,
                "user")).
                thenReturn(Collections.emptySet());
        when(membershipService.getRoles(
                MembershipReferenceType.GROUP,
                "grp2",
                MembershipMemberType.USER,
                "user")).
                thenReturn(Collections.emptySet());

        boolean userAuthorizedToAccess = groupService.isUserAuthorizedToAccessApiData(api, Collections.singletonList("grp1"), "user");

        assertFalse(userAuthorizedToAccess);
        verify(membershipService, times(2)).getRoles(any(), any(), any(), any());
        verify(membershipService, times(1)).
                getRoles(MembershipReferenceType.API,
                        api.getId(),
                        MembershipMemberType.USER,
                        "user");
        verify(membershipService, times(1)).
                getRoles(MembershipReferenceType.GROUP,
                        "grp2",
                        MembershipMemberType.USER,
                        "user");
        verify(api, never()).getGroups();
        verify(groupRepository, times(1)).findAllByEnvironment("DEFAULT");
    }
}
