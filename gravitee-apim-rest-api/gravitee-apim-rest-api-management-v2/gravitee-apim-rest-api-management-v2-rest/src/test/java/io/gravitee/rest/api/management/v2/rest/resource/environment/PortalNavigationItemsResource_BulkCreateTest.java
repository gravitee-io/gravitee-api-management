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
import static io.gravitee.common.http.HttpStatusCode.BAD_REQUEST_400;
import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static jakarta.ws.rs.client.Entity.json;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import fixtures.PortalNavigationItemsFixtures;
import io.gravitee.apim.core.portal_page.use_case.BulkCreatePortalNavigationItemUseCase;
import io.gravitee.rest.api.management.v2.rest.model.BaseCreatePortalNavigationItems;
import io.gravitee.rest.api.management.v2.rest.model.BasePortalNavigationItem;
import io.gravitee.rest.api.management.v2.rest.model.CreatePortalNavigationApi;
import io.gravitee.rest.api.management.v2.rest.model.CreatePortalNavigationFolder;
import io.gravitee.rest.api.management.v2.rest.model.CreatePortalNavigationLink;
import io.gravitee.rest.api.management.v2.rest.model.CreatePortalNavigationPage;
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
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalNavigationItemsResource_BulkCreateTest extends AbstractResourceTest {

    private static final String ENVIRONMENT = "environment-id";

    @Inject
    private BulkCreatePortalNavigationItemUseCase bulkCreatePortalNavigationItemUseCase;

    @Inject
    private EnvironmentService environmentService;

    private WebTarget target;

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/portal-navigation-items/_bulk";
    }

    @BeforeEach
    public void setUp() {
        target = rootTarget();

        EnvironmentEntity environmentEntity = EnvironmentEntity.builder().id(ENVIRONMENT).organizationId(ORGANIZATION).build();
        when(environmentService.findById(ENVIRONMENT)).thenReturn(environmentEntity);
        when(environmentService.findByOrgAndIdOrHrid(ORGANIZATION, ENVIRONMENT)).thenReturn(environmentEntity);

        GraviteeContext.setCurrentEnvironment(ENVIRONMENT);
        GraviteeContext.setCurrentOrganization(ORGANIZATION);

        when(
            permissionService.hasPermission(
                GraviteeContext.getExecutionContext(),
                RolePermission.ENVIRONMENT_DOCUMENTATION,
                ENVIRONMENT,
                RolePermissionAction.UPDATE
            )
        ).thenReturn(true);
    }

    @AfterEach
    public void tearDown() {
        GraviteeContext.cleanContext();
        Mockito.reset(bulkCreatePortalNavigationItemUseCase);
    }

    @Test
    void should_not_create_portal_navigation_items_when_no_permission() {
        // Given
        final var folder = (CreatePortalNavigationFolder) PortalNavigationItemsFixtures.aCreatePortalNavigationFolder();
        final var request = new BaseCreatePortalNavigationItems().items(List.of(folder));
        when(
            permissionService.hasPermission(
                GraviteeContext.getExecutionContext(),
                RolePermission.ENVIRONMENT_DOCUMENTATION,
                ENVIRONMENT,
                RolePermissionAction.UPDATE
            )
        ).thenReturn(false);

        // When
        Response response = target.request().post(json(request));

        // Then
        assertThat(response).hasStatus(FORBIDDEN_403);
    }

    @Test
    void should_create_portal_navigation_items_in_bulk() {
        // Given
        final var page = (CreatePortalNavigationPage) PortalNavigationItemsFixtures.aCreatePortalNavigationPage();
        final var folder = (CreatePortalNavigationFolder) PortalNavigationItemsFixtures.aCreatePortalNavigationFolder();
        final var link = (CreatePortalNavigationLink) PortalNavigationItemsFixtures.aCreatePortalNavigationLink();

        final var request = new BaseCreatePortalNavigationItems().items(List.of(page, folder, link));

        final var output = List.of(
            PortalNavigationItemsFixtures.aPortalNavigationPage(ORGANIZATION, ENVIRONMENT),
            PortalNavigationItemsFixtures.aPortalNavigationFolder(ORGANIZATION, ENVIRONMENT),
            PortalNavigationItemsFixtures.aPortalNavigationLink(ORGANIZATION, ENVIRONMENT)
        );
        when(bulkCreatePortalNavigationItemUseCase.execute(any())).thenReturn(new BulkCreatePortalNavigationItemUseCase.Output(output));

        // When
        Response response = target.request().post(json(request));

        // Then
        assertThat(response).hasStatus(OK_200);
        verify(bulkCreatePortalNavigationItemUseCase).execute(any());

        final var body = response.readEntity(PortalNavigationItemsResponse.class);
        assertThat(body).isNotNull();
        assertThat(body.getItems()).hasSize(3);
        assertThat(
            body
                .getItems()
                .stream()
                .map(i -> ((BasePortalNavigationItem) i.getActualInstance()).getTitle())
                .toList()
        ).containsExactly(page.getTitle(), folder.getTitle(), link.getTitle());
    }

    @Test
    void should_return_validation_error_when_items_is_null() {
        // Given
        final var request = new BaseCreatePortalNavigationItems().items(null);

        // When
        final var response = target.request().post(json(request));

        // Then
        assertThat(response).hasStatus(BAD_REQUEST_400);
        verifyNoInteractions(bulkCreatePortalNavigationItemUseCase);
    }

    @Test
    void should_return_validation_error_when_item_title_is_null() {
        // Given
        final var folder = (CreatePortalNavigationFolder) PortalNavigationItemsFixtures.aCreatePortalNavigationFolder();
        folder.setTitle(null);
        final var request = new BaseCreatePortalNavigationItems().items(List.of(folder));

        // When
        final var response = target.request().post(json(request));

        // Then
        assertThat(response).hasStatus(BAD_REQUEST_400);
        verifyNoInteractions(bulkCreatePortalNavigationItemUseCase);
    }

    @Test
    void should_return_validation_error_when_item_apiId_is_null() {
        // Given
        final var folder = (CreatePortalNavigationApi) PortalNavigationItemsFixtures.aCreatePortalNavigationApi();
        folder.setApiId(null);
        final var request = new BaseCreatePortalNavigationItems().items(List.of(folder));

        // When
        final var response = target.request().post(json(request));

        // Then
        assertThat(response).hasStatus(BAD_REQUEST_400);
        verifyNoInteractions(bulkCreatePortalNavigationItemUseCase);
    }
}
