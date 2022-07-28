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
package io.gravitee.rest.api.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class NewExternalUserEntityTest {

    @Test
    public void setFirstnameShouldEscapeInput() {
        NewExternalUserEntity newExternalUserEntity = new NewExternalUserEntity();
        newExternalUserEntity.setFirstname("firstname");
        assertEquals("firstname", newExternalUserEntity.getFirstname());

        newExternalUserEntity.setFirstname("<img src=\"../../../image.png\">");
        assertEquals("&lt;img src=&quot;../../../image.png&quot;&gt;", newExternalUserEntity.getFirstname());

        newExternalUserEntity.setFirstname("<script>alert()</script>");
        assertEquals("&lt;script&gt;alert()&lt;/script&gt;", newExternalUserEntity.getFirstname());
    }

    @Test
    public void setLastnameShouldEscapeInput() {
        NewExternalUserEntity newExternalUserEntity = new NewExternalUserEntity();
        newExternalUserEntity.setLastname("lastname");
        assertEquals("lastname", newExternalUserEntity.getLastname());

        newExternalUserEntity.setLastname("<img src=\"../../../image.png\">");
        assertEquals("&lt;img src=&quot;../../../image.png&quot;&gt;", newExternalUserEntity.getLastname());

        newExternalUserEntity.setLastname("<script>alert()</script>");
        assertEquals("&lt;script&gt;alert()&lt;/script&gt;", newExternalUserEntity.getLastname());
    }
}
