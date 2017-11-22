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
package io.gravitee.definition.jackson.datatype.api.deser.endpoint;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.definition.jackson.datatype.api.deser.EndpointDeserializer;
import io.gravitee.definition.model.HttpClientOptions;
import io.gravitee.definition.model.HttpClientSslOptions;
import io.gravitee.definition.model.HttpProxy;
import io.gravitee.definition.model.endpoint.HttpEndpoint;
import io.gravitee.definition.model.services.healthcheck.EndpointHealthCheckService;

import java.io.IOException;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HttpEndpointDeserializer extends EndpointDeserializer<HttpEndpoint> {
    public HttpEndpointDeserializer(Class<HttpEndpoint> vc) {
        super(vc);
    }

    @Override
    public HttpEndpoint deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);

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

        HttpEndpoint endpoint = new HttpEndpoint(name, target);
        deserialize(endpoint, jp, node, ctxt);

        JsonNode httpProxyNode = node.get("proxy");
        if (httpProxyNode != null) {
            HttpProxy httpProxy = httpProxyNode.traverse(jp.getCodec()).readValueAs(HttpProxy.class);
            endpoint.setHttpProxy(httpProxy);
        }

        JsonNode httpClientOptionsNode = node.get("http");
        if (httpClientOptionsNode != null) {
            HttpClientOptions httpClientOptions = httpClientOptionsNode.traverse(jp.getCodec()).readValueAs(HttpClientOptions.class);
            endpoint.setHttpClientOptions(httpClientOptions);
        } else {
            endpoint.setHttpClientOptions(new HttpClientOptions());
        }

        JsonNode httpClientSslOptionsNode = node.get("ssl");
        if (httpClientSslOptionsNode != null) {
            HttpClientSslOptions httpClientSslOptions = httpClientSslOptionsNode.traverse(jp.getCodec()).readValueAs(HttpClientSslOptions.class);
            endpoint.setHttpClientSslOptions(httpClientSslOptions);
        }

        JsonNode hostHeaderNode = node.get("hostHeader");
        if (hostHeaderNode != null) {
            String hostHeader = hostHeaderNode.asText();
            if (! hostHeader.trim().isEmpty()) {
                endpoint.setHostHeader(hostHeader);
            }
        }

        JsonNode healthcheckNode = node.get("healthcheck");
        if (healthcheckNode != null && healthcheckNode.isObject()) {
            EndpointHealthCheckService healthCheckService = healthcheckNode.traverse(jp.getCodec()).readValueAs(EndpointHealthCheckService.class);
            endpoint.setHealthCheck(healthCheckService);
        }

        return endpoint;
    }
}
