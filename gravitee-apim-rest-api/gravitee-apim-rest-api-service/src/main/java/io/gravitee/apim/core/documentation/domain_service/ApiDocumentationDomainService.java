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
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.documentation.exception.ApiPageNotAssociatedException;
import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.core.documentation.query_service.PageQueryService;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.plan.query_service.PlanQueryService;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@DomainService
public class ApiDocumentationDomainService {

    private static final String ROOT = "ROOT";
    private final PageQueryService pageQueryService;
    private final PlanQueryService planQueryService;

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
            throw new ApiPageNotAssociatedException(apiId, page.getId());
        }
    }

    public void validateNameIsUnique(String apiId, String parentId, String name, Page.Type type) {
        var foundPage = this.pageQueryService.findByApiIdAndParentIdAndNameAndType(apiId, parentId, name, type);
        if (foundPage.isPresent()) {
            throw new ValidationDomainException("Name already exists with the same parent and type: " + name);
        }
    }

    public Boolean pageIsHidden(Page page) {
        if (page.isFolder()) {
            var publishedChildren = this.pageQueryService.countByParentIdAndIsPublished(page.getId());
            return publishedChildren <= 0;
        }
        return null;
    }

    public Boolean pageIsUsedAsGeneralConditions(Page page, Api api) {
        if (page.isFolder()) {
            return null;
        }
        var results = this.planQueryService.findAllByApiIdAndGeneralConditionsAndIsActive(
            api.getId(),
            api.getDefinitionVersion(),
            page.getId()
        );
        return !results.isEmpty();
    }
}
