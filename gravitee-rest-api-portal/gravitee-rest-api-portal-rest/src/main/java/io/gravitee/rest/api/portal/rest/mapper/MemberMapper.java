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
package io.gravitee.rest.api.portal.rest.mapper;

import java.time.ZoneOffset;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.UriInfo;

import io.gravitee.rest.api.model.MemberEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.portal.rest.model.Member;
import io.gravitee.rest.api.portal.rest.model.User;
import io.gravitee.rest.api.service.UserService;

import static io.gravitee.rest.api.portal.rest.utils.PortalApiLinkHelper.usersURL;

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
    
    public Member convert(MemberEntity member, UriInfo uriInfo) {
        final Member memberItem = new Member();
        
        memberItem.setCreatedAt(member.getCreatedAt().toInstant().atOffset(ZoneOffset.UTC));
        
        UserEntity userEntity = userService.findById(member.getId());
        User memberUser = userMapper.convert(userEntity);
        memberUser.setLinks(userMapper.computeUserLinks(usersURL(uriInfo.getBaseUriBuilder(), userEntity.getId()), userEntity.getUpdatedAt()));
        
        memberItem.setUser(memberUser);
        if(member.getRoles() != null && !member.getRoles().isEmpty()) {
            memberItem.setRole(member.getRoles().get(0).getName());
        }
        memberItem.setUpdatedAt(member.getUpdatedAt().toInstant().atOffset(ZoneOffset.UTC));
        return memberItem;
    }

}
