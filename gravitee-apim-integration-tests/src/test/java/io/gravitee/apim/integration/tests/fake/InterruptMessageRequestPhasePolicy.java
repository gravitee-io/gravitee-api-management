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

import io.gravitee.gateway.reactive.api.ExecutionFailure;
import io.gravitee.gateway.reactive.api.context.MessageExecutionContext;
import io.gravitee.gateway.reactive.api.policy.Policy;
import io.reactivex.rxjava3.core.Completable;

/**
 * Policy that interrupts the publish phase.
 */
public class InterruptMessageRequestPhasePolicy implements Policy {

    @Override
    public String id() {
        return "interrupt-message-request-phase";
    }

    @Override
    public Completable onMessageRequest(MessageExecutionContext ctx) {
        return ctx
            .request()
            .onMessage(message -> ctx.interruptMessageWith(new ExecutionFailure(412).key("FAKE_KEY").message("An error occurred")));
    }
}
