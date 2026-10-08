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

import inmemory.PortalNavigationItemSourceDomainServiceInMemory;
import inmemory.PortalNavigationItemsCrudServiceInMemory;
import inmemory.PortalPageContentCrudServiceInMemory;
import inmemory.PortalPageContentQueryServiceInMemory;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal.model.PortalVisibility;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemSource;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.rest.api.management.v2.rest.model.FetchPortalNavigationItemResponse;
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
class ApiDocumentationNavigationItemResource_FetchTest extends AbstractResourceTest {

    private static final String ENVIRONMENT = "environment-id";
    private static final String API_ID = "api-id";
    private static final String OTHER_API_ID = "other-api-id";

    @Inject
    private EnvironmentService environmentService;

    @Autowired
    private PortalNavigationItemsCrudServiceInMemory portalNavigationItemCrudService;

    @Autowired
    private PortalPageContentCrudServiceInMemory portalPageContentCrudService;

    @Autowired
    private PortalPageContentQueryServiceInMemory portalPageContentQueryService;

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
    void should_refresh_an_externally_sourced_page_and_keep_it_unpublished_and_owned_by_the_api() {
        var page = aPageOwnedBy(API_ID, aSource());

        Response response = fetch(page.getId());

        assertThat(response).hasStatus(OK_200);
        var item = (io.gravitee.rest.api.management.v2.rest.model.PortalNavigationPage) response
            .readEntity(FetchPortalNavigationItemResponse.class)
            .getItem()
            .getActualInstance();
        assertThat(item.getSource().getLastFetchedAt()).isNotNull();
        assertThat(item.getPublished()).isFalse();
        assertThat(stored(page.getId()).getReference()).isEqualTo(new NavigationItemReference.ApiReference(API_ID));
        assertThat(storedContentOf(page)).isEqualTo(PortalNavigationItemSourceDomainServiceInMemory.MARKDOWN);
    }

    @Test
    void should_return_400_for_an_item_with_no_source_as_the_environment_path_does() {
        var page = aPageOwnedBy(API_ID, null);

        Response response = fetch(page.getId());

        assertThat(response).hasStatus(BAD_REQUEST_400);
    }

    @Test
    void should_return_404_for_an_unknown_item() {
        Response response = fetch(PortalNavigationItemId.random());

        assertThat(response).hasStatus(NOT_FOUND_404);
    }

    @Test
    void should_reject_an_item_id_belonging_to_another_api() {
        var foreign = aPageOwnedBy(OTHER_API_ID, aSource());

        Response response = fetch(foreign.getId());

        assertThat(response).hasStatus(NOT_FOUND_404);
        assertThat(storedContentOf(foreign)).isEqualTo("# Default content");
    }

    @Test
    void should_reject_a_portal_owned_item() {
        var portalPage = aPage(NavigationItemReference.defaultReference(), aSource());

        Response response = fetch(portalPage.getId());

        assertThat(response).hasStatus(NOT_FOUND_404);
        assertThat(storedContentOf(portalPage)).isEqualTo("# Default content");
    }

    @Test
    void should_return_403_with_environment_documentation_permission_only() {
        var page = aPageOwnedBy(API_ID, aSource());
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.API_DOCUMENTATION),
                eq(API_ID),
                eq(RolePermissionAction.UPDATE)
            )
        ).thenReturn(false);

        Response response = fetch(page.getId());

        assertThat(response).hasStatus(FORBIDDEN_403);
        assertThat(storedContentOf(page)).isEqualTo("# Default content");
    }

    @Test
    void should_succeed_with_api_documentation_permission_only() {
        var page = aPageOwnedBy(API_ID, aSource());
        when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(false);
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.API_DOCUMENTATION),
                eq(API_ID),
                eq(RolePermissionAction.UPDATE)
            )
        ).thenReturn(true);

        Response response = fetch(page.getId());

        assertThat(response).hasStatus(OK_200);
    }

    private Response fetch(PortalNavigationItemId id) {
        return rootTarget(id.json()).path("_fetch").request().post(Entity.json(""));
    }

    private PortalNavigationItem stored(PortalNavigationItemId id) {
        return portalNavigationItemCrudService
            .storage()
            .stream()
            .filter(item -> item.getId().equals(id))
            .findFirst()
            .orElseThrow();
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

    private PortalNavigationPage aPageOwnedBy(String apiId, PortalNavigationItemSource source) {
        return aPage(new NavigationItemReference.ApiReference(apiId), source);
    }

    private PortalNavigationPage aPage(NavigationItemReference reference, PortalNavigationItemSource source) {
        var content = GraviteeMarkdownPageContent.create(ORGANIZATION, ENVIRONMENT, "# Default content");
        portalPageContentCrudService.create(content);
        // In this test context the content query fake does not share the crud storage
        portalPageContentQueryService.storage().add(content);

        var page = PortalNavigationPage.builder()
            .id(PortalNavigationItemId.random())
            .organizationId(ORGANIZATION)
            .environmentId(ENVIRONMENT)
            .title("Sourced Page")
            .segment("sourced-page")
            .area(PortalArea.TOP_NAVBAR)
            .order(0)
            .portalPageContentId(content.getId())
            .published(false)
            .visibility(PortalVisibility.PUBLIC)
            .reference(reference)
            .source(source)
            .build();
        portalNavigationItemCrudService.create(page);
        return page;
    }

    private static PortalNavigationItemSource aSource() {
        return PortalNavigationItemSource.builder()
            .sourceType("http-fetcher")
            .sourceConfiguration("{\"url\":\"https://example.com/doc.md\"}")
            .build();
    }
}
