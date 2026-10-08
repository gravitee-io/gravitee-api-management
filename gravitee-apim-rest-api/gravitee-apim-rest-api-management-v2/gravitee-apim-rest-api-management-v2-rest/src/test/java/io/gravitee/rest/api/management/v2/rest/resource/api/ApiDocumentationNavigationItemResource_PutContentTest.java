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
package io.gravitee.rest.api.management.v2.rest.resource.api;

import static assertions.MAPIAssertions.assertThat;
import static io.gravitee.common.http.HttpStatusCode.BAD_REQUEST_400;
import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static io.gravitee.common.http.HttpStatusCode.NOT_FOUND_404;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import fixtures.core.model.PortalNavigationItemFixtures;
import fixtures.core.model.PortalPageContentFixtures;
import inmemory.PortalNavigationItemsCrudServiceInMemory;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemSource;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.rest.api.management.v2.rest.model.PortalPageContent;
import io.gravitee.rest.api.management.v2.rest.model.UpdatePortalPageContent;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiDocumentationNavigationItemResource_PutContentTest extends AbstractResourceTest {

    private static final String ENVIRONMENT = "environment-id";
    private static final String API_ID = "api-id";
    private static final String OTHER_API_ID = "other-api-id";
    private static final String ORIGINAL_CONTENT = "# Original content";

    @Inject
    private EnvironmentService environmentService;

    @Autowired
    private PortalNavigationItemsCrudServiceInMemory portalNavigationItemCrudService;

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis/" + API_ID + "/portal-navigation-items";
    }

    @BeforeEach
    public void init() {
        EnvironmentEntity environmentEntity = EnvironmentEntity.builder().id(ENVIRONMENT).organizationId(ORGANIZATION).build();
        when(environmentService.findById(ENVIRONMENT)).thenReturn(environmentEntity);
        when(environmentService.findByOrgAndIdOrHrid(ORGANIZATION, ENVIRONMENT)).thenReturn(environmentEntity);

        GraviteeContext.setCurrentEnvironment(ENVIRONMENT);
        GraviteeContext.setCurrentOrganization(ORGANIZATION);
    }

    @AfterEach
    public void cleanUp() {
        GraviteeContext.cleanContext();
        portalNavigationItemCrudService.reset();
        portalPageContentCrudService.reset();
        portalPageContentQueryService.reset();
    }

    @Test
    void should_save_the_content_of_a_page_owned_by_the_api() {
        var page = aPageOwnedBy(API_ID);

        Response response = saveContentOf(page.getId(), "# New content");

        assertThat(response)
            .hasStatus(OK_200)
            .asEntity(PortalPageContent.class)
            .satisfies(content -> {
                assertThat(content.getId()).isEqualTo(page.getPortalPageContentId().json());
                assertThat(content.getContent()).isEqualTo("# New content");
            });
        assertThat(storedContentOf(page)).isEqualTo("# New content");
    }

    @Test
    void should_not_touch_the_content_of_another_page() {
        var page = aPageOwnedBy(API_ID);
        var otherPage = aPageOwnedBy(API_ID);

        saveContentOf(page.getId(), "# New content");

        assertThat(storedContentOf(otherPage)).isEqualTo(ORIGINAL_CONTENT);
    }

    @Test
    void should_return_404_for_a_page_of_another_api() {
        var foreign = aPageOwnedBy(OTHER_API_ID);

        Response response = saveContentOf(foreign.getId(), "# Hijacked");

        assertThat(response).hasStatus(NOT_FOUND_404);
        assertThat(storedContentOf(foreign)).isEqualTo(ORIGINAL_CONTENT);
    }

    @Test
    void should_return_404_for_a_portal_owned_page() {
        var portalPage = aPage(NavigationItemReference.defaultReference(), null);

        Response response = saveContentOf(portalPage.getId(), "# Hijacked");

        assertThat(response).hasStatus(NOT_FOUND_404);
        assertThat(storedContentOf(portalPage)).isEqualTo(ORIGINAL_CONTENT);
    }

    @Test
    void should_return_400_for_a_folder() {
        var folder = aFolderOwnedByTheApi();

        Response response = saveContentOf(folder.getId(), "# New content");

        assertThat(response).hasStatus(BAD_REQUEST_400);
    }

    @Test
    void should_return_400_for_a_page_fetched_from_an_external_source() {
        var source = PortalNavigationItemSource.builder()
            .sourceType("http-fetcher")
            .sourceConfiguration("{\"url\":\"https://example.com/doc.md\"}")
            .build();
        var sourcedPage = aPage(new NavigationItemReference.ApiReference(API_ID), source);

        Response response = saveContentOf(sourcedPage.getId(), "# New content");

        assertThat(response).hasStatus(BAD_REQUEST_400);
        assertThat(storedContentOf(sourcedPage)).isEqualTo(ORIGINAL_CONTENT);
    }

    @Test
    void should_return_400_when_content_is_empty() {
        var page = aPageOwnedBy(API_ID);

        Response response = saveContentOf(page.getId(), " ");

        assertThat(response).hasStatus(BAD_REQUEST_400);
        assertThat(storedContentOf(page)).isEqualTo(ORIGINAL_CONTENT);
    }

    @Test
    void should_return_403_with_environment_documentation_permission_only() {
        var page = aPageOwnedBy(API_ID);
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.API_DOCUMENTATION),
                eq(API_ID),
                eq(RolePermissionAction.UPDATE)
            )
        ).thenReturn(false);

        Response response = saveContentOf(page.getId(), "# New content");

        assertThat(response).hasStatus(FORBIDDEN_403);
        assertThat(storedContentOf(page)).isEqualTo(ORIGINAL_CONTENT);
    }

    @Test
    void should_succeed_with_api_documentation_permission_only() {
        var page = aPageOwnedBy(API_ID);
        when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(false);
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.API_DOCUMENTATION),
                eq(API_ID),
                eq(RolePermissionAction.UPDATE)
            )
        ).thenReturn(true);

        Response response = saveContentOf(page.getId(), "# New content");

        assertThat(response).hasStatus(OK_200);
    }

    private Response saveContentOf(PortalNavigationItemId id, String content) {
        return rootTarget(id.json()).path("content").request().put(Entity.json(new UpdatePortalPageContent().content(content)));
    }

    private String storedContentOf(PortalNavigationPage page) {
        return portalPageContentCrudService
            .storage()
            .stream()
            .filter(content -> content.getId().equals(page.getPortalPageContentId()))
            .findFirst()
            .map(content -> ((GraviteeMarkdownPageContent) content).getContent().value())
            .orElseThrow();
    }

    private PortalNavigationPage aPageOwnedBy(String apiId) {
        return aPage(new NavigationItemReference.ApiReference(apiId), null);
    }

    private PortalNavigationPage aPage(NavigationItemReference reference, PortalNavigationItemSource source) {
        var content = PortalPageContentFixtures.aGraviteeMarkdownPageContent(
            PortalPageContentId.random(),
            ORGANIZATION,
            ENVIRONMENT,
            ORIGINAL_CONTENT
        );
        portalPageContentCrudService.create(content);
        // In this test context the content query fake does not share the crud storage
        portalPageContentQueryService.storage().add(content);

        var page = PortalNavigationItemFixtures.aPage(PortalNavigationItemId.random().json(), "Overview", null, content.getId())
            .toBuilder()
            .organizationId(ORGANIZATION)
            .environmentId(ENVIRONMENT)
            .reference(reference)
            .source(source)
            .build();
        portalNavigationItemCrudService.create(page);
        return page;
    }

    private PortalNavigationFolder aFolderOwnedByTheApi() {
        var folder = PortalNavigationItemFixtures.aFolder(PortalNavigationItemId.random().json(), "Guides")
            .toBuilder()
            .environmentId(ENVIRONMENT)
            .reference(new NavigationItemReference.ApiReference(API_ID))
            .build();
        portalNavigationItemCrudService.create(folder);
        return folder;
    }
}
