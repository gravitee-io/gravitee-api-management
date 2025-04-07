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
import io.gravitee.definition.model.v4.flow.FlowV4Impl;
import io.gravitee.gateway.reactive.api.context.base.BaseExecutionContext;
import io.gravitee.gateway.reactive.core.condition.ConditionFilter;
import io.gravitee.gateway.reactive.v4.flow.AbstractFlowResolver;
import io.reactivex.rxjava3.core.Flowable;
import java.util.stream.Collectors;

/**
 * Allows resolving {@link FlowV4Impl}s to execute for a given {@link Api}.
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@SuppressWarnings("common-java:DuplicatedBlocks") // Needed for v4 definition. Will replace the other one at the end.
class ApiFlowResolver extends AbstractFlowResolver {

    private final Flowable<FlowV4Impl> flows;

    public ApiFlowResolver(Api api, ConditionFilter<BaseExecutionContext, FlowV4Impl> filter) {
        super(filter);
        // Api flows can be determined once and then reused.
        this.flows = provideFlows(api);
    }

    @Override
    public Flowable<FlowV4Impl> provideFlows(BaseExecutionContext ctx) {
        return this.flows;
    }

    private Flowable<FlowV4Impl> provideFlows(Api api) {
        if (api.getFlows() == null || api.getFlows().isEmpty()) {
            return Flowable.empty();
        }

        return Flowable.fromIterable(api.getFlows().stream().filter(FlowV4Impl::isEnabled).collect(Collectors.toList()));
    }
}
