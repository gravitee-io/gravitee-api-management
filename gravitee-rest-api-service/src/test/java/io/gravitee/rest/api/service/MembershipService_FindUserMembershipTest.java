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
import io.gravitee.rest.api.model.UserMembership;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.impl.MembershipServiceImpl;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;
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
    private GroupService mockGroupService;

    @Test
    public void shouldGetEmptyResultForEnvironmentType() {
        List<UserMembership> references = membershipService.findUserMembership(io.gravitee.rest.api.model.MembershipReferenceType.ENVIRONMENT, USER_ID);

        assertTrue(references.isEmpty());
    }


    @Test
    public void shouldGetEmptyResultIfNoApiNorGroups() throws Exception {
        when(mockMembershipRepository.findByMemberIdAndMemberTypeAndReferenceType(eq(USER_ID), eq(MembershipMemberType.USER), eq(MembershipReferenceType.API)))
                .thenReturn(Collections.emptySet());
        
        doReturn(Collections.emptySet()).when(mockGroupService).findByUser(USER_ID);

        List<UserMembership> references = membershipService.findUserMembership(io.gravitee.rest.api.model.MembershipReferenceType.API, USER_ID);

        assertTrue(references.isEmpty());
    }

    @Test
    public void shouldGetApiWithoutGroups() throws Exception {
        Membership mApi = mock(Membership.class);
        when(mApi.getReferenceId()).thenReturn("api-id1");
        
        when(mockMembershipRepository.findByMemberIdAndMemberTypeAndReferenceType(eq(USER_ID), eq(MembershipMemberType.USER), eq(MembershipReferenceType.API)))
                .thenReturn(Collections.singleton(mApi));
        doReturn(Collections.emptySet()).when(mockGroupService).findByUser(USER_ID);

        List<UserMembership> references = membershipService.findUserMembership(io.gravitee.rest.api.model.MembershipReferenceType.API, USER_ID);

        assertFalse(references.isEmpty());
        assertEquals(1, references.size());
        assertEquals("api-id1", references.get(0).getReference());
        assertEquals("API", references.get(0).getType());
    }

    @Test
    public void shouldGetApiWithOnlyGroups() throws Exception {
        Membership mGroup = mock(Membership.class);
        when(mGroup.getReferenceId()).thenReturn("api-id2");
        
        when(mockMembershipRepository.findByMemberIdAndMemberTypeAndReferenceType(eq(USER_ID), eq(MembershipMemberType.USER), eq(MembershipReferenceType.API)))
                .thenReturn(Collections.emptySet());
        
        when(mockMembershipRepository.findByMemberIdAndMemberTypeAndReferenceType(eq("GROUP"), eq(MembershipMemberType.GROUP), eq(MembershipReferenceType.API)))
                .thenReturn(Collections.singleton(mGroup));
        GroupEntity group1 = mock(GroupEntity.class);
        doReturn("GROUP").when(group1).getId();
        doReturn(new HashSet<>(asList(group1))).when(mockGroupService).findByUser(USER_ID);


        List<UserMembership> references = membershipService.findUserMembership(io.gravitee.rest.api.model.MembershipReferenceType.API, USER_ID);

        assertFalse(references.isEmpty());
        assertEquals(1, references.size());
        assertEquals("api-id2", references.get(0).getReference());
        assertEquals("API", references.get(0).getType());
    }

    @Test
    public void shouldGetApiWithApiAndGroups() throws Exception {
        Membership mApi = mock(Membership.class);
        when(mApi.getReferenceId()).thenReturn("api-id1");
        
        Membership mGroup = mock(Membership.class);
        when(mGroup.getReferenceId()).thenReturn("api-id2");
        
        when(mockMembershipRepository.findByMemberIdAndMemberTypeAndReferenceType(eq(USER_ID), eq(MembershipMemberType.USER), eq(MembershipReferenceType.API)))
                .thenReturn(Collections.singleton(mApi));
        
        when(mockMembershipRepository.findByMemberIdAndMemberTypeAndReferenceType(eq("GROUP"), eq(MembershipMemberType.GROUP), eq(MembershipReferenceType.API)))
                .thenReturn(Collections.singleton(mGroup));
        GroupEntity group1 = mock(GroupEntity.class);
        doReturn("GROUP").when(group1).getId();
        doReturn(new HashSet<>(asList(group1))).when(mockGroupService).findByUser(USER_ID);


        List<UserMembership> references = membershipService.findUserMembership(io.gravitee.rest.api.model.MembershipReferenceType.API, USER_ID);

        assertFalse(references.isEmpty());
        assertEquals(2, references.size());
        assertNotEquals(references.get(0).getReference(), references.get(1).getReference());
        assertTrue(references.get(0).getReference().equals("api-id1") || references.get(0).getReference().equals("api-id2"));
        assertTrue(references.get(1).getReference().equals("api-id1") || references.get(1).getReference().equals("api-id2"));
        assertEquals("API", references.get(0).getType());
    }
}
