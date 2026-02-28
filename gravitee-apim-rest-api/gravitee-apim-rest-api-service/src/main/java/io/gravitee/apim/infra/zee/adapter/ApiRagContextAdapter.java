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

import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.zee.domain_service.RagContextStrategy;
import io.gravitee.apim.core.zee.model.ZeeResourceType;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * RAG context strategy for {@link ZeeResourceType#API}.
 *
 * <p>
 * Retrieves the current API's top-level details so the LLM has runtime
 * context about the API it is generating for (name, version, type, etc.).
 *
 * @author Derek Thompson
 */
@Component
public class ApiRagContextAdapter implements RagContextStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(ApiRagContextAdapter.class);

    private final ApiCrudService apiCrudService;

    public ApiRagContextAdapter(ApiCrudService apiCrudService) {
        this.apiCrudService = apiCrudService;
    }

    @Override
    public ZeeResourceType resourceType() {
        return ZeeResourceType.API;
    }

    @Override
    public String retrieveContext(String envId, String orgId, Map<String, Object> contextData) {
        var sb = new StringBuilder();
        appendApiDetails(sb, contextData);
        return sb.toString();
    }

    private void appendApiDetails(StringBuilder sb, Map<String, Object> contextData) {
        try {
            var apiId = (String) contextData.get("apiId");
            if (apiId == null || apiId.isBlank()) {
                LOG.debug("No apiId in contextData — skipping API details section");
                return;
            }

            var apiOpt = apiCrudService.findById(apiId);
            if (apiOpt.isEmpty()) {
                LOG.debug("API not found for id={} — skipping API details section", apiId);
                return;
            }

            var api = apiOpt.get();
            sb.append("### Current API Details\n");
            sb.append("- Name: ").append(api.getName() != null ? api.getName() : "(unnamed)").append("\n");
            sb.append("- Version: ").append(api.getVersion() != null ? api.getVersion() : "(unversioned)").append("\n");
            if (api.getDescription() != null && !api.getDescription().isBlank()) {
                sb.append("- Description: ").append(api.getDescription()).append("\n");
            }
            if (api.getDefinitionVersion() != null) {
                sb.append("- Definition Version: ").append(api.getDefinitionVersion()).append("\n");
            }
            if (api.getType() != null) {
                sb.append("- Type: ").append(api.getType()).append("\n");
            }
        } catch (Exception e) {
            LOG.warn("Failed to retrieve API details for RAG context: {}", e.getMessage());
        }
    }
}
