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
import io.gravitee.gateway.reactive.api.context.http.HttpExecutionContext;
import io.gravitee.gateway.reactive.api.hook.ChainHook;
import io.gravitee.gateway.reactive.api.hook.InvokerHook;
import io.gravitee.gateway.reactive.api.hook.ProcessorHook;
import io.gravitee.gateway.reactive.api.hook.SecurityPlanHook;
import io.gravitee.tracing.api.Span;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class TracingHook extends AbstractTracingHook implements ProcessorHook, ChainHook, InvokerHook, SecurityPlanHook {

    private final String key;

    public TracingHook(final String key) {
        this.key = key;
    }

    @Override
    public String id() {
        return "hook-tracing";
    }

    protected String getSpanName(final String id, final ExecutionPhase executionPhase) {
        StringBuilder spanNameBuilder = new StringBuilder();
        if (executionPhase != null) {
            spanNameBuilder.append(executionPhase.name()).append(" ");
        }
        if (!id.startsWith(key)) {
            spanNameBuilder.append(key).append("-");
        }
        spanNameBuilder.append(id);
        return spanNameBuilder.toString();
    }

    @Override
    protected void withAttributes(String id, HttpExecutionContext ctx, ExecutionPhase executionPhase, Span span) {
        super.withAttributes(id, ctx, executionPhase, span);
        span.withAttribute(key, id);
    }
}
