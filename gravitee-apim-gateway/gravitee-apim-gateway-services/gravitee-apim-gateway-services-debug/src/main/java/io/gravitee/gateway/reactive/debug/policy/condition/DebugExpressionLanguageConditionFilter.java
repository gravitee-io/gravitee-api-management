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
package io.gravitee.gateway.reactive.debug.policy.condition;

import io.gravitee.gateway.reactive.api.context.base.BaseExecutionContext;
import io.gravitee.gateway.reactive.core.condition.ExpressionLanguageConditionFilter;
import io.gravitee.gateway.reactive.debug.reactor.context.DebugExecutionContext;
import io.gravitee.gateway.reactive.policy.HttpConditionalPolicy;
import io.reactivex.rxjava3.core.Maybe;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DebugExpressionLanguageConditionFilter extends ExpressionLanguageConditionFilter<HttpConditionalPolicy> {

    @Override
    public Maybe<HttpConditionalPolicy> filter(final BaseExecutionContext ctx, final HttpConditionalPolicy elt) {
        return super
            .filter(ctx, elt)
            .doOnEvent((conditionalPolicy, throwable) -> {
                if (ctx instanceof DebugExecutionContext && throwable == null) {
                    DebugExecutionContext debugCtx = (DebugExecutionContext) ctx;
                    boolean isConditionTruthy = conditionalPolicy != null;
                    debugCtx.getCurrentDebugStep().onConditionFilter(elt.getCondition(), isConditionTruthy);
                }
            });
    }
}
