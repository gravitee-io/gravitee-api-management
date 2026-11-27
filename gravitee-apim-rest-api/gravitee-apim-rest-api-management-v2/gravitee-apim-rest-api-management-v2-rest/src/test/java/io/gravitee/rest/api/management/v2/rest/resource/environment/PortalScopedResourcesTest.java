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
package io.gravitee.rest.api.management.v2.rest.resource.environment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fixtures.core.model.PortalNavigationItemFixtures;
import fixtures.core.model.PortalPageContentFixtures;
import inmemory.PortalNavigationItemSourceDomainServiceInMemory;
import inmemory.PortalNavigationItemsCrudServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.PortalPageContentCrudServiceInMemory;
import inmemory.PortalPageContentQueryServiceInMemory;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.portal.model.Portal;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal.model.PortalNavigationStructure;
import io.gravitee.apim.core.portal_page.crud_service.PortalPageContentCrudService;
import io.gravitee.apim.core.portal_page.domain_service.ApiOwnedNavigationDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationAccessDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationDefaultPageDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemCreationExpansionDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemRenderingDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemSourceDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemValidatorService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationSourcedItemsDomainService;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.OpenApiPageContent;
import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemSource;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.model.PortalPageContent;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.model.RedocConfiguration;
import io.gravitee.apim.core.portal_page.use_case.BulkCreatePortalNavigationItemUseCase;
import io.gravitee.apim.core.portal_page.use_case.CreatePortalNavigationItemUseCase;
import io.gravitee.apim.core.portal_page.use_case.DeletePortalNavigationItemUseCase;
import io.gravitee.apim.core.portal_page.use_case.GetPortalPageContentUseCase;
import io.gravitee.apim.core.portal_page.use_case.SeedDefaultPagesForPortalNavigationItemsUseCase;
import io.gravitee.rest.api.management.v2.rest.model.PortalNavigationItemsResponse;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = PortalScopedResourcesTest.RealPortalOperations.class)
class PortalScopedResourcesTest extends AbstractPortalNavigationResourceTest {

    private static final PortalId OTHER_PORTAL_ID = PortalId.of("22222222-2222-2222-2222-222222222222");
    private static final String CONTENT_ID = "33333333-3333-3333-3333-333333333333";
    private static final String ITEM_ID = "44444444-4444-4444-4444-444444444444";
    private static final String OPENAPI_CONTENT = "openapi: 3.0.3\ninfo:\n  title: Initial\n  version: 1.0.0\npaths: {}";
    private static final String UPDATED_OPENAPI_CONTENT = "openapi: 3.0.3\ninfo:\n  title: Updated\n  version: 1.0.0\npaths: {}";
    private static final Menu DEFAULT_MENU = new Menu(
        PortalId.of(NAVIGATION_PORTAL_ID),
        "aaaaaaaa-1111-1111-1111-111111111111",
        "aaaaaaaa-2222-2222-2222-222222222222",
        "aaaaaaaa-3333-3333-3333-333333333333",
        "aaaaaaaa-4444-4444-4444-444444444444",
        "api-a"
    );
    private static final Menu PARTNERS_MENU = new Menu(
        OTHER_PORTAL_ID,
        "bbbbbbbb-1111-1111-1111-111111111111",
        "bbbbbbbb-2222-2222-2222-222222222222",
        "bbbbbbbb-3333-3333-3333-333333333333",
        "bbbbbbbb-4444-4444-4444-444444444444",
        "api-b"
    );

    @Inject
    private PortalNavigationItemsQueryServiceInMemory navigationQueryService;

    @Inject
    private PortalNavigationItemsCrudServiceInMemory navigationCrudService;

    @Inject
    private PortalNavigationItemSourceDomainServiceInMemory sourceService;

    @Inject
    private ObjectMapper objectMapper;

    @Override
    protected String contextPath() {
        return "/environments/" + NAVIGATION_ENVIRONMENT;
    }

    @BeforeEach
    @SneakyThrows
    void initializeEnvironment() {
        navigationCrudService.reset();
        portalPageContentCrudService.reset();
        apiCrudService.reset();
        sourceService.resetFileListing();
        clearInvocations(sourceService);
        var environment = EnvironmentEntity.builder().id(NAVIGATION_ENVIRONMENT).organizationId(ORGANIZATION).build();
        when(environmentService.findByOrgAndIdOrHrid(ORGANIZATION, NAVIGATION_ENVIRONMENT)).thenReturn(environment);
        GraviteeContext.setCurrentEnvironment(NAVIGATION_ENVIRONMENT);
        GraviteeContext.setCurrentOrganization(ORGANIZATION);
        portalCrudService.create(
            Portal.of(
                OTHER_PORTAL_ID,
                NAVIGATION_ENVIRONMENT,
                ORGANIZATION,
                "Partners",
                PortalNavigationStructure.empty(),
                null,
                false,
                "partners-access-point"
            )
        );
    }

    @AfterEach
    void clearContent() {
        navigationQueryService.reset();
        navigationCrudService.reset();
        portalPageContentQueryService.reset();
        portalPageContentCrudService.reset();
        apiCrudService.reset();
        sourceService.resetFileListing();
        clearInvocations(sourceService);
        GraviteeContext.cleanContext();
    }

    @Test
    void should_list_only_the_selected_portal() {
        var defaultFolder = folder(NAVIGATION_PORTAL_ID, "Default folder");
        var partnersFolder = folder(OTHER_PORTAL_ID.toString(), "Partners folder");
        navigationQueryService.initWith(List.of(defaultFolder, partnersFolder));

        try (var response = rootTarget("portals/" + OTHER_PORTAL_ID + "/navigation-items").request().get()) {
            assertThat(response.getStatus()).isEqualTo(200);
            var body = response.readEntity(PortalNavigationItemsResponse.class);
            assertThat(body.getItems()).hasSize(1);
            assertThat(body.getItems().getFirst().getActualInstance().toString()).contains("Partners folder");
        }
    }

    @Test
    void should_delegate_the_legacy_list_to_the_default_portal() {
        navigationQueryService.initWith(
            List.of(folder(NAVIGATION_PORTAL_ID, "Default folder"), folder(OTHER_PORTAL_ID.toString(), "Partners folder"))
        );

        try (var response = rootTarget("portal-navigation-items").request().get()) {
            assertThat(response.getStatus()).isEqualTo(200);
            var body = response.readEntity(PortalNavigationItemsResponse.class);
            assertThat(body.getItems()).hasSize(1);
            assertThat(body.getItems().getFirst().getActualInstance().toString()).contains("Default folder");
        }
    }

    @Test
    void should_refuse_a_parent_of_another_portal() {
        var foreign = folder(OTHER_PORTAL_ID.toString(), "Foreign folder");
        navigationQueryService.initWith(List.of(foreign));

        try (
            var response = rootTarget("portals/" + NAVIGATION_PORTAL_ID + "/navigation-items")
                .queryParam("parentId", foreign.getId().json())
                .request()
                .get()
        ) {
            assertThat(response.getStatus()).isEqualTo(404);
        }
    }

    @Test
    void should_return_404_for_an_unknown_portal() {
        try (var response = rootTarget("portals/" + UUID.randomUUID() + "/navigation-items").request().get()) {
            assertThat(response.getStatus()).isEqualTo(404);
        }
    }

    @Test
    void should_use_the_only_portal_when_the_default_is_missing() {
        givenTwoPortalMenus();
        var partners = portalCrudService.findByIdAndEnvironmentId(OTHER_PORTAL_ID, NAVIGATION_ENVIRONMENT).orElseThrow();
        portalCrudService.initWith(List.of(partners));
        try (var response = rootTarget("portal-navigation-items").request().get()) {
            assertThat(response.getStatus()).isEqualTo(200);
            var body = response.readEntity(PortalNavigationItemsResponse.class);
            assertThat(body.getItems()).hasSize(2);
            assertThat(body.getItems().toString()).contains("Partners folder").doesNotContain("Default folder");
        }
        assertThat(portalCrudService.findByIdAndEnvironmentId(OTHER_PORTAL_ID, NAVIGATION_ENVIRONMENT).orElseThrow().isDefault()).isFalse();
    }

    @Test
    void should_list_homepage_of_the_only_nondefault_portal() {
        var partners = portalCrudService.findByIdAndEnvironmentId(OTHER_PORTAL_ID, NAVIGATION_ENVIRONMENT).orElseThrow();
        portalCrudService.initWith(List.of(partners));
        var homepage = page(OTHER_PORTAL_ID.toString(), PortalPageContentId.of(CONTENT_ID)).toBuilder().area(PortalArea.HOMEPAGE).build();
        homepage.markAsRoot();
        navigationQueryService.initWith(List.of(homepage));

        try (var response = rootTarget("portal-navigation-items").queryParam("area", "HOMEPAGE").request().get()) {
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.readEntity(PortalNavigationItemsResponse.class).getItems()).hasSize(1);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "CREATE", "BULK", "IMPORT", "UPDATE", "DELETE", "FETCH", "SEED", "GET", "PUT", "PATCH" })
    void should_manage_the_only_nondefault_portal_using_legacy_paths(String operation) {
        givenTwoPortalMenus();
        var partners = portalCrudService.findByIdAndEnvironmentId(OTHER_PORTAL_ID, NAVIGATION_ENVIRONMENT).orElseThrow();
        portalCrudService.initWith(List.of(partners));
        if ("SEED".equals(operation)) {
            givenListings();
        }
        var untouched = snapshot(DEFAULT_MENU);
        var menu = PARTNERS_MENU;
        switch (operation) {
            case "CREATE" -> {
                var result = request("POST", "portal-navigation-items", createFolder("Created", menu.folderId()), 201);
                assertThat(item(result.path("id").asText()).getReference()).isEqualTo(menu.reference());
            }
            case "BULK" -> {
                var result = request(
                    "POST",
                    "portal-navigation-items/_bulk",
                    Map.of("items", List.of(createFolder("Bulk", menu.folderId()))),
                    200
                );
                assertThat(item(result.path("items").get(0).path("id").asText()).getReference()).isEqualTo(menu.reference());
            }
            case "IMPORT" -> {
                sourceService.givenRemoteFile("/guide.md", "# Imported guide");
                var result = request("POST", "portal-navigation-items/_import", importRequest(menu.folderId()), 200);
                assertThat(item(result.path("rootFolder").path("id").asText()).getReference()).isEqualTo(menu.reference());
            }
            case "UPDATE" -> {
                request("PUT", "portal-navigation-items/" + menu.pageId(), updatePage("Updated", menu.folderId()), 200);
                assertThat(item(menu.pageId()).getTitle()).isEqualTo("Updated");
            }
            case "DELETE" -> {
                request("DELETE", "portal-navigation-items/" + menu.pageId(), null, 204);
                assertThat(
                    navigationQueryService.findByIdAndEnvironmentId(NAVIGATION_ENVIRONMENT, PortalNavigationItemId.of(menu.pageId()))
                ).isNull();
            }
            case "FETCH" -> {
                givenSourcedPage(menu);
                portalPageContentCrudService.update(
                    PortalPageContentFixtures.aGraviteeMarkdownPageContent(
                        PortalPageContentId.of(menu.contentId()),
                        ORGANIZATION,
                        NAVIGATION_ENVIRONMENT,
                        "Before fetch"
                    )
                );
                request("POST", "portal-navigation-items/" + menu.pageId() + "/_fetch", null, 200);
                assertThat(((GraviteeMarkdownPageContent) content(menu.contentId())).getContent().value()).isEqualTo(
                    PortalNavigationItemSourceDomainServiceInMemory.MARKDOWN
                );
            }
            case "SEED" -> {
                request("POST", "portal-navigation-items/_default-pages", Map.of("ids", List.of(menu.listingId())), 204);
                assertThat(
                    navigationQueryService.findTopLevelItemsByEnvironmentIdAndPortalAreaAndReference(
                        NAVIGATION_ENVIRONMENT,
                        PortalArea.TOP_NAVBAR,
                        new NavigationItemReference.ApiReference(menu.apiId())
                    )
                ).hasSize(1);
            }
            case "GET", "PUT", "PATCH" -> {
                var result = request(operation, contentPath(true, menu.contentId(), operation), contentBody(operation), 200);
                assertThat(result.path("id").asText()).isEqualTo(menu.contentId());
                var stored = (OpenApiPageContent) content(menu.contentId());
                assertThat(stored.getContent().value()).isEqualTo("PUT".equals(operation) ? UPDATED_OPENAPI_CONTENT : OPENAPI_CONTENT);
                if ("PATCH".equals(operation)) {
                    assertThat(((RedocConfiguration) stored.getViewerSettings()).tryItUrl()).isEqualTo("https://changed.example.com");
                }
            }
            default -> throw new IllegalArgumentException(operation);
        }
        assertThat(snapshot(DEFAULT_MENU)).isEqualTo(untouched);
        assertThat(portalCrudService.findByIdAndEnvironmentId(OTHER_PORTAL_ID, NAVIGATION_ENVIRONMENT).orElseThrow().isDefault()).isFalse();
    }

    @Test
    void should_reject_legacy_requests_when_no_default_and_multiple_portals_exist() throws Exception {
        givenTwoPortalMenus();
        var partners = portalCrudService.findByIdAndEnvironmentId(OTHER_PORTAL_ID, NAVIGATION_ENVIRONMENT).orElseThrow();
        var other = Portal.of(
            PortalId.of(UUID.randomUUID().toString()),
            NAVIGATION_ENVIRONMENT,
            ORGANIZATION,
            "Other",
            PortalNavigationStructure.empty(),
            null,
            false,
            "other-access-point"
        );
        portalCrudService.initWith(List.of(partners, other));
        request("GET", "portal-navigation-items", null, 404);
        request("POST", "portal-navigation-items", createFolder("No target", null), 404);
        var response = request("GET", "portal-page-contents/" + PARTNERS_MENU.contentId(), null, 404);
        assertThat(response.path("message").asText()).isEqualTo(
            "Default portal of environment [ " + NAVIGATION_ENVIRONMENT + " ] not found"
        );
    }

    @Test
    void should_reject_legacy_requests_when_no_portal_exists() {
        givenTwoPortalMenus();
        portalCrudService.initWith(List.of());
        request("GET", "portal-navigation-items", null, 404);
        var response = request("GET", "portal-page-contents/" + PARTNERS_MENU.contentId(), null, 404);
        assertThat(response.path("message").asText()).isEqualTo(
            "Default portal of environment [ " + NAVIGATION_ENVIRONMENT + " ] not found"
        );
    }

    @Test
    void should_reject_legacy_requests_when_multiple_defaults_exist() throws Exception {
        var current = portalCrudService.findByIdAndEnvironmentId(PortalId.of(NAVIGATION_PORTAL_ID), NAVIGATION_ENVIRONMENT).orElseThrow();
        var other = Portal.of(
            PortalId.of(UUID.randomUUID().toString()),
            NAVIGATION_ENVIRONMENT,
            ORGANIZATION,
            "Other",
            PortalNavigationStructure.empty(),
            null,
            true,
            "other-access-point"
        );
        portalCrudService.initWith(List.of(current, other));
        request("GET", "portal-navigation-items", null, 404);
    }

    @ParameterizedTest
    @MethodSource("operations")
    void should_enforce_legacy_permissions_for_the_only_nondefault_portal(
        String method,
        String path,
        RolePermissionAction action,
        Object body
    ) {
        var partners = portalCrudService.findByIdAndEnvironmentId(OTHER_PORTAL_ID, NAVIGATION_ENVIRONMENT).orElseThrow();
        portalCrudService.initWith(List.of(partners));
        when(
            permissionService.hasPermission(
                GraviteeContext.getExecutionContext(),
                RolePermission.ENVIRONMENT_DOCUMENTATION,
                NAVIGATION_ENVIRONMENT,
                action
            )
        ).thenReturn(false);
        var legacyPath = "portal-" + path;
        request(method, legacyPath, body, 403);
    }

    @ParameterizedTest
    @ValueSource(strings = { "GET", "PUT", "PATCH" })
    void should_refuse_foreign_content_with_only_one_nondefault_portal(String method) {
        givenTwoPortalMenus();
        var partners = portalCrudService.findByIdAndEnvironmentId(OTHER_PORTAL_ID, NAVIGATION_ENVIRONMENT).orElseThrow();
        portalCrudService.initWith(List.of(partners));
        var before = snapshot();
        request(method, contentPath(true, DEFAULT_MENU.contentId(), method), contentBody(method), 404);
        assertThat(snapshot()).isEqualTo(before);
    }

    @Test
    void should_refuse_configuration_of_another_portals_page_without_writing() {
        var contentId = PortalPageContentId.of(CONTENT_ID);
        var content = PortalPageContentFixtures.anOpenApiPageContent(
            contentId,
            ORGANIZATION,
            NAVIGATION_ENVIRONMENT,
            "openapi: 3.0.3",
            new RedocConfiguration()
        );
        portalPageContentQueryService.initWith(List.of(content));
        portalPageContentCrudService.initWith(List.of(content));
        navigationQueryService.initWith(List.of(page(OTHER_PORTAL_ID.toString(), contentId)));

        try (
            var response = rootTarget("portals/" + NAVIGATION_PORTAL_ID + "/page-contents/" + CONTENT_ID + "/configuration")
                .request()
                .method("PATCH", Entity.json(Map.of("viewer", "REDOC", "tryItURL", "https://changed.example.com")))
        ) {
            assertThat(response.getStatus()).isEqualTo(404);
            assertThat(response.readEntity(String.class)).doesNotContain("openapi: 3.0.3");
        }
        var stored = (OpenApiPageContent) portalPageContentCrudService.storage().getFirst();
        assertThat(stored.getViewerSettings()).isEqualTo(new RedocConfiguration());
        assertThat(stored.getContent().value()).isEqualTo("openapi: 3.0.3");
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void should_create_in_the_nondefault_portal_or_the_legacy_default(boolean legacy) {
        givenTwoPortalMenus();
        var selected = selectedMenu(legacy);
        var untouched = snapshot(otherMenu(legacy));

        var response = request("POST", navigationPath(legacy), createFolder("Created", selected.folderId()), 201);

        var created = item(response.path("id").asText());
        assertThat(created.getReference()).isEqualTo(selected.reference());
        assertThat(created.getParentId()).isEqualTo(PortalNavigationItemId.of(selected.folderId()));
        assertThat(snapshot(otherMenu(legacy))).isEqualTo(untouched);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void should_bulk_create_a_linked_tree_in_the_selected_portal(boolean legacy) {
        givenTwoPortalMenus();
        var selected = selectedMenu(legacy);
        var untouched = snapshot(otherMenu(legacy));
        var parentId = "cccccccc-1111-1111-1111-111111111111";
        var childId = "cccccccc-2222-2222-2222-222222222222";
        var parent = createFolder("Bulk folder", selected.folderId());
        parent.put("id", parentId);
        var child = createFolder("Bulk child", parentId);
        child.put("id", childId);

        var response = request("POST", navigationPath(legacy) + "/_bulk", Map.of("items", List.of(parent, child)), 200);

        assertThat(response.path("items")).hasSize(2);
        assertThat(item(parentId).getReference()).isEqualTo(selected.reference());
        assertThat(item(childId).getReference()).isEqualTo(selected.reference());
        assertThat(item(childId).getParentId()).isEqualTo(PortalNavigationItemId.of(parentId));
        assertThat(snapshot(otherMenu(legacy))).isEqualTo(untouched);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void should_import_into_the_selected_portal_without_changing_the_other_portal(boolean legacy) {
        givenTwoPortalMenus();
        var selected = selectedMenu(legacy);
        var untouched = snapshot(otherMenu(legacy));
        sourceService.givenRemoteFile("/guide.md", "# Imported guide");

        var response = request("POST", navigationPath(legacy) + "/_import", importRequest(selected.folderId()), 200);

        var imported = item(response.path("rootFolder").path("id").asText());
        assertThat(imported.getReference()).isEqualTo(selected.reference());
        assertThat(imported.getParentId()).isEqualTo(PortalNavigationItemId.of(selected.folderId()));
        var child = navigationQueryService.findByParentIdAndEnvironmentId(NAVIGATION_ENVIRONMENT, imported.getId()).getFirst();
        assertThat(child.getReference()).isEqualTo(selected.reference());
        var contentId = ((PortalNavigationPage) child).getPortalPageContentId();
        assertThat(((GraviteeMarkdownPageContent) content(contentId.json())).getContent().value()).isEqualTo("# Imported guide");
        assertThat(response.path("summary").path("succeeded").asInt()).isEqualTo(1);
        assertThat(snapshot(otherMenu(legacy))).isEqualTo(untouched);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void should_update_a_page_in_the_selected_portal(boolean legacy) {
        givenTwoPortalMenus();
        var selected = selectedMenu(legacy);
        var untouched = snapshot(otherMenu(legacy));

        request("PUT", navigationPath(legacy) + "/" + selected.pageId(), updatePage("Updated page", selected.folderId()), 200);

        assertThat(item(selected.pageId()).getTitle()).isEqualTo("Updated page");
        assertThat(item(selected.pageId()).getReference()).isEqualTo(selected.reference());
        assertThat(item(selected.pageId()).getParentId()).isEqualTo(PortalNavigationItemId.of(selected.folderId()));
        assertThat(snapshot(otherMenu(legacy))).isEqualTo(untouched);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void should_delete_the_selected_page_and_content_only(boolean legacy) {
        givenTwoPortalMenus();
        var selected = selectedMenu(legacy);
        var untouched = snapshot(otherMenu(legacy));

        request("DELETE", navigationPath(legacy) + "/" + selected.pageId(), null, 204);

        assertThat(
            navigationQueryService.findByIdAndEnvironmentId(NAVIGATION_ENVIRONMENT, PortalNavigationItemId.of(selected.pageId()))
        ).isNull();
        assertThat(portalPageContentQueryService.findById(PortalPageContentId.of(selected.contentId()))).isEmpty();
        assertThat(item(selected.folderId())).isNotNull();
        assertThat(snapshot(otherMenu(legacy))).isEqualTo(untouched);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void should_fetch_only_the_selected_portals_page(boolean legacy) {
        givenTwoPortalMenus();
        var selected = selectedMenu(legacy);
        var untouched = snapshot(otherMenu(legacy));
        givenSourcedPage(selected);
        portalPageContentCrudService.update(
            PortalPageContentFixtures.aGraviteeMarkdownPageContent(
                PortalPageContentId.of(selected.contentId()),
                ORGANIZATION,
                NAVIGATION_ENVIRONMENT,
                "Before fetch"
            )
        );

        request("POST", navigationPath(legacy) + "/" + selected.pageId() + "/_fetch", null, 200);

        assertThat(((GraviteeMarkdownPageContent) content(selected.contentId())).getContent().value()).isEqualTo(
            PortalNavigationItemSourceDomainServiceInMemory.MARKDOWN
        );
        assertThat(item(selected.pageId()).getReference()).isEqualTo(selected.reference());
        assertThat(item(selected.pageId()).getSource().getLastFetchedAt()).isNotNull();
        assertThat(snapshot(otherMenu(legacy))).isEqualTo(untouched);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void should_seed_the_selected_listing_with_canonical_api_documentation(boolean legacy) {
        givenTwoPortalMenus();
        givenListings();
        var selected = selectedMenu(legacy);
        var untouched = snapshot(otherMenu(legacy));

        request("POST", navigationPath(legacy) + "/_default-pages", Map.of("ids", List.of(selected.listingId())), 204);

        var docs = navigationQueryService.findTopLevelItemsByEnvironmentIdAndPortalAreaAndReference(
            NAVIGATION_ENVIRONMENT,
            PortalArea.TOP_NAVBAR,
            new NavigationItemReference.ApiReference(selected.apiId())
        );
        assertThat(docs).hasSize(1);
        var overview = (PortalNavigationPage) docs.getFirst();
        assertThat(overview.getTitle()).isEqualTo("Overview");
        assertThat(overview.getParentId()).isNull();
        assertThat(overview.getRootId()).isEqualTo(overview.getId());
        assertThat(content(overview.getPortalPageContentId().json())).isNotNull();
        assertThat(item(selected.listingId()).getReference()).isEqualTo(selected.reference());
        assertThat(snapshot(otherMenu(legacy))).isEqualTo(untouched);
    }

    @ParameterizedTest
    @MethodSource("contentOperations")
    void should_read_or_update_content_of_the_selected_portal(String method, boolean legacy) {
        givenTwoPortalMenus();
        var selected = selectedMenu(legacy);
        var untouched = snapshot(otherMenu(legacy));
        var response = request(method, contentPath(legacy, selected.contentId(), method), contentBody(method), 200);
        var stored = (OpenApiPageContent) content(selected.contentId());

        assertThat(response.path("id").asText()).isEqualTo(selected.contentId());
        assertThat(stored.getContent().value()).isEqualTo("PUT".equals(method) ? UPDATED_OPENAPI_CONTENT : OPENAPI_CONTENT);
        if ("PATCH".equals(method)) {
            assertThat(((RedocConfiguration) stored.getViewerSettings()).tryItUrl()).isEqualTo("https://changed.example.com");
        } else {
            assertThat(stored.getViewerSettings()).isEqualTo(new RedocConfiguration());
        }
        if ("GET".equals(method)) {
            assertThat(response.path("content").asText()).isEqualTo(OPENAPI_CONTENT);
        }
        assertThat(snapshot(otherMenu(legacy))).isEqualTo(untouched);
    }

    @ParameterizedTest
    @MethodSource("contentOperations")
    void should_refuse_another_portals_content_in_the_same_organization_and_environment(String method, boolean legacy) {
        givenTwoPortalMenus();
        var foreign = otherMenu(legacy);
        var before = snapshot();

        var response = request(method, contentPath(legacy, foreign.contentId(), method), contentBody(method), 404);

        assertThat(response.toString()).doesNotContain("openapi: 3.0.3");
        assertThat(snapshot()).isEqualTo(before);
        verifyNoInteractions(sourceService);
    }

    @ParameterizedTest
    @MethodSource("foreignTargetOperations")
    void should_refuse_a_navigation_target_of_the_other_portal(String method, boolean legacy) {
        givenTwoPortalMenus();
        var foreign = otherMenu(legacy);
        givenSourcedPage(foreign);
        var before = snapshot();
        var suffix = "POST".equals(method) ? "/_fetch" : "";

        request(
            method,
            navigationPath(legacy) + "/" + foreign.pageId() + suffix,
            "PUT".equals(method) ? updatePage("Forbidden update", null) : null,
            404
        );

        assertThat(snapshot()).isEqualTo(before);
        verifyNoInteractions(sourceService);
    }

    @ParameterizedTest
    @MethodSource("foreignParentOperations")
    void should_refuse_a_foreign_parent_before_any_creation_or_provider_call(String operation, boolean legacy) {
        givenTwoPortalMenus();
        var selected = selectedMenu(legacy);
        var foreign = otherMenu(legacy);
        sourceService.givenRemoteFile("/guide.md", "# Imported guide");
        clearInvocations(sourceService);
        var before = snapshot();
        switch (operation) {
            case "CREATE" -> request("POST", navigationPath(legacy), createFolder("Forbidden", foreign.folderId()), 404);
            case "BULK" -> {
                var firstPage = createFolder("Valid first", selected.folderId());
                firstPage.put("type", "PAGE");
                request(
                    "POST",
                    navigationPath(legacy) + "/_bulk",
                    Map.of("items", List.of(firstPage, createFolder("Forbidden second", foreign.folderId()))),
                    404
                );
            }
            case "IMPORT" -> request("POST", navigationPath(legacy) + "/_import", importRequest(foreign.folderId()), 404);
            case "PUT" -> request(
                "PUT",
                navigationPath(legacy) + "/" + selected.pageId(),
                updatePage("Forbidden move", foreign.folderId()),
                404
            );
            default -> throw new IllegalArgumentException(operation);
        }

        assertThat(snapshot()).isEqualTo(before);
        verifyNoInteractions(sourceService);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void should_validate_all_seed_ids_before_seeding_the_first_listing(boolean legacy) {
        givenTwoPortalMenus();
        givenListings();
        var before = snapshot();

        request(
            "POST",
            navigationPath(legacy) + "/_default-pages",
            Map.of("ids", List.of(selectedMenu(legacy).listingId(), otherMenu(legacy).listingId())),
            404
        );

        assertThat(snapshot()).isEqualTo(before);
        verifyNoInteractions(sourceService);
    }

    @ParameterizedTest
    @MethodSource("operations")
    void should_enforce_the_existing_permission_for_every_scoped_operation(
        String method,
        String path,
        RolePermissionAction action,
        Object body
    ) {
        when(
            permissionService.hasPermission(
                GraviteeContext.getExecutionContext(),
                RolePermission.ENVIRONMENT_DOCUMENTATION,
                NAVIGATION_ENVIRONMENT,
                action
            )
        ).thenReturn(false);
        var request = rootTarget("portals/" + NAVIGATION_PORTAL_ID + "/" + path).request();
        try (var response = body == null ? request.method(method) : request.method(method, Entity.json(body))) {
            assertThat(response.getStatus()).isEqualTo(403);
        }
    }

    static Stream<Arguments> operations() {
        var folder = Map.of("title", "Folder", "type", "FOLDER", "area", "TOP_NAVBAR", "visibility", "PUBLIC");
        var update = Map.of("title", "Folder", "type", "FOLDER", "order", 0, "published", false, "visibility", "PUBLIC");
        return Stream.of(
            Arguments.of("GET", "navigation-items", RolePermissionAction.READ, null),
            Arguments.of("POST", "navigation-items", RolePermissionAction.UPDATE, folder),
            Arguments.of("POST", "navigation-items/_bulk", RolePermissionAction.UPDATE, Map.of("items", List.of(folder))),
            Arguments.of(
                "POST",
                "navigation-items/_import",
                RolePermissionAction.UPDATE,
                Map.of("title", "Docs", "source", Map.of("type", "git-fetcher", "configuration", "{}"))
            ),
            Arguments.of("POST", "navigation-items/_default-pages", RolePermissionAction.CREATE, Map.of("ids", List.of(ITEM_ID))),
            Arguments.of("PUT", "navigation-items/" + ITEM_ID, RolePermissionAction.UPDATE, update),
            Arguments.of("DELETE", "navigation-items/" + ITEM_ID, RolePermissionAction.DELETE, null),
            Arguments.of("POST", "navigation-items/" + ITEM_ID + "/_fetch", RolePermissionAction.UPDATE, null),
            Arguments.of("GET", "page-contents/" + CONTENT_ID, RolePermissionAction.READ, null),
            Arguments.of("PUT", "page-contents/" + CONTENT_ID, RolePermissionAction.UPDATE, Map.of("content", "Updated")),
            Arguments.of("PATCH", "page-contents/" + CONTENT_ID + "/configuration", RolePermissionAction.UPDATE, Map.of("viewer", "REDOC"))
        );
    }

    static Stream<Arguments> contentOperations() {
        return Stream.of("GET", "PUT", "PATCH").flatMap(method -> Stream.of(false, true).map(legacy -> Arguments.of(method, legacy)));
    }

    static Stream<Arguments> foreignTargetOperations() {
        return Stream.of("PUT", "DELETE", "POST").flatMap(method -> Stream.of(false, true).map(legacy -> Arguments.of(method, legacy)));
    }

    static Stream<Arguments> foreignParentOperations() {
        return Stream.of("CREATE", "BULK", "IMPORT", "PUT").flatMap(operation ->
            Stream.of(false, true).map(legacy -> Arguments.of(operation, legacy))
        );
    }

    private void givenTwoPortalMenus() {
        var defaultFolder = folder(NAVIGATION_PORTAL_ID, "Default folder")
            .toBuilder()
            .id(PortalNavigationItemId.of(DEFAULT_MENU.folderId()))
            .build();
        var partnersFolder = folder(OTHER_PORTAL_ID.toString(), "Partners folder")
            .toBuilder()
            .id(PortalNavigationItemId.of(PARTNERS_MENU.folderId()))
            .build();
        defaultFolder.markAsRoot();
        partnersFolder.markAsRoot();
        var defaultPage = page(NAVIGATION_PORTAL_ID, PortalPageContentId.of(DEFAULT_MENU.contentId()))
            .toBuilder()
            .id(PortalNavigationItemId.of(DEFAULT_MENU.pageId()))
            .title("Default page")
            .order(1)
            .build();
        var partnersPage = page(OTHER_PORTAL_ID.toString(), PortalPageContentId.of(PARTNERS_MENU.contentId()))
            .toBuilder()
            .id(PortalNavigationItemId.of(PARTNERS_MENU.pageId()))
            .title("Partners page")
            .order(1)
            .build();
        defaultPage.markAsRoot();
        partnersPage.markAsRoot();
        navigationCrudService.initWith(List.of(defaultFolder, defaultPage, partnersFolder, partnersPage));
        portalPageContentCrudService.initWith(
            List.of(
                PortalPageContentFixtures.anOpenApiPageContent(
                    PortalPageContentId.of(DEFAULT_MENU.contentId()),
                    ORGANIZATION,
                    NAVIGATION_ENVIRONMENT,
                    OPENAPI_CONTENT,
                    new RedocConfiguration()
                ),
                PortalPageContentFixtures.anOpenApiPageContent(
                    PortalPageContentId.of(PARTNERS_MENU.contentId()),
                    ORGANIZATION,
                    NAVIGATION_ENVIRONMENT,
                    OPENAPI_CONTENT,
                    new RedocConfiguration()
                )
            )
        );
        apiCrudService.initWith(
            List.of(
                Api.builder().id(DEFAULT_MENU.apiId()).environmentId(NAVIGATION_ENVIRONMENT).name("Default API").build(),
                Api.builder().id(PARTNERS_MENU.apiId()).environmentId(NAVIGATION_ENVIRONMENT).name("Partners API").build()
            )
        );
    }

    private void givenListings() {
        for (var menu : List.of(DEFAULT_MENU, PARTNERS_MENU)) {
            var section = (PortalNavigationFolder) item(menu.folderId());
            var listing = PortalNavigationItemFixtures.anApi(menu.listingId(), "API", section.getId(), menu.apiId());
            listing.setOrganizationId(ORGANIZATION);
            listing.setEnvironmentId(NAVIGATION_ENVIRONMENT);
            listing.changeOwner(menu.reference());
            listing.updateParent(section);
            navigationCrudService.create(listing);
        }
    }

    private void givenSourcedPage(Menu menu) {
        var page = (PortalNavigationPage) item(menu.pageId());
        navigationCrudService.update(page.toBuilder().source(externalSource()).build());
    }

    private Menu selectedMenu(boolean legacy) {
        return legacy ? DEFAULT_MENU : PARTNERS_MENU;
    }

    private Menu otherMenu(boolean legacy) {
        return legacy ? PARTNERS_MENU : DEFAULT_MENU;
    }

    private String navigationPath(boolean legacy) {
        return legacy ? "portal-navigation-items" : "portals/" + OTHER_PORTAL_ID + "/navigation-items";
    }

    private String contentPath(boolean legacy, String contentId, String method) {
        var base = legacy ? "portal-page-contents" : "portals/" + OTHER_PORTAL_ID + "/page-contents";
        return base + "/" + contentId + ("PATCH".equals(method) ? "/configuration" : "");
    }

    private Map<String, Object> createFolder(String title, String parentId) {
        var body = new LinkedHashMap<String, Object>();
        body.put("type", "FOLDER");
        body.put("title", title);
        body.put("area", "TOP_NAVBAR");
        body.put("visibility", "PUBLIC");
        if (parentId != null) {
            body.put("parentId", parentId);
        }
        return body;
    }

    private Map<String, Object> updatePage(String title, String parentId) {
        var body = new LinkedHashMap<String, Object>();
        body.put("type", "PAGE");
        body.put("title", title);
        body.put("published", true);
        body.put("visibility", "PUBLIC");
        body.put("order", 0);
        if (parentId != null) {
            body.put("parentId", parentId);
        }
        return body;
    }

    private Map<String, Object> importRequest(String parentId) {
        return Map.of(
            "title",
            "Imported docs",
            "parentId",
            parentId,
            "source",
            Map.of("type", "http-fetcher", "configuration", Map.of("url", "https://example.com/repo"))
        );
    }

    private Object contentBody(String method) {
        return switch (method) {
            case "GET" -> null;
            case "PUT" -> Map.of("content", UPDATED_OPENAPI_CONTENT);
            case "PATCH" -> Map.of("viewer", "REDOC", "tryItURL", "https://changed.example.com");
            default -> throw new IllegalArgumentException(method);
        };
    }

    private PortalNavigationItemSource externalSource() {
        return PortalNavigationItemSource.builder()
            .sourceType("http-fetcher")
            .sourceConfiguration("{\"url\":\"https://example.com/page\"}")
            .build();
    }

    @SneakyThrows
    private JsonNode request(String method, String path, Object body, int expectedStatus) {
        var request = rootTarget(path).request();
        try (var response = body == null ? request.method(method) : request.method(method, Entity.json(body))) {
            var responseBody = response.hasEntity() ? response.readEntity(String.class) : "";
            assertThat(response.getStatus()).as("%s %s: %s", method, path, responseBody).isEqualTo(expectedStatus);
            return responseBody.isBlank() ? objectMapper.createObjectNode() : objectMapper.readTree(responseBody);
        }
    }

    private PortalNavigationItem item(String id) {
        return navigationQueryService.findByIdAndEnvironmentId(NAVIGATION_ENVIRONMENT, PortalNavigationItemId.of(id));
    }

    private PortalPageContent<?> content(String id) {
        return portalPageContentQueryService.findById(PortalPageContentId.of(id)).orElseThrow();
    }

    private StoreSnapshot snapshot() {
        return new StoreSnapshot(
            rows(navigationCrudService.storage()),
            rows(portalPageContentCrudService.storage()),
            rows(apiCrudService.storage())
        );
    }

    private StoreSnapshot snapshot(Menu menu) {
        var navigation = navigationCrudService
            .storage()
            .stream()
            .filter(item -> menu.reference().equals(item.getReference()))
            .toList();
        var contentIds = navigation
            .stream()
            .filter(PortalNavigationPage.class::isInstance)
            .map(PortalNavigationPage.class::cast)
            .map(PortalNavigationPage::getPortalPageContentId)
            .toList();
        return new StoreSnapshot(
            rows(navigation),
            rows(
                portalPageContentCrudService
                    .storage()
                    .stream()
                    .filter(content -> contentIds.contains(content.getId()))
                    .toList()
            ),
            rows(apiCrudService.storage())
        );
    }

    private List<String> rows(Collection<?> values) {
        return values.stream().map(this::serialized).sorted().toList();
    }

    @SneakyThrows
    private String serialized(Object value) {
        return objectMapper.writeValueAsString(value);
    }

    private record StoreSnapshot(List<String> navigation, List<String> contents, List<String> apis) {}

    private record Menu(PortalId portalId, String folderId, String pageId, String contentId, String listingId, String apiId) {
        NavigationItemReference.PortalReference reference() {
            return new NavigationItemReference.PortalReference(portalId);
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class RealPortalOperations {

        @Bean
        @Primary
        PortalPageContentQueryServiceInMemory scopedContentQueries(PortalPageContentCrudService contents) {
            return PortalPageContentQueryServiceInMemory.sharing(((PortalPageContentCrudServiceInMemory) contents).storage());
        }

        @Bean
        @Primary
        PortalNavigationItemSourceDomainServiceInMemory scopedSourceProvider() {
            return spy(new PortalNavigationItemSourceDomainServiceInMemory());
        }

        @Bean
        @Primary
        CreatePortalNavigationItemUseCase scopedCreate(
            PortalNavigationAccessDomainService access,
            ApiOwnedNavigationDomainService apiNavigation,
            PortalNavigationItemDomainService domain,
            PortalNavigationItemValidatorService validator,
            PortalNavigationItemCreationExpansionDomainService expansion,
            PortalNavigationDefaultPageDomainService defaults,
            PortalNavigationItemSourceDomainService source,
            PortalNavigationItemRenderingDomainService rendering
        ) {
            return new CreatePortalNavigationItemUseCase(access, apiNavigation, domain, validator, expansion, defaults, source, rendering);
        }

        @Bean
        @Primary
        BulkCreatePortalNavigationItemUseCase scopedBulk(
            PortalNavigationAccessDomainService access,
            PortalNavigationItemDomainService domain,
            PortalNavigationItemValidatorService validator,
            PortalNavigationItemCreationExpansionDomainService expansion,
            PortalNavigationDefaultPageDomainService defaults,
            PortalNavigationItemSourceDomainService source,
            PortalNavigationItemRenderingDomainService rendering
        ) {
            return new BulkCreatePortalNavigationItemUseCase(access, domain, validator, expansion, defaults, source, rendering);
        }

        @Bean
        @Primary
        SeedDefaultPagesForPortalNavigationItemsUseCase scopedSeed(
            PortalNavigationAccessDomainService access,
            PortalNavigationDefaultPageDomainService defaults
        ) {
            return new SeedDefaultPagesForPortalNavigationItemsUseCase(access, defaults);
        }

        @Bean
        @Primary
        GetPortalPageContentUseCase scopedGetContent(PortalNavigationAccessDomainService access) {
            return new GetPortalPageContentUseCase(access);
        }

        @Bean
        @Primary
        DeletePortalNavigationItemUseCase scopedDelete(
            PortalNavigationAccessDomainService access,
            PortalNavigationItemDomainService domain,
            PortalNavigationSourcedItemsDomainService sourced
        ) {
            return new DeletePortalNavigationItemUseCase(access, domain, sourced);
        }
    }

    private PortalNavigationFolder folder(String portalId, String title) {
        var folder = PortalNavigationItemFixtures.aFolder(title)
            .toBuilder()
            .organizationId(ORGANIZATION)
            .environmentId(NAVIGATION_ENVIRONMENT)
            .reference(new NavigationItemReference.PortalReference(PortalId.of(portalId)))
            .build();
        folder.markAsRoot();
        return folder;
    }

    private PortalNavigationPage page(String portalId, PortalPageContentId contentId) {
        var page = PortalNavigationItemFixtures.aPage(ITEM_ID, "Page", null)
            .toBuilder()
            .organizationId(ORGANIZATION)
            .environmentId(NAVIGATION_ENVIRONMENT)
            .reference(new NavigationItemReference.PortalReference(PortalId.of(portalId)))
            .portalPageContentId(contentId)
            .build();
        page.markAsRoot();
        return page;
    }
}
