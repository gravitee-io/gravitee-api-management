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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.Endpoint;
import io.gravitee.definition.model.ssl.pem.PEMTrustStore;
import java.io.IOException;
import java.util.*;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class EndpointDeserializer extends StdScalarDeserializer<Endpoint> {

    private final GraviteeMapper mapper;

    public EndpointDeserializer(Class<Endpoint> vc, GraviteeMapper mapper) {
        super(vc);
        this.mapper = mapper;
    }

    @Override
    public Endpoint deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
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

        JsonNode typeNode = node.get("type");
        Endpoint endpoint = new Endpoint(typeNode != null ? typeNode.asText() : null, name, target);

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
            tenantsNode
                .elements()
                .forEachRemaining(
                    tenantNode -> {
                        tenants.add(tenantNode.asText());
                    }
                );

            endpoint.setTenants(tenants);
        }

        // Manage retro compatibility before SME...
        JsonNode healthcheckNode = node.get("healthcheck");
        if (healthcheckNode != null && !healthcheckNode.isObject()) {
            ((ObjectNode) node).remove("healthcheck");
        }

        JsonNode headersNode = node.get("headers");
        if (headersNode != null && !headersNode.isEmpty(null)) {
            if (headersNode.isObject()) {
                Map<String, String> headers = headersNode
                    .traverse(ctxt.getParser().getCodec())
                    .readValueAs(new TypeReference<HashMap<String, String>>() {});
                ArrayNode headersUpdated = mapper.createArrayNode();
                headers
                    .keySet()
                    .forEach(
                        key -> {
                            ObjectNode objectNode = mapper.createObjectNode();
                            objectNode.put("name", key);
                            objectNode.put("value", headers.get(key));
                            headersUpdated.add(objectNode);
                        }
                    );

                ((ObjectNode) node).set("headers", headersUpdated);
            }
        }

        JsonNode hostHeaderNode = node.get("hostHeader");
        if (hostHeaderNode != null) {
            String hostHeader = hostHeaderNode.asText();
            if (!hostHeader.trim().isEmpty()) {
                headersNode = node.get("headers");
                if (headersNode == null) {
                    headersNode = mapper.createArrayNode();
                    ((ObjectNode) node).set("headers", headersNode);
                }
                ObjectNode hh = mapper.createObjectNode();
                hh.put("name", "Host");
                hh.put("value", hostHeader);
                ((ArrayNode) headersNode).add(hh);
            }
            ((ObjectNode) node).remove("hostHeader");
        }

        // Ensure backward compatibility with Gravitee.io < 1.20
        JsonNode sslNode = node.get("ssl");
        if (sslNode != null) {
            JsonNode pemNode = sslNode.get("pem");
            if (pemNode != null) {
                String pemValue = pemNode.asText();
                if (pemValue != null && !pemValue.equals("null")) {
                    ObjectNode truststoreNode = mapper.createObjectNode();
                    truststoreNode.put("type", "PEM");
                    truststoreNode.put("content", pemValue);
                    ((ObjectNode) sslNode).set("trustStore", truststoreNode);
                    ((ObjectNode) sslNode).remove("pem");
                }
            }
        }

        JsonNode inheritNode = node.get("inherit");
        if (inheritNode != null) {
            endpoint.setInherit(inheritNode.asBoolean());
        }

        // For extendable connector architecture, preserve the whole endpoint's configuration inlined
        endpoint.setConfiguration(node.toString());

        return endpoint;
    }

    @Override
    public Object deserializeWithType(JsonParser p, DeserializationContext ctxt, TypeDeserializer typeDeserializer) throws IOException {
        return this.deserialize(p, ctxt);
    }
}
