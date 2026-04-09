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
package io.gravitee.rest.api.portal.rest.mapper;

import static io.gravitee.rest.api.portal.rest.utils.PortalApiLinkHelper.usersURL;

import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.rest.api.model.MemberEntity;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.portal.rest.model.Member;
import io.gravitee.rest.api.portal.rest.model.User;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import jakarta.ws.rs.core.UriInfo;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class MemberMapper {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserService userService;

    public Member convert(ExecutionContext executionContext, MemberEntity member, UriInfo uriInfo) {
        final Member memberItem = new Member();

        memberItem.setId(member.getId());
        memberItem.setCreatedAt(member.getCreatedAt().toInstant().atOffset(ZoneOffset.UTC));

        UserEntity userEntity = userService.findById(executionContext, member.getId());
        User memberUser = userMapper.convert(userEntity);
        memberUser.setLinks(
            userMapper.computeUserLinks(usersURL(uriInfo.getBaseUriBuilder(), userEntity.getId()), userEntity.getUpdatedAt())
        );

        memberItem.setUser(memberUser);
        if (member.getRoles() != null && !member.getRoles().isEmpty()) {
            memberItem.setRole(member.getRoles().get(0).getName());
        }
        memberItem.setUpdatedAt(member.getUpdatedAt().toInstant().atOffset(ZoneOffset.UTC));
        return memberItem;
    }

    public List<Member> convert(
        List<Membership> memberships,
        List<BaseUserEntity> users,
        Map<String, RoleEntity> rolesById,
        UriInfo uriInfo
    ) {
        var usersById = users.stream().collect(Collectors.toMap(BaseUserEntity::getId, Function.identity(), (left, right) -> left));

        return memberships
            .stream()
            .map(membership -> convert(membership, usersById.get(membership.getMemberId()), rolesById, uriInfo))
            .toList();
    }

    private Member convert(Membership membership, BaseUserEntity user, Map<String, RoleEntity> rolesById, UriInfo uriInfo) {
        var memberItem = new Member();
        memberItem.setId(membership.getMemberId());
        if (membership.getCreatedAt() != null) {
            memberItem.setCreatedAt(membership.getCreatedAt().toInstant().atOffset(ZoneOffset.UTC));
        }
        if (membership.getUpdatedAt() != null) {
            memberItem.setUpdatedAt(membership.getUpdatedAt().toInstant().atOffset(ZoneOffset.UTC));
        }

        var memberUser = new User();
        memberUser.setId(membership.getMemberId());
        if (user != null) {
            memberUser.setDisplayName(user.displayName());
            memberUser.setEmail(user.getEmail());
        }
        memberUser.setLinks(
            userMapper.computeUserLinks(usersURL(uriInfo.getBaseUriBuilder(), membership.getMemberId()), userUpdatedAt(user))
        );
        memberItem.setUser(memberUser);

        var role = rolesById.get(membership.getRoleId());
        if (role != null) {
            memberItem.setRole(role.getName());
        }
        return memberItem;
    }

    private Date userUpdatedAt(BaseUserEntity user) {
        if (user == null) {
            return null;
        }
        return user.getUpdatedAt();
    }
}
