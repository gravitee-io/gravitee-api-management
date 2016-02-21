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
package io.gravitee.definition.jackson.datatype.deser;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import io.gravitee.definition.model.*;

import java.io.IOException;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 * @author Gravitee.io Team
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
            proxy.setContextPath(contextPath.asText());
        }

        final JsonNode nodeEndpoint = node.get("endpoint");
        final JsonNode nodeEndpoints = node.get("endpoints");

        if (nodeEndpoint != null) {
            // This is just for backward compatibility with version < 0.6
            // Must be deleted for major release
            Endpoint singleEndpoint = new Endpoint();
            singleEndpoint.setTarget(nodeEndpoint.asText());
            singleEndpoint.setWeight(Endpoint.DEFAULT_WEIGHT);
            proxy.getEndpoints().add(singleEndpoint);
        } else {
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
        }

        JsonNode stripContextNode = node.get("strip_context_path");
        if (stripContextNode != null) {
            proxy.setStripContextPath(stripContextNode.asBoolean(false));
        }

        JsonNode httpClientNode = node.get("http");
        if (httpClientNode != null) {
            HttpClient httpClient = httpClientNode.traverse(jp.getCodec()).readValueAs(HttpClient.class);
            proxy.setHttpClient(httpClient);
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

        return proxy;
    }
}