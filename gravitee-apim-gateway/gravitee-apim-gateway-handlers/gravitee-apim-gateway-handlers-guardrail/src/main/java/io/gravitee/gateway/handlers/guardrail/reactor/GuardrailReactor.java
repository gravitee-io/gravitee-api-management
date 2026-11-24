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
package io.gravitee.gateway.handlers.guardrail.reactor;

import io.gravitee.gateway.handlers.guardrail.ReactableGuardrail;
import io.gravitee.gateway.handlers.guardrail.decision.GuardrailVerdicts;
import io.gravitee.gateway.reactive.api.context.http.HttpPlainExecutionContext;
import io.gravitee.gateway.reactive.api.policy.base.BasePolicy;
import io.reactivex.rxjava3.core.Completable;

/**
 * What the Guardrail reactor plugin exposes to the gateway for one deployed Guardrail: the execution of its policies by
 * the {@code guardrail-policy}. The plugin's {@link io.gravitee.gateway.reactor.handler.ReactorHandler} implements it and
 * registers itself in the {@link io.gravitee.gateway.handlers.guardrail.registry.GuardrailRegistry} while started.
 * <p>
 * The {@code guardrail-policy} looks the reactor up when it executes the Guardrail, so a call that has not executed it yet
 * runs the latest deployed revision. Stopping a reactor, when its Guardrail is updated or undeployed, must let the
 * executions already started finish before releasing the policies and resources they use.
 *
 * @author GraviteeSource Team
 */
public interface GuardrailReactor {
    /**
     * @return the Guardrail this reactor runs, never {@code null}: the registry keys the reactor by it.
     */
    ReactableGuardrail reactableGuardrail();

    /**
     * Executes the policies of the Guardrail with the execution context of the calling API, the Guardrail resources in
     * place of the API ones.
     *
     * @param ctx the execution context of the calling API.
     * @param source the policy of the API flow executing the Guardrail.
     * @param verdicts the collector of this run, where the guardrail policies of the Guardrail record what they concluded.
     */
    Completable execute(HttpPlainExecutionContext ctx, BasePolicy source, GuardrailVerdicts verdicts);
}
