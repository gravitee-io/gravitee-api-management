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
package io.gravitee.definition.jackson.datatype.services.core.deser;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import io.gravitee.definition.model.Service;
import java.io.IOException;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class ServiceDeserializer<T extends Service> extends StdScalarDeserializer<T> {

    public ServiceDeserializer(Class<T> vc) {
        super(vc);
    }

    @Override
    public T deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);

        try {
            T service = createServiceInstance();

            deserialize(service, jsonParser, node, deserializationContext);

            return service;
        } catch (Exception ex) {
            throw deserializationContext.mappingException(ex.getMessage());
        }
    }

    protected void deserialize(T service, JsonParser jsonParser, JsonNode node, DeserializationContext ctxt) throws IOException {
        final JsonNode serviceEnabledNode = node.get("enabled");
        if (serviceEnabledNode != null) {
            service.setEnabled(serviceEnabledNode.asBoolean(false));
        } else {
            service.setEnabled(true);
        }
    }

    private T createServiceInstance() throws Exception {
        Class<? extends Service> serviceClass = (Class<? extends Service>) this.handledType();
        return (T) serviceClass.newInstance();
    }
}
