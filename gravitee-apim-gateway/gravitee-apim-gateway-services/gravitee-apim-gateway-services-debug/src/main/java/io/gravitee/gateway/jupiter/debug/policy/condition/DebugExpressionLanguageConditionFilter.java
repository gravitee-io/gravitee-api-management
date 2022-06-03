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
package io.gravitee.gateway.jupiter.debug.policy.condition;

import io.gravitee.gateway.jupiter.api.context.RequestExecutionContext;
import io.gravitee.gateway.jupiter.core.condition.ExpressionLanguageConditionFilter;
import io.gravitee.gateway.jupiter.debug.reactor.context.DebugRequestExecutionContext;
import io.gravitee.gateway.jupiter.policy.ConditionalPolicy;
import io.reactivex.Maybe;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DebugExpressionLanguageConditionFilter extends ExpressionLanguageConditionFilter<ConditionalPolicy> {

    @Override
    public Maybe<ConditionalPolicy> filter(final RequestExecutionContext ctx, final ConditionalPolicy elt) {
        return super
            .filter(ctx, elt)
            .doOnEvent(
                (conditionalPolicy, throwable) -> {
                    if (ctx instanceof DebugRequestExecutionContext && throwable == null) {
                        DebugRequestExecutionContext debugCtx = (DebugRequestExecutionContext) ctx;
                        boolean isConditionTruthy = conditionalPolicy != null;
                        debugCtx.getCurrentDebugStep().onConditionFilter(elt.getCondition(), isConditionTruthy);
                    }
                }
            );
    }
}
