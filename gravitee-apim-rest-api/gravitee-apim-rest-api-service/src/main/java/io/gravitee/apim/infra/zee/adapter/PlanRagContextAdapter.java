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

import io.gravitee.apim.core.plan.query_service.PlanQueryService;
import io.gravitee.apim.core.zee.domain_service.RagContextStrategy;
import io.gravitee.apim.core.zee.model.ZeeResourceType;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * RAG context strategy for {@link ZeeResourceType#PLAN}.
 *
 * <p>
 * Retrieves existing plans for the API to help the LLM understand what
 * security and access patterns are already configured.
 *
 * @author Derek Thompson
 */
@Component
public class PlanRagContextAdapter implements RagContextStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(PlanRagContextAdapter.class);

    /** Maximum number of existing plans to include in RAG context. */
    static final int MAX_PLANS = 5;

    private final PlanQueryService planQueryService;

    public PlanRagContextAdapter(PlanQueryService planQueryService) {
        this.planQueryService = planQueryService;
    }

    @Override
    public ZeeResourceType resourceType() {
        return ZeeResourceType.PLAN;
    }

    @Override
    public String retrieveContext(String envId, String orgId, Map<String, Object> contextData) {
        var sb = new StringBuilder();
        appendPlans(sb, contextData);
        return sb.toString();
    }

    private void appendPlans(StringBuilder sb, Map<String, Object> contextData) {
        try {
            var apiId = (String) contextData.get("apiId");
            if (apiId == null || apiId.isBlank()) {
                LOG.debug("No apiId in contextData — skipping existing plans section");
                return;
            }

            var plans = planQueryService.findAllByApiId(apiId);
            if (plans == null || plans.isEmpty()) {
                return;
            }

            sb.append("### Existing Plans\n");
            plans
                    .stream()
                    .limit(MAX_PLANS)
                    .forEach(plan -> {
                        var name = plan.getName() != null ? plan.getName() : "(unnamed)";
                        var security = plan.getPlanSecurity() != null && plan.getPlanSecurity().getType() != null
                                ? plan.getPlanSecurity().getType()
                                : "none";
                        var status = plan.getPlanStatus() != null ? plan.getPlanStatus().name() : "UNKNOWN";
                        var validation = plan.getValidation() != null ? plan.getValidation().name() : "AUTO";
                        sb
                                .append("- ")
                                .append(name)
                                .append(" [security=")
                                .append(security)
                                .append(", status=")
                                .append(status)
                                .append(", validation=")
                                .append(validation)
                                .append("]\n");
                    });
        } catch (Exception e) {
            LOG.warn("Failed to retrieve existing plans for RAG context: {}", e.getMessage());
        }
    }
}
