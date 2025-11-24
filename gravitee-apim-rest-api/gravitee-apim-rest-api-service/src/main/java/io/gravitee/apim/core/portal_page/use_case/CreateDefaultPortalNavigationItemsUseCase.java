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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

@UseCase
@RequiredArgsConstructor
public class CreateDefaultPortalNavigationItemsUseCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateDefaultPortalNavigationItemsUseCase.class);

    private static final String GETTING_STARTED_PATH = "portal-getting-started-page-content.md";
    private static final String AUTHENTICATION_PATH = "portal-authentication-page-content.md";
    private static final String FIRST_API_CALL_PATH = "portal-first-api-call-page-content.md";
    private static final String DOCS_URL = "https://documentation.gravitee.io/apim/developer-portal/new-developer-portal";

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
        final var folderGuides = createPortalFolder("Guides", organizationId, environmentId, null);

        final var contentGettingStarted = createPortalPageContent(GETTING_STARTED_PATH);
        createPortalPage("Getting started", organizationId, environmentId, contentGettingStarted.getId(), folderGuides.getId());

        final var folderCoreConcepts = createPortalFolder("Core concepts", organizationId, environmentId, folderGuides.getId());

        final var contentAuthentication = createPortalPageContent(AUTHENTICATION_PATH);
        createPortalPage("Authentication", organizationId, environmentId, contentAuthentication.getId(), folderCoreConcepts.getId());

        final var contentFirstApiCall = createPortalPageContent(FIRST_API_CALL_PATH);
        createPortalPage(
            "Making your first API call",
            organizationId,
            environmentId,
            contentFirstApiCall.getId(),
            folderCoreConcepts.getId()
        );

        createPortalLink("Docs", organizationId, environmentId, DOCS_URL, null);
    }

    private PortalNavigationItem createPortalFolder(
        String title,
        String organizationId,
        String environmentId,
        PortalNavigationItemId parentId
    ) {
        final var createItem = buildCommonItem(title, parentId);
        createItem.setType(PortalNavigationItemType.FOLDER);
        return portalNavigationItemDomainService.create(organizationId, environmentId, createItem);
    }

    private PortalNavigationItem createPortalPage(
        String title,
        String organizationId,
        String environmentId,
        PortalPageContentId portalPageContentId,
        PortalNavigationItemId parentId
    ) {
        final var createItem = buildCommonItem(title, parentId);
        createItem.setType(PortalNavigationItemType.PAGE);
        createItem.setPortalPageContentId(portalPageContentId);
        return portalNavigationItemDomainService.create(organizationId, environmentId, createItem);
    }

    private PortalNavigationItem createPortalLink(
        String title,
        String organizationId,
        String environmentId,
        String url,
        PortalNavigationItemId parentId
    ) {
        final var createItem = buildCommonItem(title, parentId);
        createItem.setType(PortalNavigationItemType.LINK);
        createItem.setUrl(url);
        return portalNavigationItemDomainService.create(organizationId, environmentId, createItem);
    }

    private CreatePortalNavigationItem buildCommonItem(String title, PortalNavigationItemId parentId) {
        return CreatePortalNavigationItem.builder().title(title).area(PortalArea.TOP_NAVBAR).parentId(parentId).build();
    }

    private PortalPageContent createPortalPageContent(String contentPath) {
        final var content = new GraviteeMarkdownPageContent(PortalPageContentId.random(), loadContent(contentPath));
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
