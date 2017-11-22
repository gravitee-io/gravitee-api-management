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
import io.gravitee.definition.model.*;
import io.gravitee.definition.model.endpoint.HttpEndpoint;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

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
            Set<Endpoint> endpoints = new LinkedHashSet<>(nodeEndpoints.size());
            for (JsonNode jsonNode : nodeEndpoints) {
                EndpointType type = EndpointType.valueOf(
                        jsonNode.path("type").asText(EndpointType.HTTP.name()).toUpperCase());

                Endpoint endpoint;
                switch (type) {
                    case HTTP:
                        endpoint = jsonNode.traverse(jp.getCodec()).readValueAs(HttpEndpoint.class);
                        break;
                    default:
                        endpoint = jsonNode.traverse(jp.getCodec()).readValueAs(HttpEndpoint.class);
                        break;
                }

                if (endpoint != null) {
                    boolean added = endpoints.add(endpoint);
                    if (!added) {
                        throw ctxt.mappingException("[api] API must have single endpoint names");
                    }
                }
            }

            proxy.setEndpoints(endpoints);
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

        JsonNode loggingNode = node.get("loggingMode");
        if (loggingNode != null) {
            proxy.setLoggingMode(LoggingMode.valueOf(loggingNode.asText().toUpperCase()));
        } else {
            proxy.setLoggingMode(Proxy.DEFAULT_LOGGING_MODE);
        }

        JsonNode multiTenantNode = node.get("multiTenant");
        if (multiTenantNode != null) {
            boolean multiTenant = multiTenantNode.asBoolean(Proxy.DEFAULT_MULTI_TENANT);
            proxy.setMultiTenant(multiTenant);
        } else {
            proxy.setMultiTenant(Proxy.DEFAULT_MULTI_TENANT);
        }

        JsonNode corsNode = node.get("cors");
        if (corsNode != null) {
            Cors cors = corsNode.traverse(jp.getCodec()).readValueAs(Cors.class);
            proxy.setCors(cors);
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