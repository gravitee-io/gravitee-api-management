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
package io.gravitee.gateway.jupiter.v4.flow.selection;

import io.gravitee.definition.model.flow.Operator;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.selector.HttpSelector;
import io.gravitee.definition.model.v4.flow.selector.Selector;
import io.gravitee.definition.model.v4.flow.selector.SelectorType;
import io.gravitee.gateway.flow.condition.evaluation.PathPatterns;
import io.gravitee.gateway.jupiter.api.context.GenericExecutionContext;
import io.gravitee.gateway.jupiter.core.condition.ConditionFilter;
import io.reactivex.rxjava3.core.Maybe;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * This {@link ConditionFilter} evaluates to true if the request is matching the
 * http selector declared within the {@link Flow}.
 *
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HttpSelectorConditionFilter implements ConditionFilter<Flow> {

    private final PathPatterns pathPatterns = new PathPatterns();

    @Override
    public Maybe<Flow> filter(final GenericExecutionContext ctx, final Flow flow) {
        return Maybe.fromCallable(() -> {
            Optional<Selector> selectorOptional = flow.selectorByType(SelectorType.HTTP);
            if (selectorOptional.isPresent()) {
                HttpSelector httpSelector = (HttpSelector) selectorOptional.get();
                if (isMethodMatches(ctx, httpSelector) && isPathMatches(ctx, httpSelector)) {
                    return flow;
                }
            } else {
                return flow;
            }
            return null;
        });
    }

    private boolean isPathMatches(final GenericExecutionContext ctx, final HttpSelector httpSelector) {
        Pattern pattern = pathPatterns.getOrCreate(httpSelector.getPath());
        String pathInfo = ctx.request().pathInfo();
        return (httpSelector.getPathOperator() == Operator.EQUALS)
            ? pattern.matcher(pathInfo).matches()
            : pattern.matcher(pathInfo).lookingAt();
    }

    private boolean isMethodMatches(final GenericExecutionContext ctx, final HttpSelector httpSelector) {
        return (
            httpSelector.getMethods() == null ||
            httpSelector.getMethods().isEmpty() ||
            httpSelector.getMethods().contains(ctx.request().method())
        );
    }
}
