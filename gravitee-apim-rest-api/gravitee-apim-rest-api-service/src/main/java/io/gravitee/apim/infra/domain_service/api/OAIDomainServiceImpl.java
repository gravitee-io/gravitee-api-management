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

import io.gravitee.apim.core.api.domain_service.OAIDomainService;
import io.gravitee.apim.core.api.model.import_definition.ImportDefinition;
import io.gravitee.apim.infra.converter.oai.OAIToImportDefinitionConverter;
import io.gravitee.rest.api.model.ImportSwaggerDescriptorEntity;
import io.gravitee.rest.api.service.exceptions.SwaggerDescriptorException;
import io.gravitee.rest.api.service.impl.swagger.parser.OAIParser;
import io.gravitee.rest.api.service.impl.swagger.policy.PolicyOperationVisitorManager;
import io.gravitee.rest.api.service.impl.swagger.visitor.v3.OAIOperationVisitor;
import io.gravitee.rest.api.service.swagger.OAIDescriptor;
import io.swagger.v3.parser.core.models.ParseOptions;
import java.util.Collection;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class OAIDomainServiceImpl implements OAIDomainService {

    private final PolicyOperationVisitorManager policyOperationVisitorManager;

    @Override
    public ImportDefinition convert(String organizationId, String environmentId, ImportSwaggerDescriptorEntity importSwaggerDescriptor) {
        var descriptor = toOAIDescriptor(importSwaggerDescriptor.getPayload());
        var visitors = getVisitors(importSwaggerDescriptor);
        return OAIToImportDefinitionConverter.INSTANCE.toImportDefinition(descriptor.getSpecification(), visitors);
    }

    private OAIDescriptor toOAIDescriptor(String payload) {
        var options = new ParseOptions();
        options.setResolveFully(true);

        if (payload == null || payload.isEmpty()) {
            throw new SwaggerDescriptorException("Payload cannot be null");
        }

        var descriptor = new OAIParser().parse(payload, options);
        if (descriptor == null || descriptor.getSpecification() == null) {
            throw new SwaggerDescriptorException("The API specification is not valid");
        }

        if (descriptor.getSpecification().getInfo() == null) {
            throw new SwaggerDescriptorException("The API specification must contain an info section");
        }

        return descriptor;
    }

    private Collection<? extends OAIOperationVisitor> getVisitors(ImportSwaggerDescriptorEntity importSwaggerDescriptor) {
        return policyOperationVisitorManager
            .getPolicyVisitors()
            .stream()
            .filter(
                operationVisitor ->
                    importSwaggerDescriptor.getWithPolicies() != null &&
                    importSwaggerDescriptor.getWithPolicies().contains(operationVisitor.getId())
            )
            .map(operationVisitor -> policyOperationVisitorManager.getOAIOperationVisitor(operationVisitor.getId()))
            .collect(Collectors.toList());
    }
}
