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
import java.util.LinkedHashSet;
import java.util.SequencedSet;
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

    default SequencedSet<MemberCRD> toApiMemberCRDSet(Set<GroupCRDSpec.Member> members) {
        return toMemberCRDSet(members, RoleScope.API);
    }

    default SequencedSet<MemberCRD> toApplicationMemberCRDSet(Set<GroupCRDSpec.Member> members) {
        return toMemberCRDSet(members, RoleScope.APPLICATION);
    }

    default SequencedSet<MemberCRD> toIntegrationMemberCRDSet(Set<GroupCRDSpec.Member> members) {
        return toMemberCRDSet(members, RoleScope.INTEGRATION);
    }

    private static SequencedSet<MemberCRD> toMemberCRDSet(Set<GroupCRDSpec.Member> members, RoleScope roleScope) {
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
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
