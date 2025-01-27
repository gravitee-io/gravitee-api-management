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
package io.gravitee.apim.infra.domain_service.api;

import io.gravitee.apim.core.api.domain_service.ApiExportDomainService;
import io.gravitee.apim.core.api.model.import_definition.GraviteeDefinition;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.infra.adapter.GraviteeDefinitionAdapter;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.plan.BasePlanEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.v4.ApiImportExportService;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ApiExportDomainServiceImpl implements ApiExportDomainService {

    private final ApiImportExportService exportService;

    @Override
    public GraviteeDefinition export(String apiId, AuditInfo auditInfo) {
        var executionContext = new ExecutionContext(auditInfo.organizationId(), auditInfo.environmentId());
        var exportEntity = exportService.exportApi(executionContext, apiId, null, Set.of());
        var graviteeDefinition = GraviteeDefinitionAdapter.INSTANCE.map(exportEntity);
        if (exportEntity.getApiEntity() instanceof ApiEntity v4) {
            graviteeDefinition.getApi().setType(v4.getType());
        }
        if (exportEntity.getPlans() != null) {
            for (var source : exportEntity.getPlans()) {
                if (source instanceof BasePlanEntity v4Plan) {
                    graviteeDefinition
                        .getPlans()
                        .stream()
                        .filter(p -> p.getId().equals(v4Plan.getId()))
                        .findFirst()
                        .ifPresent(target -> target.setSecurity(v4Plan.getSecurity()));
                }
            }
        }
        return graviteeDefinition;
    }
}
