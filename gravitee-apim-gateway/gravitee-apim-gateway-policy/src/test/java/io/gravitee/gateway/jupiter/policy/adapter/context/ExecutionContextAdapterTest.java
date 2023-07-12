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
package io.gravitee.gateway.jupiter.policy.adapter.context;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.gravitee.el.TemplateEngine;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.processor.ProcessorFailure;
import io.gravitee.gateway.core.processor.RuntimeProcessorFailure;
import io.gravitee.gateway.jupiter.api.ExecutionFailure;
import io.gravitee.gateway.jupiter.api.context.*;
import io.gravitee.gateway.jupiter.reactor.handler.context.DefaultExecutionContext;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
class ExecutionContextAdapterTest {

    @Test
    void shouldAddInternalExecutionFailureFromProcessorFailure() {
        DefaultExecutionContext ctx = new DefaultExecutionContext(null, null);
        ExecutionContextAdapter contextAdapter = ExecutionContextAdapter.create(ctx);

        contextAdapter.setAttribute(ExecutionContext.ATTR_FAILURE_ATTRIBUTE, new RuntimeProcessorFailure("error"));
        assertNotNull(ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_EXECUTION_FAILURE));
    }

    @Test
    void shouldGetProcessorFailureFromInternalExecutionFailure() {
        DefaultExecutionContext ctx = new DefaultExecutionContext(null, null);
        ctx.setInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_EXECUTION_FAILURE, new ExecutionFailure(200));
        ExecutionContextAdapter contextAdapter = ExecutionContextAdapter.create(ctx);

        Object adapterAttribute = contextAdapter.getAttribute(ExecutionContext.ATTR_FAILURE_ATTRIBUTE);
        assertNotNull(adapterAttribute);
        assertInstanceOf(ProcessorFailure.class, adapterAttribute);
        assertEquals(200, ((ProcessorFailure) adapterAttribute).statusCode());
    }

    @Test
    void shouldRemoveInternalExecutionFailure() {
        DefaultExecutionContext ctx = new DefaultExecutionContext(null, null);
        ctx.setInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_EXECUTION_FAILURE, new ExecutionFailure(200));
        ExecutionContextAdapter contextAdapter = ExecutionContextAdapter.create(ctx);

        contextAdapter.removeAttribute(ExecutionContext.ATTR_FAILURE_ATTRIBUTE);
        assertNull(ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_EXECUTION_FAILURE));
    }

    @Test
    void shouldInstantiateAdaptedTemplateEngineOnce() {
        final ExecutionContextAdapter contextAdapter = ExecutionContextAdapter.create(mock(HttpExecutionContext.class));
        final TemplateEngine templateEngine = contextAdapter.getTemplateEngine();

        for (int i = 0; i < 10; i++) {
            assertEquals(templateEngine, contextAdapter.getTemplateEngine());
        }
    }

    @Test
    void shouldRestoreTemplateEngine() {
        final ExecutionContextAdapter contextAdapter = ExecutionContextAdapter.create(mock(HttpExecutionContext.class));
        final TemplateEngineAdapter templateEngine = mock(TemplateEngineAdapter.class);

        ReflectionTestUtils.setField(contextAdapter, "adaptedTemplateEngine", templateEngine);

        contextAdapter.restore();

        verify(templateEngine).restore();
    }

    @Test
    void shouldReplaceAdaptedRequest() {
        final HttpExecutionContext ctx = mock(HttpExecutionContext.class);
        final HttpRequest request = mock(HttpRequest.class);

        when(ctx.request()).thenReturn(request);

        final ExecutionContextAdapter contextAdapter = ExecutionContextAdapter.create(ctx);
        final io.gravitee.gateway.api.Request adaptedRequest = contextAdapter.request();

        assertNotNull(adaptedRequest);
        assertTrue(adaptedRequest instanceof RequestAdapter);

        // Try to override the current request with another one.
        final io.gravitee.gateway.api.Request replaced = mock(io.gravitee.gateway.api.Request.class);
        contextAdapter.request(replaced);

        // The adapted request should have been replaced.
        assertEquals(replaced, contextAdapter.request());
    }

    @Test
    void shouldReplaceAdaptedResponse() {
        final HttpExecutionContext ctx = mock(HttpExecutionContext.class);
        final HttpResponse response = mock(HttpResponse.class);

        when(ctx.response()).thenReturn(response);

        final ExecutionContextAdapter contextAdapter = ExecutionContextAdapter.create(ctx);
        final io.gravitee.gateway.api.Response adaptedResponse = contextAdapter.response();

        assertNotNull(adaptedResponse);
        assertTrue(adaptedResponse instanceof ResponseAdapter);

        // Try to override the current response with another one.
        final io.gravitee.gateway.api.Response replaced = mock(io.gravitee.gateway.api.Response.class);
        contextAdapter.response(replaced);

        // The adapted response should have been replaced.
        assertEquals(replaced, contextAdapter.response());
    }
}
