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

import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.documentation.crud_service.PageCrudService;
import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.core.documentation.query_service.PageQueryService;
import io.gravitee.apim.core.exception.DomainException;
import io.gravitee.apim.core.sanitizer.HtmlSanitizer;
import io.gravitee.apim.core.sanitizer.SanitizeResult;
import io.gravitee.rest.api.service.exceptions.PageContentUnsafeException;
import java.util.List;
import java.util.Objects;

public class ApiDocumentationDomainService {

    private static final String ROOT = "ROOT";
    private final PageQueryService pageQueryService;
    private final HtmlSanitizer htmlSanitizer;

    public ApiDocumentationDomainService(PageQueryService pageQueryService, HtmlSanitizer htmlSanitizer) {
        this.pageQueryService = pageQueryService;
        this.htmlSanitizer = htmlSanitizer;
    }

    public List<Page> getApiPages(String apiId, String parentId) {
        if (Objects.nonNull(parentId) && !parentId.isEmpty()) {
            var parentIdParam = ROOT.equals(parentId) ? null : parentId;
            return pageQueryService.searchByApiIdAndParentId(apiId, parentIdParam);
        } else {
            return pageQueryService.searchByApiId(apiId);
        }
    }

    public void validatePageAssociatedToApi(Page page, String apiId) {
        if (!Objects.equals(page.getReferenceId(), apiId) || !Page.ReferenceType.API.equals(page.getReferenceType())) {
            throw new DomainException("Page is not associated to Api: " + apiId);
        }
    }

    public void validateContentIsSafe(String content) {
        final SanitizeResult sanitizeInfos = htmlSanitizer.isSafe(content);
        if (!sanitizeInfos.isSafe()) {
            throw new PageContentUnsafeException(sanitizeInfos.getRejectedMessage());
        }
    }

    public void validateNameIsUnique(String apiId, String parentId, String name, Page.Type type) {
        var foundPage = this.pageQueryService.findByApiIdAndParentIdAndNameAndType(apiId, parentId, name, type);
        if (foundPage.isPresent()) {
            throw new DomainException("Name already exists with the same parent and type: " + name);
        }
    }
}
