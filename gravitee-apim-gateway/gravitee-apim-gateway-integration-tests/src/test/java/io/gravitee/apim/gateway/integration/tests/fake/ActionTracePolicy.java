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
 * Registers, on request, an action on the endpoint request, one on the entrypoint response and one on terminate, each
 * appending its name to the trace {@link FlowTracePolicy} keeps. The entrypoint response action exposes the trace so
 * far on the {@value FlowTracePolicy#TRACE_HEADER} response header. The terminate action, which runs once the response
 * has been written, reports it as the {@value #TRACE_METRIC} custom metric instead.
 */
public class ActionTracePolicy implements HttpPolicy {

    public static final String TRACE_METRIC = "flow-trace";

    private final FlowTracePolicyConfiguration configuration;

    public ActionTracePolicy(FlowTracePolicyConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public String id() {
        return "action-trace";
    }

    @Override
    public Completable onRequest(HttpPlainExecutionContext ctx) {
        return Completable.fromRunnable(() -> {
            ctx.addActionOnEndpointRequest(this, c ->
                Completable.fromRunnable(() -> FlowTracePolicy.trace(c, configuration.getName() + "-endpoint-request"))
            );
            ctx.addActionOnEntrypointResponse(this, c ->
                Completable.fromRunnable(() ->
                    c
                        .response()
                        .headers()
                        .set(FlowTracePolicy.TRACE_HEADER, FlowTracePolicy.trace(c, configuration.getName() + "-entrypoint-response"))
                )
            );
            ctx.addActionOnTerminate(this, c ->
                Completable.fromRunnable(() ->
                    c.metrics().addCustomMetric(TRACE_METRIC, FlowTracePolicy.trace(c, configuration.getName() + "-terminate"))
                )
            );
        });
    }
}
