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
import io.gravitee.definition.model.Failover;
import io.gravitee.definition.model.LoadBalancer;
import io.gravitee.definition.model.Proxy;

import java.io.IOException;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ProxyDeserializer extends StdScalarDeserializer<Proxy> {

    public ProxyDeserializer(Class<Proxy> vc) {
        super(vc);
    }

    @Override
    public Proxy deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);

        Proxy proxy = new Proxy();
        final JsonNode contextPath = node.get("context_path");
        if (contextPath != null) {
            String sContextPath = formatContextPath(contextPath.asText());
            proxy.setContextPath(sContextPath);
        } else {
            throw ctxt.mappingException("[api] API must have a valid context path");
        }

        final JsonNode nodeEndpoints = node.get("endpoints");

        if (nodeEndpoints != null && nodeEndpoints.isArray()) {
            nodeEndpoints.elements().forEachRemaining(jsonNode -> {
                try {
                    Endpoint endpoint = jsonNode.traverse(jp.getCodec()).readValueAs(Endpoint.class);
                    proxy.getEndpoints().add(endpoint);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }

        JsonNode stripContextNode = node.get("strip_context_path");
        if (stripContextNode != null) {
            proxy.setStripContextPath(stripContextNode.asBoolean(false));
        }

        JsonNode loadBalancingNode = node.get("load_balancing");
        if (loadBalancingNode != null) {
            LoadBalancer loadBalancer = loadBalancingNode.traverse(jp.getCodec()).readValueAs(LoadBalancer.class);
            proxy.setLoadBalancer(loadBalancer);
        }

        JsonNode failoverNode = node.get("failover");
        if (failoverNode != null) {
            Failover failover = failoverNode.traverse(jp.getCodec()).readValueAs(Failover.class);
            proxy.setFailover(failover);
        }

        JsonNode dumpRequestNode = node.get("dumpRequest");
        if (dumpRequestNode != null) {
            boolean dumpRequest = dumpRequestNode.asBoolean(Proxy.DEFAULT_DUMP_REQUEST);
            proxy.setDumpRequest(dumpRequest);
        } else {
            proxy.setDumpRequest(Proxy.DEFAULT_DUMP_REQUEST);
        }

        JsonNode multiTenantNode = node.get("multiTenant");
        if (multiTenantNode != null) {
            boolean multiTenant = multiTenantNode.asBoolean(Proxy.DEFAULT_MULTI_TENANT);
            proxy.setMultiTenant(multiTenant);
        } else {
            proxy.setMultiTenant(Proxy.DEFAULT_MULTI_TENANT);
        }

        return proxy;
    }

    private String formatContextPath(String contextPath) {
        String [] parts = contextPath.split("/");
        StringBuilder finalPath = new StringBuilder("/");

        for(String part : parts) {
            if (! part.isEmpty()) {
                finalPath.append(part).append('/');
            }
        }

        return finalPath.deleteCharAt(finalPath.length() - 1).toString();
    }
}