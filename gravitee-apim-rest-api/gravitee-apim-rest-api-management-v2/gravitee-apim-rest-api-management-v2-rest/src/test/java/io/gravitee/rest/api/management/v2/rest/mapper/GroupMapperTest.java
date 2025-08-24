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
package io.gravitee.rest.api.management.v2.rest.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.core.group.model.Group.GroupEventRule;
import io.gravitee.rest.api.management.v2.rest.model.Group;
import io.gravitee.rest.api.management.v2.rest.model.GroupEvent;
import io.gravitee.rest.api.model.GroupEntity;
import io.gravitee.rest.api.model.GroupEventRuleEntity;
import io.gravitee.rest.api.model.permissions.RoleScope;
import java.time.ZonedDateTime;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

public class GroupMapperTest extends AbstractMapperTest {

    private final GroupMapper groupMapper = Mappers.getMapper(GroupMapper.class);

    @Test
    void should_map_GroupEntity_to_Group() {
        GroupEntity groupEntity = createGroupEntity();

        Group group = groupMapper.map(groupEntity);

        assertThat(group).isNotNull();
        assertThat(group.getId()).isEqualTo(groupEntity.getId());
        assertThat(group.getName()).isEqualTo(groupEntity.getName());
        assertThat(group.getManageable()).isEqualTo(groupEntity.isManageable());
        assertThat(group.getCreatedAt().toInstant().toEpochMilli()).isEqualTo(groupEntity.getCreatedAt().getTime());
        assertThat(group.getUpdatedAt().toInstant().toEpochMilli()).isEqualTo(groupEntity.getUpdatedAt().getTime());
        assertThat(group.getMaxInvitation()).isEqualTo(groupEntity.getMaxInvitation());
        assertThat(group.getApiRole()).isEqualTo(groupEntity.getRoles().get(RoleScope.API));
        assertThat(group.getApplicationRole()).isEqualTo(groupEntity.getRoles().get(RoleScope.APPLICATION));
        assertThat(group.getLockApiRole()).isEqualTo(groupEntity.isLockApiRole());
        assertThat(group.getLockApplicationRole()).isEqualTo(groupEntity.isLockApplicationRole());
        assertThat(group.getSystemInvitation()).isEqualTo(groupEntity.isSystemInvitation());
        assertThat(group.getEmailInvitation()).isEqualTo(groupEntity.isEmailInvitation());
        assertThat(group.getDisableMembershipNotifications()).isEqualTo(groupEntity.isDisableMembershipNotifications());
        assertThat(group.getApiPrimaryOwner()).isEqualTo(groupEntity.getApiPrimaryOwner());
        assertThat(group.getPrimaryOwner()).isEqualTo(groupEntity.isPrimaryOwner());
    }

    @Test
    void should_map_list_of_GroupEntity_to_list_of_Group() {
        List<GroupEntity> groupEntities = Arrays.asList(createGroupEntity("group1"), createGroupEntity("group2"));

        List<Group> groups = groupMapper.map(groupEntities);

        assertThat(groups).isNotNull();
        assertThat(groups).hasSize(2);
        assertThat(groups.get(0).getId()).isEqualTo("group1");
        assertThat(groups.get(1).getId()).isEqualTo("group2");
    }

    @Test
    void should_map_GroupEventRuleEntities_to_GroupEvents() {
        List<GroupEventRuleEntity> eventRules = Arrays.asList(
            createGroupEventRuleEntity(io.gravitee.apim.core.group.model.Group.GroupEvent.API_CREATE.name()),
            createGroupEventRuleEntity(io.gravitee.apim.core.group.model.Group.GroupEvent.APPLICATION_CREATE.name())
        );

        List<GroupEvent> groupEvents = groupMapper.mapGroupEventRuleEntities(eventRules);

        assertThat(groupEvents).isNotNull();
        assertThat(groupEvents).hasSize(2);
        assertThat(groupEvents.get(0)).isEqualTo(GroupEvent.API_CREATE);
        assertThat(groupEvents.get(1)).isEqualTo(GroupEvent.APPLICATION_CREATE);
    }

    @Test
    void should_handle_invalid_GroupEventRuleEntity() {
        List<GroupEventRuleEntity> eventRules = Arrays.asList(
            createGroupEventRuleEntity(io.gravitee.apim.core.group.model.Group.GroupEvent.API_CREATE.name()),
            createGroupEventRuleEntity("INVALID_EVENT")
        );

        List<GroupEvent> groupEvents = groupMapper.mapGroupEventRuleEntities(eventRules);

        assertThat(groupEvents).isNotNull();
        assertThat(groupEvents).hasSize(2);
        assertThat(groupEvents.get(0)).isEqualTo(GroupEvent.API_CREATE);
        assertThat(groupEvents.get(1)).isNull();
    }

    @Test
    void should_handle_null_GroupEventRuleEntities() {
        List<GroupEventRuleEntity> eventRules = null;

        List<GroupEvent> groupEvents = groupMapper.mapGroupEventRuleEntities(eventRules);

        assertThat(groupEvents).isNull();
    }

    @Test
    void should_map_apiRole_from_roles() {
        Map<RoleScope, String> roles = new HashMap<>();
        roles.put(RoleScope.API, "API_ROLE");
        roles.put(RoleScope.APPLICATION, "APP_ROLE");

        String apiRole = groupMapper.mapApiRole(roles);

        assertThat(apiRole).isEqualTo("API_ROLE");
    }

    @Test
    void should_handle_null_roles_for_apiRole() {
        Map<RoleScope, String> roles = null;

        String apiRole = groupMapper.mapApiRole(roles);

        assertThat(apiRole).isNull();
    }

    @Test
    void should_map_applicationRole_from_roles() {
        Map<RoleScope, String> roles = new HashMap<>();
        roles.put(RoleScope.API, "API_ROLE");
        roles.put(RoleScope.APPLICATION, "APP_ROLE");

        String applicationRole = groupMapper.mapApplicationRole(roles);

        assertThat(applicationRole).isEqualTo("APP_ROLE");
    }

    @Test
    void should_handle_null_roles_for_applicationRole() {
        Map<RoleScope, String> roles = null;

        String applicationRole = groupMapper.mapApplicationRole(roles);

        assertThat(applicationRole).isNull();
    }

    @Test
    void should_map_core_Group_to_Group() {
        io.gravitee.apim.core.group.model.Group coreGroup = createCoreGroup();

        Group group = groupMapper.mapFromCore(coreGroup);

        assertThat(group).isNotNull();
        assertThat(group.getId()).isEqualTo(coreGroup.getId());
        assertThat(group.getName()).isEqualTo(coreGroup.getName());
        assertThat(group.getCreatedAt().toInstant()).isEqualTo(coreGroup.getCreatedAt().toInstant());
        assertThat(group.getUpdatedAt().toInstant()).isEqualTo(coreGroup.getUpdatedAt().toInstant());
        assertThat(group.getMaxInvitation()).isEqualTo(coreGroup.getMaxInvitation());
        assertThat(group.getLockApiRole()).isEqualTo(coreGroup.isLockApiRole());
        assertThat(group.getLockApplicationRole()).isEqualTo(coreGroup.isLockApplicationRole());
        assertThat(group.getSystemInvitation()).isEqualTo(coreGroup.isSystemInvitation());
        assertThat(group.getEmailInvitation()).isEqualTo(coreGroup.isEmailInvitation());
        assertThat(group.getDisableMembershipNotifications()).isEqualTo(coreGroup.isDisableMembershipNotifications());
        assertThat(group.getApiPrimaryOwner()).isEqualTo(coreGroup.getApiPrimaryOwner());
    }

    @Test
    void should_map_list_of_core_Group_to_list_of_Group() {
        List<io.gravitee.apim.core.group.model.Group> coreGroups = Arrays.asList(createCoreGroup("core1"), createCoreGroup("core2"));

        List<Group> groups = groupMapper.mapFromCoreList(coreGroups);

        assertThat(groups).isNotNull();
        assertThat(groups).hasSize(2);
        assertThat(groups.get(0).getId()).isEqualTo("core1");
        assertThat(groups.get(1).getId()).isEqualTo("core2");
    }

    @Test
    void should_map_core_GroupEventRules_to_GroupEvents() {
        List<GroupEventRule> eventRules = Arrays.asList(
            new GroupEventRule(io.gravitee.apim.core.group.model.Group.GroupEvent.API_CREATE),
            new GroupEventRule(io.gravitee.apim.core.group.model.Group.GroupEvent.APPLICATION_CREATE)
        );

        List<GroupEvent> groupEvents = groupMapper.mapCoreGroupEventRules(eventRules);

        assertThat(groupEvents).isNotNull();
        assertThat(groupEvents).hasSize(2);
        assertThat(groupEvents.get(0)).isEqualTo(GroupEvent.API_CREATE);
        assertThat(groupEvents.get(1)).isEqualTo(GroupEvent.APPLICATION_CREATE);
    }

    @Test
    void should_handle_null_core_GroupEventRules() {
        List<GroupEventRule> eventRules = null;

        List<GroupEvent> groupEvents = groupMapper.mapCoreGroupEventRules(eventRules);

        assertThat(groupEvents).isNull();
    }

    private GroupEntity createGroupEntity() {
        return createGroupEntity("group-id");
    }

    private GroupEntity createGroupEntity(String id) {
        GroupEntity groupEntity = GroupEntity
            .builder()
            .id(id)
            .name("Group " + id)
            .manageable(true)
            .createdAt(new Date())
            .updatedAt(new Date())
            .maxInvitation(10)
            .lockApiRole(true)
            .lockApplicationRole(false)
            .systemInvitation(true)
            .emailInvitation(true)
            .disableMembershipNotifications(false)
            .apiPrimaryOwner("user-id")
            .primaryOwner(true)
            .build();

        Map<RoleScope, String> roles = new HashMap<>();
        roles.put(RoleScope.API, "API_PUBLISHER");
        roles.put(RoleScope.APPLICATION, "APPLICATION_OWNER");
        groupEntity.setRoles(roles);

        List<GroupEventRuleEntity> eventRules = new ArrayList<>();
        eventRules.add(createGroupEventRuleEntity(io.gravitee.apim.core.group.model.Group.GroupEvent.API_CREATE.name()));
        groupEntity.setEventRules(eventRules);

        return groupEntity;
    }

    private GroupEventRuleEntity createGroupEventRuleEntity(String event) {
        return GroupEventRuleEntity.builder().event(event).build();
    }

    private io.gravitee.apim.core.group.model.Group createCoreGroup() {
        return createCoreGroup("core-group-id");
    }

    private io.gravitee.apim.core.group.model.Group createCoreGroup(String id) {
        List<GroupEventRule> eventRules = new ArrayList<>();
        eventRules.add(new GroupEventRule(io.gravitee.apim.core.group.model.Group.GroupEvent.API_CREATE));

        return io.gravitee.apim.core.group.model.Group
            .builder()
            .id(id)
            .environmentId("env-id")
            .name("Core Group " + id)
            .eventRules(eventRules)
            .createdAt(ZonedDateTime.now())
            .updatedAt(ZonedDateTime.now())
            .maxInvitation(10)
            .lockApiRole(true)
            .lockApplicationRole(false)
            .systemInvitation(true)
            .emailInvitation(true)
            .disableMembershipNotifications(false)
            .apiPrimaryOwner("user-id")
            .build();
    }
}
