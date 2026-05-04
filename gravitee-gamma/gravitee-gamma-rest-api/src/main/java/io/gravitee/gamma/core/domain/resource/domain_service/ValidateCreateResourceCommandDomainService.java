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
package io.gravitee.gamma.core.domain.resource.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.validation.Validator;
import io.gravitee.gamma.core.domain.resource.crud_service.ResourceCrudService;
import io.gravitee.gamma.core.domain.resource.model.CreateResourceCommand;
import io.gravitee.gamma.core.domain.resource.model.Resource;
import io.gravitee.gamma.core.port.service_provider.gravitee_plugin.ResourcePluginProvider;
import io.gravitee.json.validation.InvalidJsonException;
import io.gravitee.json.validation.JsonSchemaValidator;
import java.util.ArrayList;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
public class ValidateCreateResourceCommandDomainService implements Validator<ValidateCreateResourceCommandDomainService.Input> {

    public record Input(CreateResourceCommand command, AuditInfo auditInfo) implements Validator.Input {}

    private final ResourceCrudService resourceCrudService;
    private final JsonSchemaValidator jsonSchemaValidator;
    private final ResourcePluginProvider resourcePluginProvider;

    @Override
    public Result<Input> validateAndSanitize(Input input) {
        var errors = new ArrayList<Error>();
        var sanitizedCommand = input.command.toBuilder();

        if (isBlank(input.command.id())) {
            errors.add(Error.severe("Resource id is required."));
        }
        if (isBlank(input.command.name())) {
            errors.add(Error.severe("Resource name is required."));
        }
        boolean exists = resourceCrudService.existsByNameAndReference(
            input.command.name(),
            Resource.ReferenceType.ENVIRONMENT,
            input.auditInfo.environmentId()
        );
        if (exists) {
            errors.add(Error.severe("A resource with name [" + input.command.name() + "] already exists."));
        }

        if (isBlank(input.command.type())) {
            errors.add(Error.severe("Resource type is required."));
        }
        if (!isBlank(input.command.configuration())) {
            try {
                sanitizedCommand.configuration(
                    jsonSchemaValidator.validate(resourcePluginProvider.getSchema(input.command.type()), input.command.configuration())
                );
            } catch (InvalidJsonException e) {
                errors.add(Error.severe("Configuration invalid: " + e.getMessage()));
            }
        } else {
            errors.add(Error.severe("Resource configuration is required."));
        }

        if (errors.isEmpty()) {
            return Result.ofValue(new Input(sanitizedCommand.build(), input.auditInfo));
        }
        return Result.ofErrors(errors);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
