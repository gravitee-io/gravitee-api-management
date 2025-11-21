package io.gravitee.apim.core.portal_page.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.portal_page.crud_service.PortalNavigationItemCrudService;
import io.gravitee.apim.core.portal_page.crud_service.PortalPageContentCrudService;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationLink;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.model.PortalPageContent;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@UseCase
@RequiredArgsConstructor
public class CreateDefaultPortalNavigationItemsUseCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateDefaultPortalNavigationItemsUseCase.class);

    private static final String GETTING_STARTED_PATH = "portal-getting-started-page-content.md";
    private static final String AUTHENTICATION_PATH  ="portal-authentication-page-content.md";
    private static final String FIRST_API_CALL_PATH = "portal-first-api-call-page-content.md";
    private static final String DOCS_URL = "https://documentation.gravitee.io/apim/developer-portal/new-developer-portal";

    private final PortalNavigationItemCrudService portalNavigationItemCrudService;
    private final PortalPageContentCrudService pageContentCrudService;

    /**
     * Creates a collection of default navigation items:
     *
     * <pre>
     * - Folder “Guides”
     *   - GMD page “Getting started”
     *   - Folder “Core concepts”
     *     - GMD page “Authentication”
     *     - GMD page “Making your first API call”
     * - Link “Docs”
     * </pre>
     *
     * @param environmentId
     */
    public void execute(String environmentId) {
        final var organizationId = "ORGANIZATION-ID";

        final var folderGuides = createPortalFolder("Guides", organizationId, environmentId, null);

        final var contentGettingStarted = createPortalPageContent(GETTING_STARTED_PATH);
        createPortalPage("Getting started", organizationId, environmentId, contentGettingStarted.getId(), folderGuides.getId());

        final var folderCoreConcepts = createPortalFolder("Core concepts", organizationId, environmentId, folderGuides.getId());

        final var contentAuthentication = createPortalPageContent(AUTHENTICATION_PATH);
        createPortalPage("Authentication", organizationId, environmentId, contentAuthentication.getId(), folderCoreConcepts.getId());

        final var contentFirstApiCall = createPortalPageContent(FIRST_API_CALL_PATH);
        createPortalPage("Making your first API call", organizationId, environmentId, contentFirstApiCall.getId(), folderCoreConcepts.getId());

        createPortalLink("Docs", organizationId, environmentId, DOCS_URL, folderGuides.getId());
    }

    private PortalNavigationFolder createPortalFolder(String title, String organizationId, String environmentId, PortalNavigationItemId parentId) {
        final var folder = new PortalNavigationFolder(PortalNavigationItemId.random(), organizationId, environmentId, title, PortalArea.TOP_NAVBAR, 0);
        folder.setParentId(parentId);
        return (PortalNavigationFolder) portalNavigationItemCrudService.create(folder);
    }

    private PortalNavigationPage createPortalPage(String title, String organizationId, String environmentId, PortalPageContentId portalPageContentId, PortalNavigationItemId parentId) {
        final var page = new PortalNavigationPage(PortalNavigationItemId.random(), organizationId, environmentId, title, PortalArea.TOP_NAVBAR, 0, portalPageContentId);
        page.setParentId(parentId);
        return (PortalNavigationPage) portalNavigationItemCrudService.create(page);
    }

    private PortalNavigationLink createPortalLink(String title, String organizationId, String environmentId, String url, PortalNavigationItemId parentId) {
        final var link = new PortalNavigationLink(PortalNavigationItemId.random(), organizationId, environmentId, title, PortalArea.TOP_NAVBAR, 0, url);
        link.setParentId(parentId);
        return (PortalNavigationLink) portalNavigationItemCrudService.create(link);
    }

    private PortalPageContent createPortalPageContent(String contentPath) {
        final var content = new GraviteeMarkdownPageContent(PortalPageContentId.random(), loadContent(contentPath));
        return pageContentCrudService.create(content);
    }

    private String loadContent(String contentPath) {
        try {
            final var resource = new ClassPathResource(String.format("templates/%s", contentPath));
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.warn("Failed to load content from {}", contentPath, e);
            return ""; // FIXME or throw an exception?
        }
    }
}