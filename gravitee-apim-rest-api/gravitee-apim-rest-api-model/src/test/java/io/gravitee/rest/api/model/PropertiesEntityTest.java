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

import io.gravitee.definition.model.Properties;
import java.util.List;
import org.junit.Test;

public class PropertiesEntityTest {

    @Test
    public void should_convert_to_definition_model_properties() {
        PropertiesEntity propertiesEntity = new PropertiesEntity(
            List.of(
                new PropertyEntity("key1", "value1", true, false),
                new PropertyEntity("key2", "value2", false, true),
                new PropertyEntity("key3", "value3", true, true)
            )
        );

        Properties properties = propertiesEntity.toDefinition();

        assertEquals(propertiesEntity.getProperties(), properties.getProperties());
    }
}
