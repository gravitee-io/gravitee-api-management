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
package io.gravitee.apim.infra.zee;

import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.flow.crud_service.FlowCrudService;
import io.gravitee.apim.core.plan.query_service.PlanQueryService;
import io.gravitee.apim.core.plugin.query_service.EndpointPluginQueryService;
import io.gravitee.apim.core.plugin.query_service.EntrypointPluginQueryService;
import io.gravitee.apim.core.plugin.query_service.PolicyPluginQueryService;
import io.gravitee.apim.core.zee.domain_service.RagContextStrategy;
import io.gravitee.apim.core.zee.domain_service.RagSection;
import io.gravitee.apim.core.zee.model.ZeeResourceType;
import io.gravitee.apim.infra.zee.adapter.DeclarativeRagAdapter;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Zee Mode — AI-assisted resource creation.
 *
 * <p>
 * Reads Azure OpenAI connection properties and rate limiting settings
 * from {@code gravitee.yml} or environment variables.
 *
 * <p>
 * Also declares all RAG context strategies as data-driven
 * {@link DeclarativeRagAdapter} instances — no bespoke adapter class per
 * resource type.
 *
 * @author Derek Burger
 */
@Configuration
public class ZeeConfiguration {

    @Value("${ai.zee.enabled:false}")
    private boolean enabled;

    @Value("${ai.zee.azure.url:#{null}}")
    private String azureUrl;

    @Value("${ai.zee.azure.apiKey:#{null}}")
    private String azureApiKey;

    @Value("${ai.zee.azure.model:gpt-4o-mini}")
    private String azureModel;

    @Value("${ai.zee.rateLimiting.maxRequestsPerMinute:10}")
    private int maxRequestsPerMinute;

    // ── RAG context strategies (declarative) ─────────────────────────────

    @Bean
    public List<RagContextStrategy> ragContextStrategies(
        FlowCrudService flowCrudService,
        PolicyPluginQueryService policyPluginQueryService,
        PlanQueryService planQueryService,
        ApiCrudService apiCrudService,
        EndpointPluginQueryService endpointPluginQueryService,
        EntrypointPluginQueryService entrypointPluginQueryService
    ) {
        return List.of(
            flowRagStrategy(flowCrudService, policyPluginQueryService),
            planRagStrategy(planQueryService),
            apiRagStrategy(apiCrudService),
            endpointRagStrategy(endpointPluginQueryService),
            entrypointRagStrategy(entrypointPluginQueryService)
        );
    }

    private static DeclarativeRagAdapter flowRagStrategy(
        FlowCrudService flowCrudService,
        PolicyPluginQueryService policyPluginQueryService
    ) {
        return new DeclarativeRagAdapter(
            ZeeResourceType.FLOW,
            List.of(
                new RagSection<>(
                    "Existing Flows",
                    ctx -> {
                        var apiId = (String) ctx.get("apiId");
                        if (apiId == null || apiId.isBlank()) return List.of();
                        var flows = flowCrudService.getApiV4Flows(apiId);
                        return flows != null ? flows : List.of();
                    },
                    flow -> {
                        var name = flow.getName() != null ? flow.getName() : "(unnamed)";
                        int steps = 0;
                        if (flow.getRequest() != null) steps += flow.getRequest().size();
                        if (flow.getResponse() != null) steps += flow.getResponse().size();
                        if (flow.getPublish() != null) steps += flow.getPublish().size();
                        if (flow.getSubscribe() != null) steps += flow.getSubscribe().size();
                        return name + ": " + steps + " step(s)";
                    },
                    5
                ),
                new RagSection<>(
                    "Available Policies",
                    ctx -> policyPluginQueryService.findAll(),
                    policy -> {
                        var id = policy.getId() != null ? policy.getId() : "(unknown)";
                        var desc = policy.getDescription() != null ? policy.getDescription() : "";
                        return id + ": " + desc;
                    },
                    20
                )
            )
        );
    }

    private static DeclarativeRagAdapter planRagStrategy(PlanQueryService planQueryService) {
        return new DeclarativeRagAdapter(
            ZeeResourceType.PLAN,
            List.of(
                new RagSection<>(
                    "Existing Plans",
                    ctx -> {
                        var apiId = (String) ctx.get("apiId");
                        if (apiId == null || apiId.isBlank()) return List.of();
                        var plans = planQueryService.findAllByApiId(apiId);
                        return plans != null ? plans : List.of();
                    },
                    plan -> {
                        var name = plan.getName() != null ? plan.getName() : "(unnamed)";
                        var security = plan.getPlanSecurity() != null && plan.getPlanSecurity().getType() != null
                            ? plan.getPlanSecurity().getType()
                            : "none";
                        var status = plan.getPlanStatus() != null ? plan.getPlanStatus().name() : "UNKNOWN";
                        var validation = plan.getValidation() != null ? plan.getValidation().name() : "AUTO";
                        return name + " [security=" + security + ", status=" + status + ", validation=" + validation + "]";
                    },
                    5
                )
            )
        );
    }

    private static DeclarativeRagAdapter apiRagStrategy(ApiCrudService apiCrudService) {
        return new DeclarativeRagAdapter(
            ZeeResourceType.API,
            List.of(
                new RagSection<>(
                    "Current API Details",
                    ctx -> {
                        var apiId = (String) ctx.get("apiId");
                        if (apiId == null || apiId.isBlank()) return List.of();
                        var apiOpt = apiCrudService.findById(apiId);
                        return apiOpt.map(List::of).orElse(List.of());
                    },
                    api -> {
                        var sb = new StringBuilder();
                        sb.append("Name: ").append(api.getName() != null ? api.getName() : "(unnamed)");
                        sb.append(", Version: ").append(api.getVersion() != null ? api.getVersion() : "(unversioned)");
                        if (api.getDescription() != null && !api.getDescription().isBlank()) {
                            sb.append(", Description: ").append(api.getDescription());
                        }
                        if (api.getDefinitionVersion() != null) {
                            sb.append(", Definition Version: ").append(api.getDefinitionVersion());
                        }
                        if (api.getType() != null) {
                            sb.append(", Type: ").append(api.getType());
                        }
                        return sb.toString();
                    },
                    1
                )
            )
        );
    }

    private static DeclarativeRagAdapter endpointRagStrategy(EndpointPluginQueryService endpointPluginQueryService) {
        return new DeclarativeRagAdapter(
            ZeeResourceType.ENDPOINT,
            List.of(
                new RagSection<>(
                    "Available Endpoint Connectors",
                    ctx -> endpointPluginQueryService.findAll(),
                    plugin -> {
                        var id = plugin.getId() != null ? plugin.getId() : "(unknown)";
                        var desc = plugin.getDescription() != null ? plugin.getDescription() : "";
                        return id + ": " + desc;
                    },
                    20
                )
            )
        );
    }

    private static DeclarativeRagAdapter entrypointRagStrategy(EntrypointPluginQueryService entrypointPluginQueryService) {
        return new DeclarativeRagAdapter(
            ZeeResourceType.ENTRYPOINT,
            List.of(
                new RagSection<>(
                    "Available Entrypoint Connectors",
                    ctx -> entrypointPluginQueryService.findAll(),
                    plugin -> {
                        var id = plugin.getId() != null ? plugin.getId() : "(unknown)";
                        var desc = plugin.getDescription() != null ? plugin.getDescription() : "";
                        return id + ": " + desc;
                    },
                    20
                )
            )
        );
    }

    // ── property accessors ───────────────────────────────────────────────

    public boolean isEnabled() {
        return enabled;
    }

    public String getAzureUrl() {
        return azureUrl;
    }

    public String getAzureApiKey() {
        return azureApiKey;
    }

    public String getAzureModel() {
        return azureModel;
    }

    public int getMaxRequestsPerMinute() {
        return maxRequestsPerMinute;
    }
}
