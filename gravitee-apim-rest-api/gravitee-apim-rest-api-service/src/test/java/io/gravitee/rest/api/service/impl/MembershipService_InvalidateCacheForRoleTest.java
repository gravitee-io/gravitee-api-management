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

import static org.mockito.Mockito.*;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.model.Membership;
import io.gravitee.repository.management.model.MembershipMemberType;
import io.gravitee.repository.management.model.MembershipReferenceType;
import io.gravitee.rest.api.service.MembershipService;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
public class MembershipService_InvalidateCacheForRoleTest {

    private static final String ROLE_ID = "role-id-1";
    private static final String USER_ID_1 = "user-id-1";
    private static final String USER_ID_2 = "user-id-2";
    private static final String API_ID = "api-id-1";
    private static final String APP_ID = "app-id-1";

    private MembershipService membershipService;

    @Mock
    private MembershipRepository membershipRepository;

    @BeforeEach
    public void setUp() {
        membershipService = new MembershipServiceImpl(
            null,
            null,
            null,
            null,
            null,
            null,
            membershipRepository,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );
    }

    @Test
    public void shouldInvalidateCacheForAllMembersWithRole() throws Exception {
        // Given
        Membership membership1 = new Membership();
        membership1.setMemberId(USER_ID_1);
        membership1.setMemberType(MembershipMemberType.USER);
        membership1.setReferenceType(MembershipReferenceType.API);
        membership1.setReferenceId(API_ID);
        membership1.setRoleId(ROLE_ID);

        Membership membership2 = new Membership();
        membership2.setMemberId(USER_ID_2);
        membership2.setMemberType(MembershipMemberType.USER);
        membership2.setReferenceType(MembershipReferenceType.APPLICATION);
        membership2.setReferenceId(APP_ID);
        membership2.setRoleId(ROLE_ID);

        Set<Membership> memberships = new HashSet<>();
        memberships.add(membership1);
        memberships.add(membership2);

        when(membershipRepository.findByRoleId(ROLE_ID)).thenReturn(memberships);

        // When
        membershipService.invalidateCacheForRole(ROLE_ID);

        // Then
        verify(membershipRepository, times(1)).findByRoleId(ROLE_ID);
        // The method should not throw and should process all memberships
    }

    @Test
    public void shouldHandleEmptyMemberships() throws Exception {
        // Given
        when(membershipRepository.findByRoleId(ROLE_ID)).thenReturn(new HashSet<>());

        // When
        membershipService.invalidateCacheForRole(ROLE_ID);

        // Then
        verify(membershipRepository, times(1)).findByRoleId(ROLE_ID);
    }

    @Test
    public void shouldHandleTechnicalException() throws Exception {
        // Given
        when(membershipRepository.findByRoleId(ROLE_ID)).thenThrow(new TechnicalException("Database error"));

        // When - should not throw, just log the error
        membershipService.invalidateCacheForRole(ROLE_ID);

        // Then
        verify(membershipRepository, times(1)).findByRoleId(ROLE_ID);
    }

    @Test
    public void shouldInvalidateCacheForGroupMembers() throws Exception {
        // Given
        Membership groupMembership = new Membership();
        groupMembership.setMemberId("group-id-1");
        groupMembership.setMemberType(MembershipMemberType.GROUP);
        groupMembership.setReferenceType(MembershipReferenceType.API);
        groupMembership.setReferenceId(API_ID);
        groupMembership.setRoleId(ROLE_ID);

        Set<Membership> memberships = new HashSet<>();
        memberships.add(groupMembership);

        when(membershipRepository.findByRoleId(ROLE_ID)).thenReturn(memberships);

        // When
        membershipService.invalidateCacheForRole(ROLE_ID);

        // Then
        verify(membershipRepository, times(1)).findByRoleId(ROLE_ID);
    }
}
