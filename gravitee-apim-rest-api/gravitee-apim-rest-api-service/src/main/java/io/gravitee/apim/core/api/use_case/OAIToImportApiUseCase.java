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
import io.gravitee.apim.core.api.domain_service.ImportDefinitionCreateDomainService;
import io.gravitee.apim.core.api.domain_service.OAIDomainService;
import io.gravitee.apim.core.api.model.ApiWithFlows;
import io.gravitee.apim.core.api.model.import_definition.ImportDefinition;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.core.group.model.Group;
import io.gravitee.apim.core.group.query_service.GroupQueryService;
import io.gravitee.apim.core.plugin.domain_service.EndpointConnectorPluginDomainService;
import io.gravitee.apim.core.tag.model.Tag;
import io.gravitee.apim.core.tag.query_service.TagQueryService;
import io.gravitee.rest.api.model.ImportSwaggerDescriptorEntity;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@UseCase
public class OAIToImportApiUseCase {

    protected static final String DEFAULT_IMPORT_PAGE_NAME = "Swagger";

    public record Input(ImportSwaggerDescriptorEntity importSwaggerDescriptor, AuditInfo auditInfo) {}

    public record Output(ApiWithFlows apiWithFlows) {}

    private final OAIDomainService oaiDomainService;
    private final GroupQueryService groupQueryService;
    private final TagQueryService tagsQueryService;
    private final EndpointConnectorPluginDomainService endpointConnectorPluginService;
    private final ImportDefinitionCreateDomainService importDefinitionCreateDomainService;

    public OAIToImportApiUseCase(
        OAIDomainService oaiDomainService,
        GroupQueryService groupQueryService,
        TagQueryService tagsQueryService,
        EndpointConnectorPluginDomainService endpointConnectorPluginService,
        ImportDefinitionCreateDomainService importDefinitionCreateDomainService
    ) {
        this.oaiDomainService = oaiDomainService;
        this.groupQueryService = groupQueryService;
        this.tagsQueryService = tagsQueryService;
        this.endpointConnectorPluginService = endpointConnectorPluginService;
        this.importDefinitionCreateDomainService = importDefinitionCreateDomainService;
    }

    public Output execute(Input input) {
        var organizationId = input.auditInfo.organizationId();
        var environmentId = input.auditInfo.environmentId();
        var importDefinition = oaiDomainService.convert(organizationId, environmentId, input.importSwaggerDescriptor);
        var withDocumentation = input.importSwaggerDescriptor.isWithDocumentation();

        if (importDefinition != null) {
            var importWithEndpointGroupsSharedConfiguration = addEndpointGroupSharedConfiguration(importDefinition);
            var importWithGroups = replaceGroupNamesWithIds(environmentId, importWithEndpointGroupsSharedConfiguration);
            var importWithTags = replaceTagsNamesWithIds(organizationId, importWithGroups);
            var importWithDocumentation = addOAIDocumentation(
                withDocumentation,
                input.importSwaggerDescriptor.getPayload(),
                importWithTags
            );
            final ApiWithFlows apiWithFlows = importDefinitionCreateDomainService.create(input.auditInfo, importWithDocumentation);
            return new Output(apiWithFlows);
        }

        return null;
    }

    private ImportDefinition addEndpointGroupSharedConfiguration(ImportDefinition importDefinition) {
        var sharedConfiguration = endpointConnectorPluginService.getSharedConfigurationSchema("http-proxy");
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
                            .map(endpointGroup -> endpointGroup.toBuilder().sharedConfiguration(sharedConfiguration).build())
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
        var page = io.gravitee.apim.core.documentation.model.Page
            .builder()
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
}
