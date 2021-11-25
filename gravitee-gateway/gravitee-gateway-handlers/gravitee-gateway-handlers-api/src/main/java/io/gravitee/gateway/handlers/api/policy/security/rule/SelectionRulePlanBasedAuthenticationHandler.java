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
package io.gravitee.gateway.handlers.api.policy.security.rule;

import io.gravitee.definition.model.Plan;
import io.gravitee.gateway.api.el.EvaluableRequest;
import io.gravitee.gateway.handlers.api.policy.security.PlanBasedAuthenticationHandler;
import io.gravitee.gateway.security.core.AuthenticationContext;
import io.gravitee.gateway.security.core.AuthenticationHandler;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.ParseException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SelectionRulePlanBasedAuthenticationHandler extends PlanBasedAuthenticationHandler {

    private static final String EXPRESSION_REGEX = "\\{([^#|T|(])";
    private static final String EXPRESSION_REGEX_SUBSTITUTE = "{'{'}$1";

    public SelectionRulePlanBasedAuthenticationHandler(final AuthenticationHandler handler, final Plan plan) {
        super(handler, plan);
    }

    @Override
    public boolean canHandle(AuthenticationContext context) {
        boolean handle = handler.canHandle(context);

        if (!handle) {
            return false;
        }

        try {
            Expression expression = new SpelExpressionParser()
            .parseExpression(plan.getSelectionRule().replaceAll(EXPRESSION_REGEX, EXPRESSION_REGEX_SUBSTITUTE));

            StandardEvaluationContext evaluation = new StandardEvaluationContext();
            evaluation.setVariable("request", new EvaluableRequest(context.request()));
            evaluation.setVariable("context", new EvaluableAuthenticationContext(context));

            return expression.getValue(evaluation, Boolean.class);
        } catch (ParseException | EvaluationException ex) {
            return false;
        }
    }
}
