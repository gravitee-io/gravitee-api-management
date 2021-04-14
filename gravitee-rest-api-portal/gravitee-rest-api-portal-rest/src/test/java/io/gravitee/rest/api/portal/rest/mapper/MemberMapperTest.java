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

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import io.gravitee.rest.api.model.MemberEntity;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.portal.rest.model.Member;
import io.gravitee.rest.api.portal.rest.model.User;
import io.gravitee.rest.api.service.UserService;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class MemberMapperTest {

    private static final String MEMBER_DISPLAYNAME = "my-member-display-name";
    private static final String MEMBER_EMAIL = "my-member-email";
    private static final String MEMBER_ID = "my-member-id";

    private MemberEntity memberEntity;

    @InjectMocks
    private MemberMapper memberMapper;

    @Mock
    private UriInfo uriInfo;

    @Mock
    private UserService userService;

    @Spy
    private UserMapper userMapper = new UserMapper();

    @Test
    public void testConvert() {
        Instant now = Instant.now();
        Date nowDate = Date.from(now);

        //init
        RoleEntity ownerRoleEntity = new RoleEntity();
        ownerRoleEntity.setName("OWNER");

        memberEntity = new MemberEntity();

        memberEntity.setCreatedAt(nowDate);
        memberEntity.setDisplayName(MEMBER_DISPLAYNAME);
        memberEntity.setEmail(MEMBER_EMAIL);
        memberEntity.setId(MEMBER_ID);
        memberEntity.setRoles(Arrays.asList(ownerRoleEntity));
        memberEntity.setUpdatedAt(nowDate);

        UserEntity userEntity = Mockito.mock(UserEntity.class);
        when(userEntity.getDisplayName()).thenReturn(MEMBER_DISPLAYNAME);
        when(userEntity.getEmail()).thenReturn(MEMBER_EMAIL);
        when(userEntity.getId()).thenReturn(MEMBER_ID);

        when(uriInfo.getBaseUriBuilder()).thenReturn(UriBuilder.fromPath(""));

        when(userService.findById(MEMBER_ID)).thenReturn(userEntity);
        when(userMapper.convert(userEntity)).thenCallRealMethod();
        when(userMapper.computeUserLinks(anyString(), any())).thenCallRealMethod();

        //Test
        Member responseMember = memberMapper.convert(memberEntity, uriInfo);
        assertNotNull(responseMember);
        assertEquals(now.toEpochMilli(), responseMember.getCreatedAt().toInstant().toEpochMilli());
        assertNull(responseMember.getId());
        assertEquals("OWNER", responseMember.getRole());
        assertEquals(now.toEpochMilli(), responseMember.getUpdatedAt().toInstant().toEpochMilli());

        User user = responseMember.getUser();
        assertNotNull(user);
        assertEquals(MEMBER_DISPLAYNAME, user.getDisplayName());
        assertEquals(MEMBER_EMAIL, user.getEmail());
        assertEquals(MEMBER_ID, user.getId());
        assertEquals("environments/DEFAULT/users/" + MEMBER_ID + "/avatar", user.getLinks().getAvatar());
    }
}
