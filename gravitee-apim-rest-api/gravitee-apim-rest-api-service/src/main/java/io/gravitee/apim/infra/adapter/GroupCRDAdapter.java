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
package io.gravitee.apim.infra.adapter;

import io.gravitee.apim.core.group.model.crd.GroupCRDSpec;
import io.gravitee.apim.core.member.model.RoleScope;
import io.gravitee.apim.core.member.model.crd.MemberCRD;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Mapper
public interface GroupCRDAdapter {
    GroupCRDAdapter INSTANCE = Mappers.getMapper(GroupCRDAdapter.class);

    default Set<MemberCRD> toApiMemberCRDSet(Set<GroupCRDSpec.Member> members) {
        return toMemberCRDSet(members, RoleScope.API);
    }

    default Set<MemberCRD> toApplicationMemberCRDSet(Set<GroupCRDSpec.Member> members) {
        return toMemberCRDSet(members, RoleScope.APPLICATION);
    }

    default Set<MemberCRD> toIntegrationMemberCRDSet(Set<GroupCRDSpec.Member> members) {
        return toMemberCRDSet(members, RoleScope.INTEGRATION);
    }

    default Set<GroupCRDSpec.Member> toGroupMembers(Set<MemberCRD> apiMembers) {
        var groupMembers = new HashMap<String, GroupCRDSpec.Member>();
        for (var member : apiMembers) {
            groupMembers.put(member.getId(), initGroupMember(member, RoleScope.API));
        }
        return new HashSet<>(groupMembers.values());
    }

    private GroupCRDSpec.Member initGroupMember(MemberCRD memberCRD, RoleScope roleScope) {
        var memberRoles = new HashMap<RoleScope, String>();
        memberRoles.put(roleScope, memberCRD.getRole());
        return new GroupCRDSpec.Member(memberCRD.getId(), memberCRD.getSourceId(), memberCRD.getSource(), memberRoles);
    }

    private static Set<MemberCRD> toMemberCRDSet(Set<GroupCRDSpec.Member> members, RoleScope roleScope) {
        return members
            .stream()
            .flatMap(member ->
                member
                    .getRoles()
                    .entrySet()
                    .stream()
                    .filter(roleEntry -> roleEntry.getKey() == roleScope)
                    .map(roleEntry -> new MemberCRD(member.getId(), member.getSource(), member.getSourceId(), roleEntry.getValue()))
            )
            .collect(Collectors.toSet());
    }
}
