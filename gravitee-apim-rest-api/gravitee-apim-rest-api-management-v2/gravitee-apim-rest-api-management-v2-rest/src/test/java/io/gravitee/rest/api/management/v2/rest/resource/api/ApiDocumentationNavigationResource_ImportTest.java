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
import static io.gravitee.common.http.HttpStatusCode.NOT_FOUND_404;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import fixtures.core.model.PortalNavigationItemFixtures;
import inmemory.PortalNavigationItemSourceDomainServiceInMemory;
import inmemory.PortalNavigationItemsCrudServiceInMemory;
import inmemory.PortalPageContentCrudServiceInMemory;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemSourceDomainService;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.rest.api.management.v2.rest.model.ImportPortalNavigationRequest;
import io.gravitee.rest.api.management.v2.rest.model.ImportPortalNavigationResponse;
import io.gravitee.rest.api.management.v2.rest.model.PortalNavigationItemSource;
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
class ApiDocumentationNavigationResource_ImportTest extends AbstractResourceTest {

    private static final String ENVIRONMENT = "environment-id";
    private static final String API_ID = "api-id";
    private static final String OTHER_API_ID = "other-api-id";
    private static final NavigationItemReference API_REFERENCE = new NavigationItemReference.ApiReference(API_ID);

    @Inject
    private EnvironmentService environmentService;

    @Autowired
    private PortalNavigationItemsCrudServiceInMemory portalNavigationItemCrudService;

    @Autowired
    private PortalPageContentCrudServiceInMemory portalPageContentCrudService;

    @Inject
    private PortalNavigationItemSourceDomainService portalNavigationItemSourceDomainService;

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis/" + API_ID + "/portal-navigation-items/_import";
    }

    @BeforeEach
    public void init() {
        EnvironmentEntity environmentEntity = EnvironmentEntity.builder().id(ENVIRONMENT).organizationId(ORGANIZATION).build();
        when(environmentService.findById(ENVIRONMENT)).thenReturn(environmentEntity);
        when(environmentService.findByOrgAndIdOrHrid(ORGANIZATION, ENVIRONMENT)).thenReturn(environmentEntity);

        GraviteeContext.setCurrentEnvironment(ENVIRONMENT);
        GraviteeContext.setCurrentOrganization(ORGANIZATION);

        apiCrudService.initWith(List.of(Api.builder().id(API_ID).name("My API").environmentId(ENVIRONMENT).build()));

        sourceDomainService().givenRemoteFile("/docs/guide.md", "# Guide");
        sourceDomainService().givenRemoteFile("/docs/advanced/tuning.md", "# Tuning");
    }

    @AfterEach
    public void cleanUp() {
        GraviteeContext.cleanContext();
        apiCrudService.reset();
        portalNavigationItemCrudService.reset();
        portalPageContentCrudService.reset();
        sourceDomainService().resetFileListing();
    }

    @Test
    void should_import_a_folder_owned_by_the_api_with_everything_below_it() {
        Response response = rootTarget().request().post(Entity.json(aRequest()));

        assertThat(response).hasStatus(OK_200);
        var body = response.readEntity(ImportPortalNavigationResponse.class);
        assertThat(body.getSummary().getSucceeded()).isEqualTo(2);
        // root, docs, advanced, guide, tuning
        assertThat(portalNavigationItemCrudService.storage())
            .hasSize(5)
            .allSatisfy(item -> {
                assertThat(item.getReference()).isEqualTo(API_REFERENCE);
                assertThat(item.getPublished()).isFalse();
            });
    }

    @Test
    void should_import_under_a_folder_of_the_same_api() {
        var folder = aFolderOwnedBy(API_ID);
        portalNavigationItemCrudService.initWith(List.of(folder));

        Response response = rootTarget().request().post(Entity.json(aRequest().parentId(folder.getId().id())));

        assertThat(response).hasStatus(OK_200);
        var rootFolder = (io.gravitee.rest.api.management.v2.rest.model.PortalNavigationFolder) response
            .readEntity(ImportPortalNavigationResponse.class)
            .getRootFolder()
            .getActualInstance();
        assertThat(rootFolder.getParentId()).isEqualTo(folder.getId().id());
    }

    @Test
    void should_reject_a_parent_belonging_to_another_api() {
        var foreignFolder = aFolderOwnedBy(OTHER_API_ID);
        portalNavigationItemCrudService.initWith(List.of(foreignFolder));

        Response response = rootTarget().request().post(Entity.json(aRequest().parentId(foreignFolder.getId().id())));

        assertThat(response).hasStatus(NOT_FOUND_404);
        assertThat(portalNavigationItemCrudService.storage()).containsExactly(foreignFolder);
    }

    @Test
    void should_reject_a_portal_owned_parent() {
        var section = PortalNavigationItemFixtures.aFolder("APIs");
        section.setEnvironmentId(ENVIRONMENT);
        portalNavigationItemCrudService.initWith(List.of(section));

        Response response = rootTarget().request().post(Entity.json(aRequest().parentId(section.getId().id())));

        assertThat(response).hasStatus(NOT_FOUND_404);
        assertThat(portalNavigationItemCrudService.storage()).containsExactly(section);
    }

    @Test
    void should_return_403_with_environment_documentation_permission_only() {
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.API_DOCUMENTATION),
                eq(API_ID),
                eq(RolePermissionAction.CREATE)
            )
        ).thenReturn(false);

        Response response = rootTarget().request().post(Entity.json(aRequest()));

        assertThat(response).hasStatus(FORBIDDEN_403);
        assertThat(portalNavigationItemCrudService.storage()).isEmpty();
    }

    @Test
    void should_succeed_with_api_documentation_permission_only() {
        when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(false);
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.API_DOCUMENTATION),
                eq(API_ID),
                eq(RolePermissionAction.CREATE)
            )
        ).thenReturn(true);

        Response response = rootTarget().request().post(Entity.json(aRequest()));

        assertThat(response).hasStatus(OK_200);
    }

    private PortalNavigationItemSourceDomainServiceInMemory sourceDomainService() {
        return (PortalNavigationItemSourceDomainServiceInMemory) portalNavigationItemSourceDomainService;
    }

    private static ImportPortalNavigationRequest aRequest() {
        return new ImportPortalNavigationRequest()
            .title("Imported Docs")
            .source(new PortalNavigationItemSource().type("http-fetcher").configuration(Map.of("url", "https://example.com/repo")));
    }

    private static PortalNavigationFolder aFolderOwnedBy(String apiId) {
        var folder = PortalNavigationItemFixtures.aFolder(PortalNavigationItemId.random().json(), "Guides")
            .toBuilder()
            .reference(new NavigationItemReference.ApiReference(apiId))
            .published(false)
            .build();
        folder.setEnvironmentId(ENVIRONMENT);
        return folder;
    }
}
