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

import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.portal.rest.model.User;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class UserMapperTest {

    private static final String USER_EMAIL = "my-user-email";
    private static final String USER_FIRSTNAME = "my-user-firstname";
    private static final String USER_LASTNAME = "my-user-lastname";
    private static final String USER_ID = "my-user-id";
    private static final String USER_PASSWORD = "my-user-password";
    private static final String USER_PICTURE = "my-user-piucture";
    private static final String USER_SOURCE = "my-user-source";
    private static final String USER_SOURCE_ID = "my-user-source-id";
    private static final String USER_STATUS = "my-user-status";

    private UserEntity userEntity;

    @InjectMocks
    private UserMapper userMapper;
    
    @Test
    public void testConvert() {
        Instant now = Instant.now();
        Date nowDate = Date.from(now);
        
        //init
        userEntity = new UserEntity();
       
        userEntity.setCreatedAt(nowDate);
        userEntity.setEmail(USER_EMAIL);
        userEntity.setFirstname(USER_FIRSTNAME);
        userEntity.setId(USER_ID);
        userEntity.setLastConnectionAt(nowDate);
        userEntity.setLastname(USER_LASTNAME);
        userEntity.setPassword(USER_PASSWORD);
        userEntity.setPicture(USER_PICTURE);
        userEntity.setRoles(null);
        userEntity.setSource(USER_SOURCE);
        userEntity.setSourceId(USER_SOURCE_ID);
        userEntity.setStatus(USER_STATUS);
        userEntity.setUpdatedAt(nowDate);
        
        //Test
        User responseUser = userMapper.convert(userEntity);
        assertNotNull(responseUser);
        assertEquals(USER_ID, responseUser.getId());
        assertEquals(USER_PICTURE, responseUser.getAvatar());
        assertEquals(USER_EMAIL, responseUser.getEmail());
        assertEquals(USER_FIRSTNAME, responseUser.getFirstName());
        assertEquals(USER_LASTNAME, responseUser.getLastName());
        assertEquals(USER_FIRSTNAME+' '+USER_LASTNAME, responseUser.getDisplayName());
    }
    
}
