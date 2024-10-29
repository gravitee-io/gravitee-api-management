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
import io.gravitee.apim.core.api.model.crd.PlanCRD;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.federation.FederatedPlan;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.repository.management.model.Plan;
import io.gravitee.rest.api.model.v4.plan.PlanEntity;
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
        void should_convert_from_v4_repository_to_http_core_model() {
            var repository = planHttpV4().build();

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
                soft.assertThat(plan.getPlanMode()).isEqualTo(io.gravitee.definition.model.v4.plan.PlanMode.STANDARD);
                soft.assertThat(plan.getName()).isEqualTo("plan-name");
                soft.assertThat(plan.getNeedRedeployAt()).isEqualTo(Date.from(Instant.parse("2020-02-05T20:22:02.00Z")));
                soft.assertThat(plan.getOrder()).isOne();
                soft.assertThat(plan.getPublishedAt()).isEqualTo(Instant.parse("2020-02-03T20:22:02.00Z").atZone(ZoneOffset.UTC));
                soft
                    .assertThat(plan.getPlanSecurity())
                    .isEqualTo(PlanSecurity.builder().type("api-key").configuration("security-definition").build());
                soft.assertThat(plan.getPlanDefinitionV4().getSelectionRule()).isEqualTo("selection-rule");
                soft.assertThat(plan.getPlanDefinitionV4().getStatus()).isEqualTo(PlanStatus.PUBLISHED);
                soft.assertThat(plan.getPlanDefinitionV4().getTags()).isEqualTo(Set.of("tag-1"));
                soft.assertThat(plan.getType()).isEqualTo(io.gravitee.apim.core.plan.model.Plan.PlanType.API);
                soft.assertThat(plan.getValidation()).isEqualTo(io.gravitee.apim.core.plan.model.Plan.PlanValidationType.AUTO);
                soft.assertThat(plan.getUpdatedAt()).isEqualTo(Instant.parse("2020-02-02T20:22:02.00Z").atZone(ZoneOffset.UTC));
                soft.assertThat(plan.isCommentRequired()).isTrue();
            });
        }

        @Test
        void should_convert_from_v4_repository_to_native_core_model() {
            var repository = planNativeV4().build();

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
                soft.assertThat(plan.getPlanMode()).isEqualTo(io.gravitee.definition.model.v4.plan.PlanMode.STANDARD);
                soft.assertThat(plan.getName()).isEqualTo("plan-name");
                soft.assertThat(plan.getNeedRedeployAt()).isEqualTo(Date.from(Instant.parse("2020-02-05T20:22:02.00Z")));
                soft.assertThat(plan.getOrder()).isOne();
                soft.assertThat(plan.getPublishedAt()).isEqualTo(Instant.parse("2020-02-03T20:22:02.00Z").atZone(ZoneOffset.UTC));
                soft
                    .assertThat(plan.getPlanSecurity())
                    .isEqualTo(PlanSecurity.builder().type("api-key").configuration("security-definition").build());
                soft.assertThat(plan.getPlanDefinitionNativeV4().getSelectionRule()).isEqualTo("selection-rule");
                soft.assertThat(plan.getPlanDefinitionNativeV4().getStatus()).isEqualTo(PlanStatus.PUBLISHED);
                soft.assertThat(plan.getPlanDefinitionNativeV4().getTags()).isEqualTo(Set.of("tag-1"));
                soft.assertThat(plan.getType()).isEqualTo(io.gravitee.apim.core.plan.model.Plan.PlanType.API);
                soft.assertThat(plan.getValidation()).isEqualTo(io.gravitee.apim.core.plan.model.Plan.PlanValidationType.AUTO);
                soft.assertThat(plan.getUpdatedAt()).isEqualTo(Instant.parse("2020-02-02T20:22:02.00Z").atZone(ZoneOffset.UTC));
                soft.assertThat(plan.isCommentRequired()).isTrue();
            });
        }

        @Test
        void should_convert_from_federated_repository_to_core_model() {
            var repository = federatedPlan().build();

            var plan = PlanAdapter.INSTANCE.fromRepository(repository);

            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(plan.getApiId()).isEqualTo("my-api");
                soft.assertThat(plan.getCharacteristics()).containsExactly("characteristic-1");
                soft.assertThat(plan.getCommentMessage()).isEqualTo("comment-message");
                soft.assertThat(plan.getCreatedAt()).isEqualTo(Instant.parse("2020-02-01T20:22:02.00Z").atZone(ZoneOffset.UTC));
                soft.assertThat(plan.getDefinitionVersion()).isEqualTo(DefinitionVersion.FEDERATED);
                soft.assertThat(plan.getDescription()).isEqualTo("plan-description");
                soft.assertThat(plan.getExcludedGroups()).containsExactly("excluded-group-1");
                soft.assertThat(plan.getGeneralConditions()).isEqualTo("general-conditions");
                soft.assertThat(plan.getId()).isEqualTo("my-id");
                soft.assertThat(plan.getPlanMode()).isEqualTo(io.gravitee.definition.model.v4.plan.PlanMode.STANDARD);
                soft.assertThat(plan.getName()).isEqualTo("plan-name");
                soft.assertThat(plan.getOrder()).isOne();
                soft.assertThat(plan.getPlanSecurity()).isEqualTo(PlanSecurity.builder().type("api-key").build());
                soft.assertThat(plan.getPlanStatus()).isEqualTo(PlanStatus.PUBLISHED);
                soft.assertThat(plan.getType()).isEqualTo(io.gravitee.apim.core.plan.model.Plan.PlanType.API);
                soft.assertThat(plan.getValidation()).isEqualTo(io.gravitee.apim.core.plan.model.Plan.PlanValidationType.MANUAL);
                soft.assertThat(plan.getUpdatedAt()).isEqualTo(Instant.parse("2020-02-02T20:22:02.00Z").atZone(ZoneOffset.UTC));
                soft.assertThat(plan.isCommentRequired()).isTrue();
                soft
                    .assertThat(plan.getFederatedPlanDefinition())
                    .isEqualTo(
                        FederatedPlan
                            .builder()
                            .id("my-id")
                            .providerId("provider-id")
                            .security(PlanSecurity.builder().type("api-key").build())
                            .mode(PlanMode.STANDARD)
                            .status(PlanStatus.PUBLISHED)
                            .build()
                    );
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
                soft.assertThat(plan.getPlanMode()).isEqualTo(io.gravitee.definition.model.v4.plan.PlanMode.STANDARD);
                soft
                    .assertThat(plan.getPlanSecurity())
                    .isEqualTo(PlanSecurity.builder().type("api-key").configuration("security-definition").build());
                soft.assertThat(plan.getPlanStatus()).isEqualTo(PlanStatus.PUBLISHED);
                soft.assertThat(plan.getType()).isEqualTo(io.gravitee.apim.core.plan.model.Plan.PlanType.API);
                soft.assertThat(plan.getValidation()).isEqualTo(io.gravitee.apim.core.plan.model.Plan.PlanValidationType.AUTO);
                soft.assertThat(plan.getPublishedAt()).isEqualTo(Instant.parse("2020-02-03T20:22:02.00Z").atZone(ZoneOffset.UTC));
                soft.assertThat(plan.getPlanDefinitionV2().getPaths()).isEqualTo(Map.of("/", List.of()));
                soft.assertThat(plan.getPlanDefinitionV2().getSelectionRule()).isEqualTo("selection-rule");
                soft.assertThat(plan.getPlanDefinitionV2().getTags()).isEqualTo(Set.of("tag-1"));
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
                .characteristics(List.of("characteristic1", "characteristic2"))
                .commentMessage("Comment message")
                .generalConditions("General conditions")
                .planDefinitionHttpV4(
                    io.gravitee.definition.model.v4.plan.Plan
                        .builder()
                        .security(PlanSecurity.builder().type("key-less").configuration("{\"nice\": \"config\"}").build())
                        .mode(PlanMode.STANDARD)
                        .status(PlanStatus.PUBLISHED)
                        .tags(Set.of("tag1", "tag2"))
                        .selectionRule("{#request.attribute['selectionRule'] != null}")
                        .build()
                )
                .excludedGroups(List.of("excludedGroup1", "excludedGroup2"))
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
                .planDefinitionV2(
                    io.gravitee.definition.model.Plan
                        .builder()
                        .security("key-less")
                        .securityDefinition("{\"nice\": \"config\"}")
                        .selectionRule("{#request.attribute['selectionRule'] != null}")
                        .tags(Set.of("tag1", "tag2"))
                        .paths(Map.of("/", List.of()))
                        .status("PUBLISHED")
                        .build()
                )
                .closedAt(Instant.parse("2020-02-04T20:22:02.00Z").atZone(ZoneOffset.UTC))
                .needRedeployAt(Date.from(Instant.parse("2020-02-05T20:22:02.00Z")))
                .publishedAt(Instant.parse("2020-02-03T20:22:02.00Z").atZone(ZoneOffset.UTC))
                .characteristics(List.of("characteristic1", "characteristic2"))
                .commentMessage("Comment message")
                .generalConditions("General conditions")
                .excludedGroups(List.of("excludedGroup1", "excludedGroup2"))
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

        @Test
        void should_convert_federated_plan_to_repository() {
            var model = PlanFixtures
                .aFederatedPlan()
                .toBuilder()
                .closedAt(Instant.parse("2020-02-04T20:22:02.00Z").atZone(ZoneOffset.UTC))
                .publishedAt(Instant.parse("2020-02-03T20:22:02.00Z").atZone(ZoneOffset.UTC))
                .characteristics(List.of("characteristic1", "characteristic2"))
                .commentMessage("Comment message")
                .generalConditions("General conditions")
                .federatedPlanDefinition(fixtures.definition.PlanFixtures.aFederatedPlan())
                .excludedGroups(List.of("excludedGroup1", "excludedGroup2"))
                .build();

            var plan = PlanAdapter.INSTANCE.toRepository(model);

            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(plan.getApi()).isEqualTo("my-api");
                soft.assertThat(plan.getCharacteristics()).containsExactly("characteristic1", "characteristic2");
                soft.assertThat(plan.getClosedAt()).isEqualTo(Date.from(Instant.parse("2020-02-04T20:22:02.00Z")));
                soft.assertThat(plan.getCommentMessage()).isEqualTo("Comment message");
                soft.assertThat(plan.getCreatedAt()).isEqualTo(Date.from(Instant.parse("2020-02-01T20:22:02.00Z")));
                soft.assertThat(plan.getDefinitionVersion()).isEqualTo(DefinitionVersion.FEDERATED);
                soft.assertThat(plan.getDescription()).isEqualTo("Description");
                soft.assertThat(plan.getExcludedGroups()).containsExactly("excludedGroup1", "excludedGroup2");
                soft.assertThat(plan.getGeneralConditions()).isEqualTo("General conditions");
                soft.assertThat(plan.getId()).isEqualTo("federated");
                soft.assertThat(plan.getMode()).isEqualTo(Plan.PlanMode.STANDARD);
                soft.assertThat(plan.getName()).isEqualTo("Federated Plan");
                soft.assertThat(plan.getOrder()).isOne();
                soft.assertThat(plan.getSecurity()).isEqualTo(Plan.PlanSecurityType.API_KEY);
                soft.assertThat(plan.getStatus()).isEqualTo(Plan.Status.PUBLISHED);
                soft.assertThat(plan.getType()).isEqualTo(Plan.PlanType.API);
                soft.assertThat(plan.getValidation()).isEqualTo(Plan.PlanValidationType.MANUAL);
                soft.assertThat(plan.getUpdatedAt()).isEqualTo(Date.from(Instant.parse("2020-02-02T20:22:02.00Z")));
                soft.assertThat(plan.isCommentRequired()).isFalse();
                soft
                    .assertThat(plan.getDefinition())
                    .isEqualTo(
                        "{\"id\":\"my-plan\",\"providerId\":\"provider-id\",\"security\":{\"type\":\"api-key\"},\"mode\":\"standard\",\"status\":\"published\"}"
                    );
            });
        }

        private Plan.PlanBuilder planHttpV4() {
            return Plan
                .builder()
                .id("my-id")
                .api("my-api")
                .crossId("cross-id")
                .name("plan-name")
                .definitionVersion(DefinitionVersion.V4)
                .apiType(ApiType.PROXY)
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

        private Plan.PlanBuilder planNativeV4() {
            return Plan
                .builder()
                .id("my-id")
                .api("my-api")
                .crossId("cross-id")
                .name("plan-name")
                .definitionVersion(DefinitionVersion.V4)
                .apiType(ApiType.NATIVE)
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

        private Plan.PlanBuilder federatedPlan() {
            return Plan
                .builder()
                .id("my-id")
                .api("my-api")
                .name("plan-name")
                .definitionVersion(DefinitionVersion.FEDERATED)
                .description("plan-description")
                .security(Plan.PlanSecurityType.API_KEY)
                .validation(Plan.PlanValidationType.MANUAL)
                .mode(Plan.PlanMode.STANDARD)
                .order(1)
                .type(Plan.PlanType.API)
                .status(Plan.Status.PUBLISHED)
                .createdAt(Date.from(Instant.parse("2020-02-01T20:22:02.00Z")))
                .updatedAt(Date.from(Instant.parse("2020-02-02T20:22:02.00Z")))
                .characteristics(List.of("characteristic-1"))
                .excludedGroups(List.of("excluded-group-1"))
                .commentRequired(true)
                .commentMessage("comment-message")
                .generalConditions("general-conditions")
                .definition(
                    "{\"id\":\"my-id\",\"providerId\":\"provider-id\",\"security\":{\"type\":\"api-key\"},\"mode\":\"standard\",\"status\":\"published\"}"
                );
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
            var plan = PlanFixtures
                .anApiKeyV4()
                .toBuilder()
                .planDefinitionHttpV4(
                    fixtures.definition.PlanFixtures.HttpV4Definition
                        .anApiKeyV4()
                        .toBuilder()
                        .security(PlanSecurity.builder().type(PlanSecurityType.API_KEY.getLabel()).configuration("{}").build())
                        .build()
                )
                .build();

            PlanEntity planEntity = PlanAdapter.INSTANCE.toEntityV4(plan);

            assertThat(planEntity.getId()).isEqualTo(plan.getId());
            assertThat(planEntity.getName()).isEqualTo(plan.getName());
            assertThat(planEntity.getDescription()).isEqualTo(plan.getDescription());
            assertThat(planEntity.getPlanValidation().name()).isEqualTo(plan.getValidation().name());
            assertThat(planEntity.getPlanStatus().name()).isEqualTo(plan.getPlanStatus().name());
            assertThat(planEntity.getApiId()).isEqualTo(plan.getApiId());
            assertThat(planEntity.getGeneralConditions()).isEqualTo(plan.getGeneralConditions());
            assertThat(planEntity.getTags()).isEqualTo(plan.getPlanDefinitionHttpV4().getTags());
            assertThat(planEntity.getSelectionRule()).isEqualTo(plan.getPlanDefinitionV4().getSelectionRule());
            assertThat(planEntity.getPlanSecurity().getType()).isEqualTo(PlanSecurityType.API_KEY.getLabel());
            assertThat(planEntity.getPlanSecurity().getConfiguration())
                .isEqualTo(plan.getPlanDefinitionV4().getSecurity().getConfiguration());
        }

        @Test
        public void should_convert_push_plan_to_plan_entity() {
            var plan = PlanFixtures.aPushPlan();

            PlanEntity planEntity = PlanAdapter.INSTANCE.toEntityV4(plan);

            assertThat(planEntity.getId()).isEqualTo(plan.getId());
            assertThat(planEntity.getName()).isEqualTo(plan.getName());
            assertThat(planEntity.getDescription()).isEqualTo(plan.getDescription());
            assertThat(planEntity.getPlanValidation().name()).isEqualTo(plan.getValidation().name());
            assertThat(planEntity.getPlanStatus().name()).isEqualTo(plan.getPlanStatus().name());
            assertThat(planEntity.getApiId()).isEqualTo(plan.getApiId());
            assertThat(planEntity.getGeneralConditions()).isEqualTo(plan.getGeneralConditions());
            assertThat(planEntity.getTags()).isEqualTo(plan.getPlanDefinitionV4().getTags());
            assertThat(planEntity.getSelectionRule()).isEqualTo(plan.getPlanDefinitionV4().getSelectionRule());
            assertThat(planEntity.getPlanMode()).isEqualTo(PlanMode.PUSH);
            assertThat(planEntity.getPlanSecurity()).isNull();
        }
    }

    @Nested
    class toCRD {

        @Test
        public void should_convert_plan_to_crd() {
            var plan = PlanFixtures
                .anApiKeyV4()
                .toBuilder()
                .planDefinitionHttpV4(
                    fixtures.definition.PlanFixtures.HttpV4Definition
                        .anApiKeyV4()
                        .toBuilder()
                        .security(PlanSecurity.builder().type(PlanSecurityType.API_KEY.getLabel()).configuration("{}").build())
                        .build()
                )
                .build();

            PlanCRD planEntity = PlanAdapter.INSTANCE.toCRD(plan);

            assertThat(planEntity.getId()).isEqualTo(plan.getId());
            assertThat(planEntity.getCrossId()).isEqualTo(plan.getCrossId());
            assertThat(planEntity.getName()).isEqualTo(plan.getName());
            assertThat(planEntity.getDescription()).isEqualTo(plan.getDescription());
            assertThat(planEntity.getSecurity()).isEqualTo(plan.getPlanSecurity());
            assertThat(planEntity.getCharacteristics()).isEqualTo(plan.getCharacteristics());
            assertThat(planEntity.getExcludedGroups()).isEqualTo(plan.getExcludedGroups());
            assertThat(planEntity.getGeneralConditions()).isEqualTo(plan.getGeneralConditions());
            assertThat(planEntity.getOrder()).isEqualTo(plan.getOrder());
            assertThat(planEntity.getSelectionRule()).isEqualTo(plan.getPlanDefinitionV4().getSelectionRule());
            assertThat(planEntity.getStatus()).isEqualTo(plan.getPlanStatus());
            assertThat(planEntity.getValidation()).isEqualTo(plan.getValidation());
            assertThat(planEntity.getTags()).isEqualTo(plan.getPlanDefinitionV4().getTags());
            assertThat(planEntity.getType()).isEqualTo(plan.getType());
        }
    }

    @Nested
    class ToEntityV2 {

        @Test
        public void should_convert_plan_to_plan_entity() {
            var plan = PlanFixtures.aPlanV2();

            io.gravitee.rest.api.model.PlanEntity planEntity = PlanAdapter.INSTANCE.toEntityV2(plan);

            assertThat(planEntity.getId()).isEqualTo(plan.getId());
            assertThat(planEntity.getCrossId()).isEqualTo(plan.getCrossId());
            assertThat(planEntity.getName()).isEqualTo(plan.getName());
            assertThat(planEntity.getDescription()).isEqualTo(plan.getDescription());
            assertThat(planEntity.getPlanValidation().name()).isEqualTo(plan.getValidation().name());
            assertThat(planEntity.getType().name()).isEqualTo(plan.getType().name());
            assertThat(planEntity.getPlanStatus().name()).isEqualTo(plan.getPlanStatus().name());
            assertThat(planEntity.getApiId()).isEqualTo(plan.getApiId());
            assertThat(planEntity.getTags()).isEqualTo(plan.getPlanDefinitionV2().getTags());
            assertThat(planEntity.getSelectionRule()).isEqualTo(plan.getPlanDefinitionV2().getSelectionRule());
            assertThat(planEntity.getGeneralConditions()).isEqualTo(plan.getGeneralConditions());
            assertThat(planEntity.getPlanSecurity().getType()).isEqualTo(io.gravitee.rest.api.model.PlanSecurityType.KEY_LESS.name());
            assertThat(planEntity.getSecurityDefinition()).isEqualTo(plan.getPlanSecurity().getConfiguration());
        }
    }
}
