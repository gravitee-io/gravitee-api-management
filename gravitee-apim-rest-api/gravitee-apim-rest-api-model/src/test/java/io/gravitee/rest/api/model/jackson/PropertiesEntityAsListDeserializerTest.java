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
package io.gravitee.rest.api.model.jackson;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.gravitee.rest.api.model.PropertyEntity;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class PropertiesEntityAsListDeserializerTest {

    ObjectMapper objectMapper = new ObjectMapper();

    @Before
    public void setupDeserializer() {
        SimpleModule module = new SimpleModule();
        module.addDeserializer(List.class, new PropertiesEntityAsListDeserializer());
        objectMapper.registerModule(module);
    }

    @Test
    public void should_deserialize_empty_properties_list() throws JsonProcessingException {
        List<PropertyEntity> properties = objectMapper.readValue("[]", List.class);
        assertTrue(properties.isEmpty());
    }

    @Test
    public void should_deserialize_properties_list() throws JsonProcessingException {
        List<PropertyEntity> properties = objectMapper.readValue(
            "[" +
            "{\"key\":\"key1\", \"value\":\"value1\", \"encryptable\":\"true\", \"encrypted\":\"false\"}," +
            "{\"key\":\"key2\", \"value\":\"value2\", \"encryptable\":\"false\", \"encrypted\":\"false\"}," +
            "{\"key\":\"key3\", \"value\":\"value3\", \"encryptable\":\"true\", \"encrypted\":\"true\"}" +
            "]",
            List.class
        );

        List<PropertyEntity> expectedProperties = List.of(
            new PropertyEntity("key1", "value1", true, false),
            new PropertyEntity("key2", "value2", false, false),
            new PropertyEntity("key3", "value3", true, true)
        );

        assertEquals(expectedProperties, properties);
    }

    @Test
    public void should_deserialize_properties_object() throws JsonProcessingException {
        List<PropertyEntity> properties = objectMapper.readValue(
            "{" + "\"key1\": \"value1\"," + "\"key2\": \"value2\"," + "\"key3\": \"value3\"" + "}",
            List.class
        );

        List<PropertyEntity> expectedProperties = List.of(
            new PropertyEntity("key1", "value1"),
            new PropertyEntity("key2", "value2"),
            new PropertyEntity("key3", "value3")
        );

        assertEquals(expectedProperties, properties);
    }
}
