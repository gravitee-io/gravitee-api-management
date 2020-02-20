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
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import io.gravitee.definition.model.Endpoint;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class EndpointDeserializer<T extends Endpoint> extends StdScalarDeserializer<T> {

    public EndpointDeserializer(Class<T> vc) {
        super(vc);
    }

    @Override
    public T deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
        JsonNode node = parser.getCodec().readTree(parser);

        String name, target;

        JsonNode nameNode = node.get("name");
        if (nameNode != null) {
            name = nameNode.asText();
        } else {
            throw ctxt.mappingException("Endpoint name is required");
        }

        JsonNode targetNode = node.get("target");
        if (targetNode != null) {
            target = targetNode.asText();
        } else {
            throw ctxt.mappingException("Endpoint target is required");
        }

        T endpoint = createEndpoint(name, target);

        final JsonNode weightNode = node.get("weight");
        if (weightNode != null) {
            int weight = weightNode.asInt(Endpoint.DEFAULT_WEIGHT);
            endpoint.setWeight(weight);
        } else {
            endpoint.setWeight(Endpoint.DEFAULT_WEIGHT);
        }

        final JsonNode backupNode = node.get("backup");
        if (backupNode != null) {
            boolean backup = backupNode.asBoolean(false);
            endpoint.setBackup(backup);
        } else {
            endpoint.setBackup(false);
        }

        // Keep it for backward-compatibility
        final JsonNode singleTenantNode = node.get("tenant");
        if (singleTenantNode != null) {
            String tenant = singleTenantNode.asText();
            endpoint.setTenants(Collections.singletonList(tenant));
        }

        final JsonNode tenantsNode = node.get("tenants");
        if (tenantsNode != null && tenantsNode.isArray()) {
            List<String> tenants = new ArrayList<>(tenantsNode.size());
            tenantsNode.elements().forEachRemaining(tenantNode -> {
                tenants.add(tenantNode.asText());
            });

            endpoint.setTenants(tenants);
        }

        JsonNode inheritNode = node.get("inherit");
        if (inheritNode != null) {
            endpoint.setInherit(inheritNode.asBoolean());
        }

        deserialize(endpoint, node, ctxt);

        return endpoint;
    }

    protected abstract T createEndpoint(String name, String target);

    protected abstract T deserialize(T endpoint, JsonNode node, DeserializationContext ctxt) throws IOException;
}