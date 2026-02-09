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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import inmemory.AuditCrudServiceInMemory;
import inmemory.EntrypointPluginQueryServiceInMemory;
import inmemory.FlowCrudServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.PageCrudServiceInMemory;
import inmemory.ParametersQueryServiceInMemory;
import inmemory.PlanCrudServiceInMemory;
import inmemory.PlanQueryServiceInMemory;
import inmemory.SubscriptionQueryServiceInMemory;
import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.event.crud_service.EventCrudService;
import io.gravitee.apim.core.event.crud_service.EventLatestCrudService;
import io.gravitee.apim.core.flow.domain_service.FlowValidationDomainService;
import io.gravitee.apim.core.plan.domain_service.CreatePlanDomainService;
import io.gravitee.apim.core.plan.domain_service.DeletePlanDomainService;
import io.gravitee.apim.core.plan.domain_service.PlanSynchronizationService;
import io.gravitee.apim.core.plan.domain_service.PlanValidatorDomainService;
import io.gravitee.apim.core.plan.domain_service.ReorderPlanDomainService;
import io.gravitee.apim.core.plan.domain_service.UpdatePlanDomainService;
import io.gravitee.apim.core.policy.domain_service.PolicyValidationDomainService;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.repository.management.model.Parameter;
import io.gravitee.repository.management.model.ParameterReferenceType;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.service.v4.ApiService;
import java.util.List;
import java.util.stream.Stream;

public class ImportDefinitionPlanDomainServiceTestInitializer {

    // Mocks
    public final ApiService apiService = mock(ApiService.class);
    public final PlanSynchronizationService planSynchronizationService = mock(PlanSynchronizationService.class);
    public final PolicyValidationDomainService policyValidationDomainService = mock(PolicyValidationDomainService.class);
    public final EventCrudService eventCrudService = mock(EventCrudService.class);
    public final EventLatestCrudService eventLatestCrudService = mock(EventLatestCrudService.class);

    // In Memory
    public final AuditCrudServiceInMemory auditCrudServiceInMemory = new AuditCrudServiceInMemory();
    public final EntrypointPluginQueryServiceInMemory entrypointPluginQueryServiceInMemory = new EntrypointPluginQueryServiceInMemory();
    public final FlowCrudServiceInMemory flowCrudServiceInMemory = new FlowCrudServiceInMemory();
    public final ParametersQueryServiceInMemory parametersQueryService = new ParametersQueryServiceInMemory();
    public final PageCrudServiceInMemory pageCrudServiceInMemory = new PageCrudServiceInMemory();
    public final PlanQueryServiceInMemory planQueryServiceInMemory = new PlanQueryServiceInMemory();
    public final PlanCrudServiceInMemory planCrudServiceInMemory = new PlanCrudServiceInMemory();
    public final SubscriptionQueryServiceInMemory subscriptionQueryServiceInMemory = new SubscriptionQueryServiceInMemory();
    public final UserCrudServiceInMemory userCrudServiceInMemory = new UserCrudServiceInMemory();

    // Domain Services
    private final CreatePlanDomainService createPlanDomainService;
    private final DeletePlanDomainService deletePlanDomainService;
    private final UpdatePlanDomainService updatePlanDomainService;

    ImportDefinitionPlanDomainServiceTestInitializer() {
        var auditDomainService = new AuditDomainService(auditCrudServiceInMemory, userCrudServiceInMemory, new JacksonJsonDiffProcessor());
        var planValidatorDomainService = new PlanValidatorDomainService(
            parametersQueryService,
            policyValidationDomainService,
            pageCrudServiceInMemory
        );
        var flowValidationDomainService = new FlowValidationDomainService(
            policyValidationDomainService,
            entrypointPluginQueryServiceInMemory
        );
        createPlanDomainService = new CreatePlanDomainService(
            planValidatorDomainService,
            flowValidationDomainService,
            planCrudServiceInMemory,
            flowCrudServiceInMemory,
            auditDomainService
        );

        var reorderPlanDomainService = new ReorderPlanDomainService(planQueryServiceInMemory, planCrudServiceInMemory);
        updatePlanDomainService = new UpdatePlanDomainService(
            planQueryServiceInMemory,
            planCrudServiceInMemory,
            planValidatorDomainService,
            flowValidationDomainService,
            flowCrudServiceInMemory,
            auditDomainService,
            planSynchronizationService,
            reorderPlanDomainService
        );

        deletePlanDomainService = new DeletePlanDomainService(
            planCrudServiceInMemory,
            subscriptionQueryServiceInMemory,
            auditDomainService
        );
    }

    ImportDefinitionPlanDomainService initialize(String environmentId) {
        parametersQueryService.initWith(
            List.of(
                new Parameter(Key.PLAN_SECURITY_APIKEY_ENABLED.key(), environmentId, ParameterReferenceType.ENVIRONMENT, "true"),
                new Parameter(Key.PLAN_SECURITY_KEYLESS_ENABLED.key(), environmentId, ParameterReferenceType.ENVIRONMENT, "true")
            )
        );
        when(policyValidationDomainService.validateAndSanitizeConfiguration(any(), any())).thenAnswer(invocation ->
            invocation.getArgument(1)
        );

        return new ImportDefinitionPlanDomainService(
            createPlanDomainService,
            updatePlanDomainService,
            deletePlanDomainService,
            planCrudServiceInMemory
        );
    }

    void tearDown() {
        Stream.of(
            auditCrudServiceInMemory,
            entrypointPluginQueryServiceInMemory,
            flowCrudServiceInMemory,
            parametersQueryService,
            pageCrudServiceInMemory,
            planQueryServiceInMemory,
            planCrudServiceInMemory,
            subscriptionQueryServiceInMemory,
            userCrudServiceInMemory
        ).forEach(InMemoryAlternative::reset);

        reset(planSynchronizationService);
        reset(policyValidationDomainService);
    }
}
