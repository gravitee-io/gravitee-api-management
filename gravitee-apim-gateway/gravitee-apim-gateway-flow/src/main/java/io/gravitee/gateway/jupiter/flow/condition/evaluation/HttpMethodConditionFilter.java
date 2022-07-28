/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.jupiter.flow.condition.evaluation;

import io.gravitee.definition.model.v4.flow.selector.Selector;
import io.gravitee.definition.model.v4.flow.selector.SelectorType;
import io.gravitee.gateway.jupiter.api.context.RequestExecutionContext;
import io.gravitee.gateway.jupiter.core.condition.ConditionFilter;
import io.gravitee.gateway.model.Flow;
import io.reactivex.Maybe;

import java.util.function.Function;

/**
 * This {@link ConditionFilter} evaluates to true if the method of the request is matching the
 * methods declared within the {@link Flow}.
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HttpMethodConditionFilter
    extends io.gravitee.gateway.flow.condition.evaluation.HttpMethodConditionEvaluator
    implements ConditionFilter<Flow> {

    @Override
    public Maybe<Flow> filter(RequestExecutionContext ctx, Flow flow) {
        return flow.getSelectors()
                .stream()
                .filter(selector -> selector.getType() == SelectorType.HTTP)
                .findFirst()
                .map((Function<Selector, Maybe<Flow>>) selector -> evaluate(ctx.request().method(), flow) ? Maybe.just(flow) : Maybe.empty())
                .orElse(Maybe.just(flow));
    }
}
