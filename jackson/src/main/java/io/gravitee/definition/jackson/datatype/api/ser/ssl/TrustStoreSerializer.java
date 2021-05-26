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
package io.gravitee.definition.jackson.datatype.api.ser.ssl;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;
import io.gravitee.definition.model.ssl.TrustStore;
import java.io.IOException;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class TrustStoreSerializer<T extends TrustStore> extends StdScalarSerializer<T> {

    public TrustStoreSerializer(Class<T> t) {
        super(t);
    }

    @Override
    public void serialize(T trustStore, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        jgen.writeStartObject();
        doSerialize(trustStore, jgen, provider);
        jgen.writeEndObject();
    }

    @Override
    public void serializeWithType(T value, JsonGenerator g, SerializerProvider provider, TypeSerializer typeSer) throws IOException {
        serialize(value, g, provider);
    }

    protected void doSerialize(T trustStore, JsonGenerator jgen, SerializerProvider serializerProvider) throws IOException {
        jgen.writeStringField("type", trustStore.getType().name());
    }

    protected void writeStringField(JsonGenerator jgen, String field, String value) throws IOException {
        if (value != null && !value.isEmpty()) {
            jgen.writeStringField(field, value);
        }
    }
}
