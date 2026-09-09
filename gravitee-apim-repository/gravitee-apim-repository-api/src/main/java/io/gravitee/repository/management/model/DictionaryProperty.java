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
package io.gravitee.repository.management.model;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.io.IOException;

/**
 * A single dictionary property value, carrying whether it is currently stored encrypted.
 * Dual-reads a legacy bare-string value (pre-encryption-support dictionaries) as
 * {@code encrypted=false}. Symmetrically, an unencrypted value serialises back to that same
 * bare string — not the typed object — so that nothing on the wire (event payloads in
 * particular) changes shape until a value is genuinely encrypted; a consumer that only
 * understands the legacy shape keeps working for as long as encryption itself doesn't exist.
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
            String value = node.hasNonNull("value") ? node.get("value").asText() : null;
            boolean encrypted = node.hasNonNull("encrypted") && node.get("encrypted").asBoolean();
            return new DictionaryProperty(value, encrypted);
        }
    }
}
