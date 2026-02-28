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

import io.gravitee.apim.core.plugin.query_service.EntrypointPluginQueryService;
import io.gravitee.apim.core.zee.domain_service.RagContextStrategy;
import io.gravitee.apim.core.zee.model.ZeeResourceType;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * RAG context strategy for {@link ZeeResourceType#ENTRYPOINT}.
 *
 * <p>
 * Retrieves available entrypoint connector plugins so the LLM knows what
 * entrypoint types can be used (e.g. http-proxy, websocket, webhook).
 *
 * @author Derek Thompson
 */
@Component
public class EntrypointRagContextAdapter implements RagContextStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(EntrypointRagContextAdapter.class);

    /** Maximum number of entrypoint plugins to include in RAG context. */
    static final int MAX_ENTRYPOINTS = 20;

    private final EntrypointPluginQueryService entrypointPluginQueryService;

    public EntrypointRagContextAdapter(EntrypointPluginQueryService entrypointPluginQueryService) {
        this.entrypointPluginQueryService = entrypointPluginQueryService;
    }

    @Override
    public ZeeResourceType resourceType() {
        return ZeeResourceType.ENTRYPOINT;
    }

    @Override
    public String retrieveContext(String envId, String orgId, Map<String, Object> contextData) {
        var sb = new StringBuilder();
        appendEntrypoints(sb);
        return sb.toString();
    }

    private void appendEntrypoints(StringBuilder sb) {
        try {
            var entrypoints = entrypointPluginQueryService.findAll();
            if (entrypoints == null || entrypoints.isEmpty()) {
                return;
            }

            sb.append("### Available Entrypoint Connectors\n");
            entrypoints
                    .stream()
                    .limit(MAX_ENTRYPOINTS)
                    .forEach(plugin -> {
                        var id = plugin.getId() != null ? plugin.getId() : "(unknown)";
                        var description = plugin.getDescription() != null ? plugin.getDescription() : "";
                        sb.append("- ").append(id).append(": ").append(description).append("\n");
                    });
        } catch (Exception e) {
            LOG.warn("Failed to retrieve available entrypoint plugins for RAG context: {}", e.getMessage());
        }
    }
}
