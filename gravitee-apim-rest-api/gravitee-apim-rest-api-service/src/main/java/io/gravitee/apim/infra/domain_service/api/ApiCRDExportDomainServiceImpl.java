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

import io.gravitee.apim.core.api.domain_service.ApiCRDExportDomainService;
import io.gravitee.apim.core.api.model.crd.ApiCRDSpec;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.infra.adapter.ApiAdapter;
import io.gravitee.apim.infra.adapter.ApiCRDAdapter;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.v4.ApiImportExportService;
import io.gravitee.rest.api.service.v4.ApiService;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Service
@RequiredArgsConstructor
public class ApiCRDExportDomainServiceImpl implements ApiCRDExportDomainService {

    private final ApiImportExportService exportService;

    private final ApiService apiService;

    @Override
    public ApiCRDSpec export(String apiId, AuditInfo auditInfo) {
        var executionContext = new ExecutionContext(auditInfo.organizationId(), auditInfo.environmentId());
        var exportEntity = exportService.exportApi(executionContext, apiId, null, Set.of());
        var spec = ApiCRDAdapter.INSTANCE.toCRDSpec(exportEntity, exportEntity.getApiEntity());
        return generateAndSaveCrossId(spec, auditInfo);
    }

    private ApiCRDSpec generateAndSaveCrossId(ApiCRDSpec spec, AuditInfo auditInfo) {
        if (StringUtils.isEmpty(spec.getCrossId())) {
            var executionContext = new ExecutionContext(auditInfo.organizationId(), auditInfo.environmentId());
            spec.setCrossId(UuidString.generateRandom());
            var updateApiEntity = ApiAdapter.INSTANCE.toUpdateApiEntity(spec);
            apiService.update(executionContext, updateApiEntity.getId(), updateApiEntity, auditInfo.actor().userId());
        }
        return spec;
    }
}
