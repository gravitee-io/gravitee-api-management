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
import io.gravitee.apim.rest.api.automation.model.GroupMember;
import io.gravitee.apim.rest.api.automation.model.GroupSpec;
import io.gravitee.apim.rest.api.automation.model.GroupState;
import io.gravitee.apim.rest.api.automation.model.GroupStatus;
import io.gravitee.definition.model.Origin;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Mapper
public interface GroupMapper {
    GroupMapper INSTANCE = Mappers.getMapper(GroupMapper.class);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "origin", expression = "java(io.gravitee.definition.model.Origin.KUBERNETES.name())")
    GroupCRDSpec groupSpecToGroupCRDSpec(GroupSpec groupSpec);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "roles", qualifiedByName = "stringMapToRoleScopeMap")
    GroupCRDSpec.Member groupMemberToMember(GroupMember groupMember);

    @Mapping(target = "roles", qualifiedByName = "roleScopeMapToStringMap")
    GroupMember memberToGroupMember(GroupCRDSpec.Member member);

    GroupStatus toGroupStatus(GroupCRDStatus status);

    Errors toErrors(GroupCRDStatus.Errors errors);

    default GroupState groupSpecAndStatusToGroupState(GroupSpec spec, GroupCRDStatus status) {
        GroupState state = new GroupState();
        state.setSpec(spec);
        state.setStatus(toGroupStatus(status));
        return state;
    }

    default GroupState groupToGroupState(Group group, Set<GroupCRDSpec.Member> members) {
        GroupSpec spec = new GroupSpec();
        spec.setHrid(group.getHrid());
        spec.setName(group.getName());
        spec.setNotifyMembers(!group.isDisableMembershipNotifications());
        spec.setMembers(members != null ? members.stream().map(this::memberToGroupMember).toList() : List.of());

        GroupStatus status = new GroupStatus();
        status.setId(group.getId());
        status.setMembers(members != null ? (long) members.size() : 0L);

        GroupState state = new GroupState();
        state.setSpec(spec);
        state.setStatus(status);
        return state;
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
