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
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.ConditionSupplier;
import io.gravitee.el.TemplateEngine;
import io.gravitee.el.exceptions.ExpressionEvaluationException;
import io.gravitee.gateway.reactive.api.ExecutionWarn;
import io.gravitee.gateway.reactive.api.context.http.HttpPlainExecutionContext;
import io.reactivex.rxjava3.core.Maybe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class ExpressionLanguageConditionFilterTest {

    protected static final String EXPRESSION = "test";
    static final String EXPRESSION_EVALUATION_ERROR = "EXPRESSION_EVALUATION_ERROR";
    final ExpressionLanguageConditionFilter<ConditionSupplier> cut = new ExpressionLanguageConditionFilter<>();

    @Mock
    private HttpPlainExecutionContext ctx;

    @Mock
    private TemplateEngine templateEngine;

    @BeforeEach
    void setUp() {
        lenient().when(ctx.withLogger(any())).thenReturn(LoggerFactory.getLogger(ExpressionLanguageConditionFilter.class));
    }

    @Test
    void shouldNotFilterWhenConditionEvaluatedToTrue() {
        final ConditionSupplier conditionSupplier = () -> EXPRESSION;
        when(ctx.getTemplateEngine()).thenReturn(templateEngine);
        when(templateEngine.eval(EXPRESSION, Boolean.class)).thenReturn(Maybe.just(true));

        final var obs = cut.filter(ctx, conditionSupplier).test();

        obs.assertResult(conditionSupplier);
    }

    @Test
    void shouldNotFilterWhenEmptyCondition() {
        final ConditionSupplier conditionSupplier = () -> "";

        final var obs = cut.filter(ctx, conditionSupplier).test();

        obs.assertResult(conditionSupplier);
    }

    @Test
    void shouldNotFilterWhenNullCondition() {
        final ConditionSupplier conditionSupplier = () -> null;
        final var obs = cut.filter(ctx, conditionSupplier).test();

        obs.assertResult(conditionSupplier);
    }

    @Test
    void shouldFilterWhenConditionEvaluatedToFalse() {
        final ConditionSupplier conditionSupplier = () -> EXPRESSION;
        when(ctx.getTemplateEngine()).thenReturn(templateEngine);
        when(templateEngine.eval(EXPRESSION, Boolean.class)).thenReturn(Maybe.just(false));

        final var obs = cut.filter(ctx, conditionSupplier).test();

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
    void shouldFilterWhenErrorOccured() {
        final ConditionSupplier conditionSupplier = () -> EXPRESSION;
        when(ctx.getTemplateEngine()).thenReturn(templateEngine);
        when(templateEngine.eval(EXPRESSION, Boolean.class)).thenReturn(Maybe.error(new RuntimeException(MOCK_EXCEPTION)));

        cut.filter(ctx, conditionSupplier).test().assertResult();

        assertThat(capturedWarn().key()).isEqualTo(EXPRESSION_EVALUATION_ERROR);
    }

    /**
     * The template engine does not surface every EL failure as an {@link ExpressionEvaluationException}: a condition
     * that fails to <em>parse</em> comes out as a bare {@link IllegalArgumentException} whose cause has been discarded.
     * These cases go through the real engine on purpose — mocking the error would only assert the type this test exists
     * to stop relying on.
     */
    @Nested
    @DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
    class WithRealTemplateEngine {

        /** Missing right-hand operand: accepted by the console, rejected by the SpEL parser with EL1042E. */
        private static final String UNPARSEABLE_CONDITION = "{#request.headers[\"x\"][0] ==}";

        @BeforeEach
        void setUp() {
            when(ctx.getTemplateEngine()).thenReturn(TemplateEngine.templateEngine());
        }

        @Test
        void should_filter_and_let_the_execution_continue_when_the_condition_cannot_be_parsed() {
            cut
                .filter(ctx, () -> UNPARSEABLE_CONDITION)
                .test()
                .assertResult();
        }

        @Test
        void should_report_a_warning_carrying_the_parse_failure_when_the_condition_cannot_be_parsed() {
            cut
                .filter(ctx, () -> UNPARSEABLE_CONDITION)
                .test()
                .assertResult();

            final ExecutionWarn warn = capturedWarn();
            assertThat(warn.key()).isEqualTo(EXPRESSION_EVALUATION_ERROR);
            assertThat(warn.message()).contains(UNPARSEABLE_CONDITION);
            assertThat(warn.cause()).hasMessageContaining("EL1042E");
        }
    }

    private ExecutionWarn capturedWarn() {
        final ArgumentCaptor<ExecutionWarn> captor = ArgumentCaptor.forClass(ExecutionWarn.class);
        verify(ctx).warnWith(captor.capture());
        return captor.getValue();
    }
}
