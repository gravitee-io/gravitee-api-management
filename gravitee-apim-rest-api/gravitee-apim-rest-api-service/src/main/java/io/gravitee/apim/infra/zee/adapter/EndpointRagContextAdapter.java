/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.apim.infra.zee.adapter;

import io.gravitee.apim.core.plugin.query_service.EndpointPluginQueryService;
import io.gravitee.apim.core.zee.domain_service.RagContextStrategy;
import io.gravitee.apim.core.zee.model.ZeeResourceType;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * RAG context strategy for {@link ZeeResourceType#ENDPOINT}.
 *
 * <p>
 * Retrieves available endpoint connector plugins so the LLM knows what
 * endpoint types can be used (e.g. http-proxy, kafka, mqtt).
 *
 * @author Derek Thompson
 */
@Component
public class EndpointRagContextAdapter implements RagContextStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(EndpointRagContextAdapter.class);

    /** Maximum number of endpoint plugins to include in RAG context. */
    static final int MAX_ENDPOINTS = 20;

    private final EndpointPluginQueryService endpointPluginQueryService;

    public EndpointRagContextAdapter(EndpointPluginQueryService endpointPluginQueryService) {
        this.endpointPluginQueryService = endpointPluginQueryService;
    }

    @Override
    public ZeeResourceType resourceType() {
        return ZeeResourceType.ENDPOINT;
    }

    @Override
    public String retrieveContext(String envId, String orgId, Map<String, Object> contextData) {
        var sb = new StringBuilder();
        appendEndpoints(sb);
        return sb.toString();
    }

    private void appendEndpoints(StringBuilder sb) {
        try {
            var endpoints = endpointPluginQueryService.findAll();
            if (endpoints == null || endpoints.isEmpty()) {
                return;
            }

            sb.append("### Available Endpoint Connectors\n");
            endpoints
                    .stream()
                    .limit(MAX_ENDPOINTS)
                    .forEach(plugin -> {
                        var id = plugin.getId() != null ? plugin.getId() : "(unknown)";
                        var description = plugin.getDescription() != null ? plugin.getDescription() : "";
                        sb.append("- ").append(id).append(": ").append(description).append("\n");
                    });
        } catch (Exception e) {
            LOG.warn("Failed to retrieve available endpoint plugins for RAG context: {}", e.getMessage());
        }
    }
}
