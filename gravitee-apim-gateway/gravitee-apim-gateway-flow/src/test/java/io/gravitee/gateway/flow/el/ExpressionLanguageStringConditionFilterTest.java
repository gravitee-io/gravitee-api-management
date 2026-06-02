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
package io.gravitee.gateway.flow.el;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.gravitee.el.TemplateEngine;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.core.condition.ConditionEvaluator;
import io.gravitee.gateway.core.condition.ExpressionLanguageStringConditionEvaluator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class ExpressionLanguageStringConditionFilterTest {

    private final ConditionEvaluator<String> evaluator = new ExpressionLanguageStringConditionEvaluator();

    @Mock
    private ExecutionContext context;

    @Mock
    private TemplateEngine templateEngine;

    @BeforeEach
    public void setUp() {
        when(context.getTemplateEngine()).thenReturn(templateEngine);
    }

    @Test
    public void shouldEvaluate_noCondition() {
        assertTrue(evaluator.evaluate(context, null));
    }

    @Test
    public void shouldEvaluate_emptyCondition() {
        assertTrue(evaluator.evaluate(context, ""));
    }

    @Test
    public void shouldEvaluate_validCondition() {
        when(templateEngine.getValue(eq("my-condition"), eq(Boolean.class))).thenReturn(true);

        assertTrue(evaluator.evaluate(context, "my-condition"));
    }

    @Test
    public void shouldEvaluate_invalidCondition() {
        when(templateEngine.getValue(eq("invalid-condition"), eq(Boolean.class))).thenReturn(false);

        assertFalse(evaluator.evaluate(context, "invalid-condition"));
    }
}
