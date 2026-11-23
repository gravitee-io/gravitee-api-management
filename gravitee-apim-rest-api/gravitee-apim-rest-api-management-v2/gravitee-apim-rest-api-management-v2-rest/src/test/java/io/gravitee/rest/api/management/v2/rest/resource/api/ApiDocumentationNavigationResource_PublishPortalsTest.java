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

import fixtures.core.model.PortalFixtures;
import inmemory.NavigationPortalDomainServiceInMemory;
import inmemory.PortalCrudServiceInMemory;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.portal.domain_service.NavigationPortalDomainService;
import io.gravitee.apim.core.portal.model.Portal;
import io.gravitee.rest.api.management.v2.rest.model.ApiPortalPublishPortal;
import io.gravitee.rest.api.management.v2.rest.model.ApiPortalPublishPortalsResponse;
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
class ApiDocumentationNavigationResource_PublishPortalsTest extends AbstractResourceTest {

    private static final String ENVIRONMENT = "environment-id";
    private static final String API_ID = "api-id";

    @Inject
    private EnvironmentService environmentService;

    @Autowired
    private PortalCrudServiceInMemory portalCrudService;

    @Autowired
    private NavigationPortalDomainService navigationPortalDomainService;

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis/" + API_ID + "/portal-navigation-items/_publish-portals";
    }

    @BeforeEach
    public void init() {
        EnvironmentEntity environmentEntity = EnvironmentEntity.builder().id(ENVIRONMENT).organizationId(ORGANIZATION).build();
        when(environmentService.findById(ENVIRONMENT)).thenReturn(environmentEntity);
        when(environmentService.findByOrgAndIdOrHrid(ORGANIZATION, ENVIRONMENT)).thenReturn(environmentEntity);

        GraviteeContext.setCurrentEnvironment(ENVIRONMENT);
        GraviteeContext.setCurrentOrganization(ORGANIZATION);

        apiCrudService.initWith(List.of(Api.builder().id(API_ID).name("My API").environmentId(ENVIRONMENT).build()));
    }

    @AfterEach
    public void cleanUp() {
        GraviteeContext.cleanContext();
        apiCrudService.reset();
        portalCrudService.reset();
    }

    @Test
    void should_list_the_default_portal_first_and_then_the_others_by_name() {
        var partners = ((NavigationPortalDomainServiceInMemory) navigationPortalDomainService).givenConsolePortal(
            ORGANIZATION,
            ENVIRONMENT,
            "Partners"
        );
        var developer = portalCrudService.create(
            Portal.createDefault(ENVIRONMENT, ORGANIZATION, "Developer Portal", PortalFixtures.PORTAL_URL)
        );

        Response response = rootTarget().request().get();

        assertThat(response)
            .hasStatus(OK_200)
            .asEntity(ApiPortalPublishPortalsResponse.class)
            .satisfies(entity ->
                assertThat(entity.getData()).containsExactly(
                    new ApiPortalPublishPortal().id(developer.getId().id()).name("Developer Portal")._default(true),
                    new ApiPortalPublishPortal().id(partners.id()).name("Partners")._default(false)
                )
            );
    }

    @Test
    void should_not_list_the_portals_managed_through_the_automation_api() {
        var developer = portalCrudService.create(
            Portal.createDefault(ENVIRONMENT, ORGANIZATION, "Developer Portal", PortalFixtures.PORTAL_URL)
        );
        portalCrudService.create(Portal.create(ENVIRONMENT, ORGANIZATION, "Automation Portal", PortalFixtures.PORTAL_URL));

        Response response = rootTarget().request().get();

        assertThat(response)
            .hasStatus(OK_200)
            .asEntity(ApiPortalPublishPortalsResponse.class)
            .satisfies(entity ->
                assertThat(entity.getData()).containsExactly(
                    new ApiPortalPublishPortal().id(developer.getId().id()).name("Developer Portal")._default(true)
                )
            );
    }

    @Test
    void should_not_expose_any_other_field_of_a_portal() {
        portalCrudService.create(Portal.createDefault(ENVIRONMENT, ORGANIZATION, "Developer Portal", PortalFixtures.PORTAL_URL));

        Response response = rootTarget().request().get();

        assertThat(response.readEntity(new GenericType<Map<String, List<Map<String, Object>>>>() {}).get("data"))
            .singleElement()
            .satisfies(portal -> assertThat(portal).containsOnlyKeys("id", "name", "default"));
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

        assertThat(rootTarget().request().get()).hasStatus(FORBIDDEN_403);
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

        assertThat(rootTarget().request().get()).hasStatus(OK_200);
    }
}
