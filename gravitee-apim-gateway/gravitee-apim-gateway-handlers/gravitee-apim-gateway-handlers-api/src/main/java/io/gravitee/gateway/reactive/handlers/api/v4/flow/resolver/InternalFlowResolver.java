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
package io.gravitee.gateway.reactive.handlers.api.v4.flow.resolver;

import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.gateway.reactive.api.context.base.BaseExecutionContext;
import io.gravitee.gateway.reactive.core.condition.ConditionFilter;
import io.gravitee.gateway.reactive.v4.flow.AbstractFlowResolver;
import io.reactivex.rxjava3.core.Flowable;

/**
 * Allows resolving the internal {@link Flow} of an {@link Api}: the single flow managed by the platform and hidden from
 * the API publisher. It is provided only when it is enabled.
 */
class InternalFlowResolver extends AbstractFlowResolver {

    private final Flowable<Flow> flows;

    public InternalFlowResolver(Api api, ConditionFilter<BaseExecutionContext, Flow> filter) {
        super(filter);
        // The internal flow can be determined once and then reused.
        this.flows = provideFlows(api);
    }

    @Override
    public Flowable<Flow> provideFlows(BaseExecutionContext ctx) {
        return this.flows;
    }

    private Flowable<Flow> provideFlows(Api api) {
        Flow internalFlow = api.getInternalFlow();
        if (internalFlow == null || !internalFlow.isEnabled()) {
            return Flowable.empty();
        }

        return Flowable.just(internalFlow);
    }
}
