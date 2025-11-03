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
package io.gravitee.apim.core.api.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.factory.ApiModelFactory;
import io.gravitee.apim.core.api.model.import_definition.ApiExport;
import io.gravitee.apim.core.api.model.import_definition.ImportDefinition;
import io.gravitee.apim.core.api.service_provider.ApiImagesServiceProvider;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.infra.domain_service.api.ApiImagesServiceProviderImpl;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@DomainService
public class ImportDefinitionUpdateDomainService {

    private final UpdateApiDomainService updateApiDomainService;
    private final ApiImagesServiceProvider apiImagesServiceProvider;
    private final ApiIdsCalculatorDomainService apiIdsCalculatorDomainService;

    public ImportDefinitionUpdateDomainService(
        UpdateApiDomainService updateApiDomainService,
        ApiImagesServiceProvider apiImagesServiceProvider,
        ApiIdsCalculatorDomainService apiIdsCalculatorDomainService
    ) {
        this.updateApiDomainService = updateApiDomainService;
        this.apiImagesServiceProvider = apiImagesServiceProvider;
        this.apiIdsCalculatorDomainService = apiIdsCalculatorDomainService;
    }

    public Api update(ImportDefinition importDefinition, Api existingPromotedApi, AuditInfo auditInfo) {
        var apiWithIds = apiIdsCalculatorDomainService.recalculateApiDefinitionIds(auditInfo.environmentId(), importDefinition);
        return switch (existingPromotedApi.getType()) {
            case PROXY, MESSAGE -> updateHttpV4Api(apiWithIds.getApiExport(), auditInfo);
            case NATIVE -> throw new IllegalStateException("coming in the next commit");
            default -> throw new IllegalStateException("Unsupported API type: " + existingPromotedApi.getType());
        };
    }

    private Api updateHttpV4Api(ApiExport export, AuditInfo auditInfo) {
        var updatedApi = updateApiDomainService.updateV4(ApiModelFactory.fromApiExport(export, auditInfo.environmentId()), auditInfo);
        apiImagesServiceProvider.updateApiPicture(export.getId(), export.getPicture(), auditInfo);
        apiImagesServiceProvider.updateApiBackground(export.getId(), export.getBackground(), auditInfo);
        return updatedApi;
    }
}
