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
import inmemory.PortalCrudServiceInMemory;
import inmemory.PortalNavigationItemsCrudServiceInMemory;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.portal.model.Portal;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal.model.PortalNavigationStructure;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.rest.api.management.v2.rest.model.ApiPortalPublication;
import io.gravitee.rest.api.management.v2.rest.model.PublishApiToPortal;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
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
class ApiDocumentationNavigationResource_PublishTest extends AbstractResourceTest {

    private static final String ENVIRONMENT = "environment-id";
    private static final String API_ID = "api-id";

    @Inject
    private EnvironmentService environmentService;

    @Autowired
    private PortalNavigationItemsCrudServiceInMemory portalNavigationItemCrudService;

    @Autowired
    private PortalCrudServiceInMemory portalCrudService;

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis/" + API_ID + "/portal-navigation-items/_publish";
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
        portalNavigationItemCrudService.reset();
        portalCrudService.reset();
        apiCrudService.reset();
    }

    @Test
    void should_publish_the_api_into_the_chosen_section_with_its_documentation() {
        var section = aSection("APIs");
        var page = aPageOwnedByTheApi();
        portalNavigationItemCrudService.initWith(List.of(section, page));

        Response response = publishTo(section.getId());

        assertThat(response)
            .hasStatus(OK_200)
            .asEntity(ApiPortalPublication.class)
            .satisfies(publication -> {
                assertThat(publication.getPortalId()).isEqualTo(
                    portalCrudService.findDefaultByEnvironmentId(ENVIRONMENT).orElseThrow().getId().toString()
                );
                assertThat(publication.getSectionName()).isEqualTo("APIs");
                assertThat(publication.getPortalNavigationItem().getParentId()).isEqualTo(section.getId().id());
                assertThat(publication.getPortalNavigationItem().getApiId()).isEqualTo(API_ID);
                assertThat(publication.getPortalNavigationItem().getPublished()).isTrue();
            });
        assertThat(portalNavigationItemCrudService.storage())
            .filteredOn(PortalNavigationApi.class::isInstance)
            .singleElement()
            .satisfies(listing -> {
                assertThat(listing.getParentId()).isEqualTo(section.getId());
                assertThat(listing.getTitle()).isEqualTo("My API");
            });
        assertThat(portalNavigationItemCrudService.storage())
            .filteredOn(item -> item.getId().equals(page.getId()))
            .singleElement()
            .satisfies(published -> {
                assertThat(published.getPublished()).isTrue();
                assertThat(published.getParentId()).isNull();
            });
    }

    @Test
    void should_return_400_when_api_is_already_published() {
        var section = aSection("APIs");
        var listing = PortalNavigationItemFixtures.anApi(PortalNavigationItemId.random().json(), "My API", section.getId(), API_ID);
        listing.setEnvironmentId(ENVIRONMENT);
        listing.updateParent(section);
        portalNavigationItemCrudService.initWith(List.of(section, listing));

        Response response = publishTo(section.getId());

        assertThat(response).hasStatus(BAD_REQUEST_400);
        assertThat(portalNavigationItemCrudService.storage()).hasSize(2);
    }

    @Test
    void should_publish_by_reusing_a_listing_the_portal_editor_left_hidden() {
        var section = aSection("APIs");
        var hiddenListing = PortalNavigationItemFixtures.anApi(PortalNavigationItemId.random().json(), "My API", section.getId(), API_ID);
        hiddenListing.setEnvironmentId(ENVIRONMENT);
        hiddenListing.updateParent(section);
        hiddenListing.setPublished(false);
        portalNavigationItemCrudService.initWith(List.of(section, hiddenListing));

        Response response = publishTo(section.getId());

        assertThat(response)
            .hasStatus(OK_200)
            .asEntity(ApiPortalPublication.class)
            .satisfies(publication -> {
                assertThat(publication.getPortalNavigationItem().getId()).isEqualTo(hiddenListing.getId().id());
                assertThat(publication.getPortalNavigationItem().getPublished()).isTrue();
            });
        assertThat(portalNavigationItemCrudService.storage()).hasSize(2);
    }

    @Test
    void should_return_400_when_the_target_is_not_a_publish_location() {
        var section = aSection("APIs");
        var nested = PortalNavigationItemFixtures.aFolder("Payments", section.getId());
        nested.setEnvironmentId(ENVIRONMENT);
        portalNavigationItemCrudService.initWith(List.of(section, nested));

        Response response = publishTo(nested.getId());

        assertThat(response).hasStatus(BAD_REQUEST_400);
        assertThat(portalNavigationItemCrudService.storage()).hasSize(2);
    }

    @Test
    void should_return_400_when_no_section_is_given() {
        Response response = rootTarget().request().post(Entity.json(Map.of()));

        assertThat(response).hasStatus(BAD_REQUEST_400);
    }

    @Test
    void should_return_404_for_an_unknown_section() {
        Response response = publishTo(PortalNavigationItemId.random());

        assertThat(response).hasStatus(NOT_FOUND_404);
        assertThat(portalNavigationItemCrudService.storage()).isEmpty();
    }

    @Test
    void should_return_403_with_environment_documentation_permission_only() {
        var section = aSection("APIs");
        portalNavigationItemCrudService.initWith(List.of(section));
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.API_DOCUMENTATION),
                eq(API_ID),
                eq(RolePermissionAction.UPDATE)
            )
        ).thenReturn(false);

        Response response = publishTo(section.getId());

        assertThat(response).hasStatus(FORBIDDEN_403);
        assertThat(portalNavigationItemCrudService.storage()).containsExactly(section);
    }

    @Test
    void should_succeed_with_api_documentation_permission_only() {
        var section = aSection("APIs");
        portalNavigationItemCrudService.initWith(List.of(section));
        when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(false);
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.API_DOCUMENTATION),
                eq(API_ID),
                eq(RolePermissionAction.UPDATE)
            )
        ).thenReturn(true);

        Response response = publishTo(section.getId());

        assertThat(response).hasStatus(OK_200);
    }

    @Test
    void should_publish_to_the_named_portal_only() {
        var otherPortalId = givenAnotherPortal();
        var section = aSection("APIs");
        var otherPortalSection = aSectionOf(otherPortalId, "Partners");
        portalNavigationItemCrudService.initWith(List.of(section, otherPortalSection));

        Response response = publishTo(otherPortalSection.getId(), otherPortalId.toString());

        assertThat(response)
            .hasStatus(OK_200)
            .asEntity(ApiPortalPublication.class)
            .satisfies(publication -> {
                assertThat(publication.getPortalId()).isEqualTo(otherPortalId.toString());
                assertThat(publication.getSectionName()).isEqualTo("Partners");
            });
        assertThat(portalNavigationItemCrudService.storage())
            .filteredOn(PortalNavigationApi.class::isInstance)
            .singleElement()
            .satisfies(listing -> assertThat(listing.getParentId()).isEqualTo(otherPortalSection.getId()));
    }

    @Test
    void should_publish_to_the_default_portal_when_none_is_named_and_the_environment_has_several() {
        givenAnotherPortal();
        var section = aSection("APIs");
        portalNavigationItemCrudService.initWith(List.of(section));

        Response response = publishTo(section.getId());

        assertThat(response)
            .hasStatus(OK_200)
            .asEntity(ApiPortalPublication.class)
            .satisfies(publication ->
                assertThat(publication.getPortalId()).isEqualTo(
                    ((NavigationItemReference.PortalReference) PORTAL_REFERENCE).portalId().toString()
                )
            );
    }

    @Test
    void should_return_400_for_a_section_of_another_portal() {
        var otherPortalId = givenAnotherPortal();
        var otherPortalSection = aSectionOf(otherPortalId, "Partners");
        portalNavigationItemCrudService.initWith(List.of(otherPortalSection));

        Response response = publishTo(otherPortalSection.getId());

        assertThat(response).hasStatus(BAD_REQUEST_400);
        assertThat(portalNavigationItemCrudService.storage()).containsExactly(otherPortalSection);
    }

    @Test
    void should_return_404_for_an_unknown_portal() {
        var section = aSection("APIs");
        portalNavigationItemCrudService.initWith(List.of(section));

        assertThat(publishTo(section.getId(), PortalId.random().toString())).hasStatus(NOT_FOUND_404);
        assertThat(publishTo(section.getId(), "not-a-uuid")).hasStatus(NOT_FOUND_404);
        assertThat(portalNavigationItemCrudService.storage()).containsExactly(section);
    }

    private Response publishTo(PortalNavigationItemId sectionId) {
        return rootTarget().request().post(Entity.json(new PublishApiToPortal().sectionId(sectionId.id())));
    }

    private Response publishTo(PortalNavigationItemId sectionId, String portalId) {
        return rootTarget()
            .queryParam("portalId", portalId)
            .request()
            .post(Entity.json(new PublishApiToPortal().sectionId(sectionId.id())));
    }

    private PortalId givenAnotherPortal() {
        return portalCrudService.create(Portal.create(ENVIRONMENT, ORGANIZATION, "Partners Portal", PortalFixtures.PORTAL_URL)).getId();
    }

    private static PortalNavigationFolder aSectionOf(PortalId portalId, String title) {
        var section = PortalNavigationItemFixtures.aFolder(title)
            .toBuilder()
            .reference(new NavigationItemReference.PortalReference(portalId))
            .build();
        section.setEnvironmentId(ENVIRONMENT);
        section.markAsRoot();
        return section;
    }

    private static PortalNavigationFolder aSection(String title) {
        var section = PortalNavigationItemFixtures.aFolder(title);
        section.setEnvironmentId(ENVIRONMENT);
        section.markAsRoot();
        return section;
    }

    private static PortalNavigationPage aPageOwnedByTheApi() {
        var page = PortalNavigationItemFixtures.aPage("Overview", null)
            .toBuilder()
            .reference(new NavigationItemReference.ApiReference(API_ID))
            .published(false)
            .build();
        page.setEnvironmentId(ENVIRONMENT);
        return page;
    }
}
