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
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import inmemory.PortalNavigationItemSourceDomainServiceInMemory;
import inmemory.PortalNavigationItemsCrudServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.PortalPageContentCrudServiceInMemory;
import inmemory.PortalPageContentQueryServiceInMemory;
import io.gravitee.apim.core.portal_page.crud_service.PortalNavigationItemCrudService;
import io.gravitee.apim.core.portal_page.crud_service.PortalPageContentCrudService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemSourceDomainService;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import io.gravitee.apim.core.portal_page.query_service.PortalPageContentQueryService;
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
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalNavigationItemsResource_ImportTest extends AbstractResourceTest {

    private static final String ENVIRONMENT = "environment-id";

    @Inject
    private EnvironmentService environmentService;

    @Inject
    private PortalNavigationItemsQueryService portalNavigationItemsQueryService;

    @Inject
    private PortalNavigationItemCrudService portalNavigationItemCrudService;

    @Inject
    private PortalPageContentCrudService portalPageContentCrudService;

    @Inject
    private PortalPageContentQueryService portalPageContentQueryService;

    @Inject
    private PortalNavigationItemSourceDomainService portalNavigationItemSourceDomainService;

    private WebTarget target;

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/portal-navigation-items/_import";
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

        ((PortalNavigationItemsQueryServiceInMemory) portalNavigationItemsQueryService).initWith(List.of());
        ((PortalNavigationItemsCrudServiceInMemory) portalNavigationItemCrudService).initWith(List.of());
        ((PortalPageContentCrudServiceInMemory) portalPageContentCrudService).initWith(List.of());
        ((PortalPageContentQueryServiceInMemory) portalPageContentQueryService).initWith(List.of());
        ((PortalNavigationItemSourceDomainServiceInMemory) portalNavigationItemSourceDomainService).resetFileListing();
    }

    @AfterEach
    public void tearDown() {
        GraviteeContext.cleanContext();
    }

    private ImportPortalNavigationRequest aRequest() {
        return new ImportPortalNavigationRequest()
            .title("Imported Docs")
            .source(
                new PortalNavigationItemSource()
                    .type("http-fetcher")
                    .configuration(
                        Map.of("url", "https://example.com/repo", "token", PortalNavigationItemSourceDomainServiceInMemory.SENSITIVE_DATA)
                    )
            );
    }

    private Response importNavigation(ImportPortalNavigationRequest request) {
        return target.request().post(Entity.json(request));
    }

    @Test
    void should_import_the_remote_tree_and_return_the_root_folder_with_a_masked_source() {
        var sourceDomainService = (PortalNavigationItemSourceDomainServiceInMemory) portalNavigationItemSourceDomainService;
        sourceDomainService.givenRemoteFile("/docs/guide.md", "# Guide");
        sourceDomainService.givenRemoteFile("/docs/spec.yaml", "openapi: 3.0.3");

        Response response = importNavigation(aRequest());

        assertThat(response).hasStatus(OK_200);
        var body = response.readEntity(ImportPortalNavigationResponse.class);
        var rootFolder = (io.gravitee.rest.api.management.v2.rest.model.PortalNavigationFolder) body.getRootFolder().getActualInstance();
        assertThat(rootFolder.getTitle()).isEqualTo("Imported Docs");
        assertThat(rootFolder.getSource()).isNotNull();
        assertThat(rootFolder.getSource().getLastFetchedAt()).isNotNull();
        assertThat((Map<String, Object>) rootFolder.getSource().getConfiguration()).containsEntry(
            "token",
            PortalNavigationItemSourceDomainServiceInMemory.SENSITIVE_DATA_REPLACEMENT
        );
        assertThat(body.getSummary().getSucceeded()).isEqualTo(2);
        assertThat(body.getSummary().getFailed()).isZero();
    }

    @Test
    void should_return_400_when_the_source_cannot_list_files() {
        // The in-memory source only supports file listing once a remote file is registered
        assertThat(importNavigation(aRequest())).hasStatus(BAD_REQUEST_400);
    }

    @Test
    void should_return_403_when_user_lacks_update_permission() {
        when(
            permissionService.hasPermission(
                GraviteeContext.getExecutionContext(),
                RolePermission.ENVIRONMENT_DOCUMENTATION,
                ENVIRONMENT,
                RolePermissionAction.UPDATE
            )
        ).thenReturn(false);

        assertThat(importNavigation(aRequest())).hasStatus(FORBIDDEN_403);
    }
}
