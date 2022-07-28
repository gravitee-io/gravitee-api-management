/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.jupiter.handlers.api.processor.cors;

import static io.gravitee.gateway.api.http.HttpHeaderNames.ACCESS_CONTROL_REQUEST_METHOD;
import static io.gravitee.gateway.api.http.HttpHeaderNames.ORIGIN;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.definition.model.Cors;
import io.gravitee.definition.model.Proxy;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.handlers.api.processor.cors.CorsPreflightInvoker;
import io.gravitee.gateway.jupiter.api.context.ExecutionContext;
import io.gravitee.gateway.jupiter.core.context.interruption.InterruptionException;
import io.gravitee.gateway.jupiter.handlers.api.processor.AbstractProcessorTest;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
class CorsPreflightRequestProcessorTest extends AbstractProcessorTest {

    private CorsPreflightRequestProcessor corsPreflightRequestProcessor;

    @BeforeEach
    public void beforeEach() {
        corsPreflightRequestProcessor = CorsPreflightRequestProcessor.instance();
        lenient().when(mockRequest.method()).thenReturn(HttpMethod.OPTIONS);
        Proxy proxy = new Proxy();
        Cors cors = new Cors();
        cors.setEnabled(true);
        cors.setAccessControlAllowOrigin(Set.of("*"));
        cors.setAccessControlAllowMethods(Set.of("GET"));
        proxy.setCors(cors);
        api.getDefinition().setProxy(proxy);
    }

    @Test
    public void shouldInterruptWithDefaultHeadersWhenCorsEnabledAndValidRequest() {
        spyRequestHeaders.set(ORIGIN, "origin");
        spyRequestHeaders.set(ACCESS_CONTROL_REQUEST_METHOD, "GET");
        corsPreflightRequestProcessor.execute(ctx).test().assertError(InterruptionException.class);
        verify(mockMetrics, times(1)).setApplication(eq("1"));
        verify(mockResponse, times(2)).headers();

        verify(spyResponseHeaders, times(2)).set(any(), anyString());
        assertThat(spyResponseHeaders.get(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("origin");
        assertThat(spyResponseHeaders.get(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS)).isEqualTo("GET");
        verify(mockResponse, times(1)).status(eq(200));
        assertThat(ctx.<Boolean>getAttribute("skip-security-chain")).isNull();
        assertThat(ctx.<CorsPreflightInvoker>getAttribute(ExecutionContext.ATTR_INVOKER)).isNull();
    }

    @Test
    public void shouldInterruptWithCredentialsWhenCorsEnabledAndValidRequest() {
        api.getDefinition().getProxy().getCors().setAccessControlAllowCredentials(true);
        spyRequestHeaders.set(ORIGIN, "origin");
        spyRequestHeaders.set(ACCESS_CONTROL_REQUEST_METHOD, "GET");
        corsPreflightRequestProcessor.execute(ctx).test().assertError(InterruptionException.class);
        verify(mockMetrics, times(1)).setApplication(eq("1"));
        verify(mockResponse, times(3)).headers();
        verify(spyResponseHeaders, times(3)).set(any(), anyString());
        assertThat(spyResponseHeaders.get(HttpHeaderNames.ACCESS_CONTROL_ALLOW_CREDENTIALS)).isEqualTo("true");
        assertThat(spyResponseHeaders.get(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("origin");
        assertThat(spyResponseHeaders.get(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS)).isEqualTo("GET");
        verify(mockResponse, times(1)).status(eq(200));
        assertThat(ctx.<Boolean>getAttribute("skip-security-chain")).isNull();
        assertThat(ctx.<CorsPreflightInvoker>getAttribute(ExecutionContext.ATTR_INVOKER)).isNull();
    }

    @Test
    public void shouldInterruptWithControlMaxAgeHeaderWhenCorsEnabledAndValidRequest() {
        api.getDefinition().getProxy().getCors().setAccessControlMaxAge(10);
        spyRequestHeaders.set(ORIGIN, "origin");
        spyRequestHeaders.set(ACCESS_CONTROL_REQUEST_METHOD, "GET");
        corsPreflightRequestProcessor.execute(ctx).test().assertError(InterruptionException.class);
        verify(mockMetrics, times(1)).setApplication(eq("1"));
        verify(mockResponse, times(3)).headers();
        verify(spyResponseHeaders, times(3)).set(any(), anyString());
        assertThat(spyResponseHeaders.get(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("origin");
        assertThat(spyResponseHeaders.get(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS)).isEqualTo("GET");
        assertThat(spyResponseHeaders.get(HttpHeaderNames.ACCESS_CONTROL_MAX_AGE)).isEqualTo("10");
        verify(mockResponse, times(1)).status(eq(200));
        assertThat(ctx.<Boolean>getAttribute("skip-security-chain")).isNull();
        assertThat(ctx.<CorsPreflightInvoker>getAttribute(ExecutionContext.ATTR_INVOKER)).isNull();
    }

    @Test
    public void shouldInterruptWithAccessControlAllowHeadersWhenCorsEnabledAndValidRequest() {
        Set<String> accessControlAllowHeaders = Set.of("X-Test", "X-Test-2");
        api.getDefinition().getProxy().getCors().setAccessControlAllowHeaders(accessControlAllowHeaders);
        spyRequestHeaders.set(ORIGIN, "origin");
        spyRequestHeaders.set(ACCESS_CONTROL_REQUEST_METHOD, "GET");
        corsPreflightRequestProcessor.execute(ctx).test().assertError(InterruptionException.class);
        verify(mockMetrics, times(1)).setApplication(eq("1"));
        verify(mockResponse, times(3)).headers();
        verify(spyResponseHeaders, times(3)).set(any(), anyString());
        assertThat(spyResponseHeaders.get(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("origin");
        assertThat(spyResponseHeaders.get(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS)).isEqualTo("GET");
        assertThat(spyResponseHeaders.get(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS))
            .isEqualTo(String.join(", ", accessControlAllowHeaders));
        verify(mockResponse, times(1)).status(eq(200));
        assertThat(ctx.<Boolean>getAttribute("skip-security-chain")).isNull();
        assertThat(ctx.<CorsPreflightInvoker>getAttribute(ExecutionContext.ATTR_INVOKER)).isNull();
    }

    @Test
    public void shouldCompleteWithoutChangingResponseWhenCorsDisabled() {
        api.getDefinition().getProxy().getCors().setEnabled(false);
        corsPreflightRequestProcessor.execute(ctx).test().assertResult();
        verifyNoInteractions(mockMetrics);
        verifyNoInteractions(mockResponse);
    }

    @Test
    public void shouldCompleteWithoutAddingHeadersWhenCorsEnableButNotOptionsMethod() {
        when(mockRequest.method()).thenReturn(HttpMethod.GET);
        spyRequestHeaders.set(ORIGIN, "origin");
        spyRequestHeaders.set(ACCESS_CONTROL_REQUEST_METHOD, "GET");
        corsPreflightRequestProcessor.execute(ctx).test().assertResult();
        verifyNoInteractions(mockMetrics);
        verifyNoInteractions(mockResponse);
    }

    @Test
    public void shouldCompleteWithoutAddingHeadersWhenCorsEnableButNoOrigin() {
        spyRequestHeaders.set(ACCESS_CONTROL_REQUEST_METHOD, "GET");
        corsPreflightRequestProcessor.execute(ctx).test().assertResult();
        verifyNoInteractions(mockMetrics);
        verifyNoInteractions(mockResponse);
    }

    @Test
    public void shouldCompleteWithoutAddingHeadersWhenCorsEnableButNoHeaderMethod() {
        spyRequestHeaders.set(ORIGIN, "origin");
        corsPreflightRequestProcessor.execute(ctx).test().assertResult();
        verifyNoInteractions(mockMetrics);
        verifyNoInteractions(mockResponse);
    }
}
