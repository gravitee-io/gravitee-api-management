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
package io.gravitee.apim.core.portal_page.model;

import io.gravitee.apim.core.portal_page.domain_service.ContentSanitizedSpecification;
import io.gravitee.apim.core.portal_page.domain_service.PageExistsSpecification;
import java.util.Map;

public class Portal {

    private final Map<PageId, PortalPage> pages;
    private final Map<Entrypoint, PortalPage> entrypoints;

    public Portal(Map<PageId, PortalPage> pages, Map<Entrypoint, PortalPage> entrypoints) {
        this.pages = pages;
        this.entrypoints = entrypoints;
    }

    public PortalPage create(GraviteeMarkdown pageContent) {
        var contentSpec = new ContentSanitizedSpecification();
        contentSpec.throwIfNotSatisfied(pageContent);
        PageId newId = PageId.random();
        PortalPage newPage = new PortalPage(newId, pageContent);
        pages.put(newId, newPage);
        return newPage;
    }

    public PortalPage getEntrypointPage(Entrypoint entrypoint) {
        var pageExistsSpec = PageExistsSpecification.byEntrypoint(entrypoints::containsKey);
        pageExistsSpec.throwIfNotSatisfied(entrypoint);
        return entrypoints.get(entrypoint);
    }

    public PortalPage setEntrypointPage(Entrypoint entrypoint, PageId pageId) {
        var pageExistsSpec = PageExistsSpecification.byPageId(pages::containsKey);
        pageExistsSpec.throwIfNotSatisfied(pageId);
        var page = pages.get(pageId);
        entrypoints.put(entrypoint, page);
        return page;
    }
}
