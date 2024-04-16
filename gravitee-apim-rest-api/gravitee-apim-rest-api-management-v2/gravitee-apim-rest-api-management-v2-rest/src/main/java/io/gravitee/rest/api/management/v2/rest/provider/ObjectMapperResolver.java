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
package io.gravitee.rest.api.management.v2.rest.provider;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.rest.api.management.v2.rest.model.ErrorDetailsInner;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Provider
public class ObjectMapperResolver implements ContextResolver<ObjectMapper> {

    private final ObjectMapper mapper;

    public ObjectMapperResolver() {
        mapper = new GraviteeMapper();
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.registerModule(
            new SimpleModule()
                // Ser & Deser for ErrorDetailsInner. Avoid to have JsonNullable "present" parameter in the response.
                .addSerializer(new ErrorDetailsInnerSerializer(ErrorDetailsInner.class))
                .addDeserializer(ErrorDetailsInner.class, new ErrorDetailsInnerDeserializer(ErrorDetailsInner.class))
        );
    }

    @Override
    public ObjectMapper getContext(Class<?> type) {
        return mapper;
    }

    public static class ErrorDetailsInnerSerializer extends StdScalarSerializer<ErrorDetailsInner> {

        protected ErrorDetailsInnerSerializer(Class<ErrorDetailsInner> t) {
            super(t);
        }

        @Override
        public void serialize(ErrorDetailsInner value, JsonGenerator gen, SerializerProvider serializers) throws java.io.IOException {
            gen.writeStartObject();
            if (value.getMessage() != null) {
                gen.writeStringField("message", value.getMessage());
            }
            if (value.getInvalidValue() != null) {
                gen.writeObjectField("invalidValue", value.getInvalidValue());
            }
            if (value.getLocation() != null) {
                gen.writeStringField("location", value.getLocation());
            }
            gen.writeEndObject();
        }
    }

    public static class ErrorDetailsInnerDeserializer extends StdScalarDeserializer<ErrorDetailsInner> {

        protected ErrorDetailsInnerDeserializer(Class<?> vc) {
            super(vc);
        }

        @Override
        public ErrorDetailsInner deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            ErrorDetailsInner errorDetailsInner = new ErrorDetailsInner();
            JsonNode node = p.getCodec().readTree(p);
            if (node.has("message")) {
                errorDetailsInner.setMessage(node.get("message").asText());
            }
            if (node.has("invalidValue")) {
                errorDetailsInner.setInvalidValue(node.get("invalidValue"));
            }
            if (node.has("location")) {
                errorDetailsInner.setLocation(node.get("location").asText());
            }
            return errorDetailsInner;
        }
    }
}
