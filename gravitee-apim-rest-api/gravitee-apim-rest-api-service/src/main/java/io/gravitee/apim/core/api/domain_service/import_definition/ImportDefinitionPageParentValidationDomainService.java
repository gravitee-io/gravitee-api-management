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
package io.gravitee.apim.core.api.domain_service.import_definition;

import static io.gravitee.apim.core.documentation.domain_service.DocumentationValidationDomainService.validateParentOwnership;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.documentation.crud_service.PageCrudService;
import io.gravitee.apim.core.documentation.exception.PageParentNotFoundException;
import io.gravitee.apim.core.documentation.model.Page;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
public class ImportDefinitionPageParentValidationDomainService {

    private final PageCrudService pageCrudService;

    public void validate(List<Page> pages) {
        var pagesById = pages.stream().collect(Collectors.toMap(Page::getId, Function.identity(), (first, last) -> last));

        for (var page : pages) {
            pageCrudService.findById(page.getId()).ifPresent(existing -> validateParentOwnership(page, existing));

            var parentId = page.getParentId();
            if (parentId == null || parentId.isEmpty()) {
                continue;
            }

            var parent = pagesById.get(parentId);
            if (parent == null) {
                parent = pageCrudService.findById(parentId).orElseThrow(() -> new PageParentNotFoundException(parentId));
            }
            validateParentOwnership(page, parent);
        }
    }
}
