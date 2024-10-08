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
package io.gravitee.gateway.reactive.v4.flow.selection;

import static org.mockito.Mockito.when;

import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.selector.ConditionSelector;
import io.gravitee.definition.model.v4.flow.selector.SelectorType;
import io.gravitee.el.TemplateEngine;
import io.gravitee.el.exceptions.ExpressionEvaluationException;
import io.gravitee.gateway.reactive.api.context.http.HttpPlainExecutionContext;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.observers.TestObserver;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class ConditionSelectorConditionFilterTest {

    protected static final String EXPRESSION = "test";
    final ConditionSelectorConditionFilter cut = new ConditionSelectorConditionFilter();

    @Mock
    private HttpPlainExecutionContext ctx;

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private Flow flow;

    @Test
    void shouldNotFilterWithNoConditionSelector() {
        when(flow.selectorByType(SelectorType.CONDITION)).thenReturn(Optional.empty());

        final TestObserver<Flow> obs = cut.filter(ctx, flow).test();

        obs.assertResult(flow);
    }

    @Test
    void shouldNotFilterWhenConditionEvaluatedToTrue() {
        ConditionSelector conditionSelector = new ConditionSelector();
        conditionSelector.setCondition(EXPRESSION);
        when(flow.selectorByType(SelectorType.CONDITION)).thenReturn(Optional.of(conditionSelector));

        when(ctx.getTemplateEngine()).thenReturn(templateEngine);
        when(templateEngine.eval(EXPRESSION, Boolean.class)).thenReturn(Maybe.just(true));

        final TestObserver<Flow> obs = cut.filter(ctx, flow).test();

        obs.assertResult(flow);
    }

    @Test
    void shouldNotFilterWhenEmptyCondition() {
        ConditionSelector conditionSelector = new ConditionSelector();
        conditionSelector.setCondition("");
        when(flow.selectorByType(SelectorType.CONDITION)).thenReturn(Optional.of(conditionSelector));
        final TestObserver<Flow> obs = cut.filter(ctx, flow).test();

        obs.assertResult(flow);
    }

    @Test
    void shouldNotFilterWhenNullCondition() {
        ConditionSelector conditionSelector = new ConditionSelector();
        when(flow.selectorByType(SelectorType.CONDITION)).thenReturn(Optional.of(conditionSelector));
        final TestObserver<Flow> obs = cut.filter(ctx, flow).test();

        obs.assertResult(flow);
    }

    @Test
    void shouldFilterWhenConditionEvaluatedToFalse() {
        ConditionSelector conditionSelector = new ConditionSelector();
        conditionSelector.setCondition(EXPRESSION);
        when(flow.selectorByType(SelectorType.CONDITION)).thenReturn(Optional.of(conditionSelector));
        when(ctx.getTemplateEngine()).thenReturn(templateEngine);
        when(templateEngine.eval(EXPRESSION, Boolean.class)).thenReturn(Maybe.just(false));

        final TestObserver<Flow> obs = cut.filter(ctx, flow).test();
        obs.assertResult();
        obs.assertNoValues();
    }

    @Test
    void shouldFilterWhenExpressionEvaluationException() {
        ConditionSelector conditionSelector = new ConditionSelector();
        conditionSelector.setCondition(EXPRESSION);
        when(flow.selectorByType(SelectorType.CONDITION)).thenReturn(Optional.of(conditionSelector));
        when(ctx.getTemplateEngine()).thenReturn(templateEngine);
        when(templateEngine.eval(EXPRESSION, Boolean.class)).thenReturn(Maybe.error(new ExpressionEvaluationException(EXPRESSION)));

        cut.filter(ctx, flow).test().assertResult();
    }

    @Test
    void shouldThrowErrorWhenErrorOccurred() {
        ConditionSelector conditionSelector = new ConditionSelector();
        conditionSelector.setCondition(EXPRESSION);
        when(flow.selectorByType(SelectorType.CONDITION)).thenReturn(Optional.of(conditionSelector));
        when(ctx.getTemplateEngine()).thenReturn(templateEngine);
        when(templateEngine.eval(EXPRESSION, Boolean.class)).thenReturn(Maybe.error(new RuntimeException("Mock exception")));

        cut.filter(ctx, flow).test().assertResult();
    }
}
