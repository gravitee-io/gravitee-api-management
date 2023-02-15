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

import static org.junit.Assert.*;

import org.junit.Test;

public class NewApplicationEntityTest {

    @Test
    public void setNameShouldSanitizeInput() {
        NewApplicationEntity newApplicationEntity = new NewApplicationEntity();
        newApplicationEntity.setName("A Test Application");
        assertEquals("A Test Application", newApplicationEntity.getName());

        newApplicationEntity.setName("A <img src=\"../../../image.png\">Test Application");
        assertEquals("A Test Application", newApplicationEntity.getName());

        newApplicationEntity.setName("A Test <script>alert()</script>Application");
        assertEquals("A Test Application", newApplicationEntity.getName());

        newApplicationEntity.setName("<h1>A Test</h1> Application");
        assertEquals("A Test Application", newApplicationEntity.getName());

        newApplicationEntity.setName("A <a href=\"https://www.gravitee.io\">Test</a> Application");
        assertEquals("A Test Application", newApplicationEntity.getName());
    }

    @Test
    public void setDescriptionShouldSanitizeInput() {
        NewApplicationEntity newApplicationEntity = new NewApplicationEntity();
        newApplicationEntity.setDescription("A Test Application");
        assertEquals("A Test Application", newApplicationEntity.getDescription());

        newApplicationEntity.setDescription("A <img src=\"../../../image.png\">Test Application");
        assertEquals("A Test Application", newApplicationEntity.getDescription());

        newApplicationEntity.setDescription("A Test <script>alert()</script>Application");
        assertEquals("A Test Application", newApplicationEntity.getDescription());

        newApplicationEntity.setDescription("<h1>A Test</h1> Application");
        assertEquals("A Test Application", newApplicationEntity.getDescription());

        newApplicationEntity.setDescription("A <a href=\"https://www.gravitee.io\">Test</a> Application");
        assertEquals("A Test Application", newApplicationEntity.getDescription());
    }
}
