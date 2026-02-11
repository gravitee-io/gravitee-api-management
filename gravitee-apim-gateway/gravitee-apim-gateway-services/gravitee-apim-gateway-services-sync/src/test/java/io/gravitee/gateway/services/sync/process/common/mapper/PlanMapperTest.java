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
package io.gravitee.gateway.services.sync.process.common.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.repository.management.model.Plan;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PlanMapperTest {

    @Nested
    class ToDefinitionTest {

        @Test
        void should_return_null_when_repo_plan_null() {
            assertThat(PlanMapper.toDefinition(null)).isNull();
        }

        @Test
        void should_map_plan_with_all_fields() {
            Plan repoPlan = new Plan();
            repoPlan.setId("plan-id");
            repoPlan.setName("Test Plan");
            repoPlan.setStatus(Plan.Status.PUBLISHED);
            repoPlan.setMode(Plan.PlanMode.STANDARD);
            repoPlan.setSecurity(Plan.PlanSecurityType.API_KEY);
            repoPlan.setSecurityDefinition("{}");
            repoPlan.setSelectionRule("rule");
            repoPlan.setTags(Set.of("tag1", "tag2"));

            var result = PlanMapper.toDefinition(repoPlan);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo("plan-id");
            assertThat(result.getName()).isEqualTo("Test Plan");
            assertThat(result.getStatus()).isEqualTo(PlanStatus.PUBLISHED);
            assertThat(result.getMode()).isEqualTo(PlanMode.STANDARD);
            assertThat(result.getSecurity()).isNotNull();
            assertThat(result.getSecurity().getType()).isEqualTo("API_KEY");
            assertThat(result.getSecurity().getConfiguration()).isEqualTo("{}");
            assertThat(result.getSelectionRule()).isEqualTo("rule");
            assertThat(result.getTags()).containsExactlyInAnyOrder("tag1", "tag2");
            assertThat(result.getFlows()).isEmpty();
        }

        @Test
        void should_use_api_key_default_when_security_null() {
            Plan repoPlan = new Plan();
            repoPlan.setId("plan-id");
            repoPlan.setName("Plan");
            repoPlan.setStatus(Plan.Status.PUBLISHED);

            var result = PlanMapper.toDefinition(repoPlan);

            assertThat(result.getSecurity()).isNotNull();
            assertThat(result.getSecurity().getType()).isEqualTo("api-key");
        }

        @Test
        void should_use_standard_mode_when_mode_null() {
            Plan repoPlan = new Plan();
            repoPlan.setId("plan-id");
            repoPlan.setName("Plan");
            repoPlan.setStatus(Plan.Status.PUBLISHED);
            repoPlan.setMode(null);

            var result = PlanMapper.toDefinition(repoPlan);

            assertThat(result.getMode()).isEqualTo(PlanMode.STANDARD);
        }

        @Test
        void should_map_published_status() {
            Plan repoPlan = createMinimalPlan();
            repoPlan.setStatus(Plan.Status.PUBLISHED);
            assertThat(PlanMapper.toDefinition(repoPlan).getStatus()).isEqualTo(PlanStatus.PUBLISHED);
        }

        @Test
        void should_map_deprecated_status() {
            Plan repoPlan = createMinimalPlan();
            repoPlan.setStatus(Plan.Status.DEPRECATED);
            assertThat(PlanMapper.toDefinition(repoPlan).getStatus()).isEqualTo(PlanStatus.DEPRECATED);
        }

        @Test
        void should_map_staging_status() {
            Plan repoPlan = createMinimalPlan();
            repoPlan.setStatus(Plan.Status.STAGING);
            assertThat(PlanMapper.toDefinition(repoPlan).getStatus()).isEqualTo(PlanStatus.STAGING);
        }

        @Test
        void should_map_closed_status() {
            Plan repoPlan = createMinimalPlan();
            repoPlan.setStatus(Plan.Status.CLOSED);
            assertThat(PlanMapper.toDefinition(repoPlan).getStatus()).isEqualTo(PlanStatus.CLOSED);
        }

        @Test
        void should_use_published_when_status_null() {
            Plan repoPlan = createMinimalPlan();
            repoPlan.setStatus(null);
            assertThat(PlanMapper.toDefinition(repoPlan).getStatus()).isEqualTo(PlanStatus.PUBLISHED);
        }

        @Test
        void should_return_null_tags_when_tags_null() {
            Plan repoPlan = createMinimalPlan();
            repoPlan.setTags(null);

            var result = PlanMapper.toDefinition(repoPlan);

            assertThat(result.getTags()).isNull();
        }

        private Plan createMinimalPlan() {
            Plan plan = new Plan();
            plan.setId("plan-id");
            plan.setName("Plan");
            plan.setStatus(Plan.Status.PUBLISHED);
            return plan;
        }
    }
}
