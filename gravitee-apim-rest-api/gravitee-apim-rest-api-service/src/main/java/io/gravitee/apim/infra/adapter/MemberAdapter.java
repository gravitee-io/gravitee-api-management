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

import io.gravitee.apim.core.api.model.import_definition.ApiMember;
import io.gravitee.apim.core.api.model.import_definition.ApiMemberRole;
import io.gravitee.apim.core.member.model.Member;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.membership.model.Role;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.rest.api.model.MemberEntity;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.RoleEntity;
import java.util.List;
import java.util.Set;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface MemberAdapter {
    MemberAdapter INSTANCE = Mappers.getMapper(MemberAdapter.class);

    @Mapping(target = "id", source = "member.memberId")
    @Mapping(target = "type", source = "member.memberType")
    @Mapping(target = "displayName", expression = "java(user != null ? user.displayName() : null)")
    @Mapping(target = "roles", expression = "java(role != null ? List.of(mapRole(role)) : null)")
    ApiMember toApiMember(Membership member, BaseUserEntity user, Role role);

    MemberEntity toEntity(ApiMember member);
    Set<MemberEntity> toEntities(Set<ApiMember> members);

    RoleEntity toEntity(ApiMemberRole role);
    List<RoleEntity> toEntities(List<ApiMemberRole> roles);

    Member toMember(MemberEntity member);
    MemberEntity toMemberEntity(Member member);

    default MembershipMemberType mapType(Membership.Type type) {
        return switch (type) {
            case USER -> MembershipMemberType.USER;
            case GROUP -> MembershipMemberType.GROUP;
        };
    }

    ApiMemberRole mapRole(Role role);
}
