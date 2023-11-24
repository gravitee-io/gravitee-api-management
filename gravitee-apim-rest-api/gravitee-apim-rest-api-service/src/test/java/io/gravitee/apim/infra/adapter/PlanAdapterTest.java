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

import fixtures.core.model.PlanFixtures;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.definition.model.Policy;
import io.gravitee.definition.model.Rule;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.repository.management.model.Plan;
import io.gravitee.rest.api.model.BasePlanEntity;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.model.v4.plan.PlanMode;
import io.gravitee.rest.api.model.v4.plan.PlanSecurityType;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.assertj.core.api.SoftAssertions;
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
    class CoreModel {

        @Test
        void should_convert_from_v4_repository_to_core_model() {
            var repository = planV4().build();

            var plan = PlanAdapter.INSTANCE.fromRepository(repository);

            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(plan.getApiId()).isEqualTo("my-api");
                soft.assertThat(plan.getCharacteristics()).containsExactly("characteristic-1");
                soft.assertThat(plan.getClosedAt()).isEqualTo(Instant.parse("2020-02-04T20:22:02.00Z").atZone(ZoneOffset.UTC));
                soft.assertThat(plan.getCommentMessage()).isEqualTo("comment-message");
                soft.assertThat(plan.getCreatedAt()).isEqualTo(Instant.parse("2020-02-01T20:22:02.00Z").atZone(ZoneOffset.UTC));
                soft.assertThat(plan.getCrossId()).isEqualTo("cross-id");
                soft.assertThat(plan.getDescription()).isEqualTo("plan-description");
                soft.assertThat(plan.getExcludedGroups()).containsExactly("excluded-group-1");
                soft.assertThat(plan.getGeneralConditions()).isEqualTo("general-conditions");
                soft.assertThat(plan.getId()).isEqualTo("my-id");
                soft.assertThat(plan.getMode()).isEqualTo(io.gravitee.definition.model.v4.plan.PlanMode.STANDARD);
                soft.assertThat(plan.getName()).isEqualTo("plan-name");
                soft.assertThat(plan.getNeedRedeployAt()).isEqualTo(Date.from(Instant.parse("2020-02-05T20:22:02.00Z")));
                soft.assertThat(plan.getOrder()).isOne();
                soft.assertThat(plan.getPublishedAt()).isEqualTo(Instant.parse("2020-02-03T20:22:02.00Z").atZone(ZoneOffset.UTC));
                soft
                    .assertThat(plan.getSecurity())
                    .isEqualTo(PlanSecurity.builder().type("api-key").configuration("security-definition").build());
                soft.assertThat(plan.getSelectionRule()).isEqualTo("selection-rule");
                soft.assertThat(plan.getStatus()).isEqualTo(PlanStatus.PUBLISHED);
                soft.assertThat(plan.getTags()).isEqualTo(Set.of("tag-1"));
                soft.assertThat(plan.getType()).isEqualTo(io.gravitee.apim.core.plan.model.Plan.PlanType.API);
                soft.assertThat(plan.getValidation()).isEqualTo(io.gravitee.apim.core.plan.model.Plan.PlanValidationType.AUTO);
                soft.assertThat(plan.getUpdatedAt()).isEqualTo(Instant.parse("2020-02-02T20:22:02.00Z").atZone(ZoneOffset.UTC));
                soft.assertThat(plan.isCommentRequired()).isTrue();
            });
        }

        @Test
        void should_convert_from_v2_repository_to_core_model() {
            var repository = planV2().build();

            var plan = PlanAdapter.INSTANCE.fromRepository(repository);

            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(plan.getApiId()).isEqualTo("my-api");
                soft.assertThat(plan.getCharacteristics()).containsExactly("characteristic-1");
                soft.assertThat(plan.getClosedAt()).isEqualTo(Instant.parse("2020-02-04T20:22:02.00Z").atZone(ZoneOffset.UTC));
                soft.assertThat(plan.getCommentMessage()).isEqualTo("comment-message");
                soft.assertThat(plan.getCreatedAt()).isEqualTo(Instant.parse("2020-02-01T20:22:02.00Z").atZone(ZoneOffset.UTC));
                soft.assertThat(plan.getCrossId()).isEqualTo("cross-id");
                soft.assertThat(plan.getDescription()).isEqualTo("plan-description");
                soft.assertThat(plan.getExcludedGroups()).containsExactly("excluded-group-1");
                soft.assertThat(plan.getGeneralConditions()).isEqualTo("general-conditions");
                soft.assertThat(plan.getId()).isEqualTo("my-id");
                soft.assertThat(plan.getName()).isEqualTo("plan-name");
                soft.assertThat(plan.getNeedRedeployAt()).isEqualTo(Date.from(Instant.parse("2020-02-05T20:22:02.00Z")));
                soft.assertThat(plan.getOrder()).isOne();
                soft.assertThat(plan.getPaths()).isEqualTo(Map.of("/", List.of()));
                soft.assertThat(plan.getMode()).isEqualTo(io.gravitee.definition.model.v4.plan.PlanMode.STANDARD);
                soft
                    .assertThat(plan.getSecurity())
                    .isEqualTo(PlanSecurity.builder().type("api-key").configuration("security-definition").build());
                soft.assertThat(plan.getStatus()).isEqualTo(PlanStatus.PUBLISHED);
                soft.assertThat(plan.getType()).isEqualTo(io.gravitee.apim.core.plan.model.Plan.PlanType.API);
                soft.assertThat(plan.getValidation()).isEqualTo(io.gravitee.apim.core.plan.model.Plan.PlanValidationType.AUTO);
                soft.assertThat(plan.getPublishedAt()).isEqualTo(Instant.parse("2020-02-03T20:22:02.00Z").atZone(ZoneOffset.UTC));
                soft.assertThat(plan.getSelectionRule()).isEqualTo("selection-rule");
                soft.assertThat(plan.getTags()).isEqualTo(Set.of("tag-1"));
                soft.assertThat(plan.getUpdatedAt()).isEqualTo(Instant.parse("2020-02-02T20:22:02.00Z").atZone(ZoneOffset.UTC));
                soft.assertThat(plan.isCommentRequired()).isTrue();
            });
        }

        @Test
        void should_convert_v4_plan_to_repository() {
            var model = PlanFixtures
                .aPlanV4()
                .toBuilder()
                .closedAt(Instant.parse("2020-02-04T20:22:02.00Z").atZone(ZoneOffset.UTC))
                .needRedeployAt(Date.from(Instant.parse("2020-02-05T20:22:02.00Z")))
                .publishedAt(Instant.parse("2020-02-03T20:22:02.00Z").atZone(ZoneOffset.UTC))
                .build();

            var plan = PlanAdapter.INSTANCE.toRepository(model);

            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(plan.getApi()).isEqualTo("my-api");
                soft.assertThat(plan.getCharacteristics()).containsExactly("characteristic1", "characteristic2");
                soft.assertThat(plan.getClosedAt()).isEqualTo(Date.from(Instant.parse("2020-02-04T20:22:02.00Z")));
                soft.assertThat(plan.getCommentMessage()).isEqualTo("Comment message");
                soft.assertThat(plan.getCreatedAt()).isEqualTo(Date.from(Instant.parse("2020-02-01T20:22:02.00Z")));
                soft.assertThat(plan.getCrossId()).isEqualTo("my-plan-crossId");
                soft.assertThat(plan.getDescription()).isEqualTo("Description");
                soft.assertThat(plan.getExcludedGroups()).containsExactly("excludedGroup1", "excludedGroup2");
                soft.assertThat(plan.getGeneralConditions()).isEqualTo("General conditions");
                soft.assertThat(plan.getId()).isEqualTo("my-plan");
                soft.assertThat(plan.getMode()).isEqualTo(Plan.PlanMode.STANDARD);
                soft.assertThat(plan.getName()).isEqualTo("My plan");
                soft.assertThat(plan.getNeedRedeployAt()).isEqualTo(Date.from(Instant.parse("2020-02-05T20:22:02.00Z")));
                soft.assertThat(plan.getOrder()).isOne();
                soft.assertThat(plan.getPublishedAt()).isEqualTo(Date.from(Instant.parse("2020-02-03T20:22:02.00Z")));
                soft.assertThat(plan.getSecurity()).isEqualTo(Plan.PlanSecurityType.KEY_LESS);
                soft.assertThat(plan.getSecurityDefinition()).isEqualTo("{\"nice\": \"config\"}");
                soft.assertThat(plan.getSelectionRule()).isEqualTo("{#request.attribute['selectionRule'] != null}");
                soft.assertThat(plan.getStatus()).isEqualTo(Plan.Status.PUBLISHED);
                soft.assertThat(plan.getTags()).isEqualTo(Set.of("tag1", "tag2"));
                soft.assertThat(plan.getType()).isEqualTo(Plan.PlanType.API);
                soft.assertThat(plan.getValidation()).isEqualTo(Plan.PlanValidationType.AUTO);
                soft.assertThat(plan.getUpdatedAt()).isEqualTo(Date.from(Instant.parse("2020-02-02T20:22:02.00Z")));
                soft.assertThat(plan.isCommentRequired()).isFalse();
            });
        }

        @Test
        void should_convert_v2_plan_to_repository() {
            var model = PlanFixtures
                .aPlanV2()
                .toBuilder()
                .paths(Map.of("/", List.of()))
                .closedAt(Instant.parse("2020-02-04T20:22:02.00Z").atZone(ZoneOffset.UTC))
                .needRedeployAt(Date.from(Instant.parse("2020-02-05T20:22:02.00Z")))
                .publishedAt(Instant.parse("2020-02-03T20:22:02.00Z").atZone(ZoneOffset.UTC))
                .build();

            var plan = PlanAdapter.INSTANCE.toRepository(model);

            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(plan.getApi()).isEqualTo("my-api");
                soft.assertThat(plan.getCharacteristics()).containsExactly("characteristic1", "characteristic2");
                soft.assertThat(plan.getClosedAt()).isEqualTo(Date.from(Instant.parse("2020-02-04T20:22:02.00Z")));
                soft.assertThat(plan.getCommentMessage()).isEqualTo("Comment message");
                soft.assertThat(plan.getCreatedAt()).isEqualTo(Date.from(Instant.parse("2020-02-01T20:22:02.00Z")));
                soft.assertThat(plan.getCrossId()).isEqualTo("my-plan-crossId");
                soft.assertThat(plan.getDefinition()).isEqualTo("{\"/\":[]}");
                soft.assertThat(plan.getDescription()).isEqualTo("Description");
                soft.assertThat(plan.getExcludedGroups()).containsExactly("excludedGroup1", "excludedGroup2");
                soft.assertThat(plan.getGeneralConditions()).isEqualTo("General conditions");
                soft.assertThat(plan.getId()).isEqualTo("my-plan");
                soft.assertThat(plan.getMode()).isEqualTo(Plan.PlanMode.STANDARD);
                soft.assertThat(plan.getName()).isEqualTo("My plan");
                soft.assertThat(plan.getNeedRedeployAt()).isEqualTo(Date.from(Instant.parse("2020-02-05T20:22:02.00Z")));
                soft.assertThat(plan.getOrder()).isOne();
                soft.assertThat(plan.getPublishedAt()).isEqualTo(Date.from(Instant.parse("2020-02-03T20:22:02.00Z")));
                soft.assertThat(plan.getSecurity()).isEqualTo(Plan.PlanSecurityType.KEY_LESS);
                soft.assertThat(plan.getSecurityDefinition()).isEqualTo("{\"nice\": \"config\"}");
                soft.assertThat(plan.getSelectionRule()).isEqualTo("{#request.attribute['selectionRule'] != null}");
                soft.assertThat(plan.getStatus()).isEqualTo(Plan.Status.PUBLISHED);
                soft.assertThat(plan.getTags()).isEqualTo(Set.of("tag1", "tag2"));
                soft.assertThat(plan.getType()).isEqualTo(Plan.PlanType.API);
                soft.assertThat(plan.getValidation()).isEqualTo(Plan.PlanValidationType.AUTO);
                soft.assertThat(plan.getUpdatedAt()).isEqualTo(Date.from(Instant.parse("2020-02-02T20:22:02.00Z")));
                soft.assertThat(plan.isCommentRequired()).isFalse();
            });
        }

        private Plan.PlanBuilder planV4() {
            return Plan
                .builder()
                .id("my-id")
                .api("my-api")
                .crossId("cross-id")
                .name("plan-name")
                .description("plan-description")
                .security(Plan.PlanSecurityType.API_KEY)
                .securityDefinition("security-definition")
                .selectionRule("selection-rule")
                .validation(Plan.PlanValidationType.AUTO)
                .mode(Plan.PlanMode.STANDARD)
                .order(1)
                .type(Plan.PlanType.API)
                .status(Plan.Status.PUBLISHED)
                .createdAt(Date.from(Instant.parse("2020-02-01T20:22:02.00Z")))
                .updatedAt(Date.from(Instant.parse("2020-02-02T20:22:02.00Z")))
                .publishedAt(Date.from(Instant.parse("2020-02-03T20:22:02.00Z")))
                .closedAt(Date.from(Instant.parse("2020-02-04T20:22:02.00Z")))
                .needRedeployAt(Date.from(Instant.parse("2020-02-05T20:22:02.00Z")))
                .characteristics(List.of("characteristic-1"))
                .excludedGroups(List.of("excluded-group-1"))
                .commentRequired(true)
                .commentMessage("comment-message")
                .generalConditions("general-conditions")
                .tags(Set.of("tag-1"));
        }

        private Plan.PlanBuilder planV2() {
            return Plan
                .builder()
                .id("my-id")
                .api("my-api")
                .crossId("cross-id")
                .name("plan-name")
                .description("plan-description")
                .security(Plan.PlanSecurityType.API_KEY)
                .securityDefinition("security-definition")
                .selectionRule("selection-rule")
                .validation(Plan.PlanValidationType.AUTO)
                .mode(Plan.PlanMode.STANDARD)
                .order(1)
                .type(Plan.PlanType.API)
                .status(Plan.Status.PUBLISHED)
                .createdAt(Date.from(Instant.parse("2020-02-01T20:22:02.00Z")))
                .updatedAt(Date.from(Instant.parse("2020-02-02T20:22:02.00Z")))
                .publishedAt(Date.from(Instant.parse("2020-02-03T20:22:02.00Z")))
                .closedAt(Date.from(Instant.parse("2020-02-04T20:22:02.00Z")))
                .needRedeployAt(Date.from(Instant.parse("2020-02-05T20:22:02.00Z")))
                .definition("{\"/\":[]}")
                .characteristics(List.of("characteristic-1"))
                .excludedGroups(List.of("excluded-group-1"))
                .commentRequired(true)
                .commentMessage("comment-message")
                .generalConditions("general-conditions")
                .tags(Set.of("tag-1"));
        }
    }

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

            BasePlanEntity planEntity = PlanAdapter.INSTANCE.toEntityV2(plan);

            assertThat(planEntity.getId()).isEqualTo(plan.getId());
            assertThat(planEntity.getName()).isEqualTo(plan.getName());
            assertThat(planEntity.getDescription()).isEqualTo(plan.getDescription());
            assertThat(planEntity.getPlanValidation().name()).isEqualTo(plan.getValidation().name());
            assertThat(planEntity.getPlanStatus().name()).isEqualTo(plan.getStatus().name());
            assertThat(planEntity.getApiId()).isEqualTo(plan.getApi());
            assertThat(planEntity.getGeneralConditions()).isEqualTo(plan.getGeneralConditions());
            assertThat(planEntity.getPlanSecurity().getType()).isEqualTo(io.gravitee.rest.api.model.PlanSecurityType.KEY_LESS.name());
            assertThat(planEntity.getPaths().get("/"))
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
