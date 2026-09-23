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
package io.gravitee.definition.model.dictionary;

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
 * A single dictionary property value, carrying whether it is currently stored encrypted.
 *
 * <p>This is the <em>single</em> definition of the dictionary-property wire contract, shared by
 * everything that serialises or deserialises it: the Management API (persistence and the event
 * payloads it publishes) and the gateway (reading those payloads, and redistributing a dictionary
 * to peer nodes via cluster/distributed sync). It lives here rather than being redeclared per
 * module because both ends must agree byte-for-byte — the gateway deserialises exactly what the
 * Management API serialised, and its sync mappers swallow a deserialisation failure as a WARN,
 * so any drift between two copies would silently stop dictionaries syncing rather than fail loudly.
 *
 * <p>Dual-reads a legacy bare-string value (written before dictionaries carried per-property
 * encryption status) as {@code encrypted=false}. Symmetrically, an unencrypted value serialises
 * back to that same bare string — not the typed object — so nothing on the wire changes shape
 * until a value is genuinely encrypted, and a consumer that only understands the legacy shape
 * keeps working for as long as encryption itself is unused.
 *
 * @author GraviteeSource Team
 */
@JsonSerialize(using = DictionaryProperty.Serializer.class)
@JsonDeserialize(using = DictionaryProperty.Deserializer.class)
public record DictionaryProperty(String value, boolean encrypted) {
    @Override
    public String toString() {
        return "DictionaryProperty[value=<redacted>, encrypted=" + encrypted + "]";
    }

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
            // Deliberately does not echo the offending node: this value can be a secret, and the
            // gateway's sync mappers log this exception.
            JsonNode value = node.get("value");
            if (value == null || !value.isTextual()) {
                throw JsonMappingException.from(p, "A dictionary property object must have a textual 'value' field");
            }
            boolean encrypted = node.hasNonNull("encrypted") && node.get("encrypted").asBoolean();
            return new DictionaryProperty(value.asText(), encrypted);
        }

        @Override
        public DictionaryProperty getNullValue(DeserializationContext ctxt) throws JsonMappingException {
            throw JsonMappingException.from(ctxt, "A dictionary property must not be null");
        }
    }
}
