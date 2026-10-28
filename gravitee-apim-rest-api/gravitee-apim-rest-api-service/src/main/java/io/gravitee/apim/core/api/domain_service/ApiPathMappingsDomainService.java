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
import io.gravitee.apim.core.api.model.HttpListenerPathMappings;
import io.gravitee.apim.core.api.model.PathMappingTemplates;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.ApiAuditLogEntity;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.audit.model.event.ApiAuditEvent;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.rest.api.model.context.OriginContext;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;

@DomainService
@AllArgsConstructor
public class ApiPathMappingsDomainService {

    private final ApiCrudService apiCrudService;
    private final AuditDomainService auditService;

    public Api loadEditableHttpV4Api(String apiId, AuditInfo auditInfo) {
        Api api = apiCrudService.get(apiId);
        if (!api.getEnvironmentId().equals(auditInfo.environmentId())) {
            throw new ApiNotFoundException(apiId);
        }
        var definition = api.getApiDefinitionHttpV4();
        if (definition == null) {
            throw new ValidationDomainException("API must be a V4 HTTP API");
        }
        if (api.getOriginContext() instanceof OriginContext.Kubernetes) {
            throw new ValidationDomainException("Cannot update path mappings of a Kubernetes-managed API");
        }
        boolean hasHttpListener =
            definition.getListeners() != null && definition.getListeners().stream().anyMatch(HttpListener.class::isInstance);
        if (!hasHttpListener) {
            throw new ValidationDomainException("API must have an HTTP listener");
        }
        return api;
    }

    public Set<String> currentMappings(Api api) {
        return HttpListenerPathMappings.firstHttpListenerMappings(api.getApiDefinitionHttpV4().getListeners()).orElseGet(
            LinkedHashSet::new
        );
    }

    public Set<String> replace(Api api, Collection<String> mappings, AuditInfo auditInfo) {
        PathMappingTemplates.validate(mappings);
        var oldMappings = currentMappings(api);
        var newMappings = new LinkedHashSet<>(mappings);
        var definition = api.getApiDefinitionHttpV4();
        var updatedListeners = HttpListenerPathMappings.withFirstHttpListenerMappings(definition.getListeners(), newMappings);
        var updatedDefinition = definition.toBuilder().listeners(updatedListeners).build();
        apiCrudService.update(api.toBuilder().apiDefinitionValue(updatedDefinition).build());
        createAuditLog(oldMappings, newMappings, api.getId(), auditInfo);
        return newMappings;
    }

    private void createAuditLog(Set<String> before, Set<String> after, String apiId, AuditInfo auditInfo) {
        auditService.createApiAuditLog(
            ApiAuditLogEntity.builder()
                .apiId(apiId)
                .organizationId(auditInfo.organizationId())
                .environmentId(auditInfo.environmentId())
                .event(ApiAuditEvent.API_UPDATED)
                .actor(auditInfo.actor())
                .oldValue(before)
                .newValue(after)
                .createdAt(TimeProvider.now())
                .properties(Map.of(AuditProperties.API, apiId))
                .build()
        );
    }
}
