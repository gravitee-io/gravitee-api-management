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
package io.gravitee.gateway.reactive.flow.condition.evaluation;

import io.gravitee.definition.model.flow.FlowV2Impl;
import io.gravitee.gateway.reactive.api.context.http.HttpBaseExecutionContext;
import io.gravitee.gateway.reactive.core.condition.ConditionFilter;
import io.gravitee.gateway.reactive.core.condition.http.HttpConditionFilter;
import io.reactivex.rxjava3.core.Maybe;

/**
 * This {@link ConditionFilter} evaluates to true if the method of the request is matching the
 * methods declared within the {@link FlowV2Impl}.
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HttpMethodConditionFilter
    extends io.gravitee.gateway.flow.condition.evaluation.HttpMethodConditionEvaluator
    implements HttpConditionFilter<FlowV2Impl> {

    @Override
    public Maybe<FlowV2Impl> filter(HttpBaseExecutionContext ctx, FlowV2Impl flow) {
        return evaluate(ctx.request().method(), flow) ? Maybe.just(flow) : Maybe.empty();
    }
}
