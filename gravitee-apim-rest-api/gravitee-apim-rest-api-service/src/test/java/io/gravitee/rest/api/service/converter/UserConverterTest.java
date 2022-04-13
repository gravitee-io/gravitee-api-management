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
package io.gravitee.rest.api.service.converter;

import static org.junit.Assert.assertEquals;

import io.gravitee.repository.management.model.User;
import io.gravitee.rest.api.model.NewExternalUserEntity;
import io.gravitee.rest.api.model.UserEntity;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class UserConverterTest {

    @InjectMocks
    private UserConverter userConverter;

    @Test
    public void should_convert_userEntity_to_user() {
        UserEntity userEntity = new UserEntity();
        userEntity.setEmail("test@test.test.fr");
        userEntity.setFirstname("my-firstname");
        userEntity.setLastname("my-lastname");
        userEntity.setId("my-id");

        User user = userConverter.toUser(userEntity);

        assertEquals("test@test.test.fr", user.getEmail());
        assertEquals("my-firstname", user.getFirstname());
        assertEquals("my-lastname", user.getLastname());
        assertEquals("my-id", user.getId());
    }

    @Test
    public void should_convert_user_to_userEntity() {
        User user = new User();
        user.setEmail("test@test.test.fr");
        user.setFirstname("my-firstname");
        user.setLastname("my-lastname");
        user.setId("my-id");

        UserEntity userEntity = userConverter.toUserEntity(user);

        assertEquals("test@test.test.fr", userEntity.getEmail());
        assertEquals("my-firstname", userEntity.getFirstname());
        assertEquals("my-lastname", userEntity.getLastname());
        assertEquals("my-id", userEntity.getId());
    }

    @Test
    public void should_convert_newExternalUserEntity_to_User() {
        NewExternalUserEntity userEntity = new NewExternalUserEntity();
        userEntity.setEmail("test@test.test.fr");
        userEntity.setFirstname("my-firstname");
        userEntity.setLastname("my-lastname");

        User user = userConverter.toUser(userEntity);

        assertEquals("test@test.test.fr", user.getEmail());
        assertEquals("my-firstname", user.getFirstname());
        assertEquals("my-lastname", user.getLastname());
    }
}
