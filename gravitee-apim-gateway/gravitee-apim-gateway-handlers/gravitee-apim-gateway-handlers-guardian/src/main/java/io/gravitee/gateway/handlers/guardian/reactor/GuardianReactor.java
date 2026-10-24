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
package io.gravitee.gateway.handlers.guardian.reactor;

import io.gravitee.gateway.handlers.guardian.ReactableGuardian;
import io.gravitee.gateway.reactive.api.context.http.HttpPlainExecutionContext;
import io.gravitee.gateway.reactive.api.policy.base.BasePolicy;
import io.reactivex.rxjava3.core.Completable;

/**
 * What the Guardian reactor plugin exposes to the gateway for one deployed Guardian: the execution of its policies by
 * the {@code guardian-policy}. The plugin's {@link io.gravitee.gateway.reactor.handler.ReactorHandler} implements it and
 * registers itself in the {@link io.gravitee.gateway.handlers.guardian.registry.GuardianRegistry} while started.
 *
 * @author GraviteeSource Team
 */
public interface GuardianReactor {
    String id();

    ReactableGuardian reactableGuardian();

    /**
     * Executes the policies of the Guardian with the execution context of the calling API, the Guardian resources in
     * place of the API ones.
     *
     * @param ctx the execution context of the calling API.
     * @param source the policy of the API flow executing the Guardian.
     */
    Completable execute(HttpPlainExecutionContext ctx, BasePolicy source);
}
