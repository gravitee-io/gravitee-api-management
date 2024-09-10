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
package io.gravitee.apim.core.documentation.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.query_service.ApiMetadataQueryService;
import io.gravitee.apim.core.documentation.crud_service.PageCrudService;
import io.gravitee.apim.core.documentation.exception.InvalidPageNameException;
import io.gravitee.apim.core.documentation.exception.InvalidPageParentException;
import io.gravitee.apim.core.documentation.model.AccessControl;
import io.gravitee.apim.core.documentation.model.ApiFreemarkerTemplate;
import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.core.group.model.Group;
import io.gravitee.apim.core.group.query_service.GroupQueryService;
import io.gravitee.apim.core.membership.domain_service.ApiPrimaryOwnerDomainService;
import io.gravitee.apim.core.membership.model.Role;
import io.gravitee.apim.core.membership.query_service.RoleQueryService;
import io.gravitee.apim.core.sanitizer.HtmlSanitizer;
import io.gravitee.apim.core.sanitizer.SanitizeResult;
import io.gravitee.rest.api.service.exceptions.PageContentUnsafeException;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@DomainService
public class DocumentationValidationDomainService {

    private final HtmlSanitizer htmlSanitizer;
    private final TemplateResolverDomainService templateResolverDomainService;
    private final ApiCrudService apiCrudService;
    private final OpenApiDomainService openApiDomainService;
    private final ApiMetadataQueryService apiMetadataQueryService;
    private final ApiPrimaryOwnerDomainService apiPrimaryOwnerDomainService;
    private final ApiDocumentationDomainService apiDocumentationDomainService;
    private final PageCrudService pageCrudService;
    private final PageSourceDomainService pageSourceDomainService;
    private final GroupQueryService groupQueryService;
    private final RoleQueryService roleQueryService;

    public String sanitizeDocumentationName(String name) {
        if (null == name || name.trim().isEmpty()) {
            throw new InvalidPageNameException();
        }
        return name.trim();
    }

    public Set<AccessControl> sanitizeAccessControls(Set<AccessControl> accessControls) {
        if (Objects.isNull(accessControls)) {
            return null;
        }

        return this.retainExistingGroupAndRoleAccessControls(accessControls);
    }

    public void validateContent(String content, String apiId, String organizationId) {
        this.validateContentIsSafe(content);
        if (apiId != null) {
            this.validateTemplate(content, apiId, organizationId);
        }
    }

    public void validateContentIsSafe(String content) {
        final SanitizeResult sanitizeInfos = htmlSanitizer.isSafe(content);
        if (!sanitizeInfos.isSafe()) {
            throw new PageContentUnsafeException(sanitizeInfos.getRejectedMessage());
        }
    }

    public void validateTemplate(String pageContent, String apiId, String organizationId) {
        try {
            Api exitingApi = this.apiCrudService.get(apiId);
            var metadata =
                this.apiMetadataQueryService.findApiMetadata(apiId)
                    .entrySet()
                    .stream()
                    .collect(
                        Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> entry.getValue().getValue() != null ? entry.getValue().getValue() : entry.getValue().getDefaultValue()
                        )
                    );

            var api = new ApiFreemarkerTemplate(
                exitingApi,
                metadata,
                apiPrimaryOwnerDomainService.getApiPrimaryOwner(organizationId, apiId)
            );

            this.templateResolverDomainService.resolveTemplate(pageContent, Map.of("api", api));
        } catch (ApiNotFoundException e) {
            log.debug("api doesn't exist [{}]. Template will not be validated", apiId);
        }
    }

    public void parseOpenApiContent(String content) {
        if (content != null) {
            openApiDomainService.parseOpenApiContent(content);
        }
    }

    public Page validateAndSanitizeForCreation(Page page, String organizationId) {
        return this.validateAndSanitizeForCreation(page, organizationId, true);
    }

    public Page validateAndSanitizeForCreation(Page page, String organizationId, boolean shouldValidateParentId) {
        var sanitizedPage = page
            .toBuilder()
            .name(this.sanitizeDocumentationName(page.getName()))
            .accessControls(this.sanitizeAccessControls(page.getAccessControls()))
            .build();

        pageSourceDomainService.setContentFromSource(sanitizedPage);

        validatePageContent(organizationId, sanitizedPage);

        if (shouldValidateParentId) {
            this.validateParentId(sanitizedPage);
        }

        this.validateNameIsUnique(sanitizedPage);

        return sanitizedPage;
    }

    public Page validateAndSanitizeForUpdate(Page page, String organizationId, boolean shouldValidateParentId) {
        var sanitizedPage = page.toBuilder().accessControls(this.sanitizeAccessControls(page.getAccessControls())).build();

        pageSourceDomainService.setContentFromSource(sanitizedPage);

        validatePageContent(organizationId, sanitizedPage);

        if (shouldValidateParentId) {
            this.validateParentId(sanitizedPage);
        }

        return sanitizedPage;
    }

    private void validatePageContent(String organizationId, Page sanitizedPage) {
        if (sanitizedPage.isMarkdown()) {
            this.validateContent(sanitizedPage.getContent(), sanitizedPage.getReferenceId(), organizationId);
        } else if (sanitizedPage.isSwagger()) {
            this.parseOpenApiContent(sanitizedPage.getContent());
        }
    }

    private void validateParentId(Page page) {
        var parentId = page.getParentId();

        if (Objects.nonNull(parentId) && !parentId.isEmpty()) {
            var foundParent = pageCrudService.findById(parentId);

            if (foundParent.isPresent()) {
                if (!foundParent.get().isFolder()) {
                    throw new InvalidPageParentException(parentId);
                }
                return;
            }
        }
        page.setParentId(null);
    }

    private void validateNameIsUnique(Page page) {
        this.apiDocumentationDomainService.validateNameIsUnique(page.getReferenceId(), page.getParentId(), page.getName(), page.getType());
    }

    private Set<AccessControl> retainExistingGroupAndRoleAccessControls(Set<AccessControl> accessControls) {
        var accessControlsByGroupAndRole = accessControls
            .stream()
            .filter(accessControl ->
                Objects.equals("GROUP", accessControl.getReferenceType()) || Objects.equals("ROLE", accessControl.getReferenceType())
            )
            .collect(Collectors.groupingBy(AccessControl::getReferenceType));

        accessControlsByGroupAndRole.computeIfPresent(
            "GROUP",
            (key, acs) -> {
                var foundGroupIds =
                    this.groupQueryService.findByIds(acs.stream().map(AccessControl::getReferenceId).collect(Collectors.toSet()))
                        .stream()
                        .map(Group::getId)
                        .toList();

                return acs.stream().filter(accessControl -> foundGroupIds.contains(accessControl.getReferenceId())).toList();
            }
        );

        accessControlsByGroupAndRole.computeIfPresent(
            "ROLE",
            (key, acs) -> {
                var foundRoleIds =
                    this.roleQueryService.findByIds(acs.stream().map(AccessControl::getReferenceId).collect(Collectors.toSet()))
                        .stream()
                        .map(Role::getId)
                        .toList();
                return acs.stream().filter(accessControl -> foundRoleIds.contains(accessControl.getReferenceId())).toList();
            }
        );

        return accessControlsByGroupAndRole.values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
    }
}
