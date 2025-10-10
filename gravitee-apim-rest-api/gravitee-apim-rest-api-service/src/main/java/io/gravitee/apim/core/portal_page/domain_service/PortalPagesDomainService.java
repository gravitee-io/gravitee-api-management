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
package io.gravitee.apim.core.portal_page.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.portal_page.crud_service.PortalPageContextCrudService;
import io.gravitee.apim.core.portal_page.crud_service.PortalPageCrudService;
import io.gravitee.apim.core.portal_page.model.PortalPage;
import io.gravitee.apim.core.portal_page.model.PortalPageContext;
import io.gravitee.apim.core.portal_page.model.PortalPageView;
import io.gravitee.rest.api.service.common.UuidString;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
public class PortalPagesDomainService {

    private final PortalPageCrudService portalPageCrudService;
    private final PortalPageContextCrudService portalPageContextCrudService;

    public void createWithContext(PortalPage page, PortalPageView view, String environmentId) {
        var created = portalPageCrudService.create(page);
        var pageContext = PortalPageContext.builder()
            .pageId(created.getId().toString())
            .id(UuidString.generateRandom())
            .contextType(view.context())
            .published(view.published())
            .environmentId(environmentId)
            .build();
        portalPageContextCrudService.create(pageContext);
    }
}
