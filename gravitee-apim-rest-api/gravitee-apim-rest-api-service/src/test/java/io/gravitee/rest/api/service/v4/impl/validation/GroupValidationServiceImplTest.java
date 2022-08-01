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
package io.gravitee.rest.api.service.v4.impl.validation;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.repository.management.model.GroupEvent;
import io.gravitee.rest.api.model.GroupEntity;
import io.gravitee.rest.api.model.MembershipEntity;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.GroupsNotFoundException;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import io.gravitee.rest.api.service.v4.validation.GroupValidationService;
import java.util.List;
import java.util.Set;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class GroupValidationServiceImplTest {

    @Mock
    private GroupService groupService;

    @Mock
    private MembershipService membershipService;

    private GroupValidationService groupValidationService;

    @Before
    public void setUp() throws Exception {
        groupValidationService = new GroupValidationServiceImpl(groupService, membershipService);
    }

    @Test
    public void shouldReturnValidatedGroupsWithNoApiAndExistingGroupWithoutApiPrimaryOwnerAndCurrentUserPrimaryOwner() {
        // Given
        String groupId = "group";
        Set<String> groups = Set.of(groupId);
        GroupEntity groupEntity = new GroupEntity();
        groupEntity.setId(groupId);
        when(groupService.findByIds(groups)).thenReturn(Set.of(groupEntity));
        ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        GroupEntity defaultGroupEntity = new GroupEntity();
        String defaultGroup = "default";
        defaultGroupEntity.setId(defaultGroup);
        when(groupService.findByEvent(executionContext.getEnvironmentId(), GroupEvent.API_CREATE)).thenReturn(Set.of(defaultGroupEntity));

        // When
        Set<String> validatedGroups = groupValidationService.validateAndSanitize(
            executionContext,
            null,
            groups,
            new PrimaryOwnerEntity(new UserEntity())
        );

        // Then
        assertThat(validatedGroups).isNotNull();
        assertThat(validatedGroups.size()).isEqualTo(2);
        assertThat(validatedGroups.containsAll(Set.of(groupId, defaultGroup))).isTrue();
    }

    @Test
    public void shouldReturnValidatedGroupsWithNoApiAndExistingGroupWithoutApiPrimaryOwnerAndCurrentGroupPrimaryOwner() {
        // Given
        String groupId = "group";
        Set<String> groups = Set.of(groupId);
        GroupEntity groupEntity = new GroupEntity();
        groupEntity.setId(groupId);
        when(groupService.findByIds(groups)).thenReturn(Set.of(groupEntity));
        ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        GroupEntity defaultGroupEntity = new GroupEntity();
        String defaultGroup = "default";
        defaultGroupEntity.setId(defaultGroup);
        when(groupService.findByEvent(executionContext.getEnvironmentId(), GroupEvent.API_CREATE)).thenReturn(Set.of(defaultGroupEntity));

        GroupEntity currentPrimaryOwner = new GroupEntity();
        String currentGroupId = "current";
        currentPrimaryOwner.setId(currentGroupId);
        // When
        Set<String> validatedGroups = groupValidationService.validateAndSanitize(
            executionContext,
            null,
            groups,
            new PrimaryOwnerEntity(currentPrimaryOwner)
        );

        // Then
        assertThat(validatedGroups).isNotNull();
        assertThat(validatedGroups.size()).isEqualTo(3);
        assertThat(validatedGroups.containsAll(Set.of(groupId, defaultGroup, currentGroupId))).isTrue();
    }

    @Test
    public void shouldReturnFilteredGroupsWithNoApiAndExistingGroupWithApiPrimaryOwner() {
        // Given
        String groupId = "group";
        Set<String> groups = Set.of(groupId);
        GroupEntity groupEntity = new GroupEntity();
        groupEntity.setId(groupId);
        groupEntity.setApiPrimaryOwner("api");
        when(groupService.findByIds(groups)).thenReturn(Set.of(groupEntity));
        ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        GroupEntity defaultGroupEntity = new GroupEntity();
        String defaultGroup = "default";
        defaultGroupEntity.setId(defaultGroup);
        when(groupService.findByEvent(executionContext.getEnvironmentId(), GroupEvent.API_CREATE)).thenReturn(Set.of(defaultGroupEntity));

        // When
        Set<String> validatedGroups = groupValidationService.validateAndSanitize(
            executionContext,
            null,
            groups,
            new PrimaryOwnerEntity(new UserEntity())
        );

        // Then
        assertThat(validatedGroups).isNotNull();
        assertThat(validatedGroups.size()).isEqualTo(1);
        assertThat(validatedGroups.contains(defaultGroup)).isTrue();
    }

    @Test
    public void shouldReturnValidatedGroupsWithApiAndExistingGroupsWithoutApiPrimaryOwnerAndGroupPrimaryOwner() {
        // Given
        ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        String apiId = "apiId";
        String groupId = "group";
        Set<String> groups = Set.of(groupId);
        GroupEntity groupEntity = new GroupEntity();
        groupEntity.setId(groupId);
        when(groupService.findByIds(groups)).thenReturn(Set.of(groupEntity));

        MembershipEntity membershipEntity = new MembershipEntity();
        membershipEntity.setMemberType(MembershipMemberType.GROUP);
        membershipEntity.setMemberId("group");
        when(membershipService.getPrimaryOwner(executionContext.getOrganizationId(), MembershipReferenceType.API, apiId))
            .thenReturn(membershipEntity);

        GroupEntity defaultGroupEntity = new GroupEntity();
        String defaultGroup = "default";
        defaultGroupEntity.setId(defaultGroup);
        when(groupService.findByEvent(executionContext.getEnvironmentId(), GroupEvent.API_CREATE)).thenReturn(Set.of(defaultGroupEntity));

        // When
        Set<String> validatedGroups = groupValidationService.validateAndSanitize(
            executionContext,
            apiId,
            groups,
            new PrimaryOwnerEntity(new UserEntity())
        );

        // Then
        assertThat(validatedGroups).isNotNull();
        assertThat(validatedGroups.size()).isEqualTo(2);
        assertThat(validatedGroups.containsAll(Set.of(groupId, defaultGroup))).isTrue();
    }

    @Test
    public void shouldReturnValidatedGroupsWithApiAndExistingGroupsWithApiPrimaryOwnerAndGroupPrimaryOwner() {
        // Given
        ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        String apiId = "apiId";
        String groupId = "group";
        Set<String> groups = Set.of(groupId);
        GroupEntity groupEntity = new GroupEntity();
        groupEntity.setId(groupId);
        groupEntity.setApiPrimaryOwner(apiId);
        when(groupService.findByIds(groups)).thenReturn(Set.of(groupEntity));

        MembershipEntity membershipEntity = new MembershipEntity();
        membershipEntity.setMemberType(MembershipMemberType.GROUP);
        membershipEntity.setMemberId("group");
        when(membershipService.getPrimaryOwner(executionContext.getOrganizationId(), MembershipReferenceType.API, apiId))
            .thenReturn(membershipEntity);

        GroupEntity defaultGroupEntity = new GroupEntity();
        String defaultGroup = "default";
        defaultGroupEntity.setId(defaultGroup);
        when(groupService.findByEvent(executionContext.getEnvironmentId(), GroupEvent.API_CREATE)).thenReturn(Set.of(defaultGroupEntity));

        // When
        Set<String> validatedGroups = groupValidationService.validateAndSanitize(
            executionContext,
            apiId,
            groups,
            new PrimaryOwnerEntity(new UserEntity())
        );

        // Then
        assertThat(validatedGroups).isNotNull();
        assertThat(validatedGroups.size()).isEqualTo(2);
        assertThat(validatedGroups.containsAll(Set.of(groupId, defaultGroup))).isTrue();
    }

    @Test
    public void shouldReturnValidatedGroupsWithApiAndExistingGroupsWithoutApiPrimaryOwnerAndUserPrimaryOwner() {
        // Given
        ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        String apiId = "apiId";
        String groupId = "group";
        Set<String> groups = Set.of(groupId);
        GroupEntity groupEntity = new GroupEntity();
        groupEntity.setId(groupId);
        when(groupService.findByIds(groups)).thenReturn(Set.of(groupEntity));

        MembershipEntity membershipEntity = new MembershipEntity();
        membershipEntity.setMemberType(MembershipMemberType.USER);
        membershipEntity.setMemberId("user");
        when(membershipService.getPrimaryOwner(executionContext.getOrganizationId(), MembershipReferenceType.API, apiId))
            .thenReturn(membershipEntity);

        GroupEntity defaultGroupEntity = new GroupEntity();
        String defaultGroup = "default";
        defaultGroupEntity.setId(defaultGroup);
        when(groupService.findByEvent(executionContext.getEnvironmentId(), GroupEvent.API_CREATE)).thenReturn(Set.of(defaultGroupEntity));

        // When
        Set<String> validatedGroups = groupValidationService.validateAndSanitize(
            executionContext,
            apiId,
            groups,
            new PrimaryOwnerEntity(new UserEntity())
        );

        // Then
        assertThat(validatedGroups).isNotNull();
        assertThat(validatedGroups.size()).isEqualTo(2);
        assertThat(validatedGroups.containsAll(Set.of(groupId, defaultGroup))).isTrue();
    }

    @Test
    public void shouldReturnFilteredGroupsWithApiAndExistingGroupsWithApiPrimaryOwnerAndUserPrimaryOwner() {
        // Given
        ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        String apiId = "apiId";
        String groupId = "group";
        Set<String> groups = Set.of(groupId);
        GroupEntity groupEntity = new GroupEntity();
        groupEntity.setId(groupId);
        groupEntity.setApiPrimaryOwner(apiId);
        when(groupService.findByIds(groups)).thenReturn(Set.of(groupEntity));

        MembershipEntity membershipEntity = new MembershipEntity();
        membershipEntity.setMemberType(MembershipMemberType.USER);
        membershipEntity.setMemberId("user");
        when(membershipService.getPrimaryOwner(executionContext.getOrganizationId(), MembershipReferenceType.API, apiId))
            .thenReturn(membershipEntity);

        GroupEntity defaultGroupEntity = new GroupEntity();
        String defaultGroup = "default";
        defaultGroupEntity.setId(defaultGroup);
        when(groupService.findByEvent(executionContext.getEnvironmentId(), GroupEvent.API_CREATE)).thenReturn(Set.of(defaultGroupEntity));

        // When
        Set<String> validatedGroups = groupValidationService.validateAndSanitize(
            executionContext,
            apiId,
            groups,
            new PrimaryOwnerEntity(new UserEntity())
        );

        // Then
        assertThat(validatedGroups).isNotNull();
        assertThat(validatedGroups.size()).isEqualTo(1);
        assertThat(validatedGroups.contains(defaultGroup)).isTrue();
    }

    @Test
    public void shouldReturnFilteredGroupsWithApiAndExistingGroupsWithApiPrimaryOwnerAndGroupPrimaryOwner() {
        // Given
        ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        String apiId = "apiId";
        String groupId = "group";
        Set<String> groups = Set.of(groupId);
        GroupEntity groupEntity = new GroupEntity();
        groupEntity.setId(groupId);
        groupEntity.setApiPrimaryOwner(apiId);
        when(groupService.findByIds(groups)).thenReturn(Set.of(groupEntity));

        MembershipEntity membershipEntity = new MembershipEntity();
        membershipEntity.setMemberType(MembershipMemberType.USER);
        membershipEntity.setMemberId("user");
        when(membershipService.getPrimaryOwner(executionContext.getOrganizationId(), MembershipReferenceType.API, apiId))
            .thenReturn(membershipEntity);

        GroupEntity defaultGroupEntity = new GroupEntity();
        String defaultGroup = "default";
        defaultGroupEntity.setId(defaultGroup);
        when(groupService.findByEvent(executionContext.getEnvironmentId(), GroupEvent.API_CREATE)).thenReturn(Set.of(defaultGroupEntity));

        GroupEntity currentPrimaryOwner = new GroupEntity();
        String currentGroupId = "current";
        currentPrimaryOwner.setId(currentGroupId);
        // When
        Set<String> validatedGroups = groupValidationService.validateAndSanitize(
            executionContext,
            apiId,
            groups,
            new PrimaryOwnerEntity(currentPrimaryOwner)
        );

        // Then
        assertThat(validatedGroups).isNotNull();
        assertThat(validatedGroups.size()).isEqualTo(2);
        assertThat(validatedGroups.containsAll(Set.of(currentGroupId, defaultGroup))).isTrue();
    }

    @Test
    public void shouldThrowExceptionWithNotFoundGroups() {
        // Given
        String groupId = "group";
        Set<String> groups = Set.of(groupId);
        when(groupService.findByIds(groups)).thenThrow(new GroupsNotFoundException(Set.of(groupId)));

        // When
        assertThatExceptionOfType(InvalidDataException.class)
            .isThrownBy(
                () ->
                    groupValidationService.validateAndSanitize(
                        GraviteeContext.getExecutionContext(),
                        null,
                        groups,
                        new PrimaryOwnerEntity(new UserEntity())
                    )
            );
    }

    @Test
    public void shouldIgnoreEmptyList() {
        Set<String> emptyGroups = Set.of();
        Set<String> validatedGroups = groupValidationService.validateAndSanitize(
            GraviteeContext.getExecutionContext(),
            null,
            emptyGroups,
            null
        );
        Assertions.assertThat(validatedGroups).isEmpty();
        Assertions.assertThat(validatedGroups).isEqualTo(emptyGroups);
    }
}
