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

import io.gravitee.gateway.reactive.api.ExecutionFailure;
import io.gravitee.gateway.reactive.api.context.http.HttpExecutionContext;
import io.gravitee.gateway.reactive.api.context.http.HttpPlainExecutionContext;
import io.gravitee.gateway.reactive.api.policy.http.HttpPolicy;
import io.reactivex.rxjava3.core.Completable;
import java.util.function.Function;

/**
 * Registers, on request, an action of the configured kind that either interrupts the request with a {@value #STATUS}
 * or fails with an unexpected error.
 */
public class ActionOutcomePolicy implements HttpPolicy {

    public static final int STATUS = 409;

    private final ActionOutcomePolicyConfiguration configuration;

    public ActionOutcomePolicy(ActionOutcomePolicyConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public String id() {
        return "action-outcome";
    }

    @Override
    public Completable onRequest(HttpPlainExecutionContext ctx) {
        return Completable.fromRunnable(() -> {
            Function<HttpExecutionContext, Completable> action = switch (configuration.getOutcome()) {
                case INTERRUPT -> c -> c.interruptWith(new ExecutionFailure(STATUS).message("Interrupted by an action"));
                case FAIL -> c -> Completable.error(new RuntimeException("An action failed unexpectedly"));
            };
            switch (configuration.getAction()) {
                case ENDPOINT_REQUEST -> ctx.addActionOnEndpointRequest(this, action);
                case ENTRYPOINT_RESPONSE -> ctx.addActionOnEntrypointResponse(this, action);
                case TERMINATE -> ctx.addActionOnTerminate(this, action);
            }
        });
    }
}
