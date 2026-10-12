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
package io.gravitee.apim.rest.api.mcp.security.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.access_point.query_service.AccessPointQueryService;
import io.gravitee.apim.core.installation.domain_service.InstallationTypeDomainService;
import io.gravitee.apim.core.installation.query_service.InstallationAccessQueryService;
import io.gravitee.rest.api.service.OrganizationService;
import io.gravitee.rest.api.service.common.ReferenceContext;
import io.gravitee.rest.api.service.exceptions.OrganizationNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class McpMountResolverTest {

    private final InstallationTypeDomainService installationType = mock(InstallationTypeDomainService.class);
    private final AccessPointQueryService accessPoints = mock(AccessPointQueryService.class);
    private final OrganizationService organizations = mock(OrganizationService.class);
    private final InstallationAccessQueryService installationAccess = mock(InstallationAccessQueryService.class);
    private final McpMountResolver resolver = new McpMountResolver(installationType, accessPoints, organizations, installationAccess);

    @BeforeEach
    void setUp() {
        when(installationAccess.getMcpAPIUrl(any())).thenReturn("https://apim.example.com/mcp");
        when(accessPoints.getReferenceContext(any())).thenReturn(Optional.empty());
    }

    @Test
    void should_serve_the_default_organization_on_the_bare_mount_of_a_standalone_installation() {
        assertThat(resolver.resolve("", "apim.example.com", 443)).contains(new McpMount("DEFAULT", false, "https://apim.example.com/mcp"));
    }

    @Test
    void should_serve_the_organization_named_in_the_path() {
        assertThat(resolver.resolve("/organizations/acme", "apim.example.com", 443)).contains(
            new McpMount("acme", true, "https://apim.example.com/mcp/organizations/acme")
        );
    }

    @Test
    void should_not_serve_an_organization_that_does_not_exist() {
        when(organizations.findById("ghost")).thenThrow(new OrganizationNotFoundException("ghost"));

        assertThat(resolver.resolve("/organizations/ghost", "apim.example.com", 443)).isEmpty();
    }

    @Test
    void should_not_serve_a_path_under_the_organization() {
        assertThat(resolver.resolve("/organizations/acme/environments/DEFAULT", "apim.example.com", 443)).isEmpty();
    }

    @Test
    void should_serve_the_organization_of_the_host_on_a_multi_tenant_installation() {
        when(installationType.isMultiTenant()).thenReturn(true);
        when(accessPoints.getReferenceContext("acme.apim.example.com")).thenReturn(
            Optional.of(new ReferenceContext(ReferenceContext.Type.ORGANIZATION, "acme"))
        );

        assertThat(resolver.resolve("/", "acme.apim.example.com", 443)).contains(
            new McpMount("acme", true, "https://apim.example.com/mcp")
        );
    }

    @Test
    void should_ignore_an_environment_access_point() {
        when(installationType.isMultiTenant()).thenReturn(true);
        when(accessPoints.getReferenceContext("dev.apim.example.com")).thenReturn(
            Optional.of(new ReferenceContext(ReferenceContext.Type.ENVIRONMENT, "dev"))
        );

        assertThat(resolver.resolve("", "dev.apim.example.com", 443)).contains(
            new McpMount("DEFAULT", false, "https://apim.example.com/mcp")
        );
    }

    @Test
    void should_not_serve_an_organization_other_than_the_one_of_the_host() {
        when(installationType.isMultiTenant()).thenReturn(true);
        when(accessPoints.getReferenceContext("acme.apim.example.com")).thenReturn(
            Optional.of(new ReferenceContext(ReferenceContext.Type.ORGANIZATION, "acme"))
        );

        assertThat(resolver.resolve("/organizations/globex", "acme.apim.example.com", 443)).isEmpty();
    }

    @Test
    void should_have_no_resource_when_no_api_url_is_configured() {
        when(installationAccess.getMcpAPIUrl(any())).thenReturn(null);

        assertThat(resolver.resolve("/organizations/acme", "apim.example.com", 443)).contains(new McpMount("acme", true, null));
    }
}
