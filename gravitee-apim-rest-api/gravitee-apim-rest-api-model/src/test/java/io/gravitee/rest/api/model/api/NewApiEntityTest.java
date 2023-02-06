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
package io.gravitee.rest.api.model.api;

import static org.junit.Assert.*;

import org.junit.Test;

public class NewApiEntityTest {

    @Test
    public void setNameShouldSanitizeInput() {
        NewApiEntity newApiEntity = new NewApiEntity();
        newApiEntity.setName("A Test API");
        assertEquals("A Test API", newApiEntity.getName());

        newApiEntity.setName("A <img src=\"../../../image.png\">Test API");
        assertEquals("A Test API", newApiEntity.getName());

        newApiEntity.setName("A Test <script>alert()</script>API");
        assertEquals("A Test API", newApiEntity.getName());

        newApiEntity.setName("<h1>A Test</h1> API");
        assertEquals("A Test API", newApiEntity.getName());

        newApiEntity.setName("A <a href=\"https://www.gravitee.io\">Test</a> API");
        assertEquals("A Test API", newApiEntity.getName());
    }
}
