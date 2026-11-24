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

import static io.gravitee.common.http.HttpStatusCode.BAD_REQUEST_400;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import inmemory.AccessPointQueryServiceInMemory;
import inmemory.PortalCrudServiceInMemory;
import io.gravitee.apim.core.access_point.model.AccessPoint;
import io.gravitee.apim.core.installation.domain_service.InstallationTypeDomainService;
import io.gravitee.apim.core.installation.query_service.InstallationAccessQueryService;
import io.gravitee.apim.core.portal.model.Portal;
import io.gravitee.apim.core.portal.model.PortalHost;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal.model.PortalNavigationStructure;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class PortalUIBootstrapResourceTest extends AbstractResourceTest {

    private static final String ORG_ID = "DEFAULT";
    private static final String ENVIRONMENT = "DEFAULT";
    private static final String OTHER_ENV_ID = "env-2";
    private static final String OTHER_ORG_ID = "org-2";
    private static final String NO_ENVIRONMENT_HEADER = "X-Test-No-Environment";
    private static final String PORTAL_API_URL = "https://api.acme.io/portal";
    private static final PortalId DEFAULT_ENV_PORTAL_ID = PortalId.of("11111111-1111-1111-1111-111111111111");
    private static final String REQUEST_URL = "https://apim.acme.io/portal/ui/bootstrap";
    private static final PortalId OTHER_ENV_PORTAL_ID = PortalId.of("22222222-2222-2222-2222-222222222222");

    @Autowired
    private PortalCrudServiceInMemory portalCrudService;

    @Autowired
    private InstallationAccessQueryService installationAccessQueryService;

    @Autowired
    private InstallationTypeDomainService installationTypeDomainService;

    @Autowired
    private AccessPointQueryServiceInMemory accessPointQueryService;

    private final List<AccessPoint> givenAccessPoints = new ArrayList<>();

    @Override
    protected String contextPath() {
        return "";
    }

    // The in-memory test container has no servlet request: the resource builds baseURL from one when no Portal API URL is set.
    @Override
    protected void decorate(ResourceConfig resourceConfig) {
        super.decorate(resourceConfig);
        var request = mock(HttpServletRequest.class);
        when(request.getRequestURL()).thenAnswer(invocation -> new StringBuffer(REQUEST_URL));
        when(request.getHeaderNames()).thenAnswer(invocation -> Collections.emptyEnumeration());
        when(request.getMethod()).thenReturn("GET");
        resourceConfig.register(
            new AbstractBinder() {
                @Override
                protected void configure() {
                    bind(request).to(HttpServletRequest.class);
                }
            }
        );
        // Requests run on a server thread, whose context defaults to DEFAULT; GraviteeContextFilter clears it on a path
        // without an environment.
        resourceConfig.register(
            new ContainerRequestFilter() {
                @Override
                public void filter(ContainerRequestContext requestContext) {
                    GraviteeContext.setCurrentEnvironment(
                        requestContext.getHeaderString(NO_ENVIRONMENT_HEADER) != null ? null : ENVIRONMENT
                    );
                }
            }
        );
    }

    @BeforeEach
    void init() {
        reset(installationAccessQueryService, installationTypeDomainService);
        when(installationAccessQueryService.getPortalAPIUrl(ENVIRONMENT)).thenReturn(PORTAL_API_URL);
        when(installationAccessQueryService.getPortalAPIUrl(OTHER_ENV_ID)).thenReturn(PORTAL_API_URL);
        var otherEnvironment = new EnvironmentEntity();
        otherEnvironment.setId(OTHER_ENV_ID);
        otherEnvironment.setOrganizationId(ORG_ID);
        when(environmentService.findById(OTHER_ENV_ID)).thenReturn(otherEnvironment);
    }

    @AfterEach
    void tearDown() {
        portalCrudService.reset();
        accessPointQueryService.reset();
        givenAccessPoints.clear();
    }

    @Test
    void should_return_the_default_portal_of_the_current_environment() {
        portalCrudService.initWith(
            List.of(aDefaultPortal(DEFAULT_ENV_PORTAL_ID, ENVIRONMENT), aDefaultPortal(OTHER_ENV_PORTAL_ID, OTHER_ENV_ID))
        );

        var response = bootstrap().request().get();

        assertThat(response.getStatus()).isEqualTo(OK_200);
        var bootstrap = response.readEntity(Map.class);
        assertThat(bootstrap.get("portalId")).isEqualTo(DEFAULT_ENV_PORTAL_ID.toString());
        assertThat(bootstrap.get("environmentId")).isEqualTo(ENVIRONMENT);
        assertThat(bootstrap.get("organizationId")).isEqualTo(ORG_ID);
        assertThat(bootstrap.get("baseURL")).isEqualTo(PORTAL_API_URL);
    }

    @Test
    void should_return_the_default_portal_of_the_requested_environment() {
        portalCrudService.initWith(
            List.of(aDefaultPortal(DEFAULT_ENV_PORTAL_ID, ENVIRONMENT), aDefaultPortal(OTHER_ENV_PORTAL_ID, OTHER_ENV_ID))
        );

        var response = bootstrap().queryParam("environmentId", OTHER_ENV_ID).request().get();

        assertThat(response.getStatus()).isEqualTo(OK_200);
        var bootstrap = response.readEntity(Map.class);
        assertThat(bootstrap.get("portalId")).isEqualTo(OTHER_ENV_PORTAL_ID.toString());
        assertThat(bootstrap.get("environmentId")).isEqualTo(OTHER_ENV_ID);
        assertThat(bootstrap.get("organizationId")).isEqualTo(ORG_ID);
        assertThat(bootstrap.get("baseURL")).isEqualTo(PORTAL_API_URL);
    }

    @Test
    void should_return_no_portal_id_when_the_environment_has_no_default_portal() {
        var response = bootstrap().request().get();

        assertThat(response.getStatus()).isEqualTo(OK_200);
        var bootstrap = response.readEntity(Map.class);
        assertThat(bootstrap.get("portalId")).isNull();
        assertThat(bootstrap.get("environmentId")).isEqualTo(ENVIRONMENT);
        assertThat(bootstrap.get("baseURL")).isEqualTo(PORTAL_API_URL);
    }

    @Test
    void should_return_the_only_portal_when_the_environment_has_no_default_portal() {
        portalCrudService.initWith(
            List.of(
                Portal.of(
                    DEFAULT_ENV_PORTAL_ID,
                    ENVIRONMENT,
                    ORG_ID,
                    "Automation Portal",
                    PortalNavigationStructure.empty(),
                    null,
                    false,
                    "only-portal-access-point"
                )
            )
        );

        var response = bootstrap().request().get();

        assertThat(response.getStatus()).isEqualTo(OK_200);
        assertThat(response.readEntity(Map.class).get("portalId")).isEqualTo(DEFAULT_ENV_PORTAL_ID.toString());
    }

    @Test
    void should_return_the_default_portal_when_the_base_url_is_built_from_the_request() {
        when(installationAccessQueryService.getPortalAPIUrl(ENVIRONMENT)).thenReturn(null);
        portalCrudService.initWith(List.of(aDefaultPortal(DEFAULT_ENV_PORTAL_ID, ENVIRONMENT)));

        var response = bootstrap().request().get();

        assertThat(response.getStatus()).isEqualTo(OK_200);
        var bootstrap = response.readEntity(Map.class);
        assertThat(bootstrap.get("portalId")).isEqualTo(DEFAULT_ENV_PORTAL_ID.toString());
        assertThat(bootstrap.get("environmentId")).isEqualTo(ENVIRONMENT);
        assertThat(bootstrap.get("organizationId")).isEqualTo(ORG_ID);
        assertThat(bootstrap.get("baseURL")).isEqualTo("https://apim.acme.io/portal");
    }

    @Test
    void should_return_the_portal_on_the_host_with_its_environment_and_organization() {
        portalCrudService.initWith(
            List.of(
                aDefaultPortal(DEFAULT_ENV_PORTAL_ID, ENVIRONMENT),
                aPortalOn(OTHER_ENV_PORTAL_ID, OTHER_ENV_ID, OTHER_ORG_ID, "http://partners.localhost:4100/next")
            )
        );

        var response = bootstrap().queryParam("host", "Partners.localhost:4100").request().header(NO_ENVIRONMENT_HEADER, "true").get();

        assertThat(response.getStatus()).isEqualTo(OK_200);
        var bootstrap = response.readEntity(Map.class);
        assertThat(bootstrap.get("portalId")).isEqualTo(OTHER_ENV_PORTAL_ID.toString());
        assertThat(bootstrap.get("environmentId")).isEqualTo(OTHER_ENV_ID);
        assertThat(bootstrap.get("organizationId")).isEqualTo(OTHER_ORG_ID);
    }

    @Test
    void should_return_the_default_portal_when_no_portal_is_on_the_host() {
        portalCrudService.initWith(List.of(aDefaultPortal(DEFAULT_ENV_PORTAL_ID, ENVIRONMENT)));

        var response = bootstrap().queryParam("host", "127.0.0.1:4100").request().get();

        assertThat(response.getStatus()).isEqualTo(OK_200);
        assertThat(response.readEntity(Map.class).get("portalId")).isEqualTo(DEFAULT_ENV_PORTAL_ID.toString());
    }

    @Test
    void should_return_no_portal_when_no_portal_is_on_the_host_and_the_only_portal_is_not_the_default() {
        portalCrudService.initWith(List.of(aPortalOn(DEFAULT_ENV_PORTAL_ID, ENVIRONMENT, ORG_ID, "https://portal.acme.io/next")));

        var response = bootstrap().queryParam("host", "127.0.0.1:4100").request().get();

        assertThat(response.getStatus()).isEqualTo(OK_200);
        assertThat(response.readEntity(Map.class).get("portalId")).isNull();
    }

    @Test
    void should_return_the_default_portal_when_the_host_is_ambiguous() {
        portalCrudService.initWith(
            List.of(
                aDefaultPortal(DEFAULT_ENV_PORTAL_ID, ENVIRONMENT),
                aPortalOn(OTHER_ENV_PORTAL_ID, OTHER_ENV_ID, ORG_ID, "https://portal.acme.io/other")
            )
        );

        var response = bootstrap().queryParam("host", "portal.acme.io").request().header(NO_ENVIRONMENT_HEADER, "true").get();

        assertThat(response.getStatus()).isEqualTo(OK_200);
        assertThat(response.readEntity(Map.class).get("portalId")).isEqualTo(DEFAULT_ENV_PORTAL_ID.toString());
    }

    @Test
    void should_not_leave_the_requested_environment_for_a_portal_on_the_host() {
        portalCrudService.initWith(
            List.of(
                aDefaultPortal(OTHER_ENV_PORTAL_ID, OTHER_ENV_ID),
                aPortalOn(DEFAULT_ENV_PORTAL_ID, ENVIRONMENT, ORG_ID, "http://partners.localhost:4100/next")
            )
        );

        var response = bootstrap().queryParam("environmentId", OTHER_ENV_ID).queryParam("host", "partners.localhost:4100").request().get();

        assertThat(response.getStatus()).isEqualTo(OK_200);
        var bootstrap = response.readEntity(Map.class);
        assertThat(bootstrap.get("portalId")).isEqualTo(OTHER_ENV_PORTAL_ID.toString());
        assertThat(bootstrap.get("environmentId")).isEqualTo(OTHER_ENV_ID);
    }

    @Test
    void should_not_search_other_tenants_when_a_multi_tenant_request_has_no_environment() {
        when(installationTypeDomainService.isMultiTenant()).thenReturn(true);
        portalCrudService.initWith(
            List.of(
                aDefaultPortal(DEFAULT_ENV_PORTAL_ID, ENVIRONMENT),
                aPortalOn(OTHER_ENV_PORTAL_ID, OTHER_ENV_ID, OTHER_ORG_ID, "http://partners.localhost:4100/next")
            )
        );

        var response = bootstrap().queryParam("host", "partners.localhost:4100").request().header(NO_ENVIRONMENT_HEADER, "true").get();

        assertThat(response.getStatus()).isEqualTo(OK_200);
        var bootstrap = response.readEntity(Map.class);
        assertThat(bootstrap.get("portalId")).isEqualTo(DEFAULT_ENV_PORTAL_ID.toString());
        assertThat(bootstrap.get("organizationId")).isEqualTo(ORG_ID);
    }

    @Test
    void should_refuse_a_malformed_host() {
        var response = bootstrap().queryParam("host", "portal.acme.io/next").request().get();

        assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);
    }

    // The bootstrap is served at the application root, outside /environments/{envId}.
    private WebTarget bootstrap() {
        return root().path("ui/bootstrap");
    }

    private Portal aPortalOn(PortalId id, String environmentId, String organizationId, String url) {
        var accessPointId = givenPortalAccessPoint(id, environmentId, url);
        return Portal.of(id, environmentId, organizationId, "Partners", PortalNavigationStructure.empty(), null, false, accessPointId);
    }

    private Portal aDefaultPortal(PortalId id, String environmentId) {
        var accessPointId = givenPortalAccessPoint(id, environmentId, "https://portal.acme.io/next");
        return Portal.of(id, environmentId, ORG_ID, "Developer Portal", PortalNavigationStructure.empty(), null, true, accessPointId);
    }

    @SneakyThrows
    private String givenPortalAccessPoint(PortalId portalId, String environmentId, String url) {
        var parsed = URI.create(url).toURL();
        var accessPoint = AccessPoint.builder()
            .id("ap-" + portalId + "-" + environmentId)
            .referenceType(AccessPoint.ReferenceType.ENVIRONMENT)
            .referenceId(environmentId)
            .target(AccessPoint.Target.PORTAL_INSTANCE)
            .host(PortalHost.of(parsed).value())
            .secured("https".equals(parsed.getProtocol()))
            .build();
        givenAccessPoints.add(accessPoint);
        accessPointQueryService.initWith(givenAccessPoints);
        return accessPoint.getId();
    }
}
