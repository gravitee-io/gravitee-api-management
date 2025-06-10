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
package io.gravitee.apim.integration.tests.fake;

import io.gravitee.gateway.reactive.api.context.HttpExecutionContext;
import io.gravitee.gateway.reactive.api.policy.Policy;
import io.reactivex.rxjava3.core.Completable;

/**
 * Allow to add some actions that will be executed just before reporting metrics.
 * @author GraviteeSource Team
 */
public class ActionReportPolicy implements Policy {

    @Override
    public String id() {
        return "action-report-policy";
    }

    @Override
    public Completable onRequest(HttpExecutionContext ctx) {
        return Completable.fromRunnable(() -> {
            ctx.metrics().getLog().getEntrypointRequest().doOnReport(this::prefixHeader);
            ctx.metrics().getLog().getEntrypointRequest().doOnReport(this::prefixBody);
            ctx.metrics().getLog().getEndpointRequest().doOnReport(this::prefixHeader);
            ctx.metrics().getLog().getEndpointRequest().doOnReport(this::prefixBody);
        });
    }

    @Override
    public Completable onResponse(HttpExecutionContext ctx) {
        return Completable.fromRunnable(() -> {
            ctx.metrics().getLog().getEntrypointResponse().doOnReport(this::prefixHeader);
            ctx.metrics().getLog().getEntrypointResponse().doOnReport(this::prefixBody);
            ctx.metrics().getLog().getEndpointResponse().doOnReport(this::prefixHeader);
            ctx.metrics().getLog().getEndpointResponse().doOnReport(this::prefixBody);
        });
    }

    private void prefixHeader(io.gravitee.reporter.api.common.Request clientRequest) {
        clientRequest.getHeaders().toSingleValueMap().forEach((key, value) -> clientRequest.getHeaders().set(key, "prefix-" + value));
    }

    private void prefixBody(io.gravitee.reporter.api.common.Request clientRequest) {
        clientRequest.setBody("prefix-" + clientRequest.getBody());
    }

    private void prefixHeader(io.gravitee.reporter.api.common.Response clientResponse) {
        clientResponse.getHeaders().toSingleValueMap().forEach((key, value) -> clientResponse.getHeaders().set(key, "prefix-" + value));
    }

    private void prefixBody(io.gravitee.reporter.api.common.Response clientResponse) {
        clientResponse.setBody("prefix-" + clientResponse.getBody());
    }
}
