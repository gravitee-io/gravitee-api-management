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
package io.gravitee.gateway.reactive.core.tracing;

import io.gravitee.gateway.reactive.api.ExecutionPhase;
import io.gravitee.gateway.reactive.api.context.InternalContextAttributes;
import io.gravitee.gateway.reactive.api.context.http.HttpExecutionContext;
import io.gravitee.gateway.reactive.api.hook.ChainHook;
import io.gravitee.gateway.reactive.api.hook.InvokerHook;
import io.gravitee.gateway.reactive.api.hook.ProcessorHook;
import io.gravitee.gateway.reactive.api.hook.SecurityPlanHook;
import io.gravitee.node.api.opentelemetry.Span;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class InvokerTracingHook extends TracingHook implements ProcessorHook, ChainHook, InvokerHook, SecurityPlanHook {

    public static final String SPAN_ENDPOINT_ID_ATTR = "gravitee.endpoint.id";

    public InvokerTracingHook(final String key) {
        super(key);
    }

    @Override
    public String id() {
        return "hook-invoker-tracing";
    }

    protected String spanName(final String id, final ExecutionPhase executionPhase) {
        return id;
    }

    protected void endSpan(final String id, final HttpExecutionContext ctx) {
        Span span = getSpan(ctx, id);
        span.withAttribute(SPAN_ENDPOINT_ID_ATTR, ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_ENDPOINT_CONNECTOR_ID));
        putSpan(id, ctx, span);
        super.endSpan(id, ctx);
    }
}
