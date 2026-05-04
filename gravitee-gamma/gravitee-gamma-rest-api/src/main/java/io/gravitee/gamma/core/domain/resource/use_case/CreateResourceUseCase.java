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
package io.gravitee.gamma.core.domain.resource.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.audit.model.EnvironmentAuditLogEntity;
import io.gravitee.apim.core.validation.Validator;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.gamma.core.domain.resource.crud_service.ResourceCrudService;
import io.gravitee.gamma.core.domain.resource.domain_service.ValidateCreateResourceCommandDomainService;
import io.gravitee.gamma.core.domain.resource.exception.ResourceValidationException;
import io.gravitee.gamma.core.domain.resource.model.CreateResourceCommand;
import io.gravitee.gamma.core.domain.resource.model.Resource;
import io.gravitee.gamma.core.domain.resource.model.ResourceAuditEvent;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class CreateResourceUseCase {

    private final ResourceCrudService resourceCrudService;
    private final ValidateCreateResourceCommandDomainService validateCreateResourceCommandDomainService;
    private final AuditDomainService auditDomainService;

    public Output execute(Input input) {
        var result = validateAndSanitize(input);
        var sanitized = result.value();

        if (sanitized.isPresent()) {
            Resource resource = Resource.from(sanitized.get().command(), sanitized.get().auditInfo());
            Resource created = resourceCrudService.create(resource);

            createAuditLog(created, input.auditInfo());

            return new Output(created);
        } else if (result.severe().isPresent()) {
            var errors = result.severe().get();
            throw new ResourceValidationException(
                String.format("Validation errors: %s", String.join(",", errors.stream().map(Validator.Error::getMessage).toList()))
            );
        }

        throw new IllegalStateException("Unexpected error");
    }

    private Validator.Result<CreateResourceUseCase.Input> validateAndSanitize(CreateResourceUseCase.Input input) {
        return validateCreateResourceCommandDomainService
            .validateAndSanitize(new ValidateCreateResourceCommandDomainService.Input(input.command, input.auditInfo))
            .map(sanitized -> new Input(input.auditInfo, sanitized.command()));
    }

    public record Input(AuditInfo auditInfo, CreateResourceCommand command) {}

    public record Output(Resource resource) {}

    private void createAuditLog(Resource resource, AuditInfo auditInfo) {
        auditDomainService.createEnvironmentAuditLog(
            EnvironmentAuditLogEntity.builder()
                .organizationId(auditInfo.organizationId())
                .environmentId(auditInfo.environmentId())
                .actor(auditInfo.actor())
                .event(ResourceAuditEvent.RESOURCE_CREATED)
                .createdAt(TimeProvider.now())
                .newValue(resource)
                .properties(Map.of(AuditProperties.RESOURCE, resource.id()))
                .build()
        );
    }
}
