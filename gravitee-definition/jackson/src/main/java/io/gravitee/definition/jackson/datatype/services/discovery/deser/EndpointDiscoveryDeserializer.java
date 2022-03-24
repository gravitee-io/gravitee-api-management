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
package io.gravitee.definition.jackson.datatype.services.discovery.deser;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.definition.jackson.datatype.services.core.deser.ServiceDeserializer;
import io.gravitee.definition.jackson.datatype.services.discovery.EndpointDiscoveryProviderMapper;
import io.gravitee.definition.model.services.discovery.EndpointDiscoveryService;

import java.io.IOException;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class EndpointDiscoveryDeserializer extends ServiceDeserializer<EndpointDiscoveryService> {

    public EndpointDiscoveryDeserializer(Class<EndpointDiscoveryService> vc) {
        super(vc);
    }

    @Override
    protected void deserialize(EndpointDiscoveryService service, JsonParser jsonParser, JsonNode node, DeserializationContext ctxt) throws IOException {
        super.deserialize(service, jsonParser, node, ctxt);

        if (service.isEnabled()) {
            final JsonNode providerNode = node.get("provider");
            if (providerNode != null) {
                String provider = providerNode.asText().toUpperCase();
                String providerPlugin = EndpointDiscoveryProviderMapper.getProvider(provider);
                service.setProvider(providerPlugin);
            } else {
                throw ctxt.mappingException("[endpoint-discovery] Provider is required");
            }

            service.setConfiguration(node.get("configuration").toString());
        }
    }
}