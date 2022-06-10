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
package io.gravitee.gateway.jupiter.reactor.processor;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.spy;

import io.gravitee.definition.model.Api;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.jupiter.core.context.MutableRequest;
import io.gravitee.gateway.jupiter.core.context.MutableResponse;
import io.gravitee.gateway.jupiter.reactor.handler.context.DefaultRequestExecutionContext;
import io.gravitee.reporter.api.http.Metrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
public class AbstractProcessorTest {

    protected Api api;

    @Mock
    protected MutableRequest mockRequest;

    protected HttpHeaders spyRequestHeaders;

    @Mock
    protected MutableResponse mockResponse;

    protected HttpHeaders spyResponseHeaders;

    protected DefaultRequestExecutionContext ctx;
    protected Metrics metrics;

    @BeforeEach
    public void init() {
        metrics = Metrics.on(System.currentTimeMillis()).build();
        spyRequestHeaders = spy(HttpHeaders.create());
        spyResponseHeaders = spy(HttpHeaders.create());
        lenient().when(mockRequest.metrics()).thenReturn(metrics);
        lenient().when(mockRequest.headers()).thenReturn(spyRequestHeaders);
        lenient().when(mockResponse.headers()).thenReturn(spyResponseHeaders);
        ctx = new DefaultRequestExecutionContext(mockRequest, mockResponse);
    }
}
