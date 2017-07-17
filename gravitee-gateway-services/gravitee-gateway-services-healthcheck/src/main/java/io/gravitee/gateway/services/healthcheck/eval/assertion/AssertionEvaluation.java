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
package io.gravitee.gateway.services.healthcheck.eval.assertion;

import io.gravitee.gateway.el.function.JsonPathFunction;
import io.gravitee.gateway.services.healthcheck.eval.Evaluation;
import io.gravitee.gateway.services.healthcheck.eval.EvaluationException;
import org.springframework.beans.BeanUtils;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.HashMap;
import java.util.Map;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class AssertionEvaluation implements Evaluation {

    private final String assertion;

    private final Map<String, Object> variables = new HashMap<>();

    public AssertionEvaluation(final String assertion) {
        this.assertion = assertion;
    }

    @Override
    public boolean validate() throws EvaluationException {
        try {
            final ExpressionParser parser = new SpelExpressionParser();
            final Expression expr = parser.parseExpression(assertion);

            final StandardEvaluationContext context = new StandardEvaluationContext();
            context.registerFunction("jsonPath",
                    BeanUtils.resolveSignature("evaluate", JsonPathFunction.class));
            context.setVariables(variables);

            return expr.getValue(context, boolean.class);
        } catch (SpelEvaluationException spelex) {
            throw new EvaluationException("Assertion can not be verified : " + assertion, spelex);
        }
    }

    public void setVariable(String variable, Object value) {
        this.variables.put(variable, value);
    }

    public String getAssertion() {
        return assertion;
    }
}
