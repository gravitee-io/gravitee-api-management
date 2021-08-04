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
package io.gravitee.rest.api.common;

import static org.junit.Assert.*;

import io.gravitee.rest.api.service.common.UuidString;
import org.junit.Test;

/**
 * @author GraviteeSource Team
 */
public class UuidStringTest {

    @Test
    public void generateForEnvironment_should_generate_same_uuids_if_called_with_same_fields() {
        String firstUuid = UuidString.generateForEnvironment("environmentId", "field1", "field2", "field3");
        String secondUuid = UuidString.generateForEnvironment("environmentId", "field1", "field2", "field3");

        assertEquals(firstUuid, secondUuid);
    }

    @Test
    public void generateForEnvironment_should_generate_different_uuids_if_called_with_different_fields() {
        String firstUuid = UuidString.generateForEnvironment("environmentId", "field1", "field2", "field3");
        String secondUuid = UuidString.generateForEnvironment("environmentId", "field1", "field2", "field4");

        assertNotEquals(firstUuid, secondUuid);
    }

    @Test
    public void generateForEnvironment_should_generate_different_uuid_if_called_with_null_fields() {
        String firstUuid = UuidString.generateForEnvironment("environmentId", "field1", "field2", null);
        String secondUuid = UuidString.generateForEnvironment("environmentId", "field1", "field2", null);

        assertNotEquals(firstUuid, secondUuid);
    }
}
