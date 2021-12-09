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
package io.gravitee.gateway.handlers.api.processor.forward;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.context.MutableExecutionContext;
import io.gravitee.gateway.api.context.SimpleExecutionContext;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.api.http.HttpHeaders;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class XForwardedPrefixProcessorTest {

    public static final String CONTEXT_PATH = "/context_path";

    private MutableExecutionContext context;

    @Mock
    private Request request;

    @Mock
    private Response response;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        context = new SimpleExecutionContext(request, response);
        HttpHeaders headers = HttpHeaders.create();
        Mockito.when(request.headers()).thenReturn(headers);
    }

    @Test
    public void testNoXForwardedPrefixInHeader() {
        when(request.contextPath()).thenReturn(CONTEXT_PATH);

        new XForwardedPrefixProcessor()
            .handler(
                context -> {
                    List<String> xForwardedPrefixList = context.request().headers().getAll(HttpHeaderNames.X_FORWARDED_PREFIX);
                    assertEquals(xForwardedPrefixList.size(), 1);
                    assertEquals(xForwardedPrefixList.get(0), CONTEXT_PATH);
                }
            )
            .handle(context);
    }

    @Test
    public void testXForwardedPrefixInHeader() {
        when(request.contextPath()).thenReturn(CONTEXT_PATH);

        HttpHeaders headers = HttpHeaders.create();
        headers.add(HttpHeaderNames.X_FORWARDED_PREFIX, "randomPrefix");
        when(request.headers()).thenReturn(headers);

        new XForwardedPrefixProcessor()
            .handler(
                context -> {
                    List<String> xForwardedPrefixList = context.request().headers().getAll(HttpHeaderNames.X_FORWARDED_PREFIX);
                    assertEquals(xForwardedPrefixList.size(), 1);
                    assertEquals(xForwardedPrefixList.get(0), CONTEXT_PATH);
                }
            )
            .handle(context);
    }
}
