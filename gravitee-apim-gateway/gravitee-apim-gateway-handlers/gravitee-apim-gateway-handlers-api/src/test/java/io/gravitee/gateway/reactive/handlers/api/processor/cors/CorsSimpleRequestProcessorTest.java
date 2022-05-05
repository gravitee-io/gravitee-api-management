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
package io.gravitee.gateway.reactive.handlers.api.processor.cors;

import static io.gravitee.gateway.api.http.HttpHeaderNames.ORIGIN;
import static io.gravitee.gateway.reactive.handlers.api.processor.cors.AbstractCorsRequestProcessor.ALLOW_ORIGIN_PUBLIC_WILDCARD;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.definition.model.Cors;
import io.gravitee.definition.model.Proxy;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.reactive.api.context.Request;
import io.gravitee.gateway.reactive.api.context.Response;
import io.gravitee.gateway.reactive.handlers.api.processor.AbstractProcessorTest;
import io.gravitee.gateway.reactive.reactor.handler.context.DefaultRequestExecutionContext;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
class CorsSimpleRequestProcessorTest extends AbstractProcessorTest {

    private CorsSimpleRequestProcessor corsSimpleRequestProcessor;

    @BeforeEach
    public void beforeEach() {
        corsSimpleRequestProcessor = new CorsSimpleRequestProcessor();
        lenient().when(mockRequest.method()).thenReturn(HttpMethod.OPTIONS);
        Proxy proxy = new Proxy();
        Cors cors = new Cors();
        cors.setEnabled(true);
        cors.setAccessControlAllowOrigin(Set.of("*"));
        cors.setAccessControlAllowMethods(Set.of("GET"));
        proxy.setCors(cors);
        api.setProxy(proxy);
    }

    @Test
    public void shouldCompleteWithDefaultHeadersWhenCorsEnabledAndValidRequest() {
        spyRequestHeaders.set(ORIGIN, "origin");
        corsSimpleRequestProcessor.execute(ctx).test().assertResult();
        verify(mockResponse, times(1)).headers();
        verify(spyResponseHeaders, times(1)).set(any(), anyString());
        assertThat(spyResponseHeaders.get(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo(ALLOW_ORIGIN_PUBLIC_WILDCARD);
    }

    @Test
    public void shouldCompleteWithClientCredentialsHeaderWhenCorsEnabledAndValidRequest() {
        api.getProxy().getCors().setAccessControlAllowCredentials(true);
        spyRequestHeaders.set(ORIGIN, "origin");
        corsSimpleRequestProcessor.execute(ctx).test().assertResult();
        verify(mockResponse, times(2)).headers();
        verify(spyResponseHeaders, times(2)).set(any(), anyString());
        assertThat(spyResponseHeaders.get(HttpHeaderNames.ACCESS_CONTROL_ALLOW_CREDENTIALS)).isEqualTo("true");
        assertThat(spyResponseHeaders.get(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("origin");
    }

    @Test
    public void shouldCompleteWithoutChangingResponseWhenCorsDisabled() {
        api.getProxy().getCors().setEnabled(false);
        corsSimpleRequestProcessor.execute(ctx).test().assertResult();
        verifyNoInteractions(mockResponse);
    }

    @Test
    public void shouldCompleteAndRemoveCorsHeadersWhenCorsEnableButNoOrigin() {
        corsSimpleRequestProcessor.execute(ctx).test().assertResult();
        assertThat(spyResponseHeaders.contains(HttpHeaderNames.ACCESS_CONTROL_ALLOW_CREDENTIALS)).isFalse();
        assertThat(spyResponseHeaders.contains(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN)).isFalse();
        assertThat(spyResponseHeaders.contains(HttpHeaderNames.ACCESS_CONTROL_EXPOSE_HEADERS)).isFalse();
        verify(spyResponseHeaders).remove(eq(HttpHeaderNames.ACCESS_CONTROL_ALLOW_CREDENTIALS));
        verify(spyResponseHeaders).remove(eq(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN));
        verify(spyResponseHeaders).remove(eq(HttpHeaderNames.ACCESS_CONTROL_EXPOSE_HEADERS));
    }

    @Test
    public void shouldCompleteAndRemoveCorsHeadersWhenCorsEnableButOriginNotAllowed() {
        api.getProxy().getCors().setAccessControlAllowOrigin(Set.of("origin_allowed"));
        spyRequestHeaders.set(ORIGIN, "origin_not_allowed");
        corsSimpleRequestProcessor.execute(ctx).test().assertResult();
        assertThat(spyResponseHeaders.contains(HttpHeaderNames.ACCESS_CONTROL_ALLOW_CREDENTIALS)).isFalse();
        assertThat(spyResponseHeaders.contains(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN)).isFalse();
        assertThat(spyResponseHeaders.contains(HttpHeaderNames.ACCESS_CONTROL_EXPOSE_HEADERS)).isFalse();
        verify(spyResponseHeaders).remove(eq(HttpHeaderNames.ACCESS_CONTROL_ALLOW_CREDENTIALS));
        verify(spyResponseHeaders).remove(eq(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN));
        verify(spyResponseHeaders).remove(eq(HttpHeaderNames.ACCESS_CONTROL_EXPOSE_HEADERS));
    }
}
