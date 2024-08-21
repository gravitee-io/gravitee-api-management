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

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.isA;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Sets;
import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.Application;
import io.gravitee.repository.management.model.Membership;
import io.gravitee.repository.management.model.MembershipMemberType;
import io.gravitee.repository.management.model.MembershipReferenceType;
import io.gravitee.rest.api.model.GroupEntity;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.UserMembership;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
public class MembershipService_FindUserMembershipTest {

    private static final String USER_ID = "john-doe";

    private MembershipService membershipService;

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

    @BeforeEach
    public void setUp() throws Exception {
        membershipService =
            new MembershipServiceImpl(
                null,
                null,
                mockApplicationRepository,
                null,
                null,
                null,
                mockMembershipRepository,
                mockRoleService,
                null,
                null,
                null,
                null,
                mockApiRepository,
                mockGroupService,
                null,
                null,
                null
            );
    }

    @Test
    public void shouldGetEmptyResultForEnvironmentType() {
        List<UserMembership> references = membershipService.findUserMembership(
            GraviteeContext.getExecutionContext(),
            io.gravitee.rest.api.model.MembershipReferenceType.ENVIRONMENT,
            USER_ID
        );

        assertThat(references).isEmpty();
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

        verify(mockApiRepository, never()).searchIds(any(), any(), any());

        assertThat(references).isEmpty();
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

        assertThat(references)
            .hasSize(1)
            .extracting(UserMembership::getReference, UserMembership::getType)
            .containsExactly(tuple("api-id1", "API"));
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
        when(mockGroupService.findByUser(eq(USER_ID))).thenReturn(Set.of(group1));

        when(
            mockApiRepository.searchIds(eq(List.of((new ApiCriteria.Builder().groups("Group1Id").build()))), isA(Pageable.class), isNull())
        )
            .thenReturn(new Page<>(List.of("apiGroup1Id"), 0, 1, 1));

        List<UserMembership> references = membershipService.findUserMembership(
            GraviteeContext.getExecutionContext(),
            io.gravitee.rest.api.model.MembershipReferenceType.API,
            USER_ID
        );

        assertThat(references)
            .hasSize(1)
            .extracting(UserMembership::getReference, UserMembership::getType)
            .containsExactly(tuple("apiGroup1Id", "API"));
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
        when(mockGroupService.findByUser(eq(USER_ID))).thenReturn(Set.of(group1));

        when(
            mockApiRepository.searchIds(eq(List.of((new ApiCriteria.Builder().groups("Group1Id").build()))), isA(Pageable.class), isNull())
        )
            .thenReturn(new Page<>(List.of("apiGroup1Id"), 0, 1, 1));

        List<UserMembership> references = membershipService.findUserMembership(
            GraviteeContext.getExecutionContext(),
            io.gravitee.rest.api.model.MembershipReferenceType.API,
            USER_ID
        );

        assertThat(references)
            .hasSize(2)
            .extracting(UserMembership::getReference, UserMembership::getType)
            .containsExactly(tuple("api-id1", "API"), tuple("apiGroup1Id", "API"));
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

        assertThat(references)
            .hasSize(2)
            .extracting(UserMembership::getReference, UserMembership::getType)
            .containsExactly(tuple("app-id1", "GROUP"), tuple("api-id1", "GROUP"));
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
        when(mockGroupService.findByUser(eq(USER_ID))).thenReturn(Set.of(group1));

        Application application = new Application();
        application.setId("applicationGroup1Id");
        when(mockApplicationRepository.findByGroups(eq(List.of("Group1Id")))).thenReturn(Set.of(application));

        List<UserMembership> references = membershipService.findUserMembership(
            GraviteeContext.getExecutionContext(),
            io.gravitee.rest.api.model.MembershipReferenceType.APPLICATION,
            USER_ID
        );

        assertThat(references)
            .hasSize(1)
            .extracting(UserMembership::getReference, UserMembership::getType)
            .containsExactly(tuple("applicationGroup1Id", "APPLICATION"));
    }

    @Test
    public void shouldGetEmptyResultIfNoApplicationNorGroups() throws Exception {
        when(mockRoleService.findByScope(any(), any())).thenReturn(Collections.emptyList());
        when(
            mockMembershipRepository.findByMemberIdAndMemberTypeAndReferenceType(
                eq(USER_ID),
                eq(MembershipMemberType.USER),
                eq(MembershipReferenceType.APPLICATION)
            )
        )
            .thenReturn(Collections.emptySet());

        doReturn(Collections.emptySet()).when(mockGroupService).findByUser(USER_ID);

        List<UserMembership> references = membershipService.findUserMembership(
            GraviteeContext.getExecutionContext(),
            io.gravitee.rest.api.model.MembershipReferenceType.APPLICATION,
            USER_ID
        );

        verify(mockApplicationRepository, never()).findByGroups(anyList());

        assertThat(references).isEmpty();
    }
}
