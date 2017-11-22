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
package io.gravitee.definition.jackson.datatype.services.discovery.deser.consul;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import io.gravitee.definition.model.services.discovery.consul.ConsulEndpointDiscoveryConfiguration;

import java.io.IOException;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ConsulEndpointDiscoveryProviderConfigurationDeserializer extends StdScalarDeserializer<ConsulEndpointDiscoveryConfiguration> {

    public ConsulEndpointDiscoveryProviderConfigurationDeserializer(Class<ConsulEndpointDiscoveryConfiguration> vc) {
        super(vc);
    }

    @Override
    public ConsulEndpointDiscoveryConfiguration deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);

        ConsulEndpointDiscoveryConfiguration configuration = new ConsulEndpointDiscoveryConfiguration();

        final JsonNode urlNode = node.get("url");
        if (urlNode != null) {
            configuration.setUrl(urlNode.asText());
        } else {
            throw ctxt.mappingException("[discovery] [Consul.io] 'URL' is required");
        }

        JsonNode serviceNode = node.get("service");
        if (serviceNode != null) {
            String service = serviceNode.asText();
            configuration.setService(service);
        } else {
            throw ctxt.mappingException("[discovery] [Consul.io] 'Service' is required");
        }

        JsonNode aclNode = node.get("acl");
        if (aclNode != null) {
            String acl = aclNode.asText();
            configuration.setAcl(acl);
        }

        JsonNode dcNode = node.get("dc");
        if (dcNode != null) {
            String dc = dcNode.asText();
            configuration.setDc(dc);
        }

        return configuration;
    }
}