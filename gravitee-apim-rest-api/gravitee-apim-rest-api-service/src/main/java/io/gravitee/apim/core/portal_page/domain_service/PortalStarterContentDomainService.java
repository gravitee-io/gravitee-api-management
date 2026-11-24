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
import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdown;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal_page.crud_service.PortalNavigationItemCrudService;
import io.gravitee.apim.core.portal_page.crud_service.PortalPageContentCrudService;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemQueryCriteria;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.model.PortalPageContent;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.model.PortalPageContentType;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import java.util.List;
import lombok.RequiredArgsConstructor;

/** The navigation and homepage a portal starts with. */
@DomainService
@RequiredArgsConstructor
public class PortalStarterContentDomainService {

    private static final String GETTING_STARTED_PATH = "portal-getting-started-page-content.md";
    private static final String AUTHENTICATION_PATH = "portal-authentication-page-content.md";
    private static final String FIRST_API_CALL_PATH = "portal-first-api-call-page-content.md";
    private static final String DOCS_URL = "https://documentation.gravitee.io/apim/developer-portal/new-developer-portal";
    private static final String HOMEPAGE_CONTENT_PATH = "default-portal-homepage-content.md";
    private static final String APIS_SECTION_TITLE = "APIs";

    private final PortalNavigationItemDomainService portalNavigationItemDomainService;
    private final PortalPageContentCrudService pageContentCrudService;
    private final PortalNavigationItemsQueryService portalNavigationItemsQueryService;
    private final PortalNavigationItemCrudService portalNavigationItemCrudService;

    /**
     * Creates whichever of the following navigation items, and the homepage, the portal is still missing:
     *
     * <pre>
     * 🗂 Guides
     *   📄 Getting started
     *   🗂️ Core concepts
     *     📄 Authentication
     *     📄 Making your first API call
     * 🗂 APIs
     * 🔗 Docs
     * </pre>
     *
     * Idempotent per item, so it is safe to call more than once for the same portal (e.g. to repair a
     * portal left partially seeded by a previous failed run): items that already exist by title under their
     * expected parent are left untouched, and only missing items are created. Other portals' items are neither
     * read nor changed.
     */
    public void seed(String organizationId, String environmentId, NavigationItemReference.PortalReference owner) {
        final var topLevelTopNavbarItems = portalNavigationItemsQueryService.findTopLevelItemsByEnvironmentIdAndPortalAreaAndReference(
            environmentId,
            PortalArea.TOP_NAVBAR,
            owner
        );

        var folderGuides = findByTitleAndType(topLevelTopNavbarItems, "Guides", PortalNavigationItemType.FOLDER);
        if (folderGuides == null) {
            folderGuides = createPortalFolder("Guides", organizationId, environmentId, owner, PortalArea.TOP_NAVBAR, null);
        }

        final var guidesChildren = portalNavigationItemsQueryService.findByParentIdAndEnvironmentId(environmentId, folderGuides.getId());

        if (findByTitleAndType(guidesChildren, "Getting started", PortalNavigationItemType.PAGE) == null) {
            createPortalPage(
                "Getting started",
                organizationId,
                environmentId,
                owner,
                PortalArea.TOP_NAVBAR,
                GETTING_STARTED_PATH,
                folderGuides.getId()
            );
        }

        var folderCoreConcepts = findByTitleAndType(guidesChildren, "Core concepts", PortalNavigationItemType.FOLDER);
        if (folderCoreConcepts == null) {
            folderCoreConcepts = createPortalFolder(
                "Core concepts",
                organizationId,
                environmentId,
                owner,
                PortalArea.TOP_NAVBAR,
                folderGuides.getId()
            );
        }

        final var coreConceptsChildren = portalNavigationItemsQueryService.findByParentIdAndEnvironmentId(
            environmentId,
            folderCoreConcepts.getId()
        );

        if (findByTitleAndType(coreConceptsChildren, "Authentication", PortalNavigationItemType.PAGE) == null) {
            createPortalPage(
                "Authentication",
                organizationId,
                environmentId,
                owner,
                PortalArea.TOP_NAVBAR,
                AUTHENTICATION_PATH,
                folderCoreConcepts.getId()
            );
        }

        if (findByTitleAndType(coreConceptsChildren, "Making your first API call", PortalNavigationItemType.PAGE) == null) {
            createPortalPage(
                "Making your first API call",
                organizationId,
                environmentId,
                owner,
                PortalArea.TOP_NAVBAR,
                FIRST_API_CALL_PATH,
                folderCoreConcepts.getId()
            );
        }

        ensureApisSection(organizationId, environmentId, owner);

        if (findByTitleAndType(topLevelTopNavbarItems, "Docs", PortalNavigationItemType.LINK) == null) {
            createPortalLink("Docs", organizationId, environmentId, owner, DOCS_URL, PortalArea.TOP_NAVBAR, null);
        }

        final var existingHomepage = portalNavigationItemsQueryService.findTopLevelItemsByEnvironmentIdAndPortalAreaAndReference(
            environmentId,
            PortalArea.HOMEPAGE,
            owner
        );
        if (existingHomepage.isEmpty()) {
            createPortalPage("Home Page", organizationId, environmentId, owner, PortalArea.HOMEPAGE, HOMEPAGE_CONTENT_PATH, null);
        }
    }

    /** Deletes every navigation item of the portal and the content of its pages, as for a portal whose creation is undone. */
    public void deleteAll(String environmentId, NavigationItemReference.PortalReference owner) {
        var items = portalNavigationItemsQueryService.search(
            PortalNavigationItemQueryCriteria.builder().environmentId(environmentId).reference(owner).build()
        );
        portalNavigationItemCrudService.deleteByIds(items.stream().map(PortalNavigationItem::getId).toList());
        items
            .stream()
            .filter(PortalNavigationPage.class::isInstance)
            .map(page -> ((PortalNavigationPage) page).getPortalPageContentId())
            .forEach(pageContentCrudService::delete);
    }

    /**
     * Creates the "APIs" section when the portal has none: a published top-level folder of the main navigation, which is
     * what an API can be published into. Added after whatever the menu already holds.
     */
    public void ensureApisSection(String organizationId, String environmentId, NavigationItemReference owner) {
        final var hasApisSection = portalNavigationItemsQueryService
            .findTopLevelItemsByEnvironmentIdAndPortalAreaAndReference(environmentId, PortalArea.TOP_NAVBAR, owner)
            .stream()
            .anyMatch(item -> APIS_SECTION_TITLE.equals(item.getTitle()) && item.getType() == PortalNavigationItemType.FOLDER);
        if (!hasApisSection) {
            createPortalFolder(APIS_SECTION_TITLE, organizationId, environmentId, owner, PortalArea.TOP_NAVBAR, null);
        }
    }

    private static PortalNavigationItem findByTitleAndType(List<PortalNavigationItem> items, String title, PortalNavigationItemType type) {
        return items
            .stream()
            .filter(item -> title.equals(item.getTitle()) && item.getType() == type)
            .findFirst()
            .orElse(null);
    }

    private PortalNavigationItem createPortalFolder(
        String title,
        String organizationId,
        String environmentId,
        NavigationItemReference owner,
        PortalArea area,
        PortalNavigationItemId parentId
    ) {
        final var createItem = buildCommonItem(title, owner, parentId, area);
        createItem.setType(PortalNavigationItemType.FOLDER);
        return portalNavigationItemDomainService.create(organizationId, environmentId, createItem);
    }

    private PortalNavigationItem createPortalPage(
        String title,
        String organizationId,
        String environmentId,
        NavigationItemReference owner,
        PortalArea area,
        String contentPath,
        PortalNavigationItemId parentId
    ) {
        final var content = createPortalPageContent(organizationId, environmentId, contentPath);
        final var createItem = buildCommonItem(title, owner, parentId, area);
        createItem.setType(PortalNavigationItemType.PAGE);
        createItem.setPortalPageContentId(content.getId());
        try {
            return portalNavigationItemDomainService.create(organizationId, environmentId, createItem);
        } catch (RuntimeException creationFailure) {
            // No page would lead to this content, and deleting the portal's items would not find it
            try {
                pageContentCrudService.delete(content.getId());
            } catch (RuntimeException cleanupFailure) {
                creationFailure.addSuppressed(cleanupFailure);
            }
            throw creationFailure;
        }
    }

    private PortalNavigationItem createPortalLink(
        String title,
        String organizationId,
        String environmentId,
        NavigationItemReference owner,
        String url,
        PortalArea area,
        PortalNavigationItemId parentId
    ) {
        final var createItem = buildCommonItem(title, owner, parentId, area);
        createItem.setType(PortalNavigationItemType.LINK);
        createItem.setUrl(url);
        return portalNavigationItemDomainService.create(organizationId, environmentId, createItem);
    }

    private CreatePortalNavigationItem buildCommonItem(
        String title,
        NavigationItemReference owner,
        PortalNavigationItemId parentId,
        PortalArea area
    ) {
        return CreatePortalNavigationItem.builder()
            .title(title)
            .area(area)
            .parentId(parentId)
            .reference(owner)
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
            final var is = PortalStarterContentDomainService.class.getClassLoader().getResourceAsStream(
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
