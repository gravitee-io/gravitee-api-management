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

import io.gravitee.apim.core.flow.crud_service.FlowCrudService;
import io.gravitee.apim.core.plugin.query_service.PolicyPluginQueryService;
import io.gravitee.apim.core.zee.domain_service.RagContextStrategy;
import io.gravitee.apim.core.zee.model.ZeeResourceType;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * RAG context strategy for {@link ZeeResourceType#FLOW}.
 *
 * <p>
 * Retrieves two categories of context to help the LLM generate better flows:
 * <ol>
 * <li>Existing V4 API flows (up to {@value #MAX_FLOWS}) — shows the LLM what
 * patterns are already in place for this API.</li>
 * <li>Available policy plugins (up to {@value #MAX_POLICIES}) — shows the LLM
 * what policies it can use in steps.</li>
 * </ol>
 *
 * <p>
 * Each retrieval is wrapped in a try/catch for graceful degradation.
 * A failure in one section does not abort the other.
 *
 * <p>
 * <b>Tenant scoping note</b>: {@code FlowCrudService.getApiV4Flows(apiId)}
 * scopes
 * results by {@code apiId} (which implicitly belongs to one tenant). Direct
 * env/org
 * scoping at the service level is not exposed by the current
 * {@code FlowCrudService}
 * API — this is logged as a future improvement.
 *
 * @author Derek Burger
 */
@Component
public class FlowRagContextAdapter implements RagContextStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(FlowRagContextAdapter.class);

    /** Maximum number of existing flows to include in RAG context. */
    static final int MAX_FLOWS = 5;

    /** Maximum number of policy plugins to include in RAG context. */
    static final int MAX_POLICIES = 20;

    private final FlowCrudService flowCrudService;
    private final PolicyPluginQueryService policyPluginQueryService;

    public FlowRagContextAdapter(FlowCrudService flowCrudService, PolicyPluginQueryService policyPluginQueryService) {
        this.flowCrudService = flowCrudService;
        this.policyPluginQueryService = policyPluginQueryService;
    }

    @Override
    public ZeeResourceType resourceType() {
        return ZeeResourceType.FLOW;
    }

    @Override
    public String retrieveContext(String envId, String orgId, Map<String, Object> contextData) {
        var sb = new StringBuilder();

        appendFlows(sb, contextData);
        appendPolicies(sb);

        return sb.toString();
    }

    private void appendFlows(StringBuilder sb, Map<String, Object> contextData) {
        try {
            var apiId = (String) contextData.get("apiId");
            if (apiId == null || apiId.isBlank()) {
                LOG.debug("No apiId in contextData — skipping existing flows section");
                return;
            }

            var flows = flowCrudService.getApiV4Flows(apiId);
            if (flows == null || flows.isEmpty()) {
                return;
            }

            sb.append("### Existing Flows\n");
            flows
                .stream()
                .limit(MAX_FLOWS)
                .forEach(flow -> {
                    var name = flow.getName() != null ? flow.getName() : "(unnamed)";
                    var stepCount = countSteps(flow);
                    sb.append("- ").append(name).append(": ").append(stepCount).append(" step(s)\n");
                });
        } catch (Exception e) {
            LOG.warn("Failed to retrieve existing flows for RAG context: {}", e.getMessage());
        }
    }

    private void appendPolicies(StringBuilder sb) {
        try {
            var policies = policyPluginQueryService.findAll();
            if (policies == null || policies.isEmpty()) {
                return;
            }

            if (!sb.isEmpty()) {
                sb.append("\n");
            }
            sb.append("### Available Policies\n");
            policies
                .stream()
                .limit(MAX_POLICIES)
                .forEach(policy -> {
                    var id = policy.getId() != null ? policy.getId() : "(unknown)";
                    var description = policy.getDescription() != null ? policy.getDescription() : "";
                    sb.append("- ").append(id).append(": ").append(description).append("\n");
                });
        } catch (Exception e) {
            LOG.warn("Failed to retrieve available policies for RAG context: {}", e.getMessage());
        }
    }

    private static int countSteps(io.gravitee.definition.model.v4.flow.Flow flow) {
        int count = 0;
        if (flow.getRequest() != null) count += flow.getRequest().size();
        if (flow.getResponse() != null) count += flow.getResponse().size();
        if (flow.getPublish() != null) count += flow.getPublish().size();
        if (flow.getSubscribe() != null) count += flow.getSubscribe().size();
        return count;
    }
}
