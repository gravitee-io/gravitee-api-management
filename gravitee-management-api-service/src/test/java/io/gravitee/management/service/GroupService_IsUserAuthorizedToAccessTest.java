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

import io.gravitee.management.model.api.ApiEntity;
import io.gravitee.management.model.MemberEntity;
import io.gravitee.management.model.Visibility;
import io.gravitee.management.service.impl.GroupServiceImpl;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.GroupRepository;
import io.gravitee.repository.management.model.Group;
import io.gravitee.repository.management.model.MembershipReferenceType;
import io.gravitee.repository.management.model.RoleScope;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
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
        verify(membershipService, never()).getMember(any(), any(), any(), any());
        verify(groupRepository, never()).findAll();
    }

    @Test
    public void shouldNotBeAuthorizedForAnonymousAndPrivateApiWithoutRestrictions() throws TechnicalException {
        when(api.getVisibility()).thenReturn(Visibility.PRIVATE);

        boolean userAuthorizedToAccess = groupService.isUserAuthorizedToAccessApiData(api, Collections.emptyList(), null);

        assertFalse(userAuthorizedToAccess);
        verify(membershipService, never()).getMember(any(), any(), any(), any());
        verify(groupRepository, never()).findAll();
    }


    @Test
    public void shouldBeAuthorizedForPublicApiWithoutRestriction() throws TechnicalException {
        when(api.getVisibility()).thenReturn(Visibility.PUBLIC);

        boolean userAuthorizedToAccess = groupService.isUserAuthorizedToAccessApiData(api, Collections.emptyList(), "user");

        assertTrue(userAuthorizedToAccess);
        verify(membershipService, never()).getMember(any(), any(), any(), any());
        verify(groupRepository, never()).findAll();
    }

    @Test
    public void shouldBeAuthorizedForPrivateApiWithoutGroups() throws TechnicalException {
        when(api.getVisibility()).thenReturn(Visibility.PRIVATE);
        when(api.getGroups()).thenReturn(null);

        boolean userAuthorizedToAccess = groupService.isUserAuthorizedToAccessApiData(api, Collections.singletonList("grp1"), "user");

        assertFalse(userAuthorizedToAccess);
        verify(membershipService, times(1)).getMember(any(), any(), any(), any());
        verify(groupRepository, never()).findAll();
    }

    @Test
    public void shouldBeAuthorizedForPrivateApiWithoutExcludedGroups() throws TechnicalException {
        when(api.getVisibility()).thenReturn(Visibility.PRIVATE);
        when(api.getGroups()).thenReturn(Collections.singleton("grp1"));

        boolean userAuthorizedToAccess = groupService.isUserAuthorizedToAccessApiData(api, null, "user");

        assertTrue(userAuthorizedToAccess);
        verify(membershipService, never()).getMember(any(), any(), any(), any());
        verify(groupRepository, never()).findAll();
    }

    @Test
    public void shouldBeAuthorizedForPrivateApiWithDirectMember() throws TechnicalException {
        when(api.getVisibility()).thenReturn(Visibility.PRIVATE);
        when(api.getId()).thenReturn("apiId");
        when(api.getGroups()).thenReturn(Collections.singleton("grp1"));
        when(membershipService.getMember(
                MembershipReferenceType.API,
                api.getId(),
                "user",
                RoleScope.API)).
                thenReturn(new MemberEntity());

        boolean userAuthorizedToAccess = groupService.isUserAuthorizedToAccessApiData(api, Collections.singletonList("grp1"), "user");

        assertTrue(userAuthorizedToAccess);
        verify(membershipService, times(1)).getMember(any(), any(), any(), any());
        verify(membershipService, times(1)).
                getMember(MembershipReferenceType.API,
                        api.getId(),
                        "user",
                        RoleScope.API);
        verify(api, never()).getGroups();
        verify(groupRepository, never()).findAll();
    }

    @Test
    public void shouldBeAuthorizedForPrivateApiIfMemberOfAuthorizedGroups() throws TechnicalException {
        when(api.getVisibility()).thenReturn(Visibility.PRIVATE);
        when(api.getId()).thenReturn("apiId");
        when(api.getGroups()).thenReturn(new HashSet<>(Arrays.asList("grp1", "grp2")));
        when(membershipService.getMember(
                MembershipReferenceType.API,
                api.getId(),
                "user",
                RoleScope.API)).
                thenReturn(null);
        when(membershipService.getMember(
                MembershipReferenceType.GROUP,
                "grp2",
                "user",
                RoleScope.API)).
                thenReturn(new MemberEntity());

        boolean userAuthorizedToAccess = groupService.isUserAuthorizedToAccessApiData(api, Collections.singletonList("grp1"), "user");

        assertTrue(userAuthorizedToAccess);
        verify(membershipService, times(2)).getMember(any(), any(), any(), any());
        verify(membershipService, times(1)).
                getMember(MembershipReferenceType.API,
                        api.getId(),
                        "user",
                        RoleScope.API);
        verify(membershipService, times(1)).
                getMember(MembershipReferenceType.GROUP,
                        "grp2",
                        "user",
                        RoleScope.API);
        verify(api, atLeast(2)).getGroups();
        verify(groupRepository, never()).findAll();
    }

    @Test
    public void shouldNotBeAuthorizedForPrivateApiIfMemberOfUnauthorizedGroups() throws TechnicalException {
        when(api.getVisibility()).thenReturn(Visibility.PRIVATE);
        when(api.getId()).thenReturn("apiId");
        when(api.getGroups()).thenReturn(new HashSet<>(Arrays.asList("grp1", "grp2")));
        when(membershipService.getMember(
                MembershipReferenceType.API,
                api.getId(),
                "user",
                RoleScope.API)).
                thenReturn(null);
        when(membershipService.getMember(
                MembershipReferenceType.GROUP,
                "grp2",
                "user",
                RoleScope.API)).
                thenReturn(null);

        boolean userAuthorizedToAccess = groupService.isUserAuthorizedToAccessApiData(api, Collections.singletonList("grp1"), "user");

        assertFalse(userAuthorizedToAccess);
        verify(membershipService, times(2)).getMember(any(), any(), any(), any());
        verify(membershipService, times(1)).
                getMember(MembershipReferenceType.API,
                        api.getId(),
                        "user",
                        RoleScope.API);
        verify(membershipService, times(1)).
                getMember(MembershipReferenceType.GROUP,
                        "grp2",
                        "user",
                        RoleScope.API);
        verify(api, atLeast(2)).getGroups();
        verify(groupRepository, never()).findAll();
    }

    @Test
    public void shouldNotBeAuthorizedForPublicApiIfMemberOfUnauthorizedGroups() throws TechnicalException {
        when(api.getVisibility()).thenReturn(Visibility.PUBLIC);
        when(api.getId()).thenReturn("apiId");
        Group grp1 = new Group();
        Group grp2 = new Group();
        grp1.setId("grp1");
        grp2.setId("grp2");
        when(groupRepository.findAll()).thenReturn(new HashSet<>(Arrays.asList(grp1, grp2)));
        when(api.getGroups()).thenReturn(new HashSet<>(Arrays.asList("grp1", "grp2")));
        when(membershipService.getMember(
                MembershipReferenceType.API,
                api.getId(),
                "user",
                RoleScope.API)).
                thenReturn(null);
        when(membershipService.getMember(
                MembershipReferenceType.GROUP,
                "grp2",
                "user",
                RoleScope.API)).
                thenReturn(null);

        boolean userAuthorizedToAccess = groupService.isUserAuthorizedToAccessApiData(api, Collections.singletonList("grp1"), "user");

        assertFalse(userAuthorizedToAccess);
        verify(membershipService, times(2)).getMember(any(), any(), any(), any());
        verify(membershipService, times(1)).
                getMember(MembershipReferenceType.API,
                        api.getId(),
                        "user",
                        RoleScope.API);
        verify(membershipService, times(1)).
                getMember(MembershipReferenceType.GROUP,
                        "grp2",
                        "user",
                        RoleScope.API);
        verify(api, never()).getGroups();
        verify(groupRepository, times(1)).findAll();
    }
}
