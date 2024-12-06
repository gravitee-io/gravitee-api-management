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
package io.gravitee.apim.core.plan.use_case;

import static assertions.CoreAssertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import fakes.FakePolicyValidationDomainService;
import fixtures.core.model.ApiFixtures;
import fixtures.core.model.PlanFixtures;
import fixtures.definition.FlowFixtures;
import inmemory.ApiCrudServiceInMemory;
import inmemory.AuditCrudServiceInMemory;
import inmemory.EntrypointPluginQueryServiceInMemory;
import inmemory.FlowCrudServiceInMemory;
import inmemory.PageCrudServiceInMemory;
import inmemory.ParametersQueryServiceInMemory;
import inmemory.PlanCrudServiceInMemory;
import inmemory.PlanQueryServiceInMemory;
import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.flow.domain_service.FlowValidationDomainService;
import io.gravitee.apim.core.json.JsonDiffProcessor;
import io.gravitee.apim.core.plan.domain_service.PlanSynchronizationService;
import io.gravitee.apim.core.plan.domain_service.PlanValidatorDomainService;
import io.gravitee.apim.core.plan.domain_service.ReorderPlanDomainService;
import io.gravitee.apim.core.plan.domain_service.UpdatePlanDomainService;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.plan.model.PlanWithFlows;
import io.gravitee.apim.core.policy.domain_service.PolicyValidationDomainService;
import io.gravitee.apim.infra.domain_service.plan.PlanSynchronizationLegacyWrapper;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.repository.management.model.Parameter;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.v4.plan.PlanValidationType;
import io.gravitee.rest.api.model.v4.plan.UpdatePlanEntity;
import io.gravitee.rest.api.service.processor.SynchronizationService;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class UpdatePlanUseCaseTest {

    public static final String API_ID = "api-id";
    public static final String PLAN_ID = "plan-id";
    private final ObjectMapper objectMapper = new ObjectMapper();
    PlanCrudServiceInMemory planCrudService = new PlanCrudServiceInMemory();
    PlanQueryServiceInMemory planQueryService = new PlanQueryServiceInMemory();
    FlowCrudServiceInMemory flowCrudService = new FlowCrudServiceInMemory();
    PolicyValidationDomainService policyValidationDomainService = new FakePolicyValidationDomainService();
    EntrypointPluginQueryServiceInMemory entrypointConnectorPluginService = new EntrypointPluginQueryServiceInMemory();
    FlowValidationDomainService flowValidationDomainService = new FlowValidationDomainService(
        policyValidationDomainService,
        entrypointConnectorPluginService
    );
    ParametersQueryServiceInMemory parametersQueryService = new ParametersQueryServiceInMemory();
    PageCrudServiceInMemory pageCrudService = new PageCrudServiceInMemory();
    PlanValidatorDomainService planValidatorDomainService = new PlanValidatorDomainService(
        parametersQueryService,
        policyValidationDomainService,
        pageCrudService
    );
    SynchronizationService synchronizationService = new SynchronizationService(objectMapper);
    PlanSynchronizationService planSynchronizationService = new PlanSynchronizationLegacyWrapper(synchronizationService);
    AuditCrudServiceInMemory auditCrudService = new AuditCrudServiceInMemory();
    UserCrudServiceInMemory userCrudService = new UserCrudServiceInMemory();
    JsonDiffProcessor jsonDiffProcessor = new JacksonJsonDiffProcessor();
    AuditDomainService auditDomainService = new AuditDomainService(auditCrudService, userCrudService, jsonDiffProcessor);
    ReorderPlanDomainService reorderPlanDomainService = new ReorderPlanDomainService(planQueryService, planCrudService);
    UpdatePlanDomainService updatePlanDomainService = new UpdatePlanDomainService(
        planQueryService,
        planCrudService,
        planValidatorDomainService,
        flowValidationDomainService,
        flowCrudService,
        auditDomainService,
        planSynchronizationService,
        reorderPlanDomainService
    );
    ApiCrudServiceInMemory apiCrudService = new ApiCrudServiceInMemory();

    UpdatePlanUseCase updatePlanUseCase = new UpdatePlanUseCase(updatePlanDomainService, planCrudService, apiCrudService);

    @ParameterizedTest
    @MethodSource("minMaxPlans")
    void should_update_plan(UpdatePlanEntity updatePlan) {
        // Given
        var plan = PlanFixtures.aPlanHttpV4().toBuilder().id(PLAN_ID).apiId(API_ID).order(1).build();
        var anotherPlan = plan.toBuilder().id("another-plan-id").order(2).build();
        List<Plan> allPlans = List.of(plan, anotherPlan);
        planCrudService.initWith(allPlans);
        planQueryService.initWith(allPlans);
        apiCrudService.initWith(List.of(ApiFixtures.aProxyApiV4().toBuilder().id(API_ID).build()));
        parametersQueryService.initWith(
            List.of(
                Parameter.builder().key(Key.PLAN_SECURITY_KEYLESS_ENABLED.key()).value("true").build(),
                Parameter.builder().key(Key.PLAN_SECURITY_APIKEY_ENABLED.key()).value("true").build()
            )
        );

        var input = new UpdatePlanUseCase.Input(
            updatePlan,
            _api -> Collections.singletonList(FlowFixtures.aProxyFlowV4()),
            API_ID,
            new AuditInfo("user-id", "user-name", AuditActor.builder().build())
        );

        // When
        var output = updatePlanUseCase.execute(input);

        // Then
        assertThat(output)
            .isNotNull()
            .extracting(UpdatePlanUseCase.Output::updated)
            .extracting(
                PlanWithFlows::getId,
                PlanWithFlows::getCrossId,
                PlanWithFlows::getName,
                PlanWithFlows::getDescription,
                PlanWithFlows::getValidation,
                PlanWithFlows::getOrder,
                PlanWithFlows::getPlanSecurity,
                PlanWithFlows::getCharacteristics,
                PlanWithFlows::getExcludedGroups,
                PlanWithFlows::isCommentRequired,
                PlanWithFlows::getCommentMessage,
                PlanWithFlows::getGeneralConditions,
                PlanWithFlows::getPlanTags,
                PlanWithFlows::getSelectionRule
            )
            .containsExactly(
                PLAN_ID,
                "my-plan-crossId",
                updatePlan.getName(),
                updatePlan.getDescription(),
                Plan.PlanValidationType.valueOf(updatePlan.getValidation().name().toUpperCase()),
                updatePlan.getOrder(),
                updatePlan.getSecurity(),
                updatePlan.getCharacteristics(),
                updatePlan.getExcludedGroups(),
                updatePlan.isCommentRequired(),
                updatePlan.getCommentMessage(),
                updatePlan.getGeneralConditions(),
                updatePlan.getTags(),
                updatePlan.getSelectionRule()
            );
    }

    static Stream<Arguments> minMaxPlans() {
        var minPlan = planMinimal();

        var maxPlan = new UpdatePlanEntity();
        maxPlan.setId(PLAN_ID);
        maxPlan.setCrossId("my-plan-crossId");
        maxPlan.setName("plan-name-changed");
        maxPlan.setDescription("plan-description-changed");
        maxPlan.setValidation(PlanValidationType.MANUAL);
        maxPlan.setOrder(2);

        maxPlan.setCharacteristics(List.of("characteristic1", "characteristic2"));
        maxPlan.setExcludedGroups(List.of("group1", "group2"));
        maxPlan.setSecurity(PlanSecurity.builder().type("api-key").build());
        maxPlan.setCommentRequired(true);
        maxPlan.setCommentMessage("comment-message");
        maxPlan.setGeneralConditions("general-conditions");
        maxPlan.setTags(Set.of("tag1", "tag2"));
        maxPlan.setSelectionRule("selection-rule");

        return Stream.of(Arguments.of(minPlan), Arguments.of(maxPlan));
    }

    private static @NotNull UpdatePlanEntity planMinimal() {
        var minPlan = new UpdatePlanEntity();
        minPlan.setId(PLAN_ID);
        minPlan.setCrossId("my-plan-crossId");
        minPlan.setName("plan-name-changed");
        minPlan.setDescription("plan-description-changed");
        minPlan.setValidation(PlanValidationType.MANUAL);
        minPlan.setOrder(2);
        minPlan.setSecurity(PlanSecurity.builder().type("api-key").build());
        return minPlan;
    }
}
