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

import io.gravitee.apim.core.api.model.import_definition.ApiMember;
import io.gravitee.rest.api.management.v2.rest.model.AddMember;
import io.gravitee.rest.api.management.v2.rest.model.Member;
import io.gravitee.rest.api.model.MemberEntity;
import java.util.List;
import java.util.Set;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(uses = { RoleMapper.class })
public interface MemberMapper {
    MemberMapper INSTANCE = Mappers.getMapper(MemberMapper.class);

    Member map(MemberEntity memberEntity);
    MemberEntity map(Member member);

    Set<Member> map(Set<MemberEntity> memberEntityCollection);

    List<Member> map(List<MemberEntity> members);

    ApiMember toApiMember(Member member);
    Set<ApiMember> toApiMembers(Set<Member> members);

    io.gravitee.apim.core.membership.model.AddMember map(AddMember addMember);
}
