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

import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import inmemory.PortalCrudServiceInMemory;
import io.gravitee.apim.core.installation.query_service.InstallationAccessQueryService;
import io.gravitee.apim.core.portal.model.Portal;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal.model.PortalNavigationStructure;
import io.gravitee.rest.api.model.EnvironmentEntity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.client.WebTarget;
import java.net.URI;
import java.net.URL;
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
    private static final String PORTAL_API_URL = "https://api.acme.io/portal";
    private static final PortalId DEFAULT_ENV_PORTAL_ID = PortalId.of("11111111-1111-1111-1111-111111111111");
    private static final String REQUEST_URL = "https://apim.acme.io/portal/ui/bootstrap";
    private static final PortalId OTHER_ENV_PORTAL_ID = PortalId.of("22222222-2222-2222-2222-222222222222");

    @Autowired
    private PortalCrudServiceInMemory portalCrudService;

    @Autowired
    private InstallationAccessQueryService installationAccessQueryService;

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
    }

    @BeforeEach
    void init() {
        reset(installationAccessQueryService);
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

    // The bootstrap is served at the application root, outside /environments/{envId}.
    private WebTarget bootstrap() {
        return root().path("ui/bootstrap");
    }

    private static Portal aDefaultPortal(PortalId id, String environmentId) {
        return Portal.of(id, environmentId, ORG_ID, "Developer Portal", PortalNavigationStructure.empty(), null, true, url());
    }

    @SneakyThrows
    private static URL url() {
        return URI.create("https://portal.acme.io/next").toURL();
    }
}
