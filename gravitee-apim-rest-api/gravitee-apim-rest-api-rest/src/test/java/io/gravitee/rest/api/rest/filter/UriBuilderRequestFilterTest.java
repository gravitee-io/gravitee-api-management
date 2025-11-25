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
package io.gravitee.rest.api.rest.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import io.gravitee.apim.core.installation.query_service.InstallationAccessQueryService;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.Invocation;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class UriBuilderRequestFilterTest {

    @InjectMocks
    protected UriBuilderRequestFilter filter;

    @Mock
    protected ContainerRequestContext containerRequestContext;

    @Mock
    protected UriBuilder baseUriBuilder;

    @Mock
    protected UriBuilder requestUriBuilder;

    @Mock
    protected InstallationAccessQueryService installationAccessQueryService;

    private UriInfo uriInfo;

    private static final int PROTOCOL_DEFAULT_PORT = -1;

    @Before
    public void setUp() {
        initMocks(this);
        setupBuildersMocks();
    }

    @Test
    public void noForwardedHeadersCausesNoBuilderInvocations() throws IOException {
        givenHeaders(); // no X-Forwarded-* headers

        filter.filter(containerRequestContext);

        verifyUriBuildersKeptOriginalScheme();
        verifyUriBuildersKeptOriginalHost();
        verifyUriBuildersKeptOriginalPort();
    }

    @Test
    public void protoHeaderCausesUriBuildersSchemeSet() throws IOException {
        givenHeaders("X-Forwarded-Proto", "https");

        filter.filter(containerRequestContext);

        verifyUriBuildersChangedSchemeTo("https"); // override with Proto header
        verifyUriBuildersKeptOriginalHost();
        verifyUriBuildersKeptOriginalPort();
    }

    @Test
    public void hostHeaderWithoutPortCausesUriBuildersHostSetAndPortReset() throws IOException {
        givenHeaders("X-Forwarded-Host", "gravitee.io");

        filter.filter(containerRequestContext);

        verifyUriBuildersKeptOriginalScheme();
        verifyUriBuildersChangedHostTo("gravitee.io"); // override with Host header
        verifyUriBuildersChangedPortTo(PROTOCOL_DEFAULT_PORT); // reset explicit port
    }

    @Test
    public void portHeaderCausesUriBuildersPortSet() throws IOException {
        givenHeaders("X-Forwarded-Port", "1234");

        filter.filter(containerRequestContext);

        verifyUriBuildersKeptOriginalScheme();
        verifyUriBuildersKeptOriginalHost();
        verifyUriBuildersChangedPortTo(1234); // override with Port header
    }

    @Test
    public void hostHeaderWithPortCausesUriBuildersHostSetAndPortSet() throws IOException {
        givenHeaders("X-Forwarded-Host", "gravitee.io:4321");

        filter.filter(containerRequestContext);

        verifyUriBuildersKeptOriginalScheme();
        verifyUriBuildersChangedHostTo("gravitee.io"); // override with Host header
        verifyUriBuildersChangedPortTo(4321); // override with port in Host header
    }

    @Test
    public void hostHeaderWithoutPortAndPortHeaderCauseUriBuildersHostSetAndPortSet() throws IOException {
        givenHeaders("X-Forwarded-Host", "gravitee.io", "X-Forwarded-Port", "1234");

        filter.filter(containerRequestContext);

        verifyUriBuildersKeptOriginalScheme();
        verifyUriBuildersChangedHostTo("gravitee.io"); // override with Host header
        verifyUriBuildersChangedPortTo(1234); // reset explicit port but then override with Port header
    }

    @Test
    public void hostHeaderWithPortAndPortHeaderCauseUriBuildersHostSetAndPortSetFromPortHeader() throws IOException {
        givenHeaders("X-Forwarded-Host", "gravitee.io:4321", "X-Forwarded-Port", "1234");

        filter.filter(containerRequestContext);

        verifyUriBuildersKeptOriginalScheme();
        verifyUriBuildersChangedHostTo("gravitee.io"); // override with Host header
        verifyUriBuildersChangedPortTo(1234); // override with port in Host header but then override with Port header
    }

    @Test
    public void protoHeaderAndHostHeaderWithoutPortCauseUriBuildersSchemeSetHostSetAndPortReset() throws IOException {
        givenHeaders("X-Forwarded-Proto", "https", "X-Forwarded-Host", "gravitee.io");

        filter.filter(containerRequestContext);

        verifyUriBuildersChangedSchemeTo("https");
        verifyUriBuildersChangedHostTo("gravitee.io"); // override with Host header
        verifyUriBuildersChangedPortTo(PROTOCOL_DEFAULT_PORT); // reset explicit port
    }

    @Test
    public void protoHeaderAndHostHeaderWithPortCauseUriBuildersSchemeSetHostSetAndPortSet() throws IOException {
        givenHeaders("X-Forwarded-Proto", "https", "X-Forwarded-Host", "gravitee.io:4321");

        filter.filter(containerRequestContext);

        verifyUriBuildersChangedSchemeTo("https");
        verifyUriBuildersChangedHostTo("gravitee.io"); // override with Host header
        verifyUriBuildersChangedPortTo(4321); // override with port in Host header
    }

    @Test
    public void protoHeaderAndPortHeaderCauseUriBuildersSchemeSetAndPortSet() throws IOException {
        givenHeaders("X-Forwarded-Proto", "https", "X-Forwarded-Port", "1234");

        filter.filter(containerRequestContext);

        verifyUriBuildersChangedSchemeTo("https");
        verifyUriBuildersKeptOriginalHost();
        verifyUriBuildersChangedPortTo(1234); // override with Port header
    }

    @Test
    public void protoHeaderHostHeaderWithoutPortAndPortHeaderCauseUriBuildersSchemeSetHostSetAndPortSet() throws IOException {
        givenHeaders("X-Forwarded-Proto", "https", "X-Forwarded-Host", "gravitee.io", "X-Forwarded-Port", "1234");

        filter.filter(containerRequestContext);

        verifyUriBuildersChangedSchemeTo("https");
        verifyUriBuildersChangedHostTo("gravitee.io"); // override with Host header
        verifyUriBuildersChangedPortTo(1234); // reset explicit port but then override with Port header
    }

    @Test
    public void protoHeaderHostHeaderWithPortAndPortHeaderCauseUriBuildersSchemeSetHostSetAndPortSet() throws IOException {
        givenHeaders("X-Forwarded-Proto", "https", "X-Forwarded-Host", "gravitee.io:4321", "X-Forwarded-Port", "1234");

        filter.filter(containerRequestContext);

        verifyUriBuildersChangedSchemeTo("https");
        verifyUriBuildersChangedHostTo("gravitee.io"); // override with Host header
        verifyUriBuildersChangedPortTo(1234); // override with port in Host header but then override with Port header
    }

    @Test
    public void protoHeaderHostHeaderWithMultipleHostsCauseUriBuildersSchemeSetHostSetToOneHost() throws IOException {
        givenHeaders("X-Forwarded-Proto", "https", "X-Forwarded-Host", "gravitee.io,gravitee.io");

        filter.filter(containerRequestContext);

        verifyUriBuildersChangedSchemeTo("https");
        verifyUriBuildersChangedHostTo("gravitee.io"); // override with Host header
        verifyUriBuildersChangedPortTo(PROTOCOL_DEFAULT_PORT); // override with port in Host header but then override with Port header
    }

    @Test
    public void protoHeaderHostHeaderWithMultipleHostsAndPortCauseUriBuildersSchemeSetHostSetToOneHost() throws IOException {
        givenHeaders("X-Forwarded-Proto", "https", "X-Forwarded-Host", "gravitee.io,gravitee.io:8443");

        filter.filter(containerRequestContext);

        verifyUriBuildersChangedSchemeTo("https");
        verifyUriBuildersChangedHostTo("gravitee.io"); // override with Host header
        verifyUriBuildersChangedPortTo(8443); // override with port in Host header but then override with Port header
    }

    @Test
    public void protoHeaderHostHeaderWithMultipleHostsAndPortOverriddenCauseUriBuildersSchemeSetHostSetToOneHost() throws IOException {
        givenHeaders("X-Forwarded-Proto", "https", "X-Forwarded-Host", "gravitee.io,gravitee.io:8443", "X-Forwarded-Port", "443");

        filter.filter(containerRequestContext);

        verifyUriBuildersChangedSchemeTo("https");
        verifyUriBuildersChangedHostTo("gravitee.io"); // override with Host header
        verifyUriBuildersChangedPortTo(443); // override with port in Host header but then override with Port header
    }

    private void givenHeaders(String... headers) {
        MultivaluedHashMap<String, String> mockHeaders = new MultivaluedHashMap<>();
        for (int i = 0; i < headers.length / 2; i++) {
            String hName = headers[2 * i];
            String hValue = headers[(2 * i) + 1];
            mockHeaders.put(hName, Collections.singletonList(hValue));
        }
        when(containerRequestContext.getHeaders()).thenReturn(mockHeaders);
    }

    private void setupBuildersMocks() {
        when(baseUriBuilder.host(any())).thenReturn(baseUriBuilder); // in case of chaining builder calls
        when(requestUriBuilder.host(any())).thenReturn(requestUriBuilder); // in case of chaining builder calls

        uriInfo = mock(UriInfo.class);
        when(uriInfo.getBaseUriBuilder()).thenReturn(baseUriBuilder);
        when(uriInfo.getRequestUriBuilder()).thenReturn(requestUriBuilder);

        // Mock absolute path for API context detection - default to UNKNOWN context
        givenApiContext("/unknown/path");

        when(containerRequestContext.getUriInfo()).thenReturn(uriInfo);
    }

    private void givenApiContext(String absolutePath) {
        java.net.URI uri = mock(java.net.URI.class);
        when(uri.getPath()).thenReturn(absolutePath);
        when(uriInfo.getAbsolutePath()).thenReturn(uri);
    }

    private void givenPortalContext() {
        givenApiContext("/portal/environments/DEFAULT");
    }

    private void givenManagementContext() {
        givenApiContext("/management/organizations/DEFAULT");
    }

    private void verifyUriBuildersKeptOriginalScheme() {
        for (UriBuilder uriBuilder : new UriBuilder[] { baseUriBuilder, requestUriBuilder }) {
            verify(uriBuilder, never()).scheme(anyString());
        }
    }

    private void verifyUriBuildersKeptOriginalHost() {
        for (UriBuilder uriBuilder : new UriBuilder[] { baseUriBuilder, requestUriBuilder }) {
            verify(uriBuilder, never()).host(anyString());
        }
    }

    private void verifyUriBuildersKeptOriginalPort() {
        for (UriBuilder uriBuilder : new UriBuilder[] { baseUriBuilder, requestUriBuilder }) {
            verify(uriBuilder, never()).port(anyInt());
        }
    }

    private void verifyUriBuildersChangedSchemeTo(String expectedScheme) {
        for (UriBuilder uriBuilder : new UriBuilder[] { baseUriBuilder, requestUriBuilder }) {
            String actualScheme = getMethodArgBeforeLastBuild(uriBuilder, "scheme", String.class);
            assertNotNull(actualScheme);
            assertEquals(expectedScheme, actualScheme);
        }
    }

    private void verifyUriBuildersChangedHostTo(String expectedHost) {
        for (UriBuilder uriBuilder : new UriBuilder[] { baseUriBuilder, requestUriBuilder }) {
            String actualHost = getMethodArgBeforeLastBuild(uriBuilder, "host", String.class);
            assertNotNull(actualHost);
            assertEquals(expectedHost, actualHost);
        }
    }

    private void verifyUriBuildersChangedPortTo(int expectedPort) {
        for (UriBuilder uriBuilder : new UriBuilder[] { baseUriBuilder, requestUriBuilder }) {
            Integer actualPort = getMethodArgBeforeLastBuild(uriBuilder, "port", Integer.class);
            assertNotNull(actualPort);
            assertEquals(expectedPort, actualPort.intValue());
        }
    }

    private <T> T getMethodArgBeforeLastBuild(Object mock, String methodName, Class<T> argClass) {
        List<Invocation> invocations = sortedInvocations(mock);
        Collections.reverse(invocations);
        boolean buildFound = false;
        for (Invocation invocation : invocations) {
            String invocationMethodName = invocation.getMethod().getName();
            if ("build".equals(invocationMethodName)) {
                buildFound = true;
                continue;
            }
            if (buildFound && methodName.equals(invocationMethodName)) {
                return argClass.cast(invocation.getArgument(0));
            }
        }
        return null;
    }

    private List<Invocation> sortedInvocations(Object mock) {
        // mocking details invocations seem sorted but API returns Collections so we better sort using sequence number
        List<Invocation> invocations = new ArrayList<>(mockingDetails(mock).getInvocations());
        invocations.sort(Comparator.comparingInt(Invocation::getSequenceNumber));
        return invocations;
    }

    @Test
    public void prefixHeaderWithoutTrailingSlashCausesUriBuildersPathSet() throws IOException {
        givenHeaders("X-Forwarded-Prefix", "/api");
        givenBuilderPaths("/portal/resource", "/portal/resource/123");

        filter.filter(containerRequestContext);

        verifyUriBuildersChangedPathTo("/api/portal/resource", "/api/portal/resource/123");
    }

    @Test
    public void prefixHeaderWithTrailingSlashNormalizesToAvoidDoubleSlashes() throws IOException {
        givenHeaders("X-Forwarded-Prefix", "/api/");
        givenBuilderPaths("/portal/resource", "/portal/resource/123");

        filter.filter(containerRequestContext);

        // Should normalize: "/api/" + "/portal/resource" => "/api/portal/resource" (not "/api//portal/resource")
        verifyUriBuildersChangedPathTo("/api/portal/resource", "/api/portal/resource/123");
    }

    @Test
    public void prefixHeaderWithMultipleTrailingSlashesNormalizesToAvoidDoubleSlashes() throws IOException {
        givenHeaders("X-Forwarded-Prefix", "/api/v1/");
        givenBuilderPaths("/management/orgs", "/management/orgs/DEFAULT");

        filter.filter(containerRequestContext);

        verifyUriBuildersChangedPathTo("/api/v1/management/orgs", "/api/v1/management/orgs/DEFAULT");
    }

    @Test
    public void prefixHeaderWithEmptyPathHandledCorrectly() throws IOException {
        givenHeaders("X-Forwarded-Prefix", "/prefix/");
        givenBuilderPaths("", "");

        filter.filter(containerRequestContext);

        verifyUriBuildersChangedPathTo("/prefix", "/prefix");
    }

    @Test
    public void prefixHeaderWithNullPathHandledCorrectly() throws IOException {
        givenHeaders("X-Forwarded-Prefix", "/prefix");
        givenBuilderPaths(null, null);

        filter.filter(containerRequestContext);

        verifyUriBuildersChangedPathTo("/prefix", "/prefix");
    }

    @Test
    public void prefixHeaderWithPathNotStartingWithSlashNormalized() throws IOException {
        givenHeaders("X-Forwarded-Prefix", "/api");
        givenBuilderPaths("resource", "resource/123");

        filter.filter(containerRequestContext);

        // Should normalize: "/api" + "resource" => "/api/resource"
        verifyUriBuildersChangedPathTo("/api/resource", "/api/resource/123");
    }

    private void givenBuilderPaths(String basePath, String requestPath) {
        java.net.URI baseUri = mock(java.net.URI.class);
        java.net.URI requestUri = mock(java.net.URI.class);
        when(baseUri.getPath()).thenReturn(basePath);
        when(requestUri.getPath()).thenReturn(requestPath);
        when(baseUriBuilder.build()).thenReturn(baseUri);
        when(requestUriBuilder.build()).thenReturn(requestUri);
    }

    private void verifyUriBuildersChangedPathTo(String expectedBasePath, String expectedRequestPath) {
        String actualBasePath = getMethodArgBeforeLastBuild(baseUriBuilder, "replacePath", String.class);
        String actualRequestPath = getMethodArgBeforeLastBuild(requestUriBuilder, "replacePath", String.class);
        assertNotNull("Base path should have been set", actualBasePath);
        assertNotNull("Request path should have been set", actualRequestPath);
        assertEquals("Base path should be normalized without double slashes", expectedBasePath, actualBasePath);
        assertEquals("Request path should be normalized without double slashes", expectedRequestPath, actualRequestPath);
    }

    @Test
    public void portalContextFallsBackToConfiguredSchemeWhenNoProtoHeader() throws IOException {
        givenHeaders(); // no headers
        givenPortalContext();
        when(installationAccessQueryService.getPortalAPIUrl(any())).thenReturn("https://portal.gravitee.io:8443/portal");

        filter.filter(containerRequestContext);

        verifyUriBuildersChangedSchemeTo("https");
    }

    @Test
    public void portalContextFallsBackToConfiguredHostWhenNoHostHeader() throws IOException {
        givenHeaders(); // no headers
        givenPortalContext();
        when(installationAccessQueryService.getPortalAPIUrl(any())).thenReturn("https://portal.gravitee.io:8443/portal");

        filter.filter(containerRequestContext);

        verifyUriBuildersChangedHostTo("portal.gravitee.io");
        verifyUriBuildersChangedPortTo(8443);
    }

    @Test
    public void portalContextFallsBackToConfiguredHostWithDefaultPortWhenNoHostHeader() throws IOException {
        givenHeaders(); // no headers
        givenPortalContext();
        when(installationAccessQueryService.getPortalAPIUrl(any())).thenReturn("https://portal.gravitee.io/portal");

        filter.filter(containerRequestContext);

        verifyUriBuildersChangedHostTo("portal.gravitee.io");
        verifyUriBuildersChangedPortTo(PROTOCOL_DEFAULT_PORT);
    }

    @Test
    public void portalContextFallsBackToConfiguredPathWhenNoPrefixHeader() throws IOException {
        givenHeaders(); // no headers
        givenPortalContext();
        givenBuilderPaths("/portal/environments", "/portal/environments/DEFAULT");
        when(installationAccessQueryService.getPortalAPIUrl(any())).thenReturn("https://portal.gravitee.io/api/portal");
        when(installationAccessQueryService.getPortalApiPath()).thenReturn("/api/portal");

        filter.filter(containerRequestContext);

        verifyUriBuildersChangedPathTo("/api/portal/environments", "/api/portal/environments/DEFAULT");
    }

    @Test
    public void portalContextFallsBackToConfiguredPathWithTrailingSlashNormalized() throws IOException {
        givenHeaders(); // no headers
        givenPortalContext();
        givenBuilderPaths("/portal/environments", "/portal/environments/DEFAULT");
        when(installationAccessQueryService.getPortalAPIUrl(any())).thenReturn("https://portal.gravitee.io/gateway/portal/");
        when(installationAccessQueryService.getPortalApiPath()).thenReturn("/gateway/portal/");

        filter.filter(containerRequestContext);

        // Path replacement: /portal -> /gateway/portal/, then remaining path /environments should be normalized
        verifyUriBuildersChangedPathTo("/gateway/portal/environments", "/gateway/portal/environments/DEFAULT");
    }

    @Test
    public void managementContextFallsBackToConfiguredSchemeWhenNoProtoHeader() throws IOException {
        givenHeaders(); // no headers
        givenManagementContext();
        when(installationAccessQueryService.getConsoleAPIUrl(any())).thenReturn("https://console.gravitee.io:8443/management");

        filter.filter(containerRequestContext);

        verifyUriBuildersChangedSchemeTo("https");
    }

    @Test
    public void managementContextFallsBackToConfiguredHostWhenNoHostHeader() throws IOException {
        givenHeaders(); // no headers
        givenManagementContext();
        when(installationAccessQueryService.getConsoleAPIUrl(any())).thenReturn("https://console.gravitee.io:9443/management");

        filter.filter(containerRequestContext);

        verifyUriBuildersChangedHostTo("console.gravitee.io");
        verifyUriBuildersChangedPortTo(9443);
    }

    @Test
    public void managementContextFallsBackToConfiguredHostWithDefaultPortWhenNoHostHeader() throws IOException {
        givenHeaders(); // no headers
        givenManagementContext();
        when(installationAccessQueryService.getConsoleAPIUrl(any())).thenReturn("https://console.gravitee.io/management");

        filter.filter(containerRequestContext);

        verifyUriBuildersChangedHostTo("console.gravitee.io");
        verifyUriBuildersChangedPortTo(PROTOCOL_DEFAULT_PORT);
    }

    @Test
    public void managementContextFallsBackToConfiguredPathWhenNoPrefixHeader() throws IOException {
        givenHeaders(); // no headers
        givenManagementContext();
        givenBuilderPaths("/management/organizations", "/management/organizations/DEFAULT");
        when(installationAccessQueryService.getConsoleAPIUrl(any())).thenReturn("https://console.gravitee.io/api/console");
        when(installationAccessQueryService.getConsoleApiPath()).thenReturn("/api/console");

        filter.filter(containerRequestContext);

        verifyUriBuildersChangedPathTo("/api/console/organizations", "/api/console/organizations/DEFAULT");
    }

    @Test
    public void managementContextFallsBackToConfiguredPathWithTrailingSlashNormalized() throws IOException {
        givenHeaders(); // no headers
        givenManagementContext();
        givenBuilderPaths("/management/organizations", "/management/organizations/DEFAULT");
        when(installationAccessQueryService.getConsoleAPIUrl(any())).thenReturn("https://console.gravitee.io/gateway/console/");
        when(installationAccessQueryService.getConsoleApiPath()).thenReturn("/gateway/console/");

        filter.filter(containerRequestContext);

        verifyUriBuildersChangedPathTo("/gateway/console/organizations", "/gateway/console/organizations/DEFAULT");
    }

    // Test that headers take precedence over fallback

    @Test
    public void portalContextHeadersTakePrecedenceOverFallback() throws IOException {
        givenHeaders("X-Forwarded-Proto", "http", "X-Forwarded-Host", "custom.host:1234");
        givenPortalContext();
        when(installationAccessQueryService.getPortalAPIUrl(any())).thenReturn("https://portal.gravitee.io:8443/portal");

        filter.filter(containerRequestContext);

        // Headers should take precedence
        verifyUriBuildersChangedSchemeTo("http");
        verifyUriBuildersChangedHostTo("custom.host");
        verifyUriBuildersChangedPortTo(1234);
    }

    @Test
    public void managementContextHeadersTakePrecedenceOverFallback() throws IOException {
        givenHeaders("X-Forwarded-Proto", "http", "X-Forwarded-Host", "custom.host:5678");
        givenManagementContext();
        when(installationAccessQueryService.getConsoleAPIUrl(any())).thenReturn("https://console.gravitee.io:9443/management");

        filter.filter(containerRequestContext);

        // Headers should take precedence
        verifyUriBuildersChangedSchemeTo("http");
        verifyUriBuildersChangedHostTo("custom.host");
        verifyUriBuildersChangedPortTo(5678);
    }
}
