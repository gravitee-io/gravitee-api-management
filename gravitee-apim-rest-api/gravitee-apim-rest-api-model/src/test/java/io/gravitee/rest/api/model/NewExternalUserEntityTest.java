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
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class NewExternalUserEntityTest {

    @Test
    public void setFirstnameShouldEscapeInput() {
        NewExternalUserEntity newExternalUserEntity = new NewExternalUserEntity();
        newExternalUserEntity.setFirstname("A Test Firstname");
        assertEquals("A Test Firstname", newExternalUserEntity.getFirstname());

        newExternalUserEntity.setFirstname("A <img src=\"../../../image.png\">Test Firstname");
        assertEquals("A Test Firstname", newExternalUserEntity.getFirstname());

        newExternalUserEntity.setFirstname("A Test <script>alert()</script>Firstname");
        assertEquals("A Test Firstname", newExternalUserEntity.getFirstname());

        newExternalUserEntity.setFirstname("A <h1>Test</h1> Firstname");
        assertEquals("A Test Firstname", newExternalUserEntity.getFirstname());

        newExternalUserEntity.setFirstname("A <a href=\"https://www.gravitee.io\">Test</a> Firstname");
        assertEquals("A Test Firstname", newExternalUserEntity.getFirstname());

        newExternalUserEntity.setFirstname("A Firstname Wïth Accènt");
        assertEquals("A Firstname Wïth Accènt", newExternalUserEntity.getFirstname());

        newExternalUserEntity.setFirstname(null);
        assertNull(newExternalUserEntity.getFirstname());
    }

    @Test
    public void setLastnameShouldEscapeInput() {
        NewExternalUserEntity newExternalUserEntity = new NewExternalUserEntity();
        newExternalUserEntity.setLastname("A Test Lastname");
        assertEquals("A Test Lastname", newExternalUserEntity.getLastname());

        newExternalUserEntity.setLastname("A <img src=\"../../../image.png\">Test Lastname");
        assertEquals("A Test Lastname", newExternalUserEntity.getLastname());

        newExternalUserEntity.setLastname("A Test <script>alert()</script>Lastname");
        assertEquals("A Test Lastname", newExternalUserEntity.getLastname());

        newExternalUserEntity.setLastname("A <h1>Test</h1> Lastname");
        assertEquals("A Test Lastname", newExternalUserEntity.getLastname());

        newExternalUserEntity.setLastname("A <a href=\"https://www.gravitee.io\">Test</a> Lastname");
        assertEquals("A Test Lastname", newExternalUserEntity.getLastname());

        newExternalUserEntity.setLastname("A Lastname Wïth Accènt");
        assertEquals("A Lastname Wïth Accènt", newExternalUserEntity.getLastname());

        newExternalUserEntity.setLastname(null);
        assertNull(newExternalUserEntity.getLastname());
    }
}
