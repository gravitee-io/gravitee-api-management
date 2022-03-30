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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import io.gravitee.common.http.HttpHeader;
import io.gravitee.common.http.HttpHeaders;
import io.gravitee.definition.model.*;
import io.gravitee.definition.model.services.Services;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class EndpointGroupDeserializer extends StdScalarDeserializer<EndpointGroup> {

    private static final String DEFAULT_GROUP_NAME = "default";

    public EndpointGroupDeserializer(Class<EndpointGroup> vc) {
        super(vc);
    }

    @Override
    public EndpointGroup deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        JsonNode node = jp.getCodec().readTree(jp);

        final EndpointGroup group = new EndpointGroup();

        // Read group name
        group.setName(node.path("name").asText(DEFAULT_GROUP_NAME));

        // Read endpoints
        final JsonNode nodeEndpoints = node.get("endpoints");

        if (nodeEndpoints != null) {
            final Set<Endpoint> endpoints = new LinkedHashSet<>(nodeEndpoints.size());
            for (JsonNode jsonNode : nodeEndpoints) {
                Endpoint endpoint = jsonNode.traverse(jp.getCodec()).readValueAs(Endpoint.class);

                if (endpoint != null) {
                    boolean added = endpoints.add(endpoint);
                    if (!added) {
                        throw ctxt.mappingException("[api] API endpoint names must be unique");
                    }
                }
            }
            group.setEndpoints(endpoints);
        }

        // Read load-balancing
        JsonNode loadBalancingNode = node.get("load_balancing");
        if (loadBalancingNode != null) {
            LoadBalancer loadBalancer = loadBalancingNode.traverse(jp.getCodec()).readValueAs(LoadBalancer.class);
            group.setLoadBalancer(loadBalancer);
        }

        JsonNode servicesNode = node.get("services");
        if (servicesNode != null) {
            Services services = servicesNode.traverse(jp.getCodec()).readValueAs(Services.class);
            group.getServices().set(services.getAll());
        }

        JsonNode httpProxyNode = node.get("proxy");
        if (httpProxyNode != null) {
            HttpProxy httpProxy = httpProxyNode.traverse(jp.getCodec()).readValueAs(HttpProxy.class);
            group.setHttpProxy(httpProxy);
        }

        JsonNode httpClientOptionsNode = node.get("http");
        if (httpClientOptionsNode != null) {
            HttpClientOptions httpClientOptions = httpClientOptionsNode.traverse(jp.getCodec()).readValueAs(HttpClientOptions.class);
            group.setHttpClientOptions(httpClientOptions);
        } else {
            group.setHttpClientOptions(new HttpClientOptions());
        }

        JsonNode httpClientSslOptionsNode = node.get("ssl");
        if (httpClientSslOptionsNode != null) {
            HttpClientSslOptions httpClientSslOptions = httpClientSslOptionsNode
                .traverse(jp.getCodec())
                .readValueAs(HttpClientSslOptions.class);
            group.setHttpClientSslOptions(httpClientSslOptions);
        }

        JsonNode headersNode = node.get("headers");
        if (headersNode != null && !headersNode.isEmpty(null)) {
            if (headersNode.isArray()) {
                List<HttpHeader> headers = headersNode
                    .traverse(ctxt.getParser().getCodec())
                    .readValueAs(new TypeReference<ArrayList<HttpHeader>>() {});
                group.setHeaders(headers);
            } else {
                Map<String, String> headers = headersNode
                    .traverse(ctxt.getParser().getCodec())
                    .readValueAs(new TypeReference<HashMap<String, String>>() {});
                group.setHeaders(
                    headers.entrySet().stream().map(entry -> new HttpHeader(entry.getKey(), entry.getValue())).collect(Collectors.toList())
                );
            }
        }

        JsonNode hostHeaderNode = node.get("hostHeader");
        if (hostHeaderNode != null) {
            String hostHeader = hostHeaderNode.asText();
            if (!hostHeader.trim().isEmpty()) {
                if (group.getHeaders() == null) {
                    group.setHeaders(Collections.singletonList(new HttpHeader(HttpHeaders.HOST, hostHeader)));
                } else {
                    Optional<HttpHeader> first = group
                        .getHeaders()
                        .stream()
                        .filter(httpHeader -> HttpHeaders.HOST.equals(httpHeader.getName()))
                        .findFirst();
                    if (first.isPresent()) {
                        first.get().setValue(hostHeader);
                    } else {
                        group.getHeaders().add(new HttpHeader(HttpHeaders.HOST, hostHeader));
                    }
                }
            }
        }

        return group;
    }
}
