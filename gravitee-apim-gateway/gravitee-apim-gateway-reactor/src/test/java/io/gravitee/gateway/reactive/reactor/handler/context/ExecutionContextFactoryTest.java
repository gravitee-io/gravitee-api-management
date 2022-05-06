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
package io.gravitee.gateway.reactive.reactor.handler.context;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.gravitee.el.TemplateContext;
import io.gravitee.el.TemplateVariableProvider;
import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.reactive.api.context.MessageExecutionContext;
import io.gravitee.gateway.reactive.api.context.Request;
import io.gravitee.gateway.reactive.api.context.RequestExecutionContext;
import io.gravitee.gateway.reactive.api.context.Response;
import org.junit.jupiter.api.Test;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
class ExecutionContextFactoryTest {

    @Test
    public void shouldAddTemplateVariableProviders() {
        final TemplateVariableProvider provider1 = mock(TemplateVariableProvider.class);
        final TemplateVariableProvider provider2 = mock(TemplateVariableProvider.class);

        final ExecutionContextFactory cut = new ExecutionContextFactory(mock(ComponentProvider.class));

        cut.addTemplateVariableProvider(provider1);
        cut.addTemplateVariableProvider(provider2);

        assertTrue(cut.templateVariableProviders.contains(provider1));
        assertTrue(cut.templateVariableProviders.contains(provider2));
    }

    @Test
    public void shouldCreateSyncExecutionContext() {
        final TemplateVariableProvider provider = mock(TemplateVariableProvider.class);

        final Request request = mock(Request.class);
        final Response response = mock(Response.class);

        final ComponentProvider componentProvider = mock(ComponentProvider.class);
        final ExecutionContextFactory cut = new ExecutionContextFactory(componentProvider);

        cut.addTemplateVariableProvider(provider);

        final RequestExecutionContext ctx = cut.createRequestResponseContext(request, response);

        assertNotNull(ctx);
        assertEquals(request, ctx.request());
        assertEquals(response, ctx.response());

        // Check the component provider has been set.
        ctx.getComponent(Class.class);
        verify(componentProvider).getComponent(Class.class);

        // Check the provider has been well set (used when getTemplateEngine is called)
        ctx.getTemplateEngine();
        verify(provider).provide(any(TemplateContext.class));
    }

    @Test
    public void shouldCreateASyncExecutionContext() {
        final TemplateVariableProvider provider = mock(TemplateVariableProvider.class);

        final Request request = mock(Request.class);
        final Response response = mock(Response.class);

        final ComponentProvider componentProvider = mock(ComponentProvider.class);
        final ExecutionContextFactory cut = new ExecutionContextFactory(componentProvider);

        cut.addTemplateVariableProvider(provider);

        final MessageExecutionContext ctx = cut.createMessageContext(request, response);

        assertNotNull(ctx);
        assertEquals(request, ctx.request());
        assertEquals(response, ctx.response());

        // Check the component provider has been set.
        ctx.getComponent(Class.class);
        verify(componentProvider).getComponent(Class.class);

        // Check the provider has been well set (used when getTemplateEngine is called)
        ctx.getTemplateEngine();
        verify(provider).provide(any(TemplateContext.class));
    }
}
