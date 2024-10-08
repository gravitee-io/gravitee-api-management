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
package io.gravitee.gateway.reactive.flow;

import io.gravitee.definition.model.flow.Flow;
import io.gravitee.gateway.reactive.api.context.base.BaseExecutionContext;
import io.gravitee.gateway.reactive.api.context.http.HttpBaseExecutionContext;
import io.reactivex.rxjava3.core.Flowable;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface FlowResolver<C extends BaseExecutionContext> {
    /**
     * Provides the initial list of flows.
     * It's up to the implementation to decide to use the current execution context or not.
     * The implementation can decide to cache the list of flows or evaluate it against the current context.
     *
     * @param ctx the current context
     * @return a {@link Flowable} of {@link Flow}.
     */
    Flowable<Flow> provideFlows(final C ctx);

    /**
     * Resolve the flows against the current context.
     * Each flow of the initial flow list is filtered thanks to the provided evaluator before being returned.
     *
     * Initial flow list must be provided by a concrete implementation of {@link #provideFlows(BaseExecutionContext)}.
     *
     * @param ctx the current context.
     * @return a {@link Flowable} of {@link Flow} that have passed the filter step.
     */
    Flowable<Flow> resolve(final C ctx);
}
