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
package io.gravitee.gateway.debug.policy.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.PolicyScope;
import io.gravitee.definition.model.debug.DebugStepStatus;
import io.gravitee.el.TemplateEngine;
import io.gravitee.el.exceptions.ExpressionEvaluationException;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.stream.BufferedReadWriteStream;
import io.gravitee.gateway.api.stream.ReadWriteStream;
import io.gravitee.gateway.core.condition.ConditionEvaluator;
import io.gravitee.gateway.core.condition.ExpressionLanguageStringConditionEvaluator;
import io.gravitee.gateway.debug.reactor.handler.context.steps.DebugRequestStep;
import io.gravitee.gateway.debug.reactor.handler.context.steps.DebugStep;
import io.gravitee.gateway.policy.Policy;
import io.gravitee.gateway.policy.PolicyException;
import io.gravitee.gateway.policy.StreamType;
import io.gravitee.gateway.policy.impl.ConditionalExecutablePolicy;
import io.gravitee.policy.api.PolicyChain;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class DebugConditionalExecutablePolicyTest {

    @Mock
    private PolicyChain policyChain;

    @Mock
    private ExecutionContext executionContext;

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private Policy policy;

    private ConditionEvaluator<String> conditionEvaluator;
    private DebugStep<?> debugStep;

    @Before
    public void setUp() {
        when(executionContext.request()).thenReturn(mock(Request.class));
        when(executionContext.response()).thenReturn(mock(Response.class));
        conditionEvaluator = new ExpressionLanguageStringConditionEvaluator();
        debugStep = new DebugRequestStep("id", StreamType.ON_REQUEST, "uuid", PolicyScope.ON_REQUEST);
    }

    @Test
    public void shouldExecuteConditionalPolicyConditionOk() throws PolicyException {
        when(executionContext.getTemplateEngine()).thenReturn(templateEngine);
        when(templateEngine.getValue("condition", Boolean.class)).thenReturn(true);

        ConditionalExecutablePolicy delegatePolicy = new ConditionalExecutablePolicy(policy, "condition", conditionEvaluator);

        final ConditionalExecutablePolicy toDebug = new DebugConditionalExecutablePolicy(delegatePolicy, debugStep);
        toDebug.execute(policyChain, executionContext);
        verify(policyChain, never()).doNext(any(), any());
        assertThat(debugStep.getDebugDiffContent()).containsEntry(DebugStep.DIFF_KEY_CONDITION, "condition");
    }

    @Test
    public void shouldNotExecuteConditionalPolicyConditionEvaluatedToFalse() throws PolicyException {
        when(executionContext.getTemplateEngine()).thenReturn(templateEngine);
        when(templateEngine.getValue("condition", Boolean.class)).thenReturn(false);
        ConditionalExecutablePolicy delegatePolicy = new ConditionalExecutablePolicy(policy, "condition", conditionEvaluator);

        final ConditionalExecutablePolicy toDebug = new DebugConditionalExecutablePolicy(delegatePolicy, debugStep);
        toDebug.execute(policyChain, executionContext);
        verify(policyChain, times(1)).doNext(any(), any());
        assertThat(debugStep.getDebugDiffContent()).containsEntry(DebugStep.DIFF_KEY_CONDITION, "condition");
        assertThat(debugStep.getStatus()).isEqualTo(DebugStepStatus.SKIPPED);
    }

    @Test(expected = PolicyException.class)
    public void shouldNotExecuteConditionalPolicyExpressionEvaluationException() throws PolicyException {
        when(executionContext.getTemplateEngine()).thenReturn(templateEngine);
        when(templateEngine.getValue("condition", Boolean.class)).thenThrow(ExpressionEvaluationException.class);
        ConditionalExecutablePolicy delegatePolicy = new ConditionalExecutablePolicy(policy, "condition", conditionEvaluator);

        final ConditionalExecutablePolicy toDebug = new DebugConditionalExecutablePolicy(delegatePolicy, debugStep);
        toDebug.execute(policyChain, executionContext);
    }

    @Test
    public void shouldStreamConditionalPolicyConditionOk() throws PolicyException, NoSuchMethodException {
        when(executionContext.getTemplateEngine()).thenReturn(templateEngine);
        when(templateEngine.getValue("condition", Boolean.class)).thenReturn(true);
        when(policy.stream(any(), any())).thenReturn(new BufferedReadWriteStream());
        ConditionalExecutablePolicy delegatePolicy = new ConditionalExecutablePolicy(policy, "condition", conditionEvaluator);

        final ConditionalExecutablePolicy toDebug = new DebugConditionalExecutablePolicy(delegatePolicy, debugStep);
        final ReadWriteStream<Buffer> conditionedStream = toDebug.stream(policyChain, executionContext);

        conditionedStream.write(Buffer.buffer("Test")).end();
        verify(policyChain, never()).doNext(any(), any());
        assertThat(debugStep.getDebugDiffContent()).containsEntry(DebugStep.DIFF_KEY_CONDITION, "condition");
    }

    @Test
    public void shouldNotStreamConditionalPolicyConditionEvaluatedToFalse() throws PolicyException, NoSuchMethodException {
        when(executionContext.getTemplateEngine()).thenReturn(templateEngine);
        when(templateEngine.getValue("condition", Boolean.class)).thenReturn(false);
        when(policy.stream(any(), any())).thenReturn(new BufferedReadWriteStream());
        ConditionalExecutablePolicy delegatePolicy = new ConditionalExecutablePolicy(policy, "condition", conditionEvaluator);

        final ConditionalExecutablePolicy toDebug = new DebugConditionalExecutablePolicy(delegatePolicy, debugStep);
        final ReadWriteStream<Buffer> conditionedStream = toDebug.stream(policyChain, executionContext);
        conditionedStream.write(Buffer.buffer("Test")).end();
        verify(executionContext, never()).setAttribute("stream", "On Response Content Dummy Streamable Policy");
        assertThat(debugStep.getDebugDiffContent()).containsEntry(DebugStep.DIFF_KEY_CONDITION, "condition");
        assertThat(debugStep.getStatus()).isEqualTo(DebugStepStatus.SKIPPED);
    }

    @Test
    public void shouldNotStreamConditionalPolicyExpressionEvaluationException() throws PolicyException {
        when(executionContext.getTemplateEngine()).thenReturn(templateEngine);
        when(templateEngine.getValue("condition", Boolean.class)).thenThrow(ExpressionEvaluationException.class);
        when(policy.stream(any(), any())).thenReturn(new BufferedReadWriteStream());
        ConditionalExecutablePolicy delegatePolicy = new ConditionalExecutablePolicy(policy, "condition", conditionEvaluator);

        final ConditionalExecutablePolicy toDebug = new DebugConditionalExecutablePolicy(delegatePolicy, debugStep);
        final ReadWriteStream<Buffer> conditionedStream = toDebug.stream(policyChain, executionContext);
        conditionedStream.write(Buffer.buffer("Test")).end();
        verify(policyChain, times(1)).streamFailWith(any());
        assertThat(debugStep.getDebugDiffContent()).containsEntry(DebugStep.DIFF_KEY_CONDITION, "condition");
        assertThat(debugStep.getStatus()).isNull();
    }
}
