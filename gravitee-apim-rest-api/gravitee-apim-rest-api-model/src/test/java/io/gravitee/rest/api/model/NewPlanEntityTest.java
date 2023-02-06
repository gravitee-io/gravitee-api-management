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

public class NewPlanEntityTest {

    @Test
    public void setNameShouldSanitizeInput() {
        NewPlanEntity newPlanEntity = new NewPlanEntity();
        newPlanEntity.setName("A Test Plan");
        assertEquals("A Test Plan", newPlanEntity.getName());

        newPlanEntity.setName("A <img src=\"../../../image.png\">Test Plan");
        assertEquals("A Test Plan", newPlanEntity.getName());

        newPlanEntity.setName("A Test <script>alert()</script>Plan");
        assertEquals("A Test Plan", newPlanEntity.getName());

        newPlanEntity.setName("<h1>A Test</h1> Plan");
        assertEquals("A Test Plan", newPlanEntity.getName());

        newPlanEntity.setName("A <a href=\"https://www.gravitee.io\">Test</a> Plan");
        assertEquals("A Test Plan", newPlanEntity.getName());
    }

    @Test
    public void setDescriptionShouldSanitizeInput() {
        NewPlanEntity newPlanEntity = new NewPlanEntity();
        newPlanEntity.setDescription("A Test Plan");
        assertEquals("A Test Plan", newPlanEntity.getDescription());

        newPlanEntity.setDescription("A <img src=\"../../../image.png\">Test Plan");
        assertEquals("A Test Plan", newPlanEntity.getDescription());

        newPlanEntity.setDescription("A Test <script>alert()</script>Plan");
        assertEquals("A Test Plan", newPlanEntity.getDescription());

        newPlanEntity.setDescription("<h1>A Test</h1> Plan");
        assertEquals("A Test Plan", newPlanEntity.getDescription());

        newPlanEntity.setDescription("A <a href=\"https://www.gravitee.io\">Test</a> Plan");
        assertEquals("A Test Plan", newPlanEntity.getDescription());
    }
}
