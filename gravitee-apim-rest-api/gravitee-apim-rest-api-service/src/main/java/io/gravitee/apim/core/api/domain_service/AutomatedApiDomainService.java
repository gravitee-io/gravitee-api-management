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
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.ApiAuditLogEntity;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.event.ApiAuditEvent;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.model.context.OriginContext;
import java.util.Collections;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DomainService
public class AutomatedApiDomainService {

    private final ApiCrudService apiCrudService;
    private final AuditDomainService auditDomainService;

    public AutomatedApiDomainService(ApiCrudService apiCrudService, AuditDomainService auditDomainService) {
        this.apiCrudService = apiCrudService;
        this.auditDomainService = auditDomainService;
    }

    public void detach(String apiId, AuditInfo auditInfo) {
        var api = apiCrudService.findById(apiId).orElseThrow(() -> new ApiNotFoundException(apiId));

        if (!canDetach(api)) {
            throw new ValidationDomainException("The API must be managed by an automation tool in order to be detached.");
        }

        var oldApi = api.toBuilder().build();

        api.setOriginContext(new OriginContext.Management());

        var updatedApi = apiCrudService.update(api);

        createAuditLog(oldApi, updatedApi, auditInfo);
    }

    private static boolean canDetach(Api api) {
        return api.getOriginContext() instanceof OriginContext.Kubernetes;
    }

    private void createAuditLog(Api oldApi, Api updatedApi, AuditInfo auditInfo) {
        auditDomainService.createApiAuditLog(
            ApiAuditLogEntity.builder()
                .organizationId(auditInfo.organizationId())
                .environmentId(auditInfo.environmentId())
                .apiId(updatedApi.getId())
                .event(ApiAuditEvent.AUTOMATION_DETACHED)
                .actor(auditInfo.actor())
                .oldValue(oldApi)
                .newValue(updatedApi)
                .createdAt(TimeProvider.now())
                .properties(Collections.emptyMap())
                .build()
        );
    }
}
