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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class ApiGetDocumentationPageUsecase {

    private final PageCrudService pageCrudService;
    private final ApiCrudService apiCrudService;

    public ApiGetDocumentationPageUsecase(PageCrudService pageCrudService, ApiCrudService apiCrudService) {
        this.pageCrudService = pageCrudService;
        this.apiCrudService = apiCrudService;
    }

    public Output execute(Input input) {
        // Check that api exists
        apiCrudService.get(input.apiId);

        var page = this.pageCrudService.get(input.pageId);

        if (!page.getReferenceId().equals(input.apiId) || !page.getReferenceType().equals(Page.ReferenceType.API)) {
            throw new DomainException("Page is not associated to Api: " + input.apiId);
        }

        return new Output(page);
    }

    public record Input(String apiId, String pageId) {}

    public record Output(Page page) {}
}
