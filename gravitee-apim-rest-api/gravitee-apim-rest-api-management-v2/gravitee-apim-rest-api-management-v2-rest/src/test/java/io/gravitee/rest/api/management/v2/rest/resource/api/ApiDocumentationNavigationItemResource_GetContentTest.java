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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import fixtures.core.model.PortalNavigationItemFixtures;
import fixtures.core.model.PortalPageContentFixtures;
import inmemory.PortalNavigationItemsCrudServiceInMemory;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemSource;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.use_case.GetPortalPageContentUseCase;
import io.gravitee.rest.api.management.v2.rest.model.PortalPageContent;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiDocumentationNavigationItemResource_GetContentTest extends AbstractResourceTest {

    private static final String ENVIRONMENT = "environment-id";
    private static final String API_ID = "api-id";
    private static final String OTHER_API_ID = "other-api-id";
    private static final String ORIGINAL_CONTENT = "# Original content";

    @Inject
    private EnvironmentService environmentService;

    @Autowired
    private PortalNavigationItemsCrudServiceInMemory portalNavigationItemCrudService;

    @Inject
    private GetPortalPageContentUseCase getPortalPageContentUseCase;

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

        // The mock is shared with the other test classes of the Spring context, which do not all reset it
        Mockito.reset(getPortalPageContentUseCase);
    }

    @AfterEach
    public void cleanUp() {
        GraviteeContext.cleanContext();
        portalNavigationItemCrudService.reset();
        portalPageContentCrudService.reset();
        portalPageContentQueryService.reset();
        Mockito.reset(getPortalPageContentUseCase);
    }

    @Test
    void should_return_the_content_of_a_page_owned_by_the_api() {
        var page = aPageOwnedBy(API_ID);
        when(getPortalPageContentUseCase.execute(new GetPortalPageContentUseCase.Input(page.getPortalPageContentId()))).thenReturn(
            new GetPortalPageContentUseCase.Output(
                PortalPageContentFixtures.aGraviteeMarkdownPageContent(
                    page.getPortalPageContentId(),
                    ORGANIZATION,
                    ENVIRONMENT,
                    ORIGINAL_CONTENT
                )
            )
        );

        Response response = getContentOf(page.getId());

        assertThat(response)
            .hasStatus(OK_200)
            .asEntity(PortalPageContent.class)
            .satisfies(content -> {
                assertThat(content.getId()).isEqualTo(page.getPortalPageContentId().json());
                assertThat(content.getContent()).isEqualTo(ORIGINAL_CONTENT);
            });
    }

    @Test
    void should_return_404_for_a_page_of_another_api() {
        var foreign = aPageOwnedBy(OTHER_API_ID);

        Response response = getContentOf(foreign.getId());

        assertThat(response).hasStatus(NOT_FOUND_404);
        verifyNoInteractions(getPortalPageContentUseCase);
    }

    @Test
    void should_return_404_for_a_portal_owned_page() {
        var portalPage = aPage(NavigationItemReference.defaultReference(), null);

        Response response = getContentOf(portalPage.getId());

        assertThat(response).hasStatus(NOT_FOUND_404);
        verifyNoInteractions(getPortalPageContentUseCase);
    }

    @Test
    void should_return_404_for_an_unknown_item() {
        Response response = getContentOf(PortalNavigationItemId.random());

        assertThat(response).hasStatus(NOT_FOUND_404);
        verifyNoInteractions(getPortalPageContentUseCase);
    }

    @Test
    void should_return_400_for_a_folder() {
        var folder = aFolderOwnedByTheApi();

        Response response = getContentOf(folder.getId());

        assertThat(response).hasStatus(BAD_REQUEST_400);
        verifyNoInteractions(getPortalPageContentUseCase);
    }

    @Test
    void should_return_403_with_environment_documentation_permission_only() {
        var page = aPageOwnedBy(API_ID);
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.API_DOCUMENTATION),
                eq(API_ID),
                eq(RolePermissionAction.READ)
            )
        ).thenReturn(false);

        Response response = getContentOf(page.getId());

        assertThat(response).hasStatus(FORBIDDEN_403);
        verifyNoInteractions(getPortalPageContentUseCase);
    }

    @Test
    void should_succeed_with_api_documentation_permission_only() {
        var page = aPageOwnedBy(API_ID);
        when(getPortalPageContentUseCase.execute(any())).thenReturn(
            new GetPortalPageContentUseCase.Output(
                PortalPageContentFixtures.aGraviteeMarkdownPageContent(
                    page.getPortalPageContentId(),
                    ORGANIZATION,
                    ENVIRONMENT,
                    ORIGINAL_CONTENT
                )
            )
        );
        when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(false);
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.API_DOCUMENTATION),
                eq(API_ID),
                eq(RolePermissionAction.READ)
            )
        ).thenReturn(true);

        Response response = getContentOf(page.getId());

        assertThat(response).hasStatus(OK_200);
    }

    private Response getContentOf(PortalNavigationItemId id) {
        return rootTarget(id.json()).path("content").request().get();
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
