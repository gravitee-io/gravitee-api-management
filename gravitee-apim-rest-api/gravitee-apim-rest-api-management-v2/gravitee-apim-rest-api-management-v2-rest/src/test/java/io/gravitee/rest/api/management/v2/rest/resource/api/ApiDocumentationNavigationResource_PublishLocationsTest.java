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
import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import fixtures.core.model.PortalNavigationItemFixtures;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import io.gravitee.rest.api.management.v2.rest.model.ApiPortalPublishLocation;
import io.gravitee.rest.api.management.v2.rest.model.ApiPortalPublishLocationsResponse;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiDocumentationNavigationResource_PublishLocationsTest extends AbstractResourceTest {

    private static final String ENVIRONMENT = "environment-id";
    private static final String API_ID = "api-id";

    @Inject
    private EnvironmentService environmentService;

    @Autowired
    private PortalNavigationItemsQueryServiceInMemory portalNavigationItemsQueryService;

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis/" + API_ID + "/portal-navigation-items/_publish-locations";
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
        portalNavigationItemsQueryService.reset();
    }

    @Test
    void should_list_publish_locations_with_only_their_id_and_name() {
        var section = PortalNavigationItemFixtures.aFolder("APIs");
        section.setEnvironmentId(ENVIRONMENT);
        var nested = PortalNavigationItemFixtures.aFolder("Payments", section.getId());
        nested.setEnvironmentId(ENVIRONMENT);
        portalNavigationItemsQueryService.initWith(List.of(section, nested));

        Response response = rootTarget().request().get();

        assertThat(response)
            .hasStatus(OK_200)
            .asEntity(ApiPortalPublishLocationsResponse.class)
            .satisfies(entity ->
                assertThat(entity.getData()).containsExactly(new ApiPortalPublishLocation().id(section.getId().id()).name("APIs"))
            );
    }

    @Test
    void should_not_expose_any_other_field_of_a_section() {
        var section = PortalNavigationItemFixtures.aFolder("APIs");
        section.setEnvironmentId(ENVIRONMENT);
        portalNavigationItemsQueryService.initWith(List.of(section));

        Response response = rootTarget().request().get();

        assertThat(response.readEntity(new GenericType<Map<String, List<Map<String, Object>>>>() {}).get("data"))
            .singleElement()
            .satisfies(location -> assertThat(location).containsOnlyKeys("id", "name"));
    }

    @Test
    void should_return_an_empty_list_when_no_sections_exist() {
        Response response = rootTarget().request().get();

        assertThat(response)
            .hasStatus(OK_200)
            .asEntity(ApiPortalPublishLocationsResponse.class)
            .satisfies(entity -> assertThat(entity.getData()).isEmpty());
    }

    @Test
    void should_return_403_with_environment_documentation_permission_only() {
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.API_DOCUMENTATION),
                eq(API_ID),
                eq(RolePermissionAction.READ)
            )
        ).thenReturn(false);

        Response response = rootTarget().request().get();

        assertThat(response).hasStatus(FORBIDDEN_403);
    }

    @Test
    void should_succeed_with_api_documentation_permission_only() {
        when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(false);
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.API_DOCUMENTATION),
                eq(API_ID),
                eq(RolePermissionAction.READ)
            )
        ).thenReturn(true);

        Response response = rootTarget().request().get();

        assertThat(response).hasStatus(OK_200);
    }
}
