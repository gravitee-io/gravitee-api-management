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

        UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getBaseUriBuilder()).thenReturn(baseUriBuilder);
        when(uriInfo.getRequestUriBuilder()).thenReturn(requestUriBuilder);
        when(containerRequestContext.getUriInfo()).thenReturn(uriInfo);
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
}
