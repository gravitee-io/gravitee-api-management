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
package io.gravitee.apim.core.documentation.usecase;

import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.documentation.crud_service.PageCrudService;
import io.gravitee.apim.core.documentation.domain_service.ApiDocumentationDomainService;
import io.gravitee.apim.core.documentation.model.Breadcrumb;
import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.core.exception.DomainException;
import io.gravitee.apim.core.exception.InvalidPageParentException;
import java.util.*;
import java.util.stream.Stream;

public class ApiGetDocumentationPagesUsecase {

    private final ApiDocumentationDomainService apiDocumentationDomainService;
    private final PageCrudService pageCrudService;
    private final ApiCrudService apiCrudService;

    public ApiGetDocumentationPagesUsecase(
        ApiDocumentationDomainService apiDocumentationDomainService,
        PageCrudService pageCrudService,
        ApiCrudService apiCrudService
    ) {
        this.apiDocumentationDomainService = apiDocumentationDomainService;
        this.pageCrudService = pageCrudService;
        this.apiCrudService = apiCrudService;
    }

    public Output execute(Input input) {
        // Check that api exists
        apiCrudService.get(input.apiId);

        List<Breadcrumb> breadcrumbList = new ArrayList<>();

        if (!"ROOT".equals(input.parentId) && this.isNotEmpty(input.parentId)) {
            // Check if parentId exists + is a folder
            var parent = pageCrudService.get(input.parentId);
            if (!parent.isFolder()) {
                throw new InvalidPageParentException(parent.getId());
            }

            // Calculate breadcrumb list
            var pageBreadcrumbList = this.constructBreadcrumbs(parent).toList();
            for (int i = 0; i < pageBreadcrumbList.size(); i++) {
                var page = pageBreadcrumbList.get(i);
                breadcrumbList.add(Breadcrumb.builder().id(page.getId()).name(page.getName()).position(i + 1).build());
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
