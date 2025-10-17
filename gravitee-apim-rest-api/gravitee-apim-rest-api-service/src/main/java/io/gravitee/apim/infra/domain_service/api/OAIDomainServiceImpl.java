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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.api.domain_service.OAIDomainService;
import io.gravitee.apim.core.api.exception.InvalidImportWithOASValidationPolicyException;
import io.gravitee.apim.core.api.model.import_definition.ImportDefinition;
import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.core.group.model.Group;
import io.gravitee.apim.core.group.query_service.GroupQueryService;
import io.gravitee.apim.core.plugin.crud_service.PolicyPluginCrudService;
import io.gravitee.apim.core.plugin.domain_service.EndpointConnectorPluginDomainService;
import io.gravitee.apim.core.plugin.model.PolicyPlugin;
import io.gravitee.apim.core.tag.model.Tag;
import io.gravitee.apim.core.tag.query_service.TagQueryService;
import io.gravitee.apim.infra.converter.oai.OAIToImportDefinitionConverter;
import io.gravitee.definition.model.flow.Operator;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.selector.HttpSelector;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.definition.model.v4.resource.Resource;
import io.gravitee.rest.api.model.ImportSwaggerDescriptorEntity;
import io.gravitee.rest.api.service.exceptions.SwaggerDescriptorException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.swagger.parser.OAIParser;
import io.gravitee.rest.api.service.impl.swagger.policy.PolicyOperationVisitorManager;
import io.gravitee.rest.api.service.impl.swagger.visitor.v3.OAIOperationVisitor;
import io.gravitee.rest.api.service.swagger.OAIDescriptor;
import io.swagger.v3.parser.core.models.ParseOptions;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class OAIDomainServiceImpl implements OAIDomainService {

    protected static final String DEFAULT_IMPORT_PAGE_NAME = "Swagger";

    private final PolicyOperationVisitorManager policyOperationVisitorManager;
    private final GroupQueryService groupQueryService;
    private final TagQueryService tagsQueryService;
    private final EndpointConnectorPluginDomainService endpointConnectorPluginService;
    private final PolicyPluginCrudService policyPluginCrudService;

    @Override
    public ImportDefinition convert(
        String organizationId,
        String environmentId,
        ImportSwaggerDescriptorEntity importSwaggerDescriptor,
        boolean withDocumentation,
        boolean withOASValidationPolicy
    ) {
        var payload = importSwaggerDescriptor.getPayload();
        var descriptor = toOAIDescriptor(payload);
        var visitors = getVisitors(importSwaggerDescriptor);

        var importDefinition = OAIToImportDefinitionConverter.INSTANCE.toImportDefinition(descriptor.getSpecification(), visitors);

        if (importDefinition != null) {
            var importWithEndpointGroupsSharedConfiguration = addEndpointGroupSharedConfiguration(importDefinition);
            var importWithGroups = replaceGroupNamesWithIds(environmentId, importWithEndpointGroupsSharedConfiguration);
            var importWithTags = replaceTagsNamesWithIds(organizationId, importWithGroups);
            var importWithDocumentation = addOAIDocumentation(withDocumentation, payload, importWithTags);
            return addOASValidationPolicy(withOASValidationPolicy, payload, importWithDocumentation);
        }

        return null;
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

    private ImportDefinition addEndpointGroupSharedConfiguration(ImportDefinition importDefinition) {
        var sharedConfiguration = endpointConnectorPluginService.getDefaultSharedConfiguration("http-proxy");
        var endpointGroups = importDefinition.getApiExport().getEndpointGroups();
        if (endpointGroups == null || endpointGroups.isEmpty()) {
            return importDefinition;
        }

        return importDefinition
            .toBuilder()
            .apiExport(
                importDefinition
                    .getApiExport()
                    .toBuilder()
                    .endpointGroups(
                        endpointGroups
                            .stream()
                            .peek(endpointGroup -> endpointGroup.setSharedConfiguration(sharedConfiguration))
                            .toList()
                    )
                    .build()
            )
            .build();
    }

    private ImportDefinition replaceGroupNamesWithIds(String environmentId, ImportDefinition importDefinition) {
        var groups = importDefinition.getApiExport().getGroups();
        if (groups == null || groups.isEmpty()) {
            return importDefinition;
        }

        return importDefinition
            .toBuilder()
            .apiExport(
                importDefinition
                    .getApiExport()
                    .toBuilder()
                    .groups(
                        groups
                            .stream()
                            .flatMap(group -> groupQueryService.findByNames(environmentId, Set.of(group)).stream())
                            .map(Group::getId)
                            .collect(Collectors.toSet())
                    )
                    .build()
            )
            .build();
    }

    private ImportDefinition replaceTagsNamesWithIds(String organizationId, ImportDefinition importDefinition) {
        if (
            importDefinition.getApiExport() == null ||
            importDefinition.getApiExport().getTags() == null ||
            importDefinition.getApiExport().getTags().isEmpty()
        ) {
            return importDefinition;
        }

        return importDefinition
            .toBuilder()
            .apiExport(
                importDefinition
                    .getApiExport()
                    .toBuilder()
                    .tags(
                        importDefinition
                            .getApiExport()
                            .getTags()
                            .stream()
                            .flatMap(group -> tagsQueryService.findByName(organizationId, group).stream())
                            .map(Tag::getId)
                            .collect(Collectors.toSet())
                    )
                    .build()
            )
            .build();
    }

    private ImportDefinition addOAIDocumentation(boolean withDocumentation, String payload, ImportDefinition importWithTags) {
        if (!withDocumentation) {
            return importWithTags;
        }
        var page = io.gravitee.apim.core.documentation.model.Page.builder()
            .name(DEFAULT_IMPORT_PAGE_NAME)
            .type(io.gravitee.apim.core.documentation.model.Page.Type.SWAGGER)
            .homepage(false)
            .content(payload)
            .referenceType(io.gravitee.apim.core.documentation.model.Page.ReferenceType.API)
            .published(true)
            .visibility(Page.Visibility.PUBLIC)
            .build();

        return importWithTags.toBuilder().pages(List.of(page)).build();
    }

    private ImportDefinition addOASValidationPolicy(boolean withOASValidationPolicy, String payload, ImportDefinition importDefinition) {
        if (!withOASValidationPolicy) {
            return importDefinition;
        }

        PolicyPlugin oasValidationPolicy = policyPluginCrudService
            .get("oas-validation")
            .orElseThrow(() -> new InvalidImportWithOASValidationPolicyException("Policy not found"));

        try {
            // Add Content provider inline resource to API resources
            Resource resource = Resource.builder()
                .name("OpenAPI Specification")
                .type("content-provider-inline-resource")
                .configuration(new ObjectMapper().writeValueAsString(new LinkedHashMap<>(Map.of("content", payload))))
                .build();
            importDefinition.getApiExport().setResources(List.of(resource));

            // Add Flow with OAS validation policy to API flows
            var step = Step.builder()
                .policy(oasValidationPolicy.getId())
                .name(oasValidationPolicy.getName())
                .configuration(new ObjectMapper().writeValueAsString(new LinkedHashMap<>(Map.of("resourceName", "OpenAPI Specification"))))
                .build();

            var httpSelector = HttpSelector.builder().path("/").pathOperator(Operator.STARTS_WITH).build();

            var flow = Flow.builder()
                .name("OpenAPI Specification Validation")
                .selectors(List.of(httpSelector))
                .request(List.of(step))
                .response(List.of(step))
                .build();

            List<Flow> apiExportFlows = (List<Flow>) importDefinition.getApiExport().getFlows();
            apiExportFlows.addFirst(flow);

            importDefinition.getApiExport().setFlows(apiExportFlows);

            return importDefinition;
        } catch (JsonProcessingException e) {
            throw new TechnicalManagementException("Error while serializing OpenAPI Specification", e);
        }
    }
}
