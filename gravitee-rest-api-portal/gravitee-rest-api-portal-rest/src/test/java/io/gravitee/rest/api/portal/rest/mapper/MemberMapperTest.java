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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.time.Instant;
import java.util.Date;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import io.gravitee.rest.api.model.MemberEntity;
import io.gravitee.rest.api.portal.rest.model.Member;
import io.gravitee.rest.api.portal.rest.model.User;

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
    
    @Test
    public void testConvert() {
        Instant now = Instant.now();
        Date nowDate = Date.from(now);

        //init
        memberEntity = new MemberEntity();
       
        memberEntity.setCreatedAt(nowDate);
        memberEntity.setDisplayName(MEMBER_DISPLAYNAME);
        memberEntity.setEmail(MEMBER_EMAIL);
        memberEntity.setId(MEMBER_ID);
        memberEntity.setRole("OWNER");
        memberEntity.setUpdatedAt(nowDate);
        
        //Test
        Member responseMember = memberMapper.convert(memberEntity);
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

    }
    
}
