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
package io.gravitee.apim.infra.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.definition.model.Policy;
import io.gravitee.definition.model.Rule;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.repository.management.model.Plan;
import io.gravitee.rest.api.model.BasePlanEntity;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.model.v4.plan.PlanMode;
import io.gravitee.rest.api.model.v4.plan.PlanSecurityType;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PlanAdapterTest {

    @Nested
    class ToEntityV4 {

        @Test
        public void should_convert_plan_to_plan_entity() {
            Plan plan = new Plan();
            plan.setId("123123-1531-4563456166");
            plan.setName("Plan name");
            plan.setDescription("Description for the new plan");
            plan.setValidation(Plan.PlanValidationType.AUTO);
            plan.setType(Plan.PlanType.API);
            plan.setMode(Plan.PlanMode.STANDARD);
            plan.setStatus(Plan.Status.STAGING);
            plan.setApi("api1");
            plan.setGeneralConditions("general_conditions");
            plan.setSecurity(Plan.PlanSecurityType.KEY_LESS);

            GenericPlanEntity planEntity = PlanAdapter.INSTANCE.toEntityV4(plan);

            assertThat(planEntity.getId()).isEqualTo(plan.getId());
            assertThat(planEntity.getName()).isEqualTo(plan.getName());
            assertThat(planEntity.getDescription()).isEqualTo(plan.getDescription());
            assertThat(planEntity.getPlanValidation().name()).isEqualTo(plan.getValidation().name());
            assertThat(planEntity.getPlanStatus().name()).isEqualTo(plan.getStatus().name());
            assertThat(planEntity.getApiId()).isEqualTo(plan.getApi());
            assertThat(planEntity.getGeneralConditions()).isEqualTo(plan.getGeneralConditions());
            assertThat(planEntity.getPlanSecurity().getType()).isEqualTo(PlanSecurityType.KEY_LESS.getLabel());
        }

        @Test
        public void should_convert_push_plan_to_plan_entity() {
            Plan plan = new Plan();
            plan.setId("123123-1531-4563456166");
            plan.setName("Push plan name");
            plan.setDescription("Description for the new plan");
            plan.setValidation(Plan.PlanValidationType.AUTO);
            plan.setType(Plan.PlanType.API);
            plan.setMode(Plan.PlanMode.PUSH);
            plan.setStatus(Plan.Status.STAGING);
            plan.setApi("api1");
            plan.setGeneralConditions("general_conditions");

            GenericPlanEntity planEntity = PlanAdapter.INSTANCE.toEntityV4(plan);

            assertThat(planEntity.getId()).isEqualTo(plan.getId());
            assertThat(planEntity.getName()).isEqualTo(plan.getName());
            assertThat(planEntity.getDescription()).isEqualTo(plan.getDescription());
            assertThat(planEntity.getPlanValidation().name()).isEqualTo(plan.getValidation().name());
            assertThat(planEntity.getPlanStatus().name()).isEqualTo(plan.getStatus().name());
            assertThat(planEntity.getApiId()).isEqualTo(plan.getApi());
            assertThat(planEntity.getGeneralConditions()).isEqualTo(plan.getGeneralConditions());
            assertThat(planEntity.getPlanMode()).isEqualTo(PlanMode.PUSH);
            assertThat(planEntity.getPlanSecurity()).isNull();
        }
    }

    @Nested
    class ToEntityV2 {

        @Test
        public void should_convert_plan_to_plan_entity() {
            Plan plan = new Plan();
            plan.setId("123123-1531-4563456166");
            plan.setName("Plan name");
            plan.setDescription("Description for the new plan");
            plan.setValidation(Plan.PlanValidationType.AUTO);
            plan.setType(Plan.PlanType.API);
            plan.setMode(Plan.PlanMode.STANDARD);
            plan.setStatus(Plan.Status.STAGING);
            plan.setApi("api1");
            plan.setGeneralConditions("general_conditions");
            plan.setSecurity(Plan.PlanSecurityType.KEY_LESS);

            GenericPlanEntity planEntity = PlanAdapter.INSTANCE.toEntityV2(plan);

            assertThat(planEntity.getId()).isEqualTo(plan.getId());
            assertThat(planEntity.getName()).isEqualTo(plan.getName());
            assertThat(planEntity.getDescription()).isEqualTo(plan.getDescription());
            assertThat(planEntity.getPlanValidation().name()).isEqualTo(plan.getValidation().name());
            assertThat(planEntity.getPlanStatus().name()).isEqualTo(plan.getStatus().name());
            assertThat(planEntity.getApiId()).isEqualTo(plan.getApi());
            assertThat(planEntity.getGeneralConditions()).isEqualTo(plan.getGeneralConditions());
            assertThat(planEntity.getPlanSecurity().getType()).isEqualTo(io.gravitee.rest.api.model.PlanSecurityType.KEY_LESS.name());
        }

        @Test
        public void should_convert_paths_oriented_plan_to_plan_entity() {
            Plan plan = new Plan();
            plan.setId("123123-1531-4563456166");
            plan.setName("Plan name");
            plan.setDescription("Description for the new plan");
            plan.setValidation(Plan.PlanValidationType.AUTO);
            plan.setType(Plan.PlanType.API);
            plan.setMode(Plan.PlanMode.STANDARD);
            plan.setStatus(Plan.Status.STAGING);
            plan.setApi("api1");
            plan.setGeneralConditions("general_conditions");
            plan.setSecurity(Plan.PlanSecurityType.KEY_LESS);
            plan.setDefinition(
                """
                   {
                     "/": [
                       {
                         "methods": ["GET","POST","PUT"],
                         "enabled": true,
                         "resource-filtering": {
                           "whitelist": [
                             {
                               "pattern": "/**",
                               "methods": ["GET"]
                             }
                           ]
                         }
                       }
                     ]
                   }
                   """
            );

            GenericPlanEntity planEntity = PlanAdapter.INSTANCE.toEntityV2(plan);

            assertThat(planEntity.getId()).isEqualTo(plan.getId());
            assertThat(planEntity.getName()).isEqualTo(plan.getName());
            assertThat(planEntity.getDescription()).isEqualTo(plan.getDescription());
            assertThat(planEntity.getPlanValidation().name()).isEqualTo(plan.getValidation().name());
            assertThat(planEntity.getPlanStatus().name()).isEqualTo(plan.getStatus().name());
            assertThat(planEntity.getApiId()).isEqualTo(plan.getApi());
            assertThat(planEntity.getGeneralConditions()).isEqualTo(plan.getGeneralConditions());
            assertThat(planEntity.getPlanSecurity().getType()).isEqualTo(io.gravitee.rest.api.model.PlanSecurityType.KEY_LESS.name());
            BasePlanEntity planV2 = (BasePlanEntity) planEntity;
            assertThat(planV2.getPaths().get("/"))
                .hasSize(1)
                .containsExactly(
                    Rule
                        .builder()
                        .methods(Set.of(HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT))
                        .policy(
                            Policy
                                .builder()
                                .name("resource-filtering")
                                .configuration("{\"whitelist\":[{\"pattern\":\"/**\",\"methods\":[\"GET\"]}]},    \"enabled\" : true  }")
                                .build()
                        )
                        .build()
                );
        }
    }
}
