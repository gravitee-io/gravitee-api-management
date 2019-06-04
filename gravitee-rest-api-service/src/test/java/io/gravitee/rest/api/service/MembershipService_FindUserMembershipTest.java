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
import io.gravitee.repository.management.model.MembershipReferenceType;
import io.gravitee.repository.management.model.RoleScope;
import io.gravitee.rest.api.model.UserMembership;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.api.ApiQuery;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.impl.MembershipServiceImpl;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

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
    private ApiService mockApiService;

    @Mock
    private ApplicationService mockApplicationService;


    @Test
    public void shouldGetEmptyResultForManagementType() {
        List<UserMembership> references = membershipService.findUserMembership(USER_ID, MembershipReferenceType.MANAGEMENT);

        assertTrue(references.isEmpty());
    }


    @Test
    public void shouldGetEmptyResultForPortalType() {
        List<UserMembership> references = membershipService.findUserMembership(USER_ID, MembershipReferenceType.PORTAL);

        assertTrue(references.isEmpty());
    }


    @Test
    public void shouldGetEmptyResultIfNoApiNorGroups() throws Exception {
        when(mockMembershipRepository.findByUserAndReferenceType(eq(USER_ID), any())).thenReturn(Collections.emptySet());

        List<UserMembership> references = membershipService.findUserMembership(USER_ID, MembershipReferenceType.API);

        assertTrue(references.isEmpty());
        verify(mockApiService, never()).search(any(ApiQuery.class));
        verify(mockApplicationService, never()).findByGroups(any());
    }


    @Test
    public void shouldGetApiWithoutGroups() throws Exception {
        Membership m = mock(Membership.class);
        when(m.getReferenceId()).thenReturn("api-id");
        when(mockMembershipRepository.findByUserAndReferenceType(eq(USER_ID), eq(MembershipReferenceType.API))).thenReturn(Collections.singleton(m));
        when(mockMembershipRepository.findByUserAndReferenceType(eq(USER_ID), eq(MembershipReferenceType.GROUP))).thenReturn(Collections.emptySet());

        List<UserMembership> references = membershipService.findUserMembership(USER_ID, MembershipReferenceType.API);

        assertFalse(references.isEmpty());
        assertEquals(1, references.size());
        assertEquals("api-id", references.get(0).getReference());
        assertEquals("API", references.get(0).getType());
        verify(mockApiService, never()).search(any(ApiQuery.class));
        verify(mockApplicationService, never()).findByGroups(any());
    }


    @Test
    public void shouldGetApiWithOnlyGroups() throws Exception {
        Membership m = mock(Membership.class);
        when(m.getReferenceId())
                .thenReturn("group-id");
        when(m.getRoles())
                .thenReturn(Collections.singletonMap(RoleScope.API.getId(), "CUSTOM_ROLE"));
        when(mockMembershipRepository.findByUserAndReferenceType(eq(USER_ID), eq(MembershipReferenceType.API)))
                .thenReturn(Collections.emptySet());
        when(mockMembershipRepository.findByUserAndReferenceType(eq(USER_ID), eq(MembershipReferenceType.GROUP)))
                .thenReturn(Collections.singleton(m));
        ApiEntity api = mock(ApiEntity.class);
        when(api.getId()).thenReturn("api-id");
        when(mockApiService.search(any(ApiQuery.class)))
                .thenReturn(Collections.singleton(api));

        List<UserMembership> references = membershipService.findUserMembership(USER_ID, MembershipReferenceType.API);

        assertFalse(references.isEmpty());
        assertEquals(1, references.size());
        assertEquals("api-id", references.get(0).getReference());
        assertEquals("API", references.get(0).getType());
        verify(mockApiService, times(1)).search(any(ApiQuery.class));
        verify(mockApplicationService, never()).findByGroups(any());
    }


    @Test
    public void shouldGetApiWithApiAndGroups() throws Exception {
        Membership mApi = mock(Membership.class);
        when(mApi.getReferenceId()).thenReturn("api-id1");
        Membership mGroup = mock(Membership.class);
        when(mGroup.getReferenceId())
                .thenReturn("group-id");
        when(mGroup.getRoles())
                .thenReturn(Collections.singletonMap(RoleScope.API.getId(), "CUSTOM_ROLE"));
        when(mockMembershipRepository.findByUserAndReferenceType(eq(USER_ID), eq(MembershipReferenceType.API)))
                .thenReturn(Collections.singleton(mApi));
        when(mockMembershipRepository.findByUserAndReferenceType(eq(USER_ID), eq(MembershipReferenceType.GROUP)))
                .thenReturn(Collections.singleton(mGroup));
        ApiEntity api = mock(ApiEntity.class);
        when(api.getId()).thenReturn("api-id2");
        when(mockApiService.search(any(ApiQuery.class)))
                .thenReturn(Collections.singleton(api));

        List<UserMembership> references = membershipService.findUserMembership(USER_ID, MembershipReferenceType.API);

        assertFalse(references.isEmpty());
        assertEquals(2, references.size());
        assertNotEquals(references.get(0).getReference(), references.get(1).getReference());
        assertTrue(references.get(0).getReference().equals("api-id1") || references.get(0).getReference().equals("api-id2"));
        assertTrue(references.get(1).getReference().equals("api-id1") || references.get(1).getReference().equals("api-id2"));
        assertEquals("API", references.get(0).getType());
        verify(mockApiService, times(1)).search(any(ApiQuery.class));
        verify(mockApplicationService, never()).findByGroups(any());
    }
}
