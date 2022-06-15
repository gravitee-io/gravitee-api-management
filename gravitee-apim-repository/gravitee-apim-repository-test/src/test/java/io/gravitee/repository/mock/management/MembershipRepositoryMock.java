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
package io.gravitee.repository.mock.management;

import static io.gravitee.repository.management.model.MembershipReferenceType.API;
import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;

import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.model.Membership;
import io.gravitee.repository.management.model.MembershipMemberType;
import io.gravitee.repository.management.model.MembershipReferenceType;
import io.gravitee.repository.mock.AbstractRepositoryMock;
import java.util.Date;
import java.util.HashSet;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class MembershipRepositoryMock extends AbstractRepositoryMock<MembershipRepository> {

    public MembershipRepositoryMock() {
        super(MembershipRepository.class);
    }

    @Override
    protected void prepare(MembershipRepository membershipRepository) throws Exception {
        String API_OWNER_ROLE = "API_OWNER";
        Membership m1 = mock(Membership.class);
        when(m1.getId()).thenReturn("api1_user1");
        when(m1.getMemberId()).thenReturn("user1");
        when(m1.getMemberType()).thenReturn(MembershipMemberType.USER);
        when(m1.getReferenceType()).thenReturn(MembershipReferenceType.API);
        when(m1.getRoleId()).thenReturn(API_OWNER_ROLE);
        when(m1.getReferenceId()).thenReturn("api1");
        when(m1.getCreatedAt()).thenReturn(new Date(1439022010883L));
        when(m1.getUpdatedAt()).thenReturn(new Date(1439022010883L));
        when(m1.getSource()).thenReturn("myIdp");
        Membership m2 = new Membership(
            "api2_user2",
            "user2",
            MembershipMemberType.USER,
            "api2",
            MembershipReferenceType.API,
            API_OWNER_ROLE
        );
        m2.setId("api2_user2");
        Membership m3 = new Membership("api3_user3", "user3", MembershipMemberType.USER, "api3", MembershipReferenceType.API, "API_USER");
        m3.setId("api3_user3");
        Membership m4 = new Membership(
            "app1_userToDelete",
            "userToDelete",
            MembershipMemberType.USER,
            "app1",
            MembershipReferenceType.APPLICATION,
            "APPLICATION_USER"
        );
        m4.setCreatedAt(new Date(1000000000000L));

        when(membershipRepository.findById("api1_user1")).thenReturn(of(m1));
        when(membershipRepository.findById("api1")).thenReturn(empty());
        when(membershipRepository.findById("app1_userToDelete")).thenReturn(empty());
        when(membershipRepository.findByReferenceAndRoleId(eq(MembershipReferenceType.API), eq("api1"), eq(API_OWNER_ROLE)))
            .thenReturn(singleton(m1));
        when(membershipRepository.findByReferenceAndRoleId(eq(MembershipReferenceType.API), eq("api1"), any())).thenReturn(singleton(m1));
        when(
            membershipRepository.findByMemberIdAndMemberTypeAndReferenceType(
                "user1",
                MembershipMemberType.USER,
                MembershipReferenceType.API
            )
        )
            .thenReturn(singleton(m1));
        when(
            membershipRepository.findByMemberIdAndMemberTypeAndReferenceTypeAndSource(
                "user1",
                MembershipMemberType.USER,
                MembershipReferenceType.API,
                "myIdp"
            )
        )
            .thenReturn(singleton(m1));
        when(
            membershipRepository.findByMemberIdsAndMemberTypeAndReferenceType(
                asList("user2", "user3"),
                MembershipMemberType.USER,
                MembershipReferenceType.API
            )
        )
            .thenReturn(new HashSet<>(asList(m2, m3)));
        when(
            membershipRepository.findByMemberIdAndMemberTypeAndReferenceTypeAndRoleId(
                "user1",
                MembershipMemberType.USER,
                MembershipReferenceType.API,
                API_OWNER_ROLE
            )
        )
            .thenReturn(singleton(m1));

        when(
            membershipRepository.findByMemberIdAndMemberTypeAndReferenceTypeAndRoleIdIn(
                eq("user1"),
                eq(MembershipMemberType.USER),
                eq(MembershipReferenceType.API),
                argThat(roleIds -> roleIds.contains(API_OWNER_ROLE))
            )
        )
            .thenReturn(singleton(m1));

        when(
            membershipRepository.findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId(
                "user1",
                MembershipMemberType.USER,
                MembershipReferenceType.API,
                "api1"
            )
        )
            .thenReturn(singleton(m1));
        when(
            membershipRepository.findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceIdAndRoleId(
                "user1",
                MembershipMemberType.USER,
                MembershipReferenceType.API,
                "api1",
                API_OWNER_ROLE
            )
        )
            .thenReturn(singleton(m1));
        when(membershipRepository.findByReferencesAndRoleId(MembershipReferenceType.API, asList("api2", "api3"), API_OWNER_ROLE))
            .thenReturn(new HashSet<>(singletonList(m2)));
        when(membershipRepository.findByReferencesAndRoleId(MembershipReferenceType.API, asList("api2", "api3"), null))
            .thenReturn(new HashSet<>(asList(m2, m3)));
        when(membershipRepository.update(any())).thenReturn(m4);

        Membership api1_findByIds = mock(Membership.class);
        when(api1_findByIds.getId()).thenReturn("api1_user_findByIds");
        Membership api2_findByIds = mock(Membership.class);
        when(api2_findByIds.getId()).thenReturn("api2_user_findByIds");
        when(membershipRepository.findByIds(new HashSet<>(asList("api1_user_findByIds", "api2_user_findByIds", "unknown"))))
            .thenReturn(new HashSet<>(asList(api1_findByIds, api2_findByIds)));
        when(membershipRepository.findByMemberIdAndMemberType("user_findByIds", MembershipMemberType.USER))
            .thenReturn(new HashSet<>(asList(api1_findByIds, api2_findByIds)));

        when(membershipRepository.update(argThat(o -> o == null || o.getId().equals("unknown")))).thenThrow(new IllegalStateException());

        when(membershipRepository.findByRoleId("APPLICATION_USER"))
            .thenReturn(new HashSet<>(asList(mock(Membership.class), mock(Membership.class))));

        when(
            membershipRepository.findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId(
                "user_deleteRef_1",
                MembershipMemberType.USER,
                API,
                "api_deleteRef"
            )
        )
            .thenReturn(singleton(mock(Membership.class)), emptySet());
        when(
            membershipRepository.findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId(
                "user_deleteRef_2",
                MembershipMemberType.USER,
                API,
                "api_deleteRef"
            )
        )
            .thenReturn(singleton(mock(Membership.class)), emptySet());
    }
}
