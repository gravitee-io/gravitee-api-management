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
package io.gravitee.management.idp.api.authentication;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
public class UserDetailsTest {

    @Test
    public void shouldDisplayName_lastname_null() {
        UserDetails userDetails = new UserDetails("username", "", Collections.emptyList());
        userDetails.setFirstname("user");
        userDetails.setLastname(null);
        userDetails.setEmail(null);

        Assert.assertEquals("user", userDetails.getDisplayName());
    }

    @Test
    public void shouldDisplayName_lastname_empty() {
        UserDetails userDetails = new UserDetails("username", "", Collections.emptyList());
        userDetails.setFirstname("user");
        userDetails.setLastname("");
        userDetails.setEmail(null);

        Assert.assertEquals("user", userDetails.getDisplayName());
    }

    @Test
    public void shouldDisplayName_firstname_null() {
        UserDetails userDetails = new UserDetails("username", "", Collections.emptyList());
        userDetails.setFirstname(null);
        userDetails.setLastname("lastname");
        userDetails.setEmail(null);

        Assert.assertEquals("lastname", userDetails.getDisplayName());
    }

    @Test
    public void shouldDisplayName_firstname_empty() {
        UserDetails userDetails = new UserDetails("username", "", Collections.emptyList());
        userDetails.setFirstname("");
        userDetails.setLastname("lastname");
        userDetails.setEmail(null);

        Assert.assertEquals("lastname", userDetails.getDisplayName());
    }

    @Test
    public void shouldDisplayName_firstname_lastname_null() {
        UserDetails userDetails = new UserDetails("username", "", Collections.emptyList());
        userDetails.setFirstname(null);
        userDetails.setLastname(null);
        userDetails.setEmail("test@test.com");

        Assert.assertEquals("test@test.com", userDetails.getDisplayName());
    }

    @Test
    public void shouldDisplayName_firstname_lastname_empty() {
        UserDetails userDetails = new UserDetails("username", "", Collections.emptyList());
        userDetails.setFirstname("");
        userDetails.setLastname("");
        userDetails.setEmail("test@test.com");

        Assert.assertEquals("test@test.com", userDetails.getDisplayName());
    }

    @Test
    public void shouldDisplayName_email_null() {
        UserDetails userDetails = new UserDetails("username", "", Collections.emptyList());
        userDetails.setFirstname(null);
        userDetails.setLastname(null);
        userDetails.setEmail(null);
        userDetails.setSourceId("username");

        Assert.assertEquals("username", userDetails.getDisplayName());
    }

    @Test
    public void shouldDisplayName_email_empty() {
        UserDetails userDetails = new UserDetails("username", "", Collections.emptyList());
        userDetails.setFirstname("");
        userDetails.setLastname("");
        userDetails.setEmail("");
        userDetails.setSourceId("username");

        Assert.assertEquals("username", userDetails.getDisplayName());
    }
}
