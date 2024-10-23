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

import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.domain_service.UpdateApiDomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.crd.ApiCRDSpec;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.infra.adapter.ApiAdapter;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.v4.ApiService;
import org.springframework.stereotype.Service;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Service
public class UpdateApiDomainServiceImpl implements UpdateApiDomainService {

    private final ApiService delegate;
    private final ApiCrudService apiCrudService;

    public UpdateApiDomainServiceImpl(ApiService delegate, ApiCrudService apiCrudService) {
        this.delegate = delegate;
        this.apiCrudService = apiCrudService;
    }

    @Override
    public Api update(String apiId, ApiCRDSpec crd, AuditInfo auditInfo) {
        var executionContext = new ExecutionContext(auditInfo.organizationId(), auditInfo.environmentId());
        var apiEntity = delegate.update(executionContext, apiId, ApiAdapter.INSTANCE.toUpdateApiEntity(crd), auditInfo.actor().userId());

        return apiCrudService.get(apiEntity.getId());
    }

    @Override
    public Api updateV4(Api api, AuditInfo auditInfo) {
        var executionContext = new ExecutionContext(auditInfo.organizationId(), auditInfo.environmentId());

        var updateApiEntity = ApiAdapter.INSTANCE.toUpdateApiEntity(api, api.getApiDefinitionHttpV4());

        delegate.update(executionContext, api.getId(), updateApiEntity, false, auditInfo.actor().userId());

        return apiCrudService.get(api.getId());
    }
}
