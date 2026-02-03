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
package io.gravitee.apim.core.api.domain_service.import_definition;

import static org.assertj.core.api.Assertions.assertThat;

import fixtures.core.model.ApiFixtures;
import fixtures.core.model.PlanFixtures;
import fixtures.definition.FlowFixtures;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.plan.model.PlanWithFlows;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ImportDefinitionPlanDomainServiceTest {

    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String USER = "user";
    private static final AuditInfo AUDIT_INFO = AuditInfo.builder()
        .organizationId(ORGANIZATION_ID)
        .environmentId(ENVIRONMENT_ID)
        .actor(AuditActor.builder().userId(USER).build())
        .build();
    private static final String API_ID = "api-id";
    private static final String API_CROSS_ID = "api-cross-id";
    private static final Api EXISTING_API = ApiFixtures.aMessageApiV4()
        .toBuilder()
        .id(API_ID)
        .crossId(API_CROSS_ID)
        .name("api name")
        .environmentId(ENVIRONMENT_ID)
        .build();

    private final ImportDefinitionPlanDomainServiceTestInitializer initializer = new ImportDefinitionPlanDomainServiceTestInitializer();
    private ImportDefinitionPlanDomainService service;

    @BeforeEach
    void setUp() {
        service = initializer.initialize(ENVIRONMENT_ID);
    }

    @AfterEach
    void tearDown() {
        initializer.tearDown();
    }

    @Test
    @SneakyThrows
    public void should_update_api_plan() {
        var planToUpdate = PlanFixtures.HttpV4.anApiKey()
            .toBuilder()
            .crossId("plan-to-update")
            .apiId(API_ID)
            .referenceType(GenericPlanEntity.ReferenceType.API)
            .referenceId(API_ID)
            .environmentId(ENVIRONMENT_ID)
            .build();
        initializer.planCrudServiceInMemory.initWith(List.of(planToUpdate));

        Set<PlanWithFlows> importDefinitionPlans = Set.of(
            PlanWithFlows.builder()
                .id(planToUpdate.getId())
                .crossId(planToUpdate.getCrossId())
                .apiId(planToUpdate.getApiId())
                .referenceType(GenericPlanEntity.ReferenceType.API)
                .referenceId(API_ID)
                .environmentId(planToUpdate.getEnvironmentId())
                .definitionVersion(planToUpdate.getDefinitionVersion())
                .type(planToUpdate.getType())
                .planDefinitionHttpV4(planToUpdate.getPlanDefinitionHttpV4())
                .name("updated plan")
                .flows(List.of(FlowFixtures.aMessageFlowV4().toBuilder().id("updated-plan-flow").build()))
                .build()
        );

        assertThat(initializer.flowCrudServiceInMemory.getApiV4Flows(API_ID)).isEmpty();

        service.upsertPlanWithFlows(EXISTING_API, importDefinitionPlans, AUDIT_INFO);

        var apiPlans = initializer.planCrudServiceInMemory.findByApiId(API_ID);
        assertThat(apiPlans)
            .hasSize(1)
            .first()
            .satisfies(updatedPlan -> {
                assertThat(updatedPlan.getApiId()).isEqualTo(API_ID);
                assertThat(updatedPlan.getEnvironmentId()).isEqualTo(ENVIRONMENT_ID);
                assertThat(updatedPlan.getDefinitionVersion()).isEqualTo(DefinitionVersion.V4);
                assertThat(updatedPlan.getPlanDefinitionHttpV4().getSecurity().getType()).isEqualTo("api-key");
                assertThat(updatedPlan.getName()).isEqualTo("updated plan");
                assertThat(updatedPlan.getCrossId()).isEqualTo("plan-to-update");
            });

        assertThat(initializer.flowCrudServiceInMemory.getPlanV4Flows(planToUpdate.getId()))
            .extracting(Flow::getId)
            .containsOnly("updated-plan-flow");
    }

    @Test
    @SneakyThrows
    public void should_create_api_plans() {
        var planToCreate = PlanWithFlows.builder()
            .crossId("plan-to-create")
            .name("plan to create")
            .apiId(API_ID)
            .type(Plan.PlanType.API)
            .referenceType(Plan.ReferenceType.API)
            .referenceId(API_ID)
            .environmentId(ENVIRONMENT_ID)
            .definitionVersion(DefinitionVersion.V4)
            .planDefinitionHttpV4(
                io.gravitee.definition.model.v4.plan.Plan.builder()
                    .security(PlanSecurity.builder().type("api-key").build())
                    .mode(PlanMode.STANDARD)
                    .build()
            )
            .flows(Collections.emptyList())
            .build();

        assertThat(initializer.flowCrudServiceInMemory.getApiV4Flows(API_ID)).isEmpty();

        service.upsertPlanWithFlows(EXISTING_API, Set.of(planToCreate), AUDIT_INFO);

        var apiPlans = initializer.planCrudServiceInMemory.findByApiId(API_ID);
        assertThat(apiPlans)
            .hasSize(1)
            .first()
            .satisfies(
                (createdPlan -> {
                        assertThat(createdPlan.getApiId()).isEqualTo(API_ID);
                        assertThat(createdPlan.getEnvironmentId()).isEqualTo(ENVIRONMENT_ID);
                        assertThat(createdPlan.getDefinitionVersion()).isEqualTo(DefinitionVersion.V4);
                        assertThat(createdPlan.getPlanDefinitionHttpV4().getSecurity().getType()).isEqualTo("api-key");
                        assertThat(createdPlan.getPlanDefinitionHttpV4().getFlows()).isNullOrEmpty();
                        assertThat(createdPlan.getName()).isEqualTo("plan to create");
                        assertThat(createdPlan.getCrossId()).isEqualTo("plan-to-create");
                    })
            );
    }

    @Test
    @SneakyThrows
    public void should_remove_api_plans() {
        var planToUpdate = PlanFixtures.HttpV4.anApiKey()
            .toBuilder()
            .crossId("plan-to-update")
            .apiId(API_ID)
            .referenceType(GenericPlanEntity.ReferenceType.API)
            .referenceId(API_ID)
            .environmentId(ENVIRONMENT_ID)
            .build();
        var planToRemove = PlanFixtures.HttpV4.aKeyless()
            .toBuilder()
            .crossId("plan-to-remove")
            .apiId(API_ID)
            .referenceType(GenericPlanEntity.ReferenceType.API)
            .referenceId(API_ID)
            .environmentId(ENVIRONMENT_ID)
            .build();
        initializer.planCrudServiceInMemory.initWith(List.of(planToUpdate, planToRemove));

        Set<PlanWithFlows> importDefinitionPlans = Set.of(
            PlanWithFlows.builder()
                .id(planToUpdate.getId())
                .crossId(planToUpdate.getCrossId())
                .apiId(planToUpdate.getApiId())
                .referenceType(Plan.ReferenceType.API)
                .referenceId(API_ID)
                .environmentId(planToUpdate.getEnvironmentId())
                .definitionVersion(planToUpdate.getDefinitionVersion())
                .type(planToUpdate.getType())
                .planDefinitionHttpV4(planToUpdate.getPlanDefinitionHttpV4())
                .name("updated plan")
                .flows(Collections.emptyList())
                .build()
        );

        service.upsertPlanWithFlows(EXISTING_API, importDefinitionPlans, AUDIT_INFO);

        var apiPlans = initializer.planCrudServiceInMemory.findByApiId(API_ID);
        assertThat(apiPlans).hasSize(1).extracting(Plan::getCrossId).containsExactlyInAnyOrder(planToUpdate.getCrossId());
    }
}
