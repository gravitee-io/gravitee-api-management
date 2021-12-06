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
package io.gravitee.gateway.flow.el;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.flow.Flow;
import io.gravitee.el.TemplateEngine;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.core.condition.ConditionEvaluator;
import io.gravitee.gateway.core.condition.ExpressionLanguageStringConditionEvaluator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ExpressionLanguageStringConditionEvaluatorTest {

    private final ConditionEvaluator<String> evaluator = new ExpressionLanguageStringConditionEvaluator();

    @Mock
    private ExecutionContext context;

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private Flow flow;

    @Before
    public void setUp() {
        when(context.getTemplateEngine()).thenReturn(templateEngine);
    }

    @Test
    public void shouldEvaluate_noCondition() {
        assertTrue(evaluator.evaluate(flow, context));
    }

    @Test
    public void shouldEvaluate_emptyCondition() {
        when(flow.getCondition()).thenReturn("");
        assertTrue(evaluator.evaluate(flow, context));
    }

    @Test
    public void shouldEvaluate_validCondition() {
        when(flow.getCondition()).thenReturn("my-condition");
        when(templateEngine.getValue(eq("my-condition"), eq(Boolean.class))).thenReturn(true);

        assertTrue(evaluator.evaluate(flow, context));
    }

    @Test
    public void shouldEvaluate_invalidCondition() {
        when(flow.getCondition()).thenReturn("invalid-condition");
        when(templateEngine.getValue(eq("invalid-condition"), eq(Boolean.class))).thenReturn(false);

        assertFalse(evaluator.evaluate(flow, context));
    }
}
