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
import io.gravitee.definition.model.Property;
import java.io.IOException;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PropertyDeserializer extends AbstractStdScalarDeserializer<Property> {

    public PropertyDeserializer(Class<Property> vc) {
        super(vc);
    }

    @Override
    public Property deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);

        String key, value;
        boolean encrypted = false;

        JsonNode keyNode = node.get("key");
        if (keyNode == null) {
            throw ctxt.mappingException("Key property is required");
        } else {
            key = keyNode.asText();
        }

        JsonNode valueNode = node.get("value");
        if (valueNode == null) {
            throw ctxt.mappingException("Value property is required");
        } else {
            value = valueNode.asText();
        }

        if (node.has("encrypted")) {
            encrypted = node.get("encrypted").asBoolean(false);
        }

        Property property = new Property(key, value, encrypted);

        JsonNode isDynamicNode = node.get("dynamic");
        if (isDynamicNode != null) {
            boolean isDynamic = isDynamicNode.asBoolean(false);
            property.setDynamic(isDynamic);
        } else {
            property.setDynamic(false);
        }

        return property;
    }
}
