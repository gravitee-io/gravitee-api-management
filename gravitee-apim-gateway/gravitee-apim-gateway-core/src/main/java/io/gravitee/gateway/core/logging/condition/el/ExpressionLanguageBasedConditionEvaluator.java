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
package io.gravitee.gateway.core.logging.condition.el;

import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.el.EvaluableRequest;
import io.gravitee.gateway.core.condition.ConditionEvaluator;
import io.gravitee.gateway.core.condition.EvaluableExecutionContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ParseException;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ExpressionLanguageBasedConditionEvaluator implements ConditionEvaluator<Request> {

    private static final String EXPRESSION_REGEX = "\\{([^#|T|(])";
    private static final String EXPRESSION_REGEX_SUBSTITUTE = "{'{'}$1";

    private Expression expression;
    private static final Expression FALSE_EXPRESSION = new LiteralExpression("false");

    public ExpressionLanguageBasedConditionEvaluator(final String condition) {
        if (condition != null) {
            try {
                this.expression =
                    new SpelExpressionParser().parseExpression(condition.replaceAll(EXPRESSION_REGEX, EXPRESSION_REGEX_SUBSTITUTE));
            } catch (ParseException e) {
                this.expression = FALSE_EXPRESSION;
            }
        }
    }

    @Override
    public boolean evaluate(Request request, ExecutionContext executionContext) {
        if (expression != null) {
            StandardEvaluationContext context = new StandardEvaluationContext();
            context.setVariable("request", new EvaluableRequest(request));
            context.setVariable("context", new EvaluableExecutionContext(executionContext));
            return this.expression.getValue(context, Boolean.class);
        }

        return true;
    }
}
