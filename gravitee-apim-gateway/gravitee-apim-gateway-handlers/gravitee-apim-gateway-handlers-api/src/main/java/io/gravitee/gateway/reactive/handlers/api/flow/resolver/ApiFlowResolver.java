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
package io.gravitee.gateway.reactive.handlers.api.flow.resolver;

import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.flow.FlowV2;
import io.gravitee.gateway.reactive.api.context.http.HttpBaseExecutionContext;
import io.gravitee.gateway.reactive.core.condition.ConditionFilter;
import io.gravitee.gateway.reactive.flow.AbstractFlowResolver;
import io.reactivex.rxjava3.core.Flowable;
import java.util.stream.Collectors;

/**
 * Allows resolving {@link FlowV2}s to execute for a given {@link Api}.
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
class ApiFlowResolver extends AbstractFlowResolver<HttpBaseExecutionContext> {

    private final Flowable<FlowV2> flows;

    public ApiFlowResolver(Api api, ConditionFilter<HttpBaseExecutionContext, FlowV2> filter) {
        super(filter);
        // Api flows can be determined once and then reused.
        this.flows = provideFlows(api);
    }

    @Override
    public Flowable<FlowV2> provideFlows(HttpBaseExecutionContext ctx) {
        return this.flows;
    }

    private Flowable<FlowV2> provideFlows(Api api) {
        if (api.getFlows() == null || api.getFlows().isEmpty()) {
            return Flowable.empty();
        }

        return Flowable.fromIterable(api.getFlows().stream().filter(FlowV2::isEnabled).collect(Collectors.toList()));
    }
}
