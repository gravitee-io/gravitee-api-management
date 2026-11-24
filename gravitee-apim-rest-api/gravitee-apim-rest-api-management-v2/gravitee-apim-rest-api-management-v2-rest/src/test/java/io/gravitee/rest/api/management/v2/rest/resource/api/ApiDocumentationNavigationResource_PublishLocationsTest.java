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
import static fixtures.core.model.PortalNavigationItemFixtures.PORTAL_REFERENCE;
import static io.gravitee.common.http.HttpStatusCode.BAD_REQUEST_400;
import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static io.gravitee.common.http.HttpStatusCode.NOT_FOUND_404;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import fixtures.core.model.PortalFixtures;
import fixtures.core.model.PortalNavigationItemFixtures;
import inmemory.NavigationPortalDomainServiceInMemory;
import inmemory.PortalCrudServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.portal.domain_service.NavigationPortalDomainService;
import io.gravitee.apim.core.portal.model.Portal;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal.model.PortalNavigationStructure;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
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

    @Autowired
    private PortalCrudServiceInMemory portalCrudService;

    @Autowired
    private NavigationPortalDomainService navigationPortalDomainService;

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

        apiCrudService.initWith(List.of(Api.builder().id(API_ID).name("My API").environmentId(ENVIRONMENT).build()));
        portalCrudService.create(
            Portal.of(
                ((NavigationItemReference.PortalReference) PORTAL_REFERENCE).portalId(),
                ENVIRONMENT,
                ORGANIZATION,
                "Developer Portal",
                PortalNavigationStructure.empty(),
                null,
                true,
                PortalFixtures.PORTAL_URL
            )
        );
    }

    @AfterEach
    public void cleanUp() {
        GraviteeContext.cleanContext();
        apiCrudService.reset();
        portalNavigationItemsQueryService.reset();
        portalCrudService.reset();
    }

    @Test
    void should_list_publish_locations_with_only_their_id_and_name() {
        var section = PortalNavigationItemFixtures.aFolder("APIs");
        section.setEnvironmentId(ENVIRONMENT);
        var nested = PortalNavigationItemFixtures.aFolder("Payments", section.getId());
        nested.setEnvironmentId(ENVIRONMENT);
        portalNavigationItemsQueryService.initWith(List.of(section, nested));

        Response response = locations();

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

        Response response = locations();

        assertThat(response.readEntity(new GenericType<Map<String, List<Map<String, Object>>>>() {}).get("data"))
            .singleElement()
            .satisfies(location -> assertThat(location).containsOnlyKeys("id", "name"));
    }

    @Test
    void should_return_an_empty_list_when_no_sections_exist() {
        Response response = locations();

        assertThat(response)
            .hasStatus(OK_200)
            .asEntity(ApiPortalPublishLocationsResponse.class)
            .satisfies(entity -> assertThat(entity.getData()).isEmpty());
    }

    @Test
    void should_list_only_the_sections_of_the_given_portal() {
        var otherPortalId = ((NavigationPortalDomainServiceInMemory) navigationPortalDomainService).givenConsolePortal(
            ORGANIZATION,
            ENVIRONMENT,
            "Partners Portal"
        );
        var section = PortalNavigationItemFixtures.aFolder("APIs");
        section.setEnvironmentId(ENVIRONMENT);
        var otherPortalSection = PortalNavigationItemFixtures.aFolder("Partners")
            .toBuilder()
            .reference(new NavigationItemReference.PortalReference(otherPortalId))
            .environmentId(ENVIRONMENT)
            .build();
        portalNavigationItemsQueryService.initWith(List.of(section, otherPortalSection));

        assertThat(rootTarget().queryParam("portalId", otherPortalId.toString()).request().get())
            .hasStatus(OK_200)
            .asEntity(ApiPortalPublishLocationsResponse.class)
            .satisfies(entity ->
                assertThat(entity.getData()).containsExactly(
                    new ApiPortalPublishLocation().id(otherPortalSection.getId().id()).name("Partners")
                )
            );
        assertThat(locations())
            .hasStatus(OK_200)
            .asEntity(ApiPortalPublishLocationsResponse.class)
            .satisfies(entity ->
                assertThat(entity.getData()).containsExactly(new ApiPortalPublishLocation().id(section.getId().id()).name("APIs"))
            );
    }

    @Test
    void should_return_400_when_no_portal_is_given() {
        assertThat(rootTarget().request().get()).hasStatus(BAD_REQUEST_400);
    }

    @Test
    void should_return_400_when_the_portal_is_blank() {
        assertThat(rootTarget().queryParam("portalId", "").request().get()).hasStatus(BAD_REQUEST_400);
        assertThat(rootTarget().queryParam("portalId", " ").request().get()).hasStatus(BAD_REQUEST_400);
    }

    @Test
    void should_return_400_for_a_portal_managed_through_the_automation_api() {
        var automationPortalId = portalCrudService
            .create(Portal.create(ENVIRONMENT, ORGANIZATION, "Automation Portal", PortalFixtures.PORTAL_URL))
            .getId();

        assertThat(rootTarget().queryParam("portalId", automationPortalId.toString()).request().get()).hasStatus(BAD_REQUEST_400);
    }

    @Test
    void should_return_404_for_an_unknown_portal() {
        assertThat(rootTarget().queryParam("portalId", PortalId.random().toString()).request().get()).hasStatus(NOT_FOUND_404);
        assertThat(rootTarget().queryParam("portalId", "not-a-uuid").request().get()).hasStatus(NOT_FOUND_404);
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

        Response response = locations();

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

        Response response = locations();

        assertThat(response).hasStatus(OK_200);
    }

    private Response locations() {
        return rootTarget()
            .queryParam("portalId", ((NavigationItemReference.PortalReference) PORTAL_REFERENCE).portalId().toString())
            .request()
            .get();
    }
}
