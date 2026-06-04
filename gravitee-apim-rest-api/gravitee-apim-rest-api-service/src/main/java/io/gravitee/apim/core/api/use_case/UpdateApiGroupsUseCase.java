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

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.ApiAuditLogEntity;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.audit.model.event.ApiAuditEvent;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.group.domain_service.ValidateGroupsDomainService;
import io.gravitee.apim.core.group.model.Group;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.model.context.OriginContext;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@UseCase
public class UpdateApiGroupsUseCase {

    private final ApiCrudService apiCrudService;
    private final AuditDomainService auditService;
    private final ValidateGroupsDomainService validateGroupsDomainService;

    public record Input(String apiId, Set<String> groups, AuditInfo auditInfo) {}

    public record Output(Set<String> groups) {}

    public Output execute(Input input) {
        Api api = apiCrudService.get(input.apiId());

        if (!api.getEnvironmentId().equals(input.auditInfo().environmentId())) {
            throw new ApiNotFoundException(input.apiId());
        }

        if (api.getOriginContext() instanceof OriginContext.Kubernetes) {
            throw new ValidationDomainException("Cannot update groups of a Kubernetes-managed API");
        }

        var validationInput = new ValidateGroupsDomainService.Input(
            input.auditInfo().environmentId(),
            input.groups(),
            api.getDefinitionVersion().getLabel(),
            api.getOriginContext().name()
        );
        var validationResult = validateGroupsDomainService.validateAndSanitize(validationInput);
        Set<String> sanitizedGroups = validationResult.value().map(ValidateGroupsDomainService.Input::groups).orElse(input.groups());

        Api oldApi = api.toBuilder().build();

        api.setGroups(sanitizedGroups);
        Api updated = apiCrudService.update(api);

        createAuditLog(oldApi, updated, input.auditInfo());

        return new Output(updated.getGroups());
    }

    private void createAuditLog(Api oldApi, Api updatedApi, AuditInfo auditInfo) {
        auditService.createApiAuditLog(
            ApiAuditLogEntity.builder()
                .apiId(updatedApi.getId())
                .organizationId(auditInfo.organizationId())
                .environmentId(auditInfo.environmentId())
                .event(ApiAuditEvent.API_UPDATED)
                .actor(auditInfo.actor())
                .oldValue(oldApi)
                .newValue(updatedApi)
                .createdAt(TimeProvider.now())
                .properties(Map.of(AuditProperties.API, updatedApi.getId()))
                .build()
        );
    }
}
