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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import io.gravitee.rest.api.model.PropertyEntity;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author GraviteeSource Team
 */
public class PropertiesEntityAsListDeserializer extends StdScalarDeserializer<List<PropertyEntity>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PropertiesEntityAsListDeserializer.class);

    public PropertiesEntityAsListDeserializer() {
        super(List.class);
    }

    @Override
    public List<PropertyEntity> deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);

        List<PropertyEntity> properties = new ArrayList<>();

        if (node.isArray()) {
            node
                .elements()
                .forEachRemaining(
                    jsonNode -> {
                        try {
                            PropertyEntity property = jsonNode.traverse(jp.getCodec()).readValueAs(PropertyEntity.class);
                            properties.add(property);
                        } catch (IOException e) {
                            LOGGER.error("Error while deserializing API's properties list", e);
                        }
                    }
                );
        } else if (node.isObject()) {
            node.fields().forEachRemaining(jsonNode -> properties.add(new PropertyEntity(jsonNode.getKey(), jsonNode.getValue().asText())));
        }

        return properties;
    }
}
