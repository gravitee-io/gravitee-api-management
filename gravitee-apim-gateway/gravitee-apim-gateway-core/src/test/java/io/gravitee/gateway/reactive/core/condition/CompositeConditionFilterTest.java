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

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.ConditionSupplier;
import io.gravitee.gateway.reactive.api.context.base.BaseExecutionContext;
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
class CompositeConditionFilterTest {

    protected static final String MOCK_EXCEPTION = "Mock exception";

    @Mock
    private ConditionFilter<BaseExecutionContext, ConditionSupplier> evaluator1;

    @Mock
    private ConditionFilter<BaseExecutionContext, ConditionSupplier> evaluator2;

    @Mock
    private ConditionFilter<BaseExecutionContext, ConditionSupplier> evaluator3;

    @Mock
    private HttpPlainExecutionContext ctx;

    @Mock
    private ConditionSupplier conditionSupplier;

    @Test
    void shouldNotFilterWhenAllEvaluatorsReturnTheValue() {
        final CompositeConditionFilter<BaseExecutionContext, ConditionSupplier> cut = new CompositeConditionFilter<>(
            evaluator1,
            evaluator2,
            evaluator3
        );

        mockFilter(evaluator1);
        mockFilter(evaluator2);
        mockFilter(evaluator3);

        final TestObserver<ConditionSupplier> obs = cut.filter(ctx, conditionSupplier).test();

        obs.assertResult(conditionSupplier);
    }

    @Test
    void shouldFilterWhenOneEvaluatorReturnsEmpty() {
        final CompositeConditionFilter<BaseExecutionContext, ConditionSupplier> cut = new CompositeConditionFilter<>(
            evaluator1,
            evaluator2,
            evaluator3
        );

        mockFilter(evaluator1);
        mockFilter(evaluator2);
        mockFilterEmpty(evaluator3);

        final TestObserver<ConditionSupplier> obs = cut.filter(ctx, conditionSupplier).test();

        obs.assertResult();
        obs.assertNoValues();
    }

    @Test
    void shouldErrorWhenOneEvaluatorReturnsError() {
        final CompositeConditionFilter<BaseExecutionContext, ConditionSupplier> cut = new CompositeConditionFilter<>(
            evaluator1,
            evaluator2,
            evaluator3
        );

        mockFilter(evaluator1);
        mockFilter(evaluator2);
        mockFilterError(evaluator3);

        final TestObserver<ConditionSupplier> obs = cut.filter(ctx, conditionSupplier).test();

        obs.assertError(RuntimeException.class);
        obs.assertError(t -> MOCK_EXCEPTION.equals(t.getMessage()));
    }

    @Test
    void shouldNotContinueWhenReturnsEmpty() {
        final CompositeConditionFilter<BaseExecutionContext, ConditionSupplier> cut = new CompositeConditionFilter<>(
            evaluator1,
            evaluator2,
            evaluator3
        );

        mockFilterEmpty(evaluator1);

        final TestObserver<ConditionSupplier> obs = cut.filter(ctx, conditionSupplier).test();

        obs.assertResult();
        obs.assertNoValues();

        verifyNoInteractions(evaluator2);
        verifyNoInteractions(evaluator3);
    }

    private void mockFilter(ConditionFilter<BaseExecutionContext, ConditionSupplier> filter) {
        when(filter.filter(ctx, conditionSupplier)).thenAnswer(i -> Maybe.just(i.getArgument(1)));
    }

    private void mockFilterEmpty(ConditionFilter<BaseExecutionContext, ConditionSupplier> filter) {
        when(filter.filter(ctx, conditionSupplier)).thenAnswer(i -> Maybe.empty());
    }

    private void mockFilterError(ConditionFilter<BaseExecutionContext, ConditionSupplier> filter) {
        when(filter.filter(ctx, conditionSupplier)).thenAnswer(i -> Maybe.error(new RuntimeException(MOCK_EXCEPTION)));
    }
}
