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
package io.gravitee.apim.core.portal_page.domain_service.validation;

import io.gravitee.apim.core.portal_page.exception.PageContentNotFoundException;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.query_service.PortalPageContentQueryService;
import lombok.RequiredArgsConstructor;

/**
 * For PAGE type with a non-null portalPageContentId, ensures the page content exists.
 */
@RequiredArgsConstructor
public class PageContentExistsRule implements CreatePortalNavigationItemValidationRule {

    private final PortalPageContentQueryService pageContentQueryService;

    @Override
    public boolean appliesTo(CreatePortalNavigationItem item) {
        return item.getType() == PortalNavigationItemType.PAGE && item.getPortalPageContentId() != null;
    }

    @Override
    public void validate(CreatePortalNavigationItem item, String environmentId, CreateValidationContext ctx) {
        var existingPageContent = pageContentQueryService.findById(item.getPortalPageContentId());
        if (existingPageContent.isEmpty()) {
            throw new PageContentNotFoundException(item.getPortalPageContentId().toString());
        }
    }
}
