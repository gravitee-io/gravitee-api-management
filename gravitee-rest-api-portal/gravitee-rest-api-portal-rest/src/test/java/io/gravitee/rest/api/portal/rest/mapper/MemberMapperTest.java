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

import java.time.Instant;
import java.util.Date;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import io.gravitee.rest.api.model.MemberEntity;
import io.gravitee.rest.api.portal.rest.model.Member;
import io.gravitee.rest.api.portal.rest.model.RoleEnum;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class MemberMapperTest {

    private static final String MEMBER = "my-member";

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
        memberEntity.setDisplayName(MEMBER);
        memberEntity.setEmail(MEMBER);
        memberEntity.setId(MEMBER);
        memberEntity.setRole("OWNER");
        memberEntity.setUpdatedAt(nowDate);
        
        //Test
        Member responseMember = memberMapper.convert(memberEntity);
        assertNotNull(responseMember);
        
        assertEquals(now.toEpochMilli(), responseMember.getCreatedAt().toInstant().toEpochMilli());
        assertEquals(MEMBER, responseMember.getDisplayName());
        assertEquals(MEMBER, responseMember.getEmail());
        assertEquals(MEMBER, responseMember.getId());
        assertEquals(RoleEnum.OWNER, responseMember.getRole());
        assertEquals(now.toEpochMilli(), responseMember.getUpdatedAt().toInstant().toEpochMilli());
    }
    
}
