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
package io.gravitee.apim.core.portal_page.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdownContent;
import io.gravitee.apim.core.portal_page.crud_service.PortalPageContentCrudService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemDomainService;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.PortalPageContent;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
@CustomLog
public class CreateDefaultPortalNavigationItemsUseCase {

    private static final String GETTING_STARTED_PATH = "portal-getting-started-page-content.md";
    private static final String AUTHENTICATION_PATH = "portal-authentication-page-content.md";
    private static final String FIRST_API_CALL_PATH = "portal-first-api-call-page-content.md";
    private static final String DOCS_URL = "https://documentation.gravitee.io/apim/developer-portal/new-developer-portal";
    private static final String HOMEPAGE_CONTENT_PATH = "default-portal-homepage-content.md";

    private final PortalNavigationItemDomainService portalNavigationItemDomainService;
    private final PortalPageContentCrudService pageContentCrudService;

    /**
     * Creates a collection of default navigation items:
     *
     * <pre>
     * 🗂 Guides
     *   📄 Getting started
     *   🗂️ Core concepts
     *     📄 Authentication
     *     📄 Making your first API call
     * 🔗 Docs
     * </pre>
     */
    public void execute(String organizationId, String environmentId) {
        final var folderGuides = createPortalFolder("Guides", organizationId, environmentId, PortalArea.TOP_NAVBAR, null);

        final var contentGettingStarted = createPortalPageContent(organizationId, environmentId, GETTING_STARTED_PATH);
        createPortalPage(
            "Getting started",
            organizationId,
            environmentId,
            PortalArea.TOP_NAVBAR,
            contentGettingStarted.getId(),
            folderGuides.getId()
        );

        final var folderCoreConcepts = createPortalFolder(
            "Core concepts",
            organizationId,
            environmentId,
            PortalArea.TOP_NAVBAR,
            folderGuides.getId()
        );

        final var contentAuthentication = createPortalPageContent(organizationId, environmentId, AUTHENTICATION_PATH);
        createPortalPage(
            "Authentication",
            organizationId,
            environmentId,
            PortalArea.TOP_NAVBAR,
            contentAuthentication.getId(),
            folderCoreConcepts.getId()
        );

        final var contentFirstApiCall = createPortalPageContent(organizationId, environmentId, FIRST_API_CALL_PATH);
        createPortalPage(
            "Making your first API call",
            organizationId,
            environmentId,
            PortalArea.TOP_NAVBAR,
            contentFirstApiCall.getId(),
            folderCoreConcepts.getId()
        );

        createPortalLink("Docs", organizationId, environmentId, DOCS_URL, PortalArea.TOP_NAVBAR, null);

        final var contentHomePage = createPortalPageContent(organizationId, environmentId, HOMEPAGE_CONTENT_PATH);
        createPortalPage("Home Page", organizationId, environmentId, PortalArea.HOMEPAGE, contentHomePage.getId(), null);
    }

    private PortalNavigationItem createPortalFolder(
        String title,
        String organizationId,
        String environmentId,
        PortalArea area,
        PortalNavigationItemId parentId
    ) {
        final var createItem = buildCommonItem(title, parentId, area);
        createItem.setType(PortalNavigationItemType.FOLDER);
        return portalNavigationItemDomainService.create(organizationId, environmentId, createItem);
    }

    private PortalNavigationItem createPortalPage(
        String title,
        String organizationId,
        String environmentId,
        PortalArea area,
        PortalPageContentId portalPageContentId,
        PortalNavigationItemId parentId
    ) {
        final var createItem = buildCommonItem(title, parentId, area);
        createItem.setType(PortalNavigationItemType.PAGE);
        createItem.setPortalPageContentId(portalPageContentId);
        return portalNavigationItemDomainService.create(organizationId, environmentId, createItem);
    }

    private PortalNavigationItem createPortalLink(
        String title,
        String organizationId,
        String environmentId,
        String url,
        PortalArea area,
        PortalNavigationItemId parentId
    ) {
        final var createItem = buildCommonItem(title, parentId, area);
        createItem.setType(PortalNavigationItemType.LINK);
        createItem.setUrl(url);
        return portalNavigationItemDomainService.create(organizationId, environmentId, createItem);
    }

    private CreatePortalNavigationItem buildCommonItem(String title, PortalNavigationItemId parentId, PortalArea area) {
        return CreatePortalNavigationItem.builder().title(title).area(area).parentId(parentId).published(true).build();
    }

    private PortalPageContent<?> createPortalPageContent(String organizationId, String environmentId, String contentPath) {
        final var content = new GraviteeMarkdownPageContent(
            PortalPageContentId.random(),
            organizationId,
            environmentId,
            new GraviteeMarkdownContent(loadContent(contentPath))
        );
        return pageContentCrudService.create(content);
    }

    private String loadContent(String contentPath) {
        try (
            final var is = Thread.currentThread().getContextClassLoader().getResourceAsStream(String.format("templates/%s", contentPath))
        ) {
            if (is == null) {
                throw new IllegalStateException(String.format("Could not load default portal page template for %s", contentPath));
            }
            return new String(is.readAllBytes());
        } catch (Exception e) {
            throw new IllegalStateException("Could not load default portal page template", e);
        }
    }
}
