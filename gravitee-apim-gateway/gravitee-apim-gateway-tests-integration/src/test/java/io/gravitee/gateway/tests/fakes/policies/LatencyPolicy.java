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
package io.gravitee.gateway.tests.fakes.policies;

import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.jupiter.api.context.RequestExecutionContext;
import io.gravitee.gateway.jupiter.api.policy.Policy;
import io.gravitee.policy.api.PolicyChain;
import io.gravitee.policy.api.PolicyConfiguration;
import io.gravitee.policy.api.annotations.OnRequest;
import io.gravitee.policy.api.annotations.OnResponse;
import io.reactivex.Completable;
import io.vertx.core.Vertx;
import java.util.concurrent.TimeUnit;

/**
 * Allow to add a latency on response or request.
 * You can provide a delay through configuration or use the default {@link LatencyPolicy#DEFAULT_DELAY}
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class LatencyPolicy implements Policy {

    public static final int DEFAULT_DELAY = 600;
    private final Long delay;

    public LatencyPolicy(LatencyConfiguration configuration) {
        delay = configuration.getDelay() != null ? configuration.getDelay() : DEFAULT_DELAY;
    }

    @OnRequest
    public void onRequest(
        final Request request,
        final Response response,
        final ExecutionContext executionContext,
        final PolicyChain policyChain
    ) {
        executionContext.getComponent(Vertx.class).setTimer(delay, timerId -> policyChain.doNext(request, response));
    }

    @OnResponse
    public void onResponse(
        final Request request,
        final Response response,
        final ExecutionContext executionContext,
        final PolicyChain policyChain
    ) {
        executionContext.getComponent(Vertx.class).setTimer(delay, timerId -> policyChain.doNext(request, response));
    }

    @Override
    public String id() {
        return "latency";
    }

    @Override
    public Completable onRequest(RequestExecutionContext ctx) {
        return Completable.timer(delay, TimeUnit.MILLISECONDS);
    }

    @Override
    public Completable onResponse(RequestExecutionContext ctx) {
        return Completable.timer(delay, TimeUnit.MILLISECONDS);
    }

    public static class LatencyConfiguration implements PolicyConfiguration {

        private Long delay;

        public Long getDelay() {
            return delay;
        }

        public void setDelay(Long delay) {
            this.delay = delay;
        }
    }
}
