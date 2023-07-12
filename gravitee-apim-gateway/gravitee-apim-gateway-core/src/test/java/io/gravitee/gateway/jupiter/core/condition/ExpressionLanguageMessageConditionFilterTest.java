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
package io.gravitee.gateway.jupiter.core.condition;

import static io.gravitee.gateway.jupiter.core.condition.CompositeConditionFilterTest.MOCK_EXCEPTION;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.MessageConditionSupplier;
import io.gravitee.el.TemplateEngine;
import io.gravitee.el.exceptions.ExpressionEvaluationException;
import io.gravitee.gateway.jupiter.api.context.MessageExecutionContext;
import io.gravitee.gateway.jupiter.api.message.DefaultMessage;
import io.gravitee.gateway.jupiter.api.message.Message;
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
class ExpressionLanguageMessageConditionFilterTest {

    private static final String EXPRESSION = "test";
    private static final Message MESSAGE = new DefaultMessage("test");

    final ExpressionLanguageMessageConditionFilter<MessageConditionSupplier> cut = new ExpressionLanguageMessageConditionFilter<>();

    @Mock
    private MessageExecutionContext ctx;

    @Mock
    private TemplateEngine templateEngine;

    @Test
    void shouldNotFilterWhenConditionEvaluatedToTrue() {
        final MessageConditionSupplier conditionSupplier = () -> EXPRESSION;
        when(ctx.getTemplateEngine(MESSAGE)).thenReturn(templateEngine);
        when(templateEngine.eval(EXPRESSION, Boolean.class)).thenReturn(Maybe.just(true));

        final TestObserver<MessageConditionSupplier> obs = cut.filter(ctx, conditionSupplier, MESSAGE).test();

        obs.assertResult(conditionSupplier);
    }

    @Test
    void shouldNotFilterWhenEmptyCondition() {
        final MessageConditionSupplier conditionSupplier = () -> "";

        final TestObserver<MessageConditionSupplier> obs = cut.filter(ctx, conditionSupplier, MESSAGE).test();

        obs.assertResult(conditionSupplier);
    }

    @Test
    void shouldNotFilterWhenNullCondition() {
        final MessageConditionSupplier conditionSupplier = () -> null;
        final TestObserver<MessageConditionSupplier> obs = cut.filter(ctx, conditionSupplier, MESSAGE).test();

        obs.assertResult(conditionSupplier);
    }

    @Test
    void shouldFilterWhenConditionEvaluatedToFalse() {
        final MessageConditionSupplier conditionSupplier = () -> EXPRESSION;
        when(ctx.getTemplateEngine(MESSAGE)).thenReturn(templateEngine);
        when(templateEngine.eval(EXPRESSION, Boolean.class)).thenReturn(Maybe.just(false));

        final TestObserver<MessageConditionSupplier> obs = cut.filter(ctx, conditionSupplier, MESSAGE).test();

        obs.assertResult();
        obs.assertNoValues();
    }

    @Test
    void shouldFilterWhenExpressionEvaluationException() {
        final MessageConditionSupplier conditionSupplier = () -> EXPRESSION;
        when(ctx.getTemplateEngine(MESSAGE)).thenReturn(templateEngine);
        when(templateEngine.eval(EXPRESSION, Boolean.class)).thenReturn(Maybe.error(new ExpressionEvaluationException(EXPRESSION)));

        cut.filter(ctx, conditionSupplier, MESSAGE).test().assertResult();
    }

    @Test
    void shouldThrowErrorWhenErrorOccured() {
        final MessageConditionSupplier conditionSupplier = () -> EXPRESSION;
        when(ctx.getTemplateEngine(MESSAGE)).thenReturn(templateEngine);
        when(templateEngine.eval(EXPRESSION, Boolean.class)).thenReturn(Maybe.error(new RuntimeException(MOCK_EXCEPTION)));

        cut.filter(ctx, conditionSupplier, MESSAGE).test().assertFailure(RuntimeException.class);
    }
}
