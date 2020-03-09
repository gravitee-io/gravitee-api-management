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
package io.gravitee.management.model;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
public class UserEntityTest {

    @Test
    public void shouldDisplayName_lastname_null() {
        UserEntity userEntity = new UserEntity();
        userEntity.setFirstname("user");
        userEntity.setLastname(null);
        userEntity.setEmail(null);

        Assert.assertEquals("user", userEntity.getDisplayName());
    }

    @Test
    public void shouldDisplayName_lastname_empty() {
        UserEntity userEntity = new UserEntity();
        userEntity.setFirstname("user");
        userEntity.setLastname("");
        userEntity.setEmail(null);

        Assert.assertEquals("user", userEntity.getDisplayName());
    }

    @Test
    public void shouldDisplayName_firstname_null() {
        UserEntity userEntity = new UserEntity();
        userEntity.setFirstname(null);
        userEntity.setLastname("lastname");
        userEntity.setEmail(null);

        Assert.assertEquals("lastname", userEntity.getDisplayName());
    }

    @Test
    public void shouldDisplayName_firstname_empty() {
        UserEntity userEntity = new UserEntity();
        userEntity.setFirstname("");
        userEntity.setLastname("lastname");
        userEntity.setEmail(null);

        Assert.assertEquals("lastname", userEntity.getDisplayName());
    }

    @Test
    public void shouldDisplayName_firstname_lastname_null() {
        UserEntity userEntity = new UserEntity();
        userEntity.setFirstname(null);
        userEntity.setLastname(null);
        userEntity.setEmail("test@test.com");

        Assert.assertEquals("test@test.com", userEntity.getDisplayName());
    }

    @Test
    public void shouldDisplayName_firstname_lastname_empty() {
        UserEntity userEntity = new UserEntity();
        userEntity.setFirstname("");
        userEntity.setLastname("");
        userEntity.setEmail("test@test.com");

        Assert.assertEquals("test@test.com", userEntity.getDisplayName());
    }

    @Test
    public void shouldDisplayName_email_null() {
        UserEntity userEntity = new UserEntity();
        userEntity.setFirstname(null);
        userEntity.setLastname(null);
        userEntity.setEmail(null);
        userEntity.setSourceId("username");

        Assert.assertEquals("username", userEntity.getDisplayName());
    }

    @Test
    public void shouldDisplayName_email_empty() {
        UserEntity userEntity = new UserEntity();
        userEntity.setFirstname("");
        userEntity.setLastname("");
        userEntity.setEmail("");
        userEntity.setSourceId("username");

        Assert.assertEquals("username", userEntity.getDisplayName());
    }
}
