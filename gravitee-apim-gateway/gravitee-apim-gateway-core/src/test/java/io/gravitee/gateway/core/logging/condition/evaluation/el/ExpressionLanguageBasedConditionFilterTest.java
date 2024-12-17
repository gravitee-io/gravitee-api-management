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
package io.gravitee.gateway.core.logging.condition.evaluation.el;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import io.gravitee.el.TemplateEngine;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.core.logging.condition.el.ExpressionLanguageBasedConditionEvaluator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ExpressionLanguageBasedConditionFilterTest {

    @Mock
    private ExecutionContext context;

    private final TemplateEngine templateEngine = TemplateEngine.templateEngine();

    @BeforeEach
    public void setUp() {
        lenient().when(context.getTemplateEngine()).thenReturn(templateEngine);
    }

    @Test
    public void shouldEvalTrueWithEmpty() {
        ExpressionLanguageBasedConditionEvaluator evaluator = new ExpressionLanguageBasedConditionEvaluator(null);
        Request request = mock(Request.class);
        boolean evaluate = evaluator.evaluate(context, request);

        assertThat(evaluate).isTrue();
    }

    @Test
    public void shouldEvalTrueWithTrue() {
        ExpressionLanguageBasedConditionEvaluator evaluator = new ExpressionLanguageBasedConditionEvaluator("true");
        Request request = mock(Request.class);
        boolean evaluate = evaluator.evaluate(context, request);

        assertThat(evaluate).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = { "foo bar", "false" })
    public void shouldEvalFalse(String input) {
        ExpressionLanguageBasedConditionEvaluator evaluator = new ExpressionLanguageBasedConditionEvaluator(input);
        Request request = mock(Request.class);
        boolean evaluate = evaluator.evaluate(context, request);

        assertThat(evaluate).isFalse();
    }
}
