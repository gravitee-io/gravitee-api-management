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
package io.gravitee.gateway.handlers.guardrail;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;

import io.gravitee.common.event.EventManager;
import io.gravitee.gateway.handlers.guardrail.decision.GuardrailVerdicts;
import io.gravitee.gateway.handlers.guardrail.reactor.GuardrailReactor;
import io.gravitee.gateway.handlers.guardrail.registry.GuardrailRegistry;
import io.gravitee.gateway.reactive.api.context.http.HttpPlainExecutionContext;
import io.gravitee.gateway.reactive.api.policy.base.BasePolicy;
import io.gravitee.gateway.reactor.ReactorEvent;
import io.reactivex.rxjava3.core.Completable;

/**
 * Stands for the Guardrail reactor plugin on a mocked {@link EventManager}: deploying or updating a Guardrail starts a
 * reactor registered in the {@link GuardrailRegistry}, as the plugin does.
 */
public final class FakeGuardrailReactorPlugin {

    private FakeGuardrailReactorPlugin() {}

    public static void install(EventManager eventManager, GuardrailRegistry guardrailRegistry) {
        for (ReactorEvent event : new ReactorEvent[] { ReactorEvent.DEPLOY, ReactorEvent.UPDATE }) {
            lenient()
                .doAnswer(invocation -> {
                    guardrailRegistry.register(reactorOf(invocation.getArgument(1)));
                    return null;
                })
                .when(eventManager)
                .publishEvent(eq(event), any(ReactableGuardrail.class));
        }
    }

    public static GuardrailReactor reactorOf(ReactableGuardrail reactableGuardrail) {
        return new GuardrailReactor() {
            @Override
            public ReactableGuardrail reactableGuardrail() {
                return reactableGuardrail;
            }

            @Override
            public Completable execute(HttpPlainExecutionContext ctx, BasePolicy source, GuardrailVerdicts verdicts) {
                return Completable.complete();
            }
        };
    }
}
