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
package io.gravitee.apim.rest.api.automation.mapper;

import io.gravitee.apim.core.group.model.Group;
import io.gravitee.apim.core.group.model.crd.GroupCRDSpec;
import io.gravitee.apim.core.group.model.crd.GroupCRDStatus;
import io.gravitee.apim.core.member.model.RoleScope;
import io.gravitee.apim.rest.api.automation.model.Errors;
import io.gravitee.apim.rest.api.automation.model.GroupDefaultMemberRoles;
import io.gravitee.apim.rest.api.automation.model.GroupMember;
import io.gravitee.apim.rest.api.automation.model.GroupSpec;
import io.gravitee.apim.rest.api.automation.model.GroupState;
import io.gravitee.rest.api.management.v2.rest.mapper.CollectionFactory;
import io.gravitee.rest.api.model.GroupEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Mapper(uses = { CollectionFactory.class })
public interface GroupMapper {
    GroupMapper INSTANCE = Mappers.getMapper(GroupMapper.class);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "origin", expression = "java(io.gravitee.definition.model.Origin.KUBERNETES.name())")
    @Mapping(target = "defaultMemberRoles", qualifiedByName = "defaultMemberRolesToMap")
    @Mapping(target = "apiRole", ignore = true)
    @Mapping(target = "applicationRole", ignore = true)
    @Mapping(target = "apiProductRole", ignore = true)
    @Mapping(target = "ignoreMembers", ignore = true)
    GroupCRDSpec groupSpecToGroupCRDSpec(GroupSpec groupSpec);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "roles", qualifiedByName = "stringMapToRoleScopeMap")
    GroupCRDSpec.Member groupMemberToMember(GroupMember groupMember);

    @Mapping(target = "roles", qualifiedByName = "roleScopeMapToStringMap")
    GroupMember memberToGroupMember(GroupCRDSpec.Member member);

    Errors toErrors(GroupCRDStatus.Errors errors);

    default GroupState groupSpecAndStatusToGroupState(GroupSpec spec, GroupCRDStatus status, ExecutionContext executionContext) {
        var state = new GroupState(
            status.getId(),
            executionContext.getEnvironmentId(),
            executionContext.getOrganizationId(),
            toErrors(status.getErrors()),
            status.getMembers()
        );
        mapSpecToState(spec, state);
        return state;
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "environmentId", ignore = true)
    @Mapping(target = "organizationId", ignore = true)
    @Mapping(target = "errors", ignore = true)
    @Mapping(target = "memberCount", ignore = true)
    void mapSpecToState(GroupSpec spec, @MappingTarget GroupState state);

    default GroupState groupToGroupState(
        Group group,
        Set<GroupCRDSpec.Member> members,
        GroupDefaultMemberRoles defaultMemberRoles,
        ExecutionContext executionContext
    ) {
        var state = new GroupState(
            group.getId(),
            executionContext.getEnvironmentId(),
            executionContext.getOrganizationId(),
            null,
            members != null ? (long) members.size() : 0L
        );
        mapGroupToState(group, state);
        state.setMembers(members != null ? members.stream().map(this::memberToGroupMember).toList() : null);
        state.setDefaultMemberRoles(defaultMemberRoles);
        return state;
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "environmentId", ignore = true)
    @Mapping(target = "organizationId", ignore = true)
    @Mapping(target = "errors", ignore = true)
    @Mapping(target = "memberCount", ignore = true)
    @Mapping(target = "members", ignore = true)
    @Mapping(target = "defaultMemberRoles", ignore = true)
    @Mapping(target = "notifyMembers", expression = "java(!group.isDisableMembershipNotifications())")
    void mapGroupToState(Group group, @MappingTarget GroupState state);

    /**
     * An absent object means "not declared": the group's default roles are left untouched. A declared one is the whole
     * set; its unset scopes are cleared.
     */
    @Named("defaultMemberRolesToMap")
    default Map<RoleScope, String> defaultMemberRolesToMap(GroupDefaultMemberRoles roles) {
        if (roles == null) {
            return null;
        }
        var map = new java.util.LinkedHashMap<RoleScope, String>();
        if (roles.getApi() != null) {
            map.put(RoleScope.API, roles.getApi());
        }
        if (roles.getApplication() != null) {
            map.put(RoleScope.APPLICATION, roles.getApplication());
        }
        if (roles.getApiProduct() != null) {
            map.put(RoleScope.API_PRODUCT, roles.getApiProduct());
        }
        return map;
    }

    /**
     * The group's default roles as APIM holds them; an empty object when it has none, so that a client that declared
     * {} reads back what it sent.
     */
    @Named("groupEntityToDefaultMemberRoles")
    default GroupDefaultMemberRoles groupEntityToDefaultMemberRoles(GroupEntity group) {
        var roles = Optional.ofNullable(group)
            .map(GroupEntity::getRoles)
            .orElse(Map.<io.gravitee.rest.api.model.permissions.RoleScope, String>of());
        return new GroupDefaultMemberRoles()
            .api(roles.get(io.gravitee.rest.api.model.permissions.RoleScope.API))
            .application(roles.get(io.gravitee.rest.api.model.permissions.RoleScope.APPLICATION))
            .apiProduct(roles.get(io.gravitee.rest.api.model.permissions.RoleScope.API_PRODUCT));
    }

    @Named("stringMapToRoleScopeMap")
    default Map<RoleScope, String> stringMapToRoleScopeMap(Map<String, String> roles) {
        if (roles == null) {
            return null;
        }
        return roles.entrySet().stream().collect(Collectors.toMap(e -> RoleScope.valueOf(e.getKey()), Map.Entry::getValue));
    }

    @Named("roleScopeMapToStringMap")
    default Map<String, String> roleScopeMapToStringMap(Map<RoleScope, String> roles) {
        if (roles == null) {
            return null;
        }
        return roles.entrySet().stream().collect(Collectors.toMap(e -> e.getKey().name(), Map.Entry::getValue));
    }
}
