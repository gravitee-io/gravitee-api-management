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
package io.gravitee.repository.config.mock;

import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.model.Membership;
import io.gravitee.repository.management.model.MembershipReferenceType;
import io.gravitee.repository.management.model.RoleScope;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class MembershipRepositoryMock extends AbstractRepositoryMock<MembershipRepository> {

    public MembershipRepositoryMock() {
        super(MembershipRepository.class);
    }

    @Override
    void prepare(MembershipRepository membershipRepository) throws Exception {
        Map<Integer, String> API_OWNER_ROLE_MAP = Collections.singletonMap(RoleScope.API.getId(), "OWNER");
        Membership m1 = mock(Membership.class);
        when(m1.getUserId()).thenReturn("user1");
        when(m1.getReferenceType()).thenReturn(MembershipReferenceType.API);
        when(m1.getRoles()).thenReturn(API_OWNER_ROLE_MAP);
        when(m1.getReferenceId()).thenReturn("api1");
        Membership m2 = new Membership("user2", "api2", MembershipReferenceType.API);
        m2.setRoles(API_OWNER_ROLE_MAP);
        Membership m3 = new Membership("user3", "api3", MembershipReferenceType.API);
        m3.setRoles(API_OWNER_ROLE_MAP);
        Membership m4 = new Membership("userToDelete", "app1", MembershipReferenceType.APPLICATION);
        m4.setCreatedAt(new Date(1000000000000L));

        when(membershipRepository.findById("user1", MembershipReferenceType.API, "api1"))
                .thenReturn(of(m1));
        when(membershipRepository.findById(null, MembershipReferenceType.API, "api1"))
                .thenReturn(empty());
        when(membershipRepository.findById("userToDelete", MembershipReferenceType.APPLICATION, "app1"))
                .thenReturn(empty());
        when(membershipRepository.findByReferenceAndRole(eq(MembershipReferenceType.API), eq("api1"), eq(RoleScope.API), any()))
                .thenReturn(singleton(m1));
        when(membershipRepository.findByReferenceAndRole(eq(MembershipReferenceType.API), eq("api1"), eq(null), any()))
                .thenReturn(singleton(m1));
        when(membershipRepository.findByUserAndReferenceType("user1", MembershipReferenceType.API))
                .thenReturn(singleton(m1));
        when(membershipRepository.findByUserAndReferenceTypeAndRole("user1", MembershipReferenceType.API, RoleScope.API, "OWNER"))
                .thenReturn(singleton(m1));
        when(membershipRepository.findByReferencesAndRole(MembershipReferenceType.API, asList("api2", "api3"), null, null))
                .thenReturn(new HashSet<>(asList(m2, m3)));
        when(membershipRepository.findByReferencesAndRole(MembershipReferenceType.API, asList("api2", "api3"), RoleScope.API, "OWNER"))
                .thenReturn(new HashSet<>(singletonList(m2)));
        when(membershipRepository.update(any())).thenReturn(m4);

        Membership api1_findByIds = mock(Membership.class);
        when(api1_findByIds.getReferenceId()).thenReturn("api1_findByIds");
        Membership api2_findByIds = mock(Membership.class);
        when(api2_findByIds.getReferenceId()).thenReturn("api2_findByIds");
        when(membershipRepository.findByIds(
                "user_findByIds",
                MembershipReferenceType.API,
                new HashSet<>(asList("api1_findByIds", "api2_findByIds", "unknown")))).
                thenReturn(new HashSet<>(asList(api1_findByIds, api2_findByIds)));
        when(membershipRepository.findByUser("user_findByIds")).
                thenReturn(new HashSet<>(asList(api1_findByIds, api2_findByIds)));

        when(membershipRepository.update(argThat(o -> o == null || o.getReferenceId().equals("unknown")))).thenThrow(new IllegalStateException());

        when(membershipRepository.findByRole(RoleScope.APPLICATION, "USER")).thenReturn(new HashSet<>(asList(mock(Membership.class), mock(Membership.class))));
    }
}
