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
import io.gravitee.gateway.jupiter.api.invoker.Invoker;
import io.gravitee.gateway.jupiter.reactor.handler.context.DefaultExecutionContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class ExecutionContextAdapterTest {

    @Mock(extraInterfaces = { io.gravitee.gateway.api.Invoker.class })
    private Invoker invoker;

    @Test
    void shouldSetInternalExecutionFailureFromProcessorFailure() {
        DefaultExecutionContext ctx = new DefaultExecutionContext(null, null);
        ExecutionContextAdapter cut = ExecutionContextAdapter.create(ctx);

        cut.setAttribute(ExecutionContext.ATTR_FAILURE_ATTRIBUTE, new RuntimeProcessorFailure("error"));
        assertNotNull(ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_EXECUTION_FAILURE));
    }

    @Test
    void shouldSetInternalInvokerFromInvoker() {
        DefaultExecutionContext ctx = new DefaultExecutionContext(null, null);
        ExecutionContextAdapter cut = ExecutionContextAdapter.create(ctx);

        cut.setAttribute(ExecutionContext.ATTR_INVOKER, invoker);
        final Invoker internalInvoker = ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_INVOKER);
        assertNotNull(internalInvoker);
        assertSame(invoker, internalInvoker);
    }

    @Test
    void shouldSetInternalInvokerSkipFromInvokerSkip() {
        DefaultExecutionContext ctx = new DefaultExecutionContext(null, null);
        ExecutionContextAdapter cut = ExecutionContextAdapter.create(ctx);

        cut.setAttribute(ExecutionContext.ATTR_INVOKER_SKIP, true);
        final Boolean invokerSkip = ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_INVOKER_SKIP);
        assertNotNull(invokerSkip);
        assertTrue(invokerSkip);
    }

    @Test
    void shouldSetInternalSecuritySkipFromSecuritySkip() {
        DefaultExecutionContext ctx = new DefaultExecutionContext(null, null);
        ExecutionContextAdapter cut = ExecutionContextAdapter.create(ctx);

        cut.setAttribute(ExecutionContext.ATTR_SECURITY_SKIP, true);
        final Boolean securitySkip = ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_SECURITY_SKIP);
        assertNotNull(securitySkip);
        assertTrue(securitySkip);
    }

    @Test
    void shouldGetProcessorFailureFromInternalAttributeExecutionFailure() {
        DefaultExecutionContext ctx = new DefaultExecutionContext(null, null);
        ctx.setInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_EXECUTION_FAILURE, new ExecutionFailure(200));
        ExecutionContextAdapter cut = ExecutionContextAdapter.create(ctx);

        Object adapterAttribute = cut.getAttribute(ExecutionContext.ATTR_FAILURE_ATTRIBUTE);
        assertNotNull(adapterAttribute);
        assertInstanceOf(ProcessorFailure.class, adapterAttribute);
        assertEquals(200, ((ProcessorFailure) adapterAttribute).statusCode());
    }

    @Test
    void shouldGetNullProcessorFailureWhenNullInternalAttributeExecutionFailure() {
        DefaultExecutionContext ctx = new DefaultExecutionContext(null, null);
        ctx.setInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_EXECUTION_FAILURE, null);
        ExecutionContextAdapter cut = ExecutionContextAdapter.create(ctx);

        Object adapterAttribute = cut.getAttribute(ExecutionContext.ATTR_FAILURE_ATTRIBUTE);
        assertNull(adapterAttribute);
    }

    @Test
    void shouldGetInvokerFromInternalAttributeInvoker() {
        DefaultExecutionContext ctx = new DefaultExecutionContext(null, null);
        ctx.setInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_INVOKER, invoker);
        ExecutionContextAdapter cut = ExecutionContextAdapter.create(ctx);

        Object invokerAttribute = cut.getAttribute(ExecutionContext.ATTR_INVOKER);
        assertNotNull(invokerAttribute);
        assertInstanceOf(io.gravitee.gateway.api.Invoker.class, invokerAttribute);
        assertSame(invoker, invokerAttribute);
    }

    @Test
    void shouldGetInvokerSkipFromInternalAttributeInvokerSkip() {
        DefaultExecutionContext ctx = new DefaultExecutionContext(null, null);
        ctx.setInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_INVOKER_SKIP, true);
        ExecutionContextAdapter cut = ExecutionContextAdapter.create(ctx);

        Object invokerSkip = cut.getAttribute(ExecutionContext.ATTR_INVOKER_SKIP);
        assertNotNull(invokerSkip);
        assertInstanceOf(Boolean.class, invokerSkip);
        assertTrue((Boolean) invokerSkip);
    }

    @Test
    void shouldGetSecuritySkipFromInternalAttributeSecuritySkip() {
        DefaultExecutionContext ctx = new DefaultExecutionContext(null, null);
        ctx.setInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_SECURITY_SKIP, true);
        ExecutionContextAdapter cut = ExecutionContextAdapter.create(ctx);

        Object securitySkip = cut.getAttribute(ExecutionContext.ATTR_SECURITY_SKIP);
        assertNotNull(securitySkip);
        assertInstanceOf(Boolean.class, securitySkip);
        assertTrue((Boolean) securitySkip);
    }

    @Test
    void shouldGetNullInvokerWhenNullInternalAttributeInvoker() {
        DefaultExecutionContext ctx = new DefaultExecutionContext(null, null);
        ctx.setInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_INVOKER, null);
        ExecutionContextAdapter cut = ExecutionContextAdapter.create(ctx);

        Object invokerAttribute = cut.getAttribute(ExecutionContext.ATTR_INVOKER);
        assertNull(invokerAttribute);
    }

    @Test
    void shouldGetNullInvokerSkipWhenNullInternalAttributeInvokerSkip() {
        DefaultExecutionContext ctx = new DefaultExecutionContext(null, null);
        ctx.setInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_INVOKER_SKIP, null);
        ExecutionContextAdapter cut = ExecutionContextAdapter.create(ctx);

        Object invokerSkipAttribute = cut.getAttribute(ExecutionContext.ATTR_INVOKER_SKIP);
        assertNull(invokerSkipAttribute);
    }

    @Test
    void shouldGetNullSecuritySkipWhenNullInternalAttributeSecuritySkip() {
        DefaultExecutionContext ctx = new DefaultExecutionContext(null, null);
        ctx.setInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_SECURITY_SKIP, null);
        ExecutionContextAdapter cut = ExecutionContextAdapter.create(ctx);

        Object securitySkipAttribute = cut.getAttribute(ExecutionContext.ATTR_SECURITY_SKIP);
        assertNull(securitySkipAttribute);
    }

    @Test
    void shouldRemoveInternalAttributeExecutionFailure() {
        DefaultExecutionContext ctx = new DefaultExecutionContext(null, null);
        ctx.setInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_EXECUTION_FAILURE, new ExecutionFailure(200));
        ExecutionContextAdapter cut = ExecutionContextAdapter.create(ctx);

        cut.removeAttribute(ExecutionContext.ATTR_FAILURE_ATTRIBUTE);
        assertNull(ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_EXECUTION_FAILURE));
    }

    @Test
    void shouldRemoveInternalAttributeInvoker() {
        DefaultExecutionContext ctx = new DefaultExecutionContext(null, null);
        ctx.setInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_INVOKER, invoker);
        ExecutionContextAdapter cut = ExecutionContextAdapter.create(ctx);

        cut.removeAttribute(ExecutionContext.ATTR_INVOKER);
        assertNull(ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_INVOKER));
    }

    @Test
    void shouldRemoveInternalAttributeInvokerSkip() {
        DefaultExecutionContext ctx = new DefaultExecutionContext(null, null);
        ctx.setInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_INVOKER_SKIP, true);
        ExecutionContextAdapter cut = ExecutionContextAdapter.create(ctx);

        cut.removeAttribute(ExecutionContext.ATTR_INVOKER_SKIP);
        assertNull(ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_INVOKER_SKIP));
    }

    @Test
    void shouldRemoveInternalAttributeSecuritySkip() {
        DefaultExecutionContext ctx = new DefaultExecutionContext(null, null);
        ctx.setInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_SECURITY_SKIP, true);
        ExecutionContextAdapter cut = ExecutionContextAdapter.create(ctx);

        cut.removeAttribute(ExecutionContext.ATTR_SECURITY_SKIP);
        assertNull(ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_SECURITY_SKIP));
    }

    @Test
    void shouldInstantiateAdaptedTemplateEngineOnce() {
        final ExecutionContextAdapter cut = ExecutionContextAdapter.create(mock(HttpExecutionContext.class));
        final TemplateEngine templateEngine = cut.getTemplateEngine();

        for (int i = 0; i < 10; i++) {
            assertEquals(templateEngine, cut.getTemplateEngine());
        }
    }

    @Test
    void shouldRestoreTemplateEngine() {
        final ExecutionContextAdapter cut = ExecutionContextAdapter.create(mock(HttpExecutionContext.class));
        final TemplateEngineAdapter templateEngine = mock(TemplateEngineAdapter.class);

        ReflectionTestUtils.setField(cut, "adaptedTemplateEngine", templateEngine);

        cut.restore();

        verify(templateEngine).restore();
    }

    @Test
    void shouldReplaceAdaptedRequest() {
        final HttpExecutionContext ctx = mock(HttpExecutionContext.class);
        final HttpRequest request = mock(HttpRequest.class);

        when(ctx.request()).thenReturn(request);

        final ExecutionContextAdapter cut = ExecutionContextAdapter.create(ctx);
        final io.gravitee.gateway.api.Request adaptedRequest = cut.request();

        assertNotNull(adaptedRequest);
        assertTrue(adaptedRequest instanceof RequestAdapter);

        // Try to override the current request with another one.
        final io.gravitee.gateway.api.Request replaced = mock(io.gravitee.gateway.api.Request.class);
        cut.request(replaced);

        // The adapted request should have been replaced.
        assertEquals(replaced, cut.request());
    }

    @Test
    void shouldReplaceAdaptedResponse() {
        final HttpExecutionContext ctx = mock(HttpExecutionContext.class);
        final HttpResponse response = mock(HttpResponse.class);

        when(ctx.response()).thenReturn(response);

        final ExecutionContextAdapter cut = ExecutionContextAdapter.create(ctx);
        final io.gravitee.gateway.api.Response adaptedResponse = cut.response();

        assertNotNull(adaptedResponse);
        assertTrue(adaptedResponse instanceof ResponseAdapter);

        // Try to override the current response with another one.
        final io.gravitee.gateway.api.Response replaced = mock(io.gravitee.gateway.api.Response.class);
        cut.response(replaced);

        // The adapted response should have been replaced.
        assertEquals(replaced, cut.response());
    }

    void shouldSetAttribute() {}
}
