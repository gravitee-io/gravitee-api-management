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

import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.plan.query_service.PlanQueryService;
import io.gravitee.apim.core.zee.model.ZeeResourceType;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PlanRagContextAdapterTest {

    private PlanQueryService planQueryService;
    private PlanRagContextAdapter adapter;

    @BeforeEach
    void setUp() {
        planQueryService = mock(PlanQueryService.class);
        adapter = new PlanRagContextAdapter(planQueryService);
    }

    @Test
    void resource_type_is_plan() {
        assertThat(adapter.resourceType()).isEqualTo(ZeeResourceType.PLAN);
    }

    @Nested
    class RetrieveContext {

        @Test
        void returns_existing_plans_section_when_plans_present() {
            var plan = buildPlan("Gold Plan", "API_KEY", PlanStatus.PUBLISHED, Plan.PlanValidationType.AUTO);
            when(planQueryService.findAllByApiId("api-123")).thenReturn(List.of(plan));

            var context = adapter.retrieveContext("env-1", "org-1", Map.of("apiId", "api-123"));

            assertThat(context).contains("### Existing Plans");
            assertThat(context).contains("Gold Plan");
            assertThat(context).contains("security=API_KEY");
            assertThat(context).contains("status=PUBLISHED");
            assertThat(context).contains("validation=AUTO");
        }

        @Test
        void limits_plans_to_max_five() {
            var plans = IntStream.range(0, 10)
                    .mapToObj(
                            i -> buildPlan("Plan " + i, "API_KEY", PlanStatus.PUBLISHED, Plan.PlanValidationType.AUTO))
                    .collect(Collectors.toList());
            when(planQueryService.findAllByApiId("api-123")).thenReturn(plans);

            var context = adapter.retrieveContext("env-1", "org-1", Map.of("apiId", "api-123"));

            long planLines = context
                    .lines()
                    .filter(line -> line.startsWith("- Plan "))
                    .count();
            assertThat(planLines).isEqualTo(PlanRagContextAdapter.MAX_PLANS);
        }

        @Test
        void returns_empty_when_no_plans() {
            when(planQueryService.findAllByApiId("api-123")).thenReturn(List.of());

            var context = adapter.retrieveContext("env-1", "org-1", Map.of("apiId", "api-123"));

            assertThat(context).isEmpty();
        }

        @Test
        void skips_plans_when_api_id_missing() {
            var context = adapter.retrieveContext("env-1", "org-1", Map.of());

            assertThat(context).isEmpty();
        }

        @Test
        void degrades_gracefully_when_service_throws() {
            when(planQueryService.findAllByApiId("api-123")).thenThrow(new RuntimeException("DB unavailable"));

            var context = adapter.retrieveContext("env-1", "org-1", Map.of("apiId", "api-123"));

            assertThat(context).isEmpty();
        }

        @Test
        void handles_unnamed_plan_gracefully() {
            var plan = buildPlan(null, "JWT", PlanStatus.STAGING, Plan.PlanValidationType.MANUAL);
            when(planQueryService.findAllByApiId("api-123")).thenReturn(List.of(plan));

            var context = adapter.retrieveContext("env-1", "org-1", Map.of("apiId", "api-123"));

            assertThat(context).contains("(unnamed)");
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static Plan buildPlan(String name, String securityType, PlanStatus status,
            Plan.PlanValidationType validation) {
        var planDef = new io.gravitee.definition.model.v4.plan.Plan();
        planDef.setStatus(status);
        var security = new PlanSecurity();
        security.setType(securityType);
        planDef.setSecurity(security);

        return Plan.builder()
                .name(name)
                .definitionVersion(DefinitionVersion.V4)
                .planDefinitionHttpV4(planDef)
                .validation(validation)
                .build();
    }
}
