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
import static org.mockito.Mockito.when;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.GroupRepository;
import io.gravitee.repository.management.model.Group;
import io.gravitee.rest.api.model.GroupEntity;
import io.gravitee.rest.api.model.MembershipEntity;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.service.MembershipService;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class GroupService_FindByUserAndEnvironmentTest {

    @InjectMocks
    private final GroupServiceImpl groupService = new GroupServiceImpl();

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private MembershipService membershipService;

    @Test
    public void shouldReturnGroupsForUserInEnvironment() throws TechnicalException {
        String userId = "user1";
        String envId = "env1";
        Group group1 = Group.builder().id("g1").environmentId(envId).name("Group1").build();
        Group group2 = Group.builder().id("g2").environmentId("env2").name("Group2").build();
        Group group3 = Group.builder().id("g3").environmentId(envId).name("Group3").build();
        Set<String> userGroups = new HashSet<>(Arrays.asList("g1", "g2", "g3"));
        when(
            membershipService.getMembershipsByMemberAndReference(MembershipMemberType.USER, userId, MembershipReferenceType.GROUP)
        ).thenReturn(
            new HashSet<>(
                Arrays.asList(
                    MembershipEntity.builder().id("m1").referenceId("g1").build(),
                    MembershipEntity.builder().id("m2").referenceId("g2").build(),
                    MembershipEntity.builder().id("m3").referenceId("g3").build()
                )
            )
        );
        when(groupRepository.findByIds(userGroups)).thenReturn(Set.of(group1, group2, group3));
        Set<GroupEntity> result = groupService.findByUserAndEnvironment(userId, envId);
        assertThat(result).hasSize(2);
        assertThat(result.stream().map(GroupEntity::getName)).containsExactlyInAnyOrder("Group1", "Group3");
    }

    @Test
    public void shouldReturnEmptyForUserWithNoGroupsInEnv() throws TechnicalException {
        String userId = "user2";
        String envId = "env1";
        Group group1 = Group.builder().id("g1").environmentId("env2").name("Group2").build();
        Set<String> userGroups = Collections.singleton("g1");
        when(
            membershipService.getMembershipsByMemberAndReference(MembershipMemberType.USER, userId, MembershipReferenceType.GROUP)
        ).thenReturn(new HashSet<>(Collections.singletonList(MembershipEntity.builder().id("m4").referenceId("g1").build())));
        when(groupRepository.findByIds(userGroups)).thenReturn(Set.of(group1));
        Set<GroupEntity> result = groupService.findByUserAndEnvironment(userId, envId);
        assertThat(result).isEmpty();
    }

    @Test
    public void shouldReturnEmptyForNonExistentUserOrEnv() throws TechnicalException {
        String userId = "nouser";
        String envId = "noenv";
        when(
            membershipService.getMembershipsByMemberAndReference(MembershipMemberType.USER, userId, MembershipReferenceType.GROUP)
        ).thenReturn(new HashSet<>());
        when(groupRepository.findByIds(Collections.emptySet())).thenReturn(Set.of());
        Set<GroupEntity> result = groupService.findByUserAndEnvironment(userId, envId);
        assertThat(result).isEmpty();
    }

    @Test
    public void shouldReturnAllGroupsIfEnvIdIsNull() throws TechnicalException {
        String userId = "user3";
        Group group1 = Group.builder().id("g1").environmentId("env1").name("Group1").build();
        Group group2 = Group.builder().id("g2").environmentId("env2").name("Group2").build();
        Set<String> userGroups = new HashSet<>(Arrays.asList("g1", "g2"));
        when(
            membershipService.getMembershipsByMemberAndReference(MembershipMemberType.USER, userId, MembershipReferenceType.GROUP)
        ).thenReturn(
            new HashSet<>(
                Arrays.asList(
                    MembershipEntity.builder().id("m5").referenceId("g1").build(),
                    MembershipEntity.builder().id("m6").referenceId("g2").build()
                )
            )
        );
        when(groupRepository.findByIds(userGroups)).thenReturn(Set.of(group1, group2));
        Set<GroupEntity> result = groupService.findByUserAndEnvironment(userId, null);
        assertThat(result).hasSize(2);
        assertThat(result.stream().map(GroupEntity::getName)).containsExactlyInAnyOrder("Group1", "Group2");
    }
}
