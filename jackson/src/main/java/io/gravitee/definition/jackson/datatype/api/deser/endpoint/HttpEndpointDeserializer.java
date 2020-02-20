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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.common.http.HttpHeaders;
import io.gravitee.definition.jackson.datatype.api.deser.EndpointDeserializer;
import io.gravitee.definition.model.HttpClientOptions;
import io.gravitee.definition.model.HttpClientSslOptions;
import io.gravitee.definition.model.HttpProxy;
import io.gravitee.definition.model.endpoint.HttpEndpoint;
import io.gravitee.definition.model.services.healthcheck.EndpointHealthCheckService;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Boolean.FALSE;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HttpEndpointDeserializer<T extends HttpEndpoint> extends EndpointDeserializer<T> {

    public HttpEndpointDeserializer(Class<T> vc) {
        super(vc);
    }

    @Override
    protected T createEndpoint(String name, String target) {
        return (T) new HttpEndpoint(name, target);
    }

    @Override
    protected T deserialize(T endpoint, JsonNode node, DeserializationContext ctxt) throws IOException {
        JsonNode healthcheckNode = node.get("healthcheck");
        if (healthcheckNode != null && healthcheckNode.isObject()) {
            EndpointHealthCheckService healthCheckService = healthcheckNode.traverse(ctxt.getParser().getCodec()).readValueAs(EndpointHealthCheckService.class);
            endpoint.setHealthCheck(healthCheckService);
        }

        if (endpoint.getInherit() == null || endpoint.getInherit().equals(FALSE)) {
            JsonNode httpProxyNode = node.get("proxy");
            if (httpProxyNode != null) {
                HttpProxy httpProxy = httpProxyNode.traverse(ctxt.getParser().getCodec()).readValueAs(HttpProxy.class);
                endpoint.setHttpProxy(httpProxy);
            }

            JsonNode httpClientOptionsNode = node.get("http");
            if (httpClientOptionsNode != null) {
                HttpClientOptions httpClientOptions = httpClientOptionsNode.traverse(ctxt.getParser().getCodec()).readValueAs(HttpClientOptions.class);
                endpoint.setHttpClientOptions(httpClientOptions);
            } else {
                endpoint.setHttpClientOptions(new HttpClientOptions());
            }

            JsonNode httpClientSslOptionsNode = node.get("ssl");
            if (httpClientSslOptionsNode != null) {
                HttpClientSslOptions httpClientSslOptions = httpClientSslOptionsNode.traverse(ctxt.getParser().getCodec()).readValueAs(HttpClientSslOptions.class);
                endpoint.setHttpClientSslOptions(httpClientSslOptions);
            }

            JsonNode hostHeaderNode = node.get("hostHeader");
            if (hostHeaderNode != null) {
                String hostHeader = hostHeaderNode.asText();
                if (!hostHeader.trim().isEmpty()) {
                    Map<String, String> headers = new HashMap<>();
                    headers.put(HttpHeaders.HOST, hostHeader);
                    endpoint.setHeaders(headers);
                }
            }

            JsonNode headersNode = node.get("headers");
            if (headersNode != null && !headersNode.isEmpty(null)) {
                Map<String, String> headers = headersNode.traverse(ctxt.getParser().getCodec()).readValueAs(new TypeReference<HashMap<String, String>>() {
                });
                if (headers != null && !headers.isEmpty()) {
                    if (endpoint.getHeaders() == null) {
                        endpoint.setHeaders(headers);
                    } else {
                        headers.forEach(endpoint.getHeaders()::putIfAbsent);
                    }
                }
            }
        }

        return endpoint;
    }
}
