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

import static io.gravitee.gateway.reactive.core.condition.CompositeConditionFilterTest.MOCK_EXCEPTION;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.ConditionSupplier;
import io.gravitee.el.TemplateEngine;
import io.gravitee.el.exceptions.ExpressionEvaluationException;
import io.gravitee.gateway.reactive.api.context.http.HttpPlainExecutionContext;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.observers.TestObserver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class ExpressionLanguageConditionFilterTest {

    protected static final String EXPRESSION = "test";
    final ExpressionLanguageConditionFilter<ConditionSupplier> cut = new ExpressionLanguageConditionFilter<>();

    @Mock
    private HttpPlainExecutionContext ctx;

    @Mock
    private TemplateEngine templateEngine;

    @Test
    void shouldNotFilterWhenConditionEvaluatedToTrue() {
        final ConditionSupplier conditionSupplier = () -> EXPRESSION;
        when(ctx.getTemplateEngine()).thenReturn(templateEngine);
        when(templateEngine.eval(EXPRESSION, Boolean.class)).thenReturn(Maybe.just(true));

        final TestObserver<ConditionSupplier> obs = cut.filter(ctx, conditionSupplier).test();

        obs.assertResult(conditionSupplier);
    }

    @Test
    void shouldNotFilterWhenEmptyCondition() {
        final ConditionSupplier conditionSupplier = () -> "";

        final TestObserver<ConditionSupplier> obs = cut.filter(ctx, conditionSupplier).test();

        obs.assertResult(conditionSupplier);
    }

    @Test
    void shouldNotFilterWhenNullCondition() {
        final ConditionSupplier conditionSupplier = () -> null;
        final TestObserver<ConditionSupplier> obs = cut.filter(ctx, conditionSupplier).test();

        obs.assertResult(conditionSupplier);
    }

    @Test
    void shouldFilterWhenConditionEvaluatedToFalse() {
        final ConditionSupplier conditionSupplier = () -> EXPRESSION;
        when(ctx.getTemplateEngine()).thenReturn(templateEngine);
        when(templateEngine.eval(EXPRESSION, Boolean.class)).thenReturn(Maybe.just(false));

        final TestObserver<ConditionSupplier> obs = cut.filter(ctx, conditionSupplier).test();

        obs.assertResult();
        obs.assertNoValues();
    }

    @Test
    void shouldFilterWhenExpressionEvaluationException() {
        final ConditionSupplier conditionSupplier = () -> EXPRESSION;
        when(ctx.getTemplateEngine()).thenReturn(templateEngine);
        when(templateEngine.eval(EXPRESSION, Boolean.class)).thenReturn(Maybe.error(new ExpressionEvaluationException(EXPRESSION)));

        cut.filter(ctx, conditionSupplier).test().assertResult();
    }

    @Test
    void shouldThrowErrorWhenErrorOccured() {
        final ConditionSupplier conditionSupplier = () -> EXPRESSION;
        when(ctx.getTemplateEngine()).thenReturn(templateEngine);
        when(templateEngine.eval(EXPRESSION, Boolean.class)).thenReturn(Maybe.error(new RuntimeException(MOCK_EXCEPTION)));

        cut.filter(ctx, conditionSupplier).test().assertFailure(RuntimeException.class);
    }
}
