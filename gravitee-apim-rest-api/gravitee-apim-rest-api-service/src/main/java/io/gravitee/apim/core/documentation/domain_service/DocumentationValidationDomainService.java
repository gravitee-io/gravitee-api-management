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
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.query_service.ApiMetadataQueryService;
import io.gravitee.apim.core.documentation.crud_service.PageCrudService;
import io.gravitee.apim.core.documentation.exception.InvalidPageNameException;
import io.gravitee.apim.core.documentation.exception.InvalidPageParentException;
import io.gravitee.apim.core.documentation.model.ApiFreemarkerTemplate;
import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.core.membership.domain_service.ApiPrimaryOwnerDomainService;
import io.gravitee.apim.core.sanitizer.HtmlSanitizer;
import io.gravitee.apim.core.sanitizer.SanitizeResult;
import io.gravitee.rest.api.service.exceptions.ApiNotFoundException;
import io.gravitee.rest.api.service.exceptions.PageContentUnsafeException;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@DomainService
@Slf4j
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

    public String sanitizeDocumentationName(String name) {
        if (null == name || name.trim().isEmpty()) {
            throw new InvalidPageNameException();
        }
        return name.trim();
    }

    public void validateContent(String content, String apiId, String organizationId) {
        this.validateContentIsSafe(content);
        this.validateTemplate(content, apiId, organizationId);
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
                this.apiMetadataQueryService.findApiMetadata(exitingApi.getEnvironmentId(), apiId)
                    .entrySet()
                    .stream()
                    .collect(
                        Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> entry.getValue().getValue() != null ? entry.getValue().getValue() : entry.getValue().getDefaultValue()
                        )
                    );

            var api = new ApiFreemarkerTemplate(
                this.apiCrudService.get(apiId),
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
        var sanitizedPage = page.toBuilder().name(this.sanitizeDocumentationName(page.getName())).build();

        pageSourceDomainService.setContentFromSource(sanitizedPage);

        validatePageContent(organizationId, sanitizedPage);

        if (shouldValidateParentId) {
            this.validateParentId(sanitizedPage);
        }

        this.validateNameIsUnique(sanitizedPage);

        return sanitizedPage;
    }

    public Page validateAndSanitizeForUpdate(Page page, String organizationId, boolean shouldValidateParentId) {
        pageSourceDomainService.setContentFromSource(page);

        validatePageContent(organizationId, page);

        if (shouldValidateParentId) {
            this.validateParentId(page);
        }

        return page;
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
}
