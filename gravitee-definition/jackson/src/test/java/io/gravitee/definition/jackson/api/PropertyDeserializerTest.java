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

import static org.junit.Assert.assertEquals;

import io.gravitee.definition.jackson.AbstractTest;
import io.gravitee.definition.model.Property;
import org.junit.Test;

/**
 * @author GraviteeSource Team
 */
public class PropertyDeserializerTest extends AbstractTest {

    @Test
    public void deserialize() throws Exception {
        Property resultProperty = objectMapper()
            .readValue("{\"key\" : \"key1\", \"value\" : \"myvalue\", \"encrypted\" : true}", Property.class);

        Property expectedProperty = new Property("key1", "myvalue", true);

        assertEquals(expectedProperty, resultProperty);
    }
}
