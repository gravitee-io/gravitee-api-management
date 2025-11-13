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

import static assertions.MAPIAssertions.assertThat;
import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.mockito.Mockito.when;

import fixtures.core.model.PortalNavigationItemFixtures;
import io.gravitee.apim.core.portal_page.model.PortalArea;
import io.gravitee.apim.core.portal_page.use_case.ListPortalNavigationItemsUseCase;
import io.gravitee.rest.api.management.v2.rest.model.PortalNavigationItemsResponse;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalNavigationItemResourceTest extends AbstractResourceTest {

    private static final String ENVIRONMENT = "environment-id";

    @Inject
    private ListPortalNavigationItemsUseCase listPortalNavigationItemsUseCase;

    @Inject
    private EnvironmentService environmentService;

    private WebTarget target;

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/portal-navigation-items";
    }

    @BeforeEach
    public void setUp() {
        target = rootTarget();

        EnvironmentEntity environmentEntity = EnvironmentEntity.builder().id(ENVIRONMENT).organizationId(ORGANIZATION).build();
        when(environmentService.findById(ENVIRONMENT)).thenReturn(environmentEntity);
        when(environmentService.findByOrgAndIdOrHrid(ORGANIZATION, ENVIRONMENT)).thenReturn(environmentEntity);

        GraviteeContext.setCurrentEnvironment(ENVIRONMENT);
        GraviteeContext.setCurrentOrganization(ORGANIZATION);
    }

    @AfterEach
    public void tearDown() {
        GraviteeContext.cleanContext();
    }

    @Test
    void should_return_portal_navigation_items() {
        // Given
        var items = PortalNavigationItemFixtures.sampleNavigationItems();

        ArgumentCaptor<ListPortalNavigationItemsUseCase.Input> inputCaptor = ArgumentCaptor.forClass(
            ListPortalNavigationItemsUseCase.Input.class
        );
        when(listPortalNavigationItemsUseCase.execute(inputCaptor.capture())).thenReturn(
            new ListPortalNavigationItemsUseCase.Output(items)
        );

        when(
            permissionService.hasPermission(
                GraviteeContext.getExecutionContext(),
                RolePermission.ENVIRONMENT_DOCUMENTATION,
                ENVIRONMENT,
                RolePermissionAction.READ
            )
        ).thenReturn(true);

        // When
        Response response = target.queryParam("area", PortalArea.TOP_NAVBAR).queryParam("loadChildren", true).request().get();

        // Then
        assertThat(response)
            .hasStatus(OK_200)
            .asEntity(PortalNavigationItemsResponse.class)
            .satisfies(entity -> {
                // After mapping the returned structure should be hierarchical: only top-level items are returned
                assertThat(entity.getItems()).hasSize(3);

                // Top-level ids should be folder 0001, folder 0002 and page 0003
                assertThat(
                    entity
                        .getItems()
                        .stream()
                        .map(i -> (io.gravitee.rest.api.management.v2.rest.model.BasePortalNavigationItem) i.getActualInstance())
                        .map(io.gravitee.rest.api.management.v2.rest.model.BasePortalNavigationItem::getId)
                ).containsExactlyInAnyOrder(
                    "00000000-0000-0000-0000-000000000001",
                    "00000000-0000-0000-0000-000000000002",
                    "00000000-0000-0000-0000-000000000003"
                );

                // Verify APIs folder children
                var apisFolder = entity
                    .getItems()
                    .stream()
                    .filter(i ->
                        ((io.gravitee.rest.api.management.v2.rest.model.BasePortalNavigationItem) i.getActualInstance()).getId().equals(
                            "00000000-0000-0000-0000-000000000001"
                        )
                    )
                    .map(i -> (io.gravitee.rest.api.management.v2.rest.model.PortalNavigationFolder) i.getActualInstance())
                    .findFirst()
                    .orElseThrow();

                assertThat(apisFolder.getChildren()).hasSize(3);
                assertThat(
                    apisFolder
                        .getChildren()
                        .stream()
                        .map(i -> (io.gravitee.rest.api.management.v2.rest.model.BasePortalNavigationItem) i.getActualInstance())
                        .map(io.gravitee.rest.api.management.v2.rest.model.BasePortalNavigationItem::getId)
                ).containsExactlyInAnyOrder(
                    "00000000-0000-0000-0000-000000000004",
                    "00000000-0000-0000-0000-000000000005",
                    "00000000-0000-0000-0000-000000000006"
                );

                // Verify category folder 0006 has its two pages
                var categoryFolder = apisFolder
                    .getChildren()
                    .stream()
                    .filter(i ->
                        ((io.gravitee.rest.api.management.v2.rest.model.BasePortalNavigationItem) i.getActualInstance()).getId().equals(
                            "00000000-0000-0000-0000-000000000006"
                        )
                    )
                    .map(i -> (io.gravitee.rest.api.management.v2.rest.model.PortalNavigationFolder) i.getActualInstance())
                    .findFirst()
                    .orElseThrow();

                assertThat(categoryFolder.getChildren()).hasSize(2);
                assertThat(
                    categoryFolder
                        .getChildren()
                        .stream()
                        .map(i -> (io.gravitee.rest.api.management.v2.rest.model.BasePortalNavigationItem) i.getActualInstance())
                        .map(io.gravitee.rest.api.management.v2.rest.model.BasePortalNavigationItem::getId)
                ).containsExactlyInAnyOrder("00000000-0000-0000-0000-000000000007", "00000000-0000-0000-0000-000000000008");

                // Verify folder 0002 (Guides) exists and has no children
                var guidesFolder = entity
                    .getItems()
                    .stream()
                    .filter(i ->
                        ((io.gravitee.rest.api.management.v2.rest.model.BasePortalNavigationItem) i.getActualInstance()).getId().equals(
                            "00000000-0000-0000-0000-000000000002"
                        )
                    )
                    .map(i -> (io.gravitee.rest.api.management.v2.rest.model.PortalNavigationFolder) i.getActualInstance())
                    .findFirst()
                    .orElseThrow();

                assertThat(guidesFolder.getChildren()).isEmpty();

                // Verify Support page 0003 is present as top-level page
                var supportPage = entity
                    .getItems()
                    .stream()
                    .filter(i ->
                        ((io.gravitee.rest.api.management.v2.rest.model.BasePortalNavigationItem) i.getActualInstance()).getId().equals(
                            "00000000-0000-0000-0000-000000000003"
                        )
                    )
                    .map(i -> (io.gravitee.rest.api.management.v2.rest.model.PortalNavigationPage) i.getActualInstance())
                    .findFirst()
                    .orElseThrow();

                assertThat(supportPage.getConfiguration()).isNotNull();
                assertThat(supportPage.getConfiguration().getPortalPageContentId()).isNotNull();
            });

        var capturedInput = inputCaptor.getValue();
        assertThat(capturedInput.environmentId()).isEqualTo(ENVIRONMENT);
        assertThat(capturedInput.portalArea()).isEqualTo(PortalArea.TOP_NAVBAR);
        assertThat(capturedInput.parentId()).isEmpty();
        assertThat(capturedInput.loadChildren()).isTrue();
    }

    @Test
    void should_return_portal_navigation_items_with_parent_id() {
        // Given
        var items = PortalNavigationItemFixtures.sampleNavigationItems()
            .stream()
            .filter(
                item ->
                    item.getId().id().toString().equals("00000000-0000-0000-0000-000000000004") ||
                    item.getId().id().toString().equals("00000000-0000-0000-0000-000000000005") ||
                    item.getId().id().toString().equals("00000000-0000-0000-0000-000000000006")
            )
            .toList();

        ArgumentCaptor<ListPortalNavigationItemsUseCase.Input> inputCaptor = ArgumentCaptor.forClass(
            ListPortalNavigationItemsUseCase.Input.class
        );
        when(listPortalNavigationItemsUseCase.execute(inputCaptor.capture())).thenReturn(
            new ListPortalNavigationItemsUseCase.Output(items)
        );

        when(
            permissionService.hasPermission(
                GraviteeContext.getExecutionContext(),
                RolePermission.ENVIRONMENT_DOCUMENTATION,
                ENVIRONMENT,
                RolePermissionAction.READ
            )
        ).thenReturn(true);

        // When
        Response response = target
            .queryParam("area", PortalArea.TOP_NAVBAR)
            .queryParam("parentId", "00000000-0000-0000-0000-000000000001")
            .queryParam("loadChildren", false)
            .request()
            .get();

        // Then
        assertThat(response)
            .hasStatus(OK_200)
            .asEntity(PortalNavigationItemsResponse.class)
            .satisfies(entity -> assertThat(entity.getItems()).hasSize(3));

        var capturedInput = inputCaptor.getValue();
        assertThat(capturedInput.environmentId()).isEqualTo(ENVIRONMENT);
        assertThat(capturedInput.portalArea()).isEqualTo(PortalArea.TOP_NAVBAR);
        assertThat(capturedInput.parentId()).isPresent();
        assertThat(capturedInput.parentId().get().id().toString()).isEqualTo("00000000-0000-0000-0000-000000000001");
        assertThat(capturedInput.loadChildren()).isFalse();
    }

    @Test
    void should_return_portal_navigation_items_without_loading_children() {
        // Given
        var items = PortalNavigationItemFixtures.sampleNavigationItems()
            .stream()
            .filter(item -> item.getParentId() == null) // Assuming top-level items
            .toList();

        ArgumentCaptor<ListPortalNavigationItemsUseCase.Input> inputCaptor = ArgumentCaptor.forClass(
            ListPortalNavigationItemsUseCase.Input.class
        );
        when(listPortalNavigationItemsUseCase.execute(inputCaptor.capture())).thenReturn(
            new ListPortalNavigationItemsUseCase.Output(items)
        );

        when(
            permissionService.hasPermission(
                GraviteeContext.getExecutionContext(),
                RolePermission.ENVIRONMENT_DOCUMENTATION,
                ENVIRONMENT,
                RolePermissionAction.READ
            )
        ).thenReturn(true);

        // When
        Response response = target.queryParam("area", PortalArea.TOP_NAVBAR).queryParam("loadChildren", false).request().get();

        // Then
        assertThat(response)
            .hasStatus(OK_200)
            .asEntity(PortalNavigationItemsResponse.class)
            .satisfies(entity -> assertThat(entity.getItems()).hasSize(items.size()));

        var capturedInput = inputCaptor.getValue();
        assertThat(capturedInput.environmentId()).isEqualTo(ENVIRONMENT);
        assertThat(capturedInput.portalArea()).isEqualTo(PortalArea.TOP_NAVBAR);
        assertThat(capturedInput.parentId()).isEmpty();
        assertThat(capturedInput.loadChildren()).isFalse();
    }

    @Test
    void should_return_portal_navigation_items_with_parent_id_and_loading_children() {
        // Given
        var items = PortalNavigationItemFixtures.sampleNavigationItems(); // Assuming this includes the parent and children

        ArgumentCaptor<ListPortalNavigationItemsUseCase.Input> inputCaptor = ArgumentCaptor.forClass(
            ListPortalNavigationItemsUseCase.Input.class
        );
        when(listPortalNavigationItemsUseCase.execute(inputCaptor.capture())).thenReturn(
            new ListPortalNavigationItemsUseCase.Output(items)
        );

        when(
            permissionService.hasPermission(
                GraviteeContext.getExecutionContext(),
                RolePermission.ENVIRONMENT_DOCUMENTATION,
                ENVIRONMENT,
                RolePermissionAction.READ
            )
        ).thenReturn(true);

        // When
        Response response = target
            .queryParam("area", PortalArea.TOP_NAVBAR)
            .queryParam("parentId", "00000000-0000-0000-0000-000000000001")
            .queryParam("loadChildren", true)
            .request()
            .get();

        // Then
        assertThat(response)
            .hasStatus(OK_200)
            .asEntity(PortalNavigationItemsResponse.class)
            .satisfies(entity -> {
                // When requesting with parentId and loadChildren=true the returned structure should be hierarchical as well
                // (the mocked use case returns the full sample items, so top-level should be the same 3 elements)
                assertThat(entity.getItems()).hasSize(3);

                // Additional structural checks (same as previous test)
                var apisFolder = entity
                    .getItems()
                    .stream()
                    .filter(i ->
                        ((io.gravitee.rest.api.management.v2.rest.model.BasePortalNavigationItem) i.getActualInstance()).getId().equals(
                            "00000000-0000-0000-0000-000000000001"
                        )
                    )
                    .map(i -> (io.gravitee.rest.api.management.v2.rest.model.PortalNavigationFolder) i.getActualInstance())
                    .findFirst()
                    .orElseThrow();

                assertThat(apisFolder.getChildren()).hasSize(3);
            });

        var capturedInput = inputCaptor.getValue();
        assertThat(capturedInput.environmentId()).isEqualTo(ENVIRONMENT);
        assertThat(capturedInput.portalArea()).isEqualTo(PortalArea.TOP_NAVBAR);
        assertThat(capturedInput.parentId()).isPresent();
        assertThat(capturedInput.parentId().get().id().toString()).isEqualTo("00000000-0000-0000-0000-000000000001");
        assertThat(capturedInput.loadChildren()).isTrue();
    }

    @Test
    void should_return_forbidden_when_no_permission() {
        // Given
        when(
            permissionService.hasPermission(
                GraviteeContext.getExecutionContext(),
                RolePermission.ENVIRONMENT_DOCUMENTATION,
                ENVIRONMENT,
                RolePermissionAction.READ
            )
        ).thenReturn(false);

        // When
        Response response = target.queryParam("area", PortalArea.TOP_NAVBAR).request().get();

        // Then
        assertThat(response).hasStatus(FORBIDDEN_403);
    }
}
