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
import io.gravitee.definition.model.HttpClient;
import io.gravitee.definition.model.Proxy;

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

        final JsonNode endpoint = node.get("endpoint");
        final JsonNode endpoints = node.get("endpoints");

        if (endpoint != null) {
            if (endpoint.isArray()) {
                proxy.getEndpoints().add(endpoint.elements().next().asText());
            } else {
                proxy.getEndpoints().add(endpoint.asText());
            }
        }

        if (endpoints != null && endpoints.isArray()) {
            endpoints.elements().forEachRemaining(
                    endpointNode -> proxy.getEndpoints().add(endpointNode.asText()));
        }

        JsonNode stripContextNode = node.get("strip_context_path");
        if (stripContextNode != null) {
            proxy.setStripContextPath(stripContextNode.asBoolean(false));
        }

        JsonNode httpClientNode = node.get("http_client");
        if (httpClientNode != null) {
            HttpClient httpClient = httpClientNode.traverse(jp.getCodec()).readValueAs(HttpClient.class);
            proxy.setHttpClient(httpClient);
        }

        return proxy;
    }
}