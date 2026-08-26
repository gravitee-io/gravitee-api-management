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
package io.gravitee.gateway.reactive.core.condition;

import io.gravitee.definition.model.ConditionSupplier;
import io.gravitee.gateway.reactive.api.ExecutionWarn;
import io.gravitee.gateway.reactive.api.context.base.BaseExecutionContext;
import io.reactivex.rxjava3.core.Maybe;
import lombok.CustomLog;

/**
 * {@link ConditionFilter} base on an EL expression.
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
public class ExpressionLanguageConditionFilter<T extends ConditionSupplier> implements ConditionFilter<BaseExecutionContext, T> {

    @Override
    public Maybe<T> filter(BaseExecutionContext ctx, T elt) {
        final String condition = elt.getCondition();

        if (condition == null || condition.isEmpty()) {
            return Maybe.just(elt);
        }

        return ctx
            .getTemplateEngine()
            .eval(condition, Boolean.class)
            .filter(Boolean::booleanValue)
            .map(aBoolean -> elt)
            .doOnError(throwable -> {
                ctx.withLogger(log).warn("Error parsing condition {}", condition, throwable);
                ctx.warnWith(
                    new ExecutionWarn("EXPRESSION_EVALUATION_ERROR").message("Unable to execute EL condition " + condition).cause(throwable)
                );
            })
            // A condition the gateway cannot evaluate is a configuration defect, and the element simply does not pass
            // the filter — it is never a reason to fail live traffic. Which failures qualify cannot be decided from the
            // exception type: the template engine reports a parse failure as a bare IllegalArgumentException built by
            // ExpressionEvaluationException#buildCause, which drops the original cause on the way.
            .onErrorComplete();
    }
}
