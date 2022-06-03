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
package io.gravitee.gateway.jupiter.debug.policy.condition;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

import io.gravitee.definition.model.debug.DebugStepStatus;
import io.gravitee.el.TemplateEngine;
import io.gravitee.gateway.jupiter.api.ExecutionPhase;
import io.gravitee.gateway.jupiter.api.policy.Policy;
import io.gravitee.gateway.jupiter.debug.policy.steps.PolicyRequestStep;
import io.gravitee.gateway.jupiter.debug.reactor.context.DebugRequestExecutionContext;
import io.gravitee.gateway.jupiter.policy.ConditionalPolicy;
import io.reactivex.Maybe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class DebugExpressionLanguageConditionFilterTest {

    private static final String EXPRESSION = "test";

    @Mock
    private DebugRequestExecutionContext debugCtx;

    @Mock
    private TemplateEngine templateEngine;

    private DebugExpressionLanguageConditionFilter conditionFilter;
    private ConditionalPolicy conditionalPolicy;
    private PolicyRequestStep policyRequestStep;

    @BeforeEach
    public void beforeEach() {
        conditionFilter = new DebugExpressionLanguageConditionFilter();
        conditionalPolicy = new ConditionalPolicy(mock(Policy.class), EXPRESSION, conditionFilter);
        when(debugCtx.getTemplateEngine()).thenReturn(templateEngine);
        policyRequestStep = new PolicyRequestStep("policyId", ExecutionPhase.REQUEST, "flowPhase");
        doReturn(policyRequestStep).when(debugCtx).getCurrentDebugStep();
    }

    @Test
    void shouldNotSkippedWhenConditionEvaluatedToTrue() {
        when(templateEngine.eval(EXPRESSION, Boolean.class)).thenReturn(Maybe.just(true));

        conditionFilter.filter(debugCtx, conditionalPolicy).test().assertResult(conditionalPolicy);
        assertThat(policyRequestStep.getCondition()).isEqualTo(EXPRESSION);
        assertThat(policyRequestStep.getStatus()).isNotEqualTo(DebugStepStatus.SKIPPED);
    }

    @Test
    void shouldSkippedWhenConditionEvaluatedToFalse() {
        when(templateEngine.eval(EXPRESSION, Boolean.class)).thenReturn(Maybe.just(false));

        conditionFilter.filter(debugCtx, conditionalPolicy).test().assertResult();
        assertThat(policyRequestStep.getCondition()).isEqualTo(EXPRESSION);
        assertThat(policyRequestStep.getStatus()).isEqualTo(DebugStepStatus.SKIPPED);
    }
}
