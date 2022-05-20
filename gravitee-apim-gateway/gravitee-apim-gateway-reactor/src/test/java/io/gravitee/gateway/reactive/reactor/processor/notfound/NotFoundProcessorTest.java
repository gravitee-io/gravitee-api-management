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
package io.gravitee.gateway.reactive.reactor.processor.notfound;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.core.component.CustomComponentProvider;
import io.gravitee.gateway.reactive.api.context.Request;
import io.gravitee.gateway.reactive.api.context.Response;
import io.gravitee.gateway.reactive.core.context.MutableRequest;
import io.gravitee.gateway.reactive.core.context.MutableResponse;
import io.gravitee.gateway.reactive.reactor.handler.context.DefaultRequestExecutionContext;
import io.gravitee.gateway.reactive.reactor.processor.responsetime.ResponseTimeProcessor;
import io.gravitee.reporter.api.http.Metrics;
import io.reactivex.Completable;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.StandardEnvironment;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class NotFoundProcessorTest {

    @Mock
    private MutableRequest request;

    @Mock
    private MutableResponse response;

    private NotFoundProcessor notFoundProcessor;
    private DefaultRequestExecutionContext ctx;

    @BeforeEach
    public void beforeEach() {
        when(response.headers()).thenReturn(HttpHeaders.create());
        when(response.body(any(Buffer.class))).thenReturn(Completable.complete());
        when(response.end()).thenReturn(Completable.complete());
        notFoundProcessor = new NotFoundProcessor(new StandardEnvironment());
        ctx = new DefaultRequestExecutionContext(request, response);
    }

    @Test
    public void shouldEndResponseWith404() {
        notFoundProcessor.execute(ctx).test().assertResult();
        verify(response).status(HttpStatusCode.NOT_FOUND_404);
        verify(response, times(2)).headers();
        verify(response).body(any(Buffer.class));
        verify(response).end();
    }
}
