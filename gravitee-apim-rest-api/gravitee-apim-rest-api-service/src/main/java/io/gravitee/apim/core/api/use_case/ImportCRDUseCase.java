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
package io.gravitee.apim.core.api.use_case;

import io.gravitee.apim.core.api.domain_service.ApiMetadataDomainService;
import io.gravitee.apim.core.api.domain_service.CreateApiDomainService;
import io.gravitee.apim.core.api.domain_service.UpdateApiDomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.ApiCRD;
import io.gravitee.apim.core.api.model.ApiCRDStatus;
import io.gravitee.apim.core.api.query_service.ApiQueryService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.plan.domain_service.CreatePlanDomainService;
import io.gravitee.apim.core.plan.domain_service.UpdatePlansDomainService;
import io.gravitee.apim.infra.adapter.ApiAdapter;
import io.gravitee.apim.infra.adapter.PlanAdapter;
import io.gravitee.rest.api.model.v4.plan.BasePlanEntity;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.stream.Collectors;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ImportCRDUseCase {

    private final ApiQueryService apiQueryService;
    private final CreateApiDomainService createApiDomainService;
    private final UpdateApiDomainService updateApiDomainService;
    private final CreatePlanDomainService createPlanDomainService;
    private final UpdatePlansDomainService updatePlansDomainService;
    private final ApiMetadataDomainService apiMetadataDomainService;

    // ApiCRDAdapter
    // PlanCRDAdapter
    // APIDomainService  <-
    // PlanDomainService <- update : determine which plans get deleted, created or updated
    // FlowDomainService <- just call crud service

    public ImportCRDUseCase(
        ApiQueryService apiQueryService,
        CreateApiDomainService createApiDomainService,
        UpdateApiDomainService updateApiDomainService,
        CreatePlanDomainService createPlanDomainService,
        UpdatePlansDomainService updatePlansDomainService,
        ApiMetadataDomainService apiMetadataDomainService
    ) {
        this.apiQueryService = apiQueryService;
        this.createApiDomainService = createApiDomainService;
        this.updateApiDomainService = updateApiDomainService;
        this.createPlanDomainService = createPlanDomainService;
        this.updatePlansDomainService = updatePlansDomainService;
        this.apiMetadataDomainService = apiMetadataDomainService;
    }

    public record Output(ApiCRDStatus status) {}

    public record Input(AuditInfo auditInfo, ApiCRD crd) {}

    public Output execute(Input request) {
        return new Output(createOrUpdate(request));
    }

    private ApiCRDStatus createOrUpdate(Input request) {
        return apiQueryService
            .findByEnvironmentIdAndCrossId(request.auditInfo.environmentId(), request.crd.getCrossId())
            .map(api -> this.update(api, request))
            .orElseGet(() -> this.create(request));
    }

    private ApiCRDStatus create(Input request) {
        try {
            var api = createApiDomainService.create(ApiAdapter.INSTANCE.fromCRD(request.crd), request.auditInfo);
            apiMetadataDomainService.saveApiMetadata(api.getId(), request.crd.getMetadata(), request.auditInfo);

            var planNameIdMapping = request.crd
                .getPlans()
                .stream()
                .map(plan -> createPlanDomainService.create(plan.toBuilder().apiId(api.getId()).build(), request.auditInfo))
                .collect(Collectors.toMap(BasePlanEntity::getName, BasePlanEntity::getId));

            if (request.crd.getDefinitionContext().syncFrom().equals("MANAGEMENT")) {
                // deploy API so that it's sync from DB
            }

            return ApiCRDStatus
                .builder()
                .id(api.getId())
                .crossId(api.getCrossId())
                .environmentId(api.getEnvironmentId())
                .organizationId(request.auditInfo.organizationId())
                .state(api.getLifecycleState().name())
                .plans(planNameIdMapping)
                .build();
        } catch (Exception e) {
            throw new TechnicalManagementException(e);
        }
    }

    private ApiCRDStatus update(Api existingApi, Input request) {
        var api = ApiAdapter.INSTANCE.fromCRD(request.crd);
        // create / update / delete plans
        //      create / update / delete plan flows
        // create / update / delete api flows
        // find list
        //  if in spec but not in the list => create
        // if not in spec but in the list => delete
        // if in both => update
        return ApiCRDStatus.builder().build();
    }
}
