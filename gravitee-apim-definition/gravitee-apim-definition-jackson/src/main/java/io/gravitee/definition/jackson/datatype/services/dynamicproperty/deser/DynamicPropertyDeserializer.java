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
package io.gravitee.definition.jackson.datatype.services.dynamicproperty.deser;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.definition.jackson.datatype.services.core.deser.ScheduledServiceDeserializer;
import io.gravitee.definition.model.services.dynamicproperty.DynamicPropertyProvider;
import io.gravitee.definition.model.services.dynamicproperty.DynamicPropertyService;
import io.gravitee.definition.model.services.dynamicproperty.http.HttpDynamicPropertyProviderConfiguration;
import java.io.IOException;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DynamicPropertyDeserializer extends ScheduledServiceDeserializer<DynamicPropertyService> {

    public DynamicPropertyDeserializer(Class<DynamicPropertyService> vc) {
        super(vc);
    }

    @Override
    protected void deserialize(DynamicPropertyService service, JsonParser jsonParser, JsonNode node, DeserializationContext ctxt)
        throws IOException, JsonProcessingException {
        super.deserialize(service, jsonParser, node, ctxt);

        final JsonNode providerNode = node.get("provider");
        if (providerNode != null) {
            service.setProvider(DynamicPropertyProvider.valueOf(providerNode.asText().toUpperCase()));
        } else {
            throw ctxt.mappingException("[dynamic-property] Provider is required");
        }

        final JsonNode configurationNode = node.get("configuration");
        if (service.getProvider() == DynamicPropertyProvider.HTTP) {
            HttpDynamicPropertyProviderConfiguration configuration = configurationNode
                .traverse(jsonParser.getCodec())
                .readValueAs(HttpDynamicPropertyProviderConfiguration.class);
            service.setConfiguration(configuration);
        }
    }
}
