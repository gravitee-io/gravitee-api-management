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

import static org.mockito.Mockito.*;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.model.Membership;
import io.gravitee.repository.management.model.MembershipMemberType;
import io.gravitee.repository.management.model.MembershipReferenceType;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.*;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class MembershipService_GetMembershipsTest {

    private static final String API_ID = "api-id-1";

    @InjectMocks
    private MembershipService membershipService = new MembershipServiceImpl();

    @Mock
    private MembershipRepository membershipRepository;

    private String memberId = "memberId";

    @Test
    public void shouldGetEmptyMembersWithMembership() throws Exception {
        when(
            membershipRepository.findByMemberIdAndMemberTypeAndReferenceType(
                memberId,
                MembershipMemberType.USER,
                MembershipReferenceType.APPLICATION
            )
        )
            .thenReturn(Collections.emptySet());

        Set<String> referenceIds = membershipService.getReferenceIdsByMemberAndReference(
            io.gravitee.rest.api.model.MembershipMemberType.USER,
            memberId,
            io.gravitee.rest.api.model.MembershipReferenceType.APPLICATION
        );

        Assert.assertNotNull(referenceIds);
        Assert.assertTrue("references must be empty", referenceIds.isEmpty());
        verify(membershipRepository, times(1))
            .findByMemberIdAndMemberTypeAndReferenceType(memberId, MembershipMemberType.USER, MembershipReferenceType.APPLICATION);
    }

    @Test
    public void shouldGetMembersWithMembership() throws Exception {
        Membership m1 = mock(Membership.class);
        when(m1.getReferenceId()).thenReturn("ref-1");
        Membership m2 = mock(Membership.class);
        when(m2.getReferenceId()).thenReturn("m2");
        when(
            membershipRepository.findByMemberIdAndMemberTypeAndReferenceType(
                memberId,
                MembershipMemberType.USER,
                MembershipReferenceType.APPLICATION
            )
        )
            .thenReturn(Set.of(m1, m2));

        Set<String> referenceIds = membershipService.getReferenceIdsByMemberAndReference(
            io.gravitee.rest.api.model.MembershipMemberType.USER,
            memberId,
            io.gravitee.rest.api.model.MembershipReferenceType.APPLICATION
        );

        Assert.assertNotNull(referenceIds);
        Assert.assertEquals(2, referenceIds.size());
        verify(membershipRepository, times(1))
            .findByMemberIdAndMemberTypeAndReferenceType(memberId, MembershipMemberType.USER, MembershipReferenceType.APPLICATION);
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldThrowTechnicalManagementException() throws Exception {
        when(
            membershipRepository.findByMemberIdAndMemberTypeAndReferenceType(
                memberId,
                MembershipMemberType.USER,
                MembershipReferenceType.APPLICATION
            )
        )
            .thenThrow(new TechnicalException());

        membershipService.getReferenceIdsByMemberAndReference(
            io.gravitee.rest.api.model.MembershipMemberType.USER,
            memberId,
            io.gravitee.rest.api.model.MembershipReferenceType.APPLICATION
        );
    }
}
