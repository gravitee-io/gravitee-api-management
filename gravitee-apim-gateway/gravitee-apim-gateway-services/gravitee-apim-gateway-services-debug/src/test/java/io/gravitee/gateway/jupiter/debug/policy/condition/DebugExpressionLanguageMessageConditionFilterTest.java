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
package io.gravitee.gateway.jupiter.debug.policy.condition;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

import io.gravitee.definition.model.debug.DebugStepStatus;
import io.gravitee.el.TemplateEngine;
import io.gravitee.gateway.jupiter.api.ExecutionPhase;
import io.gravitee.gateway.jupiter.api.message.DefaultMessage;
import io.gravitee.gateway.jupiter.api.message.Message;
import io.gravitee.gateway.jupiter.api.policy.Policy;
import io.gravitee.gateway.jupiter.debug.policy.steps.PolicyRequestStep;
import io.gravitee.gateway.jupiter.debug.reactor.context.DebugExecutionContext;
import io.gravitee.gateway.jupiter.policy.ConditionalPolicy;
import io.reactivex.rxjava3.core.Maybe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class DebugExpressionLanguageMessageConditionFilterTest {

    private static final String CONDITION = "test";
    private static final String MESSAGE_CONDITION = "test";

    private static final Message MESSAGE = new DefaultMessage("test");

    @Mock
    private DebugExecutionContext debugCtx;

    @Mock
    private TemplateEngine templateEngine;

    private DebugExpressionLanguageMessageConditionFilter messageConditionFilter;
    private ConditionalPolicy conditionalPolicy;
    private PolicyRequestStep policyRequestStep;

    @BeforeEach
    public void beforeEach() {
        DebugExpressionLanguageConditionFilter conditionFilter = new DebugExpressionLanguageConditionFilter();
        messageConditionFilter = new DebugExpressionLanguageMessageConditionFilter();
        conditionalPolicy =
            new ConditionalPolicy(mock(Policy.class), CONDITION, MESSAGE_CONDITION, conditionFilter, messageConditionFilter);
        when(debugCtx.getTemplateEngine(MESSAGE)).thenReturn(templateEngine);
        policyRequestStep = new PolicyRequestStep("policyId", ExecutionPhase.REQUEST, "flowPhase");
        doReturn(policyRequestStep).when(debugCtx).getCurrentDebugStep();
    }

    @Test
    void shouldNotSkippedWhenConditionEvaluatedToTrue() {
        when(templateEngine.eval(CONDITION, Boolean.class)).thenReturn(Maybe.just(true));

        messageConditionFilter.filter(debugCtx, conditionalPolicy, MESSAGE).test().assertResult(conditionalPolicy);
        assertThat(policyRequestStep.getCondition()).isEqualTo(CONDITION);
        assertThat(policyRequestStep.getStatus()).isNotEqualTo(DebugStepStatus.SKIPPED);
    }

    @Test
    void shouldSkippedWhenConditionEvaluatedToFalse() {
        when(templateEngine.eval(CONDITION, Boolean.class)).thenReturn(Maybe.just(false));

        messageConditionFilter.filter(debugCtx, conditionalPolicy, MESSAGE).test().assertResult();
        assertThat(policyRequestStep.getCondition()).isEqualTo(CONDITION);
        assertThat(policyRequestStep.getStatus()).isEqualTo(DebugStepStatus.SKIPPED);
    }
}
