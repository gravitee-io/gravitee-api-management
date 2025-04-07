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

import io.gravitee.definition.model.flow.FlowV2;
import io.gravitee.gateway.reactive.api.context.base.BaseExecutionContext;
import io.gravitee.gateway.reactive.core.condition.ConditionFilter;
import io.reactivex.rxjava3.core.Flowable;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractFlowResolver<C extends BaseExecutionContext> implements FlowResolver<C> {

    private final ConditionFilter<C, FlowV2> filter;

    protected AbstractFlowResolver(ConditionFilter<C, FlowV2> filter) {
        this.filter = filter;
    }

    public Flowable<FlowV2> resolve(C ctx) {
        return provideFlows(ctx).flatMapMaybe(flow -> filter.filter(ctx, flow));
    }
}
