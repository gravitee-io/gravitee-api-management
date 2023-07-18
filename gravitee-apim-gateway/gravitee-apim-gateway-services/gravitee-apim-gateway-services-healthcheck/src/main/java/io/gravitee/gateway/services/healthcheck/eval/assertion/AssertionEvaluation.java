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
package io.gravitee.gateway.services.healthcheck.eval.assertion;

import io.gravitee.el.exceptions.ExpressionEvaluationException;
import io.gravitee.el.spel.SpelTemplateEngine;
import io.gravitee.gateway.services.healthcheck.eval.Evaluation;
import io.gravitee.gateway.services.healthcheck.eval.EvaluationException;
import io.gravitee.gateway.services.healthcheck.eval.assertion.spel.AssertionSpelExpressionParser;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class AssertionEvaluation implements Evaluation {

    private static final AssertionSpelExpressionParser EXPRESSION_PARSER = new AssertionSpelExpressionParser();

    private final SpelTemplateEngine templateEngine = new SpelTemplateEngine(EXPRESSION_PARSER);

    private final String assertion;

    public AssertionEvaluation(final String assertion) {
        this.assertion = assertion;
    }

    @Override
    public boolean validate() throws EvaluationException {
        try {
            return templateEngine.getValue(assertion, Boolean.class);
        } catch (ExpressionEvaluationException eee) {
            throw new EvaluationException("Assertion cannot be verified : " + assertion, eee);
        }
    }

    public void setVariable(String variable, Object value) {
        templateEngine.getTemplateContext().setVariable(variable, value);
    }

    public String getAssertion() {
        return assertion;
    }
}
