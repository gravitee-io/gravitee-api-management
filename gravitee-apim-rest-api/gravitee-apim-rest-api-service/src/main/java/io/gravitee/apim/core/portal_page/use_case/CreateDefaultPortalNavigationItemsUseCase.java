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
import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdown;
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
import io.gravitee.apim.core.portal_page.model.PortalPageContentType;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import java.util.List;
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
    private final PortalNavigationItemsQueryService portalNavigationItemsQueryService;

    /**
     * Creates whichever of the following default navigation items are still missing for the environment:
     *
     * <pre>
     * 🗂 Guides
     *   📄 Getting started
     *   🗂️ Core concepts
     *     📄 Authentication
     *     📄 Making your first API call
     * 🔗 Docs
     * </pre>
     *
     * Idempotent per item, so it is safe to call more than once for the same environment (e.g. to repair
     * an environment left partially seeded by a previous failed run): items that already exist by title
     * under their expected parent are left untouched, and only missing items are created.
     */
    public void execute(String organizationId, String environmentId) {
        final var topLevelTopNavbarItems = portalNavigationItemsQueryService.findTopLevelItemsByEnvironmentIdAndPortalArea(
            environmentId,
            PortalArea.TOP_NAVBAR
        );

        var folderGuides = findByTitle(topLevelTopNavbarItems, "Guides");
        if (folderGuides == null) {
            folderGuides = createPortalFolder("Guides", organizationId, environmentId, PortalArea.TOP_NAVBAR, null);
        }

        final var guidesChildren = portalNavigationItemsQueryService.findByParentIdAndEnvironmentId(environmentId, folderGuides.getId());

        if (findByTitle(guidesChildren, "Getting started") == null) {
            final var contentGettingStarted = createPortalPageContent(organizationId, environmentId, GETTING_STARTED_PATH);
            createPortalPage(
                "Getting started",
                organizationId,
                environmentId,
                PortalArea.TOP_NAVBAR,
                contentGettingStarted.getId(),
                folderGuides.getId()
            );
        }

        var folderCoreConcepts = findByTitle(guidesChildren, "Core concepts");
        if (folderCoreConcepts == null) {
            folderCoreConcepts = createPortalFolder(
                "Core concepts",
                organizationId,
                environmentId,
                PortalArea.TOP_NAVBAR,
                folderGuides.getId()
            );
        }

        final var coreConceptsChildren = portalNavigationItemsQueryService.findByParentIdAndEnvironmentId(
            environmentId,
            folderCoreConcepts.getId()
        );

        if (findByTitle(coreConceptsChildren, "Authentication") == null) {
            final var contentAuthentication = createPortalPageContent(organizationId, environmentId, AUTHENTICATION_PATH);
            createPortalPage(
                "Authentication",
                organizationId,
                environmentId,
                PortalArea.TOP_NAVBAR,
                contentAuthentication.getId(),
                folderCoreConcepts.getId()
            );
        }

        if (findByTitle(coreConceptsChildren, "Making your first API call") == null) {
            final var contentFirstApiCall = createPortalPageContent(organizationId, environmentId, FIRST_API_CALL_PATH);
            createPortalPage(
                "Making your first API call",
                organizationId,
                environmentId,
                PortalArea.TOP_NAVBAR,
                contentFirstApiCall.getId(),
                folderCoreConcepts.getId()
            );
        }

        if (findByTitle(topLevelTopNavbarItems, "Docs") == null) {
            createPortalLink("Docs", organizationId, environmentId, DOCS_URL, PortalArea.TOP_NAVBAR, null);
        }

        final var existingHomepage = portalNavigationItemsQueryService.findTopLevelItemsByEnvironmentIdAndPortalArea(
            environmentId,
            PortalArea.HOMEPAGE
        );
        if (existingHomepage.isEmpty()) {
            final var contentHomePage = createPortalPageContent(organizationId, environmentId, HOMEPAGE_CONTENT_PATH);
            createPortalPage("Home Page", organizationId, environmentId, PortalArea.HOMEPAGE, contentHomePage.getId(), null);
        }
    }

    private static PortalNavigationItem findByTitle(List<PortalNavigationItem> items, String title) {
        return items.stream().filter(item -> title.equals(item.getTitle())).findFirst().orElse(null);
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
        return CreatePortalNavigationItem.builder()
            .title(title)
            .area(area)
            .parentId(parentId)
            .published(true)
            .contentType(PortalPageContentType.GRAVITEE_MARKDOWN)
            .build();
    }

    private PortalPageContent<?> createPortalPageContent(String organizationId, String environmentId, String contentPath) {
        final var content = new GraviteeMarkdownPageContent(
            PortalPageContentId.random(),
            organizationId,
            environmentId,
            new GraviteeMarkdown(loadContent(contentPath))
        );
        return pageContentCrudService.create(content);
    }

    private String loadContent(String contentPath) {
        try (
            final var is = CreateDefaultPortalNavigationItemsUseCase.class.getClassLoader().getResourceAsStream(
                String.format("templates/%s", contentPath)
            )
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
