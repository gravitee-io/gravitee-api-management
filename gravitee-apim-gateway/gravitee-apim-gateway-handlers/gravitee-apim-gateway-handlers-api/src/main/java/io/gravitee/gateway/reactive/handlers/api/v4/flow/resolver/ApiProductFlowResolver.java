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

import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.gateway.reactive.api.context.base.BaseExecutionContext;
import io.gravitee.gateway.reactive.core.condition.ConditionFilter;
import io.gravitee.gateway.reactive.v4.flow.AbstractFlowResolver;
import io.reactivex.rxjava3.core.Flowable;
import java.util.List;

/**
 * Resolves the flows of one deployed version of an API Product, whatever its plan.
 */
class ApiProductFlowResolver extends AbstractFlowResolver {

    private final Flowable<Flow> flows;

    ApiProductFlowResolver(List<Flow> flows, ConditionFilter<BaseExecutionContext, Flow> filter) {
        super(filter);
        this.flows = Flowable.fromIterable(flows == null ? List.of() : flows.stream().filter(Flow::isEnabled).toList());
    }

    @Override
    public Flowable<Flow> provideFlows(BaseExecutionContext ctx) {
        return flows;
    }
}
