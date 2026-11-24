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

import static io.gravitee.common.http.HttpStatusCode.BAD_REQUEST_400;
import static io.gravitee.common.http.HttpStatusCode.CONFLICT_409;
import static io.gravitee.common.http.HttpStatusCode.CREATED_201;
import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static jakarta.ws.rs.client.Entity.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.core.model.LicenseFixtures;
import inmemory.InstallationAccessQueryServiceInMemory;
import inmemory.PortalCrudServiceInMemory;
import io.gravitee.apim.core.portal.model.Portal;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal.model.PortalNavigationStructure;
import io.gravitee.apim.core.portal_page.domain_service.PortalStarterContentDomainService;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.node.api.license.LicenseManager;
import io.gravitee.rest.api.management.v2.rest.model.CreatePortal;
import io.gravitee.rest.api.management.v2.rest.model.Error;
import io.gravitee.rest.api.management.v2.rest.model.PortalQuota;
import io.gravitee.rest.api.management.v2.rest.model.PortalsResponse;
import io.gravitee.rest.api.management.v2.rest.model.PortalsStats;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PortalsResource_CreateTest extends AbstractResourceTest {

    private static final String ENVIRONMENT = "my-env";
    private static final PortalId DEFAULT_PORTAL_ID = PortalId.of("8f6a2b2e-3c1d-4f5e-9a7b-1c2d3e4f5a6b");

    @Inject
    private PortalCrudServiceInMemory portalCrudService;

    @Inject
    private InstallationAccessQueryServiceInMemory installationAccessQueryService;

    @Inject
    private PortalStarterContentDomainService portalStarterContentDomainService;

    @Inject
    private LicenseManager licenseManager;

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/portals";
    }

    @BeforeEach
    void init() {
        super.setUp();
        GraviteeContext.cleanContext();

        EnvironmentEntity environmentEntity = EnvironmentEntity.builder().id(ENVIRONMENT).organizationId(ORGANIZATION).build();
        when(environmentService.findByOrgAndIdOrHrid(ORGANIZATION, ENVIRONMENT)).thenReturn(environmentEntity);
        when(licenseManager.getOrganizationLicenseOrPlatform(ORGANIZATION)).thenReturn(
            LicenseFixtures.aLicenseWithAttribute("apim-portals-max", 10)
        );

        GraviteeContext.setCurrentEnvironment(ENVIRONMENT);
        GraviteeContext.setCurrentOrganization(ORGANIZATION);

        installationAccessQueryService.withPortalUrl(ENVIRONMENT, "https://portal.acme.io");
        portalCrudService.initWith(List.of(defaultPortal()));
    }

    @AfterEach
    public void tearDown() {
        super.tearDown();
        GraviteeContext.cleanContext();
        portalCrudService.reset();
        installationAccessQueryService.reset();
        reset(portalStarterContentDomainService);
        when(licenseManager.getOrganizationLicenseOrPlatform(any())).thenReturn(LicenseFixtures.anEnterpriseLicense());
    }

    @Test
    void should_create_a_portal_served_at_the_environment_portal_url() {
        var response = rootTarget().request().post(json(new CreatePortal().name("Partners")));

        assertThat(response.getStatus()).isEqualTo(CREATED_201);
        var body = response.readEntity(io.gravitee.rest.api.management.v2.rest.model.Portal.class);
        assertThat(body.getId()).isNotNull();
        assertThat(body.getName()).isEqualTo("Partners");
        assertThat(body.getUrl()).isEqualTo("https://portal.acme.io/next");
        assertThat(body.getDefault()).isFalse();
        assertThat(body.getLogoUrl()).isNull();
        assertThat(body.getPublishedApisCount()).isZero();
        assertThat(response.getLocation()).hasToString(rootTarget(body.getId().toString()).getUri().toString());
        assertThat(portalCrudService.findByIdAndEnvironmentId(PortalId.of(body.getId().toString()), ENVIRONMENT)).hasValueSatisfying(
            stored -> assertThat(stored.isCreatedFromConsole()).isTrue()
        );
        verify(portalStarterContentDomainService).seed(
            ORGANIZATION,
            ENVIRONMENT,
            new NavigationItemReference.PortalReference(PortalId.of(body.getId().toString()))
        );
    }

    @Test
    void should_list_the_created_portal_and_count_it_in_the_quota() {
        var created = rootTarget()
            .request()
            .post(json(new CreatePortal().name("Partners")))
            .readEntity(io.gravitee.rest.api.management.v2.rest.model.Portal.class);

        var list = rootTarget().request().get();
        var stats = rootTarget("_stats").request().get();

        assertThat(list.getStatus()).isEqualTo(OK_200);
        assertThat(list.readEntity(PortalsResponse.class).getData())
            .extracting(io.gravitee.rest.api.management.v2.rest.model.Portal::getId)
            .containsExactly(DEFAULT_PORTAL_ID.id(), created.getId());
        assertThat(stats.readEntity(PortalsStats.class).getQuota()).isEqualTo(new PortalQuota().used(2).limit(10));
    }

    @Test
    void should_return_400_for_an_empty_name() {
        var response = rootTarget().request().post(json(new CreatePortal().name("")));

        assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);
        assertThat(portalCrudService.storage()).hasSize(1);
    }

    @Test
    void should_return_400_for_a_blank_name() {
        var response = rootTarget().request().post(json(new CreatePortal().name("   ")));

        assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);
        assertThat(portalCrudService.storage()).hasSize(1);
    }

    @Test
    void should_return_400_for_a_url_until_portal_urls_can_be_chosen() {
        var response = rootTarget().request().post(json(new CreatePortal().name("Partners").url("https://partners.example.com")));

        assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);
        assertThat(response.readEntity(Error.class).getMessage()).contains("cannot be chosen yet");
        assertThat(portalCrudService.storage()).hasSize(1);
        verify(portalStarterContentDomainService, never()).seed(any(), any(), any());
    }

    @Test
    void should_return_403_with_the_limit_when_the_license_limit_is_reached() {
        when(licenseManager.getOrganizationLicenseOrPlatform(ORGANIZATION)).thenReturn(LicenseFixtures.anEnterpriseLicense());

        var response = rootTarget().request().post(json(new CreatePortal().name("Partners")));

        assertThat(response.getStatus()).isEqualTo(FORBIDDEN_403);
        var error = response.readEntity(Error.class);
        assertThat(error.getHttpStatus()).isEqualTo(FORBIDDEN_403);
        assertThat(error.getTechnicalCode()).isEqualTo("portal.limitReached");
        assertThat(error.getParameters()).isEqualTo(Map.of("limit", "1"));
        assertThat(error.getMessage()).isEqualTo("The license allows 1 portal in this environment");
        assertThat(portalCrudService.storage()).hasSize(1);
    }

    @Test
    void should_return_409_for_a_name_already_used_in_the_environment() {
        var response = rootTarget().request().post(json(new CreatePortal().name("developer portal")));

        assertThat(response.getStatus()).isEqualTo(CONFLICT_409);
        assertThat(response.readEntity(Error.class).getTechnicalCode()).isEqualTo("portal.name.exists");
        assertThat(portalCrudService.storage()).hasSize(1);
    }

    @Test
    void should_return_403_without_the_portal_create_permission() {
        shouldReturn403(RolePermission.ENVIRONMENT_PORTAL, ENVIRONMENT, RolePermissionAction.CREATE, () ->
            rootTarget().request().post(json(new CreatePortal().name("Partners")))
        );
        assertThat(portalCrudService.storage()).hasSize(1);
        verify(portalStarterContentDomainService, never()).seed(any(), eq(ENVIRONMENT), any());
    }

    @Test
    void should_create_a_portal_without_the_documentation_create_permission() {
        when(
            permissionService.hasPermission(
                GraviteeContext.getExecutionContext(),
                RolePermission.ENVIRONMENT_DOCUMENTATION,
                ENVIRONMENT,
                RolePermissionAction.CREATE
            )
        ).thenReturn(false);

        var response = rootTarget().request().post(json(new CreatePortal().name("Partners")));

        assertThat(response.getStatus()).isEqualTo(CREATED_201);
        assertThat(portalCrudService.storage()).hasSize(2);
    }

    private static Portal defaultPortal() {
        return Portal.of(
            DEFAULT_PORTAL_ID,
            ENVIRONMENT,
            ORGANIZATION,
            "Developer Portal",
            PortalNavigationStructure.empty(),
            null,
            true,
            "default-portal-access-point"
        );
    }
}
