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
import java.util.Map;

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

    protected String spanName(final String id, final ExecutionPhase executionPhase) {
        return "%s %s (%s)".formatted(executionPhase.name(), key, id);
    }

    @Override
    protected Map<String, String> spanAttributes(String id, HttpExecutionContext ctx, ExecutionPhase executionPhase) {
        Map<String, String> spanAttributes = super.spanAttributes(id, ctx, executionPhase);
        spanAttributes.put(key, id);
        return spanAttributes;
    }
}
