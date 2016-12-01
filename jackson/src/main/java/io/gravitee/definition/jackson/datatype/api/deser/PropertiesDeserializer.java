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
package io.gravitee.definition.jackson.datatype.api.deser;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import io.gravitee.definition.model.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PropertiesDeserializer extends StdScalarDeserializer<Properties> {

    public PropertiesDeserializer(Class<Properties> vc) {
        super(vc);
    }

    @Override
    public Properties deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);

        Properties properties = new Properties();
        List<Property> values = new ArrayList<>();

        if (node.isArray()) {
            node.elements().forEachRemaining(jsonNode -> {
                try {
                    Property property = jsonNode.traverse(jp.getCodec()).readValueAs(Property.class);
                    values.add(property);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } else if (node.isObject()) {
            node.fields().forEachRemaining(jsonNode ->
                    values.add(new Property(jsonNode.getKey(), jsonNode.getValue().asText()))
            );
        }

        properties.setProperties(values);

        return properties;
    }
}