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
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.OpenApiConfiguration;
import io.gravitee.apim.core.portal_page.model.OpenApiPageContent;
import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemSource;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.model.RedocConfiguration;
import io.gravitee.apim.core.portal_page.model.SwaggerUiConfiguration;
import io.gravitee.rest.api.management.v2.rest.model.PortalPageContent;
import io.gravitee.rest.api.management.v2.rest.model.PortalPageRedocConfiguration;
import io.gravitee.rest.api.management.v2.rest.model.PortalPageSwaggerConfiguration;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiDocumentationNavigationItemResource_PatchContentConfigurationTest extends AbstractResourceTest {

    private static final String ENVIRONMENT = "environment-id";
    private static final String API_ID = "api-id";
    private static final String OTHER_API_ID = "other-api-id";
    private static final String SPEC = "openapi: 3.0.3\ninfo:\n  title: Petstore\n  version: 1.0.0\npaths: {}\n";
    private static final RedocConfiguration ORIGINAL_CONFIGURATION = new RedocConfiguration("https://original.example.com");
    private static final Map<String, Object> SWAGGER_CONFIGURATION = Map.ofEntries(
        Map.entry("viewer", "SWAGGER"),
        Map.entry("displayOperationId", true),
        Map.entry("docExpansion", "full"),
        Map.entry("enableFiltering", true),
        Map.entry("maxDisplayedTags", 10),
        Map.entry("showCommonExtensions", true),
        Map.entry("showExtensions", true),
        Map.entry("showURL", true),
        Map.entry("tryIt", true),
        Map.entry("disableSyntaxHighlight", true),
        Map.entry("tryItAnonymous", true),
        Map.entry("tryItURL", "https://api.example.com"),
        Map.entry("usePkce", true),
        Map.entry("entrypointsAsServers", false),
        Map.entry("contextPathAsServerPath", true)
    );

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
    void should_save_a_swagger_configuration_of_a_page_owned_by_the_api() {
        var page = anOpenApiPageOwnedBy(API_ID);

        Response response = configure(page.getId(), SWAGGER_CONFIGURATION);

        assertThat(response)
            .hasStatus(OK_200)
            .asEntity(PortalPageContent.class)
            .satisfies(content -> {
                assertThat(content.getId()).isEqualTo(page.getPortalPageContentId().json());
                assertThat(content.getConfiguration().getActualInstance()).isInstanceOfSatisfying(
                    PortalPageSwaggerConfiguration.class,
                    swagger -> assertThat(swagger.getTryIt()).isTrue()
                );
            });
        assertThat(storedContentOf(page).getViewerSettings()).isEqualTo(
            new SwaggerUiConfiguration(
                true,
                "full",
                true,
                10,
                true,
                true,
                true,
                true,
                true,
                true,
                "https://api.example.com",
                true,
                false,
                true
            )
        );
    }

    @Test
    void should_save_a_redoc_configuration() {
        var page = anOpenApiPageOwnedBy(API_ID);

        Response response = configure(page.getId(), Map.of("viewer", "REDOC", "tryItURL", "https://redoc.example.com"));

        assertThat(response)
            .hasStatus(OK_200)
            .asEntity(PortalPageContent.class)
            .satisfies(content ->
                assertThat(content.getConfiguration().getActualInstance()).isInstanceOfSatisfying(
                    PortalPageRedocConfiguration.class,
                    redoc -> assertThat(redoc.getTryItURL()).isEqualTo("https://redoc.example.com")
                )
            );
        assertThat(storedContentOf(page).getViewerSettings()).isEqualTo(new RedocConfiguration("https://redoc.example.com"));
    }

    @ParameterizedTest
    @ValueSource(
        strings = { "javascript:alert(1)", "ftp://files.example.com", "/petstore/v1", "not a url", "https://api example.com", "https://" }
    )
    void should_return_400_for_a_base_url_that_is_not_http(String tryItUrl) {
        var page = anOpenApiPageOwnedBy(API_ID);

        Response response = configure(page.getId(), Map.of("viewer", "REDOC", "tryItURL", tryItUrl));

        assertThat(response).hasStatus(BAD_REQUEST_400);
        assertThat(storedContentOf(page).getViewerSettings()).isEqualTo(ORIGINAL_CONFIGURATION);
    }

    @Test
    void should_not_touch_the_content() {
        var page = anOpenApiPageOwnedBy(API_ID);

        configure(page.getId(), SWAGGER_CONFIGURATION);

        assertThat(storedContentOf(page).getContent().value()).isEqualTo(SPEC);
    }

    @Test
    void should_save_the_configuration_of_a_page_with_its_own_source() {
        var source = PortalNavigationItemSource.builder()
            .sourceType("http-fetcher")
            .sourceConfiguration("{\"url\":\"https://example.com/openapi.yaml\"}")
            .build();
        var sourcedPage = anOpenApiPage(new NavigationItemReference.ApiReference(API_ID), source);

        Response response = configure(sourcedPage.getId(), SWAGGER_CONFIGURATION);

        assertThat(response).hasStatus(OK_200);
        assertThat(storedContentOf(sourcedPage).getViewerSettings().viewer()).isEqualTo(OpenApiConfiguration.Viewer.SWAGGER);
    }

    @Test
    void should_return_404_for_a_page_of_another_api() {
        var foreign = anOpenApiPageOwnedBy(OTHER_API_ID);

        Response response = configure(foreign.getId(), SWAGGER_CONFIGURATION);

        assertThat(response).hasStatus(NOT_FOUND_404);
        assertThat(storedContentOf(foreign).getViewerSettings()).isEqualTo(ORIGINAL_CONFIGURATION);
    }

    @Test
    void should_return_404_for_a_portal_owned_page() {
        var portalPage = anOpenApiPage(NavigationItemReference.defaultReference(), null);

        Response response = configure(portalPage.getId(), SWAGGER_CONFIGURATION);

        assertThat(response).hasStatus(NOT_FOUND_404);
        assertThat(storedContentOf(portalPage).getViewerSettings()).isEqualTo(ORIGINAL_CONFIGURATION);
    }

    @Test
    void should_return_404_for_a_page_that_is_not_openapi() {
        var content = PortalPageContentFixtures.aGraviteeMarkdownPageContent(
            PortalPageContentId.random(),
            ORGANIZATION,
            ENVIRONMENT,
            "# Guide"
        );
        var markdownPage = aPageWith(content, new NavigationItemReference.ApiReference(API_ID), null);

        Response response = configure(markdownPage.getId(), SWAGGER_CONFIGURATION);

        assertThat(response).hasStatus(NOT_FOUND_404);
    }

    @Test
    void should_return_400_for_a_folder() {
        var folder = aFolderOwnedByTheApi();

        Response response = configure(folder.getId(), SWAGGER_CONFIGURATION);

        assertThat(response).hasStatus(BAD_REQUEST_400);
    }

    @Test
    void should_return_404_when_navigation_item_does_not_exist() {
        Response response = configure(PortalNavigationItemId.random(), SWAGGER_CONFIGURATION);

        assertThat(response).hasStatus(NOT_FOUND_404);
    }

    @Test
    void should_return_403_with_environment_documentation_permission_only() {
        var page = anOpenApiPageOwnedBy(API_ID);
        when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(false);
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.ENVIRONMENT_DOCUMENTATION),
                eq(ENVIRONMENT),
                eq(RolePermissionAction.UPDATE)
            )
        ).thenReturn(true);

        Response response = configure(page.getId(), SWAGGER_CONFIGURATION);

        assertThat(response).hasStatus(FORBIDDEN_403);
        assertThat(storedContentOf(page).getViewerSettings()).isEqualTo(ORIGINAL_CONFIGURATION);
    }

    @Test
    void should_succeed_with_api_documentation_permission_only() {
        var page = anOpenApiPageOwnedBy(API_ID);
        when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(false);
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.API_DOCUMENTATION),
                eq(API_ID),
                eq(RolePermissionAction.UPDATE)
            )
        ).thenReturn(true);

        Response response = configure(page.getId(), SWAGGER_CONFIGURATION);

        assertThat(response).hasStatus(OK_200);
    }

    private Response configure(PortalNavigationItemId id, Map<String, Object> configuration) {
        return rootTarget(id.json()).path("content").path("configuration").request().method("PATCH", Entity.json(configuration));
    }

    private OpenApiPageContent storedContentOf(PortalNavigationPage page) {
        return portalPageContentCrudService
            .storage()
            .stream()
            .filter(content -> content.getId().equals(page.getPortalPageContentId()))
            .findFirst()
            .map(OpenApiPageContent.class::cast)
            .orElseThrow();
    }

    private PortalNavigationPage anOpenApiPageOwnedBy(String apiId) {
        return anOpenApiPage(new NavigationItemReference.ApiReference(apiId), null);
    }

    private PortalNavigationPage anOpenApiPage(NavigationItemReference reference, PortalNavigationItemSource source) {
        var content = PortalPageContentFixtures.anOpenApiPageContent(
            PortalPageContentId.random(),
            ORGANIZATION,
            ENVIRONMENT,
            SPEC,
            ORIGINAL_CONFIGURATION
        );
        return aPageWith(content, reference, source);
    }

    private PortalNavigationPage aPageWith(
        io.gravitee.apim.core.portal_page.model.PortalPageContent<?> content,
        NavigationItemReference reference,
        PortalNavigationItemSource source
    ) {
        portalPageContentCrudService.create(content);
        // In this test context the content query fake does not share the crud storage
        portalPageContentQueryService.storage().add(content);

        var page = PortalNavigationItemFixtures.aPage(PortalNavigationItemId.random().json(), "Reference", null, content.getId())
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
