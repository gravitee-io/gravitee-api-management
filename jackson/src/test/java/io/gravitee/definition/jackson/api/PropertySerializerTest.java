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
package io.gravitee.definition.jackson.api;

import io.gravitee.definition.jackson.AbstractTest;
import io.gravitee.definition.model.Property;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

/**
 * @author GraviteeSource Team
 */
public class PropertySerializerTest extends AbstractTest {

    @Test
    public void serialize() throws Exception {
        Property property = new Property("key1", "myvalue", true);
        String generatedJsonDefinition = objectMapper().writeValueAsString(property);
        JSONAssert.assertEquals(
            "{\"key\" : \"key1\", \"value\" : \"myvalue\", \"encrypted\" : true}",
            generatedJsonDefinition,
            JSONCompareMode.STRICT
        );
    }
}
