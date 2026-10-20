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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.api.ClusterRepository;
import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.model.Cluster;
import io.gravitee.rest.api.model.MemberEntity;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ApiNotFoundException;
import io.gravitee.rest.api.service.exceptions.ApplicationNotFoundException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
public class MembershipService_GetUserMemberTest {

    private MembershipService cut;

    @Mock
    private MembershipRepository membershipRepository;

    @Mock
    private ApiRepository apiRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private ClusterRepository clusterRepository;

    @Mock
    private UserService userService;

    @Mock
    private RoleService roleService;

    @BeforeEach
    public void setUp() throws Exception {
        cut = new MembershipServiceImpl(
            null,
            userService,
            applicationRepository,
            null,
            null,
            null,
            membershipRepository,
            roleService,
            null,
            null,
            null,
            null,
            apiRepository,
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
            clusterRepository
        );
    }

    @Test
    public void should_throw_if_no_api_found() throws TechnicalException {
        when(membershipRepository.findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId(any(), any(), any(), any())).thenReturn(
            Set.of()
        );
        when(apiRepository.findById("reference-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
            cut.getUserMember(GraviteeContext.getExecutionContext(), MembershipReferenceType.API, "reference-id", "user-id")
        ).isInstanceOf(ApiNotFoundException.class);
    }

    @Test
    public void should_throw_if_no_app_found() throws TechnicalException {
        when(membershipRepository.findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId(any(), any(), any(), any())).thenReturn(
            Set.of()
        );
        when(applicationRepository.findById("reference-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
            cut.getUserMember(GraviteeContext.getExecutionContext(), MembershipReferenceType.APPLICATION, "reference-id", "user-id")
        ).isInstanceOf(ApplicationNotFoundException.class);
    }

    @Test
    public void should_grant_cluster_role_to_user_member_of_group_attached_to_cluster() throws TechnicalException {
        givenGroupMemberWithClusterRole(Set.of("group-id"));

        MemberEntity member = cut.getUserMember(
            GraviteeContext.getExecutionContext(),
            MembershipReferenceType.CLUSTER,
            "cluster-id",
            "user-id"
        );

        assertThat(member).isNotNull();
        assertThat(member.getPermissions()).containsKey("DEFINITION");
    }

    @Test
    public void should_not_grant_cluster_role_when_group_is_not_attached_to_cluster() throws TechnicalException {
        givenGroupMemberWithClusterRole(Set.of("other-group-id"));

        MemberEntity member = cut.getUserMember(
            GraviteeContext.getExecutionContext(),
            MembershipReferenceType.CLUSTER,
            "cluster-id",
            "user-id"
        );

        assertThat(member).isNotNull();
        assertThat(member.getPermissions()).isEmpty();
    }

    private void givenGroupMemberWithClusterRole(Set<String> clusterGroups) throws TechnicalException {
        io.gravitee.repository.management.model.Membership groupMembership = new io.gravitee.repository.management.model.Membership();
        groupMembership.setReferenceType(io.gravitee.repository.management.model.MembershipReferenceType.GROUP);
        groupMembership.setReferenceId("group-id");
        groupMembership.setRoleId("role-id");

        Cluster cluster = new Cluster();
        cluster.setId("cluster-id");
        cluster.setGroups(clusterGroups);
        when(clusterRepository.findById("cluster-id")).thenReturn(Optional.of(cluster));
        when(
            membershipRepository.findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId(any(), any(), any(), eq("cluster-id"))
        ).thenReturn(Set.of());
        when(userService.findById(any(), eq("user-id"))).thenReturn(new UserEntity());

        RoleEntity role = new RoleEntity();
        role.setId("role-id");
        role.setScope(RoleScope.CLUSTER);
        role.setPermissions(Map.of("DEFINITION", new char[] { 'R' }));
        if (clusterGroups.contains("group-id")) {
            when(
                membershipRepository.findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceIds(any(), any(), any(), eq(Set.of("group-id")))
            ).thenReturn(Set.of(groupMembership));
            when(roleService.findByIds(Set.of("role-id"))).thenReturn(Map.of("role-id", role));
        }
    }
}
