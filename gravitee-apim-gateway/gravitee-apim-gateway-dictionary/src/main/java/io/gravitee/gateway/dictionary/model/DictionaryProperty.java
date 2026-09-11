/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.dictionary.model;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.io.IOException;

/**
 * A single dictionary property value, as deployed to the gateway. Dual-reads a legacy
 * bare-string value (produced by a not-yet-upgraded Management API) as {@code encrypted=false}.
 * Symmetrically, an unencrypted value serialises back to that same bare string — not the typed
 * object — so redistributing a dictionary to peer gateway nodes (cluster/distributed sync)
 * doesn't change wire shape until a value is genuinely encrypted, keeping a not-yet-upgraded
 * peer node able to read it.
 *
 * @author GraviteeSource Team
 */
@JsonSerialize(using = DictionaryProperty.Serializer.class)
@JsonDeserialize(using = DictionaryProperty.Deserializer.class)
public record DictionaryProperty(String value, boolean encrypted) {
    static class Serializer extends JsonSerializer<DictionaryProperty> {

        @Override
        public void serialize(DictionaryProperty property, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            if (property.encrypted()) {
                gen.writeStartObject();
                gen.writeStringField("value", property.value());
                gen.writeBooleanField("encrypted", true);
                gen.writeEndObject();
            } else {
                gen.writeString(property.value());
            }
        }
    }

    static class Deserializer extends JsonDeserializer<DictionaryProperty> {

        @Override
        public DictionaryProperty deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonNode node = p.getCodec().readTree(p);
            if (node.isTextual()) {
                return new DictionaryProperty(node.asText(), false);
            }
            if (!node.hasNonNull("value")) {
                throw JsonMappingException.from(p, "A dictionary property object must have a non-null 'value' field");
            }
            String value = node.get("value").asText();
            boolean encrypted = node.hasNonNull("encrypted") && node.get("encrypted").asBoolean();
            return new DictionaryProperty(value, encrypted);
        }

        @Override
        public DictionaryProperty getNullValue(DeserializationContext ctxt) throws JsonMappingException {
            throw JsonMappingException.from(ctxt, "A dictionary property must not be null");
        }
    }
}
