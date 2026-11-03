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
package io.gravitee.apim.gateway.integration.tests.fake;

import io.gravitee.gateway.reactive.api.context.http.HttpPlainExecutionContext;
import io.gravitee.gateway.reactive.api.policy.http.HttpPolicy;
import io.reactivex.rxjava3.core.Completable;

/**
 * Records the order the flows run in: each step appends its configured name to a trace kept on the context, and
 * exposes the trace so far on the {@value #TRACE_HEADER} response header. Once every response step ran, the header
 * holds the whole execution order.
 */
public class FlowTracePolicy implements HttpPolicy {

    public static final String TRACE_HEADER = "X-Flow-Trace";

    private static final String TRACE_ATTRIBUTE = "flow-trace";

    private final FlowTracePolicyConfiguration configuration;

    public FlowTracePolicy(FlowTracePolicyConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public String id() {
        return "flow-trace";
    }

    @Override
    public Completable onRequest(HttpPlainExecutionContext ctx) {
        return Completable.fromRunnable(() -> trace(ctx, configuration.getName()));
    }

    @Override
    public Completable onResponse(HttpPlainExecutionContext ctx) {
        return Completable.fromRunnable(() -> ctx.response().headers().set(TRACE_HEADER, trace(ctx, configuration.getName())));
    }

    static String trace(HttpPlainExecutionContext ctx, String name) {
        String previous = ctx.getAttribute(TRACE_ATTRIBUTE);
        String trace = previous == null ? name : previous + "," + name;
        ctx.setAttribute(TRACE_ATTRIBUTE, trace);
        return trace;
    }
}
