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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.flow.crud_service.FlowCrudService;
import io.gravitee.apim.core.plugin.model.PolicyPlugin;
import io.gravitee.apim.core.plugin.query_service.PolicyPluginQueryService;
import io.gravitee.apim.core.zee.model.ZeeResourceType;
import io.gravitee.definition.model.v4.flow.Flow;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class FlowRagContextAdapterTest {

    private FlowCrudService flowCrudService;
    private PolicyPluginQueryService policyPluginQueryService;
    private FlowRagContextAdapter adapter;

    @BeforeEach
    void setUp() {
        flowCrudService = mock(FlowCrudService.class);
        policyPluginQueryService = mock(PolicyPluginQueryService.class);
        adapter = new FlowRagContextAdapter(flowCrudService, policyPluginQueryService);
    }

    @Test
    void resource_type_is_flow() {
        assertThat(adapter.resourceType()).isEqualTo(ZeeResourceType.FLOW);
    }

    @Nested
    class RetrieveContext {

        @Test
        void returns_existing_flows_section_when_flows_present() {
            var flow = namedFlow("Rate Limit Flow", 2);
            when(flowCrudService.getApiV4Flows("api-123")).thenReturn(List.of(flow));
            when(policyPluginQueryService.findAll()).thenReturn(Set.of());

            var context = adapter.retrieveContext("env-1", "org-1", Map.of("apiId", "api-123"));

            assertThat(context).contains("### Existing Flows");
            assertThat(context).contains("- Rate Limit Flow: 2 step(s)");
        }

        @Test
        void returns_available_policies_section_when_policies_present() {
            when(flowCrudService.getApiV4Flows("api-123")).thenReturn(List.of());
            when(policyPluginQueryService.findAll()).thenReturn(
                Set.of(
                    policyPlugin("rate-limit", "Rate limiting and quota management"),
                    policyPlugin("transform-headers", "Add/remove HTTP headers")
                )
            );

            var context = adapter.retrieveContext("env-1", "org-1", Map.of("apiId", "api-123"));

            assertThat(context).contains("### Available Policies");
            assertThat(context).contains("rate-limit: Rate limiting and quota management");
            assertThat(context).contains("transform-headers: Add/remove HTTP headers");
        }

        @Test
        void returns_both_sections_when_both_present() {
            var flow = namedFlow("Auth Flow", 1);
            when(flowCrudService.getApiV4Flows("api-abc")).thenReturn(List.of(flow));
            when(policyPluginQueryService.findAll()).thenReturn(Set.of(policyPlugin("jwt", "JWT validation")));

            var context = adapter.retrieveContext("env-1", "org-1", Map.of("apiId", "api-abc"));

            assertThat(context).contains("### Existing Flows");
            assertThat(context).contains("### Available Policies");
            assertThat(context).contains("Auth Flow");
            assertThat(context).contains("jwt: JWT validation");
        }

        @Test
        void limits_flows_to_max_five() {
            var flows = IntStream.range(0, 10)
                .mapToObj(i -> namedFlow("Flow " + i, 0))
                .collect(Collectors.toList());
            when(flowCrudService.getApiV4Flows("api-123")).thenReturn(flows);
            when(policyPluginQueryService.findAll()).thenReturn(Set.of());

            var context = adapter.retrieveContext("env-1", "org-1", Map.of("apiId", "api-123"));

            // Count occurrences of "- Flow"
            long flowLines = context
                .lines()
                .filter(line -> line.startsWith("- Flow "))
                .count();
            assertThat(flowLines).isEqualTo(FlowRagContextAdapter.MAX_FLOWS);
        }

        @Test
        void limits_policies_to_max_twenty() {
            when(flowCrudService.getApiV4Flows("api-123")).thenReturn(List.of());
            var policies = IntStream.range(0, 30)
                .mapToObj(i -> policyPlugin("policy-" + i, "Description " + i))
                .collect(Collectors.toSet());
            when(policyPluginQueryService.findAll()).thenReturn(policies);

            var context = adapter.retrieveContext("env-1", "org-1", Map.of("apiId", "api-123"));

            long policyLines = context
                .lines()
                .filter(line -> line.startsWith("- policy-"))
                .count();
            assertThat(policyLines).isEqualTo(FlowRagContextAdapter.MAX_POLICIES);
        }

        @Test
        void returns_empty_when_no_flows_and_no_policies() {
            when(flowCrudService.getApiV4Flows("api-123")).thenReturn(List.of());
            when(policyPluginQueryService.findAll()).thenReturn(Set.of());

            var context = adapter.retrieveContext("env-1", "org-1", Map.of("apiId", "api-123"));

            assertThat(context).isEmpty();
        }

        @Test
        void skips_flows_when_api_id_missing() {
            when(policyPluginQueryService.findAll()).thenReturn(Set.of(policyPlugin("rate-limit", "Rate limiting")));

            var context = adapter.retrieveContext("env-1", "org-1", Map.of());

            assertThat(context).doesNotContain("### Existing Flows");
            assertThat(context).contains("### Available Policies");
        }

        @Test
        void degrades_gracefully_when_flow_service_throws() {
            when(flowCrudService.getApiV4Flows("api-123")).thenThrow(new RuntimeException("DB unavailable"));
            when(policyPluginQueryService.findAll()).thenReturn(Set.of(policyPlugin("jwt", "JWT validation")));

            // Must NOT throw
            var context = adapter.retrieveContext("env-1", "org-1", Map.of("apiId", "api-123"));

            assertThat(context).doesNotContain("### Existing Flows");
            assertThat(context).contains("### Available Policies");
            assertThat(context).contains("jwt: JWT validation");
        }

        @Test
        void degrades_gracefully_when_policy_service_throws() {
            var flow = namedFlow("Rate Limit Flow", 1);
            when(flowCrudService.getApiV4Flows("api-123")).thenReturn(List.of(flow));
            when(policyPluginQueryService.findAll()).thenThrow(new RuntimeException("Plugin registry unavailable"));

            // Must NOT throw
            var context = adapter.retrieveContext("env-1", "org-1", Map.of("apiId", "api-123"));

            assertThat(context).contains("### Existing Flows");
            assertThat(context).contains("Rate Limit Flow");
            assertThat(context).doesNotContain("### Available Policies");
        }

        @Test
        void handles_unnamed_flow_gracefully() {
            var flow = new Flow();
            flow.setName(null);
            when(flowCrudService.getApiV4Flows("api-123")).thenReturn(List.of(flow));
            when(policyPluginQueryService.findAll()).thenReturn(Set.of());

            var context = adapter.retrieveContext("env-1", "org-1", Map.of("apiId", "api-123"));

            assertThat(context).contains("(unnamed)");
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static Flow namedFlow(String name, int requestSteps) {
        var flow = new Flow();
        flow.setName(name);
        var steps = new ArrayList<io.gravitee.definition.model.v4.flow.step.Step>();
        for (int i = 0; i < requestSteps; i++) {
            steps.add(new io.gravitee.definition.model.v4.flow.step.Step());
        }
        flow.setRequest(steps);
        return flow;
    }

    private static PolicyPlugin policyPlugin(String id, String description) {
        return PolicyPlugin.builder().id(id).description(description).build();
    }
}
