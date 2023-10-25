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

import io.gravitee.apim.core.documentation.crud_service.PageCrudService;
import io.gravitee.apim.core.documentation.query_service.PageQueryService;

public class HomepageDomainService {

    private final PageQueryService pageQueryService;
    private final PageCrudService pageCrudService;

    public HomepageDomainService(PageQueryService pageQueryService, PageCrudService pageCrudService) {
        this.pageQueryService = pageQueryService;
        this.pageCrudService = pageCrudService;
    }

    public void setPreviousHomepageToFalse(String apiId, String currentHomepageId) {
        var pages = pageQueryService.findHomepageByApiId(apiId);
        pages
            .stream()
            .filter(p -> !p.getId().equals(currentHomepageId))
            .forEach(p -> {
                p.setHomepage(false);
                pageCrudService.updateDocumentationPage(p);
            });
    }
}
