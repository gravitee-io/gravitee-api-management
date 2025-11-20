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
package io.gravitee.rest.api.portal.rest.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.portal_page.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.use_case.ListPortalNavigationItemsUseCase;
import io.gravitee.rest.api.portal.rest.fixture.PortalNavigationFixtures;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class PortalNavigationItemsResourceTest extends AbstractResourceTest {

    private static final String ENV_ID = "DEFAULT";

    @Inject
    private ListPortalNavigationItemsUseCase listPortalNavigationItemsUseCase;

    @Override
    protected String contextPath() {
        return "portal-navigation-items";
    }

    @BeforeEach
    public void init() {
        GraviteeContext.setCurrentEnvironment(ENV_ID);
    }

    @AfterEach
    public void tearDown() {
        GraviteeContext.cleanContext();
    }

    @Test
    public void should_return_portal_navigation_items_for_environment() {
        when(
            permissionService.hasPermission(
                GraviteeContext.getExecutionContext(),
                io.gravitee.rest.api.model.permissions.RolePermission.ENVIRONMENT_DOCUMENTATION,
                ENV_ID,
                io.gravitee.rest.api.model.permissions.RolePermissionAction.READ
            )
        ).thenReturn(true);

        // Setup mock use-case to return fixture items when no parentId is provided
        List<PortalNavigationItem> items = PortalNavigationFixtures.sampleList(PortalArea.HOMEPAGE);
        when(listPortalNavigationItemsUseCase.execute(any())).thenAnswer(invocation -> {
            var input = invocation.getArgument(0, ListPortalNavigationItemsUseCase.Input.class);
            if (input != null && input.parentId().isPresent()) {
                return new ListPortalNavigationItemsUseCase.Output(List.of());
            }
            return new ListPortalNavigationItemsUseCase.Output(items);
        });

        // When
        Response response = target()
            .queryParam("area", io.gravitee.rest.api.portal.rest.model.PortalArea.HOMEPAGE)
            .queryParam("loadChildren", true)
            .request()
            .get();

        // Then
        assertThat(response.getStatus()).isEqualTo(200);
        var result = response.readEntity(
            new jakarta.ws.rs.core.GenericType<List<io.gravitee.rest.api.portal.rest.model.PortalNavigationItem>>() {}
        );
        assertThat(result).hasSize(items.size());
    }

    @Test
    public void should_return_portal_navigation_items_with_parent_id() {
        when(
            permissionService.hasPermission(
                GraviteeContext.getExecutionContext(),
                io.gravitee.rest.api.model.permissions.RolePermission.ENVIRONMENT_DOCUMENTATION,
                ENV_ID,
                io.gravitee.rest.api.model.permissions.RolePermissionAction.READ
            )
        ).thenReturn(true);

        // Setup mock use-case: return empty list when parentId is provided (fixtures have no parent-child relations)
        List<PortalNavigationItem> items = PortalNavigationFixtures.sampleList(PortalArea.HOMEPAGE);
        when(listPortalNavigationItemsUseCase.execute(any())).thenAnswer(invocation -> {
            var input = invocation.getArgument(0, ListPortalNavigationItemsUseCase.Input.class);
            if (input != null && input.parentId().isPresent()) {
                return new ListPortalNavigationItemsUseCase.Output(List.of());
            }
            return new ListPortalNavigationItemsUseCase.Output(items);
        });

        // When
        String parentId = PortalNavigationFixtures.randomNavigationId().toString();
        Response response = target()
            .queryParam("area", io.gravitee.rest.api.portal.rest.model.PortalArea.HOMEPAGE)
            .queryParam("parentId", parentId)
            .queryParam("loadChildren", false)
            .request()
            .get();

        // Then
        assertThat(response.getStatus()).isEqualTo(200);
        var result = response.readEntity(
            new jakarta.ws.rs.core.GenericType<List<io.gravitee.rest.api.portal.rest.model.PortalNavigationItem>>() {}
        );
        assertThat(result).isEmpty();
    }
}
