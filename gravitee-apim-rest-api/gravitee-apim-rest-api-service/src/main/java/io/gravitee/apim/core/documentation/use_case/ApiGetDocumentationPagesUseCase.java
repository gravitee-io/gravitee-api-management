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
package io.gravitee.apim.core.documentation.use_case;

import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.documentation.crud_service.PageCrudService;
import io.gravitee.apim.core.documentation.domain_service.ApiDocumentationDomainService;
import io.gravitee.apim.core.documentation.exception.InvalidPageParentException;
import io.gravitee.apim.core.documentation.model.Breadcrumb;
import io.gravitee.apim.core.documentation.model.Page;
import java.util.*;
import java.util.stream.Stream;

public class ApiGetDocumentationPagesUseCase {

    private final ApiDocumentationDomainService apiDocumentationDomainService;
    private final ApiCrudService apiCrudService;
    private final PageCrudService pageCrudService;

    public ApiGetDocumentationPagesUseCase(
        ApiDocumentationDomainService apiDocumentationDomainService,
        ApiCrudService apiCrudService,
        PageCrudService pageCrudService
    ) {
        this.apiDocumentationDomainService = apiDocumentationDomainService;
        this.apiCrudService = apiCrudService;
        this.pageCrudService = pageCrudService;
    }

    public Output execute(Input input) {
        this.apiCrudService.get(input.apiId);

        List<Breadcrumb> breadcrumbList = new ArrayList<>();

        if (!"ROOT".equals(input.parentId) && this.isNotEmpty(input.parentId)) {
            var page = this.pageCrudService.get(input.parentId);
            this.apiDocumentationDomainService.validatePageAssociatedToApi(page, input.apiId);

            if (!page.isFolder()) {
                throw new InvalidPageParentException(page.getId());
            }

            // Calculate breadcrumb list
            var pageBreadcrumbList = this.constructBreadcrumbs(page).toList();
            for (int i = 0; i < pageBreadcrumbList.size(); i++) {
                var pageBreadcrumb = pageBreadcrumbList.get(i);
                breadcrumbList.add(Breadcrumb.builder().id(pageBreadcrumb.getId()).name(pageBreadcrumb.getName()).position(i + 1).build());
            }
        }

        var pages = apiDocumentationDomainService.getApiPages(input.apiId, input.parentId);

        return new Output(pages, breadcrumbList);
    }

    public record Input(String apiId, String parentId) {}

    public record Output(List<Page> pages, List<Breadcrumb> breadcrumbList) {}

    private Stream<Page> constructBreadcrumbs(Page page) {
        if (this.isNotEmpty(page.getParentId())) {
            var parent = pageCrudService.findById(page.getParentId());
            if (parent.isPresent()) {
                return Stream.concat(this.constructBreadcrumbs(parent.get()), Stream.of(page));
            }
        }
        return Stream.of(page);
    }

    private boolean isNotEmpty(String str) {
        return !Objects.isNull(str) && !str.isEmpty();
    }
}
